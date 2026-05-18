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
    temp_min DECIMAL(10, 1),
    temp_max DECIMAL(10, 1),
    humidity_min INT,
    humidity_max INT,
    particle_min INT,
    particle_max INT,
    co_gas_min INT,
    co_gas_max INT,
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
    install_at DATE,
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
    planned_start_date DATE,
    planned_end_date DATE,
    status VARCHAR(50),
    CONSTRAINT FK_WO_PRODUCT FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id)
);

-- 9. WIP_LOT
CREATE TABLE WIP_LOT (
    lot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_id BIGINT,
    current_qty INT,
    pr_routing_id VARCHAR(50),
    status VARCHAR(50),
    CONSTRAINT FK_LOT_WO FOREIGN KEY (work_id) REFERENCES WORK_ORDER(work_id),
    CONSTRAINT FK_LOT_ROUT FOREIGN KEY (pr_routing_id) REFERENCES PR_ROUTING(pr_routing_id)
);

-- 10. AMR_MASTER
CREATE TABLE AMR_MASTER (
    amr_id INT PRIMARY KEY,
    amr_name VARCHAR(100),
    total_mileage DOUBLE PRECISION,
    load_max INT,
    battery_capacity INT,
    inspection_dt DATE
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
    created_at DATE
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
INSERT INTO SITE (site_id, site_name) VALUES ('SITE_BSA_01', 'BSA 제조 공장');

-- AREA
INSERT INTO AREA (area_id, site_id, area_name, area_type, temp_min, temp_max, humidity_min, humidity_max, particle_min, particle_max, co_gas_min, co_gas_max)
VALUES ('AREA_LOAD_LC', 'SITE_BSA_01', 'Lower Case 로딩 구역', 'STORAGE', 18.00, 25.00, 30.00, 60.00, 50, 150, 0, 20);
INSERT INTO AREA (area_id, site_id, area_name, area_type, temp_min, temp_max, humidity_min, humidity_max, particle_min, particle_max, co_gas_min, co_gas_max)
VALUES ('AREA_ASSEMBLE_01', 'SITE_BSA_01', '조립 구역 1', 'PRODUCTION', 18.00, 25.00, 30.00, 40.00, 10, 30, 0, 10);
INSERT INTO AREA (area_id, site_id, area_name, area_type, temp_min, temp_max, humidity_min, humidity_max, particle_min, particle_max, co_gas_min, co_gas_max)
VALUES ('AREA_ASSEMBLE_02', 'SITE_BSA_01', '조립 구역 2', 'PRODUCTION', 18.00, 22.00, 30.00, 50.00, 10, 30, 0, 10);
INSERT INTO AREA (area_id, site_id, area_name, area_type, temp_min, temp_max, humidity_min, humidity_max, particle_min, particle_max, co_gas_min, co_gas_max)
VALUES ('AREA_OUT_BSA', 'SITE_BSA_01', 'BSA 출고 구역', 'STORAGE', 18.00, 22.00, 30.00, 50.00, 50, 150, 0, 10);

-- ENV_SENSOR
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_TEMP_001', 'AREA_LOAD_LC', '온도 센서 1호', 'TEMP', '°C', -20.0, 120.0, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_HUMID_001', 'AREA_LOAD_LC', '습도 센서 1호', 'HUMIDITY', '%', 0, 100, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_PC_001', 'AREA_LOAD_LC', '파티클 센서 1호', 'PARTICLE', 'ug/m3', 0, 500, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_GAS_CO_001', 'AREA_LOAD_LC', 'CO 가스 센서 1호', 'GAS', 'ppm', 0, 1000, '2026-01-05');

INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_TEMP_002', 'AREA_ASSEMBLE_01', '온도 센서 2호', 'TEMP', '°C', -20.0, 120.0, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_HUMID_002', 'AREA_ASSEMBLE_01', '습도 센서 2호', 'HUMIDITY', '%', 0, 100, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_PC_002', 'AREA_ASSEMBLE_01', '파티클 센서 2호', 'PARTICLE', 'ug/m3', 0, 500, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_GAS_CO_002', 'AREA_ASSEMBLE_01', 'CO 가스 센서 2호', 'GAS', 'ppm', 0, 1000, '2026-01-05');

INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_TEMP_003', 'AREA_ASSEMBLE_02', '온도 센서 3호', 'TEMP', '°C', -20.0, 120.0, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_HUMID_003', 'AREA_ASSEMBLE_02', '습도 센서 3호', 'HUMIDITY', '%', 0, 100, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_PC_003', 'AREA_ASSEMBLE_02', '파티클 센서 3호', 'PARTICLE', 'ug/m3', 0, 500, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_GAS_CO_003', 'AREA_ASSEMBLE_02', 'CO 가스 센서 3호', 'GAS', 'ppm', 0, 1000, '2026-05-05');

INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_TEMP_004', 'AREA_OUT_BSA', '온도 센서 4호', 'TEMP', '°C', -20.0, 120.0, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_HUMID_004', 'AREA_OUT_BSA', '습도 센서 4호', 'HUMIDITY', '%', 0, 100, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_PC_004', 'AREA_OUT_BSA', '파티클 센서 4호', 'PARTICLE', 'ug/m3', 0, 500, '2026-01-01');
INSERT INTO ENV_SENSOR (env_sensor_id, area_id, sensor_name, sensor_type, unit, min_val, max_val, install_at)
VALUES ('SNSR_GAS_CO_004', 'AREA_OUT_BSA', 'CO 가스 센서 4호', 'GAS', 'ppm', 0, 1000, '2026-01-05');

-- PRODUCT
INSERT INTO PRODUCT (product_id, product_name) VALUES ('BSA_HEV_01', 'BSA for HEV 1');
INSERT INTO PRODUCT (product_id, product_name) VALUES ('BSA_BEV_01', 'BSA for BEV 1');
INSERT INTO PRODUCT (product_id, product_name) VALUES ('BMA_001', 'BMA 1');
INSERT INTO PRODUCT (product_id, product_name) VALUES ('BMS_001', 'BMS 1');
INSERT INTO PRODUCT (product_id, product_name) VALUES ('LC_001', 'Lower Case 1');
INSERT INTO PRODUCT (product_id, product_name) VALUES ('UC_001', 'Upper Case 1');

-- PR_PROCESS
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_LOAD_LC', 'Lower Case 로딩', 'AREA_LOAD_LC');
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_AS_BMA', 'BMA 조립', 'AREA_ASSEMBLE_01');
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_AS_BMS', 'BMS 조립', 'AREA_ASSEMBLE_02');
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_AS_UC', 'Upper Case 조립', 'AREA_ASSEMBLE_02');
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_QC', '품질 검사', 'AREA_ASSEMBLE_02');
INSERT INTO PR_PROCESS (pr_process_id, process_name, area_id) VALUES ('PROC_OUT_BSA', 'BSA 출고', 'AREA_OUT_BSA');

-- PR_ROUTING
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_01', 'BSA_HEV_01', 'PROC_LOAD_LC', 1, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_02', 'BSA_HEV_01', 'PROC_AS_BMA', 2, 30);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_03', 'BSA_HEV_01', 'PROC_AS_BMS', 3, 20);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_04', 'BSA_HEV_01', 'PROC_AS_UC', 4, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_05', 'BSA_HEV_01', 'PROC_QC', 5, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_HEV_01_06', 'BSA_HEV_01', 'PROC_OUT_BSA', 6, 10);

INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_01', 'BSA_BEV_01', 'PROC_LOAD_LC', 1, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_02', 'BSA_BEV_01', 'PROC_AS_BMA', 2, 50);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_03', 'BSA_BEV_01', 'PROC_AS_BMS', 3, 30);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_04', 'BSA_BEV_01', 'PROC_AS_UC', 4, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_05', 'BSA_BEV_01', 'PROC_QC', 5, 10);
INSERT INTO PR_ROUTING (pr_routing_id, product_id, pr_process_id, seq_no, standard_lead_time) VALUES ('RT_BSA_BEV_01_06', 'BSA_BEV_01', 'PROC_OUT_BSA', 6, 10);

-- WORK_ORDER
INSERT INTO WORK_ORDER (product_id, planned_qty, planned_start_date, planned_end_date, status)
VALUES ('BSA_HEV_01', 200, '2026-05-15', '2026-05-16', 'RUNNING');
INSERT INTO WORK_ORDER (product_id, planned_qty, planned_start_date, planned_end_date, status)
VALUES ('BSA_BEV_01', 150, '2026-05-15', '2026-05-16', 'PLANNED');

-- WIP_LOT
INSERT INTO WIP_LOT (work_id, current_qty, pr_routing_id, status) VALUES (1, 100, 'RT_BSA_HEV_01_02', 'IN_PROGRESS');
INSERT INTO WIP_LOT (work_id, current_qty, pr_routing_id, status) VALUES (1, 100, 'RT_BSA_HEV_01_01', 'WAITING');
INSERT INTO WIP_LOT (work_id, current_qty, pr_routing_id, status) VALUES (2, 75, 'RT_BSA_BEV_01_04', 'IN_PROGRESS');
INSERT INTO WIP_LOT (work_id, current_qty, pr_routing_id, status) VALUES (2, 75, 'RT_BSA_BEV_01_03', 'IN_PROGRESS');

-- AMR_MASTER
INSERT INTO AMR_MASTER (amr_id, amr_name, total_mileage, load_max, battery_capacity, inspection_dt) VALUES (1, 'AMR_BSA_01', 1736.2, 600, 100, '2026-05-01');
INSERT INTO AMR_MASTER (amr_id, amr_name, total_mileage, load_max, battery_capacity, inspection_dt) VALUES (2, 'AMR_BSA_02', 1538.1, 600, 100, '2026-05-01');
INSERT INTO AMR_MASTER (amr_id, amr_name, total_mileage, load_max, battery_capacity, inspection_dt) VALUES (3, 'AMR_BSA_03', 1645.8, 600, 100, '2026-05-01');
INSERT INTO AMR_MASTER (amr_id, amr_name, total_mileage, load_max, battery_capacity, inspection_dt) VALUES (4, 'AMR_BSA_04', 1592.4, 600, 100, '2026-05-01');
INSERT INTO AMR_MASTER (amr_id, amr_name, total_mileage, load_max, battery_capacity, inspection_dt) VALUES (5, 'AMR_BSA_05', 1721.7, 600, 100, '2026-05-01');

-- AMR_CHARGE_STATION
INSERT INTO AMR_CHARGE_STATION (station_id, area_id, station_name, station_status) VALUES (1, 'AREA_LOAD_LC', '충전소_입고', 'AVAILABLE');
INSERT INTO AMR_CHARGE_STATION (station_id, area_id, station_name, station_status) VALUES (2, 'AREA_LOAD_LC', '충전소_입고', 'OCCUPIED');
INSERT INTO AMR_CHARGE_STATION (station_id, area_id, station_name, station_status) VALUES (3, 'AREA_LOAD_LC', '충전소_입고', 'OCCUPIED');

-- AMR 작업
INSERT INTO AMR_TASK (amr_id, task_type, lot_id, from_area_id, to_area_id, status, pick_time, drop_time)
VALUES (1, 'TRANSPORT', 1, 'AREA_LOAD_LC', 'AREA_ASSEMBLE_01', 'COMPLETED', '2026-05-15 10:00:00', '2026-05-15 10:30:00');
INSERT INTO AMR_TASK (amr_id, task_type, lot_id, from_area_id, to_area_id, status, pick_time, drop_time)
VALUES (2, 'TRANSPORT', 3, 'AREA_ASSEMBLE_01', 'AREA_ASSEMBLE_02', 'COMPLETED', '2026-05-15 11:00:00', '2026-05-15 11:30:00');

-- AMR 상태 로그
INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (1, 'AREA_ASSEMBLE_01', 'OPERATING', 120, 450, 90, 50, 85, 98, 35.5, '2026-05-15 11:25:00');
INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (2, 'AREA_ASSEMBLE_02', 'IDLE', 50, 200, 0, 0, 95, 99, 30.0, '2026-05-15 09:00:00');
INSERT INTO AMR_STATUS_LOG (amr_id, area_id, status, pos_x, pos_y, yaw, load_weight, battery_pct, SOH_pct, battery_temp, updated_at)
VALUES (3, 'AREA_LOAD_LC', 'CHARGING', 300, 150, 180, 0, 20, 95, 28.5, '2026-05-15 10:45:00');

-- 사용자 인증
INSERT INTO USER_ACCOUNT (user_id, username, password_hash, display_name, role, created_at)
VALUES ('user-001', 'admin', '$2a$10$examplehashforadminpassword', '관리자', 'admin', '2026-05-01');
INSERT INTO REFRESH_TOKEN (token, user_id, expires_at, revoked, created_at)
VALUES ('refresh-token-example', 'user-001', '2026-05-16 08:00:00', FALSE, '2026-05-15 08:00:00');

-- 알람 및 AMR 명령
INSERT INTO ALARM_LOG (source_type, source_id, level, message, occurred_at, acknowledged, acknowledged_at)
VALUES ('CHARGE_STATION', '1', 'warning', '충전 스테이션 1 혼잡 상태', '2026-05-13 14:29:00', FALSE, NULL);
INSERT INTO AMR_COMMAND (command_id, amr_id, command_type, params, accepted, status, requested_at)
VALUES ('cmd-001', 1, 'emergencyStop', '{}', TRUE, 'PENDING', '2026-05-15 11:00:00');