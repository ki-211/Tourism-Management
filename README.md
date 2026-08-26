# 校园出游管理

面向校园社团和同学的跨端出游协作应用。项目由 Spring Boot 3 后端与 uni-app/Vue 3 前端组成，同时支持 H5 和微信小程序。

## 已实现功能

- 注册、登录、15 分钟访问令牌、30 天轮换刷新令牌与退出
- 公开活动、邀请制活动、10 位邀请码、报名名单与团长转让
- 活动室聊天、相册、主动位置共享、签到任务与签到证据
- 车辆发布与成员查看、个人资料与签到历史
- 原生 WebSocket 通知、断线重连；业务数据以 HTTP 查询为准
- 本地 H2 + 文件存储零依赖启动；生产 MySQL 8.4 + S3 兼容存储

## Windows 本地启动

要求：JDK 21 或更高版本、Node.js 20 或更高版本。后端不要求本机安装 Maven。

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

首次启动会在 `backend/.local` 创建 H2 数据文件和上传目录。Swagger 地址为 <http://localhost:8080/swagger-ui.html>。

另开终端：

```powershell
cd frontend
Copy-Item .env.example .env.local
npm ci
npm run dev:h5
```

H5 默认访问 <http://localhost:5173>。微信小程序构建：

```powershell
npm run build:mp-weixin
```

将 `frontend/dist/build/mp-weixin` 导入微信开发者工具，并在 `frontend/src/manifest.json` 配置自己的小程序 AppID。真机请求必须使用已备案 HTTPS 域名，不能使用 `localhost`。

## 配置与安全

复制根目录 [`.env.example`](./.env.example) 后按环境填写。生产环境必须设置随机 `JWT_SECRET`、数据库、S3、CORS 和高德 Web Service Key。仓库不再保存默认 JWT 密钥或地图 Key；旧 Key 若曾公开，应立即在供应商控制台轮换。

定位只在用户主动开启后每 15 秒更新，停止共享或离开活动室会立即删除，异常断线数据 60 秒后不再返回。H5 定位通常要求 HTTPS；拒绝权限后可使用后端地址搜索或手动坐标。

更多说明见：

- [架构](docs/architecture.md)
- [API](docs/api.md)
- [部署](docs/deployment.md)
- [测试](docs/testing.md)

## 仓库约定

- `backend/target`、`frontend/node_modules`、`frontend/dist`、本地数据库、上传内容和环境密钥不得提交。
- 数据库由 Flyway 从零创建，不兼容旧库；开发期结构变更必须新增迁移文件。
- 本次重整不包含管理员后台和完整团队系统，团长权限属于单个活动。
