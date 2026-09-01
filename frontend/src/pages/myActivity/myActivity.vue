<template>
  <view class="page with-desktop-nav">
    <DesktopNav active="journey" />
    <view class="page-head"><text class="eyebrow">行程</text><view class="page-title">我的活动</view><text class="muted">查看你参与和负责的活动</text></view>
    <view class="tabs">
      <button :class="{ active: scope === 'joined' }" @click="change('joined')"><text>我参与的</text></button>
      <button :class="{ active: scope === 'created' }" @click="change('created')"><text>我负责的</text></button>
    </view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 2" :key="item" class="skeleton-card activity-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-else class="activity-grid"><ActivityCard v-for="item in list" :key="item.id" :activity="item" @open="open" /></view>
    <view v-if="!loading && !error && !list.length" class="empty">{{ scope === 'joined' ? '还没有参与活动' : '还没有负责的活动' }}</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ActivityCard from '@/components/ActivityCard.vue'
import DesktopNav from '@/components/DesktopNav.vue'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { requireSession } from '@/services/session'
import type { Activity, PageData } from '@/services/types'
const scope = ref('joined')
const list = ref<Activity[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  if (!requireSession()) return
  loading.value = true
  error.value = ''
  try { list.value = (await api<PageData<Activity>>(`/activities?scope=${scope.value}&size=50`)).items }
  catch (reason: any) { error.value = reason?.message || '活动列表加载失败'; list.value = [] }
  finally { loading.value = false }
}
onShow(load)
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
async function change(value: string) { scope.value = value; await load() }
const open = (id: number) => uni.navigateTo({ url: `/pages/activityDetail/activityDetail?id=${id}` })
</script>
<style scoped lang="scss">
.tabs { display: flex; width: 100%; margin-bottom: 28rpx; padding: 7rpx; background: #e7eeec; border-radius: 20rpx; gap: 6rpx; }
.tabs button { display: flex; align-items: center; justify-content: center; flex: 1; min-width: 0; height: 74rpx; margin: 0; padding: 0 16rpx; color: $text-muted; font-size: 26rpx; font-weight: 650; line-height: 1; white-space: nowrap; background: transparent; border-radius: 15rpx; }
.tabs button.active { color: $primary-dark; background: #fff; box-shadow: 0 6rpx 16rpx rgba(29,65,59,.08); }
.activity-skeleton { height: 500rpx; }
/* #ifdef H5 */
@media (min-width: 900px) { .activity-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; } }
/* #endif */
</style>
