-- 签到规则：截止时间、地理围栏（GCJ-02 坐标 + 半径）与必须拍照；记录签到时距签到点的距离
ALTER TABLE sign_tasks ADD COLUMN deadline TIMESTAMP NULL;
ALTER TABLE sign_tasks ADD COLUMN latitude DECIMAL(10,7) NULL;
ALTER TABLE sign_tasks ADD COLUMN longitude DECIMAL(10,7) NULL;
ALTER TABLE sign_tasks ADD COLUMN radius_meters INT NULL;
ALTER TABLE sign_tasks ADD COLUMN location_name VARCHAR(200) NULL;
ALTER TABLE sign_tasks ADD COLUMN photo_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sign_records ADD COLUMN distance_meters INT NULL;
