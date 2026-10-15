<template>
  <view class="page room-page" :class="{ 'is-chat': tab === 0 && activity }">
    <view v-if="loading" class="loading-stack"><view class="skeleton-card head-skeleton"></view><view class="skeleton-card content-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="loadActivity" />
    <template v-else-if="activity">
      <view v-if="readonly" class="cancel-banner">活动已取消{{ activity.cancelReason ? `：${activity.cancelReason}` : '' }}，活动空间仅供查看</view>
      <view class="tabs"><button v-for="(item, index) in tabs" :key="item" :class="{ active: tab === index }" @click="tab = index"><text>{{ item }}</text></button></view>
      <view class="room-body">
        <ChatTab v-if="tab === 0" :activity-id="id" :refresh-key="refreshKey" :room-event="roomEvent" :readonly="readonly" />
        <SignTab v-if="tab === 1" :activity-id="id" :creator="activity.creator" :refresh-key="refreshKey" :room-event="roomEvent" :readonly="readonly" />
        <AlbumTab v-if="tab === 2" :activity-id="id" :refresh-key="refreshKey" :room-event="roomEvent" :readonly="readonly" />
        <LocationTab v-if="tab === 3" :activity-id="id" :activity-end-time="activity.endTime" :refresh-key="refreshKey" :room-event="roomEvent" :readonly="readonly" />
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onUnload, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { ActivitySocket } from '@/services/realtime'
import type { Activity } from '@/services/types'
import { useLocationSharingStore } from '@/stores/locationSharing'
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
const roomEvent = ref<any>(null)
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
      socket.on(event => {
        if (event?.type === 'ACCESS_REVOKED') {
          socket = null
          uni.showToast({ title: '已无权访问该活动空间', icon: 'none' })
          uni.navigateBack()
        } else if (event?.type === 'SOCKET_RECONNECTED') refreshKey.value++
        else if (event?.type === 'CREATOR_TRANSFERRED' || event?.type === 'ACTIVITY_UPDATED') refreshActivity()
        else if (event?.type === 'ACTIVITY_CANCELLED') onCancelled()
        // Tabs apply events incrementally; only reconnects, pull-down and returning to the page revalidate.
        else roomEvent.value = event
      })
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

const readonly = computed(() => activity.value?.status === 'CANCELLED')
const sharingStore = useLocationSharingStore()

async function onCancelled() {
  await refreshActivity()
  if (sharingStore.activityId === id.value && sharingStore.sharing) await sharingStore.stop(true).catch(() => undefined)
  refreshKey.value++
}

async function refreshActivity() {
  try {
    const loaded = await api<Activity>(`/activities/${id.value}`, 'GET', undefined, { silent: true })
    if (loaded.joined) activity.value = loaded
  } catch { return }
}

let shown = false
onLoad((options: any) => { id.value = Number(options.id); loadActivity() })
// The first onShow follows onLoad, whose tabs already load; later ones silently revalidate the active tab.
onShow(() => { if (shown) refreshKey.value++; shown = true })
onPullDownRefresh(async () => {
  if (error.value) await loadActivity()
  else refreshKey.value++
  uni.stopPullDownRefresh()
})
onUnload(() => socket?.close())
</script>
<style scoped lang="scss">
.room-page{padding-top:20rpx}
.cancel-banner{flex-shrink:0;margin-bottom:20rpx;padding:18rpx 22rpx;color:$danger;font-size:24rpx;line-height:1.5;background:#fff0f0;border-radius:16rpx}
.room-page.is-chat{
  display:flex;
  flex-direction:column;
  height:calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  padding-bottom:0;
  overflow:hidden;
}
/* #ifdef H5 */
.room-page.is-chat{height:calc(100vh - 44px);min-height:calc(100vh - 44px);}
/* #endif */
.tabs{position:sticky;top:0;z-index:5;display:flex;flex-shrink:0;width:100%;margin-bottom:28rpx;padding:7rpx;background:#e4ecea;border-radius:20rpx;gap:4rpx}
.tabs button{display:flex;align-items:center;justify-content:center;flex:1;min-width:0;height:72rpx;margin:0;padding:0 8rpx;color:$text-muted;font-size:25rpx;font-weight:650;line-height:1;white-space:nowrap;background:transparent;border-radius:15rpx}
.tabs .active{color:#fff;background:$primary;box-shadow:0 6rpx 16rpx rgba(15,118,110,.18)}
.room-body{display:flex;flex:1;flex-direction:column;min-height:0}
.head-skeleton{height:86rpx}.content-skeleton{height:58vh}
</style>
