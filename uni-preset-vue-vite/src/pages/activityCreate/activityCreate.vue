<template>
  <view class="container">
    <u-form label-width="80">
      <!-- 活动名称 & 地点 -->
      <u-form-item label="活动名称">
        <u-input v-model="form.title" placeholder="请输入活动名称" />
      </u-form-item>
      <u-form-item label="活动地点">
        <u-input v-model="form.location" placeholder="请输入地点" />
      </u-form-item>

      <!-- 开始时间 -->
      <u-form-item label="开始时间" @click="showStartPicker = true">
        <u-input
            v-model="formattedStartTime"
            placeholder="请选择开始时间"
            readonly
            prefixIcon="calendar"
        />
      </u-form-item>
      <u-datetime-picker
          v-model="startTimeValue"
          mode="datetime"
          :show="showStartPicker"
          closeOnClickOverlay
          @confirm="onStartConfirm"
          @cancel="showStartPicker = false"
      />

      <!-- 结束时间 -->
      <u-form-item label="结束时间" @click="showEndPicker = true">
        <u-input
            v-model="formattedEndTime"
            placeholder="请选择结束时间"
            readonly
            prefixIcon="calendar"
        />
      </u-form-item>
      <u-datetime-picker
          v-model="endTimeValue"
          mode="datetime"
          :show="showEndPicker"
          closeOnClickOverlay
          @confirm="onEndConfirm"
          @cancel="showEndPicker = false"
      />

      <!-- 报名开始 -->
      <u-form-item label="报名开始" @click="showSignupStartPicker = true">
        <u-input
            v-model="formattedSignupStart"
            placeholder="请选择报名开始"
            readonly
            prefixIcon="calendar"
        />
      </u-form-item>
      <u-datetime-picker
          v-model="signupStartValue"
          mode="datetime"
          :show="showSignupStartPicker"
          closeOnClickOverlay
          @confirm="onSignupStartConfirm"
          @cancel="showSignupStartPicker = false"
      />

      <!-- 报名结束 -->
      <u-form-item label="报名结束" @click="showSignupEndPicker = true">
        <u-input
            v-model="formattedSignupEnd"
            placeholder="请选择报名结束"
            readonly
            prefixIcon="calendar"
        />
      </u-form-item>
      <u-datetime-picker
          v-model="signupEndValue"
          mode="datetime"
          :show="showSignupEndPicker"
          closeOnClickOverlay
          @confirm="onSignupEndConfirm"
          @cancel="showSignupEndPicker = false"
      />

      <!-- 其它字段 -->
      <u-form-item label="费用规则">
        <u-input v-model="form.feeRule" type="textarea" placeholder="请输入费用说明" />
      </u-form-item>

      <u-form-item label="可见性">
        <u-radio-group v-model="form.visibleToTeam">
          <u-radio :name="true">团队可见</u-radio>
          <u-radio :name="false">仅自己可见</u-radio>
        </u-radio-group>
      </u-form-item>

      <u-form-item label="上传图片">
        <u-upload
            :file-list="fileList"
            :max-count="1"
            @afterRead="afterRead"
        />
      </u-form-item>

      <u-form-item label="活动说明">
        <u-input v-model="form.description" type="textarea" placeholder="请输入说明" />
      </u-form-item>
    </u-form>

    <u-button type="primary" @click="submit">发布</u-button>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request, { baseURL } from '@/utils/request'

/** 表单主体 */
const form = reactive({
  title: '',
  location: '',
  feeRule: '',
  visibleToTeam: true,
  description: '',
  imageUrl: '',
  creatorId: ''
})
const fileList = ref([])

/** 原生时间戳 & 格式化显示字段 */
const startTimeValue = ref(Date.now())
const endTimeValue = ref(Date.now())
const signupStartValue = ref(Date.now())
const signupEndValue = ref(Date.now())

const formattedStartTime = ref('')
const formattedEndTime = ref('')
const formattedSignupStart = ref('')
const formattedSignupEnd = ref('')

/** 控制弹窗 */
const showStartPicker = ref(false)
const showEndPicker = ref(false)
const showSignupStartPicker = ref(false)
const showSignupEndPicker = ref(false)

/** 图片上传 */
function afterRead(event) {
  const file = event.file
  uni.uploadFile({
    url: `${baseURL}/upload/image`,
    filePath: file.url,
    name: 'file',
    success(res) {
      const data = JSON.parse(res.data)
      form.imageUrl = data.url
      fileList.value = [{ url: data.url }]
    },
    fail() {
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  })
}

/** 格式化函数 */
function formatDate(ts) {
  const d = new Date(ts)
  const pad = n => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 各个时间选择器确认回调 */
function onStartConfirm(e) {
  showStartPicker.value = false
  startTimeValue.value = e.value
  formattedStartTime.value = formatDate(e.value)
}
function onEndConfirm(e) {
  showEndPicker.value = false
  endTimeValue.value = e.value
  formattedEndTime.value = formatDate(e.value)
}
function onSignupStartConfirm(e) {
  showSignupStartPicker.value = false
  signupStartValue.value = e.value
  formattedSignupStart.value = formatDate(e.value)
}
function onSignupEndConfirm(e) {
  showSignupEndPicker.value = false
  signupEndValue.value = e.value
  formattedSignupEnd.value = formatDate(e.value)
}

/** 提交 */
function submit() {
  // 登录态
  const token = uni.getStorageSync('token')
  const uid = uni.getStorageSync('userId')
  if (!token) return uni.showToast({ title: '未登录，请先登录', icon: 'none' })
  if (!uid)   return uni.showToast({ title: '未获取到用户信息', icon: 'none' })
  form.creatorId = uid

  // 必填项填入 form
  if (!form.title ||
      !formattedStartTime.value ||
      !formattedEndTime.value) {
    return uni.showToast({ title: '请完整填写活动名称和时间', icon: 'none' })
  }
  form.startTime = formattedStartTime.value
  form.endTime   = formattedEndTime.value
  form.signupStart = formattedSignupStart.value
  form.signupEnd   = formattedSignupEnd.value

  request.post('/activity/create', form)
      .then(() => {
        uni.showToast({ title: '发布成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      })
      .catch(err => {
        uni.showToast({ title: err.msg || '发布失败', icon: 'none' })
      })
}
</script>

<style scoped>
.container {
  padding: 20px;
  background: #fff;
  min-height: 100vh;
}
</style>
