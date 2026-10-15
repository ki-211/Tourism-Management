<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">出行安排</text><view class="page-title">{{ vehicleId ? '编辑车辆' : '发布车辆' }}</view><text class="muted">补充司机、时间、上车点和座位，方便大家准时集合。</text></view>
    <view class="card form-card">
      <view class="field"><text class="field-label">车牌号</text><input v-model="form.plateNumber" class="input" maxlength="20" placeholder="请输入车牌号" /></view>
      <view class="field"><text class="field-label">司机姓名</text><input v-model="form.driverName" class="input" maxlength="30" placeholder="请输入司机姓名" /></view>
      <view class="field"><text class="field-label">上车时间</text><DateTimeField v-model="form.pickupTime" /></view>
      <view class="field"><text class="field-label">上车地点</text><input v-model="form.pickupLocation" class="input" maxlength="120" placeholder="请输入集合地点" /></view>
      <view class="field"><text class="field-label">乘客座位数（不含司机）</text><input v-model="form.seats" class="input" type="number" maxlength="2" placeholder="例如：4" /></view>
      <button class="primary-btn" :loading="loading" :disabled="loading" @click="submit">{{ vehicleId ? '保存修改' : '发布车辆信息' }}</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import DateTimeField from '@/components/DateTimeField.vue'
import { api } from '@/services/api'
import { apiTime } from '@/utils/time'
import type { Vehicle } from '@/services/types'
const id = ref(0)
const vehicleId = ref(0)
const loading = ref(false)
const form = reactive({ plateNumber: '', driverName: '', pickupTime: '', pickupLocation: '', seats: '4' })
onLoad(async (options: any) => {
  id.value = Number(options.id)
  if (!options.vehicleId) return
  vehicleId.value = Number(options.vehicleId)
  uni.setNavigationBarTitle({ title: '编辑车辆' })
  const vehicle = (await api<Vehicle[]>(`/activities/${id.value}/vehicles`)).find(item => item.id === vehicleId.value)
  if (vehicle) Object.assign(form, { ...vehicle, pickupTime: vehicle.pickupTime.slice(0, 16), seats: String(vehicle.seats) })
})
async function submit() {
  if (loading.value) return
  if ([form.plateNumber, form.driverName, form.pickupTime, form.pickupLocation].some(value => !String(value).trim())) return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  if (Number.isNaN(new Date(form.pickupTime).getTime())) return uni.showToast({ title: '请选择正确的上车时间', icon: 'none' })
  const seats = Number(form.seats)
  if (!Number.isInteger(seats) || seats < 1 || seats > 80) return uni.showToast({ title: '座位数须为 1–80 的整数', icon: 'none' })
  loading.value = true
  try {
    const payload = { plateNumber: form.plateNumber, driverName: form.driverName, pickupLocation: form.pickupLocation, pickupTime: apiTime(form.pickupTime), seats }
    if (vehicleId.value) await api(`/activities/${id.value}/vehicles/${vehicleId.value}`, 'PUT', payload)
    else await api(`/activities/${id.value}/vehicles`, 'POST', payload)
    uni.showToast({ title: vehicleId.value ? '已保存' : '发布成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { loading.value = false }
}
</script>
