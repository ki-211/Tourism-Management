<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">发布任务</text><view class="page-title">发布签到</view><text class="muted">创建任务后，参与者即可在活动空间完成签到。</text></view>
    <view class="card form-card">
      <view class="field"><text class="field-label">任务标题</text><input v-model="title" class="input" maxlength="80" placeholder="例如：景区入口集合" /></view>
      <view class="field"><text class="field-label">任务说明（选填）</text><textarea v-model="description" class="textarea" maxlength="500" placeholder="补充签到要求或集合说明" /></view>

      <view class="rule-row">
        <view class="rule-copy"><text class="rule-title">截止时间</text><text class="muted">截止后不能再签到，也不再出现在待签到列表</text></view>
        <switch :checked="useDeadline" color="#0F766E" @change="useDeadline = switchValue($event)" />
      </view>
      <view v-if="useDeadline" class="field"><DateTimeField v-model="deadline" /></view>

      <view class="rule-row">
        <view class="rule-copy"><text class="rule-title">限定签到范围</text><text class="muted">成员需在签到点附近才能签到</text></view>
        <switch :checked="useGeofence" color="#0F766E" @change="useGeofence = switchValue($event)" />
      </view>
      <view v-if="useGeofence" class="geofence">
        <view class="point" :class="{ ready: point }">
          <AppIcon name="pin" :size="34" />
          <view class="point-copy"><text class="point-label">{{ point ? '签到点' : '尚未选择签到点' }}</text><text class="point-value">{{ point ? point.address : '使用当前位置，或搜索地址' }}</text></view>
        </view>
        <button class="secondary-btn" :disabled="locating" @click="useCurrentLocation">{{ locating ? '定位中…' : '使用我当前的位置' }}</button>
        <view class="search-row">
          <input v-model="keyword" class="input" maxlength="100" confirm-type="search" placeholder="搜索集合地址" @confirm="search" />
          <button class="mini-primary" :loading="searching" :disabled="searching" @click="search">搜索</button>
        </view>
        <view v-for="place in places" :key="`${place.latitude},${place.longitude}`" class="place" @click="choose(place)">
          <text>{{ place.address }}</text><text class="place-pick">选择</text>
        </view>
        <text class="field-label radius-label">签到半径</text>
        <view class="radius-options">
          <button v-for="option in radiusOptions" :key="option" class="radius" :class="{ active: radius === option }" @click="radius = option">{{ option >= 1000 ? `${option / 1000} 公里` : `${option} 米` }}</button>
        </view>
      </view>

      <view class="rule-row">
        <view class="rule-copy"><text class="rule-title">必须上传现场照片</text><text class="muted">适合需要留存凭证的集合或打卡</text></view>
        <switch :checked="photoRequired" color="#0F766E" @change="photoRequired = switchValue($event)" />
      </view>

      <button class="primary-btn" :loading="loading" :disabled="loading" @click="submit">发布签到</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import DateTimeField from '@/components/DateTimeField.vue'
import { api } from '@/services/api'
import { apiTime } from '@/utils/time'

type Place = { address: string; latitude: number; longitude: number }
const radiusOptions = [100, 200, 500, 1000, 2000]
const id = ref(0)
const title = ref('')
const description = ref('')
const loading = ref(false)
const useDeadline = ref(false)
const deadline = ref('')
const useGeofence = ref(false)
const point = ref<Place | null>(null)
const radius = ref(200)
const keyword = ref('')
const places = ref<Place[]>([])
const searching = ref(false)
const locating = ref(false)
const photoRequired = ref(false)

onLoad((options: any) => { id.value = Number(options.id) })

// uni's <switch> emits { detail: { value } }, which the DOM Event type does not describe.
const switchValue = (event: any) => Boolean(event.detail.value)

function useCurrentLocation() {
  if (locating.value) return
  locating.value = true
  uni.getLocation({
    type: 'gcj02',
    geocode: true,
    success: (result: any) => {
      point.value = { latitude: result.latitude, longitude: result.longitude, address: result.address || `${result.latitude.toFixed(5)}, ${result.longitude.toFixed(5)}` }
    },
    fail: () => uni.showToast({ title: '定位失败，可以改用地址搜索', icon: 'none' }),
    complete: () => { locating.value = false }
  })
}

async function search() {
  if (!keyword.value.trim() || searching.value) return
  searching.value = true
  try {
    places.value = await api<Place[]>(`/geocoding/search?address=${encodeURIComponent(keyword.value.trim())}`)
    if (!places.value.length) uni.showToast({ title: '没有找到该地址', icon: 'none' })
  } finally { searching.value = false }
}

function choose(place: Place) {
  point.value = { ...place, latitude: Number(place.latitude), longitude: Number(place.longitude) }
  places.value = []
}

async function submit() {
  if (loading.value) return
  if (!title.value.trim()) return uni.showToast({ title: '请输入标题', icon: 'none' })
  if (useDeadline.value && (!deadline.value || new Date(deadline.value).getTime() <= Date.now()))
    return uni.showToast({ title: '截止时间需晚于当前时间', icon: 'none' })
  if (useGeofence.value && !point.value) return uni.showToast({ title: '请选择签到点', icon: 'none' })
  loading.value = true
  try {
    await api(`/activities/${id.value}/sign-tasks`, 'POST', {
      title: title.value,
      description: description.value,
      deadline: useDeadline.value ? apiTime(deadline.value) : null,
      ...(useGeofence.value && point.value
        ? { latitude: point.value.latitude, longitude: point.value.longitude, radiusMeters: radius.value, locationName: point.value.address.slice(0, 200) }
        : {}),
      photoRequired: photoRequired.value
    })
    uni.showToast({ title: '发布成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.rule-row { display: flex; align-items: center; justify-content: space-between; margin: 8rpx 0 22rpx; padding-top: 24rpx; border-top: 1rpx solid $border; gap: 20rpx; }
.rule-copy { min-width: 0; }
.rule-title { display: block; color: $text-main; font-size: 27rpx; font-weight: 650; line-height: 1.4; }
.rule-copy .muted { display: block; font-size: 22rpx; }
.geofence { margin-bottom: 22rpx; }
.point { display: flex; align-items: center; margin-bottom: 16rpx; padding: 20rpx; background: #f9fbfa; border: 2rpx dashed $border; border-radius: 18rpx; gap: 16rpx; }
.point.ready { background: $primary-soft; border-style: solid; border-color: transparent; }
.point-icon { color: $primary; font-size: 32rpx; }
.point-copy { min-width: 0; }
.point-label, .point-value { display: block; line-height: 1.5; }
.point-label { color: $text-muted; font-size: 22rpx; }
.point-value { color: $text-main; font-size: 25rpx; word-break: break-all; }
.search-row { display: flex; align-items: center; margin-top: 16rpx; gap: 12rpx; }
.search-row .input { flex: 1; min-width: 0; }
.place { display: flex; align-items: center; justify-content: space-between; padding: 18rpx 6rpx; color: $text-secondary; font-size: 24rpx; border-bottom: 1rpx solid $border; gap: 16rpx; }
.place-pick { flex-shrink: 0; color: $primary; }
.radius-label { margin-top: 22rpx; }
.radius-options { display: flex; flex-wrap: wrap; gap: 12rpx; }
.radius { height: 64rpx; margin: 0; padding: 0 22rpx; color: $text-secondary; font-size: 24rpx; line-height: 64rpx; background: #f9fbfa; border: 2rpx solid $border; border-radius: 14rpx; }
.radius.active { color: #fff; background: $primary; border-color: $primary; }
</style>
