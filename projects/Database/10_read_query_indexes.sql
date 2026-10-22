-- Migration 10: optional performance indexes. Run after 01-09, with a backup.
-- Additive and repeatable: no records, academic rules or constraints are removed.
-- Contains searches (LIKE '%keyword%') still require scanning the narrowed candidate set.
SET NAMES utf8mb4;
USE quanlydetai;
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_upgrade_read_indexes$$
CREATE PROCEDURE sp_upgrade_read_indexes()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='DE_TAI' AND INDEX_NAME='IX_DT_STATE_DATE') THEN
        CREATE INDEX IX_DT_STATE_DATE ON DE_TAI (Trang_Thai, Ngay_Dang_Ky DESC, Ma_De_Tai);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='DE_TAI' AND INDEX_NAME='IX_DT_PERIOD_STATE_DATE') THEN
        CREATE INDEX IX_DT_PERIOD_STATE_DATE ON DE_TAI (Ma_Dot, Trang_Thai, Ngay_Dang_Ky DESC, Ma_De_Tai);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='DE_TAI' AND INDEX_NAME='IX_DT_DEPARTMENT_STATE_DATE') THEN
        CREATE INDEX IX_DT_DEPARTMENT_STATE_DATE ON DE_TAI (Ma_Bo_Mon, Trang_Thai, Ngay_Dang_Ky DESC, Ma_De_Tai);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='TAI_KHOAN' AND INDEX_NAME='IX_TK_ROLE_USERNAME') THEN
        CREATE INDEX IX_TK_ROLE_USERNAME ON TAI_KHOAN (Vai_Tro, Ten_Dang_Nhap, Ma_TK);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='BAO_CAO' AND INDEX_NAME='IX_BC_DATE') THEN
        CREATE INDEX IX_BC_DATE ON BAO_CAO (Ngay_Nop DESC, Ma_BC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='THONG_BAO' AND INDEX_NAME='IX_TB_DATE') THEN
        CREATE INDEX IX_TB_DATE ON THONG_BAO (Ngay_Dang DESC, Ma_TB);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='DANG_KY_DE_TAI' AND INDEX_NAME='IX_DK_STATE_DATE') THEN
        CREATE INDEX IX_DK_STATE_DATE ON DANG_KY_DE_TAI (Trang_Thai, Ngay_Dang_Ky, Ma_DK);
    END IF;
END$$
CALL sp_upgrade_read_indexes()$$
DROP PROCEDURE sp_upgrade_read_indexes$$
DELIMITER ;
