<template>
  <image class="app-icon" :src="src" mode="aspectFit" :style="{ width: `${size}rpx`, height: `${size}rpx` }" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { iconDataUri, type IconName } from '@/utils/icons'
// virtualHost: no extra host node in mini programs, so class/style from the parent land on the <image> itself.
defineOptions({ options: { virtualHost: true } })
const props = withDefaults(defineProps<{ name: IconName; size?: number; color?: string; strokeWidth?: number }>(), { size: 30, color: '#0f766e', strokeWidth: 1.8 })
const src = computed(() => iconDataUri(props.name, props.color, props.strokeWidth))
</script>
<style scoped>
.app-icon { display: block; flex-shrink: 0; }
</style>
