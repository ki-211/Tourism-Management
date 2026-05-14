<template>
  <view class="container">
    <u-form>
      <u-form-item label="用户名">
        <u-input
            v-model="form.username"
            placeholder="请输入用户名"
            clearable
        />
      </u-form-item>
      <u-form-item label="密码">
        <u-input
            v-model="form.password"
            :password="!showPassword"
            placeholder="请输入密码"
            clearable
            @focus="passwordFocused = true"
            @blur="passwordFocused = false"
        >
          <u-icon
              slot="suffix"
              :name="showPassword ? 'eye-fill' : 'eye-off'"
              @click="showPassword = !showPassword"
          ></u-icon>
        </u-input>
      </u-form-item>

      <view v-if="passwordFocused" class="password-hint">
        密码应为6-20位，包含字母和数字
      </view>

      <u-button
          class="login-btn"
          type="primary"
          @click="doLogin"
      >
        {{ loading ? '登录中...' : '登录' }}
      </u-button>

      <view class="footer">
        <text @click="toRegister">没有账号？去注册</text>
      </view>
    </u-form>
  </view>
</template>

<script>
import request from '@/utils/request';

export default {
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      loading: false,
      passwordFocused: false,
      showPassword: false
    };
  },
  onLoad() {
    // 页面加载时检查是否已登录
    this.checkAutoLogin();
  },
  onShow() {
    // 每次页面显示时也检查（从其他页面返回时）
    this.checkAutoLogin();
  },
  methods: {
    checkAutoLogin() {
      if (this.isTokenValid()) {
        console.log('检测到有效token，自动跳转到首页');
        uni.switchTab({
          url: '/pages/home/home'
        });
      }
    },
    async doLogin() {
      if (!this.validateForm()) {
        return;
      }

      this.loading = true;
      try {
        const res = await request.post('/auth/login', this.form);
        const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null);
        const payload = packet ? packet.data : (res && res.data !== undefined ? res.data : res);

        if (!payload || !payload.token) {
          throw new Error('登录失败，token未返回');
        }

        const expiresIn = 86400;
        const expireTime = Date.now() + expiresIn * 1000;

        uni.setStorageSync('token', payload.token);
        uni.setStorageSync('token_expire', expireTime);
        uni.setStorageSync('userId', payload.userId);

        uni.showToast({
          title: (packet && packet.msg) ? packet.msg : '登录成功',
          icon: 'success',
          duration: 1500
        });

        // 跳转到 tab 页面
        setTimeout(() => {
          this.safeSwitchTab('/pages/home/home');
        }, 1500);

      } catch (err) {
        // 错误已在拦截器中处理，这里只需兜底处理
        if (err && err.msg) {
          // 拦截器已经显示了toast，这里可以不再显示
        } else {
          uni.showToast({
            title: err.message || '登录失败，请检查网络连接',
            icon: 'none',
            duration: 2000
          });
        }
      } finally {
        this.loading = false;
      }
    },

    validateForm() {
      if (!this.form.username.trim()) {
        uni.showToast({
          title: '用户名不能为空',
          icon: 'none',
          duration: 2000
        });
        return false;
      }
      if (!this.form.password.trim()) {
        uni.showToast({
          title: '密码不能为空',
          icon: 'none',
          duration: 2000
        });
        return false;
      }
      if (this.form.password.length < 6) {
        uni.showToast({
          title: '密码长度不能少于6位',
          icon: 'none',
          duration: 2000
        });
        return false;
      }
      return true;
    },

    toRegister() {
      uni.navigateTo({ url: '/pages/register/register' });
    },

    isTokenValid() {
      const token = uni.getStorageSync('token');
      const expire = uni.getStorageSync('token_expire');
      if (!token || !expire) return false;
      return Date.now() < expire;
    },

    safeSwitchTab(url) {
      if (!this.isTokenValid()) {
        // token过期，重新登录
        uni.showToast({
          title: '登录状态已过期，请重新登录',
          icon: 'none',
          duration: 2000
        });
        // 这里可以跳转到登录页或者重新执行登录流程，这里简单跳转登录页
        uni.reLaunch({ url: '/pages/login/login' });
      } else {
        uni.switchTab({ url });
      }
    }
  }
};
</script>


<style scoped>
.container {
  padding: 40rpx;
  background-color: #ffffff;
  min-height: 100vh;
}

.login-btn {
  margin-top: 60rpx;
}

.footer {
  margin-top: 40rpx;
  text-align: center;
}

.footer text {
  color: #007AFF;
  font-size: 28rpx;
}

.password-hint {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #999;
  text-align: center;
}
</style>
