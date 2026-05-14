CREATE TABLE album_photo (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             activity_id BIGINT NOT NULL,
                             user_id BIGINT NOT NULL,
                             url VARCHAR(512) NOT NULL,
                             create_time DATETIME NOT NULL DEFAULT NOW()
);
