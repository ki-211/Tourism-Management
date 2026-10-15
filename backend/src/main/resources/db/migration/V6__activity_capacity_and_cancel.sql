-- 人数上限（含负责人，NULL 为不限）与活动取消
ALTER TABLE activities ADD COLUMN capacity INT NULL;
ALTER TABLE activities ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE activities ADD COLUMN cancel_reason VARCHAR(200) NULL;
ALTER TABLE activities ADD COLUMN cancelled_at TIMESTAMP NULL;
-- 发现页: visibility = ? AND status = 'ACTIVE' ORDER BY created_at DESC
CREATE INDEX idx_activity_visibility_status_created ON activities(visibility, status, created_at);
