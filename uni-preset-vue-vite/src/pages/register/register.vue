<template>
  <view class="container">
    <u-form>
      <u-form-item label="用户名">
        <u-input 
          v-model="form.username" 
          placeholder="请输入用户名(3-16位字母数字)" 
          clearable
        />
      </u-form-item>
      <u-form-item label="密码">
        <u-input 
          v-model="form.password" 
          :password="!showPassword"
          placeholder="请输入密码(6-20位)" 
          clearable
        >
          <u-icon
            slot="suffix"
            :name="showPassword ? 'eye-fill' : 'eye-off'"
            @click="showPassword = !showPassword"
          ></u-icon>
        </u-input>
      </u-form-item>
      <u-form-item label="确认密码">
        <u-input 
          v-model="form.confirmPassword" 
          :password="!showConfirmPassword"
          placeholder="请再次输入密码" 
          clearable
        >
          <u-icon
            slot="suffix"
            :name="showConfirmPassword ? 'eye-fill' : 'eye-off'"
            @click="showConfirmPassword = !showConfirmPassword"
          ></u-icon>
        </u-input>
      </u-form-item>
      <u-form-item label="昵称">
        <u-input 
          v-model="form.nickname" 
          placeholder="请输入昵称(选填)" 
          clearable
        />
      </u-form-item>
      <u-button type="primary" @click="doRegister" :loading="loading">
        {{ loading ? '注册中...' : '注册' }}
      </u-button>
      <view class="footer">
        <text @click="toLogin">已有账号？去登录</text>
      </view>
    </u-form>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import request from '@/utils/request';

const form = reactive({ 
  username: '', 
  password: '', 
  confirmPassword: '',
  nickname: '' 
});

const loading = ref(false);
const showPassword = ref(false);
const showConfirmPassword = ref(false);

// 表单验证
function validateForm() {
  // 验证用户名
  if (!form.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none', duration: 2000 });
    return false;
  }
  if (form.username.length < 3 || form.username.length > 16) {
    uni.showToast({ title: '用户名长度应为3-16位', icon: 'none', duration: 2000 });
    return false;
  }
  if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
    uni.showToast({ title: '用户名只能包含字母、数字和下划线', icon: 'none', duration: 2000 });
    return false;
  }

  // 验证密码
  if (!form.password.trim()) {
    uni.showToast({ title: '请输入密码', icon: 'none', duration: 2000 });
    return false;
  }
  if (form.password.length < 6 || form.password.length > 20) {
    uni.showToast({ title: '密码长度应为6-20位', icon: 'none', duration: 2000 });
    return false;
  }

  // 验证确认密码
  if (form.password !== form.confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none', duration: 2000 });
    return false;
  }

  // 昵称是选填的，如果填写了则验证长度
  if (form.nickname && form.nickname.length > 20) {
    uni.showToast({ title: '昵称长度不能超过20位', icon: 'none', duration: 2000 });
    return false;
  }

  return true;
}

function doRegister() {
  if (!validateForm()) {
    return;
  }

  loading.value = true;

  // 准备提交的数据（不包含confirmPassword）
  const submitData = {
    username: form.username,
    password: form.password,
    nickname: form.nickname || form.username // 如果没填昵称，默认使用用户名
  };

  request.post('/auth/register', submitData)
    .then((res) => {
      uni.showToast({ 
        title: res.msg || '注册成功', 
        icon: 'success',
        duration: 1500
      });
      setTimeout(() => uni.navigateBack(), 1500);
    })
    .catch(err => {
      // 错误已在拦截器中处理
      console.error('注册失败:', err);
    })
    .finally(() => {
      loading.value = false;
    });
}

function toLogin() {
  uni.navigateBack();
}
</script>

<style scoped>
.container { 
  padding: 40rpx;
  background-color: #ffffff;
  min-height: 100vh;
}

.footer {
  margin-top: 40rpx;
  text-align: center;
}

.footer text {
  color: #007AFF;
  font-size: 28rpx;
}
</style>
