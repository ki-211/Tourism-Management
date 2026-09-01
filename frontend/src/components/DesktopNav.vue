<template>
  <!-- #ifdef H5 -->
  <view class="desktop-sidebar">
    <view class="desktop-brand"><view class="brand-mark">协</view><view><text class="brand-name">协作集</text><text class="brand-sub">活动协作</text></view></view>
    <view class="desktop-menu">
      <button v-for="item in items" :key="item.key" :class="{ active: active === item.key }" @click="go(item.url)">
        <image :src="active === item.key ? item.activeIcon : item.icon" mode="aspectFit" />
        <text>{{ item.text }}</text>
      </button>
    </view>
    <view class="desktop-foot"><text>组织 · 协作 · 复盘</text><text>让每场活动更有序</text></view>
  </view>
  <!-- #endif -->
</template>

<script setup lang="ts">
defineProps<{ active: string }>()
const items = [
  { key: 'home', text: '发现活动', url: '/pages/home/home', icon: '/static/icons/discover-v2.png', activeIcon: '/static/icons/discover-v2-active.png' },
  { key: 'sign', text: '签到任务', url: '/pages/signList/signList', icon: '/static/icons/checkin-v2.png', activeIcon: '/static/icons/checkin-v2-active.png' },
  { key: 'journey', text: '我的活动', url: '/pages/myActivity/myActivity', icon: '/static/icons/journey-v2.png', activeIcon: '/static/icons/journey-v2-active.png' },
  { key: 'profile', text: '个人中心', url: '/pages/personal/personal', icon: '/static/icons/profile-v2.png', activeIcon: '/static/icons/profile-v2-active.png' }
]
function go(url: string) { uni.switchTab({ url }) }
</script>

<style scoped lang="scss">
/* #ifdef H5 */
.desktop-sidebar { display: none; }
@media (min-width: 900px) {
  .desktop-sidebar { position: fixed; z-index: 30; left: 0; top: 44px; bottom: 0; display: flex; flex-direction: column; width: 224px; padding: 34px 22px 26px; background: rgba(255,255,255,.96); border-right: 1px solid $border; box-shadow: 10px 0 36px rgba(29,65,59,.04); }
  .desktop-brand { display: flex; align-items: center; padding: 0 10px 34px; gap: 13px; }
  .brand-mark { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 46px; height: 46px; color: #fff; font-size: 22px; font-weight: 800; background: linear-gradient(145deg,$primary,#3ca69b); border-radius: 15px 15px 15px 5px; }
  .brand-name,.brand-sub{display:block}.brand-name{font-size:20px;font-weight:800;letter-spacing:2px}.brand-sub{margin-top:2px;color:$text-muted;font-size:11px;letter-spacing:1px}
  .desktop-menu{display:flex;flex-direction:column;gap:8px}.desktop-menu button{display:flex;align-items:center;width:100%;height:52px;margin:0;padding:0 16px;color:$text-secondary;font-size:15px;line-height:1;background:transparent;border-radius:15px;gap:14px}.desktop-menu button.active{color:$primary-dark;background:$primary-soft}.desktop-menu image{flex-shrink:0;width:25px;height:25px}.desktop-foot{margin-top:auto;padding:20px 12px 0;color:$text-muted;font-size:11px;line-height:1.8;border-top:1px solid $border}.desktop-foot text{display:block}
}
/* #endif */
</style>
