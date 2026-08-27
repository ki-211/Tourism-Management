<template>
  <view class="load-error">
    <view class="error-mark">!</view>
    <text class="error-title">加载失败</text>
    <text class="error-message">{{ message || '网络开小差了，请稍后重试' }}</text>
    <button class="secondary-btn retry-button" @click="$emit('retry')">重新加载</button>
  </view>
</template>

<script setup lang="ts">
defineProps<{ message?: string }>()
defineEmits<{ retry: [] }>()
</script>

<style scoped lang="scss">
.load-error { padding: 80rpx 30rpx; text-align: center; }
.error-mark { display: flex; align-items: center; justify-content: center; width: 86rpx; height: 86rpx; margin: 0 auto 20rpx; color: $warning; font-size: 42rpx; font-weight: 800; background: $accent-soft; border-radius: 50%; }
.error-title, .error-message { display: block; }
.error-title { color: $text-main; font-size: 30rpx; font-weight: 700; line-height: 1.4; }
.error-message { margin-top: 7rpx; color: $text-muted; font-size: 24rpx; line-height: 1.6; }
.retry-button { width: 260rpx; height: 76rpx; margin: 26rpx auto 0; font-size: 25rpx; }
</style>
