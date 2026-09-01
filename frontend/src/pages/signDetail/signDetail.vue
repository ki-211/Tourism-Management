<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">历史记录</text><view class="page-title">签到记录</view><text class="muted">每一次准时抵达，都值得被记录。</text></view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 3" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="record in records" v-else :key="record.recordId" class="card record-card">
      <view class="record-icon">✓</view>
      <view class="record-copy"><view class="section-title">{{ record.taskTitle }}</view><text class="activity-name">{{ record.activityTitle }}</text><text class="muted">{{ displayTime(record.signedAt) }}</text></view>
      <text class="done-tag">已签到</text>
    </view>
    <view v-if="!loading && !error && !records.length" class="empty">暂无签到记录</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { displayTime } from '@/utils/time'
const records = ref<any[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try { records.value = await api('/users/me/sign-records') }
  catch (reason: any) { error.value = reason?.message || '签到记录加载失败'; records.value = [] }
  finally { loading.value = false }
}
onShow(load)
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
</script>
<style scoped lang="scss">
.record-card{display:flex;align-items:center;gap:20rpx}.record-icon{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:72rpx;height:72rpx;color:$primary;font-size:30rpx;font-weight:800;background:$primary-soft;border-radius:22rpx}.record-copy{flex:1;min-width:0}.record-copy .section-title{margin-bottom:4rpx}.activity-name{display:block;margin-bottom:5rpx;color:$text-secondary;font-size:26rpx;line-height:1.5;word-break:break-word}.done-tag{flex-shrink:0;color:$success;font-size:23rpx;font-weight:700;white-space:nowrap}
</style>
