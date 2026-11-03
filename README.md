# QAU-smartAI

青岛农业大学智能问答系统，为学生、教师和管理人员提供校园知识检索、办事咨询与 AI 对话服务。

项目采用前后端分离架构：

- 前端：Vue 3、Vite、TypeScript
- 后端：Spring Boot 多模块应用
- AI：Spring AI，使用阿里云百炼 DashScope 兼容接口
- 数据库：PostgreSQL、pgvector、pg_trgm
- 基础设施：Redis（已配置连接，当前业务链路尚未使用）
- 认证：JWT
- 数据库迁移：Flyway

## 功能概览

- 用户登录与 JWT 身份认证
- 多轮校园问答，支持 SSE 流式输出、会话历史和回答来源
- 多知识库范围检索
- 文档上传、持久化解析任务、文本切片和向量化
- 向量与关键词混合召回、RRF 融合及 DashScope 重排
- RAG 查询改写和受限的会话短期记忆
- RAG 离线检索评估代码与评估用例表
- 响应式校园工作台及浅色/深色主题

## 项目结构

    QAU-smartAI/
    ├── qau-common/          # 公共返回结构、异常、常量和工具
    ├── qau-domain/          # 用户、会话、知识库等领域实体
    ├── qau-ai/              # 模型调用、RAG、切片和提示词模板
    ├── qau-infrastructure/  # 数据库、Redis、向量库、文件存储适配
    ├── qau-app/             # Spring Boot 启动模块、Controller、Service、安全配置
    ├── vue-project/         # Vue 3 前端
    ├── docker-compose.yml   # PostgreSQL + pgvector、Redis
    ├── .env.example         # 环境变量模板
    └── pom.xml              # Maven 多模块父项目

## 环境要求

- JDK 25
- Maven 3.9+
- Node.js 20+
- pnpm 10+
- Docker Desktop
- PostgreSQL 16（启用 pgvector、pg_trgm）
- Redis 7

## 快速开始

### 1. 配置环境变量

复制模板并按需填写：

    # macOS / Linux
    cp .env.example .env

    # Windows PowerShell
    Copy-Item .env.example .env

至少配置 DashScope API Key、数据库密码和生产环境 JWT 密钥（在 .env 中添加 JWT_SECRET）。真实密钥不要提交到仓库；.env 已加入 Git 忽略规则。

当前 application.yml 将文件存储类型配置为 OSS。上传文档前需要配置 OSS_REGION、OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET 和 OSS_BUCKET_NAME。若本地开发不使用 OSS，可将 app.knowledge.storage.type 改为 local；本地文件默认保存在 ./data/knowledge-files。

REDIS_PASSWORD 留空时 Redis 不启用密码；设置密码后，Compose 会以 requirepass 启动 Redis 并使用对应密码做健康检查。Redis 容器已纳入开发环境，但当前业务代码没有 Redis 命令调用，现有聊天、检索和文档解析流程不依赖它。

### 2. 启动依赖服务

    docker compose up -d
    docker compose ps

默认端口：

| 服务 | 端口 |
| --- | ---: |
| Spring Boot | 8080 |
| PostgreSQL 宿主机映射 | 5433 |
| Redis 宿主机映射 | 16380 |
| Vite | 5173 |

Flyway 会在后端启动时执行 qau-infrastructure/src/main/resources/db/migration/ 下的迁移。

### 3. 启动后端

在项目根目录运行：

    mvn clean install
    mvn -pl qau-app -am spring-boot:run

后端默认地址为 http://localhost:8080。

### 4. 启动前端

    cd vue-project
    pnpm install
    pnpm dev

前端默认地址为 http://localhost:5173。Vite 将 /api 和 /admin 请求代理到 http://localhost:8080。

前端生产构建：

    cd vue-project
    pnpm build

## 后端模块

- qau-common：统一响应对象、错误码、业务异常和公共常量。
- qau-domain：用户、知识库、文档、切片、会话、消息等实体。
- qau-ai：聊天模型、Embedding、RAG 提示词、文档切片和重排服务。
- qau-infrastructure：PostgreSQL/pgvector、Redis 连接配置、MyBatis-Plus、Flyway、OSS/本地文件存储。
- qau-app：REST API、应用服务、JWT、安全配置、异步任务调度和 OpenAPI。

启动类：qau-app/src/main/java/com/aimi/QauAppApplication.java。

## API

### 认证

    POST /admin/user/login
    POST /admin/user/logout

受保护接口使用 Authorization: Bearer <token>。

### 聊天

    POST   /api/chat/sessions
    GET    /api/chat/sessions
    GET    /api/chat/sessions/{sessionId}/messages
    DELETE /api/chat/sessions/{sessionId}
    POST   /api/chat/message
    POST   /api/chat/message/stream

流式接口返回 Server-Sent Events。会话业务标识通过 X-Session-Id 请求头传递。

### 知识库与文档

    GET    /api/knowledge-bases?keyword=&status=&category=
    POST   /api/knowledge-bases
    DELETE /api/knowledge-bases/{id}
    POST   /api/knowledge-bases/{id}/documents
    GET    /api/knowledge-bases/{id}/documents
    DELETE /api/knowledge-bases/{id}/documents/{documentId}

文档上传使用 multipart/form-data，文件字段名为 file。知识库查询支持名称、分类、描述关键词模糊匹配，以及状态和分类精确筛选。禁用知识库不会出现在列表中。知识库状态包括 building、syncing、ready 和 review。删除知识库目前为逻辑删除；如需清理文档切片和原文件，应使用文档删除接口。

前端还保留知识库更新、所选知识库保存和手动同步等 API 封装，但后端目前没有对应接口。死信任务也尚未提供管理员查询、重试或放弃接口。

### API 文档

后端启动后可访问：

    http://localhost:8080/swagger-ui.html
    http://localhost:8080/v3/api-docs

## 文档解析任务队列

上传时先将原始文件写入 OSS 或本地存储，再在同一个数据库事务中写入文档记录和 pending 解析任务。数据库回滚时会尝试删除已写入的存储文件。任务保存在 knowledge_sync_job 表中，由定时调度器每 2 秒轮询；每轮最多领取 5 条任务。

调度器通过 PostgreSQL 的 FOR UPDATE SKIP LOCKED 并发领取任务，再交给专用线程池执行。线程池用于限制并发，不是任务可靠性的来源。线程池默认核心线程数为 2、最大线程数为 4、队列容量为 100。

任务带有 worker_id 和 10 分钟租约。应用轮询时会恢复租约过期的 running 任务。最多执行 5 次；正常解析失败路径按 1、5、15、30 分钟间隔重试，达到上限或遇到不可恢复错误后进入 dead。实现中还定义了 60 分钟退避档，但在当前 5 次执行上限下通常不会触发。对同一文档和任务类型有活动任务唯一约束，删除文档时会取消其活动任务。

这套机制依赖 PostgreSQL，不是 RabbitMQ 或 Kafka。当前死信查询和重试仅有内部 Mapper 能力，没有对外的管理 API/UI。

## 切片与 RAG 检索

### 文档切片

TextDocumentChunker 先按空行和常见标题切分语义块，再将相邻块合并到目标长度。当前默认目标长度为 800 字符，块间重叠为 100 字符；超过目标长度的语义块使用带重叠滑动窗口切分。参数位于 application.yml 的 app.knowledge.chunk.size 和 app.knowledge.chunk.overlap。

解析任务抽取文本后生成切片，并尝试调用 Embedding 模型。若 Embedding 未配置或调用失败，当前实现仍会保存不带向量的切片，因此这些切片不能参与向量召回，但仍可能被关键词检索命中。

### 检索、融合与重排

在线检索大致按以下顺序执行：

1. 使用 pgvector 余弦距离进行向量召回，并按相似度阈值过滤。
2. 使用 PostgreSQL pg_trgm 进行关键词召回；默认也启用。
3. 使用 Reciprocal Rank Fusion（RRF，常数 K=60）融合候选结果。
4. 若 DashScope 重排服务可用，调用 gte-rerank-v2 重新排序并过滤低分结果。
5. 返回 Top 5 切片作为回答上下文。

默认向量和关键词候选数均为 20，向量阈值为 0.3，关键词阈值为 0.05，重排最低分为 0.1。重排未启用、API Key 缺失或调用失败时，使用 RRF 融合顺序继续回答。相关参数集中在 application.yml 的 app.rag 配置下。

### 会话短期记忆

聊天消息持久化在 chat_message，会话信息在 chat_session。默认最多加载当前会话 10 条消息，总字符预算为 12000；这些消息可用于追问改写和最终回答。查询改写最多使用其中 6 条，并将较长的助手回答截断到 200 字符。

当前同步接口会先保存本轮用户消息再加载历史，因此本轮问题也可能被作为历史上下文传入；流式接口则在保存本轮用户消息前加载历史。两种接口的处理顺序目前不同，属于后续可统一的行为。

这是基于会话历史的短期上下文，不是跨会话的用户画像或长期记忆。参数位于 application.yml 的 app.rag.history 和 app.rag.query-rewrite 下。

## AI 配置与提示词

模型通过 Spring AI 的 OpenAI 兼容接口接入 DashScope。常用环境变量：

    DASHSCOPE_API_KEY=你的 API Key
    DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
    DASHSCOPE_MODEL=qwen3.7-plus
    DASHSCOPE_EMBEDDING_MODEL=text-embedding-v4
    DASHSCOPE_EMBEDDING_DIMS=1536

向量维度必须与 Spring AI pgvector 配置和数据库 document_chunk.embedding 一致。当前默认使用 vector(1536)、HNSW 索引和余弦距离。

RAG 在线链路已接入以下提示词，位于 qau-ai/src/main/resources/prompts/：

- campus-qa-system.st、campus-qa-user.st：检索命中时的回答。
- campus-qa-no-context-system.st、campus-qa-no-context-user.st：无召回结果时的回答。
- campus-qa-query-rewrite.st：检索前问题改写。
- rag-judge-system.st、rag-judge-user.st：离线评估中的可选 LLM-as-judge。

campus-chat-system.st 和 chat-session-title-* 模板当前未接入主要 RAG 对话链路。

## RAG 离线评估

项目包含 rag_eval_case 评估用例表和离线评估服务，可计算 Recall@K、Precision@K、MRR、命中率；可选启用 LLM-as-judge 评估忠实度和引用正确性。评估入口目前是测试代码，不是公开 REST API。测试需要可用的 PostgreSQL、已标注的评估用例和可检索的知识切片；启用 judge 还会调用聊天模型并产生额外费用。

显式运行评估测试：

    mvn -pl qau-app -am "-Drag.eval=true" "-Dtest=RagEvaluationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

## 数据库

主要数据表：

- user_account：用户账号
- knowledge_base：知识库
- knowledge_document：原始文档元数据
- document_chunk：文档切片及向量
- knowledge_sync_job：持久化解析任务
- chat_session、chat_message、chat_message_source：会话、消息及回答来源
- rag_eval_case：离线评估用例
- nav_group、nav_item：导航
- quick_question：快捷问题
- search_log、usage_stat_daily：搜索和使用统计
- system_config：系统配置
- audit_log：操作审计

修改数据库结构时新增 Flyway 迁移文件，不要改动已在环境中执行过的迁移。

## 开发与验证

常用命令：

    # 启动依赖
    docker compose up -d

    # 运行后端测试
    mvn test

    # 构建后端
    mvn clean package

    # 构建前端并执行 TypeScript 检查
    cd vue-project
    pnpm build

后端集成测试使用 application-test.yml，但数据库仍需按当前 .env 配置在本机可访问；目前没有配置 Testcontainers。KnowledgeBaseMapperXmlTest 可单独运行：

    mvn -o -pl qau-app -am "-Dtest=KnowledgeBaseMapperXmlTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

开发注意事项：

- 不要提交 .env、API Key、数据库密码或 OSS 密钥。
- Windows PowerShell 下可使用全局 mvn；pnpm 命令受执行策略影响时使用 pnpm.cmd。
- 动态条件 SQL 放在 resources/mapper/*.xml；自定义 SQL 显式处理逻辑删除条件。
- entity/DTO/VO 映射集中在 com.aimi.converter。
- API 返回的展示时间统一使用 MM-dd HH:mm。
