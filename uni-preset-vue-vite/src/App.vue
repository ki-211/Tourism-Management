<script>
export default {
  onLaunch: function () {
    console.log('App Launch')
    // 检查token是否存在且有效
    this.checkLoginStatus()
  },
  onShow: function () {
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  },
  methods: {
    checkLoginStatus() {
      const token = uni.getStorageSync('token')
      const tokenExpire = uni.getStorageSync('token_expire')
      const userId = uni.getStorageSync('userId')
      
      // 获取当前页面路径
      const pages = getCurrentPages()
      const currentPage = pages.length > 0 ? pages[pages.length - 1] : null
      const currentPath = currentPage ? '/' + currentPage.route : ''
      
      // 如果token存在且未过期，并且当前在登录页，则跳转到首页
      if (token && userId && tokenExpire && Date.now() < tokenExpire) {
        console.log('Token有效，自动登录')
        // 使用setTimeout确保页面已经加载完成
        setTimeout(() => {
          const pages = getCurrentPages()
          const currentPage = pages.length > 0 ? pages[pages.length - 1] : null
          const currentRoute = currentPage ? currentPage.route : ''
          
          // 只有在登录页或注册页时才自动跳转
          if (currentRoute === 'pages/login/login' || currentRoute === 'pages/register/register') {
            uni.switchTab({
              url: '/pages/home/home'
            })
          }
        }, 100)
      } else if (token || tokenExpire || userId) {
        // token过期或无效，清除存储
        console.log('Token无效或已过期，清除登录状态')
        uni.removeStorageSync('token')
        uni.removeStorageSync('token_expire')
        uni.removeStorageSync('userId')
      }
    }
  }
}
</script>

<style>
/*每个页面公共css */
</style>
