<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">注册账号</text><view class="page-title">创建协作账号</view><text class="muted intro">只需简单几步，开始组织你的下一次活动。</text></view>
    <view class="card form-card">
      <view class="field"><text class="field-label">用户名</text><input v-model="form.username" class="input" placeholder="3-16 位字母、数字或下划线" /></view>
      <view class="field"><text class="field-label">昵称</text><input v-model="form.nickname" class="input" placeholder="大家怎么称呼你" /></view>
      <view class="field"><text class="field-label">密码</text><input v-model="form.password" class="input" password placeholder="至少 8 位" /></view>
      <view class="field"><text class="field-label">确认密码</text><input v-model="confirm" class="input" password placeholder="再次输入密码" /></view>
      <button class="primary-btn" :loading="loading" @click="submit">完成注册</button>
    </view>
    <text class="tip">注册即代表你同意合理使用活动协作功能</text>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '@/services/api'
const form = reactive({ username: '', nickname: '', password: '' })
const confirm = ref('')
const loading = ref(false)
async function submit() {
  if (!/^[A-Za-z0-9_]{3,16}$/.test(form.username) || form.password.length < 8 || form.password !== confirm.value) return uni.showToast({ title: '请检查用户名和密码', icon: 'none' })
  loading.value = true
  try {
    await api('/auth/register', 'POST', form)
    uni.showToast({ title: '注册成功' })
    setTimeout(() => uni.navigateBack(), 600)
  } finally { loading.value = false }
}
</script>
<style scoped lang="scss">
.intro { margin-top: 12rpx; }
.tip { display: block; padding: 8rpx 24rpx; color: $text-muted; font-size: 22rpx; line-height: 1.6; text-align: center; }
</style>
