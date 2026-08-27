<template>
  <view class="page">
    <view class="page-head"><text class="eyebrow">TRAVEL MEMBERS</text><view class="page-title">参与者名单</view><text class="muted">{{ loading ? '正在加载成员…' : `共 ${members.length} 位参与者` }}</text></view>
    <view v-if="loading" class="loading-stack"><view v-for="item in 3" :key="item" class="skeleton-card"></view></view>
    <LoadError v-else-if="error" :message="error" @retry="load" />
    <view v-for="member in members" v-else :key="member.userId" class="card member-card">
      <view class="member-main">
        <view class="member-avatar">{{ member.nickname?.slice(0, 1) || '友' }}</view>
        <view class="member-copy"><view class="section-title">{{ member.nickname }}</view><text class="muted">@{{ member.username }} · {{ member.grade || '未填写身份或部门' }}</text></view>
      </view>
      <view class="member-details">
        <view><text class="detail-label">同行人数</text><text class="detail-value">{{ member.passengerCount ?? '未填写' }}</text></view>
        <view><text class="detail-label">备注</text><text class="detail-value">{{ member.remark || '无' }}</text></view>
      </view>
      <button class="transfer-btn" :disabled="transferring" @click="transfer(member.userId)">转让负责人</button>
    </view>
    <view v-if="!loading && !error && !members.length" class="empty">暂无参与者</view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LoadError from '@/components/LoadError.vue'
import { api } from '@/services/api'
import type { Member } from '@/services/types'
const id = ref(0)
const members = ref<Member[]>([])
const loading = ref(false)
const transferring = ref(false)
const error = ref('')
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
.member-main{display:flex;align-items:center;min-width:0;gap:18rpx}.member-avatar{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:74rpx;height:74rpx;color:#fff;font-size:29rpx;font-weight:800;background:linear-gradient(145deg,$primary,#47aaa0);border-radius:24rpx 24rpx 24rpx 8rpx}.member-copy{flex:1;min-width:0}.member-copy .section-title{margin-bottom:3rpx}.member-details{display:flex;margin-top:22rpx;padding:20rpx;background:#f7faf9;border-radius:18rpx;gap:24rpx}.member-details>view{flex:1;min-width:0}.detail-label,.detail-value{display:block}.detail-label{margin-bottom:5rpx;color:$text-muted;font-size:22rpx}.detail-value{color:$text-secondary;font-size:25rpx;line-height:1.5;word-break:break-word}.transfer-btn{width:100%;height:70rpx;margin-top:20rpx;color:$primary;font-size:24rpx;line-height:70rpx;background:$primary-soft;border-radius:15rpx}
</style>
