# 本地开发环境

本文说明后端在本机的数据库配置方式、配置文件的分工与优先级，以及常见报错的排查方法。前端的启动方式见 [README](../README.md)。

## 运行环境一览

| 环境（profile） | 用途 | 数据库 | 配置文件 | 如何启用 |
|---|---|---|---|---|
| `local` | 日常开发（默认） | 本机 MySQL | `application-local.yml`（自己从模板复制，不进仓库） | 不指定 profile 时自动启用 |
| `h2` | 零依赖开发 / 演示 | H2 文件库 `.local/tourism.mv.db` | `application-h2.yml`（仓库自带） | `-Dspring-boot.run.profiles=h2` 或 `--spring.profiles.active=h2` |
| `test` | 自动化测试 | H2 内存库 | `src/test/resources/application-test.yml` | 测试类上的 `@ActiveProfiles("test")` |
| `prod` | 生产部署 | MySQL 8.4 + Redis + S3 | `application-prod.yml` + 环境变量 | Docker 镜像启动参数固定为 `prod` |

所有环境共用 `application.yml` 中的通用配置；数据库结构统一由 Flyway 从 `db/migration` 创建和升级，不需要手工建表。

`backend/src/main/resources` 下与本地开发相关的文件：

| 文件 | 是否在仓库中 | 说明 |
|---|---|---|
| `application-local.yml.example` | 是 | 本地 MySQL 配置模板，只有占位符 |
| `application-local.yml` | **否**（已被 `.gitignore`、`.dockerignore` 排除） | 从模板复制的个人配置，写你自己的账号密码 |
| `application-h2.yml` | 是 | H2 环境配置，不含任何密码 |

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

### 2. 复制配置模板并填写账号密码

```powershell
cd backend\src\main\resources
Copy-Item application-local.yml.example application-local.yml
```

用编辑器打开 `application-local.yml`，把账号密码改成你自己的，例如：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tourism?...   # 保持模板里的写法；库名、端口不同时再改
    username: root
    password: 123456
```

这个文件只存在于你的电脑上：不会被提交，不会被 `git pull` 覆盖，也不会被打进 Docker 镜像。

**不想在文件里写密码？** 保留模板中的占位符 `${DB_USERNAME:root}`、`${DB_PASSWORD:}` 不动，改用环境变量：

- PowerShell（只对当前终端生效）：`$env:DB_USERNAME = 'root'; $env:DB_PASSWORD = '你的密码'`
- IDEA：`BackendApplication` 运行配置 → Environment variables 填 `DB_USERNAME=root;DB_PASSWORD=你的密码`

### 3. 启动并确认

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

使用 IDEA 时，新建或修改 `application-local.yml` 后先执行一次 Build → Rebuild Project，让它被复制到 `target/classes`。

启动日志中出现以下内容即表示连接成功、迁移完成：

```
Database: jdbc:mysql://localhost:3306/tourism ...
Successfully applied N migrations to schema `tourism`, now at version vN
Started BackendApplication in ... seconds
```

也可以访问 <http://localhost:8080/actuator/health>，返回 `{"status":"UP"}` 即正常。

## 使用 H2（零依赖）

不想安装 MySQL 时使用，不需要 `application-local.yml`：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

IDEA 中在运行配置的 Active profiles 填 `h2` 即可。

- 数据文件在**启动工作目录**下的 `.local/tourism.mv.db`，上传文件在 `.local/uploads`。从 `backend` 目录启动时位于 `backend/.local/`；从项目根目录启动时位于 `.local/`。两个位置都已加入 `.gitignore`。
- H2 控制台：<http://localhost:8080/h2-console>，JDBC URL 填日志中打印的地址，用户名 `sa`，密码留空。
- MySQL 与 H2 的数据互不相通，切换环境不会迁移已有数据。

## 配置优先级

同一个配置项出现在多处时，后者覆盖前者：

1. `application.yml`（通用默认值）
2. `application-local.yml` / `application-h2.yml`（当前环境）
3. 环境变量（如 `DB_PASSWORD`，或 `SPRING_DATASOURCE_URL` 这类 Spring 标准变量）
4. 启动参数（如 `--spring.datasource.url=...`）

## 从旧版本升级

`application-local.yml` 曾短暂作为占位符文件提交在仓库里（`210f67f`），现在改为由每个人从模板复制的本地文件，仓库中只保留 `application-local.yml.example`。拉取这次更新时：

- **没改过**仓库里那份 `application-local.yml`：直接 `git pull`，它会被移除；然后按"复制配置模板"重新建一份。
- **改过**（在里面写了账号密码）：先备份再拉取：

  ```powershell
  cd backend\src\main\resources
  Copy-Item application-local.yml application-local.yml.bak
  git checkout -- application-local.yml
  git pull
  Move-Item application-local.yml.bak application-local.yml
  ```

  拉取后这个路径已被忽略，放回去的文件不会再出现在 `git status` 里。

- 如果之前按旧说明建过 `application-secret.yml`，把其中的账号密码挪到 `application-local.yml` 后删除它即可，该文件已不再被读取。

最后执行一次 `.\mvnw.cmd clean`，清掉 `target/classes` 里旧构建残留的配置文件。

## 常见问题

**启动时提示"缺少本地配置文件 application-local.yml"**
默认的 `local` 环境需要这个文件，按"复制配置模板并填写账号密码"操作即可；IDEA 用户复制后记得 Rebuild。若只是想快速跑起来，改用 H2 环境。

**`Access denied for user 'xxx'@'localhost' (using password: NO)`**
没有读到密码：`application-local.yml` 里还是占位符 `${DB_PASSWORD:}`，并且也没有设置环境变量。把密码直接写进文件，或设置 `DB_PASSWORD`。注意 PowerShell 中的 `$env:` 只对当前终端生效，IDEA 启动需要在运行配置里另外设置。报错中的用户名如果是你的 Windows 登录名，说明账号也没有配上。

**`Access denied ... (using password: YES)`**
账号或密码错误，或该账号没有访问 `tourism` 库（或建库）的权限。

**`Communications link failure` / `Connection refused`**
MySQL 没有启动，或地址、端口不对。检查 `application-local.yml` 中的 `url`，以及 MySQL 是否监听 `3306`。

**改了 `application-local.yml` 却不生效**
运行的是 `target/classes` 中的旧副本。IDEA 中 Rebuild Project，或执行 `.\mvnw.cmd clean` 后重新启动。

**Flyway 报 `Validate failed` / `checksum mismatch`**
已经执行过的迁移文件被修改过。开发期结构变更必须新增迁移文件（如 `V9__xxx.sql`），不能改动已执行的旧文件。本地库可以直接删库重建：`DROP DATABASE tourism;`，启动时会自动重新迁移。

**`Table 'tourism.xxx' doesn't exist` / `Schema-validation: missing column`**
数据库停留在旧版本，或曾用 H2 时手动改过结构。确认启动日志中的 Flyway 版本号已是最新；必要时删库重建。
