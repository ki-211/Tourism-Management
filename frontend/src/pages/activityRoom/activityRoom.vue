<template>
  <view class="page room-page">
    <view v-if="loading" class="loading-stack"><view class="skeleton-card head-skeleton"></view><view class="skeleton-card content-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="loadActivity" />
    <template v-else-if="activity">
      <view class="tabs"><button v-for="(item, index) in tabs" :key="item" :class="{ active: tab === index }" @click="tab = index"><text>{{ item }}</text></button></view>
      <ChatTab v-if="tab === 0" :activity-id="id" :refresh-key="refreshKey" />
      <SignTab v-if="tab === 1" :activity-id="id" :creator="activity.creator" :refresh-key="refreshKey" />
      <AlbumTab v-if="tab === 2" :activity-id="id" :refresh-key="refreshKey" />
      <LocationTab v-if="tab === 3" :activity-id="id" :refresh-key="refreshKey" />
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onUnload, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { ActivitySocket } from '@/services/realtime'
import type { Activity } from '@/services/types'
import ChatTab from '@/components/room/ChatTab.vue'
import SignTab from '@/components/room/SignTab.vue'
import AlbumTab from '@/components/room/AlbumTab.vue'
import LocationTab from '@/components/room/LocationTab.vue'

const id = ref(0)
const activity = ref<Activity | null>(null)
const tab = ref(0)
const tabs = ['聊天', '签到', '相册', '位置']
const refreshKey = ref(0)
const loading = ref(true)
const error = ref('')
let socket: ActivitySocket | null = null

async function loadActivity() {
  loading.value = true
  error.value = ''
  try {
    const loaded = await api<Activity>(`/activities/${id.value}`)
    activity.value = loaded
    if (!loaded.joined) {
      uni.showToast({ title: '请先报名活动', icon: 'none' })
      uni.navigateBack()
      return
    }
    if (!socket) {
      socket = new ActivitySocket(id.value)
      socket.on(() => refreshKey.value++)
      await socket.connect()
    }
  } catch (reason: any) {
    socket?.close()
    socket = null
    error.value = reason?.message || '活动空间加载失败'
    activity.value = null
  } finally {
    loading.value = false
  }
}

onLoad((options: any) => { id.value = Number(options.id); loadActivity() })
onShow(() => refreshKey.value++)
onPullDownRefresh(async () => {
  if (error.value) await loadActivity()
  else refreshKey.value++
  uni.stopPullDownRefresh()
})
onUnload(() => socket?.close())
</script>
<style scoped lang="scss">
.room-page{padding-top:20rpx}.tabs{position:sticky;top:0;z-index:5;display:flex;width:100%;margin-bottom:28rpx;padding:7rpx;background:#e4ecea;border-radius:20rpx;gap:4rpx}.tabs button{display:flex;align-items:center;justify-content:center;flex:1;min-width:0;height:72rpx;margin:0;padding:0 8rpx;color:$text-muted;font-size:25rpx;font-weight:650;line-height:1;white-space:nowrap;background:transparent;border-radius:15rpx}.tabs .active{color:#fff;background:$primary;box-shadow:0 6rpx 16rpx rgba(15,118,110,.18)}
.head-skeleton{height:86rpx}.content-skeleton{height:58vh}
</style>
