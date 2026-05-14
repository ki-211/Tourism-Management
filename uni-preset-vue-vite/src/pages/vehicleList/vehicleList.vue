<template>
  <view class="container">
    <view class="title">车辆列表</view>
    <view v-if="vehicleList.length === 0" class="empty-text">
      暂未发布车辆
    </view>
    <view v-for="item in vehicleList" :key="item.id" class="vehicle-card">
      <view><text class="label">车牌号：</text>{{ item.plateNumber }}</view>
      <view><text class="label">司机姓名：</text>{{ item.driverName }}</view>
      <view><text class="label">上车时间：</text>{{ formatDateTime(item.pickupTime) }}</view>
      <view><text class="label">上车地点：</text>{{ item.pickupLocation }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '@/utils/request';

const vehicleList = ref([]);
const activityId = ref(null);

function formatDateTime(datetime) {
  if (!datetime) return '';
  // 时间格式化示例，后端格式为 yyyy-MM-dd HH:mm:ss
  const dateObj = new Date(datetime);
  if (isNaN(dateObj)) return datetime; // 兼容异常数据
  const y = dateObj.getFullYear();
  const m = (dateObj.getMonth() + 1).toString().padStart(2, '0');
  const d = dateObj.getDate().toString().padStart(2, '0');
  const hh = dateObj.getHours().toString().padStart(2, '0');
  const mm = dateObj.getMinutes().toString().padStart(2, '0');
  return `${y}年${m}月${d}日 ${hh}时${mm}分`;
}

onLoad((options) => {
  activityId.value = options.id;
  loadVehicleList();
});

function loadVehicleList() {
  request.get('/vehicle/list/' + activityId.value).then(res => {
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    vehicleList.value = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
  });
}
</script>

<style scoped>
.container {
  padding: 20rpx;
}

.title {
  font-weight: bold;
  font-size: 30rpx;
  margin-bottom: 20rpx;
  color: #333;
}

.vehicle-card {
  background: #fff;
  padding: 20rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.1);
  border: 1rpx solid #eee;
}

.label {
  font-weight: 600;
  color: #555;
}
.empty-text {
  color: #999;
  font-size: 28rpx;
  text-align: center;
  margin-top: 50rpx;
}
</style>
