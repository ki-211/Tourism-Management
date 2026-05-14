<template>
  <view class="container">
    <u-form :model="form" ref="formRef">
      <u-form-item label="签到标题" prop="title" required>
        <u-input v-model="form.title" placeholder="请输入签到任务标题" />
      </u-form-item>
      <u-form-item label="签到说明" prop="description">
        <u-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入签到说明（可选）"
        />
      </u-form-item>
      <u-button type="primary" @click="submit">发布签到</u-button>
    </u-form>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '@/utils/request';

const formRef = ref(null);
const form = ref({
  title: '',
  description: ''
});
const activityId = ref(null);

onLoad((options) => {
  activityId.value = options.id;
});

function submit() {
  if (!form.value.title) {
    return uni.showToast({ title: '请输入签到标题', icon: 'none' });
  }

  const userId = uni.getStorageSync('userId');
  if (!userId) {
    return uni.redirectTo({ url: '/pages/login/login' });
  }

  const payload = {
    ...form.value,
    activityId: activityId.value,
    createUserId: userId
  };

  request.post('/signTask/create', payload).then(() => {
    uni.showToast({ title: '签到任务已发布' });
    setTimeout(() => {
      uni.navigateBack();
    }, 800);
  });
}
</script>

<style scoped>
.container {
  padding: 24rpx;
}
</style>
