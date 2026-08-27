<template>
  <view class="page">
    <view class="page-head">
      <text class="eyebrow">CREATE ACTIVITY</text>
      <view class="page-title">发布新活动</view>
      <text class="muted">填写完整信息，让参与者一眼了解行程安排。</text>
    </view>
    <view class="card form-card">
      <view class="field"><text class="field-label">活动名称</text><input v-model="form.title" class="input" maxlength="80" placeholder="给活动起一个好记的名字" /></view>
      <view class="field"><text class="field-label">活动地点</text><input v-model="form.location" class="input" maxlength="120" placeholder="请输入集合或活动地点" /></view>
      <view v-for="item in times" :key="item.key" class="field">
        <text class="field-label">{{ item.label }}</text>
        <DateTimeField v-model="form[item.key]" />
      </view>
      <view class="field">
        <text class="field-label">活动类型</text>
        <picker :range="['公开活动', '邀请制活动']" @change="form.visibility = Number($event.detail.value) === 0 ? 'PUBLIC' : 'INVITE_ONLY'">
          <view class="input picker-value"><text>{{ form.visibility === 'PUBLIC' ? '公开活动' : '邀请制活动' }}</text><text class="picker-arrow">⌄</text></view>
        </picker>
      </view>
      <view class="field"><text class="field-label">费用说明</text><textarea v-model="form.feeRule" class="textarea" maxlength="300" placeholder="例如：AA 制，门票自理" /></view>
      <view class="field"><text class="field-label">活动说明</text><textarea v-model="form.description" class="textarea" maxlength="2000" placeholder="介绍活动安排和注意事项" /></view>
      <view v-if="cover" class="cover-preview">
        <image :src="cover" mode="aspectFill" />
        <button class="cover-remove" @click="cover = ''">移除</button>
      </view>
      <text class="image-tip">图片会自动压缩，并居中裁剪为 16:9 封面</text>
      <button class="secondary-btn" :loading="processingCover" :disabled="loading || processingCover" @click="chooseCover">{{ processingCover ? '正在裁剪…' : cover ? '重新选择封面' : '选择活动封面（可选）' }}</button>
      <view v-if="uploadProgress > 0" class="upload-progress"><view class="progress-copy"><text>正在上传封面</text><text>{{ uploadProgress }}%</text></view><view class="progress-track"><view :style="{ width: `${uploadProgress}%` }"></view></view></view>
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
const times = [
  { key: 'signupStart', label: '报名开始' },
  { key: 'signupEnd', label: '报名结束' },
  { key: 'startTime', label: '活动开始' },
  { key: 'endTime', label: '活动结束' }
]

function chooseCover() {
  uni.showToast({ title: '将自动压缩并裁剪为 16:9', icon: 'none' })
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
function scheduleError() {
  if (times.some(item => !form[item.key])) return '请选择全部日期和时间'
  const signupStart = new Date(form.signupStart).getTime()
  const signupEnd = new Date(form.signupEnd).getTime()
  const startTime = new Date(form.startTime).getTime()
  const endTime = new Date(form.endTime).getTime()
  if ([signupStart, signupEnd, startTime, endTime].some(Number.isNaN)) return '日期时间格式不正确'
  if (signupStart >= signupEnd) return '报名结束时间必须晚于报名开始时间'
  if (signupEnd > startTime) return '报名结束时间不能晚于活动开始时间'
  if (startTime >= endTime) return '活动结束时间必须晚于活动开始时间'
  return ''
}
async function submit() {
  if (loading.value) return
  if (!form.title.trim() || !form.location.trim()) return uni.showToast({ title: '请填写活动名称和地点', icon: 'none' })
  const error = scheduleError()
  if (error) return uni.showToast({ title: error, icon: 'none' })
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
.cover-preview { position: relative; width: 100%; height: 280rpx; margin-top: 12rpx; overflow: hidden; border-radius: 20rpx; }
.cover-preview image { width: 100%; height: 100%; }
.cover-remove { position: absolute; top: 16rpx; right: 16rpx; height: 58rpx; margin: 0; padding: 0 20rpx; color: #fff; font-size: 22rpx; line-height: 58rpx; background: rgba(22,35,33,.68); border-radius: 999rpx; }
.image-tip { display: block; margin-top: 14rpx; color: $text-muted; font-size: 22rpx; line-height: 1.5; text-align: center; }
.upload-progress{margin-top:20rpx;padding:20rpx;background:$primary-soft;border-radius:18rpx}.progress-copy{display:flex;justify-content:space-between;margin-bottom:12rpx;color:$primary-dark;font-size:23rpx}.progress-track{height:10rpx;overflow:hidden;background:rgba(15,118,110,.14);border-radius:999rpx}.progress-track view{height:100%;background:$primary;border-radius:999rpx;transition:width .2s}
.crop-canvas { position: fixed; left: -2000px; top: -2000px; width: 1200px; height: 675px; opacity: 0; pointer-events: none; }
</style>
