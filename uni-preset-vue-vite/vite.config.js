// vite.config.js
import { defineConfig } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import { resolve } from 'path';
import basicSsl from '@vitejs/plugin-basic-ssl';
import { AMAP_JS_KEY, AMAP_JS_SECURITY_CODE } from './src/utils/mapConfig.js';

// 将 index.html 中的占位符替换为 mapConfig.js 中的实际 Key
const injectAmapKeys = {
  name: 'inject-amap-keys',
  transformIndexHtml(html) {
    return html
        .replace('__AMAP_JS_KEY__', AMAP_JS_KEY)
        .replace('__AMAP_SECURITY_CODE__', AMAP_JS_SECURITY_CODE);
  }
};

export default defineConfig({
  plugins: [
    uni(),
    basicSsl(),  // 启用 HTTPS，手机 GPS 需要 HTTPS 才能工作
    injectAmapKeys
  ],
  server: {
    host: true,   // 允许局域网访问
    https: true   // 开启 HTTPS
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        charset: false,
        additionalData: '@import "@/uni.scss";'
      }
    }
  }
});

