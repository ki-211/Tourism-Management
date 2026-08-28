<template>
  <view class="page auth">
    <view class="brand-block">
      <view class="logo-mark"><text>行</text></view>
      <text class="eyebrow">ACTIVITY HUB</text>
      <view class="brand">协作集</view>
      <text class="slogan">让每一场活动，都有人协作、有序完成</text>
    </view>
    <view class="card auth-card">
      <view class="section-title">欢迎回来</view>
      <text class="muted form-intro">登录后继续管理你的活动与协作</text>
      <view class="field"><text class="field-label">用户名</text><input v-model="form.username" class="input" placeholder="请输入用户名" /></view>
      <view class="field"><text class="field-label">密码</text><input v-model="form.password" class="input" password placeholder="请输入 8-64 位密码" /></view>
      <button class="primary-btn" :loading="loading" @click="login">登录</button>
      <button class="secondary-btn" @click="goRegister">创建新账号</button>
    </view>
    <text class="auth-note">一起计划 · 随时沟通 · 安心签到</text>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/services/api'
import { hasSession } from '@/services/session'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
onShow(() => { if (hasSession()) uni.switchTab({ url: '/pages/home/home' }) })
const goRegister = () => uni.navigateTo({ url: '/pages/register/register' })
async function login() {
  if (!form.username || form.password.length < 8) return uni.showToast({ title: '请填写正确的账号密码', icon: 'none' })
  loading.value = true
  try {
    const data = await api<any>('/auth/login', 'POST', form)
    session.save(data)
    uni.switchTab({ url: '/pages/home/home' })
  } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.auth { display: flex; flex-direction: column; justify-content: center; min-height: 100vh; padding-top: 60rpx; padding-bottom: 60rpx; }
.brand-block { margin-bottom: 42rpx; text-align: center; }
.logo-mark { display: flex; align-items: center; justify-content: center; width: 100rpx; height: 100rpx; margin: 0 auto 24rpx; color: #fff; font-size: 46rpx; font-weight: 800; background: linear-gradient(145deg, $primary, #39a399); border: 8rpx solid rgba(255,255,255,.8); border-radius: 32rpx 32rpx 32rpx 10rpx; box-shadow: 0 18rpx 38rpx rgba(15,118,110,.2); }
.brand { color: $text-main; font-size: 56rpx; font-weight: 850; line-height: 1.2; letter-spacing: 6rpx; }
.slogan { display: block; margin-top: 14rpx; color: $text-muted; font-size: 25rpx; line-height: 1.6; }
.auth-card { padding: 38rpx 32rpx 34rpx; }
.auth-card .section-title { margin-bottom: 6rpx; }
.auth-note { display: block; margin-top: 8rpx; color: #91a09d; font-size: 22rpx; line-height: 1.5; text-align: center; letter-spacing: 2rpx; }
</style>
