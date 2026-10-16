<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">参与成员</text><view class="page-title">参与者名单</view><text class="muted">{{ loading ? '正在加载成员…' : `共 ${members.length} 位参与者` }}</text></view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 3" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="member in members" v-else :key="member.userId" class="card member-card">
      <view class="member-main">
        <view class="member-avatar">{{ member.nickname?.slice(0, 1) || '友' }}</view>
        <view class="member-copy">
          <text class="member-name">{{ member.nickname }}</text>
          <text class="muted">@{{ member.username }}{{ member.grade ? ` · ${member.grade}` : '' }}</text>
        </view>
        <button v-if="canTransfer(member.userId)" class="ghost-btn transfer-btn" :disabled="transferring" @click="transfer(member.userId)">转让负责人</button>
      </view>
      <!-- Only filled-in sign-up details are shown; empty ones used to repeat "未填写 / 无" on every card. -->
      <view v-if="member.passengerCount || member.remark" class="member-extra">
        <text v-if="member.passengerCount" class="chip">随行 {{ member.passengerCount }} 人</text>
        <text v-if="member.remark" class="note">{{ member.remark }}</text>
      </view>
    </view>
    <view v-if="!loading && !error && !members.length" class="empty">暂无参与者</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import { currentUser } from '@/services/session'
import type { Member } from '@/services/types'
const id = ref(0)
const members = ref<Member[]>([])
const loading = ref(false)
const transferring = ref(false)
const error = ref('')
const me = currentUser()
const canTransfer = (userId: number) => me?.id !== userId
onLoad((options: any) => { id.value = Number(options.id) })
onShow(load)
async function load() {
  if (!id.value) return
  loading.value = true
  error.value = ''
  try { members.value = await api(`/activities/${id.value}/signups`) }
  catch (reason: any) { error.value = reason?.message || '参与者名单加载失败'; members.value = [] }
  finally { loading.value = false }
}
onPullDownRefresh(async () => { try { await load() } finally { uni.stopPullDownRefresh() } })
function transfer(userId: number) {
  if (transferring.value) return
  uni.showModal({ title: '转让活动负责人', content: '转让后你将不再是活动负责人，确认继续吗？', success: async result => {
    if (!result.confirm) return
    transferring.value = true
    try {
      await api(`/activities/${id.value}/transfer`, 'POST', { newCreatorId: userId })
      uni.showToast({ title: '负责人已转让' })
      setTimeout(() => uni.navigateBack(), 500)
    } finally { transferring.value = false }
  } })
}
</script>
<style scoped lang="scss">
.member-card { padding: 26rpx 28rpx; }
.member-main { display: flex; align-items: center; min-width: 0; gap: 18rpx; }
.member-avatar { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 76rpx; height: 76rpx; color: #fff; font-size: 30rpx; font-weight: 800; background: linear-gradient(145deg, $primary, #47aaa0); border-radius: 24rpx 24rpx 24rpx 8rpx; }
.member-copy { flex: 1; min-width: 0; }
.member-name { display: block; color: $text-main; font-size: 30rpx; font-weight: 700; line-height: 1.4; }
.member-copy .muted { font-size: 23rpx; line-height: 1.5; }
.transfer-btn { height: 56rpx; padding: 0 18rpx; font-size: 22rpx; }
.member-extra { display: flex; flex-wrap: wrap; align-items: center; margin: 18rpx 0 0 94rpx; gap: 12rpx; }
.chip { padding: 6rpx 16rpx; color: $primary-dark; font-size: 22rpx; background: $primary-soft; border-radius: 999rpx; }
.note { min-width: 0; color: $text-secondary; font-size: 24rpx; line-height: 1.5; word-break: break-word; }
</style>
