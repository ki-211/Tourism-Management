<template>
  <view class="home-container">
    <u-tabbar :active="active" @change="onChange">
      <u-tabbar-item icon="list" title="活动" />
      <u-tabbar-item icon="checkmark-circle" title="签到" />
      <u-tabbar-item icon="myActivity" title="参加的活动" />
      <u-tabbar-item icon="user" title="个人" />
    </u-tabbar>

    <!-- 用 v-show 替代 v-if 保留组件状态 -->
    <ActivityList v-show="active === 0" />
    <SignIn v-show="active === 1" />
    <MyActivity v-show="active === 2" />
    <Personal v-show="active === 3" />
  </view>
</template>

<script setup>
import { ref } from 'vue';
import ActivityList from '@/pages/activityList/activityList.vue';
import SignIn from '@/pages/signList/signList.vue';
import MyActivity from '@/pages/myActivity/myActivity.vue';
import Personal from '@/pages/personal/personal.vue';

const active = ref(0);

function onChange(index) {
  active.value = index;
}
</script>

<style scoped>
.home-container {
  height: 100%;
}
</style>
