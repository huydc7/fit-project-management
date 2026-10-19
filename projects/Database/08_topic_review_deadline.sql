-- Migration 08: strict approval/publication before Begin_SV, confirmed by Huy 19/10/2026.
-- Non-destructive: no existing topic, review, period, membership or score is rewritten.
SET NAMES utf8mb4;
USE quanlydetai;
DELIMITER $$
DROP TRIGGER IF EXISTS trg_DE_TAI_bu$$
CREATE TRIGGER trg_DE_TAI_bu
BEFORE UPDATE ON DE_TAI
FOR EACH ROW
BEGIN
    DECLARE v_SoGVHD INT DEFAULT 0;
    DECLARE v_BeginSV DATETIME;

    IF NEW.Ma_Dot <> OLD.Ma_Dot
       AND OLD.Trang_Thai <> 'Nhap' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Không được đổi Ma_Dot sau khi đề tài rời trạng thái Nhap.';
    END IF;

    IF NEW.Trang_Thai <> OLD.Trang_Thai THEN
        IF NOT (
               (OLD.Trang_Thai = 'Nhap'         AND NEW.Trang_Thai IN ('DaCongBo','Huy'))
            OR (OLD.Trang_Thai = 'DaCongBo'     AND NEW.Trang_Thai IN ('DangThucHien','Huy'))
            OR (OLD.Trang_Thai = 'DangThucHien' AND NEW.Trang_Thai = 'HoanThanh')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Chuyển trạng thái đề tài không hợp lệ.';
        END IF;

        IF OLD.Trang_Thai = 'Nhap' AND NEW.Trang_Thai = 'DaCongBo' THEN
            SELECT Begin_SV INTO v_BeginSV FROM DOT_DANG_KY WHERE Ma_Dot=NEW.Ma_Dot;
            IF CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+07:00') >= v_BeginSV THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Chi cong bo de tai truoc khi bat dau dang ky SV.';
            END IF;
            SELECT COUNT(*)
              INTO v_SoGVHD
              FROM GVHD_DETAI
             WHERE Ma_De_Tai = NEW.Ma_De_Tai;

            IF v_SoGVHD < 1 OR v_SoGVHD > 2 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Chỉ được công bố đề tài khi có từ 1 đến 2 GVHD.';
            END IF;
        END IF;

        IF OLD.Trang_Thai = 'DaCongBo'
           AND NEW.Trang_Thai = 'DangThucHien'
           AND NOT EXISTS (
                SELECT 1 FROM NHOM WHERE Ma_De_Tai = NEW.Ma_De_Tai
           ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Đề tài phải có nhóm đăng ký trước khi chuyển sang DangThucHien.';
        END IF;

        IF NEW.Trang_Thai = 'Huy'
           AND EXISTS (
                SELECT 1 FROM NHOM WHERE Ma_De_Tai = NEW.Ma_De_Tai
           ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Không thể hủy đề tài đã có nhóm đăng ký.';
        END IF;
    END IF;
END$$
DROP TRIGGER IF EXISTS trg_DUYET_BO_MON_bi$$
CREATE TRIGGER trg_DUYET_BO_MON_bi BEFORE INSERT ON DUYET_BO_MON FOR EACH ROW
BEGIN
    DECLARE v_BeginSV DATETIME;
    SELECT d.Begin_SV INTO v_BeginSV FROM DE_TAI t JOIN DOT_DANG_KY d ON d.Ma_Dot=t.Ma_Dot WHERE t.Ma_De_Tai=NEW.Ma_De_Tai;
    IF CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+07:00') >= v_BeginSV THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Chi duyet bo mon truoc khi bat dau dang ky SV.';
    END IF;
END$$
DELIMITER ;
