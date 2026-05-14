<template>
  <view class="activity-list">
    <view class="header">
      <view class="title">进行中报名</view>
    </view>

    <u-list>
      <u-list-item v-for="item in ongoingList" :key="item.id">
        <view class="card">
          <view class="title">{{ item.title }}</view>
          <view class="time">活动时间：{{ item.startTime }} ~ {{ item.endTime }}</view>
          <view class="signup-time">报名时间：{{ item.signupStart }} ~ {{ item.signupEnd }}</view>
          <view class="location">地点：{{ item.location }}</view>
          <view class="fee-rule">费用说明：{{ item.feeRule }}</view>
          <view v-if="item.imageUrl" class="image-container">
            <image :src="item.imageUrl" mode="aspectFill" class="activity-image" />
          </view>
          <view class="btn-row">
            <u-button type="primary" size="mini" @click="goDetail(item.id)">查看详情</u-button>
            <u-button type="success" size="mini" plain @click="goSignup(item.id)" style="margin-left: 12rpx">前往报名</u-button>
            <u-button type="warning" size="mini" plain @click="goActivityRoom(item.id)" style="margin-left: 12rpx">进入活动室</u-button>
          </view>
        </view>
      </u-list-item>

      <view v-if="ongoingList.length === 0" class="empty-tip">
        暂无正在报名的活动
      </view>
    </u-list>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '@/utils/request'

const all = ref([])
const ongoingList = ref([])

function parseDate(str) {
  if (!str) return null
  // 兼容各平台的日期解析：YYYY-MM-DD HH:mm:ss
  const [datePart, timePart = '00:00:00'] = String(str).split(' ')
  const [y, m, d] = datePart.split('-').map(n => parseInt(n, 10))
  const [hh, mm, ss] = timePart.split(':').map(n => parseInt(n, 10))
  return new Date(y, m - 1, d, hh || 0, mm || 0, ss || 0)
}

function isSignupOpen(item) {
  const now = Date.now()
  const start = parseDate(item.signupStart)?.getTime()
  const end = parseDate(item.signupEnd)?.getTime()
  if (!start || !end) return false
  return now >= start && now <= end
}

async function loadAll() {
  try {
    const res = await request.get('/activity/all')
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    all.value = Array.isArray(arr) ? arr : []
    ongoingList.value = all.value.filter(isSignupOpen)
  } catch (e) {
    all.value = []
    ongoingList.value = []
  }
}

onShow(loadAll)
onMounted(loadAll)

function goDetail(id) {
  uni.navigateTo({
    url: `/pages/activityDetail/activityDetail?id=${encodeURIComponent(id)}`
  })
}
function goSignup(id) {
  uni.navigateTo({
    url: `/pages/signupList/signupList?id=${encodeURIComponent(id)}`
  })
}

function goActivityRoom(id) {
  uni.navigateTo({
    url: `/pages/activityRoom/activityRoom?id=${encodeURIComponent(id)}`
  })
}
</script>

<style scoped>
.activity-list { padding: 16px; }
.header { display: flex; align-items: center; margin-bottom: 12px; }
.header .title { font-size: 18px; font-weight: bold; }
.card { padding: 16px; border-bottom: 1px solid #eee; }
.title { font-weight: bold; font-size: 16px; }
.time { color: #999; font-size: 13px; margin-top: 4px; }
.location, .signup-time, .fee-rule, .desc { margin-top: 6px; font-size: 14px; color: #666; }
.image-container { margin-top: 10px; width: 100%; height: 160px; }
.activity-image { width: 100%; height: 100%; border-radius: 6px; }
.btn-row { margin-top: 10px; display: flex; }
.empty-tip { color: #888; text-align: center; padding: 40rpx 0; }
</style>