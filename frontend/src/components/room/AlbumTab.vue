<template>
  <view>
    <view class="tab-head row-between"><view><view class="section-title">活动相册</view><text class="muted">共享活动中的精彩瞬间</text></view><button class="mini-primary" :loading="uploading" :disabled="uploading" @click="choose">＋ 上传</button></view>
    <view v-if="uploading" class="upload-progress"><view class="progress-copy"><text>正在压缩并上传</text><text>{{ progress }}%</text></view><view class="progress-track"><view :style="{ width: `${progress}%` }"></view></view></view>
    <view v-if="loading" class="photo-grid"><view v-for="item in 6" :key="item" class="photo-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load()" />
    <view v-else class="photo-grid"><view v-for="(photo,index) in photos" :key="photo.id" class="photo-wrap"><image class="photo" :src="photo.url" mode="aspectFill" @click="preview(index)" /><button v-if="photo.deletable" class="photo-delete" @click.stop="remove(photo)">×</button></view></view>
    <view v-if="!loading && !error && !photos.length" class="empty">暂无照片<br>上传第一张活动照片吧</view>
  </view>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import LoadError from '@/components/LoadError.vue'
import { api, upload } from '@/services/api'
import type { Photo } from '@/services/types'
import { reuseSignedUrls } from '@/utils/photos'
// Private photo URLs are signed for 5 minutes; reuse them for 4 so <image> keeps its cached copy.
const URL_REUSE_MS = 4 * 60 * 1000
const props = defineProps<{ activityId: number; refreshKey: number; roomEvent?: any }>()
const photos = ref<Photo[]>([])
const loading = ref(false)
const uploading = ref(false)
const progress = ref(0)
const error = ref('')
let urlsSignedAt = 0
async function load(showLoading = true) {
  if (showLoading) loading.value = true
  error.value = ''
  try {
    const fresh = await api<Photo[]>(`/activities/${props.activityId}/photos`)
    if (Date.now() - urlsSignedAt < URL_REUSE_MS) photos.value = reuseSignedUrls(photos.value, fresh)
    else {
      photos.value = fresh
      urlsSignedAt = Date.now()
    }
  }
  catch (reason: any) { error.value = reason?.message || '相册加载失败'; photos.value = [] }
  finally { loading.value = false }
}
watch(() => props.refreshKey, (_, previous) => load(previous === undefined), { immediate: true })
watch(() => props.roomEvent, event => {
  if (event?.type === 'PHOTO_ADDED') load(false)
  else if (event?.type === 'PHOTO_REMOVED') photos.value = photos.value.filter(photo => photo.id !== Number(event.payload?.photoId))
})
function choose() {
  if (uploading.value) return
  uni.showToast({ title: '图片将自动压缩后上传', icon: 'none' })
  uni.chooseImage({ count: 1, sizeType: ['compressed'], success: async result => {
    uploading.value = true
    progress.value = 0
    try { await upload(`/activities/${props.activityId}/photos`, result.tempFilePaths[0], {}, value => { progress.value = value }); await load(false); uni.showToast({ title: '上传成功' }) }
    finally { uploading.value = false; progress.value = 0 }
  } })
}
async function preview(index: number) {
  const id = photos.value[index].id
  // Re-sign before previewing so the full-size viewer never receives expired URLs.
  if (Date.now() - urlsSignedAt >= URL_REUSE_MS) { urlsSignedAt = 0; await load(false) }
  const current = photos.value.find(photo => photo.id === id) ?? photos.value[0]
  if (current) uni.previewImage({ current: current.url, urls: photos.value.map(photo => photo.url) })
}
function remove(photo: Photo) {
  uni.showModal({ title: '删除照片', content: '删除后无法恢复，确定继续吗？', confirmColor: '#dc4c4c', success: async result => {
    if (!result.confirm) return
    await api(`/activities/${props.activityId}/photos/${photo.id}`, 'DELETE')
    photos.value = photos.value.filter(item => item.id !== photo.id)
    uni.showToast({ title: '已删除' })
  } })
}
</script>
<style scoped lang="scss">
.tab-head{align-items:flex-end;margin-bottom:24rpx}.tab-head .section-title{margin-bottom:3rpx}.photo-grid{display:flex;flex-wrap:wrap;margin:-6rpx}.photo-wrap,.photo-skeleton{position:relative;width:calc(33.333% - 12rpx);height:210rpx;margin:6rpx}.photo{width:100%;height:100%;border-radius:16rpx}.photo-skeleton{border-radius:16rpx;background:linear-gradient(100deg,#e7efed,#f7faf9,#e7efed);background-size:300% 100%;animation:skeleton-wave 1.35s ease-in-out infinite}.photo-delete{position:absolute;top:8rpx;right:8rpx;width:48rpx;height:48rpx;margin:0;padding:0;color:#fff;font-size:34rpx;line-height:44rpx;background:rgba(23,35,33,.72);border-radius:50%}
.upload-progress{margin-bottom:22rpx;padding:20rpx;background:$primary-soft;border-radius:18rpx}.progress-copy{display:flex;justify-content:space-between;margin-bottom:12rpx;color:$primary-dark;font-size:23rpx}.progress-track{height:10rpx;overflow:hidden;background:rgba(15,118,110,.14);border-radius:999rpx}.progress-track view{height:100%;background:$primary;border-radius:999rpx;transition:width .2s}
</style>
