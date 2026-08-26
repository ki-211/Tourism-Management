# API 约定

基础路径为 `/api/v1`。除注册、登录、刷新、公开媒体和健康检查外均需 `Authorization: Bearer <accessToken>`。

成功与失败统一返回：

```json
{ "code": "OK", "message": "操作成功", "data": {}, "requestId": "..." }
```

分页数据为 `{ "items": [], "page": 0, "size": 20, "total": 0 }`，并同时使用正确的 HTTP 状态码。开发环境可在 `/swagger-ui.html` 查看完整 OpenAPI。

## 主要端点

- `POST /auth/register|login|refresh|logout`，`POST /auth/ws-ticket`
- `GET|PATCH /users/me`，`GET /users/me/sign-records`
- `GET|POST /activities`，`GET /activities/{id}`
- `GET /activities/invitations/{code}`，`POST /activities/{id}/invitation-code/rotate`
- `POST /activities/{id}/signups`，`GET /activities/{id}/signups`，`POST /activities/{id}/transfer`
- `GET|POST /activities/{id}/messages`
- `GET|POST /activities/{id}/photos`
- `GET /activities/{id}/locations`，`PUT|DELETE /activities/{id}/locations/me`
- `GET|POST /activities/{id}/vehicles`
- `GET|POST /activities/{id}/sign-tasks`，`GET /sign-tasks/{id}`
- `POST /sign-tasks/{id}/records`（JSON 无照片或 multipart 带照片）
- `GET /sign-tasks/{id}/summary`，`GET /sign-tasks/unsigned`
- `GET /geocoding/search`，`GET /geocoding/reverse`
- `GET /media/public/**`

图片上传字段名固定为 `file`。聊天最多 1000 字；用户名为 3–16 位字母、数字或下划线；密码 8–64 位。活动时间必须满足 `报名开始 ≤ 报名结束 ≤ 活动开始 < 活动结束`。

WebSocket 使用原生 JSON。连接后发送 `{"type":"SUBSCRIBE_ACTIVITY","activityId":1}` 订阅活动，服务端推送事件对象；发送 `{"type":"PING"}` 会收到 `PONG`。
