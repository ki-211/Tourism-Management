<template>
  <view class="page">
    <view class="page-head row-between">
      <view><text class="eyebrow">TRANSPORT</text><view class="page-title">车辆信息</view><text class="muted">提前确认车辆和集合安排</text></view>
      <button v-if="creator" class="mini-primary" @click="add">＋ 发布</button>
    </view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 2" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="vehicle in list" v-else :key="vehicle.id" class="card vehicle-card">
      <view class="row-between"><view class="plate">{{ vehicle.plateNumber }}</view><text class="tag">出行车辆</text></view>
      <view class="vehicle-info">
        <view class="info-line"><text class="info-icon">◇</text><text class="info-value">司机 {{ vehicle.driverName }}</text></view>
        <view class="info-line"><text class="info-icon">◷</text><text class="info-value">{{ displayTime(vehicle.pickupTime) }}</text></view>
        <view class="info-line"><text class="info-icon">⌖</text><text class="info-value">{{ vehicle.pickupLocation }}</text></view>
      </view>
    </view>
    <view v-if="!loading && !error && !list.length" class="empty">暂未发布车辆信息</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { displayTime } from '@/utils/time'
import type { Vehicle } from '@/services/types'
const id = ref(0)
const creator = ref(false)
const list = ref<Vehicle[]>([])
const loading = ref(false)
const error = ref('')
onLoad((options: any) => { id.value = Number(options.id); creator.value = options.creator === '1' })
async function load() {
  if (!id.value) return
  loading.value = true
  error.value = ''
  try { list.value = await api(`/activities/${id.value}/vehicles`) }
  catch (reason: any) { error.value = reason?.message || '车辆信息加载失败'; list.value = [] }
  finally { loading.value = false }
}
onShow(load)
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
const add = () => uni.navigateTo({ url: `/pages/vehicleAdd/vehicleAdd?id=${id.value}` })
</script>
<style scoped lang="scss">
.page-head{align-items:flex-end}.plate{min-width:0;font-size:38rpx;font-weight:850;line-height:1.3;letter-spacing:3rpx;word-break:break-word}.vehicle-info{margin-top:22rpx;padding-top:14rpx;border-top:1rpx solid $border}
</style>
