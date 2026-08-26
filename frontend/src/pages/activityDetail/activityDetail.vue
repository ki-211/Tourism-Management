<template>
<view v-if="activity" class="page"><image v-if="activity.coverUrl" class="hero" :src="activity.coverUrl" mode="aspectFill" /><view class="card"><view class="row-between"><view class="page-title">{{ activity.title }}</view><text class="tag">{{ activity.visibility==='PUBLIC'?'公开':'邀请制' }}</text></view><view>📍 {{ activity.location }}</view><view>🕒 {{ displayTime(activity.startTime) }} - {{ displayTime(activity.endTime) }}</view><view>👤 {{ activity.creatorName }}</view><view v-if="activity.feeRule">💰 {{ activity.feeRule }}</view><view class="description">{{ activity.description||'暂无说明' }}</view><view v-if="activity.creator&&activity.invitationCode" class="invite">邀请码：<text selectable>{{ activity.invitationCode }}</text><button size="mini" @click="rotate">换一个</button></view></view><view v-if="!activity.joined" class="card"><view class="section-title">报名信息</view><input v-model="join.grade" class="input" placeholder="年级（可选）" /><input v-model.number="join.passengerCount" class="input gap" type="number" placeholder="乘车人数（可选）" /><textarea v-model="join.remark" class="textarea gap" placeholder="备注（可选）" /><button class="primary-btn" @click="signup">提交报名</button></view><view v-else class="card actions"><button class="primary-btn" @click="room">进入活动室</button><button class="secondary-btn" @click="vehicles">车辆信息</button><button v-if="activity.creator" class="secondary-btn" @click="members">报名名单</button><button v-if="activity.creator" class="secondary-btn" @click="retryCover">{{ activity.coverUrl?'更新封面':'补传封面' }}</button></view></view>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api, upload } from '@/services/api'
import { displayTime } from '@/utils/time'
import type { Activity } from '@/services/types'

const id = ref(0)
const code = ref('')
const activity = ref<Activity | null>(null)
const join = reactive<any>({ grade: '', passengerCount: undefined, remark: '', invitationCode: '' })

onLoad((options: any) => {
  id.value = Number(options.id)
  code.value = options.code || ''
  join.invitationCode = code.value
})
onShow(load)

async function load() {
  if (!id.value) return
  const query = code.value ? `?invitationCode=${encodeURIComponent(code.value)}` : ''
  activity.value = await api(`/activities/${id.value}${query}`)
}
async function signup() {
  await api(`/activities/${id.value}/signups`, 'POST', join)
  code.value = ''
  await load()
}
async function rotate() { activity.value = await api(`/activities/${id.value}/invitation-code/rotate`, 'POST') }
function retryCover() { uni.chooseImage({ count: 1, sizeType: ['compressed'], success: async result => { activity.value = await upload(`/activities/${id.value}/cover`, result.tempFilePaths[0]); uni.showToast({ title: '封面已更新' }) } }) }
const room = () => uni.navigateTo({ url: `/pages/activityRoom/activityRoom?id=${id.value}` })
const vehicles = () => uni.navigateTo({ url: `/pages/vehicleList/vehicleList?id=${id.value}&creator=${activity.value?.creator ? 1 : 0}` })
const members = () => uni.navigateTo({ url: `/pages/signupList/signupList?id=${id.value}` })
</script>
<style scoped lang="scss">.hero{width:100%;height:360rpx;border-radius:$radius;margin-bottom:22rpx}.description{margin-top:24rpx;line-height:1.7}.invite{margin-top:22rpx;padding:18rpx;background:$primary-soft;border-radius:12rpx}.invite button{float:right}.gap{margin-top:16rpx}.actions button{width:100%}</style>
