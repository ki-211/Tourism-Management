<template>
  <view class="activity-list">
    <!-- 顶部导航栏样式 -->
    <view class="header">
      <view class="title">活动列表</view>
      <u-button
          type="success"
          size="mini"
          plain
          @click="toOngoing"
          style="margin-right: 12rpx;"
      >进行中</u-button>
      <u-button
          class="create-btn"
          type="primary"
          size="mini"
          @click="toCreate"
      >发布活动</u-button>
    </view>

    <u-list>
      <u-list-item v-for="item in list" :key="item.id">
        <view class="card">
          <view class="title">{{ item.title }}</view>
          <view class="time">{{ item.startTime }} ~ {{ item.endTime }}</view>
          <view class="location">地点: {{ item.location }}</view>
          <view class="signup-time">报名时间: {{ item.signupStart }} ~ {{ item.signupEnd }}</view>
          <view class="fee-rule">费用说明: {{ item.feeRule }}</view>
          <view class="desc">说明: {{ item.description }}</view>
          <view v-if="item.imageUrl" class="image-container">
            <image :src="item.imageUrl" mode="aspectFill" class="activity-image" />
          </view>
          <u-button
              class="create-btn"
              type="primary"
              size="mini"
              @click="goDetail(item.id)"
          >查看详情</u-button>
        </view>
      </u-list-item>
    </u-list>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'

const list = ref([])

const loadList = async () => {
  try {
    const res = await request.get('/activity/all')
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    list.value = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
  } catch (e) {
    // 错误已在拦截器中处理
    console.error('加载活动列表失败:', e)
  }
}

onShow(() => {
  loadList()
})

onMounted(() => {
  loadList()
})

function goDetail(id) {
  uni.navigateTo({
    url: `/pages/activityDetail/activityDetail?id=${encodeURIComponent(id)}`
  })
}

function toCreate() {
  uni.navigateTo({ url: '/pages/activityCreate/activityCreate' })
}

function toOngoing() {
  uni.navigateTo({ url: '/pages/ongoingSignup/ongoingSignup' })
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
  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 超出隐藏 */
  text-overflow: ellipsis; /* 超出用省略号 */
  flex: 1; /* 占据剩余空间 */
  margin-right: 12px; /* 标题和按钮间距 */
}

.create-btn {
  padding: 4px 8px; /* 缩小左右内边距 */
  width: 80px; /* 固定宽度 */
  min-width: unset; /* 取消u-button默认最小宽度限制 */
  flex-shrink: 0; /* 不缩小 */
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
</style>
