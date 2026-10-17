ALTER TABLE users ADD COLUMN wechat_openid VARCHAR(64) NULL;
ALTER TABLE users ADD COLUMN password_set BOOLEAN NOT NULL DEFAULT TRUE;
CREATE UNIQUE INDEX uk_users_wechat_openid ON users (wechat_openid);
