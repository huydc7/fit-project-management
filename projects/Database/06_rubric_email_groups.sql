-- Rubric update 06/10/2026: new registrations need 3-5 students; durable email queue.
-- Non-destructive: no existing members/results are rewritten, no old results are emailed.
-- Run on quanlydetai AFTER scripts 01-05. Back up before applying to an existing DB.
-- EMAIL_KET_QUA is used by the restored result publication email module.
SET NAMES utf8mb4;
USE quanlydetai;

CREATE TABLE IF NOT EXISTS EMAIL_KET_QUA (
    Ma_Email VARCHAR(32) NOT NULL PRIMARY KEY,
    Ma_De_Tai VARCHAR(20) NOT NULL,
    Ma_SV VARCHAR(20) NOT NULL,
    Nguoi_Nhan VARCHAR(255) NOT NULL,
    Tieu_De VARCHAR(255) NOT NULL,
    Noi_Dung TEXT NOT NULL,
    Trang_Thai VARCHAR(20) NOT NULL,
    So_Lan_Thu INT NOT NULL DEFAULT 0,
    Ngay_Tao DATETIME NOT NULL,
    Lan_Thu_Tiep DATETIME NOT NULL,
    Ngay_Chap_Nhan DATETIME NULL,
    Ma_Dich_Vu VARCHAR(512) NULL,
    CONSTRAINT UQ_EMAIL_KQ_SV UNIQUE(Ma_De_Tai, Ma_SV),
    CONSTRAINT FK_EMAIL_KQ FOREIGN KEY(Ma_De_Tai) REFERENCES KET_QUA_DE_TAI(Ma_De_Tai),
    CONSTRAINT FK_EMAIL_SV FOREIGN KEY(Ma_SV) REFERENCES SV(Ma_SV),
    CONSTRAINT CK_EMAIL_STATE CHECK (Trang_Thai IN ('PENDING','PROCESSING','ACCEPTED','FAILED')),
    INDEX IX_EMAIL_DUE (Trang_Thai, Lan_Thu_Tiep)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DELIMITER $$
DROP TRIGGER IF EXISTS trg_NHOM_bu$$
CREATE TRIGGER trg_NHOM_bu
BEFORE UPDATE ON NHOM
FOR EACH ROW
BEGIN
    DECLARE v_CountTV INT DEFAULT 0;
    DECLARE v_CountLeader INT DEFAULT 0;
    DECLARE v_TrangThai VARCHAR(30);
    DECLARE v_BeginSV DATETIME;
    DECLARE v_EndSV DATETIME;

    IF NEW.Ma_Dot <> OLD.Ma_Dot THEN
        IF OLD.Ma_De_Tai IS NOT NULL
           OR EXISTS (
                SELECT 1
                  FROM THANH_VIEN_NHOM
                 WHERE Ma_Nhom = OLD.Ma_Nhom
           ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Không được đổi Ma_Dot khi nhóm đã có thành viên hoặc đã chọn đề tài.';
        END IF;
    END IF;

    IF OLD.Ma_De_Tai IS NOT NULL
       AND NOT (NEW.Ma_De_Tai <=> OLD.Ma_De_Tai) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Nhóm đã chọn đề tài; không được đổi hoặc hủy trực tiếp.';
    END IF;

    IF OLD.Ma_De_Tai IS NULL AND NEW.Ma_De_Tai IS NOT NULL THEN

        SELECT Trang_Thai
          INTO v_TrangThai
          FROM DE_TAI
         WHERE Ma_De_Tai = NEW.Ma_De_Tai
         FOR UPDATE;

        IF v_TrangThai <> 'DaCongBo' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Nhóm chỉ được chọn đề tài đang ở trạng thái DaCongBo.';
        END IF;

        SELECT Begin_SV, End_SV
          INTO v_BeginSV, v_EndSV
          FROM DOT_DANG_KY
         WHERE Ma_Dot = NEW.Ma_Dot;

        IF CURRENT_TIMESTAMP < v_BeginSV
           OR CURRENT_TIMESTAMP > v_EndSV THEN
            IF NOT EXISTS (SELECT 1 FROM DANG_KY_DE_TAI r
                WHERE r.Ma_Nhom=NEW.Ma_Nhom AND r.Ma_De_Tai=NEW.Ma_De_Tai
                  AND r.Trang_Thai='ChoDuyet' AND r.Ngay_Dang_Ky BETWEEN v_BeginSV AND v_EndSV) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Không nằm trong thời gian sinh viên chọn đề tài.';
            END IF;
        END IF;

        SELECT COUNT(*),
               COALESCE(SUM(Vai_Tro = 'Truong_Nhom'), 0)
          INTO v_CountTV, v_CountLeader
          FROM THANH_VIEN_NHOM
         WHERE Ma_Nhom = NEW.Ma_Nhom;

        IF v_CountTV < 3 OR v_CountTV > 5 OR v_CountLeader <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Khi chọn đề tài, nhóm phải có 3-5 SV và đúng 1 trưởng nhóm.';
        END IF;

        SET NEW.Ngay_Chon_De_Tai = CURRENT_TIMESTAMP;
    END IF;

    IF NEW.Ma_De_Tai IS NULL THEN
        SET NEW.Ngay_Chon_De_Tai = NULL;
    ELSEIF OLD.Ma_De_Tai IS NOT NULL THEN
        SET NEW.Ngay_Chon_De_Tai = OLD.Ngay_Chon_De_Tai;
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_THANH_VIEN_NHOM_bi$$
CREATE TRIGGER trg_THANH_VIEN_NHOM_bi
BEFORE INSERT ON THANH_VIEN_NHOM
FOR EACH ROW
BEGIN
    DECLARE v_Dot VARCHAR(20);
    DECLARE v_DeTai VARCHAR(20);
    DECLARE v_CountTV INT DEFAULT 0;
    DECLARE v_CountLeader INT DEFAULT 0;

    SELECT Ma_Dot, Ma_De_Tai
      INTO v_Dot, v_DeTai
      FROM NHOM
     WHERE Ma_Nhom = NEW.Ma_Nhom
     FOR UPDATE;

    SELECT COUNT(*)
      INTO v_CountTV
      FROM THANH_VIEN_NHOM
     WHERE Ma_Nhom = NEW.Ma_Nhom;

    IF v_CountTV >= 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Một nhóm tối đa 5 sinh viên.';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM THANH_VIEN_NHOM tv
          JOIN NHOM n ON n.Ma_Nhom = tv.Ma_Nhom
         WHERE tv.Ma_SV = NEW.Ma_SV
           AND n.Ma_Dot = v_Dot
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh viên đã thuộc một nhóm khác trong cùng đợt.';
    END IF;

    IF v_DeTai IS NOT NULL THEN
        SELECT COALESCE(SUM(Vai_Tro = 'Truong_Nhom'),0)
          INTO v_CountLeader
          FROM THANH_VIEN_NHOM
         WHERE Ma_Nhom = NEW.Ma_Nhom;

        SET v_CountLeader = v_CountLeader + (NEW.Vai_Tro = 'Truong_Nhom');

        IF v_CountLeader <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Nhóm đã chọn đề tài phải luôn có đúng 1 trưởng nhóm.';
        END IF;
    END IF;
END$$
DELIMITER ;
