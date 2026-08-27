<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { hasSession, clearSession } from '@/services/session'
import { useSessionStore } from '@/stores/session'
import { useLocationSharingStore } from '@/stores/locationSharing'

onLaunch(() => {
  if (!hasSession()) {
    clearSession()
    uni.reLaunch({ url: '/pages/login/login' })
  }
  useSessionStore().reload()
  const locationSharing = useLocationSharingStore()
  uni.$on('session-expired', () => locationSharing.stop(false))
  locationSharing.initialize()
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
