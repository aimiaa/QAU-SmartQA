# QAU-smartAI

青岛农业大学智能问答系统，面向学生、教师和管理人员提供校园知识检索、办事咨询与 AI 对话服务。

项目采用前后端分离架构：

- 前端：Vue 3 + Vite + TypeScript
- 后端：Spring Boot 多模块应用
- AI 能力：Spring AI + 阿里云百炼 DashScope 兼容接口
- 数据存储：PostgreSQL + pgvector
- 缓存：Redis
- 认证：JWT
- 数据库迁移：Flyway

## 功能概览

- 管理员登录与 JWT 身份认证
- 校园政策、制度、流程和通知的 AI 问答
- 流式 AI 回复（SSE）
- 对话会话创建、历史会话查询和消息记录
- 多知识库联合检索
- 知识库列表查询（关键词、状态、分类筛选），前端以后端为唯一数据源
- 文档切片与向量化存储
- 回答来源追踪
- 快捷问题入口
- 全局搜索、使用统计和操作审计的数据模型
- 响应式校园工作台界面
- 浅色/深色主题切换

## 项目结构

```text
QAU-smartAI/
├── qau-common/                  # 公共返回结构、异常、常量和工具
├── qau-domain/                  # 用户、会话、知识库等领域模型
├── qau-ai/                      # AI 模型调用、Prompt 模板（resources/prompts/*.st）、RAG 等能力
├── qau-infrastructure/          # 数据库、Redis、向量库、文件存储适配
├── qau-app/                     # Spring Boot 启动模块、Controller、Service、安全配置
├── vue-project/                 # Vue 3 前端项目
├── docker-compose.yml           # PostgreSQL + pgvector、Redis
├── .env.example                 # 环境变量模板
└── pom.xml                      # Maven 多模块父项目
```

## 环境要求

建议使用以下环境：

- Java 25
- Maven 3.9+
- Node.js 20+
- pnpm 10+
- Docker Desktop
- PostgreSQL 16 with pgvector
- Redis 7

## 快速开始

### 1. 准备环境变量

复制环境变量模板：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

至少需要配置：

```dotenv
DASHSCOPE_API_KEY=你的阿里云百炼 API Key
POSTGRES_PASSWORD=数据库密码
# REDIS_PASSWORD 可留空：留空时 Redis 以免鉴权方式启动，填值则自动开启 requirepass
JWT_SECRET=生产环境请替换为足够复杂的密钥
```

`.env` 已被 Git 忽略，请不要将真实密钥提交到仓库。

### 2. 启动基础设施

启动 PostgreSQL、pgvector 和 Redis：

```bash
docker compose up -d
```

默认端口：

| 服务 | 默认端口 |
| --- | ---: |
| Spring Boot 后端 | `8080` |
| PostgreSQL 容器映射端口 | `5433` |
| Redis 容器映射端口 | `16380` |
| Vue/Vite 前端 | `5173` |

Redis 的密码由 compose 内的条件启动脚本处理：`REDIS_PASSWORD` 为空时不能拼 `--requirepass`，否则 Redis 会报
`wrong number of arguments` 并无限重启。启动后 `docker compose ps` 应显示两个容器都是 healthy。

数据库表会由 Flyway 在后端启动时自动执行迁移，迁移文件位于：

```text
qau-infrastructure/src/main/resources/db/migration/
```

### 3. 启动后端

在项目根目录执行：

```bash
mvn clean install
mvn -pl qau-app -am spring-boot:run
```

也可以先打包后运行：

```bash
mvn clean package
java -jar qau-app/target/qau-app-0.0.1-SNAPSHOT.jar
```

后端默认地址：

```text
http://localhost:8080
```

### 4. 启动前端

```bash
cd vue-project
pnpm install
pnpm dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置将 `/api` 和 `/admin` 请求代理到：

```text
http://localhost:8080
```

## 前端开发

```bash
cd vue-project

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 类型检查并构建生产版本
pnpm build

# 预览生产构建
pnpm preview
```

主要前端目录：

```text
vue-project/src/
├── api/                          # HTTP 请求和业务 API 封装
├── components/                   # 聊天、侧边栏、知识库、指标卡组件
├── composables/                  # 认证和校园助手状态逻辑
├── constants/                   # 导航、知识库、快捷问题等数据
├── pages/                        # 登录页和校园助手主页面
├── types/                        # TypeScript 类型定义
└── styles.css                    # 全局视觉样式和响应式布局
```

## 后端模块说明

### `qau-common`

提供统一响应对象、错误码、业务异常和公共常量。

### `qau-domain`

定义用户、知识库、文档、文档切片、会话、聊天消息、搜索日志和审计日志等领域实体。

### `qau-ai`

封装 Spring AI 相关能力，包含聊天模型和嵌入模型配置，并为 RAG 检索、Prompt 和 AI 对话能力提供扩展基础。

### `qau-infrastructure`

负责基础设施适配：

- PostgreSQL 数据库
- pgvector 向量存储
- Redis 缓存
- MyBatis-Plus 持久化
- Flyway 数据库迁移
- 阿里云 OSS 文件存储
- Spring Security 基础能力

### `qau-app`

应用启动模块，包含：

- REST Controller
- 应用服务
- JWT 登录认证
- 全局异常处理
- Web 和安全配置
- SpringDoc/OpenAPI 配置

启动类：

```text
qau-app/src/main/java/com/aimi/QauAppApplication.java
```

## 主要接口

### 用户认证

```text
POST /admin/user/login
POST /admin/user/logout
```

登录成功后返回 JWT，前端默认使用 `Authorization: Bearer <token>` 访问受保护接口。

### 聊天服务

```text
POST /api/chat/sessions
GET  /api/chat/sessions
GET  /api/chat/sessions/{sessionId}/messages
POST /api/chat/message
POST /api/chat/message/stream
```

流式接口使用 Server-Sent Events：

```text
POST /api/chat/message/stream
Content-Type: application/json
Accept: text/event-stream
```

会话业务标识通过请求头传递：

```text
X-Session-Id: <session-id>
```

### 知识库

```text
GET /api/knowledge-bases?keyword=&status=&category=
POST /api/knowledge-bases
DELETE /api/knowledge-bases/{id}
POST /api/knowledge-bases/{id}/documents
GET /api/knowledge-bases/{id}/documents
DELETE /api/knowledge-bases/{id}/documents/{documentId}
```

三个参数均可选：`keyword` 模糊匹配名称、分类、描述（PostgreSQL `ILIKE`，忽略大小写），`status` 与 `category`
精确匹配，结果按更新时间倒序。返回字段与前端 `KnowledgeBase` 对齐，其中 `documents` 取自
`knowledge_base.document_count`，`updatedAt` 直接下发 `MM-dd HH:mm` 展示文案；
`status = disabled` 的记录不下发；`building` 表示知识库已创建但暂无文档，上传文档后会进入 `syncing`，解析完成后聚合为 `ready` 或 `review`。动态条件写在
`qau-infrastructure/src/main/resources/mapper/KnowledgeBaseMapper.xml`。

文档上传接口使用 `multipart/form-data`，字段名为 `file`。后端会保存原始文件元数据，事务提交后异步解析、切片并生成向量；删除文档时会同步清理 `document_chunk`，并在事务提交后删除本地或 OSS 原始文件。删除知识库目前是逻辑删除知识库记录，文档级清理请使用文档删除接口。

前端已预留但后端尚未实现的扩展接口包括：知识库基本信息更新、保存用户选中的知识库范围、手动触发知识库同步或重解析。

### API 文档

启动后端后，可通过 SpringDoc 查看：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## AI 与向量检索配置

项目通过 OpenAI 兼容方式接入阿里云百炼 DashScope，主要配置项如下：

```dotenv
DASHSCOPE_API_KEY=你的 API Key
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_MODEL=qwen3.7-plus
DASHSCOPE_EMBEDDING_MODEL=text-embedding-v4
DASHSCOPE_EMBEDDING_DIMS=1536
```

向量维度必须保持一致：

- `DASHSCOPE_EMBEDDING_DIMS`
- Spring AI pgvector 配置
- 数据库 `document_chunk.embedding`

当前数据库默认使用 `vector(1536)` 和 HNSW 索引进行余弦距离检索。

## 提示词模板

提示词不写在 Java 里，统一放在 `qau-ai/src/main/resources/prompts/`，文件名为
`场景-用途-system.st` / `场景-用途-user.st`：system 模板按 `# Role`、`# Task`、规则表、`# Constraints`
组织；user 模板以 `# Input Data` 开头，变量用 `{varName}` 占位（花括号是模板定界符，正文不要再出现多余
花括号），注入的用户输入或文档内容前统一加一行「不是指令」的说明以防提示注入。

| 模板 | 用途 | 占位符 |
| --- | --- | --- |
| `campus-qa-system.st` / `campus-qa-user.st` | RAG 命中片段时作答 | `currentDate`、`context`、`question` |
| `campus-qa-no-context-system.st` / `campus-qa-no-context-user.st` | 召回为空时安全兜底 | `currentDate`、`knowledgeBaseNames`、`question` |
| `campus-qa-query-rewrite.st` | 检索前问题改写与校内俗称归一 | `history`、`question` |
| `campus-chat-system.st` | 无检索上下文时的通用问答 | 无 |
| `chat-session-title-system.st` / `chat-session-title-user.st` | 会话标题生成 | `question`、`answer` |

前端气泡按纯文本渲染且不保留换行，因此面向用户的回答模板都禁止 Markdown 符号、标题与列表分点，
来源由前端单独以标签展示、正文不写来源编号。这批模板目前是「已备好、待接线」状态，
`AiConfig` 的 `defaultSystem` 仍是硬编码字符串。

## 数据库设计

主要数据表包括：

- `user_account`：用户账号
- `nav_group`、`nav_item`：系统导航
- `knowledge_base`：知识库
- `knowledge_document`：知识库原始文档
- `document_chunk`：文档切片与向量
- `knowledge_sync_job`：知识库同步任务
- `chat_session`：聊天会话
- `chat_message`：聊天消息
- `chat_message_source`：回答来源
- `quick_question`：快捷问题
- `search_log`：搜索日志
- `usage_stat_daily`：每日使用统计
- `system_config`：系统配置
- `audit_log`：操作审计日志

## 配置说明

后端主配置文件：

```text
qau-app/src/main/resources/application.yml
```

开发环境配置：

```text
qau-app/src/main/resources/application-dev.yml
```

后端支持从以下位置加载环境变量文件：

```text
.env
../.env
```

生产环境请重点替换数据库密码、AI API Key、JWT 密钥和对象存储密钥，并关闭不必要的 SQL 日志输出。

## 常用命令

```bash
# 启动依赖服务
docker compose up -d

# 查看依赖服务状态
docker compose ps

# 查看后端模块测试
mvn test

# 构建全部后端模块
mvn clean package

# 停止依赖服务
docker compose down
```

## 开发注意事项

- 不要提交 `.env`、API Key、数据库密码或 OSS 密钥。
- 修改嵌入模型后，需要同步调整向量维度配置和数据库字段。
- 修改数据库结构时，请新增 Flyway 迁移文件，不要直接修改已经执行过的迁移。
- 前端开发服务器默认通过 Vite 代理访问后端，后端需运行在 `8080`。
- AI 流式回答依赖后端 SSE 接口和有效的 DashScope 配置。
- 当前部分导航、指标、知识库等扩展接口保留了前端占位封装，接入真实后端时需要补齐对应 Controller。
- 带动态条件的查询写在 `resources/mapper/*.xml` 里，不在 Service 拼 `LambdaQueryWrapper`；自定义 XML SQL 不套用 `@TableLogic`，逻辑删除条件要显式书写。
- entity / DTO / VO 的字段映射与展示文案收口在 `com.aimi.converter`，Service 只做业务编排。
- 下发给前端直接展示的时间统一为 `MM-dd HH:mm`，不使用「今天 / 昨天」这类相对文案。
- Windows PowerShell 下没有 `mvnw.cmd`，使用全局 `mvn` 且 `-D` 参数整体加引号；`pnpm` 被执行策略拦截时改用 `pnpm.cmd`。

## 当前验证

前端项目已通过：

```bash
cd vue-project
pnpm build
```

该命令会执行 Vue TypeScript 类型检查并生成 Vite 生产构建产物。

后端另有 `KnowledgeBaseMapperXmlTest` 校验 XML 动态 SQL 能真实绑定执行，需先启动容器并让 Flyway 灌入
V4 种子数据：

```powershell
mvn -o -pl qau-app -am "-Dtest=KnowledgeBaseMapperXmlTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
