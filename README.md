# 多角色活动管理系统

面向组织、团队与个人的跨端活动协作系统。项目由 Spring Boot 3 后端与 uni-app/Vue 3 前端组成，同时支持 H5 和微信小程序；活动负责人和参与者拥有不同的活动级权限。

## 已实现功能

- 注册、登录、15 分钟访问令牌、30 天轮换刷新令牌、修改密码、全端下线与账号注销
- 公开活动、邀请制活动、10 位邀请码、报名名单与负责人转让
- 活动空间聊天、相册、主动位置共享、签到任务与签到证据
- 活动编辑、取消（取消后只读）与人数上限，并发报名不超员
- 签到截止时间、地理围栏范围校验与必须拍照
- 车辆座位与乘客分配（按随行人数占座、自选或由负责人安排、并发不超座）、个人资料与签到历史
- 登录按 IP 与用户名双重限流
- 单用途 WebSocket ticket、断线重连与 Redis 跨实例广播；业务数据以 HTTP 查询为准
- 私有图片短时签名访问与权限二次校验、事务失败对象清理及孤儿文件回收
- 本地默认 MySQL + 文件存储，可切换 H2 零依赖启动；生产 MySQL 8.4 + Redis + S3 兼容存储

## Windows 本地启动

要求：JDK 21 或更高版本、Node.js 24。后端不要求本机安装 Maven。

后端默认启用 `local` 环境，连接本机 MySQL 的 `localhost:3306/tourism`，连接配置位于 `backend/src/main/resources/application-local.yml`。启动前设置数据库账号和密码：

```powershell
cd backend
$env:DB_USERNAME = '你的MySQL用户名'
$env:DB_PASSWORD = '你的MySQL密码'
.\mvnw.cmd spring-boot:run
```

使用 IDEA 启动时，在 `BackendApplication` 运行配置的环境变量中设置 `DB_USERNAME` 和 `DB_PASSWORD` 即可；默认环境仍为 `local`。

如需使用原来的 H2 文件数据库，将活动环境设为 `h2`（配置位于 `application-h2.yml`），或在 `backend` 目录执行：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

H2 数据文件保存在启动工作目录下的 `.local/tourism.mv.db`；从 `backend` 目录启动时为 `backend/.local/tourism.mv.db`，从项目根目录启动时为 `.local/tourism.mv.db`。上传目录同样相对于启动工作目录。切换数据库不会自动迁移已有数据。Swagger 地址为 <http://localhost:8080/swagger-ui.html>。

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

复制根目录 [`.env.example`](./.env.example) 后按环境填写。生产环境会对 JWT、HTTPS 域名、S3 和高德 Key 做启动前校验，缺项直接拒绝启动。仓库不保存默认 JWT 密钥或地图 Key；旧 Key 若曾公开，应立即在供应商控制台轮换。

定位只在用户主动开启后运行：移动约 20 米或最长 60 秒上报一次。微信小程序在开通后台定位能力后可持续共享；H5 仅在页面可见时更新，切到后台会暂停并在返回后恢复。手动停止、退出登录或活动结束会删除位置，异常断线数据 90 秒后不再返回。拒绝权限后可使用后端地址搜索或共享固定坐标。

更多说明见：

- [架构](docs/architecture.md)
- [API](docs/api.md)
- [部署](docs/deployment.md)
- [测试](docs/testing.md)

## 仓库约定

- `backend/target`、`frontend/node_modules`、`frontend/dist`、本地数据库、上传内容和环境密钥不得提交。
- 数据库由 Flyway 从零创建，不兼容旧库；开发期结构变更必须新增迁移文件。
- 当前内置角色为活动负责人和参与者，权限属于单个活动；暂不包含平台管理员后台和独立组织架构管理。
