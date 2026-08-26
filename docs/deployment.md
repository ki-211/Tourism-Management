# 部署说明

## Compose

根目录 `compose.yaml` 提供 MySQL 8.4、MinIO、后端和 Nginx 前端。先复制 `.env.example` 为 `.env`，至少修改所有密码、`JWT_SECRET`、域名和 CORS：

```bash
docker compose up -d --build
```

生产 JWT 密钥需为至少 32 字节随机值的 Base64。`AMAP_WEB_KEY` 必须是高德 Web Service Key，不是 H5 JS Key。生产环境禁用 Swagger，不把任何密钥打进前端镜像。

## HTTPS / WSS

`deploy/nginx.conf` 展示同域反代：`/api/` 到后端，`/ws` 以 Upgrade 头转发，其他请求回退到 H5 `index.html`。正式环境需替换证书路径和域名，并只开放 HTTPS。微信小程序后台需登记 request/uploadFile/downloadFile/socket 合法域名。

## 备份与恢复

- 每日至少一次 `mysqldump --single-transaction`，保留异地副本并定期演练恢复。
- 对 S3 bucket 开启版本控制或生命周期备份；数据库与对象存储应使用同一备份时间窗口。
- 本地 `backend/.local` 只用于开发，不作为生产数据方案。

升级前备份数据库，先在副本运行 Flyway。迁移文件发布后不得改写，只能新增版本。

## 常见故障

- `401`：访问令牌过期且刷新失败，前端会清空会话并回登录页。
- H5 无法定位：检查 HTTPS、浏览器权限与系统定位；可改用地址搜索或手动坐标。
- 小程序无法请求：检查合法域名、TLS 证书和 `VITE_API_BASE_URL`。
- 图片失败：确认格式是真实 JPEG/PNG/WebP 且不超过 10 MB，检查 S3 bucket 权限。
- WebSocket 连接失败：检查 `/ws` Upgrade 反代、Origin 白名单和 ticket 是否过期。
