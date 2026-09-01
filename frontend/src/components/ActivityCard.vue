<template>
  <view class="card activity" @click="$emit('open', activity.id)">
    <view class="cover-wrap">
      <image v-if="activity.coverUrl" class="cover" :src="activity.coverUrl" mode="aspectFill" />
      <view v-else class="cover placeholder-cover">
        <text class="placeholder-mark">行</text>
        <text>把期待装进行程</text>
      </view>
      <text class="life-tag" :class="life">{{ lifeLabel }}</text>
      <text class="visibility">{{ activity.visibility === 'PUBLIC' ? '公开活动' : '邀请制' }}</text>
    </view>
    <view class="content">
      <view class="row-between title-row">
        <text class="title">{{ activity.title }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="info-line"><text class="info-icon">⌖</text><text class="info-value">{{ activity.location }}</text></view>
      <view class="info-line"><text class="info-icon">◷</text><text class="info-value">{{ timeText }}</text></view>
      <view class="meta-row">
        <text class="host">发起人 {{ activity.creatorName }}</text>
        <text v-if="activity.joined" class="tag">{{ activity.creator ? '我负责' : '已参与' }}</text>
        <text v-else class="deadline">{{ deadlineText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useCurrentTime } from '@/composables/useCurrentTime'
import type { Activity } from '@/services/types'
import { activityLife, activityLifeLabel, displayTime, displayTimeRange } from '@/utils/time'

const props = defineProps<{ activity: Activity }>()
defineEmits<{ open: [id: number] }>()

const now = useCurrentTime()
const life = computed(() => activityLife(props.activity, now.value))
const lifeLabel = computed(() => activityLifeLabel[life.value])
const timeText = computed(() => displayTimeRange(props.activity.startTime, props.activity.endTime, now.value))
const deadlineText = computed(() => {
  if (life.value === 'upcoming') return `${displayTime(props.activity.signupStart, now.value)} 开始报名`
  if (life.value === 'signup') return `报名至 ${displayTime(props.activity.signupEnd, now.value)}`
  if (life.value === 'closed') return '报名已截止'
  return ''
})
</script>

<style scoped lang="scss">
.activity { padding: 0; }
.cover-wrap { position: relative; width: 100%; height: 286rpx; overflow: hidden; background: $primary-soft; }
.cover { display: flex; width: 100%; height: 100%; }
.placeholder-cover { align-items: center; justify-content: center; flex-direction: column; gap: 8rpx; color: rgba(255, 255, 255, .88); font-size: 24rpx; background: linear-gradient(145deg, #0f766e, #52a99e); }
.placeholder-mark { display: flex; align-items: center; justify-content: center; width: 72rpx; height: 72rpx; color: $primary; font-size: 34rpx; font-weight: 800; background: rgba(255, 255, 255, .92); border-radius: 50%; }
.visibility, .life-tag {
  position: absolute;
  top: 20rpx;
  padding: 8rpx 16rpx;
  color: #fff;
  font-size: 22rpx;
  font-weight: 650;
  line-height: 1.3;
  white-space: nowrap;
  border-radius: 999rpx;
}
.visibility { right: 20rpx; background: rgba(20, 42, 39, .64); }
.life-tag { left: 20rpx; background: rgba(20, 42, 39, .64); }
.life-tag.signup { background: $primary; }
.life-tag.ongoing { background: $success; }
.life-tag.closed { background: $warning; }
.life-tag.ended { background: rgba(23, 35, 33, .45); }
.content { padding: 28rpx 30rpx 30rpx; }
.title-row { align-items: flex-start; }
.title { min-width: 0; color: $text-main; font-size: 34rpx; font-weight: 750; line-height: 1.35; word-break: break-word; }
.arrow { flex-shrink: 0; margin-top: -4rpx; color: $primary; font-size: 46rpx; line-height: 1; }
.meta-row { display: flex; align-items: center; min-width: 0; margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid $border; gap: 14rpx; }
.host, .deadline { min-width: 0; color: $text-muted; font-size: 23rpx; line-height: 1.5; }
.host { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.deadline { flex-shrink: 0; text-align: right; }
</style>
