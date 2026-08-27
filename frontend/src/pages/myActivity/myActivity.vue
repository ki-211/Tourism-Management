<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">MY JOURNEYS</text><view class="page-title">我的活动</view></view>
    <view class="tabs">
      <button :class="{ active: scope === 'joined' }" @click="change('joined')"><text>我参与的</text></button>
      <button :class="{ active: scope === 'created' }" @click="change('created')"><text>我负责的</text></button>
    </view>
    <ActivityCard v-for="item in list" :key="item.id" :activity="item" @open="open" />
    <view v-if="!list.length" class="empty">{{ scope === 'joined' ? '还没有参与活动' : '还没有负责的活动' }}</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import ActivityCard from '@/components/ActivityCard.vue'
import { api } from '@/services/api'
import { requireSession } from '@/services/session'
import type { Activity, PageData } from '@/services/types'
const scope = ref('joined')
const list = ref<Activity[]>([])
async function load() {
  if (!requireSession()) return
  list.value = (await api<PageData<Activity>>(`/activities?scope=${scope.value}&size=50`)).items
}
onShow(load)
async function change(value: string) { scope.value = value; await load() }
const open = (id: number) => uni.navigateTo({ url: `/pages/activityDetail/activityDetail?id=${id}` })
</script>
<style scoped lang="scss">
.tabs { display: flex; width: 100%; margin-bottom: 28rpx; padding: 7rpx; background: #e7eeec; border-radius: 20rpx; gap: 6rpx; }
.tabs button { display: flex; align-items: center; justify-content: center; flex: 1; min-width: 0; height: 74rpx; margin: 0; padding: 0 16rpx; color: $text-muted; font-size: 26rpx; font-weight: 650; line-height: 1; white-space: nowrap; background: transparent; border-radius: 15rpx; }
.tabs button.active { color: $primary-dark; background: #fff; box-shadow: 0 6rpx 16rpx rgba(29,65,59,.08); }
</style>
