ALTER TABLE chat_message
  ADD COLUMN IF NOT EXISTS token_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN chat_message.token_count IS '消息消耗的 token 数量';
