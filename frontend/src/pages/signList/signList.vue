<template>
  <view class="page with-desktop-nav">
    <DesktopNav active="sign" />
    <view class="page-head row-between">
      <view><text class="eyebrow">CHECK IN</text><view class="page-title">待签到</view><text class="muted">别错过正在进行的签到任务</text></view>
      <button class="ghost-btn" @click="history">签到记录</button>
    </view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 3" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="task in tasks" v-else :key="task.id" class="card task-card" @click="sign(task.id)">
      <view class="row-between task-head"><view class="task-icon">✓</view><text class="tag">待完成</text></view>
      <view class="section-title">{{ task.title }}</view>
      <text class="body-text">{{ task.description || '发起人未填写额外说明' }}</text>
      <view class="task-footer"><text class="muted">发布于 {{ displayTime(task.createdAt) }}</text><text class="go">去签到 ›</text></view>
    </view>
    <view v-if="!loading && !error && !tasks.length" class="empty">暂无待签到任务</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import DesktopNav from '@/components/DesktopNav.vue'
import { api } from '@/services/api'
import { requireSession } from '@/services/session'
import { displayTime } from '@/utils/time'
import type { SignTask } from '@/services/types'
const tasks = ref<SignTask[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  if (!requireSession()) return
  loading.value = true
  error.value = ''
  try { tasks.value = await api('/sign-tasks/unsigned') }
  catch (reason: any) { error.value = reason?.message || '签到任务加载失败'; tasks.value = [] }
  finally { loading.value = false }
}
onShow(load)
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
const sign = (id: number) => uni.navigateTo({ url: `/pages/signIn/signIn?taskId=${id}` })
const history = () => uni.navigateTo({ url: '/pages/signDetail/signDetail' })
</script>
<style scoped lang="scss">
.page-head{align-items:flex-end}.task-card{position:relative}.task-head{margin-bottom:18rpx}.task-icon{display:flex;align-items:center;justify-content:center;width:64rpx;height:64rpx;color:#fff;font-size:30rpx;font-weight:800;background:linear-gradient(145deg,$primary,#35a298);border-radius:20rpx}.task-card .section-title{margin-bottom:10rpx}.task-footer{display:flex;align-items:center;justify-content:space-between;margin-top:24rpx;padding-top:20rpx;border-top:1rpx solid $border;gap:16rpx}.go{flex-shrink:0;color:$primary;font-size:25rpx;font-weight:700;white-space:nowrap}
</style>
