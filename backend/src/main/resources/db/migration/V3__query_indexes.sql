-- Composite indexes matching the WHERE + ORDER BY of list queries, so they avoid filesorts and full scans.

-- "我创建的活动": creator_id = ? ORDER BY created_at DESC
CREATE INDEX idx_activity_creator_created ON activities(creator_id, created_at);
-- "报名中": visibility = ? AND signup_end >= ? (signup_start filtered) ORDER BY signup_end
CREATE INDEX idx_activity_visibility_signup_end ON activities(visibility, signup_end);
-- 活动成员与签到汇总: activity_id = ? ORDER BY joined_at
CREATE INDEX idx_signup_activity_joined ON signups(activity_id, joined_at);
-- 相册: activity_id = ? ORDER BY created_at DESC
CREATE INDEX idx_photo_activity_created ON activity_photos(activity_id, created_at);
-- 车辆: activity_id = ? ORDER BY pickup_time
CREATE INDEX idx_vehicle_activity_pickup ON vehicles(activity_id, pickup_time);
-- 签到历史与待签到: user_id = ? ORDER BY signed_at DESC / NOT EXISTS (task_id, user_id)
CREATE INDEX idx_record_user_signed ON sign_records(user_id, signed_at);
-- 签到汇总: task_id = ? ORDER BY signed_at
CREATE INDEX idx_record_task_signed ON sign_records(task_id, signed_at);
-- 每小时清理过期刷新令牌: expires_at < ?
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
