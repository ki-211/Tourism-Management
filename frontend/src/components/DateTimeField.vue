<template>
  <view class="datetime-field">
    <picker class="picker-part" mode="date" :value="dateValue" :start="startDate" @change="onDateChange">
      <view class="picker-box" :class="{ placeholder: !dateValue }">
        <text class="picker-icon">▦</text>
        <text>{{ dateValue || '选择日期' }}</text>
      </view>
    </picker>
    <picker class="picker-part time-part" mode="time" :value="timeValue" @change="onTimeChange">
      <view class="picker-box" :class="{ placeholder: !timeValue }">
        <text class="picker-icon">◷</text>
        <text>{{ timeValue || '选择时间' }}</text>
      </view>
    </picker>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const now = new Date()
const pad = (value: number) => String(value).padStart(2, '0')
const startDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
const dateValue = computed(() => props.modelValue?.slice(0, 10) || '')
const timeValue = computed(() => props.modelValue?.slice(11, 16) || '')

function valueOf(event: any) {
  return String(event.detail.value || '')
}
function onDateChange(event: any) {
  emit('update:modelValue', `${valueOf(event)}T${timeValue.value || '09:00'}`)
}
function onTimeChange(event: any) {
  emit('update:modelValue', `${dateValue.value || startDate}T${valueOf(event)}`)
}
</script>

<style scoped lang="scss">
.datetime-field { display: flex; width: 100%; gap: 14rpx; }
.picker-part { flex: 1; min-width: 0; }
.time-part { flex: 0 0 230rpx; }
.picker-box { display: flex; align-items: center; width: 100%; height: 92rpx; padding: 0 20rpx; color: $text-main; font-size: 27rpx; line-height: 1; white-space: nowrap; background: #f9fbfa; border: 2rpx solid $border; border-radius: 18rpx; gap: 12rpx; }
.picker-box.placeholder { color: $text-muted; }
.picker-icon { flex-shrink: 0; color: $primary; font-size: 25rpx; }
</style>
