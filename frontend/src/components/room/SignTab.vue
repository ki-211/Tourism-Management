<template>
  <view>
    <view class="tab-head row-between"><view><view class="section-title">签到任务</view><text class="muted">及时确认每位成员的行程状态</text></view><button v-if="creator" class="mini-primary" @click="create">＋ 发布</button></view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 2" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="task in tasks" v-else :key="task.id" class="card task-card">
      <view class="row-between"><view class="task-icon">✓</view><text :class="task.signed ? 'tag' : 'pending-tag'">{{ task.signed ? '已签到' : '待签到' }}</text></view>
      <view class="section-title">{{ task.title }}</view>
      <text class="body-text">{{ task.description || '暂无任务说明' }}</text>
      <view class="task-count">已有 <text>{{ task.signedCount }}</text> 人完成签到</view>
      <view class="task-actions">
        <button v-if="creator" class="ghost-btn" @click="detail(task.id)">查看详情</button>
        <button v-if="!task.signed" class="mini-primary" @click="sign(task.id)">立即签到</button>
      </view>
    </view>
    <view v-if="!loading && !error && !tasks.length" class="empty">暂无签到任务</view>
  </view>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import type { SignTask } from '@/services/types'
const props = defineProps<{ activityId: number; creator: boolean; refreshKey: number }>()
const tasks = ref<SignTask[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try { tasks.value = await api(`/activities/${props.activityId}/sign-tasks`) }
  catch (reason: any) { error.value = reason?.message || '签到任务加载失败'; tasks.value = [] }
  finally { loading.value = false }
}
watch(() => props.refreshKey, load, { immediate: true })
const create = () => uni.navigateTo({ url: `/pages/signTask/signTask?id=${props.activityId}` })
const detail = (id: number) => uni.navigateTo({ url: `/pages/signTaskDetail/signTaskDetail?taskId=${id}` })
const sign = (id: number) => uni.navigateTo({ url: `/pages/signIn/signIn?taskId=${id}` })
</script>
<style scoped lang="scss">
.tab-head{align-items:flex-end;margin-bottom:24rpx}.tab-head .section-title{margin-bottom:3rpx}.task-card .section-title{margin-top:18rpx;margin-bottom:8rpx}.task-icon{display:flex;align-items:center;justify-content:center;width:60rpx;height:60rpx;color:#fff;font-size:28rpx;font-weight:800;background:$primary;border-radius:18rpx}.pending-tag{flex-shrink:0;padding:7rpx 16rpx;color:$warning;font-size:22rpx;font-weight:700;line-height:1.3;white-space:nowrap;background:$accent-soft;border-radius:999rpx}.task-count{margin-top:20rpx;color:$text-muted;font-size:24rpx}.task-count text{color:$primary;font-size:30rpx;font-weight:800}.task-actions{display:flex;justify-content:flex-end;margin-top:20rpx;padding-top:18rpx;border-top:1rpx solid $border;gap:12rpx}.task-actions button{margin:0}
</style>
