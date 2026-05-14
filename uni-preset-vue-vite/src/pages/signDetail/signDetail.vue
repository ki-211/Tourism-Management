<template>
  <view class="sign-detail-page">
    <view v-if="records.length === 0" class="empty-tip">暂无签到记录</view>
    <view v-for="item in records" :key="item.id" class="record-card">
      <view class="record-title">{{ item.title }}</view>
      <view class="record-time">签到时间：{{ item.signTime }}</view>
    </view>
  </view>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import request from '@/utils/request';

const records = ref([]);

const loadRecords = async () => {
  const userId = uni.getStorageSync('userId');
  try {
    const res = await request.get('/signRecord/listByUser', { userId });
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    records.value = arr.map(item => ({
      ...item,
      signTime: (item.signTime || '').replace('T', ' ')
    }));
  } catch (e) {
    uni.showToast({title: '加载失败', icon: 'none'});
  }
};

onMounted(loadRecords);
</script>

<style scoped>
.sign-detail-page {
  padding: 30rpx;
}

.record-card {
  background: #fff;
  padding: 20rpx;
  margin-bottom: 16rpx;
  border-radius: 10rpx;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.05);
}

.record-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.record-time {
  margin-top: 6rpx;
  color: #666;
  font-size: 26rpx;
}

.empty-tip {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 30rpx;
}
</style>
