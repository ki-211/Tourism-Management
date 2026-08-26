# 架构说明

## 模块

后端按 `auth`、`activity`、`attendance`、`room`、`location`、`media`、`vehicle`、`common` 划分。Controller 只处理协议和校验，Service 负责事务与权限，Repository 负责持久化。操作者始终来自 JWT，不接受客户端传入的 `userId` 或 `creatorId`。

前端以 `services/api.ts` 统一请求和一次性刷新，以 `services/realtime.ts` 管理 WebSocket。活动室拆为聊天、签到、相册和位置四个组件，父页面销毁时关闭连接，各组件清理自己的定时器和位置共享。

## 数据与一致性

Flyway `V1__baseline.sql` 创建用户、刷新令牌、活动、报名、车辆、聊天、签到任务/记录、媒体、相册和共享位置表。创建活动与创建者报名、团长转让、签到写入均在事务中执行；唯一约束阻止重复报名和重复签到。

HTTP 是最终事实来源。WebSocket 仅推送 `CHAT_CREATED`、`SIGN_TASK_CREATED`、`SIGN_RECORD_CREATED`、`PHOTO_ADDED`、`LOCATION_UPDATED`、`LOCATION_REMOVED`、`CREATOR_TRANSFERRED` 等变更通知，客户端收到通知后补拉 HTTP 数据。

## 鉴权

访问令牌有效期 15 分钟。刷新令牌有效期 30 天，只保存 SHA-256 摘要，每次刷新都会撤销旧令牌并签发新令牌。WebSocket 先通过鉴权接口领取 60 秒单用途时效 ticket，再连接 `/ws?ticket=...`。

## 文件与位置

图片先按文件头识别，只允许 JPEG、PNG、WebP，单文件不超过 10 MB，并使用随机对象名。本地 profile 写入本地目录；生产 profile 使用 S3 兼容存储。

位置坐标由端侧通过原生 `uni.getLocation` 获取。地址搜索和逆地理编码由后端代理高德 Web Service，Key 不下发前端。位置记录按活动和用户唯一，关闭即删除，查询只返回 60 秒内的活跃数据。
