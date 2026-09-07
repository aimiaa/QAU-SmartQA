ALTER TABLE chat_session
  ADD COLUMN IF NOT EXISTS session_id VARCHAR(64);

UPDATE chat_session
SET session_id = CONCAT('legacy-', id)
WHERE session_id IS NULL;

ALTER TABLE chat_session
  ALTER COLUMN session_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_session_session_id
  ON chat_session(session_id);

COMMENT ON COLUMN chat_session.session_id IS '前端 localStorage 生成并通过 X-Session-Id 传入的业务会话标识';
