<template>
  <view class="placeholder" :class="variant" :style="{ background: theme.background }">
    <view class="orb orb-a"></view>
    <view class="orb orb-b"></view>
    <view class="monogram"><text>{{ initial }}</text></view>
    <text v-if="location" class="place">{{ location }}</text>
  </view>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { coverInitial, coverTheme } from '@/utils/cover'
defineOptions({ options: { virtualHost: true } })
const props = withDefaults(defineProps<{ id: number; title?: string; location?: string; variant?: 'card' | 'hero' }>(), { title: '', location: '', variant: 'card' })
const theme = computed(() => coverTheme(props.id))
const initial = computed(() => coverInitial(props.title))
</script>
<style scoped lang="scss">
.placeholder { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100%; height: 100%; overflow: hidden; gap: 12rpx; }
.orb { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, .12); }
.orb-a { top: -90rpx; right: -60rpx; width: 300rpx; height: 300rpx; }
.orb-b { bottom: -120rpx; left: -80rpx; width: 260rpx; height: 260rpx; background: rgba(255, 255, 255, .08); }
.monogram { position: relative; display: flex; align-items: center; justify-content: center; width: 104rpx; height: 104rpx; color: #fff; font-size: 52rpx; font-weight: 800; background: rgba(255, 255, 255, .2); border: 2rpx solid rgba(255, 255, 255, .35); border-radius: 30rpx; }
.place { position: relative; max-width: 80%; overflow: hidden; color: rgba(255, 255, 255, .9); font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.hero .monogram { width: 132rpx; height: 132rpx; font-size: 66rpx; border-radius: 38rpx; }
.hero .place { font-size: 25rpx; }
</style>
