<template>
  <view class="room-tab">
    <view v-if="loading" class="loading-stack"><view v-for="item in 4" :key="item" class="skeleton-card message-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <scroll-view v-else id="chatScroll" scroll-y class="scroll" :scroll-into-view="bottomId" :scroll-with-animation="false">
      <view v-for="message in messages" :key="message.id" class="bubble" :class="message.userId === me?.id ? 'mine' : ''">
        <view class="meta">{{ message.nickname }} · {{ displayTime(message.createdAt) }}</view><view>{{ message.content }}</view>
      </view>
      <view v-if="!messages.length" class="empty">还没有消息，来打个招呼吧</view>
      <view :id="bottomId" class="scroll-end"></view>
    </scroll-view>
    <view class="composer">
      <input v-model="content" class="input" maxlength="1000" confirm-type="send" :adjust-position="true" :cursor-spacing="24" placeholder="说点什么…" @confirm="send" />
      <button size="mini" class="mini-primary" :loading="sending" :disabled="sending" @click="send">发送</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { currentUser } from '@/services/session'
import { displayTime } from '@/utils/time'
import type { Message } from '@/services/types'

const props = defineProps<{ activityId: number; refreshKey: number }>()
const messages = ref<Message[]>([]), content = ref(''), me = currentUser()
const loading = ref(true), error = ref(''), sending = ref(false)
const bottomId = ref('chat-end')

function scrollToEnd() {
  bottomId.value = ''
  nextTick(() => { bottomId.value = 'chat-end' })
}

async function load(showLoading = true) {
  if (showLoading) loading.value = true
  error.value = ''
  try { messages.value = await api(`/activities/${props.activityId}/messages?afterId=0&limit=100`) }
  catch (reason: any) { error.value = reason?.message || '消息加载失败'; messages.value = []; return }
  finally { loading.value = false }
  await nextTick()
  scrollToEnd()
}
watch(() => props.refreshKey, () => load(true), { immediate: true })

async function send() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    await api(`/activities/${props.activityId}/messages`, 'POST', { content: content.value })
    content.value = ''
    await load(false)
  } finally { sending.value = false }
}
</script>
<style scoped lang="scss">
.room-tab {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}
.scroll {
  flex: 1;
  height: 0;
  min-height: 360rpx;
}
.scroll-end { width: 100%; height: 8rpx; }
.bubble {
  max-width: 82%;
  padding: 20rpx 22rpx;
  margin: 14rpx 0;
  color: $text-secondary;
  font-size: 27rpx;
  line-height: 1.6;
  word-break: break-word;
  background: #fff;
  border: 1rpx solid $border;
  border-radius: 20rpx 20rpx 20rpx 6rpx;
  box-shadow: 0 6rpx 18rpx rgba(29,65,59,.04);
}
.mine { margin-left: auto; background: $primary-soft; border-color: transparent; border-radius: 20rpx 20rpx 6rpx 20rpx; }
.meta { margin-bottom: 8rpx; color: $text-muted; font-size: 21rpx; line-height: 1.4; }
.composer {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  width: 100%;
  margin: 0 -28rpx;
  padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(244, 247, 246, .96);
  border-top: 1rpx solid $border;
  gap: 12rpx;
}
/* #ifdef H5 */
.composer { margin: 0 -24px; padding-left: 24px; padding-right: 24px; }
@media (min-width: 900px) {
  .composer { margin: 0; padding-left: 0; padding-right: 0; }
}
/* #endif */
.composer .input { flex: 1; min-width: 0; }
.composer button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 124rpx;
  height: 82rpx;
  margin: 0;
  padding: 0;
  color: #fff;
  font-size: 25rpx;
  line-height: 1;
  white-space: nowrap;
  background: $primary;
  border-radius: 18rpx;
}
.message-skeleton { width: 78%; height: 110rpx; }
.message-skeleton:nth-child(even) { margin-left: auto; }
</style>
