<template>
  <view class="container">
    <view v-if="!hasSignedUp">
      <u-form :model="form">
        <u-form-item label="年级">
          <u-input v-model="form.grade" placeholder="请输入年级" />
        </u-form-item>
        <u-form-item label="乘车人数">
          <u-input type="number" v-model="form.passengerCount" placeholder="请输入乘车人数" />
        </u-form-item>
        <u-form-item label="备注">
          <u-input type="textarea" v-model="form.remark" placeholder="请输入备注" />
        </u-form-item>
        <u-button type="primary" @click="doSignup">提交报名</u-button>
      </u-form>
    </view>

    <view v-else class="signed-up-row">
      <u-tag text="已报名" type="success" />
      <u-button
          type="primary"
          size="mini"
          plain
          class="btn-small"
          @click="goVehicleList"
      >
        查看车辆
      </u-button>
    </view>

    <view class="list">
      <view class="title-container">
        <view class="title">报名名单（仅发起人可见）</view>
        <u-button
            v-if="isCreator"
            type="primary"
            size="mini"
            plain
            class="btn-small"
            @click="goVehicleAdd"
        >
          发布车辆
        </u-button>
        <u-button
            type="warning"
            size="mini"
            plain
            class="btn-small"
            @click="goSignTask"
            style="margin-left: 16rpx"
        >
          发布签到
        </u-button>
      </view>

      <view v-if="isCreator">
        <view v-for="item in list" :key="item.id" class="signup-card">
          <view class="signup-row">
            <text class="label">用户ID：</text><text>{{ item.userId }}</text>
          </view>
          <view class="signup-row">
            <text class="label">年级：</text><text>{{ item.grade || '未填写' }}</text>
          </view>
          <view class="signup-row">
            <text class="label">乘车人数：</text><text>{{ item.passengerCount || '未填写' }}</text>
          </view>
          <view class="signup-row">
            <text class="label">备注：</text><text>{{ item.remark || '无' }}</text>
          </view>
          <view class="signup-row">
            <text class="label">签到：</text>
            <text>
              {{ (signStatus[item.userId]?.totalTasks || 0) === 0
                ? '无签到任务'
                : (signStatus[item.userId]?.signedCount || 0) + '/' + (signStatus[item.userId]?.totalTasks || 0) }}
            </text>
          </view>
          <view v-for="t in taskList" :key="t.id" class="signup-row">
            <text class="label">{{ t.title }}：</text>
            <text>{{ taskSignedMap[item.userId + '-' + t.id] ? '已签' : '未签' }}</text>
          </view>
        </view>
      </view>
      <view v-else>
        <text>报名名单仅活动发起人可见</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request from '@/utils/request';

const list = ref([]);
const form = ref({
  grade: '',
  passengerCount: '',
  remark: ''
});

const hasSignedUp = ref(false);
const isCreator = ref(false);
const activityId = ref(null);
const signStatus = ref({});
const taskList = ref([]);
const taskSignedMap = ref({});


onLoad((options) => {
  activityId.value = options.id;
  fetchList();
});

function fetchList() {
  request.get('/signup/list/' + activityId.value).then(res => {
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    list.value = arr
    const uid = uni.getStorageSync('userId');
    hasSignedUp.value = Array.isArray(arr) ? arr.some(i => i.userId === uid) : false

    request.get('/activity/' + activityId.value).then(activity => {
      const apacket = (activity && activity.code !== undefined) ? activity : ((activity && activity.data && activity.data.code !== undefined) ? activity.data : null)
      const adata = apacket ? apacket.data : (activity && activity.data !== undefined ? activity.data : activity)
      isCreator.value = adata && Number(adata.creatorId) === Number(uid);
      if (isCreator.value) {
        loadSignStatus()
        loadTasksAndStatus()
      }
    });
  });
}

async function loadSignStatus() {
  try {
    const res = await request.get('/signRecord/listByActivity', { activityId: activityId.value })
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    const map = {}
    ;(arr || []).forEach(i => { map[i.userId] = { signedCount: i.signedCount || 0, totalTasks: i.totalTasks || 0 } })
    signStatus.value = map
  } catch (e) {
    signStatus.value = {}
  }
}

async function loadTasksAndStatus() {
  try {
    const [tasksRes, statusRes] = await Promise.all([
      request.get('/signTask/listByActivity', { activityId: activityId.value }),
      request.get('/signRecord/statusByActivity', { activityId: activityId.value })
    ])
    const tPacket = (tasksRes && tasksRes.code !== undefined) ? tasksRes : ((tasksRes && tasksRes.data && tasksRes.data.code !== undefined) ? tasksRes.data : null)
    const sPacket = (statusRes && statusRes.code !== undefined) ? statusRes : ((statusRes && statusRes.data && statusRes.data.code !== undefined) ? statusRes.data : null)
    taskList.value = tPacket ? (tPacket.data || []) : (Array.isArray(tasksRes) ? tasksRes : (tasksRes && tasksRes.data ? tasksRes.data : []))
    const statusArr = sPacket ? (sPacket.data || []) : (Array.isArray(statusRes) ? statusRes : (statusRes && statusRes.data ? statusRes.data : []))
    const map = {}
    ;(statusArr || []).forEach(i => { map[`${i.userId}-${i.taskId}`] = true })
    taskSignedMap.value = map
  } catch (e) {
    taskList.value = []
    taskSignedMap.value = {}
  }
}



function doSignup() {
  const uid = uni.getStorageSync('userId');
  const payload = {
    ...form.value,
    userId: uid,
    activityId: activityId.value
  };
  request.post(`/signup/${activityId.value}`, payload).then(() => {
    uni.showToast({title: '报名成功'});
    fetchList();
  });
}

function goVehicleAdd() {
  uni.navigateTo({
    url: `/pages/vehicleAdd/vehicleAdd?id=${activityId.value}`
  });
}

function goVehicleList() {
  uni.navigateTo({
    url: `/pages/vehicleList/vehicleList?id=${activityId.value}`
  });
}

function goSignTask() {
  uni.navigateTo({
    url: `/pages/signTask/signTask?id=${activityId.value}`
  });
}
</script>

<style scoped>
.container {
  padding: 20rpx;
}

.signed-up-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.title-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 30rpx 0 20rpx;
}

.title {
  font-weight: bold;
  font-size: 30rpx;
  color: #333;
}

.btn-small {
  width: 120rpx;
  padding: 0 10rpx;
  line-height: 28rpx;
  border-radius: 12rpx;
}

/* 报名卡片 */
.signup-card {
  background-color: #fff;
  padding: 20rpx;
  margin-bottom: 20rpx;
  border-radius: 12rpx;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.1);
  border: 1rpx solid #eee;
}

.signup-row {
  display: flex;
  margin-bottom: 10rpx;
}

.label {
  width: 90rpx;
  font-weight: 600;
  color: #555;
}

.btn-group {
  display: flex;
  gap: 20rpx;
}
</style>
