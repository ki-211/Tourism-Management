<template>
  <view class="page">
    <view v-if="loading" class="loading-stack"><view class="skeleton-card"></view><view v-for="item in 3" :key="item" class="skeleton-card member-skeleton"></view></view>
    <template v-else-if="summary">
      <view class="page-head"><text class="eyebrow">签到统计</text><view class="page-title">{{ summary.task.title }}</view><text class="muted">{{ summary.task.description || '签到完成情况' }}</text></view>
      <view class="summary-bar">
        <view><text class="summary-number">{{ signedCount }}</text><text class="summary-label">已签到</text></view>
        <view><text class="summary-number">{{ summary.members.length - signedCount }}</text><text class="summary-label">未签到</text></view>
        <view><text class="summary-number">{{ summary.members.length }}</text><text class="summary-label">总人数</text></view>
      </view>
      <view v-for="member in summary.members" :key="member.userId" class="card member-card">
        <view class="row-between"><view class="member-name"><view class="mini-avatar">{{ member.nickname?.slice(0,1) || '友' }}</view><text>{{ member.nickname }}</text></view><text :class="member.signed ? 'status-ok' : 'status-no'">{{ member.signed ? '已签到' : '未签到' }}</text></view>
        <view v-if="member.record" class="record-info"><text class="muted">{{ displayTime(member.record.signedAt) }}</text><text v-if="member.record.address" class="muted">⌖ {{ member.record.address }}</text></view>
        <image v-if="member.record?.photoUrl" class="proof" :src="member.record.photoUrl" mode="aspectFill" @click="preview(member.record.photoUrl)" />
      </view>
    </template>
    <LoadError v-else :message="error" @retry="load" />
  </view>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { displayTime } from '@/utils/time'
const summary = ref<any>(null)
const loading = ref(true)
const error = ref('')
const taskId = ref(0)
const signedCount = computed(() => summary.value?.members.filter((member: any) => member.signed).length || 0)
async function load() {
  loading.value = true
  error.value = ''
  try { summary.value = await api(`/sign-tasks/${taskId.value}/summary`) }
  catch (reason: any) { error.value = reason?.message || '签到详情加载失败'; summary.value = null }
  finally { loading.value = false }
}
onLoad((options: any) => { taskId.value = Number(options.taskId); load() })
function preview(url: string) { uni.previewImage({ current: url, urls: [url] }) }
</script>
<style scoped lang="scss">
.summary-bar{display:flex;margin-bottom:26rpx;padding:24rpx;background:linear-gradient(135deg,$primary,#279a8f);border-radius:$radius;box-shadow:0 12rpx 28rpx rgba(15,118,110,.16)}.summary-bar>view{flex:1;text-align:center}.summary-number,.summary-label{display:block;color:#fff}.summary-number{font-size:40rpx;font-weight:800;line-height:1.2}.summary-label{margin-top:5rpx;color:rgba(255,255,255,.75);font-size:22rpx}.member-name{display:flex;align-items:center;min-width:0;gap:14rpx;font-size:28rpx;font-weight:700}.mini-avatar{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:58rpx;height:58rpx;color:$primary;font-size:24rpx;background:$primary-soft;border-radius:18rpx}.status-ok,.status-no{flex-shrink:0;font-size:23rpx;font-weight:700;white-space:nowrap}.status-ok{color:$success}.status-no{color:$text-muted}.record-info{margin-top:16rpx;padding-top:14rpx;border-top:1rpx solid $border}.proof{width:180rpx;height:180rpx;margin-top:16rpx;border-radius:16rpx}.member-skeleton{height:150rpx}
</style>
