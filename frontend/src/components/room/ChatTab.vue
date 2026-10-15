<template>
  <view class="room-tab">
    <view v-if="loading" class="loading-stack"><view v-for="item in 4" :key="item" class="skeleton-card message-skeleton"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <scroll-view v-else id="chatScroll" scroll-y class="scroll" :scroll-into-view="scrollTarget" :scroll-with-animation="false" :upper-threshold="60" @scrolltoupper="loadEarlier">
      <view v-if="loadingEarlier" class="history-hint">正在加载更早的消息…</view>
      <view v-else-if="!hasMore && messages.length" class="history-hint">没有更早的消息了</view>
      <view v-for="message in messages" :id="`msg-${message.id}`" :key="message.id" class="bubble" :class="message.userId === me?.id ? 'mine' : ''">
        <view class="meta">{{ message.nickname }} · {{ displayTime(message.createdAt) }}</view><view>{{ message.content }}</view>
      </view>
      <view v-if="!messages.length" class="empty">还没有消息，来打个招呼吧</view>
      <view id="chat-end" class="scroll-end"></view>
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

const PAGE_SIZE = 50
const CATCH_UP_SIZE = 100
const props = defineProps<{ activityId: number; refreshKey: number }>()
const messages = ref<Message[]>([]), content = ref(''), me = currentUser()
const loading = ref(true), error = ref(''), sending = ref(false)
const hasMore = ref(false), loadingEarlier = ref(false)
const scrollTarget = ref('')
const base = () => `/activities/${props.activityId}/messages`

function scrollTo(id: string) {
  scrollTarget.value = ''
  nextTick(() => { scrollTarget.value = id })
}

function merge(incoming: Message[]) {
  const byId = new Map(messages.value.map(message => [message.id, message]))
  incoming.forEach(message => byId.set(message.id, message))
  messages.value = [...byId.values()].sort((a, b) => a.id - b.id)
}

// Initial load shows the newest page; older pages are fetched on demand when scrolling up.
async function load(showLoading = true) {
  if (showLoading) loading.value = true
  error.value = ''
  try {
    const page = await api<Message[]>(`${base()}?limit=${PAGE_SIZE}`)
    messages.value = page
    hasMore.value = page.length === PAGE_SIZE
  }
  catch (reason: any) { error.value = reason?.message || '消息加载失败'; messages.value = []; return }
  finally { loading.value = false }
  await nextTick()
  scrollTo('chat-end')
}

// Realtime notifications and reconnects only fetch messages newer than the last one we have.
async function catchUp() {
  if (!messages.value.length || error.value) return load(false)
  try {
    for (;;) {
      const afterId = messages.value[messages.value.length - 1].id
      const page = await api<Message[]>(`${base()}?afterId=${afterId}&limit=${CATCH_UP_SIZE}`, 'GET', undefined, { silent: true })
      merge(page)
      if (page.length < CATCH_UP_SIZE) break
    }
  } catch { return }
  await nextTick()
  scrollTo('chat-end')
}

async function loadEarlier() {
  if (!hasMore.value || loadingEarlier.value || !messages.value.length) return
  loadingEarlier.value = true
  const anchor = messages.value[0].id
  try {
    const page = await api<Message[]>(`${base()}?beforeId=${anchor}&limit=${PAGE_SIZE}`)
    merge(page)
    hasMore.value = page.length === PAGE_SIZE
    await nextTick()
    scrollTo(`msg-${anchor}`)
  } catch { return }
  finally { loadingEarlier.value = false }
}

watch(() => props.refreshKey, (_, previous) => (previous === undefined ? load(true) : catchUp()), { immediate: true })

async function send() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    const sent = await api<Message>(base(), 'POST', { content: content.value })
    content.value = ''
    merge([sent])
    await nextTick()
    scrollTo('chat-end')
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
.history-hint { padding: 12rpx 0; color: $text-muted; font-size: 21rpx; text-align: center; }
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
