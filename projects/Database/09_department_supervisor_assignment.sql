-- Migration 09. Run AFTER 01-08, with the application stopped and a backup available.
-- Preserve existing topics, assignments, reviews and results. Old confirmation fields stay NULL.
SET NAMES utf8mb4;
USE quanlydetai;
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_upgrade_department_supervisors$$
CREATE PROCEDURE sp_upgrade_department_supervisors()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
            AND TABLE_NAME='DE_TAI' AND COLUMN_NAME='Ma_GV_Xac_Nhan') THEN
        ALTER TABLE DE_TAI ADD COLUMN Ma_GV_Xac_Nhan VARCHAR(20) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
            AND TABLE_NAME='DE_TAI' AND COLUMN_NAME='Ngay_Xac_Nhan') THEN
        ALTER TABLE DE_TAI ADD COLUMN Ngay_Xac_Nhan DATETIME NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='DE_TAI'
            AND CONSTRAINT_NAME='FK_DT_XAC_NHAN_GV') THEN
        ALTER TABLE DE_TAI ADD CONSTRAINT FK_DT_XAC_NHAN_GV
            FOREIGN KEY (Ma_GV_Xac_Nhan) REFERENCES GV(Ma_GV);
    END IF;
END$$
CALL sp_upgrade_department_supervisors()$$
DROP PROCEDURE sp_upgrade_department_supervisors$$

-- An approved department decision always includes a valid, complete supervisor list.
DROP TRIGGER IF EXISTS trg_DUYET_BO_MON_bi$$
CREATE TRIGGER trg_DUYET_BO_MON_bi BEFORE INSERT ON DUYET_BO_MON FOR EACH ROW
BEGIN
    DECLARE v_BeginSV DATETIME;
    DECLARE v_Department VARCHAR(20);
    DECLARE v_State VARCHAR(30);
    DECLARE v_Count INT DEFAULT 0;
    SELECT t.Ma_Bo_Mon,t.Trang_Thai,d.Begin_SV
        INTO v_Department,v_State,v_BeginSV
        FROM DE_TAI t JOIN DOT_DANG_KY d ON d.Ma_Dot=t.Ma_Dot
        WHERE t.Ma_De_Tai=NEW.Ma_De_Tai FOR UPDATE;
    IF v_State <> 'Nhap' OR CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+07:00') >= v_BeginSV THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Chi duyet de tai nhap truoc giai doan SV.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM TRUONG_BO_MON
            WHERE Ma_Bo_Mon=v_Department AND Ma_GV=NEW.Ma_GV AND Dang_Giu_Chuc=TRUE) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Nguoi duyet phai la Truong bo mon phu trach.';
    END IF;
    SELECT COUNT(*) INTO v_Count FROM GVHD_DETAI WHERE Ma_De_Tai=NEW.Ma_De_Tai;
    IF v_Count < 1 OR v_Count > 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Bo mon phai chon 1-2 GVHD khi duyet.';
    END IF;
END$$

-- Prevent an approved supervisor list from changing without invalidating its approval first.
-- Existing triggers still enforce draft-only editing, capacity and reviewer conflicts.
DROP TRIGGER IF EXISTS trg_GVHD_APPROVAL_bi$$
CREATE TRIGGER trg_GVHD_APPROVAL_bi BEFORE INSERT ON GVHD_DETAI FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM DUYET_BO_MON WHERE Ma_De_Tai=NEW.Ma_De_Tai) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Huy xac nhan bo mon truoc khi doi GVHD.';
    END IF;
END$$
DROP TRIGGER IF EXISTS trg_GVHD_APPROVAL_bd$$
CREATE TRIGGER trg_GVHD_APPROVAL_bd BEFORE DELETE ON GVHD_DETAI FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM DUYET_BO_MON WHERE Ma_De_Tai=OLD.Ma_De_Tai) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Huy xac nhan bo mon truoc khi doi GVHD.';
    END IF;
END$$
DELIMITER ;
