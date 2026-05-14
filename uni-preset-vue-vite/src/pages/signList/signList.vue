<template>
  <view class="sign-list-page">
    <!-- 右上角按钮 -->
    <view class="top-bar">
      <u-button
          type="primary"
          size="mini"
          plain
          class="history-btn"
          @click="goHistory"
      >
        签到记录
      </u-button>
    </view>

    <view v-if="taskList.length === 0" class="empty-tip">
      暂无签到任务
    </view>

    <view v-for="item in taskList" :key="item.id" class="task-card">
      <view class="left">
        <view class="task-title">{{ item.title }}</view>
        <view class="task-time">签到时间：{{ item.signTime }}</view>
      </view>
      <u-button
          type="success"
          size="mini"
          class="sign-btn"
          @click="doSign(item.id)"
          :disabled="item.signed"
      >
        {{ item.signed ? '已签到' : '签到' }}
      </u-button>
    </view>
  </view>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {onShow} from '@dcloudio/uni-app' // ✅ 注意：来自 uni-app
import request from '@/utils/request'

const taskList = ref([])

const loadTasks = async () => {
  const userId = uni.getStorageSync('userId')
  if (!userId) {
    uni.redirectTo({url: '/pages/login/login'})
    return
  }

  try {
    const res = await request.get('/signTask/unsigned', { userId })
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    console.log('签到列表页后端返回数据:', arr)
    taskList.value = arr
  } catch (e) {
    uni.showToast({title: '加载失败', icon: 'none'})
  }
}

// 页面显示时自动刷新
onShow(() => {
  loadTasks()
})

// 初次挂载（可选）
onMounted(loadTasks)

const doSign = async (taskId) => {
  const userId = uni.getStorageSync('userId')
  try {
    await request.post('/signRecord/do', {taskId, userId})
    uni.showToast({title: '签到成功'})
    loadTasks() // 签到后刷新
  } catch (e) {
    uni.showToast({title: '签到失败', icon: 'none'})
  }
}

const goHistory = () => {
  uni.navigateTo({url: '/pages/signDetail/signDetail'})
}
</script>

<style scoped>
.sign-list-page {
  padding: 30rpx;
}

.top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20rpx;
}

.history-btn {
  font-size: 24rpx;
  padding: 0 20rpx;
  height: 50rpx;
  line-height: 50rpx;
  border-radius: 12rpx;
}

.task-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left {
  flex: 1;
  margin-right: 20rpx;
}

.task-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #222;
}

.task-time {
  font-size: 26rpx;
  color: #888;
  margin-top: 6rpx;
}

.sign-btn {
  font-size: 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 8rpx;
  border-radius: 28rpx;
  min-width: 60rpx;
  max-width: 80rpx;
  text-align: center;
}

.empty-tip {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 30rpx;
}
</style>
