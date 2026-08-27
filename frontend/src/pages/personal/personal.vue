<template>
  <view class="page profile-page">
    <view class="profile-hero">
      <view class="avatar">{{ user?.nickname?.slice(0, 1) || '行' }}</view>
      <view class="profile-copy"><text class="nickname">{{ user?.nickname || '旅行者' }}</text><text class="account">@{{ user?.username || '--' }}</text></view>
      <button class="edit-button" @click="edit">编辑</button>
    </view>
    <view class="motto"><text class="motto-mark">“</text><text>去见想见的风景，也珍惜一起出发的人。</text></view>
    <view class="section-label">我的服务</view>
    <view class="card menu-card">
      <view class="menu-item" @click="history"><view class="menu-icon warm">✓</view><view class="menu-copy"><text class="menu-title">签到记录</text><text class="muted">查看已完成的活动签到</text></view><text class="chevron">›</text></view>
      <view class="divider"></view>
      <view class="menu-item" @click="edit"><view class="menu-icon">✎</view><view class="menu-copy"><text class="menu-title">个人资料</text><text class="muted">修改昵称与个人信息</text></view><text class="chevron">›</text></view>
    </view>
    <button class="logout-button" @click="logout">退出当前账号</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/services/api'
import { refreshToken, requireSession } from '@/services/session'
import { useSessionStore } from '@/stores/session'
import type { User } from '@/services/types'
const session = useSessionStore()
const user = ref<User | null>(null)
onShow(async () => {
  if (!requireSession()) return
  user.value = await api('/users/me')
  uni.setStorageSync('tourism.user', user.value)
  session.reload()
})
const edit = () => uni.navigateTo({ url: '/pages/editInfo/editInfo' })
const history = () => uni.navigateTo({ url: '/pages/signDetail/signDetail' })
async function logout() {
  try { await api('/auth/logout', 'POST', { refreshToken: refreshToken() }) }
  finally { session.clear(); uni.reLaunch({ url: '/pages/login/login' }) }
}
</script>

<style scoped lang="scss">
.profile-page { padding-top: 38rpx; }
.profile-hero { display: flex; align-items: center; min-width: 0; padding: 28rpx 10rpx 34rpx; gap: 22rpx; }
.avatar { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 116rpx; height: 116rpx; color: #fff; font-size: 46rpx; font-weight: 800; background: linear-gradient(145deg, $primary, #45a99f); border: 7rpx solid #fff; border-radius: 38rpx 38rpx 38rpx 12rpx; box-shadow: 0 12rpx 30rpx rgba(15,118,110,.18); }
.profile-copy { flex: 1; min-width: 0; }
.nickname, .account { display: block; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.nickname { font-size: 38rpx; font-weight: 800; line-height: 1.35; }
.account { margin-top: 5rpx; color: $text-muted; font-size: 24rpx; line-height: 1.4; }
.edit-button { flex-shrink: 0; height: 62rpx; margin: 0; padding: 0 24rpx; color: $primary; font-size: 24rpx; line-height: 62rpx; white-space: nowrap; background: $primary-soft; border-radius: 999rpx; }
.motto { position: relative; display: flex; align-items: center; min-height: 108rpx; margin-bottom: 38rpx; padding: 20rpx 28rpx 20rpx 70rpx; overflow: hidden; color: $text-secondary; font-size: 25rpx; line-height: 1.6; background: $accent-soft; border-radius: 22rpx; }
.motto-mark { position: absolute; left: 20rpx; top: 2rpx; color: $accent; font-family: Georgia, serif; font-size: 76rpx; line-height: 1; }
.section-label { margin: 0 6rpx 16rpx; font-size: 29rpx; font-weight: 750; }
.menu-card { padding: 4rpx 28rpx; }
.menu-item { display: flex; align-items: center; min-height: 132rpx; gap: 20rpx; }
.menu-icon { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 70rpx; height: 70rpx; color: $primary; font-size: 30rpx; font-weight: 700; background: $primary-soft; border-radius: 20rpx; }
.menu-icon.warm { color: $warning; background: $accent-soft; }
.menu-copy { flex: 1; min-width: 0; }
.menu-title { display: block; margin-bottom: 4rpx; font-size: 28rpx; font-weight: 700; line-height: 1.4; }
.chevron { flex-shrink: 0; color: #9baba8; font-size: 42rpx; }
.divider { height: 1rpx; margin-left: 90rpx; background: $border; }
.logout-button { width: 100%; height: 84rpx; margin-top: 32rpx; color: $danger; font-size: 26rpx; line-height: 84rpx; background: transparent; border: 1rpx solid rgba(220,76,76,.24); border-radius: 18rpx; }
</style>
