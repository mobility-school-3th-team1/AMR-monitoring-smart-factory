-- [1] 기존 테이블 삭제 (자식 테이블부터 역순 삭제)
DROP TABLE IF EXISTS AMR_STATUS_LOG;
DROP TABLE IF EXISTS AMR_COMMAND;
DROP TABLE IF EXISTS ALARM_LOG;
DROP TABLE IF EXISTS REFRESH_TOKEN;
DROP TABLE IF EXISTS USER_ACCOUNT;
DROP TABLE IF EXISTS AMR_TASK;
DROP TABLE IF EXISTS AMR_CHARGING_LOG;
DROP TABLE IF EXISTS AMR_MASTER;
DROP TABLE IF EXISTS AMR_CHARGE_STATION;
DROP TABLE IF EXISTS WIP_LOT;
DROP TABLE IF EXISTS WORK_ORDER;
DROP TABLE IF EXISTS PR_ROUTING;
DROP TABLE IF EXISTS PR_PROCESS;
DROP TABLE IF EXISTS PRODUCT;
DROP TABLE IF EXISTS ENV_SENSOR_LOG;
DROP TABLE IF EXISTS ENV_SENSOR;
DROP TABLE IF EXISTS AREA;
DROP TABLE IF EXISTS SITE;


-- [2] 테이블 생성 (부모 테이블부터 생성)

-- 1. SITE
CREATE TABLE SITE (
    site_id VARCHAR(50) PRIMARY KEY,
    site_name VARCHAR(100) NOT NULL
);

-- 2. AREA
CREATE TABLE AREA (
    area_id VARCHAR(50) PRIMARY KEY,
    site_id VARCHAR(50),
    area_name VARCHAR(100),
    area_type VARCHAR(50),
    temp_min DECIMAL(10, 2),
    temp_max DECIMAL(10, 2),
    humidity_min DECIMAL(10, 2),
    humidity_max DECIMAL(10, 2),
    CONSTRAINT FK_AREA_SITE FOREIGN KEY (site_id) REFERENCES SITE(site_id)
);

-- 3. ENV_SENSOR
CREATE TABLE ENV_SENSOR (
    env_sensor_id VARCHAR(50) PRIMARY KEY,
    area_id VARCHAR(50),
    sensor_name VARCHAR(100),
    sensor_type VARCHAR(50),
    unit VARCHAR(20),
    min_val DECIMAL(10, 2),
    max_val DECIMAL(10, 2),
    install_at DATETIME,
    CONSTRAINT FK_SENSOR_AREA FOREIGN KEY (area_id) REFERENCES AREA(area_id)
);

-- 4. ENV_SENSOR_LOG
CREATE TABLE ENV_SENSOR_LOG (
    sensor_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    env_sensor_id VARCHAR(50),
    sensor_value DECIMAL(10, 2),
    sensor_status VARCHAR(50),
    measured_at DATETIME,
    CONSTRAINT FK_LOG_SENSOR FOREIGN KEY (env_sensor_id) REFERENCES ENV_SENSOR(env_sensor_id)
);

-- 5. PRODUCT
CREATE TABLE PRODUCT (
    product_id VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(100)
);

-- 6. PR_PROCESS
CREATE TABLE PR_PROCESS (
    pr_process_id VARCHAR(50) PRIMARY KEY,
    process_name VARCHAR(100),
    area_id VARCHAR(50),
    CONSTRAINT FK_PROCESS_AREA FOREIGN KEY (area_id) REFERENCES AREA(area_id)
);

-- 7. PR_ROUTING
CREATE TABLE PR_ROUTING (
    pr_routing_id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50),
    pr_process_id VARCHAR(50),
    seq_no INT,
    standard_lead_time INT,
    CONSTRAINT FK_ROUTING_PRODUCT FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id),
    CONSTRAINT FK_ROUTING_PROCESS FOREIGN KEY (pr_process_id) REFERENCES PR_PROCESS(pr_process_id)
);

-- 8. WORK_ORDER
CREATE TABLE WORK_ORDER (
    work_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50),
    planned_qty INT,
    planned_start_date DATETIME,
    planned_end_date DATETIME,
    status VARCHAR(50),
    CONSTRAINT FK_WO_PRODUCT FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id)
);

-- 9. WIP_LOT
CREATE TABLE WIP_LOT (
    lot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_id BIGINT,
    current_qty INT,
    current_routing_id VARCHAR(50),
    status VARCHAR(50),
    CONSTRAINT FK_LOT_WO FOREIGN KEY (work_id) REFERENCES WORK_ORDER(work_id)
);

-- 10. AMR_MASTER
CREATE TABLE AMR_MASTER (
    amr_id INT PRIMARY KEY,
    amr_name VARCHAR(100),
    total_mileage DOUBLE PRECISION,
    load_max INT,
    battery_capacity INT,
    inspection_dt DATETIME
);

-- 11. AMR_CHARGE_STATION
CREATE TABLE AMR_CHARGE_STATION (
    station_id INT PRIMARY KEY,
    area_id VARCHAR(50),
    station_name VARCHAR(100),
    station_status VARCHAR(50),
    CONSTRAINT FK_STATION_AREA FOREIGN KEY (area_id) REFERENCES AREA(area_id)
);

-- 12. AMR_CHARGING_LOG
CREATE TABLE AMR_CHARGING_LOG (
    charging_session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amr_id INT,
    station_id INT,
    session_status VARCHAR(50),
    start_time DATETIME,
    end_time DATETIME,
    CONSTRAINT FK_CHG_LOG_AMR FOREIGN KEY (amr_id) REFERENCES AMR_MASTER(amr_id),
    CONSTRAINT FK_CHG_LOG_STATION FOREIGN KEY (station_id) REFERENCES AMR_CHARGE_STATION(station_id)
);

-- 13. AMR_TASK
CREATE TABLE AMR_TASK (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amr_id INT,
    task_type VARCHAR(50),
    lot_id BIGINT,
    from_area_id VARCHAR(50),
    to_area_id VARCHAR(50),
    status VARCHAR(50),
    pick_time DATETIME,
    drop_time DATETIME,
    CONSTRAINT FK_TASK_AMR FOREIGN KEY (amr_id) REFERENCES AMR_MASTER(amr_id),
    CONSTRAINT FK_TASK_LOT FOREIGN KEY (lot_id) REFERENCES WIP_LOT(lot_id),
    CONSTRAINT FK_TASK_FROM FOREIGN KEY (from_area_id) REFERENCES AREA(area_id),
    CONSTRAINT FK_TASK_TO FOREIGN KEY (to_area_id) REFERENCES AREA(area_id)
);

-- 14. AMR_STATUS_LOG
CREATE TABLE AMR_STATUS_LOG (
    amr_statlog_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amr_id INT,
    area_id VARCHAR(50),
    status VARCHAR(50),
    pos_x INT,
    pos_y INT,
    yaw INT,
    load_weight INT,
    battery_pct INT,
    SOH_pct INT,
    battery_temp FLOAT,
    updated_at DATETIME,
    CONSTRAINT FK_STAT_LOG_AMR FOREIGN KEY (amr_id) REFERENCES AMR_MASTER(amr_id),
    CONSTRAINT FK_STAT_LOG_AREA FOREIGN KEY (area_id) REFERENCES AREA(area_id)
);

-- 15. USER_ACCOUNT
CREATE TABLE USER_ACCOUNT (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(100),
    role VARCHAR(50),
    created_at DATETIME
);

-- 16. REFRESH_TOKEN
CREATE TABLE REFRESH_TOKEN (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(50),
    expires_at DATETIME,
    revoked BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT FK_REFRESH_USER FOREIGN KEY (user_id) REFERENCES USER_ACCOUNT(user_id)
);

-- 17. ALARM_LOG
CREATE TABLE ALARM_LOG (
    alarm_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(50),
    source_id VARCHAR(50),
    level VARCHAR(50),
    message VARCHAR(255),
    occurred_at DATETIME,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_at DATETIME
);

-- 18. AMR_COMMAND
CREATE TABLE AMR_COMMAND (
    command_id VARCHAR(50) PRIMARY KEY,
    amr_id INT,
    command_type VARCHAR(50),
    params TEXT,
    accepted BOOLEAN DEFAULT FALSE,
    status VARCHAR(50),
    requested_at DATETIME,
    executed_at DATETIME,
    CONSTRAINT FK_COMMAND_AMR FOREIGN KEY (amr_id) REFERENCES AMR_MASTER(amr_id)
);


-- [3] 임시 데이터 삽입

-- 기초 정보
INSERT INTO SITE VALUES ('SITE_BSA_01', 'BSA 제조 공장');
INSERT INTO AREA VALUES ('AREA_INBOUND', 'SITE_BSA_01', '입고 구역', 'STORAGE', 15.00, 25.00, 30.00, 60.00);
INSERT INTO AREA VALUES ('AREA_PRODUCTION', 'SITE_BSA_01', '생산 라인', 'PRODUCTION', 20.00, 24.00, 40.00, 50.00);
INSERT INTO AREA VALUES ('AREA_OUTBOUND', 'SITE_BSA_01', '출고 구역', 'STORAGE', 15.00, 25.00, 30.00, 60.00);
INSERT INTO AREA VALUES ('AREA_QC', 'SITE_BSA_01', '품질 검사 구역', 'QUALITY', 18.00, 22.00, 35.00, 45.00);

-- 센서 정보
INSERT INTO ENV_SENSOR VALUES ('SNSR_TEMP_001', 'AREA_PRODUCTION', '온도 센서 1호', 'TEMP', '°C', 10.00, 40.00, '2026-01-01 09:00:00');
INSERT INTO ENV_SENSOR VALUES ('SNSR_HUMID_001', 'AREA_PRODUCTION', '습도 센서 1호', 'HUMIDITY', '%', 20.00, 80.00, '2026-01-01 09:00:00');
INSERT INTO ENV_SENSOR_LOG (env_sensor_id, sensor_value, sensor_status, measured_at) 
VALUES ('SNSR_TEMP_001', 22.5, 'NORMAL', '2026-05-15 11:00:00');
INSERT INTO ENV_SENSOR_LOG (env_sensor_id, sensor_value, sensor_status, measured_at) 
VALUES ('SNSR_HUMID_001', 45.0, 'NORMAL', '2026-05-15 11:00:00');

-- 공정 정보
INSERT INTO PRODUCT VALUES ('PROD_BSA_001', 'BSA 스마트 모듈');
INSERT INTO PRODUCT VALUES ('PROD_BSA_002', 'BSA 센서 유닛');
INSERT INTO PR_PROCESS VALUES ('PROC_ASSEMBLY', '조립 공정', 'AREA_PRODUCTION');
INSERT INTO PR_PROCESS VALUES ('PROC_QC', '품질 검사 공정', 'AREA_QC');
INSERT INTO PR_ROUTING VALUES ('RT_BSA_001_01', 'PROD_BSA_001', 'PROC_ASSEMBLY', 1, 20);
INSERT INTO PR_ROUTING VALUES ('RT_BSA_001_02', 'PROD_BSA_001', 'PROC_QC', 2, 10);
INSERT INTO PR_ROUTING VALUES ('RT_BSA_002_01', 'PROD_BSA_002', 'PROC_ASSEMBLY', 1, 15);

-- 작업 지시 및 재공
INSERT INTO WORK_ORDER (product_id, planned_qty, planned_start_date, planned_end_date, status)
VALUES ('PROD_BSA_001', 200, '2026-05-15 08:00:00', '2026-05-16 18:00:00', 'RUNNING');
INSERT INTO WORK_ORDER (product_id, planned_qty, planned_start_date, planned_end_date, status)
VALUES ('PROD_BSA_002', 150, '2026-05-15 09:00:00', '2026-05-16 17:00:00', 'PLANNED');
INSERT INTO WIP_LOT (work_id, current_qty, current_routing_id, status)
VALUES (1, 100, 'RT_BSA_001_01', 'IN_PROGRESS');
INSERT INTO WIP_LOT (work_id, current_qty, current_routing_id, status)
VALUES (2, 75, 'RT_BSA_002_01', 'WAITING');

-- AMR 정보
INSERT INTO AMR_MASTER VALUES (1, 'AMR-BSA-01', 250.0, 600, 120, '2026-05-01 10:00:00');
INSERT INTO AMR_MASTER VALUES (2, 'AMR-BSA-02', 180.5, 500, 100, '2026-05-02 10:00:00');
INSERT INTO AMR_CHARGE_STATION VALUES (1, 'AREA_INBOUND', '충전소-입고', 'AVAILABLE');
INSERT INTO AMR_CHARGE_STATION VALUES (2, 'AREA_PRODUCTION', '충전소-생산', 'OCCUPIED');

-- AMR 로그 및 작업
INSERT INTO AMR_TASK (amr_id, task_type, lot_id, from_area_id, to_area_id, status, pick_time, drop_time)
VALUES (1, 'TRANSPORT', 1, 'AREA_INBOUND', 'AREA_PRODUCTION', 'COMPLETED', '2026-05-15 10:00:00', '2026-05-15 10:30:00');
INSERT INTO AMR_TASK (amr_id, task_type, lot_id, from_area_id, to_area_id, status, pick_time)
VALUES (2, 'TRANSPORT', 2, 'AREA_PRODUCTION', 'AREA_QC', 'IN_TRANSIT', '2026-05-15 11:00:00');

INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (1, 'AREA_PRODUCTION', 'OPERATING', 120, 450, 90, 50, 85, 98, 35.5, '2026-05-15 11:25:00');
INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (1, 'AREA_INBOUND', 'IDLE', 50, 200, 0, 0, 95, 99, 30.0, '2026-05-15 09:00:00');
INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (2, 'AREA_QC', 'CHARGING', 300, 150, 180, 0, 20, 95, 28.5, '2026-05-15 10:45:00');

-- 인증 및 알람 샘플 데이터
INSERT INTO USER_ACCOUNT (user_id, username, password_hash, display_name, role, created_at)
VALUES ('user-001', 'admin', '$2a$10$examplehashforadminpassword', '관리자', 'admin', '2026-05-01 08:00:00');
INSERT INTO REFRESH_TOKEN (token, user_id, expires_at, revoked, created_at)
VALUES ('refresh-token-example', 'user-001', '2026-05-16 08:00:00', FALSE, '2026-05-15 08:00:00');
INSERT INTO ALARM_LOG (source_type, source_id, level, message, occurred_at, acknowledged, acknowledged_at)
VALUES ('CHARGE_STATION', '1', 'warning', '충전 스테이션 1 혼잡 상태', '2026-05-13 14:29:00', FALSE, NULL);
INSERT INTO AMR_COMMAND (command_id, amr_id, command_type, params, accepted, status, requested_at)
VALUES ('cmd-001', 1, 'emergencyStop', '{}', TRUE, 'PENDING', '2026-05-15 11:00:00');