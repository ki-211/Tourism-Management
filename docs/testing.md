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
npm audit --omit=dev --audit-level=critical
```

常规后端测试使用内存 H2 和独立上传目录。安装 Docker 时，Testcontainers 会额外启动 MySQL、MinIO 和 Redis，以 `prod` profile 验证真实迁移、生产配置和服务连通性；未安装 Docker 时该项明确跳过。构建产物均被 `.gitignore` 排除。

前端已通过 `overrides` 固定可兼容的 i18n、JPEG 与 HTTP 依赖安全版本。当前审计仍会报告 DCloud 构建链固定的 `adm-zip` High（及其受影响包聚合项）；该依赖不进入 H5 静态产物或小程序运行时代码，CI 暂以 Critical 为阻断级别，升级 uni-app 工具链时需重新审计并移除不再需要的覆盖。

后端本地启动后，可在仓库根目录执行真实 HTTP 冒烟。脚本会生成一次性账号，覆盖邀请制活动、封面、报名、聊天、相册、位置开启/停止、JSON 与照片签到、车辆、负责人转让、资料修改及令牌轮换/退出：

```powershell
.\scripts\smoke.ps1
```

若后端不在默认端口，可传入 `-ApiBase 'http://localhost:18080/api/v1'`。

## 核心联调

准备两个新账号，按顺序验证：注册登录；创建公开活动和邀请制活动；第二账号通过邀请码预览并报名；双向聊天；上传相册；开启和停止位置；活动负责人发布签到、参与者提交位置/地址/备注/照片；发布车辆；转让负责人；修改资料、刷新令牌并退出。

H5 需验证高德地图加载、定位允许/拒绝、地址解析、固定位置、切后台暂停与返回恢复。微信开发者工具及真机需验证首次授权、拒绝后打开设置、锁屏和切后台持续定位、20 米位移/60 秒心跳、手动停止与活动结束自动停止。网络恢复后检查 WebSocket 增量标记与共享续期，切换聊天/签到/相册时共享不得中断，退出登录后不得残留监听、定时器或位置记录。

自动测试和本机构建不能替代微信真机权限、真实 HTTPS/WSS 和多人并发验收；这些边界应在发布环境单独记录。上线门禁至少包括：后端 `verify`、H5/微信双构建、Critical 依赖审计、Compose 健康检查、HTTP 冒烟和一次微信真机完整流程。
