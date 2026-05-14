<template>
  <view class="sign-in-page">
    <!-- 签到任务信息 -->
    <view class="task-card">
      <view class="task-title">{{ taskInfo.title }}</view>
      <view v-if="taskInfo.description" class="task-desc">{{ taskInfo.description }}</view>
      <view class="task-time">{{ formatTime(taskInfo.createTime) }}</view>
    </view>

    <!-- 签到信息卡片 -->
    <view class="info-card">
      <view class="info-item">
        <text class="info-label">📍 当前位置</text>
        <text class="info-value">{{ locationInfo || '正在获取...' }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">⏰ 签到时间</text>
        <text class="info-value">{{ currentTime }}</text>
      </view>
    </view>

    <!-- 拍照区域（可选） -->
    <view class="photo-section">
      <view class="section-title">📷 签到照片（可选）</view>
      <view class="photo-upload" @click="chooseImage">
        <image v-if="photoPath" :src="photoPath" mode="aspectFill" class="photo-preview"></image>
        <view v-else class="upload-placeholder">
          <text class="upload-icon">+</text>
          <text class="upload-text">点击拍照</text>
        </view>
      </view>
    </view>

    <!-- 备注区域（可选） -->
    <view class="remark-section">
      <view class="section-title">✏️ 签到备注（可选）</view>
      <textarea 
        v-model="remark" 
        class="remark-input" 
        placeholder="请输入签到备注..."
        maxlength="200"
      ></textarea>
    </view>

    <!-- 签到按钮 -->
    <view class="btn-container">
      <button 
        class="sign-btn" 
        :disabled="signing" 
        @click="confirmSign"
      >
        {{ signing ? '签到中...' : '确认签到' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'

const taskId = ref(null)
const taskInfo = ref({})
const locationInfo = ref('正在获取位置...')
const currentTime = ref('')
const photoPath = ref('')
const remark = ref('')
const signing = ref(false)
const latitude = ref(null)
const longitude = ref(null)

onLoad((options) => {
  taskId.value = options.taskId
  if (!taskId.value) {
    uni.showToast({ title: '任务ID无效', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
    return
  }
  loadTaskInfo()
  getLocation()
  updateTime()
})

onMounted(() => {
  setInterval(updateTime, 1000)
})

// 加载签到任务信息
async function loadTaskInfo() {
  try {
    const res = await request.get('/signTask/detail', { id: taskId.value })
    const data = res.data || res
    taskInfo.value = {
      title: data.title || '签到任务',
      description: data.description || '',
      createTime: data.createTime
    }
  } catch (e) {
    console.error('加载任务信息失败:', e)
    uni.showToast({ title: '加载任务信息失败', icon: 'none' })
  }
}

// 高德地图 Web 服务 API Key
const AMAP_KEY = 'a3dca7a0a33ed56dbee3ab27d0d89ece'

// 获取当前位置（高精度模式）
function getLocation() {
  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,              // 启用高精度定位（优先使用GPS）
    highAccuracyExpireTime: 10000,     // 高精度定位超时时间（毫秒）
    altitude: true,                    // 获取海拔信息（增加精度）
    geocode: true,                     // 获取地址信息
    success: async (res) => {
      console.log('✅ 高精度位置获取成功:', res)
      console.log('📍 定位精度约:', res.accuracy ? res.accuracy + '米' : '未知')
      
      latitude.value = res.latitude
      longitude.value = res.longitude
      
      // 使用逆地理编码获取详细地址
      try {
        const address = await reverseGeocode(res.latitude, res.longitude)
        locationInfo.value = address
      } catch (e) {
        // 如果逆地理编码失败，使用原始数据
        if (res.address) {
          locationInfo.value = res.address
        } else {
          locationInfo.value = `${res.latitude.toFixed(6)}, ${res.longitude.toFixed(6)}`
        }
      }
    },
    fail: (err) => {
      console.error('❌ 高精度定位失败，尝试普通定位:', err)
      // 高精度失败时尝试普通定位
      uni.getLocation({
        type: 'gcj02',
        geocode: true,
        success: async (res) => {
          latitude.value = res.latitude
          longitude.value = res.longitude
          
          try {
            const address = await reverseGeocode(res.latitude, res.longitude)
            locationInfo.value = address
          } catch (e) {
            locationInfo.value = res.address || `${res.latitude.toFixed(6)}, ${res.longitude.toFixed(6)}`
          }
        },
        fail: (err2) => {
          console.error('获取位置失败:', err2)
          locationInfo.value = '获取位置失败（不影响签到）'
        }
      })
    }
  })
}

// 逆地理编码：将经纬度转换为精确地址
async function reverseGeocode(lat, lng) {
  return new Promise((resolve, reject) => {
    const url = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${lng},${lat}&extensions=all&radius=1000`
    
    uni.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (res.data.status === '1' && res.data.regeocode) {
          const addressComponent = res.data.regeocode.addressComponent || {}
          const city = addressComponent.city || addressComponent.province || ''
          const district = addressComponent.district || ''
          const township = addressComponent.township || '' // 街道/乡镇（关键！）
          const neighborhood = addressComponent.neighborhood?.name || ''
          const street = addressComponent.streetNumber?.street || ''
          const number = addressComponent.streetNumber?.number || ''
          
          // 拼接详细地址，包含街道信息
          let detailedAddress = `${city}${district}${township}`
          if (street) detailedAddress += street
          if (number) detailedAddress += number
          if (neighborhood) detailedAddress += neighborhood
          
          console.log('📍 逆地理编码成功:', {
            区: district,
            街道: township,
            详细地址: detailedAddress
          })
          
          resolve(detailedAddress || res.data.regeocode.formatted_address || '未知位置')
        } else {
          reject(new Error('逆地理编码失败'))
        }
      },
      fail: (err) => {
        console.error('逆地理编码请求失败:', err)
        reject(err)
      }
    })
  })
}

// 更新当前时间
function updateTime() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')
  
  currentTime.value = `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

// 选择图片
function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      photoPath.value = res.tempFilePaths[0]
    }
  })
}

// 确认签到
async function confirmSign() {
  signing.value = true
  
  const userId = uni.getStorageSync('userId')
  if (!userId) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  
  try {
    const payload = {
      taskId: taskId.value,
      userId: userId,
      latitude: latitude.value,
      longitude: longitude.value,
      address: locationInfo.value,
      photo: photoPath.value,
      remark: remark.value
    }
    
    await request.post('/signRecord/sign', payload)
    
    uni.showToast({ 
      title: '签到成功', 
      icon: 'success',
      duration: 2000
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 2000)
    
  } catch (e) {
    console.error('签到失败:', e)
    uni.showToast({ 
      title: e.message || '签到失败，请重试', 
      icon: 'none' 
    })
    signing.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const time = new Date(timeStr)
  if (isNaN(time.getTime())) return timeStr
  
  const year = time.getFullYear()
  const month = String(time.getMonth() + 1).padStart(2, '0')
  const day = String(time.getDate()).padStart(2, '0')
  const hour = String(time.getHours()).padStart(2, '0')
  const minute = String(time.getMinutes()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hour}:${minute}`
}
</script>

<style scoped>
.sign-in-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 16px;
}

.task-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.task-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.task-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
}

.task-time {
  font-size: 12px;
  color: #999;
}

.info-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.photo-section,
.remark-section {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 15px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
}

.photo-upload {
  width: 100%;
  height: 200px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.upload-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
}

.remark-input {
  width: 100%;
  min-height: 100px;
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background-color: #fafafa;
}

.btn-container {
  padding: 20px 0;
}

.sign-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sign-btn[disabled] {
  opacity: 0.6;
}
</style>

