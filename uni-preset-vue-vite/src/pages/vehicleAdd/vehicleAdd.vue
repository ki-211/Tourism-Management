<template>
  <view class="container">
    <u-form labelPosition="top">
      <u-form-item label="车牌号">
        <u-input v-model="form.plateNumber" placeholder="请输入车牌号" />
      </u-form-item>

      <u-form-item label="司机姓名">
        <u-input v-model="form.driverName" placeholder="请输入司机姓名" />
      </u-form-item>

      <!-- 上车时间选择 -->
      <u-form-item label="上车时间" @click="showPickupTimePicker = true">
        <u-input
            v-model="formattedPickupTime"
            placeholder="请选择上车时间"
            readonly
            prefixIcon="calendar"
        />
      </u-form-item>

      <u-datetime-picker
          v-model="pickupTimeValue"
          mode="datetime"
          :show="showPickupTimePicker"
          closeOnClickOverlay
          @confirm="onPickupTimeConfirm"
          @cancel="showPickupTimePicker = false"
      />

      <u-form-item label="上车地点">
        <u-input v-model="form.pickupLocation" placeholder="请输入上车地点" />
      </u-form-item>

      <u-button type="primary" @click="submit">发布车辆信息</u-button>
    </u-form>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';

const form = ref({
  plateNumber: '',
  driverName: '',
  pickupTime: '',
  pickupLocation: '',
  activityId: null,
  creatorId: null
});

const pickupTimeValue = ref(Date.now());
const formattedPickupTime = ref('');
const showPickupTimePicker = ref(false);

// 格式化函数
function formatDate(ts) {
  const d = new Date(ts);
  const pad = n => (n < 10 ? '0' + n : n);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

// 时间确认回调（统一处理）
function onPickupTimeConfirm(e) {
  showPickupTimePicker.value = false;
  pickupTimeValue.value = e.value;
  form.value.pickupTime = formatDate(e.value);
  const d = new Date(e.value);
  formattedPickupTime.value = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours()}时${d.getMinutes()}分`;
}

// 页面加载：获取活动 ID 和 userId
onMounted(() => {
  const params = getCurrentPages()?.pop()?.options || {};
  if (params.id || params.activityId) {
    form.value.activityId = Number(params.id || params.activityId);
  }
  const uid = uni.getStorageSync('userId');
  if (uid) {
    form.value.creatorId = uid;
  }
});

function submit() {
  if (
      !form.value.plateNumber ||
      !form.value.driverName ||
      !form.value.pickupLocation ||
      !form.value.pickupTime ||
      !form.value.activityId ||
      !form.value.creatorId
  ) {
    return uni.showToast({ title: '请填写完整信息', icon: 'none' });
  }

  request
      .post('/vehicle/add', form.value)
      .then(() => {
        uni.showToast({ title: '发布成功', icon: 'success' });
        uni.navigateBack();
      })
      .catch(() => {
        uni.showToast({ title: '发布失败，请重试', icon: 'none' });
      });
}
</script>

<style scoped>
.container {
  padding: 20rpx;
}
</style>
