<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">出行安排</text><view class="page-title">发布车辆</view><text class="muted">补充司机、时间和上车点，方便大家准时集合。</text></view>
    <view class="card form-card">
      <view class="field"><text class="field-label">车牌号</text><input v-model="form.plateNumber" class="input" maxlength="20" placeholder="请输入车牌号" /></view>
      <view class="field"><text class="field-label">司机姓名</text><input v-model="form.driverName" class="input" maxlength="30" placeholder="请输入司机姓名" /></view>
      <view class="field"><text class="field-label">上车时间</text><DateTimeField v-model="form.pickupTime" /></view>
      <view class="field"><text class="field-label">上车地点</text><input v-model="form.pickupLocation" class="input" maxlength="120" placeholder="请输入集合地点" /></view>
      <button class="primary-btn" :loading="loading" :disabled="loading" @click="submit">发布车辆信息</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import DateTimeField from '@/components/DateTimeField.vue'
import { api } from '@/services/api'
import { apiTime } from '@/utils/time'
const id = ref(0)
const loading = ref(false)
const form = reactive({ plateNumber: '', driverName: '', pickupTime: '', pickupLocation: '' })
onLoad((options: any) => { id.value = Number(options.id) })
async function submit() {
  if (loading.value) return
  if (Object.values(form).some(value => !value)) return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  if (Number.isNaN(new Date(form.pickupTime).getTime())) return uni.showToast({ title: '请选择正确的上车时间', icon: 'none' })
  loading.value = true
  try {
    await api(`/activities/${id.value}/vehicles`, 'POST', { ...form, pickupTime: apiTime(form.pickupTime) })
    uni.showToast({ title: '发布成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { loading.value = false }
}
</script>
