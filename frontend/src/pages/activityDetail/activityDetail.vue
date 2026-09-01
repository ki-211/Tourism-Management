<template>
  <view v-if="activity" class="page detail-page">
    <view class="detail-layout">
      <view class="detail-main">
      <view class="hero-wrap">
      <image v-if="activity.coverUrl" class="hero" :src="activity.coverUrl" mode="aspectFill" />
      <view v-else class="hero hero-empty"><text class="hero-mark">行</text><text>一起去更远的地方</text></view>
      <text class="hero-tag">{{ activity.visibility === 'PUBLIC' ? '公开活动' : '邀请制活动' }}</text>
      <text class="life-tag" :class="life">{{ lifeLabel }}</text>
    </view>
    <view class="card overview">
      <view class="row-between heading"><view class="page-title">{{ activity.title }}</view><text v-if="activity.joined" class="tag">{{ activity.creator ? '我负责' : '已参与' }}</text></view>
      <view class="info-line"><text class="info-icon">⌖</text><text class="info-value">{{ activity.location }}</text></view>
      <view class="info-line"><text class="info-icon">◷</text><text class="info-value">{{ timeText }}</text></view>
      <view class="info-line"><text class="info-icon">◇</text><text class="info-value">由 {{ activity.creatorName }} 发起</text></view>
      <view v-if="activity.feeRule" class="notice"><text class="notice-label">费用说明</text><text>{{ activity.feeRule }}</text></view>
      <view class="description-block"><text class="section-title">活动介绍</text><text class="body-text">{{ activity.description || '发起人暂未填写活动说明。' }}</text></view>
      <view v-if="activity.creator && activity.invitationCode" class="invite">
        <view><text class="invite-label">专属邀请码</text><text class="invite-code" selectable>{{ activity.invitationCode }}</text></view>
        <button class="rotate-btn" :disabled="busy" @click="rotate">换一个</button>
      </view>
    </view>
      </view>
      <view class="detail-side">
    <view v-if="!activity.joined" class="card form-card">
      <view class="section-title">填写报名信息</view><text class="muted form-intro">补充信息，方便发起人更好地安排活动。</text>
      <view class="field"><text class="field-label">身份或部门（选填）</text><input v-model="join.grade" class="input" placeholder="例如：摄影组" /></view>
      <view class="field"><text class="field-label">随行人数（选填）</text><input v-model.number="join.passengerCount" class="input" type="number" placeholder="请输入人数" /></view>
      <view class="field"><text class="field-label">备注（选填）</text><textarea v-model="join.remark" class="textarea" placeholder="饮食偏好、特殊需求等" /></view>
      <button class="primary-btn" :loading="busy" :disabled="busy" @click="signup">提交报名</button>
    </view>
    <view v-else class="card actions">
      <button class="primary-btn" @click="room">进入活动空间</button>
      <view class="action-grid">
        <button class="secondary-btn" @click="vehicles">车辆信息</button>
        <button v-if="activity.creator" class="secondary-btn" @click="members">参与者名单</button>
        <button v-if="activity.creator" class="secondary-btn" @click="retryCover">{{ activity.coverUrl ? '更新封面' : '补传封面' }}</button>
      </view>
      <button v-if="!activity.creator" class="leave-btn" :loading="busy" :disabled="busy" @click="leaveActivity">退出活动</button>
    </view>
      </view>
    </view>
  </view>
  <view v-else-if="loading" class="page"><view class="skeleton-card detail-skeleton"></view><view class="skeleton-card"></view><view class="skeleton-card"></view></view>
  <view v-else class="page"><view class="empty">活动加载失败<button class="secondary-btn" @click="load">重新加载</button></view></view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useCurrentTime } from '@/composables/useCurrentTime'
import { api, upload } from '@/services/api'
import { activityLife, activityLifeLabel, displayTimeRange } from '@/utils/time'
import type { Activity } from '@/services/types'
import { useLocationSharingStore } from '@/stores/locationSharing'
const id = ref(0)
const code = ref('')
const activity = ref<Activity | null>(null)
const loading = ref(true)
const busy = ref(false)
const locationSharing = useLocationSharingStore()
const join = reactive<any>({ grade: '', passengerCount: undefined, remark: '', invitationCode: '' })
const now = useCurrentTime()
const life = computed(() => activity.value ? activityLife(activity.value, now.value) : 'upcoming')
const lifeLabel = computed(() => activityLifeLabel[life.value])
const timeText = computed(() => activity.value ? displayTimeRange(activity.value.startTime, activity.value.endTime, now.value) : '')
onLoad((options: any) => {
  id.value = Number(options.id)
  code.value = options.code || ''
  join.invitationCode = code.value
})
onShow(load)
async function load() {
  if (!id.value) return
  loading.value = true
  const query = code.value ? `?invitationCode=${encodeURIComponent(code.value)}` : ''
  try { activity.value = await api(`/activities/${id.value}${query}`) }
  finally { loading.value = false }
}
async function signup() {
  if (busy.value) return
  busy.value = true
  try {
    await api(`/activities/${id.value}/signups`, 'POST', join)
    code.value = ''
    await load()
  } finally { busy.value = false }
}
async function rotate() {
  if (busy.value) return
  busy.value = true
  try { activity.value = await api(`/activities/${id.value}/invitation-code/rotate`, 'POST') }
  finally { busy.value = false }
}
async function leaveActivity() {
  if (busy.value) return
  const confirmed = await new Promise<boolean>(resolve => uni.showModal({
    title: '退出活动',
    content: '退出后将无法进入活动空间，当前位置共享也会立即停止。确定退出吗？',
    confirmText: '确认退出',
    confirmColor: '#DC4C4C',
    success: result => resolve(result.confirm),
    fail: () => resolve(false)
  }))
  if (!confirmed) return
  busy.value = true
  try {
    if (locationSharing.activityId === id.value && locationSharing.sharing) await locationSharing.stop(true)
    await api(`/activities/${id.value}/signups/me`, 'DELETE')
    uni.showToast({ title: '已退出活动' })
    setTimeout(() => uni.switchTab({ url: '/pages/home/home' }), 500)
  } finally { busy.value = false }
}
function retryCover() {
  uni.chooseImage({ count: 1, sizeType: ['compressed'], success: async result => {
    activity.value = await upload(`/activities/${id.value}/cover`, result.tempFilePaths[0])
    uni.showToast({ title: '封面已更新' })
  } })
}
const room = () => uni.navigateTo({ url: `/pages/activityRoom/activityRoom?id=${id.value}` })
const vehicles = () => uni.navigateTo({ url: `/pages/vehicleList/vehicleList?id=${id.value}&creator=${activity.value?.creator ? 1 : 0}` })
const members = () => uni.navigateTo({ url: `/pages/signupList/signupList?id=${id.value}` })
</script>

<style scoped lang="scss">
.detail-page { padding-top: 0; }
.detail-skeleton { height: 400rpx; }
.hero-wrap { position: relative; height: 400rpx; margin: 0 -28rpx 24rpx; overflow: hidden; }
.hero { display: flex; width: 100%; height: 100%; }
.hero-empty { align-items: center; justify-content: center; flex-direction: column; color: rgba(255,255,255,.85); font-size: 25rpx; background: linear-gradient(145deg, #0b5f59, #55ada3); gap: 10rpx; }
.hero-mark { display: flex; align-items: center; justify-content: center; width: 82rpx; height: 82rpx; color: $primary; font-size: 38rpx; font-weight: 800; background: rgba(255,255,255,.92); border-radius: 50%; }
.hero-tag, .life-tag { position: absolute; bottom: 24rpx; padding: 10rpx 18rpx; color: #fff; font-size: 22rpx; font-weight: 650; line-height: 1.3; white-space: nowrap; border-radius: 999rpx; }
.hero-tag { right: 26rpx; background: rgba(19,44,40,.66); }
.life-tag { left: 26rpx; background: rgba(19,44,40,.66); }
.life-tag.signup { background: $primary; }
.life-tag.ongoing { background: $success; }
.life-tag.closed { background: $warning; }
.life-tag.ended { background: rgba(23, 35, 33, .45); }
.overview { overflow: visible; }
.heading { align-items: flex-start; margin-bottom: 22rpx; }
.notice { display: flex; align-items: flex-start; margin-top: 24rpx; padding: 20rpx; color: $text-secondary; font-size: 25rpx; line-height: 1.6; background: $accent-soft; border-radius: 18rpx; gap: 18rpx; }
.notice-label { flex-shrink: 0; color: $warning; font-weight: 700; white-space: nowrap; }
.description-block { margin-top: 30rpx; padding-top: 26rpx; border-top: 1rpx solid $border; }
.description-block .section-title { display: block; }
.invite { display: flex; align-items: center; justify-content: space-between; margin-top: 28rpx; padding: 22rpx; background: $primary-soft; border-radius: 18rpx; gap: 20rpx; }
.invite > view { min-width: 0; }
.invite-label, .invite-code { display: block; }
.invite-label { color: $text-muted; font-size: 22rpx; line-height: 1.4; }
.invite-code { margin-top: 5rpx; color: $primary-dark; font-size: 34rpx; font-weight: 800; line-height: 1.2; letter-spacing: 4rpx; word-break: break-all; }
.rotate-btn { flex-shrink: 0; height: 60rpx; margin: 0; padding: 0 20rpx; color: $primary; font-size: 23rpx; line-height: 60rpx; white-space: nowrap; background: #fff; border-radius: 14rpx; }
.actions { padding-top: 12rpx; }
.action-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.action-grid .secondary-btn { flex: 1; min-width: 210rpx; margin-top: 0; }
.leave-btn { width: 100%; height: 76rpx; margin-top: 26rpx; color: $danger; font-size: 25rpx; line-height: 76rpx; background: transparent; border: 1rpx solid rgba(220,76,76,.26); border-radius: 17rpx; }
/* #ifdef H5 */
@media (min-width: 900px) {
  .detail-page { max-width: 1280px; padding-top: 32px; }
  .detail-layout { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(320px, .75fr); align-items: start; gap: 28px; }
  .detail-main, .detail-side { min-width: 0; }
  .detail-side { position: sticky; top: 70px; }
  .hero-wrap { margin: 0 0 24rpx; border-radius: $radius; }
}
/* #endif */
</style>
