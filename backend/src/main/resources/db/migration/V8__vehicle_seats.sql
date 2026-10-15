-- 车辆座位（乘客座，不含司机）与乘车分配；每位成员在一个活动内只能乘坐一辆车
ALTER TABLE vehicles ADD COLUMN seats INT NOT NULL DEFAULT 4;
CREATE TABLE vehicle_passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_passenger_activity_user UNIQUE (activity_id, user_id),
    CONSTRAINT fk_passenger_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_passenger_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_passenger_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_passenger_vehicle ON vehicle_passengers(vehicle_id, created_at);
