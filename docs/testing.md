# 测试与验收

## 自动化命令

```powershell
cd backend
.\mvnw.cmd verify

cd ..\frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build:h5
npm run build:mp-weixin
```

后端测试使用内存 H2 和独立上传目录，不访问生产数据库。构建产物均被 `.gitignore` 排除。

后端本地启动后，可在仓库根目录执行真实 HTTP 冒烟。脚本会生成一次性账号，覆盖邀请制活动、封面、报名、聊天、相册、位置开启/停止、JSON 与照片签到、车辆、团长转让、资料修改及令牌轮换/退出：

```powershell
.\scripts\smoke.ps1
```

若后端不在默认端口，可传入 `-ApiBase 'http://localhost:18080/api/v1'`。

## 核心联调

准备两个新账号，按顺序验证：注册登录；创建公开活动和邀请制活动；第二账号通过邀请码预览并报名；双向聊天；上传相册；开启和停止位置；团长发布签到、成员提交位置/地址/备注/照片；发布车辆；转让团长；修改资料、刷新令牌并退出。

H5 需验证定位允许、拒绝、搜索和手动坐标。微信开发者工具及真机需分别验证定位、相机和相册的拒绝、重试、成功路径。网络断开后恢复时检查 WebSocket 重连和 HTTP 补拉，离开活动室后确认没有残留连接、定时器或位置记录。

自动测试和本机构建不能替代微信真机权限、真实 HTTPS/WSS、MySQL/S3 和多人并发验收；这些边界应在发布环境单独记录。
