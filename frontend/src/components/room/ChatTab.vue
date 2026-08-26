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
<style scoped lang="scss">.scroll{height:58vh}.bubble{max-width:78%;padding:18rpx;margin:14rpx 0;background:#fff;border-radius:16rpx}.mine{margin-left:auto;background:$primary-soft}.meta{font-size:21rpx;color:$text-muted;margin-bottom:8rpx}.composer{display:flex;gap:12rpx;padding-top:16rpx}.composer .input{flex:1}.composer button{width:120rpx;margin:0}</style>
