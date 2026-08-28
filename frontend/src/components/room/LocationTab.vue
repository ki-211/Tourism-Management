<template>
  <view class="location-tab">
    <view v-if="initialLoading" class="loading-stack"><view class="skeleton-card map-skeleton"></view><view class="skeleton-card"></view></view>
    <template v-else>
      <LoadError v-if="loadError && !locations.length" :message="loadError" @retry="load(true)" />
      <template v-else>
        <view class="card sharing-card" :class="`status-${sharingStore.status}`">
          <view class="row-between">
            <view class="sharing-copy"><text class="section-title">{{ statusTitle }}</text><text class="muted">{{ statusDescription }}</text></view>
            <view class="status-dot"></view>
          </view>
          <button v-if="!sharingThisActivity" class="primary-btn" :loading="sharingStore.status === 'starting'" :disabled="sharingStore.status === 'starting'" @click="startSharing">开启位置共享</button>
          <button v-else class="danger-btn" :loading="sharingStore.status === 'stopping'" :disabled="sharingStore.status === 'stopping'" @click="stopSharing">停止共享</button>
          <button v-if="sharingStore.canOpenSettings" class="secondary-btn" @click="sharingStore.openSettings">打开定位设置</button>
          <button v-if="!sharingThisActivity || sharingStore.status === 'error'" class="secondary-btn" @click="manual = true">搜索或共享固定位置</button>
        </view>

        <view v-if="!mapConfigured" class="config-warning">
          <text class="warning-title">H5 地图服务尚未配置</text>
          <text>配置高德 H5 JS Key 与安全设置后即可显示地图；成员位置列表仍可正常查看。</text>
        </view>

        <view v-if="locations.length" class="map-shell">
          <map v-if="mapConfigured" id="memberMap" class="map" :latitude="center.latitude" :longitude="center.longitude" :scale="scale" :markers="markers" :include-points="includePoints" show-location />
          <view class="map-actions">
            <button v-if="mapConfigured" size="mini" @click="viewAll">查看全部</button>
            <button v-if="mapConfigured && myPosition" size="mini" @click="focus(myPosition)">回到我的位置</button>
          </view>
        </view>
        <view v-else-if="!loadError" class="empty">暂时没有成员共享位置</view>

        <view v-if="locations.length" class="member-head row-between"><view><text class="section-title">共享成员</text><text class="muted">{{ locations.length }} 人正在共享</text></view><button class="ghost-btn" @click="load(false)">刷新</button></view>
        <view v-for="location in locations" :key="location.userId" class="card member-card" @click="focus(location)">
          <view class="row-between"><view class="member-name"><view class="mini-avatar" :class="{ mine: location.userId === me?.id }">{{ location.nickname?.slice(0, 1) || '友' }}</view><view><text class="member-title">{{ location.nickname }}{{ location.userId === me?.id ? '（我）' : '' }}</text><text class="muted">{{ location.address || `${location.latitude}, ${location.longitude}` }}</text></view></view><text class="focus-link">查看 ›</text></view>
          <text class="updated-at">更新于 {{ displayTime(location.updatedAt) }}</text>
        </view>

        <view v-if="manual" class="card manual-card">
          <view class="row-between"><view><text class="section-title">选择固定位置</text><text class="muted">适合定位权限不可用时临时标记集合点</text></view><button class="close-button" @click="manual = false">×</button></view>
          <view class="search-row"><input v-model="keyword" class="input search-input" maxlength="100" placeholder="机构、场馆或详细地址" confirm-type="search" @confirm="search" /><button size="mini" :loading="searching" :disabled="searching" @click="search">搜索</button></view>
          <view v-if="searchMessage" class="search-message">{{ searchMessage }}</view>
          <view v-for="place in places" :key="`${place.latitude}-${place.longitude}`" class="place" @click="choose(place)">
            <view>{{ place.address }}</view><view class="muted">{{ place.latitude }}, {{ place.longitude }}</view>
          </view>
          <view class="advanced-toggle" @click="showCoordinates = !showCoordinates">{{ showCoordinates ? '收起坐标输入' : '高级：直接输入坐标' }} ›</view>
          <template v-if="showCoordinates">
            <input v-model.number="manualForm.latitude" class="input gap" type="digit" placeholder="纬度（-90 至 90）" />
            <input v-model.number="manualForm.longitude" class="input gap" type="digit" placeholder="经度（-180 至 180）" />
            <input v-model="manualForm.address" class="input gap" maxlength="500" placeholder="地址描述" />
          </template>
          <view v-if="selectedAddress" class="selected-place"><text>已选择</text><text>{{ selectedAddress }}</text></view>
          <button class="primary-btn" :disabled="manualStarting" :loading="manualStarting" @click="applyManual">共享此固定位置</button>
          <button class="secondary-btn" :disabled="manualStarting" @click="manual = false">取消</button>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { currentUser } from '@/services/session'
import type { Location } from '@/services/types'
import { useLocationSharingStore } from '@/stores/locationSharing'
import { applyLocationEvent, validCoordinates } from '@/utils/location'
import { displayTime } from '@/utils/time'

type Place = { address?: string; latitude: number; longitude: number }
const props = defineProps<{ activityId: number; activityEndTime: string; refreshKey: number; roomEvent?: any }>()
const sharingStore = useLocationSharingStore()
const me = currentUser()
const locations = ref<Location[]>([])
const initialLoading = ref(true)
const loadError = ref('')
const manual = ref(false)
const searching = ref(false)
const manualStarting = ref(false)
const showCoordinates = ref(false)
const keyword = ref('')
const searchMessage = ref('')
const places = ref<Place[]>([])
const manualForm = reactive<any>({ latitude: '', longitude: '', address: '' })
const focused = ref<Place | null>(null)
const scale = ref(13)
const mapConfigured = ref(true)
// #ifdef H5
mapConfigured.value = Boolean(import.meta.env.VITE_AMAP_JS_KEY) && import.meta.env.VITE_AMAP_SECURITY_CONFIGURED === 'true'
// #endif

const sharingThisActivity = computed(() => sharingStore.sharing && sharingStore.activityId === props.activityId)
const myPosition = computed(() => locations.value.find(item => item.userId === me?.id) || sharingStore.current)
const center = computed(() => focused.value || myPosition.value || locations.value[0] || { latitude: 39.9042, longitude: 116.4074 })
const includePoints = computed(() => locations.value.map(item => ({ latitude: Number(item.latitude), longitude: Number(item.longitude) })))
const markers = computed(() => locations.value.map(location => ({
  id: location.userId,
  latitude: Number(location.latitude),
  longitude: Number(location.longitude),
  title: location.nickname,
  width: 34,
  height: 42,
  iconPath: location.userId === me?.id ? '/static/icons/marker-self.png' : '/static/icons/marker-member.png',
  callout: { content: location.nickname, display: 'BYCLICK', padding: 6, borderRadius: 8 }
})))
const statusTitle = computed(() => {
  if (!sharingThisActivity.value) return '查看队友位置'
  return ({ starting: '正在开启共享', active: '位置共享中', paused: '位置共享已暂停', stopping: '正在停止共享', error: '位置共享异常', idle: '位置共享未开启' } as Record<string, string>)[sharingStore.status]
})
const statusDescription = computed(() => {
  if (sharingStore.activityId && sharingStore.activityId !== props.activityId && sharingStore.sharing) return '你正在另一个活动中共享位置，开启后将自动切换到当前活动。'
  if (sharingThisActivity.value && sharingStore.message) return sharingStore.message
  if (sharingThisActivity.value) return '微信小程序可在后台持续共享；H5 切到后台时会自动暂停。'
  return '无需共享自己的位置，也可以查看当前活动中已共享的成员。'
})
const selectedAddress = computed(() => manualForm.address || (validCoordinates(manualForm.latitude, manualForm.longitude) ? `${manualForm.latitude}, ${manualForm.longitude}` : ''))

async function load(first = false) {
  if (first) initialLoading.value = true
  loadError.value = ''
  try { locations.value = await api(`/activities/${props.activityId}/locations`) }
  catch (reason: any) { loadError.value = reason?.message || '位置列表加载失败' }
  finally { initialLoading.value = false }
}
watch(() => props.refreshKey, () => load(false), { immediate: true })
watch(() => props.roomEvent, event => { locations.value = applyLocationEvent(locations.value, event) })
watch(() => sharingStore.current, current => {
  if (current && sharingStore.activityId === props.activityId) locations.value = applyLocationEvent(locations.value, { type: 'LOCATION_UPDATED', payload: current })
})

async function startSharing() { await sharingStore.start(props.activityId, props.activityEndTime) }
async function stopSharing() { await sharingStore.stop(true); locations.value = locations.value.filter(item => item.userId !== me?.id) }
function focus(location: Place) { focused.value = { address: location.address || '', latitude: Number(location.latitude), longitude: Number(location.longitude) }; scale.value = 16 }
function viewAll() {
  focused.value = null
  scale.value = 13
  const context = uni.createMapContext('memberMap') as any
  context.includePoints?.({ points: includePoints.value, padding: [80, 60, 80, 60] })
}
async function search() {
  if (!keyword.value.trim() || searching.value) return
  searching.value = true
  searchMessage.value = ''
  try {
    places.value = await api(`/geocoding/search?address=${encodeURIComponent(keyword.value.trim())}`)
    if (!places.value.length) searchMessage.value = '没有找到匹配地点，请补充更详细的地址'
  } catch { places.value = []; searchMessage.value = '地址服务暂不可用，可使用坐标输入' }
  finally { searching.value = false }
}
function choose(place: Place) { Object.assign(manualForm, place); places.value = []; showCoordinates.value = false }
async function applyManual() {
  if (!validCoordinates(manualForm.latitude, manualForm.longitude)) return uni.showToast({ title: '请输入有效的经纬度', icon: 'none' })
  if (manualStarting.value) return
  manualStarting.value = true
  try {
    await sharingStore.startAt(props.activityId, props.activityEndTime, { latitude: Number(manualForm.latitude), longitude: Number(manualForm.longitude) }, String(manualForm.address || ''))
    manual.value = false
  } catch { /* The store exposes the actionable error message in the sharing card. */ }
  finally { manualStarting.value = false }
}
</script>

<style scoped lang="scss">
.sharing-card{padding:30rpx}.sharing-copy{flex:1;min-width:0}.sharing-copy .section-title{display:block;margin-bottom:7rpx}.status-dot{flex-shrink:0;width:20rpx;height:20rpx;background:#a8b4b1;border:5rpx solid #edf1f0;border-radius:50%}.status-active .status-dot{background:$success;border-color:rgba(34,158,102,.16)}.status-paused .status-dot{background:$warning;border-color:rgba(236,145,36,.16)}.status-error .status-dot{background:$danger;border-color:rgba(220,76,76,.14)}
.config-warning{margin-bottom:24rpx;padding:24rpx;color:#80531e;font-size:24rpx;line-height:1.6;background:$accent-soft;border:1rpx solid rgba(236,145,36,.25);border-radius:20rpx}.warning-title{display:block;margin-bottom:5rpx;font-weight:750}.map-shell{position:relative;margin-bottom:28rpx}.map{width:100%;height:52vh;min-height:560rpx;border-radius:$radius}.map-actions{position:absolute;right:18rpx;bottom:18rpx;display:flex;gap:10rpx}.map-actions button{height:62rpx;margin:0;padding:0 20rpx;color:$primary-dark;font-size:22rpx;line-height:62rpx;background:rgba(255,255,255,.94);border-radius:999rpx;box-shadow:0 5rpx 18rpx rgba(20,55,50,.16)}
.member-head{margin:34rpx 5rpx 16rpx}.member-head .section-title{display:block;margin-bottom:4rpx}.member-card{padding:24rpx;cursor:pointer}.member-name{display:flex;align-items:center;min-width:0;gap:16rpx}.member-name>view:last-child{min-width:0}.mini-avatar{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:68rpx;height:68rpx;color:#ad552d;font-size:25rpx;font-weight:800;background:#ffede4;border-radius:20rpx}.mini-avatar.mine{color:$primary;background:$primary-soft}.member-title{display:block;margin-bottom:4rpx;font-size:27rpx;font-weight:750}.focus-link{flex-shrink:0;color:$primary;font-size:23rpx}.updated-at{display:block;margin-top:14rpx;padding-top:12rpx;color:$text-muted;font-size:21rpx;border-top:1rpx solid $border}
.manual-card{margin-top:28rpx}.close-button{flex-shrink:0;width:58rpx;height:58rpx;margin:0;padding:0;color:$text-muted;font-size:38rpx;line-height:54rpx;background:#eef3f2;border-radius:50%}.search-row{display:flex;align-items:center;width:100%;margin-top:24rpx;gap:12rpx}.search-input{flex:1;min-width:0}.search-row button{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:116rpx;height:84rpx;margin:0;padding:0;color:#fff;line-height:1;background:$primary;border-radius:16rpx}.search-message{padding:18rpx 4rpx;color:$text-muted;font-size:23rpx}.place{padding:20rpx 4rpx;border-bottom:1rpx solid $border}.advanced-toggle{padding:22rpx 2rpx 8rpx;color:$primary;font-size:24rpx}.gap{margin-top:14rpx}.selected-place{margin-top:20rpx;padding:20rpx;background:$primary-soft;border-radius:16rpx}.selected-place text{display:block;color:$primary-dark;font-size:24rpx}.selected-place text:first-child{margin-bottom:5rpx;font-size:21rpx;font-weight:750}.map-skeleton{height:52vh;min-height:560rpx}
</style>
