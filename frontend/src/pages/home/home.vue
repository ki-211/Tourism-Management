<template>
  <view class="page home-page with-desktop-nav">
    <DesktopNav active="home" />
    <view class="hero-head">
      <view class="hero-copy">
        <text class="eyebrow">EXPLORE TOGETHER</text>
        <view class="page-title">发现下一段同行</view>
        <text class="hero-sub">从报名到出发，把每一次相聚安排得从容有序。</text>
      </view>
      <view class="sun"><text>↗</text></view>
    </view>
    <view class="quick-actions">
      <button class="quick-item" @click="invite"><text class="quick-icon">⌁</text><view><text class="quick-title">邀请码</text><text class="quick-desc">加入专属活动</text></view></button>
      <button class="quick-item primary-quick" @click="create"><text class="quick-icon">＋</text><view><text class="quick-title">发布活动</text><text class="quick-desc">创建新的旅程</text></view></button>
    </view>
    <view class="list-head row-between"><view><text class="section-title">精选活动</text><text class="muted">看看大家最近都在计划什么</text></view><text v-if="list.length" class="count">{{ list.length }} 个</text></view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 3" :key="item" class="skeleton-card home-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-else class="activity-grid"><ActivityCard v-for="item in list" :key="item.id" :activity="item" @open="open" /></view>
    <view v-if="!loading && !error && !list.length" class="empty">暂时还没有公开活动<br>去发布第一场活动吧</view>
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

const list = ref<Activity[]>([])
const loading = ref(false)
const error = ref('')
async function load() {
  if (!requireSession()) return
  loading.value = true
  error.value = ''
  try { list.value = (await api<PageData<Activity>>('/activities?scope=discover&size=50')).items }
  catch (reason: any) { error.value = reason?.message || '活动列表加载失败'; list.value = [] }
  finally { loading.value = false }
}
onShow(load)
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
const open = (id: number) => uni.navigateTo({ url: `/pages/activityDetail/activityDetail?id=${id}` })
const create = () => uni.navigateTo({ url: '/pages/activityCreate/activityCreate' })
const invite = () => uni.navigateTo({ url: '/pages/invite/invite' })
</script>

<style scoped lang="scss">
.home-page { padding-top: 22rpx; }
.hero-head { position: relative; min-height: 250rpx; margin: 0 -28rpx 30rpx; padding: 38rpx 44rpx; overflow: hidden; color: #fff; background: linear-gradient(135deg, #0b5f59 0%, #15958a 100%); }
.hero-head::after { position: absolute; right: -80rpx; bottom: -130rpx; width: 330rpx; height: 330rpx; content: ""; border: 44rpx solid rgba(255, 255, 255, .08); border-radius: 50%; }
.hero-copy { position: relative; z-index: 1; max-width: 540rpx; }
.hero-head .eyebrow { color: rgba(255,255,255,.72); }
.hero-head .page-title { color: #fff; font-size: 46rpx; }
.hero-sub { display: block; margin-top: 16rpx; color: rgba(255,255,255,.82); font-size: 25rpx; line-height: 1.65; }
.sun { position: absolute; z-index: 1; right: 40rpx; top: 38rpx; display: flex; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; color: $primary-dark; font-size: 34rpx; background: $accent; border-radius: 50%; }
.quick-actions { display: flex; width: 100%; margin-bottom: 38rpx; gap: 18rpx; }
.quick-item { display: flex; align-items: center; flex: 1; min-width: 0; min-height: 116rpx; margin: 0; padding: 20rpx; color: $text-main; text-align: left; background: #fff; border: 1rpx solid $border; border-radius: 22rpx; box-shadow: 0 10rpx 26rpx rgba(29,65,59,.06); gap: 14rpx; }
.quick-item::after { border: 0; }
.quick-item view { min-width: 0; }
.quick-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 58rpx; height: 58rpx; color: $primary; font-size: 34rpx; font-weight: 500; background: $primary-soft; border-radius: 16rpx; }
.quick-title, .quick-desc { display: block; line-height: 1.4; }
.quick-title { font-size: 27rpx; font-weight: 700; white-space: nowrap; }
.quick-desc { margin-top: 3rpx; overflow: hidden; color: $text-muted; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.primary-quick { color: #fff; background: $primary; border-color: $primary; }
.primary-quick .quick-icon { color: $primary; background: #fff; }
.primary-quick .quick-desc { color: rgba(255,255,255,.72); }
.list-head { align-items: flex-end; margin-bottom: 20rpx; }
.list-head .section-title { display: block; margin-bottom: 4rpx; }
.count { flex-shrink: 0; color: $primary; font-size: 24rpx; }
.home-skeleton { height: 500rpx; }
/* #ifdef H5 */
@media (min-width: 900px) { .activity-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; } }
/* #endif */
</style>
