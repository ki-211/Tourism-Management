<template>
  <view class="page">
    <view class="page-head">
      <text class="eyebrow">发布活动</text>
      <view class="page-title">发布新活动</view>
      <text class="muted">填写完整信息，让参与者一眼了解行程安排。</text>
    </view>
    <view class="card form-card">
      <view class="form-section">
        <text class="form-section-title">基本信息</text>
        <text class="form-section-desc">名称和地点会显示在活动卡片上</text>
        <view class="field" :class="{ 'has-error': errors.title }">
          <text class="field-label">活动名称</text>
          <input v-model="form.title" class="input" maxlength="80" placeholder="给活动起一个好记的名字" @input="clearError('title')" />
          <text v-if="errors.title" class="field-error">{{ errors.title }}</text>
        </view>
        <view class="field" :class="{ 'has-error': errors.location }">
          <text class="field-label">活动地点</text>
          <input v-model="form.location" class="input" maxlength="120" placeholder="请输入集合或活动地点" @input="clearError('location')" />
          <text v-if="errors.location" class="field-error">{{ errors.location }}</text>
        </view>
        <view class="field">
          <text class="field-label">活动类型</text>
          <picker :range="['公开活动', '邀请制活动']" @change="form.visibility = Number($event.detail.value) === 0 ? 'PUBLIC' : 'INVITE_ONLY'">
            <view class="input picker-value"><text>{{ form.visibility === 'PUBLIC' ? '公开活动' : '邀请制活动' }}</text><text class="picker-arrow">⌄</text></view>
          </picker>
        </view>
      </view>

      <view class="form-section">
        <text class="form-section-title">报名时间</text>
        <text class="form-section-desc">参与者可以提交报名的时间段</text>
        <view class="field" :class="{ 'has-error': errors.signupStart }">
          <text class="field-label">报名开始</text>
          <DateTimeField v-model="form.signupStart" @update:model-value="clearError('signupStart')" />
          <text v-if="errors.signupStart" class="field-error">{{ errors.signupStart }}</text>
        </view>
        <view class="field" :class="{ 'has-error': errors.signupEnd }">
          <text class="field-label">报名结束</text>
          <DateTimeField v-model="form.signupEnd" @update:model-value="clearError('signupEnd')" />
          <text v-if="errors.signupEnd" class="field-error">{{ errors.signupEnd }}</text>
        </view>
      </view>

      <view class="form-section">
        <text class="form-section-title">活动时间</text>
        <text class="form-section-desc">须晚于报名结束时间</text>
        <view class="field" :class="{ 'has-error': errors.startTime }">
          <text class="field-label">活动开始</text>
          <DateTimeField v-model="form.startTime" @update:model-value="clearError('startTime')" />
          <text v-if="errors.startTime" class="field-error">{{ errors.startTime }}</text>
        </view>
        <view class="field" :class="{ 'has-error': errors.endTime }">
          <text class="field-label">活动结束</text>
          <DateTimeField v-model="form.endTime" @update:model-value="clearError('endTime')" />
          <text v-if="errors.endTime" class="field-error">{{ errors.endTime }}</text>
        </view>
      </view>

      <view class="form-section">
        <text class="form-section-title">补充说明</text>
        <text class="form-section-desc">费用、介绍和封面均可稍后完善</text>
        <view class="field"><text class="field-label">费用说明</text><textarea v-model="form.feeRule" class="textarea" maxlength="300" placeholder="例如：AA 制，门票自理" /></view>
        <view class="field"><text class="field-label">活动说明</text><textarea v-model="form.description" class="textarea" maxlength="2000" placeholder="介绍活动安排和注意事项" /></view>
        <view v-if="cover" class="cover-preview">
          <image :src="cover" mode="aspectFill" />
          <button class="cover-remove" @click="cover = ''">移除</button>
        </view>
        <text class="image-tip">图片会自动压缩，并居中裁剪为 16:9 封面</text>
        <button class="secondary-btn" :loading="processingCover" :disabled="loading || processingCover" @click="chooseCover">{{ processingCover ? '正在裁剪…' : cover ? '重新选择封面' : '选择活动封面（可选）' }}</button>
        <view v-if="uploadProgress > 0" class="upload-progress"><view class="progress-copy"><text>正在上传封面</text><text>{{ uploadProgress }}%</text></view><view class="progress-track"><view :style="{ width: `${uploadProgress}%` }"></view></view></view>
      </view>

      <button class="primary-btn" :loading="loading" :disabled="loading" @click="submit">确认发布</button>
    </view>
    <canvas id="coverCropCanvas" canvas-id="coverCropCanvas" class="crop-canvas"></canvas>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import DateTimeField from '@/components/DateTimeField.vue'
import { api, upload } from '@/services/api'
import { apiTime } from '@/utils/time'

const loading = ref(false)
const cover = ref('')
const processingCover = ref(false)
const uploadProgress = ref(0)
const form = reactive<any>({ title: '', location: '', startTime: '', endTime: '', signupStart: '', signupEnd: '', visibility: 'PUBLIC', feeRule: '', description: '' })
const errors = reactive<Record<string, string>>({})
const times = [
  { key: 'signupStart', label: '报名开始' },
  { key: 'signupEnd', label: '报名结束' },
  { key: 'startTime', label: '活动开始' },
  { key: 'endTime', label: '活动结束' }
]

function clearError(key: string) {
  errors[key] = ''
}

function resetErrors() {
  Object.keys(errors).forEach(key => { errors[key] = '' })
}

function chooseCover() {
  uni.chooseImage({ count: 1, sizeType: ['compressed'], success: async result => {
    processingCover.value = true
    try { cover.value = await cropCover(result.tempFilePaths[0]) }
    finally { processingCover.value = false }
  } })
}
function cropCover(filePath: string): Promise<string> {
  return new Promise(resolve => {
    uni.getImageInfo({ src: filePath, success: info => {
      const sourceWidth = Number(info.width)
      const sourceHeight = Number(info.height)
      const targetRatio = 16 / 9
      let sourceX = 0
      let sourceY = 0
      let cropWidth = sourceWidth
      let cropHeight = sourceHeight
      if (sourceWidth / sourceHeight > targetRatio) {
        cropWidth = sourceHeight * targetRatio
        sourceX = (sourceWidth - cropWidth) / 2
      } else {
        cropHeight = sourceWidth / targetRatio
        sourceY = (sourceHeight - cropHeight) / 2
      }
      const context = uni.createCanvasContext('coverCropCanvas')
      context.clearRect(0, 0, 1200, 675)
      context.drawImage(filePath, sourceX, sourceY, cropWidth, cropHeight, 0, 0, 1200, 675)
      context.draw(false, () => {
        uni.canvasToTempFilePath({ canvasId: 'coverCropCanvas', x: 0, y: 0, width: 1200, height: 675, destWidth: 1200, destHeight: 675, fileType: 'jpg', quality: 0.86,
          success: result => resolve(result.tempFilePath), fail: () => { uni.showToast({ title: '裁剪失败，已保留原图', icon: 'none' }); resolve(filePath) } })
      })
    }, fail: () => resolve(filePath) })
  })
}

function validate() {
  resetErrors()
  if (!form.title.trim()) errors.title = '请填写活动名称'
  if (!form.location.trim()) errors.location = '请填写活动地点'
  times.forEach(item => {
    if (!form[item.key]) errors[item.key] = `请选择${item.label}时间`
    else if (Number.isNaN(new Date(form[item.key]).getTime())) errors[item.key] = `${item.label}时间格式不正确`
  })
  if (times.every(item => form[item.key] && !Number.isNaN(new Date(form[item.key]).getTime()))) {
    const signupStart = new Date(form.signupStart).getTime()
    const signupEnd = new Date(form.signupEnd).getTime()
    const startTime = new Date(form.startTime).getTime()
    const endTime = new Date(form.endTime).getTime()
    if (signupStart >= signupEnd) errors.signupEnd = '报名结束时间必须晚于报名开始时间'
    else if (signupEnd > startTime) errors.startTime = '活动开始不能早于报名结束'
    if (startTime >= endTime) errors.endTime = '活动结束时间必须晚于活动开始时间'
  }
  return !Object.values(errors).some(Boolean)
}

async function submit() {
  if (loading.value) return
  if (!validate()) {
    const first = Object.values(errors).find(Boolean)
    if (first) uni.showToast({ title: first, icon: 'none' })
    return
  }
  loading.value = true
  try {
    const payload = { ...form }
    times.forEach(item => { payload[item.key] = apiTime(payload[item.key]) })
    const activity = await api<any>('/activities', 'POST', payload)
    if (cover.value) {
      uploadProgress.value = 1
      try { await upload(`/activities/${activity.id}/cover`, cover.value, {}, value => { uploadProgress.value = value }) }
      catch { uni.showToast({ title: '活动已创建，封面可稍后补传', icon: 'none' }) }
    }
    uni.showToast({ title: '发布成功' })
    setTimeout(() => uni.redirectTo({ url: `/pages/activityDetail/activityDetail?id=${activity.id}` }), 500)
  } finally { loading.value = false; uploadProgress.value = 0 }
}
</script>

<style scoped lang="scss">
.picker-value { display: flex; align-items: center; justify-content: space-between; }
.picker-arrow { flex-shrink: 0; color: $primary; font-size: 26rpx; }
.field.has-error :deep(.picker-box) { background: #fff8f8; border-color: rgba(220, 76, 76, .5); }
.cover-preview { position: relative; width: 100%; height: 280rpx; margin-top: 12rpx; overflow: hidden; border-radius: 20rpx; }
.cover-preview image { width: 100%; height: 100%; }
.cover-remove { position: absolute; top: 16rpx; right: 16rpx; height: 58rpx; margin: 0; padding: 0 20rpx; color: #fff; font-size: 22rpx; line-height: 58rpx; background: rgba(22,35,33,.68); border-radius: 999rpx; }
.image-tip { display: block; margin-top: 14rpx; color: $text-muted; font-size: 22rpx; line-height: 1.5; text-align: center; }
.upload-progress{margin-top:20rpx;padding:20rpx;background:$primary-soft;border-radius:18rpx}.progress-copy{display:flex;justify-content:space-between;margin-bottom:12rpx;color:$primary-dark;font-size:23rpx}.progress-track{height:10rpx;overflow:hidden;background:rgba(15,118,110,.14);border-radius:999rpx}.progress-track view{height:100%;background:$primary;border-radius:999rpx;transition:width .2s}
.crop-canvas { position: fixed; left: -2000px; top: -2000px; width: 1200px; height: 675px; opacity: 0; pointer-events: none; }
</style>
