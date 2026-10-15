# 本地开发环境

本文说明后端在本机的数据库配置方式、配置文件的分工与优先级，以及常见报错的排查方法。前端的启动方式见 [README](../README.md)。

## 运行环境一览

| 环境（profile） | 用途 | 数据库 | 配置文件 | 如何启用 |
|---|---|---|---|---|
| `local` | 日常开发（默认） | 本机 MySQL | `application-local.yml` | 不指定 profile 时自动启用 |
| `h2` | 零依赖开发 / 演示 | H2 文件库 `.local/tourism.mv.db` | `application-h2.yml` | `-Dspring-boot.run.profiles=h2` 或 `--spring.profiles.active=h2` |
| `test` | 自动化测试 | H2 内存库 | `src/test/resources/application-test.yml` | 测试类上的 `@ActiveProfiles("test")` |
| `prod` | 生产部署 | MySQL 8.4 + Redis + S3 | `application-prod.yml` + 环境变量 | Docker 镜像启动参数固定为 `prod` |

所有环境共用 `application.yml` 中的通用配置；数据库结构统一由 Flyway 从 `db/migration` 创建和升级，不需要手工建表。

## 使用 MySQL（默认）

### 1. 准备 MySQL

本机已有 MySQL 8.x 即可。连接串带有 `createDatabaseIfNotExist=true`，账号有建库权限时，`tourism` 库会在首次启动时自动创建；没有建库权限就先手工建库：

```sql
CREATE DATABASE tourism DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

没有 MySQL 也可以用 Docker 临时起一个：

```powershell
docker run -d --name tourism-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=change-me mysql:8.4
```

### 2. 提供账号密码（二选一）

仓库中的 `application-local.yml` 只包含占位符，**不要把真实账号密码写进这个文件**。

**方式 A：环境变量**

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_USERNAME` | 空 | 数据库账号，必填 |
| `DB_PASSWORD` | 空 | 数据库密码，必填 |
| `DB_HOST` | `localhost` | 数据库地址 |
| `DB_PORT` | `3306` | 端口 |
| `DB_NAME` | `tourism` | 库名 |
| `DB_URL` | 由以上几项拼出 | 设置后整体替换 JDBC 连接串 |

PowerShell（只对当前终端生效）：

```powershell
cd backend
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的密码'
.\mvnw.cmd spring-boot:run
```

IDEA：在 `BackendApplication` 的运行配置 → Environment variables 中填写 `DB_USERNAME=root;DB_PASSWORD=你的密码`。

**方式 B：个人配置文件 `application-secret.yml`**

```powershell
cd backend\src\main\resources
Copy-Item application-secret.yml.example application-secret.yml
# 编辑 application-secret.yml，填入账号密码
```

- 该文件已被 `.gitignore` 和 `backend/.dockerignore` 排除，不会被提交，也不会打进生产镜像。
- 只在 `local` 环境加载（`application-local.yml` 中的 `spring.config.import`），其中的值会覆盖 `application-local.yml`。
- 除了账号密码，也可以在这里覆盖 `spring.datasource.url` 等任何本地配置。

### 3. 启动并确认

启动日志中出现以下内容即表示连接成功、迁移完成：

```
Database: jdbc:mysql://localhost:3306/tourism ...
Successfully applied N migrations to schema `tourism`, now at version vN
Started BackendApplication in ... seconds
```

也可以访问 <http://localhost:8080/actuator/health>，返回 `{"status":"UP"}` 即正常。

## 使用 H2（零依赖）

不想安装 MySQL 时使用：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

- 数据文件在**启动工作目录**下的 `.local/tourism.mv.db`，上传文件在 `.local/uploads`。从 `backend` 目录启动时位于 `backend/.local/`；从项目根目录启动时位于 `.local/`。两个位置都已加入 `.gitignore`。
- H2 控制台：<http://localhost:8080/h2-console>，JDBC URL 填日志中打印的地址，用户名 `sa`，密码留空。
- MySQL 与 H2 的数据互不相通，切换环境不会迁移已有数据。

## 配置优先级

同一个配置项出现在多处时，后者覆盖前者：

1. `application.yml`（通用默认值）
2. `application-local.yml` / `application-h2.yml`（当前环境）
3. `application-secret.yml`（仅 `local` 环境，个人覆盖）
4. 环境变量（如 `DB_PASSWORD`，或 `SPRING_DATASOURCE_URL` 这类 Spring 标准变量）
5. 启动参数（如 `--spring.datasource.url=...`）

## 从旧版本升级

`8fb046d` 曾把 `application-local.yml` 改为本地忽略文件，现在它已重新纳入版本库（只含占位符）。

> **注意**：Git 会把被忽略的文件视为可以随意覆盖。如果你本机还保留着那个被忽略、写有真实账号密码的 `application-local.yml`，`git pull` **不会报错，而是直接用仓库版本覆盖它**，账号密码随之丢失（已实测）。

请在拉取**之前**处理：

1. 把旧文件里的账号密码挪到 `application-secret.yml`（见上文方式 B），或改用环境变量；
2. 执行 `git pull`（本机的 `application-local.yml` 会被换成占位符版本，这是预期行为）；
3. 执行一次 `.\mvnw.cmd clean`，清掉 `target/classes` 中旧构建残留的配置文件（见下文"启动后连的不是 MySQL"）；
4. 启动后端，确认日志中是 `jdbc:mysql://...`。

如果已经被覆盖了也不要紧：按"使用 MySQL"一节重新提供账号密码即可，数据库里的数据不受影响。

## 常见问题

**`Access denied for user 'xxx'@'localhost' (using password: NO)`**
没有提供密码：环境变量没设置，或 `application-secret.yml` 不存在、不在 `src/main/resources` 目录下。注意 PowerShell 中的 `$env:` 只对当前终端生效，IDEA 启动需要在运行配置里另外设置。

**`Access denied ... (using password: YES)`**
账号或密码错误，或该账号没有访问 `tourism` 库（或建库）的权限。

**`Communications link failure` / `Connection refused`**
MySQL 没有启动，或地址、端口不对。检查 `DB_HOST`、`DB_PORT`，以及 MySQL 是否监听 `3306`。

**启动后连的不是 MySQL（日志里是 `jdbc:h2:...`）**
Maven 增量构建不会删除 `target/classes` 中已经不存在的资源文件，旧版本残留的配置可能仍然生效。执行 `.\mvnw.cmd clean` 后重新启动。如果日志里是 `jdbc:h2:mem:...`，说明当前环境没有任何数据源配置，Spring Boot 自动退回到了内存数据库，重启后数据会全部丢失，请确认 `application-local.yml` 存在并且启用的是 `local` 环境。

**Flyway 报 `Validate failed` / `checksum mismatch`**
已经执行过的迁移文件被修改过。开发期结构变更必须新增迁移文件（如 `V9__xxx.sql`），不能改动已执行的旧文件。本地库可以直接删库重建：`DROP DATABASE tourism;`，启动时会自动重新迁移。

**`Table 'tourism.xxx' doesn't exist` / `Schema-validation: missing column`**
数据库停留在旧版本，或曾用 H2 时手动改过结构。确认启动日志中的 Flyway 版本号已是最新；必要时删库重建。
