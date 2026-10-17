ALTER TABLE knowledge_sync_job
  DROP CONSTRAINT IF EXISTS chk_sync_job_status;

ALTER TABLE knowledge_sync_job
  ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 5,
  ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS lease_until TIMESTAMP,
  ADD COLUMN IF NOT EXISTS worker_id VARCHAR(128),
  ADD COLUMN IF NOT EXISTS last_error_type VARCHAR(128),
  ADD COLUMN IF NOT EXISTS last_error_message TEXT;

ALTER TABLE knowledge_sync_job
  ADD CONSTRAINT chk_sync_job_status
    CHECK (status IN ('pending', 'running', 'success', 'failed', 'dead', 'cancelled'));

ALTER TABLE knowledge_sync_job
  ADD CONSTRAINT chk_sync_job_attempts CHECK (attempts >= 0 AND max_attempts > 0);

CREATE INDEX IF NOT EXISTS idx_sync_job_claim
  ON knowledge_sync_job(status, next_run_at, created_at, id);

CREATE INDEX IF NOT EXISTS idx_sync_job_lease
  ON knowledge_sync_job(status, lease_until);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sync_job_active_document_type
  ON knowledge_sync_job(document_id, job_type)
  WHERE document_id IS NOT NULL
    AND status IN ('pending', 'running', 'failed');

COMMENT ON COLUMN knowledge_sync_job.status IS
  '任务状态：pending 待处理、running 执行中、failed 等待重试、success 成功、dead 死信、cancelled 已取消';
