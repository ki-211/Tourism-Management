<template>
  <view class="activity-list">
    <!-- 顶部导航栏样式 -->
    <view class="header">
      <view class="title">参加过的活动</view>
      <u-button
          class="create-btn"
          type="primary"
          size="mini"
          @click="toMyPublished"
      >我发布的活动</u-button>
    </view>

    <u-list>
      <u-list-item
          v-for="item in list"
          :key="item.id"
      >
        <view class="card">
          <view class="title">{{ item.title }}</view>
          <view class="time">{{ item.startTime }} ~ {{ item.endTime }}</view>
          <view class="location">地点: {{ item.location }}</view>
          <view class="signup-time">报名时间: {{ item.signupStart }} ~ {{ item.signupEnd }}</view>
          <view class="fee-rule">费用说明: {{ item.feeRule }}</view>
          <view class="desc">说明: {{ item.description }}</view>
          <u-button
              class="create-btn"
              type="primary"
              size="mini"
              @click="goDetail(item.id)"
          >查看详情</u-button>
          <view v-if="item.imageUrl" class="image-container">
            <image :src="item.imageUrl" mode="aspectFill" class="activity-image" />
          </view>
        </view>
      </u-list-item>

      <view v-if="list.length === 0" class="empty-tip">
        您还没有参加任何活动
      </view>
    </u-list>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'

const list = ref([])

const loadMyActivities = async () => {
  const userId = uni.getStorageSync('userId')
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await request.get(`/activity/my?userId=${userId}`)
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    list.value = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
    list.value = []
  }
}

onShow(() => {
  loadMyActivities()
})

onMounted(() => {
  loadMyActivities()
})

function goDetail(id) {
  uni.navigateTo({
    url: '/pages/activityDetail/activityDetail?id=' + id
  })
}

function toMyPublished() {
  uni.navigateTo({
    url: '/pages/myPublished/myPublished'
  })
}
</script>

<style scoped>
.activity-list {
  padding: 16px;
}

/* 顶部区域：左标题 + 右按钮 */
.header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.header .title {
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 12px;
}

.create-btn {
  padding: 4px 8px;
  width: 100px;
  min-width: unset;
  flex-shrink: 0;
}

.card {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.title {
  font-weight: bold;
  font-size: 16px;
}

.time {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}

.location,
.signup-time,
.fee-rule,
.visibility,
.desc {
  margin-top: 6px;
  font-size: 14px;
  color: #666;
}

.image-container {
  margin-top: 10px;
  width: 100%;
  height: 160px;
}

.activity-image {
  width: 100%;
  height: 100%;
  border-radius: 6px;
}

.empty-tip {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 16px;
}
</style>
