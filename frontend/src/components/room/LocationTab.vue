<template>
  <view>
    <view v-if="!sharing" class="card welcome">
      <view class="section-title">主动开启位置共享</view>
      <view class="muted">仅活动参与者可查看；关闭或离开活动空间后立即停止。</view>
      <button class="primary-btn" @click="start">开启位置共享</button>
      <button class="secondary-btn" @click="manual = true">地址搜索 / 手动选点</button>
    </view>
    <view v-else>
      <map class="map" :latitude="center.latitude" :longitude="center.longitude" :markers="markers" show-location />
      <view class="row-between card"><text>{{ locations.length }} 人正在共享</text><button size="mini" class="mini-danger" @click="stop">停止共享</button></view>
      <view v-for="location in locations" :key="location.userId" class="card">
        <view class="section-title">{{ location.nickname }}</view>
        <view class="muted">{{ location.address || `${location.latitude}, ${location.longitude}` }}</view>
        <view class="muted">{{ displayTime(location.updatedAt) }}</view>
      </view>
    </view>
    <view v-if="manual" class="card">
      <view class="section-title">查找或手动输入位置</view>
      <view class="search-row"><input v-model="keyword" class="input search-input" placeholder="学校、景点或详细地址" /><button size="mini" @click="search">搜索</button></view>
      <view v-for="place in places" :key="`${place.latitude}-${place.longitude}`" class="place" @click="choose(place)">
        <view>{{ place.address }}</view><view class="muted">{{ place.latitude }}, {{ place.longitude }}</view>
      </view>
      <input v-model.number="manualForm.latitude" class="input gap" type="digit" placeholder="纬度" />
      <input v-model.number="manualForm.longitude" class="input gap" type="digit" placeholder="经度" />
      <input v-model="manualForm.address" class="input gap" placeholder="地址描述" />
      <button class="primary-btn" @click="applyManual">开始共享此位置</button>
      <button class="secondary-btn" @click="manual = false">取消</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { api } from '@/services/api'
import { displayTime } from '@/utils/time'
import type { Location } from '@/services/types'

type Place = { address: string; latitude: number; longitude: number }
const props = defineProps<{ activityId: number; refreshKey: number }>()
const sharing = ref(false), manual = ref(false), locations = ref<Location[]>([]), timer = ref<any>()
const keyword = ref(''), places = ref<Place[]>([])
const current = reactive({ latitude: 39.9042, longitude: 116.4074, address: '' })
const manualForm = reactive({ latitude: 39.9042, longitude: 116.4074, address: '' })
const center = computed(() => locations.value[0] || current)
const markers = computed(() => locations.value.map(location => ({
  id: location.userId, latitude: Number(location.latitude), longitude: Number(location.longitude), title: location.nickname,
  width: 28, height: 36, iconPath: '/static/icons/marker.svg', callout: { content: location.nickname, display: 'ALWAYS', padding: 4 }
})))

async function load() { locations.value = await api(`/activities/${props.activityId}/locations`) }
watch(() => props.refreshKey, load, { immediate: true })
function start() {
  uni.getLocation({ type: 'gcj02', isHighAccuracy: true, success: async (result: any) => {
    current.latitude = result.latitude; current.longitude = result.longitude; current.address = result.address || ''
    sharing.value = true; await update(); timer.value = setInterval(refreshPosition, 15000)
  }, fail: () => { manual.value = true; uni.showToast({ title: '自动定位失败，请搜索或手动设置', icon: 'none' }) } })
}
function refreshPosition() { uni.getLocation({ type: 'gcj02', success: async (result: any) => { current.latitude = result.latitude; current.longitude = result.longitude; current.address = result.address || current.address; await update() } }) }
async function search() {
  if (!keyword.value.trim()) return
  try { places.value = await api(`/geocoding/search?address=${encodeURIComponent(keyword.value.trim())}`) }
  catch { places.value = []; uni.showToast({ title: '地址服务不可用，可直接输入坐标', icon: 'none' }) }
}
function choose(place: Place) { Object.assign(manualForm, place); places.value = [] }
async function update() { await api(`/activities/${props.activityId}/locations/me`, 'PUT', current); await load() }
async function applyManual() { Object.assign(current, manualForm); manual.value = false; sharing.value = true; await update(); timer.value = setInterval(() => update(), 15000) }
async function stop() { if (timer.value) clearInterval(timer.value); timer.value = null; if (sharing.value) await api(`/activities/${props.activityId}/locations/me`, 'DELETE'); sharing.value = false; locations.value = [] }
onUnmounted(() => { if (timer.value) clearInterval(timer.value); if (sharing.value) api(`/activities/${props.activityId}/locations/me`, 'DELETE').catch(() => undefined) })
</script>

<style scoped lang="scss">
.welcome{text-align:center;padding:60rpx 30rpx}.map{width:100%;height:52vh;border-radius:$radius}.gap{margin-top:14rpx}
.search-row{display:flex;align-items:center;gap:12rpx}.search-input{flex:1}.place{padding:18rpx 4rpx;border-bottom:1px solid $border}
</style>
