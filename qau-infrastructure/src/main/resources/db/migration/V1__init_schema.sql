-- QAU AI Campus Assistant PostgreSQL schema
-- Database: PostgreSQL + pgvector
-- Note: vector(1536) should be adjusted to match the embedding model dimension.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  real_name VARCHAR(64),
  password_hash VARCHAR(255) NOT NULL,
  role_code VARCHAR(32) NOT NULL DEFAULT 'student',
  department VARCHAR(128),
  email VARCHAR(128),
  phone VARCHAR(32),
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  last_login_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_user_status CHECK (status IN ('active', 'disabled', 'locked'))
);

COMMENT ON TABLE user_account IS '系统用户表：学生、教师、管理员等账号信息';
COMMENT ON COLUMN user_account.role_code IS '角色编码，例如 student、teacher、admin';

CREATE TABLE IF NOT EXISTS nav_group (
  id BIGSERIAL PRIMARY KEY,
  group_key VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(64) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE nav_group IS '侧边栏导航分组表，对应前端 NavGroup';

CREATE TABLE IF NOT EXISTS nav_item (
  id BIGSERIAL PRIMARY KEY,
  group_id BIGINT NOT NULL REFERENCES nav_group(id),
  item_key VARCHAR(64) NOT NULL UNIQUE,
  label VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  icon_name VARCHAR(64),
  route_path VARCHAR(255),
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE nav_item IS '侧边栏导航菜单项表，对应前端 NavItem';
COMMENT ON COLUMN nav_item.icon_name IS '前端图标名称，例如 MessageSquare、Database';

CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ready',
  description TEXT,
  document_count INTEGER NOT NULL DEFAULT 0,
  owner_department VARCHAR(128),
  last_synced_at TIMESTAMP,
  created_by BIGINT REFERENCES user_account(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_knowledge_status CHECK (status IN ('ready', 'syncing', 'review', 'disabled'))
);

COMMENT ON TABLE knowledge_base IS '知识库表，对应前端 KnowledgeBase';
COMMENT ON COLUMN knowledge_base.status IS '知识库状态：ready 已完成、syncing 同步中、review 待复核、disabled 停用';

CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGSERIAL PRIMARY KEY,
  knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id),
  title VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(32),
  file_size BIGINT,
  storage_url VARCHAR(500),
  checksum VARCHAR(128),
  status VARCHAR(20) NOT NULL DEFAULT 'uploaded',
  version_no INTEGER NOT NULL DEFAULT 1,
  uploaded_by BIGINT REFERENCES user_account(id),
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  parsed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_document_status CHECK (status IN ('uploaded', 'parsing', 'vectorized', 'review', 'failed', 'disabled'))
);

COMMENT ON TABLE knowledge_document IS '知识库文档表：PDF、Word、通知附件等原始文件元数据';
COMMENT ON COLUMN knowledge_document.storage_url IS '原始文件在 MinIO / OSS 中的存储地址';

CREATE TABLE IF NOT EXISTS knowledge_sync_job (
  id BIGSERIAL PRIMARY KEY,
  knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id),
  document_id BIGINT REFERENCES knowledge_document(id),
  job_type VARCHAR(32) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  progress INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  started_at TIMESTAMP,
  finished_at TIMESTAMP,
  created_by BIGINT REFERENCES user_account(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_sync_job_type CHECK (job_type IN ('parse', 'embed', 'full_sync')),
  CONSTRAINT chk_sync_job_status CHECK (status IN ('pending', 'running', 'success', 'failed')),
  CONSTRAINT chk_sync_job_progress CHECK (progress >= 0 AND progress <= 100)
);

COMMENT ON TABLE knowledge_sync_job IS '知识库同步任务表：记录文档解析、向量化、全量同步任务';

CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGSERIAL PRIMARY KEY,
  knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id),
  document_id BIGINT NOT NULL REFERENCES knowledge_document(id),
  chunk_index INTEGER NOT NULL,
  title VARCHAR(255),
  content TEXT NOT NULL,
  token_count INTEGER NOT NULL DEFAULT 0,
  page_no INTEGER,
  section_title VARCHAR(255),
  embedding vector(1536),
  metadata_json JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uq_document_chunk UNIQUE (document_id, chunk_index)
);

COMMENT ON TABLE document_chunk IS '文档切片表：用于知识库向量检索和回答来源追踪';
COMMENT ON COLUMN document_chunk.embedding IS '文本向量，维度需要与 embedding 模型保持一致';
COMMENT ON COLUMN document_chunk.metadata_json IS '额外元数据，例如章节、页码、来源链接、适用人群';

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES user_account(id),
  title VARCHAR(255) NOT NULL,
  scope VARCHAR(128),
  message_count INTEGER NOT NULL DEFAULT 0,
  pinned BOOLEAN NOT NULL DEFAULT FALSE,
  last_message_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE chat_session IS '对话会话表，对应前端 ChatSession';
COMMENT ON COLUMN chat_session.scope IS '会话主要知识库范围说明，用于前端展示';

CREATE TABLE IF NOT EXISTS chat_session_knowledge_base (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
  knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_session_knowledge_base UNIQUE (session_id, knowledge_base_id)
);

COMMENT ON TABLE chat_session_knowledge_base IS '会话与知识库关联表：保存一次对话选择的知识库范围';

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
  parent_message_id BIGINT REFERENCES chat_message(id),
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  model_name VARCHAR(128),
  token_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_chat_message_role CHECK (role IN ('user', 'assistant', 'system'))
);

COMMENT ON TABLE chat_message IS '聊天消息表，对应前端 ChatMessage';
COMMENT ON COLUMN chat_message.role IS '消息角色：user 用户、assistant AI、system 系统';

CREATE TABLE IF NOT EXISTS chat_message_source (
  id BIGSERIAL PRIMARY KEY,
  message_id BIGINT NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
  knowledge_base_id BIGINT REFERENCES knowledge_base(id),
  document_id BIGINT REFERENCES knowledge_document(id),
  chunk_id BIGINT REFERENCES document_chunk(id),
  source_title VARCHAR(255) NOT NULL,
  source_excerpt TEXT,
  similarity_score NUMERIC(8, 6),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chat_message_source IS '回答来源表：记录 AI 回复引用了哪些知识库、文档和切片';

CREATE TABLE IF NOT EXISTS quick_question (
  id BIGSERIAL PRIMARY KEY,
  label VARCHAR(64) NOT NULL,
  question VARCHAR(500) NOT NULL,
  category VARCHAR(64),
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE quick_question IS '快捷问题表，对应前端 QuickQuestion';

CREATE TABLE IF NOT EXISTS search_log (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES user_account(id),
  keyword VARCHAR(255) NOT NULL,
  knowledge_base_ids BIGINT[],
  result_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE search_log IS '全局搜索日志表：用于搜索记录和数据看板统计';

CREATE TABLE IF NOT EXISTS usage_stat_daily (
  id BIGSERIAL PRIMARY KEY,
  stat_date DATE NOT NULL UNIQUE,
  chat_count INTEGER NOT NULL DEFAULT 0,
  search_count INTEGER NOT NULL DEFAULT 0,
  active_user_count INTEGER NOT NULL DEFAULT 0,
  document_count INTEGER NOT NULL DEFAULT 0,
  knowledge_base_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE usage_stat_daily IS '每日使用统计表：用于数据看板和首页指标趋势';

CREATE TABLE IF NOT EXISTS system_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(128) NOT NULL UNIQUE,
  config_value TEXT,
  config_type VARCHAR(32) NOT NULL DEFAULT 'string',
  description VARCHAR(255),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_system_config_type CHECK (config_type IN ('string', 'number', 'boolean', 'json'))
);

COMMENT ON TABLE system_config IS '系统配置表：模型、接口、知识库参数等配置';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES user_account(id),
  module_name VARCHAR(64) NOT NULL,
  action_name VARCHAR(64) NOT NULL,
  target_type VARCHAR(64),
  target_id BIGINT,
  request_ip VARCHAR(64),
  user_agent VARCHAR(500),
  detail_json JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_log IS '权限审计日志表：记录用户关键操作';

CREATE INDEX IF NOT EXISTS idx_nav_item_group_sort ON nav_item(group_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_status ON knowledge_base(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_category ON knowledge_base(category);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_kb ON knowledge_document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_status ON knowledge_document(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_title_trgm ON knowledge_document USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sync_job_kb_status ON knowledge_sync_job(knowledge_base_id, status);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document ON document_chunk(document_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_document_chunk_kb ON document_chunk(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_content_trgm ON document_chunk USING gin (content gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding ON document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_chat_session_user_updated ON chat_session(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_session_pinned ON chat_session(user_id, pinned);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_created ON chat_message(session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_source_message ON chat_message_source(message_id);
CREATE INDEX IF NOT EXISTS idx_quick_question_enabled_sort ON quick_question(enabled, sort_order);
CREATE INDEX IF NOT EXISTS idx_search_log_user_created ON search_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_created ON audit_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_module_action ON audit_log(module_name, action_name);

CREATE TRIGGER trg_user_account_updated_at
BEFORE UPDATE ON user_account
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_nav_group_updated_at
BEFORE UPDATE ON nav_group
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_nav_item_updated_at
BEFORE UPDATE ON nav_item
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_knowledge_base_updated_at
BEFORE UPDATE ON knowledge_base
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_knowledge_document_updated_at
BEFORE UPDATE ON knowledge_document
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_knowledge_sync_job_updated_at
BEFORE UPDATE ON knowledge_sync_job
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_document_chunk_updated_at
BEFORE UPDATE ON document_chunk
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_chat_session_updated_at
BEFORE UPDATE ON chat_session
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_quick_question_updated_at
BEFORE UPDATE ON quick_question
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_usage_stat_daily_updated_at
BEFORE UPDATE ON usage_stat_daily
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_system_config_updated_at
BEFORE UPDATE ON system_config
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
