<template>
  <view class="sign-task-detail">
    <view class="header">
      <view class="task-title">{{ taskInfo.taskTitle }}</view>
      <view v-if="taskInfo.taskDescription" class="task-desc">{{ taskInfo.taskDescription }}</view>
      <view class="task-time">发布时间: {{ formatTime(taskInfo.createTime) }}</view>
    </view>

    <view class="tabs">
      <view 
        class="tab-item"
        :class="{ active: currentTab === 0 }"
        @click="currentTab = 0"
      >
        已签到 ({{ signedUsers.length }})
      </view>
      <view 
        class="tab-item"
        :class="{ active: currentTab === 1 }"
        @click="currentTab = 1"
      >
        未签到 ({{ unsignedUsers.length }})
      </view>
    </view>

    <scroll-view scroll-y class="user-list">
      <!-- 已签到列表 -->
      <view v-show="currentTab === 0">
        <view v-if="signedUsers.length === 0" class="empty-tip">暂无已签到用户</view>
        <view 
          v-for="user in signedUsers" 
          :key="user.userId" 
          class="user-item"
        >
          <view class="user-info">
            <view class="user-avatar">{{ user.nickname ? user.nickname.charAt(0) : '用' }}</view>
            <view class="user-detail">
              <view class="user-name">{{ user.nickname || '用户' + user.userId }}</view>
              <view class="sign-time">签到时间: {{ formatTime(user.signTime) }}</view>
            </view>
          </view>
          <view class="status-tag signed">已签到</view>
        </view>
      </view>

      <!-- 未签到列表 -->
      <view v-show="currentTab === 1">
        <view v-if="unsignedUsers.length === 0" class="empty-tip">所有人都已签到</view>
        <view 
          v-for="user in unsignedUsers" 
          :key="user.userId" 
          class="user-item"
        >
          <view class="user-info">
            <view class="user-avatar">{{ user.nickname ? user.nickname.charAt(0) : '用' }}</view>
            <view class="user-detail">
              <view class="user-name">{{ user.nickname || '用户' + user.userId }}</view>
            </view>
          </view>
          <view class="status-tag unsigned">未签到</view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'

const taskId = ref(null)
const taskInfo = ref({})
const currentTab = ref(0)
const signedUsers = ref([])
const unsignedUsers = ref([])

onLoad((options) => {
  taskId.value = options.taskId
  if (!taskId.value) {
    uni.showToast({ title: '任务ID无效', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
    return
  }
  loadTaskDetail()
})

async function loadTaskDetail() {
  try {
    const res = await request.get('/signRecord/taskDetail', { taskId: taskId.value })
    const data = res.data || {}
    
    taskInfo.value = {
      taskId: data.taskId,
      taskTitle: data.taskTitle,
      taskDescription: data.taskDescription,
      createTime: data.createTime,
      activityId: data.activityId,
      activityTitle: data.activityTitle
    }
    
    // 解析 JSON 数组
    signedUsers.value = data.signedUsers ? (typeof data.signedUsers === 'string' ? JSON.parse(data.signedUsers) : data.signedUsers) : []
    unsignedUsers.value = data.unsignedUsers ? (typeof data.unsignedUsers === 'string' ? JSON.parse(data.unsignedUsers) : data.unsignedUsers) : []
    
  } catch (e) {
    console.error('加载签到任务详情失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const time = new Date(timeStr)
  if (isNaN(time.getTime())) return timeStr
  
  const month = String(time.getMonth() + 1).padStart(2, '0')
  const day = String(time.getDate()).padStart(2, '0')
  const hour = String(time.getHours()).padStart(2, '0')
  const minute = String(time.getMinutes()).padStart(2, '0')
  
  return `${month}-${day} ${hour}:${minute}`
}
</script>

<style scoped>
.sign-task-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  padding: 16px;
  margin-bottom: 8px;
}

.task-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.task-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
}

.task-time {
  font-size: 12px;
  color: #999;
}

.tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 15px;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #2979ff;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background-color: #2979ff;
  border-radius: 2px;
}

.user-list {
  height: calc(100vh - 200px);
  background-color: #fff;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.user-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #2979ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  margin-right: 12px;
}

.user-detail {
  flex: 1;
}

.user-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 4px;
}

.sign-time {
  font-size: 12px;
  color: #999;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-tag.signed {
  background-color: #e7f7ff;
  color: #19be6b;
}

.status-tag.unsigned {
  background-color: #fff3e6;
  color: #ff9800;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}
</style>
