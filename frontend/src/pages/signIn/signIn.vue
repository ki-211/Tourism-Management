<template><view class="page"><view v-if="task" class="card"><view class="page-title">{{ task.title }}</view><view class="muted">{{ task.description||'无说明' }}</view></view><view class="card"><view class="section-title">签到信息</view><view class="muted">📍 {{ address||'未获取位置（不影响签到）' }}</view><button class="secondary-btn" @click="locate">重新定位</button><button class="secondary-btn" @click="photo">{{ photoPath?'已选择照片':'选择签到照片（可选）' }}</button><textarea v-model="remark" class="textarea" maxlength="200" placeholder="签到备注（可选）" /><button class="primary-btn" :loading="signing" @click="submit">确认签到</button></view></view></template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, upload } from '@/services/api'
import type { SignTask } from '@/services/types'

const taskId = ref(0), task = ref<SignTask | null>(null)
const lat = ref<number>(), lon = ref<number>(), address = ref(''), photoPath = ref(''), remark = ref(''), signing = ref(false)
onLoad(async (options: any) => { taskId.value = Number(options.taskId); task.value = await api(`/sign-tasks/${taskId.value}`); locate() })
function locate() { uni.getLocation({ type: 'gcj02', geocode: true, success: (result: any) => { lat.value = result.latitude; lon.value = result.longitude; address.value = result.address || `${result.latitude.toFixed(6)}, ${result.longitude.toFixed(6)}` }, fail: () => address.value = '' }) }
function photo() { uni.chooseImage({ count: 1, sizeType: ['compressed'], success: result => photoPath.value = result.tempFilePaths[0] }) }
async function submit() {
  signing.value = true
  try {
    const payload = { remark: remark.value, address: address.value, latitude: lat.value, longitude: lon.value }
    if (photoPath.value) {
      const fields: Record<string, string> = { remark: remark.value, address: address.value }
      if (lat.value != null) fields.latitude = String(lat.value)
      if (lon.value != null) fields.longitude = String(lon.value)
      await upload(`/sign-tasks/${taskId.value}/records`, photoPath.value, fields)
    } else {
      await api(`/sign-tasks/${taskId.value}/records`, 'POST', payload)
    }
    uni.showToast({ title: '签到成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { signing.value = false }
}
</script>
