<template>
  <view class="edit-info-page">
    <u-form :model="form" ref="formRef">
      <u-form-item label="昵称">
        <u-input v-model="form.nickname" placeholder="请输入新昵称" />
      </u-form-item>

      <u-button type="primary" @click="submit" class="save-btn">保存</u-button>
    </u-form>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';

const form = ref({ nickname: '' });
const formRef = ref();

onMounted(() => {
  const userId = uni.getStorageSync('userId');
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    return;
  }

  request.get(`/auth/info?userId=${userId}`).then(res => {
    form.value.nickname = res.data.nickname || '';
  });
});

function submit() {
  const userId = uni.getStorageSync('userId');
  if (!userId) return;

  request.post('/auth/update', {
    userId,
    nickname: form.value.nickname
  }).then(() => {
    uni.showToast({ title: '修改成功', icon: 'success' });
    setTimeout(() => {
      uni.navigateBack();
    }, 800);
  });
}
</script>

<style scoped>
.edit-info-page {
  padding: 20px;
}
.save-btn {
  margin-top: 20px;
}
</style>
