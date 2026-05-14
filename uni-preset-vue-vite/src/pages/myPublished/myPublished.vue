<template>
  <view class="activity-list">
    <view class="header">
      <view class="title">我发布的活动</view>
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
          <view class="desc">说明: {{ item.description }}</view>
          <u-button
              class="create-btn"
              type="primary"
              size="mini"
              @click="goDetail(item.id)"
          >查看详情</u-button>
        </view>
      </u-list-item>

      <view v-if="list.length === 0" class="empty-tip">
        暂无发布的活动
      </view>
    </u-list>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'

const list = ref([])

const loadPublished = async () => {
  const userId = uni.getStorageSync('userId')
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  try {
    const res = await request.get(`/activity/published?userId=${userId}`)
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    list.value = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
  } catch (e) {
    list.value = []
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

onShow(() => {
  loadPublished()
})

onMounted(() => {
  loadPublished()
})

function goDetail(id) {
  uni.navigateTo({
    url: '/pages/activityDetail/activityDetail?id=' + id
  })
}
</script>

<style scoped>
.activity-list {
  padding: 16px;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.header .title {
  font-size: 18px;
  font-weight: bold; /* 加粗标题 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
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
.desc {
  margin-top: 6px;
  font-size: 14px;
  color: #666;
}

.empty-tip {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 16px;
}

.create-btn {
  margin-top: 10px;
  padding: 4px 8px;
  width: 80px;
  min-width: unset;
  flex-shrink: 0;
}
</style>
