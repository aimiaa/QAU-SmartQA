# QAU AI Backend Entity Reference

这些文件是给 Java / Spring Boot 后端使用的参考设计，不参与当前 Vue 前端构建。

## 推荐后端组合

- Spring Boot
- MyBatis-Plus
- PostgreSQL + pgvector
- Redis
- MinIO / OSS

## 与前端模块的对应关系

| 前端数据 / API | 后端实体 | 数据表 | 作用 |
| --- | --- | --- | --- |
| `NavGroup` / `NavItem` | `NavGroupEntity`, `NavItemEntity` | `nav_group`, `nav_item` | 侧边栏菜单配置 |
| `KnowledgeBase` | `KnowledgeBaseEntity` | `knowledge_base` | 知识库列表、状态、分类 |
| 知识库文档 | `KnowledgeDocumentEntity` | `knowledge_document` | 存原始文档元数据 |
| 文档向量切片 | `DocumentChunkEntity` | `document_chunk` | RAG 检索的文本块与向量 |
| 同步状态 | `KnowledgeSyncJobEntity` | `knowledge_sync_job` | 文档解析、向量化任务记录 |
| `ChatSession` | `ChatSessionEntity` | `chat_session` | 对话历史 |
| 会话知识库范围 | `ChatSessionKnowledgeBaseEntity` | `chat_session_knowledge_base` | 保存会话选择了哪些知识库 |
| `ChatMessage` | `ChatMessageEntity` | `chat_message` | 用户问题与 AI 回复 |
| 回答来源 | `ChatMessageSourceEntity` | `chat_message_source` | AI 回复引用的知识库文档来源 |
| `QuickQuestion` | `QuickQuestionEntity` | `quick_question` | 快捷问题按钮配置 |
| 顶部搜索 | `SearchLogEntity` | `search_log` | 搜索记录与统计分析 |
| 首页指标 / 看板 | `UsageStatDailyEntity` | `usage_stat_daily` | 每日问答、搜索、文档统计 |
| 系统设置 | `SystemConfigEntity` | `system_config` | 模型、接口、系统参数配置 |
| 权限审计 | `AuditLogEntity` | `audit_log` | 用户操作审计 |
| 用户登录 | `UserEntity` | `user_account` | 用户、角色、状态 |

## 文件说明

- `sql/postgresql-schema.sql`：PostgreSQL 建表语句，包含 `pgvector`、索引、外键、表注释。
- `java-entities/*.java`：按 MyBatis-Plus 风格写的 Java 实体类。

注意：`document_chunk.embedding` 使用 `vector(1536)`，维度需要与你最终选用的 embedding 模型一致。
