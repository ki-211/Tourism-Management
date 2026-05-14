<template>
  <view class="personal-page">
    <!-- 右上角退出按钮 -->
    <view class="logout">
      <u-button type="error" size="small" @click="logout">退出</u-button>
    </view>

    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="user-name">👤 {{ userInfo.nickname || '未命名用户' }}</view>
      <view class="user-account">账号：{{ userInfo.username || '未知' }}</view>
      <u-button class="edit-btn" type="primary" size="mini" @click="toEdit">修改信息</u-button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app' // 正确引入 onShow
import request from '@/utils/request'

const userInfo = ref({})

// 加载用户信息
function loadUserInfo() {
  const userId = uni.getStorageSync('userId')
  if (!userId) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }

  request.get(`/auth/info?userId=${userId}`).then(res => {
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    userInfo.value = packet ? (packet.data || {}) : (res && res.data !== undefined ? (res.data || {}) : (res || {}))
  })
}

// 页面加载时 & 返回时都重新拉取信息
onMounted(loadUserInfo)
onShow(loadUserInfo)

function toEdit() {
  uni.navigateTo({ url: '/pages/editInfo/editInfo' })
}

function logout() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userId')
  uni.redirectTo({ url: '/pages/login/login' })
}
</script>

<style scoped>
.personal-page {
  padding: 20px;
  position: relative;
  min-height: 100vh;
  background-color: #f9f9f9;
}

.logout {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
}

.user-card {
  background: #ffffff;
  padding: 16px;
  border-radius: 10px;
  margin-top: 60px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 6px;
}

.user-account {
  font-size: 14px;
  color: #666;
}

.edit-btn {
  margin-top: 10px;
}
</style>
