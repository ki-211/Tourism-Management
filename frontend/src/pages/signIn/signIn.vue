<template>
  <view class="page">
    <view v-if="loading" class="loading-stack"><view class="skeleton-card"></view><view class="skeleton-card form-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="loadTask" />
    <template v-else>
      <view v-if="task" class="checkin-hero">
        <view class="check-mark">✓</view>
        <view><text class="eyebrow">现在签到</text><view class="page-title">{{ task.title }}</view><text class="hero-desc">{{ task.description || '完成本次活动签到' }}</text></view>
      </view>
      <view v-if="task && rules.length" class="rule-chips"><text v-for="rule in rules" :key="rule" class="rule-chip">{{ rule }}</text></view>
      <view class="card form-card">
        <view class="section-title">签到信息</view>
        <view class="location-box" :class="{ ready: address }">
          <view class="location-icon"><AppIcon name="pin" :size="34" /></view>
          <view><text class="location-label">{{ locating ? '正在获取位置…' : address ? '当前位置' : '未获取位置' }}</text><text class="location-value">{{ address || (task?.radiusMeters ? '该签到需要定位' : '位置为可选信息，不影响签到') }}</text><text v-if="distance != null" class="distance" :class="{ far: distance > (task?.radiusMeters || 0) }">距签到点约 {{ distance }} 米（范围 {{ task?.radiusMeters }} 米）</text></view>
        </view>
        <button class="secondary-btn" :disabled="locating || signing" @click="locate">{{ locating ? '定位中…' : '重新定位' }}</button>
        <view v-if="photoPath" class="photo-preview"><image :src="photoPath" mode="aspectFill" /><button @click="photoPath = ''">移除</button></view>
        <text class="image-tip">照片会自动压缩后上传，节省流量</text>
        <button class="secondary-btn" :disabled="signing" @click="photo">{{ photoPath ? '重新选择照片' : task?.photoRequired ? '拍摄现场照片（必填）' : '选择签到照片（可选）' }}</button>
        <view class="field remark-field"><text class="field-label">签到备注（选填）</text><textarea v-model="remark" class="textarea" maxlength="200" placeholder="记录此刻想说的话" /></view>
        <view v-if="uploadProgress > 0" class="upload-progress"><view class="progress-copy"><text>正在上传签到照片</text><text>{{ uploadProgress }}%</text></view><view class="progress-track"><view :style="{ width: `${uploadProgress}%` }"></view></view></view>
        <text v-if="blocker" class="blocker">{{ blocker }}</text>
        <button class="primary-btn" :loading="signing" :disabled="signing || !!blocker" @click="submit">确认签到</button>
      </view>
    </template>
  </view>
</template>
<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api, upload } from '@/services/api'
import type { SignTask } from '@/services/types'
import { distanceToTask, signInBlocker, signRuleLabels } from '@/utils/sign'
const taskId = ref(0)
const task = ref<SignTask | null>(null)
const lat = ref<number>()
const lon = ref<number>()
const address = ref('')
const photoPath = ref('')
const remark = ref('')
const loading = ref(true)
const locating = ref(false)
const signing = ref(false)
const uploadProgress = ref(0)
const error = ref('')
const position = computed(() => lat.value != null && lon.value != null ? { latitude: lat.value, longitude: lon.value } : null)
const rules = computed(() => task.value ? signRuleLabels(task.value) : [])
const distance = computed(() => task.value ? distanceToTask(task.value, position.value) : null)
const blocker = computed(() => task.value && !locating.value ? signInBlocker(task.value, position.value, !!photoPath.value) : null)
onLoad(async (options: any) => {
  taskId.value = Number(options.taskId)
  await loadTask()
})
async function loadTask() {
  loading.value = true
  error.value = ''
  try { task.value = await api(`/sign-tasks/${taskId.value}`); locate() }
  catch (reason: any) { error.value = reason?.message || '签到任务加载失败'; task.value = null }
  finally { loading.value = false }
}
function locate() {
  if (locating.value) return
  locating.value = true
  uni.getLocation({
    type: 'gcj02',
    geocode: true,
    success: (result: any) => {
      lat.value = result.latitude
      lon.value = result.longitude
      address.value = result.address || `${result.latitude.toFixed(6)}, ${result.longitude.toFixed(6)}`
    },
    fail: () => {
      address.value = ''
      uni.showToast({ title: task.value?.radiusMeters ? '定位失败，请开启定位后重试' : '定位失败，可不带位置签到', icon: 'none' })
    },
    complete: () => { locating.value = false }
  })
}
function photo() {
  uni.showToast({ title: '照片将自动压缩后上传', icon: 'none' })
  uni.chooseImage({ count: 1, sizeType: ['compressed'], success: result => { photoPath.value = result.tempFilePaths[0] } })
}
async function submit() {
  if (signing.value) return
  signing.value = true
  try {
    const payload = { remark: remark.value, address: address.value, latitude: lat.value, longitude: lon.value }
    if (photoPath.value) {
      uploadProgress.value = 1
      const fields: Record<string, string> = { remark: remark.value, address: address.value }
      if (lat.value != null) fields.latitude = String(lat.value)
      if (lon.value != null) fields.longitude = String(lon.value)
      await upload(`/sign-tasks/${taskId.value}/records`, photoPath.value, fields, value => { uploadProgress.value = value })
    } else await api(`/sign-tasks/${taskId.value}/records`, 'POST', payload)
    uni.showToast({ title: '签到成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally { signing.value = false; uploadProgress.value = 0 }
}
</script>
<style scoped lang="scss">
.rule-chips{display:flex;flex-wrap:wrap;margin:-8rpx 0 22rpx;gap:12rpx}.rule-chip{padding:8rpx 18rpx;color:$primary-dark;font-size:22rpx;line-height:1.4;background:$primary-soft;border-radius:999rpx}
.distance{display:block;margin-top:6rpx;color:$primary;font-size:22rpx;line-height:1.4}.distance.far{color:$danger}
.blocker{display:block;margin:22rpx 0 -6rpx;color:$danger;font-size:23rpx;line-height:1.5;text-align:center}
.checkin-hero{display:flex;align-items:center;margin:8rpx 0 28rpx;padding:28rpx 10rpx;gap:22rpx}.check-mark{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:94rpx;height:94rpx;color:#fff;font-size:42rpx;font-weight:800;background:linear-gradient(145deg,$primary,#3ca69b);border-radius:30rpx 30rpx 30rpx 10rpx;box-shadow:0 12rpx 28rpx rgba(15,118,110,.18)}.checkin-hero>view{min-width:0}.hero-desc{display:block;margin-top:7rpx;color:$text-muted;font-size:25rpx;line-height:1.5}.location-box{display:flex;align-items:center;margin:22rpx 0 4rpx;padding:22rpx;color:$text-muted;background:#f7faf9;border:1rpx solid $border;border-radius:18rpx;gap:18rpx}.location-box.ready{color:$text-secondary;background:$primary-soft;border-color:transparent}.location-icon{flex-shrink:0;color:$primary;font-size:36rpx}.location-box>view{min-width:0}.location-label,.location-value{display:block}.location-label{font-size:24rpx;font-weight:700}.location-value{margin-top:4rpx;font-size:23rpx;line-height:1.5;word-break:break-word}.photo-preview{position:relative;width:100%;height:300rpx;margin-top:22rpx;overflow:hidden;border-radius:20rpx}.photo-preview image{width:100%;height:100%}.photo-preview button{position:absolute;right:16rpx;top:16rpx;height:58rpx;margin:0;padding:0 20rpx;color:#fff;font-size:22rpx;line-height:58rpx;background:rgba(22,35,33,.68);border-radius:999rpx}.remark-field{margin-top:26rpx}.form-skeleton{height:560rpx}
.image-tip{display:block;margin-top:16rpx;color:$text-muted;font-size:22rpx;line-height:1.5;text-align:center}.upload-progress{margin-top:20rpx;padding:20rpx;background:$primary-soft;border-radius:18rpx}.progress-copy{display:flex;justify-content:space-between;margin-bottom:12rpx;color:$primary-dark;font-size:23rpx}.progress-track{height:10rpx;overflow:hidden;background:rgba(15,118,110,.14);border-radius:999rpx}.progress-track view{height:100%;background:$primary;border-radius:999rpx;transition:width .2s}
</style>
