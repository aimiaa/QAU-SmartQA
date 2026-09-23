-- Allow empty knowledge bases to be represented as "building" / waiting for documents.
ALTER TABLE knowledge_base
  DROP CONSTRAINT IF EXISTS chk_knowledge_status;

ALTER TABLE knowledge_base
  ADD CONSTRAINT chk_knowledge_status
    CHECK (status IN ('building', 'ready', 'syncing', 'review', 'disabled'));

COMMENT ON COLUMN knowledge_base.status IS '知识库状态：building 待上传、ready 已完成、syncing 同步中、review 待复核、disabled 停用';
