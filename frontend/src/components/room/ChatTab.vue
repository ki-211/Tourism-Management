<template><view class="room-tab"><scroll-view scroll-y class="scroll"><view v-for="m in messages" :key="m.id" class="bubble" :class="m.userId===me?.id?'mine':''"><view class="meta">{{ m.nickname }} · {{ displayTime(m.createdAt) }}</view><view>{{ m.content }}</view></view><view v-if="!messages.length" class="empty">暂无消息</view></scroll-view><view class="composer"><input v-model="content" class="input" maxlength="1000" confirm-type="send" placeholder="说点什么…" @confirm="send" /><button size="mini" class="mini-primary" @click="send">发送</button></view></view></template>
<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { api } from '@/services/api'
import { currentUser } from '@/services/session'
import { displayTime } from '@/utils/time'
import type { Message } from '@/services/types'

const props = defineProps<{ activityId: number; refreshKey: number }>()
const messages = ref<Message[]>([]), content = ref(''), me = currentUser()
let scrollTimer: ReturnType<typeof setTimeout> | null = null

async function load() {
  messages.value = await api(`/activities/${props.activityId}/messages?afterId=0&limit=100`)
  if (scrollTimer) clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    uni.pageScrollTo({ scrollTop: 99999, duration: 0 })
  }, 30)
}
watch(() => props.refreshKey, load, { immediate: true })
onUnmounted(() => { if (scrollTimer) clearTimeout(scrollTimer) })

async function send() {
  if (!content.value.trim()) return
  await api(`/activities/${props.activityId}/messages`, 'POST', { content: content.value })
  content.value = ''
  await load()
}
</script>
<style scoped lang="scss">.scroll{height:58vh}.bubble{max-width:82%;padding:20rpx 22rpx;margin:14rpx 0;color:$text-secondary;font-size:27rpx;line-height:1.6;word-break:break-word;background:#fff;border:1rpx solid $border;border-radius:20rpx 20rpx 20rpx 6rpx;box-shadow:0 6rpx 18rpx rgba(29,65,59,.04)}.mine{margin-left:auto;background:$primary-soft;border-color:transparent;border-radius:20rpx 20rpx 6rpx 20rpx}.meta{font-size:21rpx;line-height:1.4;color:$text-muted;margin-bottom:8rpx}.composer{display:flex;align-items:center;width:100%;gap:12rpx;padding-top:16rpx}.composer .input{flex:1;min-width:0}.composer button{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:124rpx;height:82rpx;margin:0;padding:0;color:#fff;font-size:25rpx;line-height:1;white-space:nowrap;background:$primary;border-radius:18rpx}</style>
