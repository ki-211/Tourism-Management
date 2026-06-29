// 高德地图 Key 集中管理
// 修改 Key 时请同时更新 index.html 中对应的 JS API Key

// Web端 JS API Key（地图渲染，与 index.html 中保持一致）
export const AMAP_JS_KEY = '*'

// JS API 安全密钥（v2.0+ 使用，与 manifest.json 中保持一致）
export const AMAP_JS_SECURITY_CODE = '*'

// Web服务 Key（REST API 使用，如逆地理编码、IP 定位等）
// 如与 JS API Key 不同，请在此替换为你的 Web服务类型 Key
export const AMAP_WEB_KEY = '*'
