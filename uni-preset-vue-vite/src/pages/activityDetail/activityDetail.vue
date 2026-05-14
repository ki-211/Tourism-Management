<template>
  <view class="container">
    <view class="title">{{ activity.title }}</view>

    <view class="section">📅 活动时间：{{ activity.startTime }} ~ {{ activity.endTime }}</view>
    <view class="section">📝 报名时间：{{ activity.signupStart }} ~ {{ activity.signupEnd }}</view>
    <view class="section">📍 活动地点：{{ activity.location }}</view>
    <view class="section">💰 费用规则：{{ activity.feeRule }}</view>
    <view class="section">👤 发布人：{{ creatorName || activity.creatorId }}</view>
    <view class="section">📝 活动说明：</view>
    <view class="desc">{{ activity.description }}</view>

    <view class="section">🖼 活动图册：</view>
    <u-album :urls="galleryUrls" />

    <view v-if="hasSignedUp" class="upload-row">
      <u-upload :max-count="1" :file-list="uploadList" @afterRead="afterReadPhoto" />
    </view>

    <view class="btn-group">
      <u-button type="primary" class="action-btn" @click="goSignup">前往报名</u-button>
      <u-button v-if="hasSignedUp" type="success" class="action-btn" @click="goActivityRoom">进入活动室</u-button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import request, { baseURL } from '@/utils/request';

const activity = ref({});
const activityId = ref(null);
const creatorName = ref('');
const galleryUrls = ref([]);
const uploadList = ref([]);
const hasSignedUp = ref(false);

onLoad((options) => {
  activityId.value = options.id;
  loadActivity();
  loadGallery();
  checkSignup();
});

function loadActivity() {
  request.get('/activity/' + activityId.value)
      .then(res => {
        const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
        activity.value = packet ? packet.data : (res && res.data !== undefined ? res.data : res)
        if (activity.value && activity.value.creatorId) {
          request.get(`/auth/info?userId=${activity.value.creatorId}`).then(info => {
            const ipacket = (info && info.code !== undefined) ? info : ((info && info.data && info.data.code !== undefined) ? info.data : null)
            const idata = ipacket ? ipacket.data : (info && info.data !== undefined ? info.data : info)
            creatorName.value = idata && (idata.nickname || idata.username) || ''
          })
        }
      })
      .catch(err => {
        console.error('加载活动详情失败:', err);
      });
}

async function loadGallery() {
  try {
    const res = await request.get('/album/list/' + activityId.value)
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    galleryUrls.value = (arr || []).map(i => i.url || i.imageUrl || i)
  } catch (e) {
    galleryUrls.value = []
  }
}

function checkSignup() {
  request.get('/signup/list/' + activityId.value).then(res => {
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    const uid = uni.getStorageSync('userId');
    hasSignedUp.value = Array.isArray(arr) ? arr.some(i => Number(i.userId) === Number(uid)) : false
  }).catch(() => {
    hasSignedUp.value = false
  })
}

function afterReadPhoto(event) {
  const file = event.file
  const token = uni.getStorageSync('token')
  uni.uploadFile({
    url: `${baseURL}/upload/image`,
    filePath: file.url,
    name: 'file',
    header: token ? { Authorization: 'Bearer ' + token } : {},
    success(res) {
      let data = {}
      try { data = JSON.parse(res.data) } catch (e) {}
      const url = data.url || data.data || ''
      if (!url) {
        uni.showToast({ title: '上传失败', icon: 'none' })
        return
      }
      request.post('/album/add', {
        activityId: Number(activityId.value),
        userId: Number(uni.getStorageSync('userId')),
        url
      }).then(() => {
        uploadList.value = []
        uni.showToast({ title: '上传成功', icon: 'success' })
        loadGallery()
      }).catch(() => {
        uni.showToast({ title: '保存失败', icon: 'none' })
      })
    },
    fail() {
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  })
}

function goSignup() {
  uni.navigateTo({
    url: '/pages/signupList/signupList?id=' + activityId.value
  });
}

function goActivityRoom() {
  uni.navigateTo({
    url: '/pages/activityRoom/activityRoom?id=' + activityId.value
  });
}
</script>

<style scoped>
.container {
  padding: 20px;
  background-color: #fff;
}

.title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.section {
  margin-top: 10px;
  font-size: 16px;
}

.desc {
  margin-top: 5px;
  line-height: 1.6;
  color: #666;
}

.upload-row {
  margin: 12px 0;
}
.btn-group {
  margin-top: 30px;
  display: flex;
  gap: 12px;
}
.action-btn {
  flex: 1;
}
</style>
