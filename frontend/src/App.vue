<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { hasSession, clearSession } from '@/services/session'
import { useSessionStore } from '@/stores/session'

onLaunch(() => {
  if (!hasSession()) {
    clearSession()
    uni.reLaunch({ url: '/pages/login/login' })
  }
  useSessionStore().reload()
})
</script>

<style lang="scss">
@import "@/styles/global.scss";

page {
  background: $page-bg;
  color: $text-main;
  font-family: "PingFang SC", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: 28rpx;
  line-height: 1.5;
  -webkit-text-size-adjust: 100%;
  text-size-adjust: 100%;
}

button::after { border: 0; }
</style>
