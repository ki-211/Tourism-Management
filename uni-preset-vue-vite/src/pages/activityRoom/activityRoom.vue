<template>
  <view class="activity-room">
    <view class="header">
      <view class="title">{{ activityTitle }}</view>
    </view>

    <view class="tabs">
      <view
        v-for="(tab, index) in tabList"
        :key="index"
        class="tab-item"
        :class="{ active: currentTab === index }"
        @click="onTabChange(index)"
      >
        {{ tab.name }}
      </view>
    </view>

    <view class="content-area">
      <!-- 聊天区 -->
      <view v-show="currentTab === 0" class="chat-section">
        <scroll-view
          scroll-y
          class="message-list"
          :scroll-top="scrollTop"
          scroll-with-animation
        >
          <view v-if="messages.length === 0" class="empty-tip">暂无消息</view>
          <view
            v-for="msg in messages"
            :key="msg.id"
            class="message-item"
            :class="{ 'my-message': msg.userId == currentUserId }"
          >
            <view class="message-header">
              <text class="nickname">{{ msg.nickname || '用户' + msg.userId }}</text>
              <text class="time">{{ formatTime(msg.createTime) }}</text>
            </view>
            <view class="message-content">{{ msg.content }}</view>
          </view>
        </scroll-view>

        <view class="input-area">
          <input
            v-model="messageInput"
            class="message-input"
            placeholder="输入消息..."
            @confirm="sendMessage"
          />
          <button
            class="send-btn"
            @click="sendMessage"
          >发送</button>
        </view>
      </view>

      <!-- 签到区 -->
      <view v-show="currentTab === 1" class="sign-section">
        <view class="section-header">
          <text class="section-title">签到任务列表</text>
          <button
            v-if="isCreator"
            class="create-sign-btn"
            @click="goCreateSign"
          >发布签到</button>
        </view>

        <scroll-view scroll-y class="sign-list">
          <view v-if="signTasks.length === 0" class="empty-tip">暂无签到任务</view>
          <view
            v-for="task in signTasks"
            :key="task.id"
            class="sign-item"
          >
            <view class="sign-header">
              <text class="sign-title">{{ task.title }}</text>
              <text class="sign-time">{{ formatTime(task.createTime) }}</text>
            </view>
            <view v-if="task.description" class="sign-desc">{{ task.description }}</view>
            <view class="sign-footer">
              <text class="sign-count">已签到: {{ task.signedCount || 0 }}人</text>
              <view class="sign-actions">
                <button
                  class="detail-btn"
                  @click.stop="goTaskDetail(task.id)"
                >查看详情</button>
                <button
                  v-if="!task.hasSigned"
                  class="sign-btn"
                  @click.stop="doSign(task.id)"
                >立即签到</button>
                <text v-else class="signed-tag">已签到</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 相册区 -->
      <view v-show="currentTab === 2" class="album-section">
        <view class="section-header">
          <text class="section-title">活动相册</text>
          <button class="create-sign-btn" @click="choosePhoto">上传图片</button>
        </view>

        <!-- 图片网格 -->
        <scroll-view scroll-y class="album-scroll">
          <view v-if="galleryUrls.length === 0" class="empty-tip">暂无图片，快来上传吧</view>
          <view class="album-grid">
            <view
              v-for="(url, index) in galleryUrls"
              :key="index"
              class="album-item"
              @click="previewImage(index)"
            >
              <image :src="url" mode="aspectFill" class="album-img" />
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 位置区 - 现代化设计 -->
      <view v-show="currentTab === 3" class="location-section-modern">
        <!-- 未开启位置时的欢迎界面 -->
        <view v-if="!locationEnabled" class="location-welcome">
          <view class="welcome-content">
            <view class="welcome-icon">📍</view>
            <text class="welcome-title">位置共享</text>
            <text class="welcome-desc">开启位置后可查看所有参与者的实时位置</text>
            <button class="modern-btn-primary" @click="requestLocation">
              <text class="btn-icon">🚀</text>
              <text class="btn-text">开启位置共享</text>
            </button>
            <button class="modern-btn-secondary" @click="showManualLocationPicker">
              <text class="btn-icon">📌</text>
              <text class="btn-text">手动选择位置</text>
            </button>
            <text class="welcome-hint">如果自动定位失败，可手动选择区域</text>
          </view>
        </view>

        <!-- 已开启位置时的地图+列表界面 -->
        <view v-else class="location-map-container">
          <!-- 全屏地图 -->
          <view class="map-fullscreen">
            <map
              id="activityMap"
              :latitude="mapCenter.latitude"
              :longitude="mapCenter.longitude"
              :markers="mapMarkers"
              :show-location="true"
              :enable-zoom="true"
              :enable-scroll="true"
              :enable-rotate="false"
              :scale="selectedUserId ? 16 : 14"
              class="map-component-full"
              @markertap="onMarkerTap"
              @updated="onMapUpdated"
              @error="onMapError"
            >
              <!-- 地图加载/获取位置中的提示 -->
              <cover-view class="map-overlay" v-if="isGettingLocation || mapMarkers.length === 0">
                <cover-view class="overlay-card">
                  <cover-view class="loading-spinner">📍</cover-view>
                  <cover-view class="loading-text">
                    {{ isGettingLocation ? '正在获取你的位置...' : '等待位置信息...' }}
                  </cover-view>
                </cover-view>
              </cover-view>

              <!-- 地图加载失败提示 -->
              <cover-view class="map-error-overlay" v-if="mapLoadError">
                <cover-view class="error-card">
                  <cover-view class="error-icon">⚠️</cover-view>
                  <cover-view class="error-text">地图加载失败</cover-view>
                  <cover-view class="error-hint">{{ mapErrorMessage }}</cover-view>
                </cover-view>
              </cover-view>
            </map>

            <!-- 地图上的控制按钮 -->
            <view class="map-controls">
              <button class="control-btn" @click="centerToMyLocation">
                <text class="control-icon">📍</text>
              </button>
              <button class="control-btn" @click="stopLocation">
                <text class="control-icon">✕</text>
              </button>
            </view>

            <!-- 参与者计数徽章 -->
            <view class="participant-count-badge">
              <text class="count-icon">👥</text>
              <text class="count-text">{{ participants.length }} 人在线</text>
            </view>
          </view>

          <!-- 底部抽屉式用户列表 -->
          <view
            class="bottom-drawer"
            :class="{ 'drawer-expanded': drawerExpanded }"
            @touchstart="onDrawerTouchStart"
            @touchmove="onDrawerTouchMove"
            @touchend="onDrawerTouchEnd"
          >
            <!-- 抽屉把手 -->
            <view class="drawer-handle-container" @click="toggleDrawer">
              <view class="drawer-handle"></view>
            </view>

            <!-- 抽屉标题 -->
            <view class="drawer-header">
              <text class="drawer-title">参与者位置</text>
              <text class="drawer-subtitle">点击卡片查看详情</text>
            </view>

            <!-- 用户列表 -->
            <scroll-view
              scroll-y
              class="drawer-content"
              :style="{ maxHeight: drawerExpanded ? '60vh' : '35vh' }"
            >
              <view v-if="participants.length === 0" class="empty-state">
                <text class="empty-icon">🔍</text>
                <text class="empty-text">暂无其他参与者位置信息</text>
              </view>

              <view
                v-for="participant in participants"
                :key="participant.userId"
                class="modern-participant-card"
                :class="{ 'card-active': selectedUserId === participant.userId }"
                @click="onParticipantClick(participant)"
              >
                <view class="card-left">
                  <view
                    class="modern-avatar"
                    :class="{ 'avatar-me': participant.userId == currentUserId }"
                  >
                    <text class="avatar-text">
                      {{ participant.nickname ? participant.nickname.charAt(0) : '用' }}
                    </text>
                    <view v-if="participant.userId == currentUserId" class="avatar-badge">我</view>
                  </view>

                  <view class="card-info">
                    <view class="info-row">
                      <text class="user-name">
                        {{ participant.nickname || '用户' + participant.userId }}
                      </text>
                      <text
                        v-if="participant.userId == currentUserId"
                        class="badge-me"
                      >当前位置</text>
                    </view>

                    <!-- 地址展示区域 -->
                    <view class="address-container">
                      <text class="user-address">
                        {{ participant.address || '位置未知' }}
                      </text>
                    </view>

                    <text class="user-time">{{ formatTime(participant.updateTime) }}</text>
                  </view>
                </view>

                <view class="card-right">
                  <view class="distance-badge">
                    <text class="distance-icon">📏</text>
                    <text class="distance-text">{{ getDistanceFromMe(participant) }}</text>
                  </view>
                  <view class="nav-icon">→</view>
                </view>
              </view>
            </scroll-view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import request, { baseURL } from '@/utils/request'
import { AMAP_WEB_KEY as AMAP_KEY } from '@/utils/mapConfig'

const activityId = ref(null)
const activityTitle = ref('活动室')
const currentUserId = ref(null)
const isCreator = ref(false)
const currentTab = ref(0)
const tabList = ref([
  { name: '聊天' },
  { name: '签到' },
  { name: '相册' },
  { name: '位置' }
])

const messages = ref([])
const messageInput = ref('')
const scrollTop = ref(0)

const signTasks = ref([])

// 相册相关
const galleryUrls = ref([])

// 位置相关
const participants = ref([])
const currentLocation = ref(null)
const locationEnabled = ref(false)
const showMap = ref(false)
const mapMarkers = ref([])
// 初始化为null，等待真实位置
const mapCenter = ref({ latitude: 39.9042, longitude: 116.4074 }) // 北京作为临时中心
const selectedUserId = ref(null) // 选中的用户ID
const drawerExpanded = ref(false) // 抽屉是否展开
const isGettingLocation = ref(false) // 正在获取位置
const mapLoadError = ref(false) // 地图加载错误
const mapErrorMessage = ref('') // 错误信息
const expandedAddressMap = ref({}) // 记录每个参与者地址的展开状态 { userId: true/false }
let locationUploadTimer = null
let drawerStartY = 0 // 抽屉滑动起始Y坐标
let drawerCurrentY = 0 // 抽屉滑动当前Y坐标
let mapContext = null // 地图上下文

let messageTimer = null
let signTaskTimer = null
let locationTimer = null

onLoad(async (options) => {
  activityId.value = options.id ? Number(options.id) : null
  currentUserId.value = uni.getStorageSync('userId')

  if (!currentUserId.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/login/login' })
    }, 1500)
    return
  }

  if (!activityId.value) {
    uni.showToast({ title: '活动ID无效', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
    return
  }

  // 验证用户是否已报名该活动
  try {
    const signupRes = await request.get('/signup/list/' + activityId.value)
    const packet = (signupRes && signupRes.code !== undefined) ? signupRes : ((signupRes && signupRes.data && signupRes.data.code !== undefined) ? signupRes.data : null)
    const signupList = packet ? (packet.data || []) : (Array.isArray(signupRes) ? signupRes : (signupRes && signupRes.data ? signupRes.data : []))
    const hasSignedUp = Array.isArray(signupList) && signupList.some(s => Number(s.userId) === Number(currentUserId.value))

    if (!hasSignedUp) {
      uni.showModal({
        title: '无法进入',
        content: '您还未报名该活动，无法进入活动室',
        showCancel: false,
        success: () => {
          uni.navigateBack()
        }
      })
      return
    }
  } catch (e) {
    console.error('验证报名状态失败:', e)
    uni.showModal({
      title: '提示',
      content: '验证报名状态失败，请稍后重试',
      showCancel: false,
      success: () => {
        uni.navigateBack()
      }
    })
    return
  }

  loadActivityInfo()
  loadMessages()
  loadSignTasks()

  messageTimer = setInterval(loadMessages, 3000)
  signTaskTimer = setInterval(loadSignTasks, 5000)
  locationTimer = setInterval(loadParticipants, 10000)

  // 初始化地图上下文
  setTimeout(() => {
    mapContext = uni.createMapContext('activityMap')
  }, 500)
})

onShow(() => {
  if (activityId.value) {
    loadMessages()
    loadSignTasks()
  }
})

onMounted(() => {
  uni.$on('refreshActivityRoom', () => {
    loadMessages()
    loadSignTasks()
  })
})

function onTabChange(index) {
  currentTab.value = index
  if (index === 0) {
    loadMessages()
  } else if (index === 1) {
    loadSignTasks()
  } else if (index === 2) {
    loadGallery()
  } else if (index === 3) {
    loadParticipants()
    if (!locationEnabled.value) {
      requestLocation()
    }
  }
}

async function loadActivityInfo() {
  try {
    const res = await request.get('/activity/' + activityId.value)
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const data = packet ? packet.data : (res && res.data !== undefined ? res.data : res)
    if (data) {
      activityTitle.value = data.title || '活动室'
      isCreator.value = Number(data.creatorId) === Number(currentUserId.value)
    }
  } catch (e) {
    console.error('加载活动信息失败:', e)
  }
}

async function loadMessages() {
  if (!activityId.value) {
    console.error('活动ID为空，无法加载消息')
    return
  }
  try {
    const res = await request.get('/chat/list', {
      activityId: activityId.value,
      userId: currentUserId.value
    })
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)

    // 处理权限错误
    if (packet && packet.code !== 200 && packet.msg && packet.msg.includes('未报名')) {
      console.error('权限验证失败:', packet.msg)
      return
    }

    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    messages.value = Array.isArray(arr) ? arr : []

    nextTick(() => {
      scrollTop.value = 999999
    })
  } catch (e) {
    console.error('加载消息失败:', e)
  }
}

async function sendMessage() {
  if (!messageInput.value.trim()) {
    return uni.showToast({ title: '请输入消息内容', icon: 'none' })
  }

  if (!activityId.value) {
    return uni.showToast({ title: '活动ID无效', icon: 'none' })
  }

  try {
    await request.post('/chat/send', {
      activityId: activityId.value,
      userId: currentUserId.value,
      content: messageInput.value.trim()
    })

    messageInput.value = ''
    await loadMessages()
  } catch (e) {
    console.error('发送消息失败:', e)
    uni.showToast({ title: '发送失败', icon: 'none' })
  }
}

async function loadSignTasks() {
  if (!activityId.value) {
    console.error('活动ID为空，无法加载签到任务')
    return
  }
  try {
    const res = await request.get('/signTask/listByActivity', { activityId: activityId.value })
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))

    const tasks = Array.isArray(arr) ? arr : []

    for (let task of tasks) {
      try {
        const signRes = await request.get('/signRecord/list', { taskId: task.id })
        const signPacket = (signRes && signRes.code !== undefined) ? signRes : ((signRes && signRes.data && signRes.data.code !== undefined) ? signRes.data : null)
        const signArr = signPacket ? (signPacket.data || []) : (Array.isArray(signRes) ? signRes : (signRes && signRes.data ? signRes.data : []))

        task.signedCount = Array.isArray(signArr) ? signArr.length : 0
        task.hasSigned = Array.isArray(signArr) ? signArr.some(r => Number(r.userId) === Number(currentUserId.value)) : false
      } catch (e) {
        task.signedCount = 0
        task.hasSigned = false
      }
    }

    signTasks.value = tasks
  } catch (e) {
    console.error('加载签到任务失败:', e)
  }
}

function goCreateSign() {
  uni.navigateTo({
    url: `/pages/signTask/signTask?id=${activityId.value}`
  })
}

function goTaskDetail(taskId) {
  uni.navigateTo({
    url: `/pages/signTaskDetail/signTaskDetail?taskId=${taskId}`
  })
}

async function doSign(taskId) {
  // 跳转到签到页面（可以拍照、定位、填写备注）
  uni.navigateTo({
    url: `/pages/signIn/signIn?taskId=${taskId}`
  })
}

// ========== 相册相关方法 ==========
async function loadGallery() {
  if (!activityId.value) return
  try {
    const res = await request.get('/album/list/' + activityId.value)
    const packet = (res && res.code !== undefined) ? res : ((res && res.data && res.data.code !== undefined) ? res.data : null)
    const arr = packet ? (packet.data || []) : (Array.isArray(res) ? res : (res && res.data ? res.data : []))
    galleryUrls.value = (arr || []).map(i => i.url || i.imageUrl || i)
  } catch (e) {
    galleryUrls.value = []
  }
}

function choosePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success(res) {
      const filePath = res.tempFilePaths[0]
      uploadPhoto(filePath)
    }
  })
}

function uploadPhoto(filePath) {
  const token = uni.getStorageSync('token')
  uni.showLoading({ title: '上传中...' })
  uni.uploadFile({
    url: `${baseURL}/upload/image`,
    filePath: filePath,
    name: 'file',
    formData: {
      activityId: String(activityId.value),
      userId: String(currentUserId.value)
    },
    header: token ? { Authorization: 'Bearer ' + token } : {},
    success(res) {
      let data = {}
      try { data = JSON.parse(res.data) } catch (e) {}
      if (data.code !== 0) {
        uni.showToast({ title: data.msg || '上传失败', icon: 'none' })
        return
      }
      uni.showToast({ title: '上传成功', icon: 'success' })
      loadGallery()
    },
    fail() {
      uni.showToast({ title: '上传失败', icon: 'none' })
    },
    complete() {
      uni.hideLoading()
    }
  })
}

function previewImage(index) {
  uni.previewImage({
    urls: galleryUrls.value,
    current: index
  })
}

// 位置相关方法
async function requestLocation() {
  isGettingLocation.value = true
  uni.showLoading({ title: '获取位置中...' })

  // #ifdef H5
  // H5 环境
  try {
    console.log('🌐 H5环境，尝试获取位置...')

    let position = null
    let useIpLocation = false
    let isPermissionDenied = false

    // 方案A: 优先使用浏览器原生 GPS（navigator.geolocation），精度最高
    try {
      console.log('📡 方案A: 尝试浏览器原生 GPS 定位...')
      position = await new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error('浏览器不支持 Geolocation API'))
          return
        }
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            console.log('✅ 方案A 浏览器 GPS 定位成功:', pos.coords.latitude, pos.coords.longitude, '精度:', pos.coords.accuracy, 'm')
            resolve({
              coords: {
                latitude: pos.coords.latitude,
                longitude: pos.coords.longitude,
                accuracy: pos.coords.accuracy || 100
              },
              address: ''
            })
          },
          (err) => {
            reject(new Error('浏览器定位失败: ' + err.message))
          },
          {
            enableHighAccuracy: true,  // 强制使用 GPS（手机端精准）
            timeout: 12000,
            maximumAge: 30000
          }
        )
      })
    } catch (navError) {
      console.log('📍 方案A 失败:', navError.message)
      isPermissionDenied = navError.message.toLowerCase().includes('permission denied')
                        || navError.message.includes('PERMISSION_DENIED')
                        || navError.message.includes('1')
      position = null
    }

    // 方案B: 高德定位插件（禁止内部 IP 降级，只用 GPS）
    if (!position && !isPermissionDenied) {
      try {
        if (!window.AMap) {
          throw new Error('AMap SDK 未加载')
        }
        if (!AMap.Geolocation) {
          console.log('🔌 动态加载 AMap.Geolocation 插件...')
          await new Promise((resolve, reject) => {
            AMap.plugin(['AMap.Geolocation'], () => {
              if (AMap.Geolocation) resolve()
              else reject(new Error('插件加载失败'))
            })
          })
        }
        console.log('📡 方案B: 使用高德定位插件（纯 GPS 模式）...')
        position = await new Promise((resolve, reject) => {
          const geolocation = new AMap.Geolocation({
            enableHighAccuracy: true,
            timeout: 10000,
            GeoLocationFirst: true,
            noGeoLocation: 3,  // 3=禁止 IP 降级，确保是真实 GPS
            noIpLocate: 3,     // 3=禁止内部 IP 定位
            needAddress: true,
            extensions: 'all'
          })
          geolocation.getCurrentPosition((status, result) => {
            console.log('📍 高德定位返回:', status, result?.location_type)
            if (status === 'complete') {
              console.log('✅ 方案B 高德定位成功:', result.position.lat, result.position.lng, '精度:', result.accuracy, '类型:', result.location_type)
              resolve({
                coords: {
                  latitude: result.position.lat,
                  longitude: result.position.lng,
                  accuracy: result.accuracy || 100
                },
                address: result.formattedAddress || ''
              })
            } else {
              reject(new Error(result?.message || result?.info || '高德定位失败'))
            }
          })
        })
      } catch (geoError) {
        console.log('📍 方案B 失败:', geoError.message)
        position = null
      }
    }

    // 若 A、B 均失败，走 IP 兜底链路
    if (!position) {
      useIpLocation = true
      if (isPermissionDenied) {
        console.log('🚫 定位权限被拒绝，直接走 IP 定位')
      }
      // 若仍未获取到位置，走 CitySearch -> 高德 IP -> 备用 IP 链路
      if (!position) {
        // 方案C: 使用 AMap.CitySearch 获取城市级位置
        try {
          if (window.AMap) {
            await new Promise((resolve, reject) => {
              AMap.plugin(['AMap.CitySearch'], () => {
                if (AMap.CitySearch) resolve()
                else reject(new Error('CitySearch 插件加载失败'))
              })
            })

            const cityResult = await new Promise((resolve, reject) => {
              const citySearch = new AMap.CitySearch()
              citySearch.getLocalCity((status, result) => {
                console.log('🏤️ CitySearch 返回:', status, result)
                if (status === 'complete' && result.info === 'OK') {
                  resolve(result)
                } else {
                  reject(new Error('CitySearch 失败: ' + (result.info || status)))
                }
              })
            })

            // 从 bounds 中取中心点
            if (cityResult.bounds) {
              const center = cityResult.bounds.getCenter()
              position = {
                coords: {
                  latitude: center.lat,
                  longitude: center.lng,
                  accuracy: 5000
                }
              }
              console.log('✅ CitySearch 定位成功:', cityResult.city, center.lat, center.lng)
            } else {
              throw new Error('CitySearch 无 bounds 数据')
            }
          } else {
            throw new Error('AMap SDK 未加载')
          }
        } catch (cityErr) {
          console.log('📍 CitySearch 也失败，尝试高德 REST IP 定位...')

          // 方案D: 高德 REST API IP 定位（中国大陆 IP 可用）
          const ipLocationResult = await getLocationByAmapIP().catch(() => null)
          if (ipLocationResult) {
            position = {
              coords: {
                latitude: ipLocationResult.latitude,
                longitude: ipLocationResult.longitude,
                accuracy: 3000
              }
            }
            console.log('✅ 高德 IP 定位成功:', ipLocationResult)
          } else {
            // 方案E: 备用 IP 定位（ipapi.co，支持境外/VPN网络）
            console.log('📍 高德 IP 定位无数据，尝试备用 IP 接口 (ipapi.co)...')
            const fallbackIpResult = await getLocationByFallbackIP()
            if (fallbackIpResult) {
              position = {
                coords: {
                  latitude: fallbackIpResult.latitude,
                  longitude: fallbackIpResult.longitude,
                  accuracy: 5000
                }
              }
              console.log('✅ 备用 IP 定位成功:', fallbackIpResult)
            } else {
              throw new Error('所有自动定位均失败，将使用手动定位')
            }
          }
        }
      }
    }

    uni.hideLoading()

    const lat = position.coords.latitude
    const lng = position.coords.longitude
    const accuracy = position.coords.accuracy

    console.log('🎯 最终位置:', lat, lng, '精度:', accuracy + 'm', useIpLocation ? '(IP定位)' : '(GPS定位)')

    if (useIpLocation) {
      uni.showToast({ title: 'IP定位成功，精度约3km', icon: 'none', duration: 2000 })
    }

    // 获取实际地址（逆地理编码）
    const actualAddress = await reverseGeocode(lat, lng)

    currentLocation.value = {
      latitude: lat,
      longitude: lng,
      address: actualAddress
    }

    console.log('📍 解析后的地址:', actualAddress)

    mapCenter.value = { latitude: lat, longitude: lng }

    locationEnabled.value = true
    isGettingLocation.value = false

    uni.showToast({
      title: `已定位到\n${currentLocation.value.address}`,
      icon: 'success',
      duration: 2500
    })

    await updateLocation()
    await loadParticipants()
    updateMarkersFromParticipants()

    setTimeout(() => {
      if (mapContext) {
        mapContext.moveToLocation({ latitude: lat, longitude: lng })
      }
    }, 500)

    startLocationAutoUpdate()

  } catch (e) {
    uni.hideLoading()
    isGettingLocation.value = false
    console.error('❌ 获取位置失败:', e)
    uni.showToast({ title: '自动定位失败，请手动选择位置', icon: 'none', duration: 1500 })
    setTimeout(() => showManualLocationPicker(), 1600)
  }
  // #endif

  // #ifndef H5
  // 非 H5 环境（小程序等）
  try {
    console.log('🌐 使用 uni.getLocation 获取位置...')

    const res = await new Promise((resolve, reject) => {
      uni.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        highAccuracyExpireTime: 10000,
        altitude: true,
        geocode: true,
        success: (loc) => {
          console.log('✅ 高精度位置获取成功，原始数据:', loc)
          resolve(loc)
        },
        fail: (err) => {
          console.error('❌ 高精度位置获取失败，尝试普通定位:', err)
          uni.getLocation({
            type: 'gcj02',
            geocode: true,
            success: resolve,
            fail: reject
          })
        }
      })
    })

    uni.hideLoading()

    const lat = res.latitude
    const lng = res.longitude

    console.log('🎯 获取到的真实位置(uni):', lat, lng)

    let actualAddress = res.address || ''
    if (!actualAddress) {
      actualAddress = await reverseGeocode(lat, lng)
    }

    currentLocation.value = {
      latitude: lat,
      longitude: lng,
      address: actualAddress
    }

    console.log('📍 解析后的地址:', actualAddress)

    mapCenter.value = { latitude: lat, longitude: lng }

    locationEnabled.value = true
    isGettingLocation.value = false

    uni.showToast({
      title: `已定位到\n${currentLocation.value.address}`,
      icon: 'success',
      duration: 2500
    })

    await updateLocation()
    await loadParticipants()
    updateMarkersFromParticipants()

    setTimeout(() => {
      if (mapContext) {
        mapContext.moveToLocation({ latitude: lat, longitude: lng })
      }
    }, 500)

    startLocationAutoUpdate()

  } catch (e) {
    uni.hideLoading()
    isGettingLocation.value = false
    console.error('❌ 获取位置失败:', e)

    if (e.errMsg && e.errMsg.indexOf('auth') !== -1) {
      uni.showModal({
        title: '位置权限',
        content: '需要获取位置权限才能使用位置功能，请在设置中开启位置权限',
        confirmText: '去设置',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.openSetting({
              success: (settingRes) => {
                if (settingRes.authSetting['scope.userLocation']) {
                  uni.showToast({ title: '请重新点击开启位置', icon: 'none' })
                }
              }
            })
          }
        }
      })
    } else {
      uni.showModal({
        title: '定位失败',
        content: '无法获取位置信息，请确保：\n1. 已开启手机GPS定位\n2. 网络连接正常\n3. 在户外或窗边信号较好的地方',
        showCancel: false,
        confirmText: '我知道了'
      })
    }
  }
  // #endif
}

// 手动设置位置
function manualSetLocation() {
  // 深圳各区预设坐标
  const shenzhenDistricts = [
    { name: '坪山区', lat: 22.7089, lng: 114.3500 },
    { name: '福田区', lat: 22.5431, lng: 114.0579 },
    { name: '南山区', lat: 22.5329, lng: 113.9300 },
    { name: '龙岗区', lat: 22.7210, lng: 114.2474 },
    { name: '罗湖区', lat: 22.5485, lng: 114.1312 },
    { name: '宝安区', lat: 22.5544, lng: 113.8836 },
    { name: '龙华区', lat: 22.6569, lng: 114.0297 },
    { name: '盐田区', lat: 22.5569, lng: 114.2361 },
    { name: '光明区', lat: 22.7495, lng: 113.9388 },
    { name: '大鹏新区', lat: 22.5942, lng: 114.4785 }
  ]

  uni.showActionSheet({
    itemList: shenzhenDistricts.map(d => `深圳${d.name}`),
    success: async (res) => {
      const selected = shenzhenDistricts[res.tapIndex]

      console.log('🎯 用户选择:', selected.name)

      // 更新本地状态
      currentLocation.value = {
        latitude: selected.lat,
        longitude: selected.lng,
        address: `深圳市${selected.name}`
      }

      mapCenter.value = {
        latitude: selected.lat,
        longitude: selected.lng
      }

      locationEnabled.value = true

      uni.showLoading({ title: '设置位置中...' })

      try {
        // 上传到服务器
        await updateLocation()
        await loadParticipants()
        updateMarkersFromParticipants()

        // 使用 MapContext 移动
        setTimeout(() => {
          if (mapContext) {
            mapContext.moveToLocation({
              latitude: selected.lat,
              longitude: selected.lng
            })
          }
        }, 500)

        uni.hideLoading()
        uni.showToast({
          title: `位置已设置为深圳${selected.name}`,
          icon: 'success',
          duration: 2000
        })

        console.log('✅ 位置设置成功:', selected)

        // 开始自动更新
        startLocationAutoUpdate()

      } catch (e) {
        uni.hideLoading()
        uni.showToast({
          title: '设置失败: ' + e.message,
          icon: 'none'
        })
        console.error('❌ 设置位置失败:', e)
      }
    }
  })
}

function stopLocation() {
  // 停止自动上传并标记为未开启
  if (locationUploadTimer) {
    clearInterval(locationUploadTimer)
    locationUploadTimer = null
  }
  locationEnabled.value = false
  currentLocation.value = null
  uni.showToast({ title: '位置已关闭', icon: 'none' })
}

function startLocationAutoUpdate() {
  // 如果已经在运行则不重复启动
  if (locationUploadTimer) return

  // 立即执行一次，然后每15秒上传一次
  const uploadOnce = async () => {
    // #ifdef H5
    try {
      let lat, lng

      try {
        // 使用高德定位插件（GPS 优先）
        const pos = await new Promise((resolve, reject) => {
          if (window.AMap && window.AMap.Geolocation) {
            const geolocation = new AMap.Geolocation({
              enableHighAccuracy: true,
              timeout: 8000,
              GeoLocationFirst: true,
              noGeoLocation: 0,
              noIpLocate: 0,
              needAddress: false,
              extensions: 'base'
            })
            geolocation.getCurrentPosition((status, result) => {
              if (status === 'complete') {
                resolve({ coords: { latitude: result.position.lat, longitude: result.position.lng } })
              } else {
                reject(new Error(result.message || '高德定位失败'))
              }
            })
          } else {
            reject(new Error('AMap.Geolocation 未加载'))
          }
        })
        lat = pos.coords.latitude
        lng = pos.coords.longitude
      } catch (geoErr) {
        // 高德插件失败，尝试浏览器原生 GPS 定位
        try {
          const pos = await new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
              reject(new Error('不支持'))
              return
            }
            navigator.geolocation.getCurrentPosition(
              (p) => resolve(p),
              (e) => reject(e),
              { enableHighAccuracy: true, timeout: 6000, maximumAge: 60000 }
            )
          })
          lat = pos.coords.latitude
          lng = pos.coords.longitude
        } catch (navErr) {
          // 浏览器定位也失败，使用高德 IP 定位
          console.log('📍 自动更新: 定位失败，使用高德 IP 定位')
          const ipResult = await getLocationByAmapIP()
          lat = ipResult.latitude
          lng = ipResult.longitude
        }
      }

      const address = await reverseGeocode(lat, lng)

      currentLocation.value = {
        latitude: lat,
        longitude: lng,
        address: address
      }
      await updateLocation()
      await loadParticipants()
    } catch (e) {
      console.error('自动获取/上传位置失败:', e)
    }
    // #endif

    // #ifndef H5
    try {
      const res = await new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          isHighAccuracy: true,
          highAccuracyExpireTime: 10000,
          geocode: true,
          success: resolve,
          fail: (err) => {
            console.warn('⚠️ 高精度定位失败，尝试普通定位')
            uni.getLocation({ type: 'gcj02', geocode: true, success: resolve, fail: reject })
          }
        })
      })
      const lat = res.latitude
      const lng = res.longitude
      let address = res.address || ''
      if (!address) {
        address = await reverseGeocode(lat, lng)
      }

      currentLocation.value = {
        latitude: lat,
        longitude: lng,
        address: address
      }
      await updateLocation()
      await loadParticipants()
    } catch (e) {
      console.error('自动获取/上传位置失败:', e)
    }
    // #endif
  }

  uploadOnce()
  locationUploadTimer = setInterval(uploadOnce, 15000)
}

function updateMarkersFromParticipants() {
  // 生成 map markers
  const markers = []
  for (const p of participants.value) {
    if (p.latitude && p.longitude) {
      markers.push({
        id: Number(p.userId || p.user_id || 0),
        latitude: Number(p.latitude),
        longitude: Number(p.longitude),
        width: 34,
        height: 34,
        iconPath: '/static/icons/marker.svg',
        callout: {
          content: `${p.nickname || ('用户' + p.userId)}\n${p.address || '位置未知'}`,
          color: '#fff',
          fontSize: 13,
          bgColor: 'rgba(0, 0, 0, 0.75)',
          borderRadius: 8,
          padding: 8,
          display: 'BYCLICK'
        }
      })
    }
  }

  // 如果当前用户有位置，添加一个不同颜色的标注放在最前
  if (currentLocation.value && currentLocation.value.latitude && currentLocation.value.longitude) {
    markers.unshift({
      id: Number(currentUserId.value || 0) * 100000,
      latitude: Number(currentLocation.value.latitude),
      longitude: Number(currentLocation.value.longitude),
      width: 36,
      height: 36,
      iconPath: '/static/icons/marker-me.svg',
      callout: {
        content: '📍 我的位置\n' + (currentLocation.value.address || '位置未知'),
        color: '#fff',
        fontSize: 13,
        bgColor: 'rgba(33, 150, 243, 0.85)',
        borderRadius: 8,
        padding: 8,
        display: 'BYCLICK'
      }
    })
    mapCenter.value.latitude = Number(currentLocation.value.latitude)
    mapCenter.value.longitude = Number(currentLocation.value.longitude)
  } else if (markers.length > 0) {
    mapCenter.value.latitude = markers[0].latitude
    mapCenter.value.longitude = markers[0].longitude
  }

  mapMarkers.value = markers
}

function onMarkerTap(e) {
  const markerId = (e && e.markerId) || (e && e.detail && e.detail.markerId) || (e && e.detail && e.detail.markerId === 0 ? 0 : undefined)
  if (markerId === undefined) return

  const marker = mapMarkers.value.find(m => Number(m.id) === Number(markerId))
  if (!marker) return

  // 找到参与者信息
  const participant = participants.value.find(p => Number(p.userId || p.user_id) === Number(markerId) || Number(p.userId || 0) * 100000 === Number(markerId))

  const name = participant ? (participant.nickname || ('用户' + participant.userId)) : '位置'
  const address = participant ? (participant.address || '') : ''
  uni.showModal({
    title: name,
    content: address || '查看位置',
    confirmText: '导航',
    cancelText: '关闭',
    success: (res) => {
      if (res.confirm) {
        uni.openLocation({
          latitude: Number(marker.latitude),
          longitude: Number(marker.longitude),
          name: name,
          address: address,
          scale: 18
        })
      }
    }
  })
}

// 地图更新事件
function onMapUpdated(e) {
  console.log('🗺️ 地图已更新:', e)
  mapLoadError.value = false
}

// 地图错误事件
function onMapError(e) {
  console.error('❌ 地图加载错误:', e)
  mapLoadError.value = true

  // 检查坐标是否合理
  const lat = mapCenter.value.latitude
  const lng = mapCenter.value.longitude

  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    mapErrorMessage.value = '坐标超出有效范围'
  } else if (lat > 53.5 || lat < 3.5 || lng > 135 || lng < 73) {
    mapErrorMessage.value = '坐标可能超出地图服务范围'
  } else {
    mapErrorMessage.value = '请检查网络连接或刷新页面'
  }

  uni.showToast({
    title: '地图加载失败',
    icon: 'none',
    duration: 2000
  })
}

async function updateLocation() {
  if (!currentLocation.value || !activityId.value) {
    console.error('❌ 无法更新位置:', {
      hasLocation: !!currentLocation.value,
      hasActivityId: !!activityId.value
    })
    return
  }

  try {
    const uploadData = {
      activityId: activityId.value ? Number(activityId.value) : null,
      userId: currentUserId.value ? Number(currentUserId.value) : null,
      latitude: currentLocation.value.latitude !== undefined ? Number(currentLocation.value.latitude) : null,
      longitude: currentLocation.value.longitude !== undefined ? Number(currentLocation.value.longitude) : null,
      address: currentLocation.value.address || null
    }

    console.log('📤 上传位置到服务器:', uploadData)

    const res = await request.post('/location/update', uploadData)

    console.log('✅ 位置上传成功，服务器响应:', res)

    // 更新本地中心与标注
    updateMarkersFromParticipants()
  } catch (e) {
    console.error('❌ 更新位置失败:', e)
    uni.showToast({
      title: '位置上传失败: ' + e.message,
      icon: 'none',
      duration: 2000
    })
  }
}

async function loadParticipants() {
  if (!activityId.value) return

  try {
    console.log('📥 从服务器加载参与者位置...')

    const res = await request.get('/location/list', { activityId: activityId.value })
    console.log('📍 位置列表响应:', res)

    // 处理不同的响应格式
    let data = null
    if (res && res.success && res.data) {
      // 格式1: { success: true, data: [...] }
      data = res.data
    } else if (res && res.code === 0 && res.data) {
      // 格式2: { code: 0, data: [...] }
      data = res.data
    } else if (Array.isArray(res)) {
      // 格式3: 直接返回数组
      data = res
    } else if (res && res.data && Array.isArray(res.data)) {
      // 格式4: { data: [...] }
      data = res.data
    }

    participants.value = Array.isArray(data) ? data : []
    console.log('✅ 参与者位置列表:', participants.value)

    // 如果有参与者，显示详细信息
    if (participants.value.length > 0) {
      participants.value.forEach(p => {
        console.log(`  - ${p.nickname || '用户' + p.userId}: (${p.latitude}, ${p.longitude})`)
      })
    }

    // 更新地图标注
    updateMarkersFromParticipants()
  } catch (e) {
    console.error('❌ 加载参与者位置失败:', e)
    participants.value = []
  }
}

function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371 // 地球半径（公里）
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon/2) * Math.sin(dLon/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  const distance = R * c

  if (distance < 1) {
    return Math.round(distance * 1000) + 'm'
  } else {
    return distance.toFixed(1) + 'km'
  }
}

function getDistanceFromMe(participant) {
  if (!currentLocation.value || !participant.latitude || !participant.longitude) {
    return '未知距离'
  }

  return calculateDistance(
    currentLocation.value.latitude,
    currentLocation.value.longitude,
    participant.latitude,
    participant.longitude
  )
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const time = new Date(timeStr)
  const now = new Date()
  const diff = now - time

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'

  const month = String(time.getMonth() + 1).padStart(2, '0')
  const day = String(time.getDate()).padStart(2, '0')
  const hour = String(time.getHours()).padStart(2, '0')
  const minute = String(time.getMinutes()).padStart(2, '0')

  return `${month}-${day} ${hour}:${minute}`
}

// 判断地址是否过长(超过20个字符需要折叠)
function isAddressLong(address) {
  if (!address) return false
  return address.length > 20
}

// 切换地址展开/收起状态
function toggleAddressExpand(userId) {
  expandedAddressMap.value[userId] = !expandedAddressMap.value[userId]
  console.log('🔄 切换地址展开状态:', {
    用户ID: userId,
    展开状态: expandedAddressMap.value[userId] ? '展开' : '收起'
  })
}

// 新增：点击参与者卡片
function onParticipantClick(participant) {
  selectedUserId.value = participant.userId

  // 更新地图中心到该参与者位置
  if (participant.latitude && participant.longitude) {
    const lat = Number(participant.latitude)
    const lng = Number(participant.longitude)

    // 更新地图中心
    mapCenter.value = { latitude: lat, longitude: lng }

    // 使用 MapContext 移动地图（带动画效果）
    if (mapContext) {
      mapContext.moveToLocation({
        latitude: lat,
        longitude: lng,
        success: () => {
          console.log('地图移动成功')
        }
      })

      // 也可以使用 setCenterOffset 来调整中心点
      mapContext.getCenterLocation({
        success: (res) => {
          console.log('当前地图中心:', res)
        }
      })
    }

    // 提示
    uni.showToast({
      title: `定位到${participant.nickname || '该用户'}`,
      icon: 'none',
      duration: 1500
    })
  }

  // 收起抽屉以更好地查看地图
  drawerExpanded.value = false
}

// 新增：定位到我的位置
function centerToMyLocation() {
  if (currentLocation.value && currentLocation.value.latitude && currentLocation.value.longitude) {
    const lat = Number(currentLocation.value.latitude)
    const lng = Number(currentLocation.value.longitude)

    mapCenter.value = { latitude: lat, longitude: lng }
    selectedUserId.value = currentUserId.value

    // 使用 MapContext 移动到当前位置
    if (mapContext) {
      mapContext.moveToLocation({
        latitude: lat,
        longitude: lng
      })
    }

    uni.showToast({
      title: '已定位到我的位置',
      icon: 'none',
      duration: 1500
    })
  } else {
    uni.showToast({
      title: '当前位置未获取',
      icon: 'none'
    })
  }
}

// 新增：切换抽屉展开/收起
function toggleDrawer() {
  drawerExpanded.value = !drawerExpanded.value
}

// 手动定位选择器（地址搜索 + 区域选择）
function showManualLocationPicker() {
  uni.showActionSheet({
    itemList: ['🔍 输入地址搜索定位', '📍 从列表选择区域'],
    success: (res) => {
      if (res.tapIndex === 0) {
        showAddressSearchPicker()
      } else {
        showDistrictPicker()
      }
    }
  })
}

// 地址搜索定位（使用高德地理编码 API）
function showAddressSearchPicker() {
  // H5 环境可以用 prompt
  const address = prompt('请输入你的位置（如：深圳大学、南山科技园、北京天安门）:')
  if (!address || !address.trim()) {
    // 用户取消，回到区域选择
    showDistrictPicker()
    return
  }

  uni.showLoading({ title: '搜索中...' })

  // 使用高德地理编码 API 搜索地址
  const url = `https://restapi.amap.com/v3/geocode/geo?key=${AMAP_KEY}&address=${encodeURIComponent(address.trim())}&output=json`

  fetch(url)
    .then(r => r.json())
    .then(async (data) => {
      uni.hideLoading()
      console.log('🔍 地址搜索结果:', data)

      if (data.status === '1' && data.geocodes && data.geocodes.length > 0) {
        // 有结果，取第一个
        const geo = data.geocodes[0]
        const [lng, lat] = geo.location.split(',')
        const formattedAddr = geo.formatted_address || address

        // 如果有多个结果，让用户选择
        if (data.geocodes.length > 1) {
          const items = data.geocodes.map(g => g.formatted_address || g.province + g.city + g.district)
          uni.showActionSheet({
            itemList: items.slice(0, 6),
            success: async (res2) => {
              const selected = data.geocodes[res2.tapIndex]
              const [sLng, sLat] = selected.location.split(',')
              await applyManualLocation(
                parseFloat(sLat),
                parseFloat(sLng),
                selected.formatted_address || address
              )
            },
            fail: () => {
              // 用户取消，用第一个结果
              applyManualLocation(parseFloat(lat), parseFloat(lng), formattedAddr)
            }
          })
        } else {
          await applyManualLocation(parseFloat(lat), parseFloat(lng), formattedAddr)
        }
      } else {
        uni.showToast({ title: '未找到该地址，请重试', icon: 'none' })
        setTimeout(() => showManualLocationPicker(), 1500)
      }
    })
    .catch((err) => {
      uni.hideLoading()
      console.error('地址搜索失败:', err)
      uni.showToast({ title: '搜索失败，请重试', icon: 'none' })
      setTimeout(() => showManualLocationPicker(), 1500)
    })
}

// 应用手动选择的位置
async function applyManualLocation(lat, lng, address) {
  currentLocation.value = { latitude: lat, longitude: lng, address: address }
  mapCenter.value = { latitude: lat, longitude: lng }
  locationEnabled.value = true
  isGettingLocation.value = false

  uni.showToast({ title: `已定位到${address}`, icon: 'success', duration: 2000 })
  console.log('📍 手动设置位置:', address, lat, lng)

  await updateLocation()
  await loadParticipants()
  updateMarkersFromParticipants()

  setTimeout(() => {
    if (mapContext) {
      mapContext.moveToLocation({ latitude: lat, longitude: lng })
    }
  }, 500)

  startLocationAutoUpdate()
}

// 区域列表选择器
function showDistrictPicker() {
  const shenzhenAreas = [
    { name: '坪山区', lat: 22.7089, lng: 114.3500 },
    { name: '福田区', lat: 22.5474, lng: 114.0549 },
    { name: '南山区', lat: 22.5329, lng: 113.9303 },
    { name: '罗湖区', lat: 22.5551, lng: 114.1249 },
    { name: '龙岗区', lat: 22.7209, lng: 114.2472 },
    { name: '宝安区', lat: 22.5540, lng: 113.8832 },
    { name: '龙华区', lat: 22.6568, lng: 114.0364 },
    { name: '盐田区', lat: 22.5574, lng: 114.2361 },
    { name: '光明区', lat: 22.7492, lng: 113.9380 },
    { name: '大鹏新区', lat: 22.5942, lng: 114.4779 }
  ]

  const itemList = shenzhenAreas.map(area => `深圳市${area.name}`)

  uni.showActionSheet({
    itemList: itemList,
    success: async (res) => {
      const selectedArea = shenzhenAreas[res.tapIndex]
      await applyManualLocation(selectedArea.lat, selectedArea.lng, `深圳市${selectedArea.name}`)
    }
  })
}

// 新增：抽屉滑动开始
function onDrawerTouchStart(e) {
  drawerStartY = e.touches[0].clientY
}

// 新增：抽屉滑动中
function onDrawerTouchMove(e) {
  drawerCurrentY = e.touches[0].clientY
}

// 高德地图 IP 定位 API（中国大陆可用）
async function getLocationByAmapIP() {
  return new Promise((resolve, reject) => {
    const url = `https://restapi.amap.com/v3/ip?key=${AMAP_KEY}`

    // #ifdef H5
    fetch(url)
      .then(response => response.json())
      .then(data => {
        console.log('🌐 高德 IP 定位返回:', data)

        if (data.status === '1' && data.rectangle && typeof data.rectangle === 'string' && data.rectangle.includes(';')) {
          // rectangle 格式: "113.7,22.5;114.5,22.8" (左下角;右上角)
          const coords = data.rectangle.split(';')
          if (coords.length < 2 || !coords[0].includes(',') || !coords[1].includes(',')) {
            reject(new Error('IP定位返回无效 rectangle: ' + data.rectangle))
            return
          }
          const corner1 = coords[0].split(',')
          const corner2 = coords[1].split(',')

          // 取中心点
          const longitude = (parseFloat(corner1[0]) + parseFloat(corner2[0])) / 2
          const latitude = (parseFloat(corner1[1]) + parseFloat(corner2[1])) / 2

          resolve({
            latitude: latitude,
            longitude: longitude,
            city: Array.isArray(data.city) ? '' : (data.city || ''),
            province: Array.isArray(data.province) ? '' : (data.province || ''),
            adcode: data.adcode || ''
          })
        } else {
          // 处理高德返回 status=1 但 province/city 为空数组的情况
          const msg = data.status === '1' 
            ? 'IP定位无数据（无法识别当前 IP 位置）' 
            : ('IP定位失败: ' + (data.info || '未知错误'))
          reject(new Error(msg))
        }
      })
      .catch(err => {
        console.error('IP定位请求失败:', err)
        reject(err)
      })
    // #endif

    // #ifndef H5
    uni.request({
      url: url,
      method: 'GET',
      success: (res) => {
        const data = res.data
        if (data.status === '1' && data.rectangle && typeof data.rectangle === 'string') {
          const coords = data.rectangle.split(';')
          const corner1 = coords[0].split(',')
          const corner2 = coords[1].split(',')

          const longitude = (parseFloat(corner1[0]) + parseFloat(corner2[0])) / 2
          const latitude = (parseFloat(corner1[1]) + parseFloat(corner2[1])) / 2

          resolve({
            latitude: latitude,
            longitude: longitude,
            city: data.city || '',
            province: data.province || ''
          })
        } else {
          reject(new Error('IP定位失败'))
        }
      },
      fail: reject
    })
    // #endif
  })
}

// 备用 IP 定位（并发多个服务取最快成功，支持境外/VPN网络）
async function getLocationByFallbackIP() {
  // #ifdef H5
  const TIMEOUT = 10000

  // 各备用服务的请求与解析逻辑
  const services = [
    {
      name: 'ipapi.co',
      url: 'https://ipapi.co/json/',
      parse: (data) => {
        if (typeof data.latitude === 'number' && typeof data.longitude === 'number') {
          return { latitude: data.latitude, longitude: data.longitude,
                   city: data.city || '', country: data.country_name || '' }
        }
        return null
      }
    },
    {
      name: 'ipinfo.io',
      url: 'https://ipinfo.io/json',
      parse: (data) => {
        if (data.loc) {
          const [lat, lng] = data.loc.split(',').map(Number)
          if (!isNaN(lat) && !isNaN(lng)) {
            return { latitude: lat, longitude: lng,
                     city: data.city || '', country: data.country || '' }
          }
        }
        return null
      }
    }
  ]

  // 并发所有服务，取第一个成功的结果
  const results = await Promise.allSettled(
    services.map(async (svc) => {
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), TIMEOUT)
      try {
        const resp = await fetch(svc.url, { signal: controller.signal })
        const data = await resp.json()
        console.log(`🌐 备用 IP 定位返回 (${svc.name}):`, data)
        const result = svc.parse(data)
        if (!result) throw new Error(`${svc.name} 数据无坐标`)
        return result
      } finally {
        clearTimeout(timer)
      }
    })
  )

  for (const r of results) {
    if (r.status === 'fulfilled' && r.value) {
      console.log('✅ 备用 IP 定位成功:', r.value)
      return r.value
    }
  }

  console.warn('所有备用 IP 服务均失败:', results.map(r => r.reason?.message || r.status))
  return null
  // #endif
  return null
}

// 逆地理编码：将经纬度转换为实际地址
async function reverseGeocode(lat, lng) {
  try {
    // #ifdef H5
    // 使用高德地图Web服务API
    // extensions=all 返回详细信息（街道、门牌号、POI等）
    // radius=50 搜索半径50米内的POI，提高精度
    // roadlevel=0 显示所有道路级别
    const url = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${lng},${lat}&poitype=&radius=50&extensions=all&batch=false&roadlevel=0`

    const response = await fetch(url)
    const data = await response.json()

    if (data.status === '1' && data.regeocode) {
      const addressComponent = data.regeocode.addressComponent
      const formattedAddress = data.regeocode.formatted_address

      // 获取更详细的地址信息
      const province = addressComponent.province || ''
      const city = addressComponent.city || addressComponent.province || ''
      const district = addressComponent.district || ''
      const township = addressComponent.township || '' // 街道/乡镇
      const neighborhood = addressComponent.neighborhood?.name || '' // 社区/小区
      const building = addressComponent.building?.name || '' // 建筑物
      const streetNumber = addressComponent.streetNumber || {}
      const street = streetNumber.street || '' // 街道名
      const number = streetNumber.number || '' // 门牌号

      // 获取附近POI（兴趣点）- 只显示50米内的
      let poi = ''
      if (data.regeocode.pois && data.regeocode.pois.length > 0) {
        const nearestPoi = data.regeocode.pois[0]
        if (nearestPoi.distance && parseFloat(nearestPoi.distance) <= 50) {
          poi = nearestPoi.name || ''
        }
      }

      // 拼接详细地址（根据有无信息动态组合）
      let detailedAddress = ''

      // 基础地址：省市区
      if (city && city !== province && !['北京市', '上海市', '天津市', '重庆市'].includes(province)) {
        detailedAddress = `${city}${district}`
      } else {
        detailedAddress = `${province}${district}`
      }

      // 添加街道/乡镇（重要：确保街道信息准确）
      if (township) {
        detailedAddress += township
      }

      // 添加街道名和门牌号
      if (street) {
        detailedAddress += street
        if (number) {
          detailedAddress += number
        }
      }

      // 添加小区/社区
      if (neighborhood) {
        detailedAddress += neighborhood
      }

      // 添加建筑物
      if (building) {
        detailedAddress += building
      }

      // 如果有POI且不同于building，添加POI作为补充
      if (poi && poi !== building && poi !== neighborhood) {
        detailedAddress += `(${poi}附近)`
      }

      // 如果拼接的地址太短，使用格式化地址
      if (detailedAddress.length < 10 && formattedAddress) {
        detailedAddress = formattedAddress
      }

      console.log('🌐 逆地理编码成功 (详细版):', {
        完整地址: detailedAddress,
        省市区: `${province} ${city} ${district}`,
        街道: township,
        社区: neighborhood,
        建筑: building,
        街路: street,
        门牌: number,
        POI: poi,
        原始formatted_address: formattedAddress,
        坐标: `${lat}, ${lng}`
      })

      // 特别检查深圳坪山区的情况
      if (city.includes('深圳') && district) {
        console.log('🏙️ 深圳地区定位详情:', {
          区县: district,
          街道: township,
          是否坪山区: district.includes('坪山'),
          是否石井街道: township.includes('石井')
        })
      }

      return detailedAddress || formattedAddress || '位置未知'
    }
    // #endif

    // 非H5环境或API失败，使用uni-app的逆地理编码
    const result = await new Promise((resolve, reject) => {
      uni.request({
        url: `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${lng},${lat}&extensions=all&radius=1000`,
        method: 'GET',
        success: (res) => {
          if (res.data.status === '1' && res.data.regeocode) {
            const addressComponent = res.data.regeocode.addressComponent || {}
            const city = addressComponent.city || addressComponent.province || ''
            const district = addressComponent.district || ''
            const township = addressComponent.township || ''
            const neighborhood = addressComponent.neighborhood?.name || ''
            const street = addressComponent.streetNumber?.street || ''
            const number = addressComponent.streetNumber?.number || ''

            // 拼接详细地址
            let detailedAddress = `${city}${district}${township}`
            if (street) detailedAddress += street
            if (number) detailedAddress += number
            if (neighborhood) detailedAddress += neighborhood

            console.log('🌐 逆地理编码成功 (uni-app):', detailedAddress)
            resolve(detailedAddress || res.data.regeocode.formatted_address || '未知位置')
          } else {
            resolve('未知位置')
          }
        },
        fail: () => resolve('未知位置')
      })
    })

    return result

  } catch (error) {
    console.warn('⚠️ 逆地理编码失败，返回坐标:', error)
    // 如果逆地理编码失败，返回简化的坐标描述
    return `北纬${lat.toFixed(4)}°, 东经${lng.toFixed(4)}°`
  }
}

// 新增：抽屉滑动结束
function onDrawerTouchEnd() {
  const deltaY = drawerStartY - drawerCurrentY

  // 向上滑动超过50px，展开抽屉
  if (deltaY > 50) {
    drawerExpanded.value = true
  }
  // 向下滑动超过50px，收起抽屉
  else if (deltaY < -50) {
    drawerExpanded.value = false
  }

  // 重置
  drawerStartY = 0
  drawerCurrentY = 0
}

onUnmounted(() => {
  if (messageTimer) clearInterval(messageTimer)
  if (signTaskTimer) clearInterval(signTaskTimer)
  if (locationTimer) clearInterval(locationTimer)
  if (locationUploadTimer) clearInterval(locationUploadTimer)
  uni.$off('refreshActivityRoom')
})
</script>

<style scoped>
.activity-room {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.header .title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 15px;
  color: #666;
  position: relative;
  cursor: pointer;
}

.tab-item.active {
  color: #2979ff;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background-color: #2979ff;
  border-radius: 2px;
}

.content-area {
  flex: 1;
  overflow: hidden;
}

.chat-section,
.sign-section,
.location-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.message-list {
  flex: 1;
  padding: 12px;
  background-color: #f5f5f5;
}

.message-item {
  margin-bottom: 16px;
  padding: 10px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.message-item.my-message {
  background-color: #e7f7ff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.nickname {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.message-content {
  font-size: 15px;
  color: #333;
  line-height: 1.5;
  word-break: break-all;
}

.input-area {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #fff;
  border-top: 1px solid #eee;
  flex-shrink: 0;
}

.message-input {
  flex: 1;
  height: 36px;
  line-height: 36px;
  padding: 0 12px;
  border: 1px solid #ddd;
  border-radius: 18px;
  font-size: 14px;
  background-color: #f5f5f5;
}

.send-btn {
  margin-left: 8px;
  padding: 0 20px;
  height: 36px;
  line-height: 36px;
  background-color: #2979ff;
  color: #fff;
  border: none;
  border-radius: 18px;
  font-size: 14px;
}

.send-btn:active {
  opacity: 0.8;
}

.sign-section {
  padding: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.create-sign-btn {
  padding: 6px 16px;
  background-color: #2979ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
}

.create-sign-btn:active {
  opacity: 0.8;
}

.sign-list {
  flex: 1;
}

.sign-item {
  margin-bottom: 12px;
  padding: 12px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #eee;
}

.sign-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sign-title {
  font-size: 15px;
  font-weight: bold;
  color: #333;
}

.sign-time {
  font-size: 12px;
  color: #999;
}

.sign-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.4;
}

.sign-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sign-count {
  font-size: 13px;
  color: #666;
}

.sign-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-btn {
  padding: 4px 12px;
  background-color: #fff;
  color: #2979ff;
  border: 1px solid #2979ff;
  border-radius: 4px;
  font-size: 12px;
}

.detail-btn:active {
  opacity: 0.8;
}

.sign-btn {
  padding: 4px 12px;
  background-color: #19be6b;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
}

.sign-btn:active {
  opacity: 0.8;
}

.signed-tag {
  font-size: 13px;
  color: #19be6b;
  font-weight: bold;
}

.location-section {
  padding: 12px;
}

.location-btn {
  padding: 6px 16px;
  background-color: #ff9800;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
}

.location-btn:active {
  opacity: 0.8;
}

.location-status {
  font-size: 13px;
  color: #19be6b;
  font-weight: bold;
}

.location-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.close-btn {
  padding: 6px 12px;
  background-color: #f44336;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
}

.close-btn:active { opacity: 0.85; }

.location-list {
  flex: 1;
}

.location-tip {
  text-align: center;
  padding: 40px 20px;
}

.location-tip .icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.location-tip .tip {
  font-size: 16px;
  color: #666;
  display: block;
  margin-bottom: 8px;
}

.location-tip .sub-tip {
  font-size: 14px;
  color: #999;
  display: block;
}

.map-wrap {
  height: 300px;
  margin-bottom: 8px;
}

.map-component {
  width: 100%;
  height: 100%;
}

.participant-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #eee;
}

.participant-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.participant-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ff9800;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  margin-right: 12px;
}

.participant-detail {
  flex: 1;
}

.participant-name {
  font-size: 15px;
  color: #333;
  margin-bottom: 4px;
  font-weight: bold;
}

.me-tag {
  font-size: 12px;
  color: #2979ff;
  font-weight: normal;
}

.participant-address {
  font-size: 13px;
  color: #666;
  margin-bottom: 2px;
  line-height: 1.3;
}

.participant-time {
  font-size: 11px;
  color: #999;
}

.participant-distance {
  font-size: 13px;
  color: #ff9800;
  font-weight: bold;
  text-align: right;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}

/* ====================================
   现代化位置功能样式
   ==================================== */

/* 位置区域容器 */
.location-section-modern {
  height: 100%;
  background: #f5f5f5; /* 改为浅灰色，不遮挡地图 */
  position: relative;
  overflow: hidden;
}

/* 欢迎界面 */
.location-welcome {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.welcome-content {
  text-align: center;
  padding: 32px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 320px;
  margin: 0 auto;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-title {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
}

.welcome-desc {
  display: block;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 30px;
}

.modern-btn-primary {
  width: 100%;
  height: 44px;
  background: #3c9cff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: normal;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
}

.modern-btn-primary:active {
  opacity: 0.85;
}

.modern-btn-secondary {
  width: 100%;
  height: 40px;
  background: #fff;
  color: #555;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-weight: normal;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
}

.modern-btn-secondary:active {
  background: #f5f5f5;
}

.welcome-hint {
  display: block;
  margin-top: 16px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.btn-icon {
  margin-right: 8px;
  font-size: 20px;
}

.btn-text {
  font-size: 16px;
}

/* 地图容器 */
.location-map-container {
  height: 100%;
  position: relative;
  background: #e0e0e0; /* 浅灰色背景，避免紫色 */
}

.map-fullscreen {
  width: 100%;
  height: 100%;
  position: relative;
  background: #fff; /* 白色背景 */
  z-index: 1;
}

.map-component-full {
  width: 100%;
  height: 100%;
  display: block; /* 确保地图块级显示 */
}

/* 地图覆盖层（获取位置时） */
.map-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.overlay-card {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px 32px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  text-align: center;
}

.loading-spinner {
  font-size: 32px;
  margin-bottom: 10px;
}

.loading-text {
  font-size: 14px;
  color: #555;
  font-weight: normal;
}

/* 地图错误覆盖层 */
.map-error-overlay {
  position: absolute;
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.error-card {
  background: rgba(244, 67, 54, 0.95);
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.3);
  text-align: center;
}

.error-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.error-text {
  font-size: 15px;
  color: #fff;
  font-weight: bold;
  margin-bottom: 4px;
}

.error-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
}

/* 旧的加载样式（保留向后兼容） */
.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

/* 地图控制按钮 */
.map-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.control-btn {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.control-btn:active {
  transform: scale(0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.control-icon {
  font-size: 20px;
}

/* 参与者计数徽章 */
.participant-count-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  padding: 6px 12px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.count-icon {
  font-size: 14px;
}

.count-text {
  font-size: 13px;
  font-weight: normal;
  color: #333;
}

/* 底部抽屉 */
.bottom-drawer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 20;
  background: #fff;
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  max-height: 45vh;
  transition: max-height 0.3s ease, transform 0.3s ease;
}

.drawer-expanded {
  max-height: 70vh;
}

/* 抽屉把手 */
.drawer-handle-container {
  padding: 12px 0;
  display: flex;
  justify-content: center;
  cursor: pointer;
}

.drawer-handle {
  width: 40px;
  height: 4px;
  background: #ddd;
  border-radius: 2px;
}

/* 抽屉头部 */
.drawer-header {
  padding: 0 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.drawer-title {
  display: block;
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.drawer-subtitle {
  display: block;
  font-size: 12px;
  color: #999;
}

/* 抽屉内容 */
.drawer-content {
  overflow-y: auto;
  padding: 16px 20px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  display: block;
  font-size: 14px;
  color: #999;
}

/* 现代化参与者卡片 */
.modern-participant-card {
  background: linear-gradient(135deg, #fff 0%, #f8f9fa 100%);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.modern-participant-card:active {
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.card-active {
  border-color: #ff9800;
  background: linear-gradient(135deg, #fff5e6 0%, #ffe0b2 100%);
  box-shadow: 0 4px 16px rgba(255, 152, 0, 0.2);
}

/* 卡片左侧 */
.card-left {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 12px;
}

/* 现代化头像 */
.modern-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.avatar-me {
  background: linear-gradient(135deg, #ff9800 0%, #ff5722 100%);
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.4);
  animation: pulse-avatar 2s ease-in-out infinite;
}

@keyframes pulse-avatar {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(255, 152, 0, 0.4);
  }
  50% {
    box-shadow: 0 4px 20px rgba(255, 152, 0, 0.6);
  }
}

.avatar-text {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
}

.avatar-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 18px;
  height: 18px;
  background: #fff;
  color: #ff9800;
  font-size: 10px;
  font-weight: bold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ff9800;
}

/* 卡片信息 */
.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.badge-me {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #ff9800 0%, #ff5722 100%);
  color: #fff;
  border-radius: 10px;
  font-weight: bold;
}

/* 地址容器（支持点击） */
.address-container {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  transition: all 0.3s ease;
  max-width: 200px;
}

.address-container:active {
  opacity: 0.7;
}

/* 用户地址样式 */
.user-address {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  word-break: break-all;
  transition: all 0.3s ease;
  /* 默认状态：最多显示2行 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 展开状态：显示全部内容 */
.user-address.address-expanded {
  -webkit-line-clamp: unset;
  max-height: none;
}

/* 展开/收起按钮 */
.expand-btn {
  font-size: 11px;
  color: #2979ff;
  font-weight: 500;
  margin-top: 2px;
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(41, 121, 255, 0.08);
  transition: all 0.2s ease;
}

.expand-btn:active {
  background: rgba(41, 121, 255, 0.15);
  transform: scale(0.95);
}

.user-time {
  font-size: 11px;
  color: #999;
}

/* 卡片右侧 */
.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.distance-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 4px 10px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.distance-icon {
  font-size: 12px;
}

.distance-text {
  font-size: 12px;
  font-weight: bold;
  color: #fff;
}

.nav-icon {
  font-size: 20px;
  color: #ff9800;
  font-weight: bold;
}

/* ========== 相册区样式 ========== */
.album-section {
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.album-scroll {
  flex: 1;
  max-height: 60vh;
}

.album-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px;
}

.album-item {
  width: calc(33.33% - 6px);
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.album-img {
  width: 100%;
  height: 100%;
}
</style>
