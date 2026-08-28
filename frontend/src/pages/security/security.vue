<template>
  <view class="page security-page">
    <view class="page-head"><text class="eyebrow">ACCOUNT SECURITY</text><view class="page-title">账号与安全</view><text class="muted">密码修改后会立即退出所有已登录设备。</text></view>
    <view class="card form-card">
      <view class="section-title">修改密码</view>
      <view class="field"><text class="field-label">当前密码</text><input v-model="password.current" class="input" password maxlength="64" placeholder="请输入当前密码" /></view>
      <view class="field"><text class="field-label">新密码</text><input v-model="password.next" class="input" password maxlength="64" placeholder="8-64 位，不能与当前密码相同" /></view>
      <view class="field"><text class="field-label">确认新密码</text><input v-model="password.confirm" class="input" password maxlength="64" placeholder="再次输入新密码" /></view>
      <button class="primary-btn" :loading="saving" :disabled="saving" @click="changePassword">保存新密码</button>
    </view>
    <view class="card danger-card">
      <view class="section-title">设备与账号</view>
      <button class="secondary-btn" :loading="processing" :disabled="processing" @click="logoutAll">退出所有设备</button>
      <text class="danger-copy">注销账号前，必须先转让你负责的所有活动。注销后用户名和昵称会匿名化，且无法恢复。</text>
      <view class="field"><text class="field-label">输入密码确认注销</text><input v-model="deletePassword" class="input" password maxlength="64" placeholder="请输入当前密码" /></view>
      <button class="danger-button" :loading="deleting" :disabled="deleting" @click="deleteAccount">永久注销账号</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useSessionStore } from '@/stores/session'
import { useLocationSharingStore } from '@/stores/locationSharing'

const session = useSessionStore()
const sharing = useLocationSharingStore()
const password = reactive({ current: '', next: '', confirm: '' })
const deletePassword = ref('')
const saving = ref(false)
const processing = ref(false)
const deleting = ref(false)

async function leaveSession(message: string) {
  await sharing.stop(true)
  session.clear()
  uni.showToast({ title: message, icon: 'none' })
  setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 500)
}
async function changePassword() {
  if (password.current.length < 8 || password.next.length < 8 || password.next !== password.confirm)
    return uni.showToast({ title: '请检查当前密码和新密码', icon: 'none' })
  saving.value = true
  try { await api('/users/me/password', 'PUT', { currentPassword: password.current, newPassword: password.next }); await leaveSession('密码已修改，请重新登录') }
  finally { saving.value = false }
}
function logoutAll() {
  uni.showModal({ title: '退出所有设备', content: '包括当前设备在内的全部登录状态都会失效。', success: async result => {
    if (!result.confirm) return
    processing.value = true
    try { await api('/users/me/logout-all', 'POST'); await leaveSession('所有设备已退出') }
    finally { processing.value = false }
  } })
}
function deleteAccount() {
  if (deletePassword.value.length < 8) return uni.showToast({ title: '请输入当前密码', icon: 'none' })
  uni.showModal({ title: '确认永久注销', content: '账号注销后无法恢复，确定继续吗？', confirmColor: '#dc4c4c', success: async result => {
    if (!result.confirm) return
    deleting.value = true
    try { await api('/users/me', 'DELETE', { password: deletePassword.value }); await leaveSession('账号已注销') }
    finally { deleting.value = false }
  } })
}
</script>

<style scoped lang="scss">
.security-page{padding-top:38rpx}.form-card,.danger-card{padding:30rpx}.danger-card{margin-top:24rpx;border:1rpx solid rgba(220,76,76,.2)}.danger-copy{display:block;margin:24rpx 0;color:$text-secondary;font-size:24rpx;line-height:1.7}.danger-button{width:100%;height:84rpx;margin-top:20rpx;color:#fff;font-size:26rpx;line-height:84rpx;background:$danger;border-radius:18rpx}
</style>
