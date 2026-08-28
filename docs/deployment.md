# 部署说明

## Compose

根目录 `compose.yaml` 提供 MySQL 8.4、Redis 8.2、MinIO、后端和 Nginx 前端。先复制 `.env.example` 为 `.env`，修改全部密码、`JWT_SECRET`、域名、CORS 和地图配置：

```bash
docker compose up -d --build
```

Compose 只把前端入口绑定到 `127.0.0.1:8088`，数据库、Redis、对象存储和后端均不直接暴露公网。宿主机 Nginx 使用 `deploy/nginx.conf` 将 HTTPS/WSS 转发到该入口。启动后先确认：

```bash
docker compose ps
curl -fsS http://127.0.0.1:8088/healthz
curl -fsS http://127.0.0.1:8088/actuator/health/readiness
```

后端在 `prod` profile 下会校验 Base64 JWT 密钥（解码后至少 32 字节）、HTTPS CORS/公开地址、S3 和高德配置；校验不通过时不会带病启动。内置 MinIO 必须保持 `S3_PATH_STYLE=true`。若改用 AWS S3，再按供应商要求改为 `false`。

生产 JWT 密钥需为至少 32 字节随机值的 Base64。地图需要三类配置：后端 `AMAP_WEB_KEY` 使用高德 Web Service Key；H5 构建参数 `VITE_AMAP_JS_KEY` 使用高德 JS API Key；安全校验二选一配置 `AMAP_H5_SECURITY_JS_CODE` 或 `AMAP_H5_SERVICE_HOST`。`npm run dev:h5` 和 H5 构建都会临时注入客户端配置，并在开发服务停止或构建结束后恢复 `manifest.json`；仓库不得提交真实值。未配置客户端 Key 时 H5 会显示明确提示并保留成员位置列表，不渲染空白地图。

## HTTPS / WSS

`frontend/nginx.conf` 在容器内完成 `/api/` 和 `/ws` 转发；`deploy/nginx.conf` 负责公网 TLS 终止并整体转发到 `127.0.0.1:8088`。正式环境需替换证书路径和域名，只开放 443，并确保 WebSocket access log 不记录 query ticket。微信小程序后台需登记 request/uploadFile/downloadFile/socket 合法域名，并为 AppID 申请后台持续定位能力；小程序产物的 `requiredBackgroundModes` 必须包含 `location`。

## 备份与恢复

- 每日至少一次 `mysqldump --single-transaction`，保留异地副本并定期演练恢复。
- 对 S3 bucket 开启版本控制或生命周期备份；数据库与对象存储应使用同一备份时间窗口。
- Redis 只承载限流、短时 ticket 和实时事件，不是业务事实源；可通过 AOF 加快恢复，但不替代 MySQL/S3 备份。
- 本地 `backend/.local` 只用于开发，不作为生产数据方案。

升级前备份数据库，先在副本运行 Flyway。迁移文件发布后不得改写，只能新增版本。

## 常见故障

- `401`：访问令牌过期且刷新失败，前端会清空会话并回登录页。
- H5 地图未配置：检查 `VITE_AMAP_JS_KEY` 以及安全密钥或安全代理；定位还需 HTTPS、浏览器权限与系统定位。
- 微信后台位置无法启动：检查 AppID 的后台定位能力、隐私声明、基础库版本和用户授权。
- 小程序无法请求：检查合法域名、TLS 证书和 `VITE_API_BASE_URL`。
- 图片失败：确认格式是真实 JPEG/PNG/WebP 且不超过 10 MB，检查 S3 bucket 权限。
- WebSocket 连接失败：检查 `/ws` Upgrade 反代、Origin 白名单、Redis 连通性和 ticket 是否过期或已使用。
