<template>
  <view class="page">
    <view class="page-head row-between">
      <view><text class="eyebrow">出行车辆</text><view class="page-title">车辆信息</view><text class="muted">提前确认车辆、座位和集合安排</text></view>
      <button v-if="creator" class="mini-primary" @click="add">＋ 发布</button>
    </view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 2" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load()" />
    <view v-for="vehicle in list" v-else :key="vehicle.id" class="card vehicle-card">
      <view class="row-between"><view class="plate">{{ vehicle.plateNumber }}</view><text class="tag">{{ vehicle.mine ? '我的座位' : '出行车辆' }}</text></view>
      <view class="vehicle-info">
        <view class="info-line"><view class="info-icon"><AppIcon name="user" /></view><text class="info-value">司机 {{ vehicle.driverName }}</text></view>
        <view class="info-line"><view class="info-icon"><AppIcon name="clock" /></view><text class="info-value">{{ displayTime(vehicle.pickupTime) }}</text></view>
        <view class="info-line"><view class="info-icon"><AppIcon name="pin" /></view><text class="info-value">{{ vehicle.pickupLocation }}</text></view>
      </view>
      <view class="seats">
        <view class="row-between seat-copy"><text>座位</text><text :class="{ full: isFull(vehicle) }">{{ vehicle.occupiedSeats }} / {{ vehicle.seats }}{{ isFull(vehicle) ? ' 已坐满' : '' }}</text></view>
        <view class="seat-track"><view :style="{ width: `${Math.min(100, vehicle.occupiedSeats / vehicle.seats * 100)}%` }"></view></view>
      </view>
      <view v-if="vehicle.passengers.length" class="riders">
        <view v-for="rider in vehicle.passengers" :key="rider.userId" class="rider">
          <text>{{ rider.nickname }}{{ rider.seats > 1 ? ` ×${rider.seats}` : '' }}</text>
          <text v-if="creator || rider.userId === me?.id" class="rider-remove" @click="alight(vehicle, rider.userId)">×</text>
        </view>
      </view>
      <view class="vehicle-actions">
        <button v-if="vehicle.mine" class="ghost-btn" :disabled="busy" @click="alight(vehicle, me!.id)">下车</button>
        <button v-else class="mini-primary" :disabled="busy || isFull(vehicle)" @click="board(vehicle)">{{ isFull(vehicle) ? '已坐满' : seatedElsewhere(vehicle) ? '换乘这辆' : '我要乘坐' }}</button>
        <template v-if="creator">
          <picker :range="candidates" range-key="nickname" :disabled="!candidates.length || busy" @change="assign(vehicle, $event)">
            <button class="ghost-btn" :disabled="!candidates.length || busy">{{ candidates.length ? '安排乘客' : '成员均已安排' }}</button>
          </picker>
          <button class="ghost-btn" @click="edit(vehicle)">编辑</button>
          <button class="ghost-btn danger" :disabled="busy" @click="remove(vehicle)">删除</button>
        </template>
      </view>
    </view>
    <view v-if="!loading && !error && !list.length" class="empty">暂未发布车辆信息</view>
  </view>
</template>
<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { currentUser } from '@/services/session'
import { displayTime } from '@/utils/time'
import type { Member, Vehicle } from '@/services/types'
const id = ref(0)
const creator = ref(false)
const list = ref<Vehicle[]>([])
const members = ref<Member[]>([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const me = currentUser()
// Members not yet seated anywhere; the leader assigns them with a picker (action sheets cap at 6 items on WeChat).
const candidates = computed(() => {
  const seated = new Set(list.value.flatMap(vehicle => vehicle.passengers.map(rider => rider.userId)))
  return members.value.filter(member => !seated.has(member.userId))
})
onLoad((options: any) => { id.value = Number(options.id); creator.value = options.creator === '1' })
async function load(showLoading = true) {
  if (!id.value) return
  if (showLoading) loading.value = true
  error.value = ''
  try {
    list.value = await api(`/activities/${id.value}/vehicles`)
    if (creator.value) members.value = await api<Member[]>(`/activities/${id.value}/signups`, 'GET', undefined, { silent: true }).catch(() => [])
  }
  catch (reason: any) { error.value = reason?.message || '车辆信息加载失败'; list.value = [] }
  finally { loading.value = false }
}
onShow(() => load())
onPullDownRefresh(async () => { try { await load(false) } finally { uni.stopPullDownRefresh() } })
const isFull = (vehicle: Vehicle) => vehicle.occupiedSeats >= vehicle.seats
const seatedElsewhere = (vehicle: Vehicle) => list.value.some(item => item.mine && item.id !== vehicle.id)
// Boarding can move a rider between vehicles, so refresh the whole list after every change.
async function mutate(action: () => Promise<unknown>, success: string) {
  if (busy.value) return
  busy.value = true
  try { await action(); await load(false); uni.showToast({ title: success }) }
  finally { busy.value = false }
}
const board = (vehicle: Vehicle) => mutate(() => api(`/activities/${id.value}/vehicles/${vehicle.id}/passengers`, 'POST', {}), '已上车')
const alight = (vehicle: Vehicle, userId: number) => mutate(() => api(`/activities/${id.value}/vehicles/${vehicle.id}/passengers/${userId}`, 'DELETE'), '已取消座位')
function assign(vehicle: Vehicle, event: any) {
  const member = candidates.value[Number(event.detail.value)]
  if (member) mutate(() => api(`/activities/${id.value}/vehicles/${vehicle.id}/passengers`, 'POST', { userId: member.userId }), `已安排 ${member.nickname}`)
}
function remove(vehicle: Vehicle) {
  uni.showModal({ title: '删除车辆', content: `删除 ${vehicle.plateNumber} 后，车上乘客需要重新安排。确定删除吗？`, confirmColor: '#DC4C4C', success: result => {
    if (result.confirm) mutate(() => api(`/activities/${id.value}/vehicles/${vehicle.id}`, 'DELETE'), '已删除')
  } })
}
const add = () => uni.navigateTo({ url: `/pages/vehicleAdd/vehicleAdd?id=${id.value}` })
const edit = (vehicle: Vehicle) => uni.navigateTo({ url: `/pages/vehicleAdd/vehicleAdd?id=${id.value}&vehicleId=${vehicle.id}` })
</script>
<style scoped lang="scss">
.page-head{align-items:flex-end}.plate{min-width:0;font-size:38rpx;font-weight:850;line-height:1.3;letter-spacing:3rpx;word-break:break-word}.vehicle-info{margin-top:22rpx;padding-top:14rpx;border-top:1rpx solid $border}
.seats{margin-top:20rpx}.seat-copy{margin-bottom:10rpx;color:$text-secondary;font-size:24rpx}.seat-copy .full{color:$danger;font-weight:650}
.seat-track{height:12rpx;overflow:hidden;background:$primary-soft;border-radius:999rpx}.seat-track view{height:100%;background:$primary;border-radius:999rpx;transition:width .2s}
.riders{display:flex;flex-wrap:wrap;margin-top:18rpx;gap:12rpx}.rider{display:flex;align-items:center;padding:8rpx 16rpx;color:$text-secondary;font-size:23rpx;background:#f4f7f6;border-radius:999rpx;gap:10rpx}.rider-remove{color:$text-muted;font-size:28rpx;line-height:1}
.vehicle-actions{display:flex;flex-wrap:wrap;align-items:center;margin-top:22rpx;gap:12rpx}.vehicle-actions .ghost-btn{padding:0 22rpx}.vehicle-actions .danger{color:$danger}
</style>
