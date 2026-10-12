-- Run after 01, 02, 03 on MySQL 8.0.16+. Additive workflow extension; never drops data.
SET NAMES utf8mb4;
USE quanlydetai;
CREATE TABLE IF NOT EXISTS DANG_KY_DE_TAI (
 Ma_DK VARCHAR(30) NOT NULL PRIMARY KEY,
 Ma_Nhom VARCHAR(20) NOT NULL,
 Ma_De_Tai VARCHAR(20) NOT NULL,
 Trang_Thai VARCHAR(20) NOT NULL,
 Ngay_Dang_Ky DATETIME NOT NULL,
 Ly_Do VARCHAR(1000) NULL,
 Ma_GV_Duyet VARCHAR(20) NULL,
 Ngay_Duyet DATETIME NULL,
 Active_Group VARCHAR(20) NULL UNIQUE,
 Active_Topic VARCHAR(20) NULL UNIQUE,
 CONSTRAINT FK_DK_NHOM FOREIGN KEY(Ma_Nhom) REFERENCES NHOM(Ma_Nhom),
 CONSTRAINT FK_DK_DETAI FOREIGN KEY(Ma_De_Tai) REFERENCES DE_TAI(Ma_De_Tai),
 CONSTRAINT FK_DK_GV FOREIGN KEY(Ma_GV_Duyet) REFERENCES GV(Ma_GV),
 CONSTRAINT CK_DK_STATUS CHECK(Trang_Thai IN ('ChoDuyet','ChapThuan','TuChoi','Rut')),
 CONSTRAINT CK_DK_HOLD CHECK(
   (Trang_Thai IN ('ChoDuyet','ChapThuan') AND (Active_Group <=> Ma_Nhom) AND (Active_Topic <=> Ma_De_Tai)) OR
   (Trang_Thai IN ('TuChoi','Rut') AND Active_Group IS NULL AND Active_Topic IS NULL))
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS LOI_MOI_NHOM (
 Ma_Loi_Moi VARCHAR(30) NOT NULL PRIMARY KEY,
 Ma_Nhom VARCHAR(20) NOT NULL,
 Ma_SV VARCHAR(20) NOT NULL,
 Trang_Thai VARCHAR(20) NOT NULL,
 Ngay_Moi DATETIME NOT NULL,
 CONSTRAINT UQ_LOI_MOI UNIQUE(Ma_Nhom,Ma_SV),
 CONSTRAINT FK_MOI_NHOM FOREIGN KEY(Ma_Nhom) REFERENCES NHOM(Ma_Nhom),
 CONSTRAINT FK_MOI_SV FOREIGN KEY(Ma_SV) REFERENCES SV(Ma_SV),
 CONSTRAINT CK_MOI_STATUS CHECK(Trang_Thai IN ('Cho','XacNhan','TuChoi'))
) ENGINE=InnoDB;

DELIMITER $$
-- Upgrade an existing database without repeating ALTER on a constraint already installed.
DROP PROCEDURE IF EXISTS sp_upgrade_workflow$$
CREATE PROCEDURE sp_upgrade_workflow()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='TRUONG_KHOA' AND COLUMN_NAME='Dang_Giu_Chuc') THEN
  ALTER TABLE TRUONG_KHOA ADD COLUMN Dang_Giu_Chuc BOOLEAN NOT NULL DEFAULT TRUE;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='DIEM_THANH_PHAN' AND CONSTRAINT_NAME='CK_DIEM_RANGE') THEN
  ALTER TABLE DIEM_THANH_PHAN ADD CONSTRAINT CK_DIEM_RANGE CHECK(Diem>=0 AND Diem<=10);
 END IF;
END$$
CALL sp_upgrade_workflow()$$
DROP PROCEDURE sp_upgrade_workflow$$
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

        IF v_CountTV < 1 OR v_CountTV > 3 OR v_CountLeader <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Khi chọn đề tài, nhóm phải có 1-3 SV và đúng 1 trưởng nhóm.';
        END IF;

        SET NEW.Ngay_Chon_De_Tai = CURRENT_TIMESTAMP;
    END IF;

    IF NEW.Ma_De_Tai IS NULL THEN
        SET NEW.Ngay_Chon_De_Tai = NULL;
    ELSEIF OLD.Ma_De_Tai IS NOT NULL THEN
        SET NEW.Ngay_Chon_De_Tai = OLD.Ngay_Chon_De_Tai;
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_KET_QUA_DE_TAI_bi$$
CREATE TRIGGER trg_KET_QUA_DE_TAI_bi
BEFORE INSERT ON KET_QUA_DE_TAI
FOR EACH ROW
BEGIN
    DECLARE v_Ma_HD VARCHAR(20);
    DECLARE v_AvgGVPB DECIMAL(10,4);
    DECLARE v_AvgHD DECIMAL(10,4);
    DECLARE v_Missing INT DEFAULT 0;
    DECLARE v_Dummy VARCHAR(20);
    DECLARE v_TrangThaiDeTai VARCHAR(30);

    -- Lock đề tài, serialize với thao tác điểm.
    SELECT Ma_Dot, Trang_Thai
      INTO v_Dummy, v_TrangThaiDeTai
      FROM DE_TAI
     WHERE Ma_De_Tai = NEW.Ma_De_Tai
     FOR UPDATE;

    IF v_TrangThaiDeTai NOT IN ('DangThucHien','HoanThanh') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chỉ tổng hợp kết quả khi đề tài đang thực hiện hoặc đã hoàn thành.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM NHOM
         WHERE Ma_De_Tai = NEW.Ma_De_Tai
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đề tài chưa có nhóm thực hiện.';
    END IF;

    SELECT hd.Ma_HD
      INTO v_Ma_HD
      FROM HOI_DONG_DE_TAI hdt
      JOIN HOI_DONG hd ON hd.Ma_HD = hdt.Ma_HD
     WHERE hdt.Ma_De_Tai = NEW.Ma_De_Tai
       AND hd.Trang_Thai = 'DaChot'
     LIMIT 1;

    IF v_Ma_HD IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đề tài chưa thuộc hội đồng đã chốt.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM THANHVIEN_HOIDONG
         WHERE Ma_HD = v_Ma_HD
           AND Ma_GV = NEW.Ma_GV_Tong_Hop
           AND Vai_Tro = 'ChuTich'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Người tổng hợp phải là Chủ tịch đúng hội đồng.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM GVPB_DETAI g
          JOIN DIEM_THANH_PHAN d
            ON d.Ma_GV = g.Ma_GV
           AND d.Ma_De_Tai = g.Ma_De_Tai
           AND d.Ma_Loai_Diem = 'GVPB'
         WHERE g.Ma_De_Tai = NEW.Ma_De_Tai
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chưa có đủ điểm GVPB.';
    END IF;

    SELECT COUNT(*)
      INTO v_Missing
      FROM THANHVIEN_HOIDONG tv
     WHERE tv.Ma_HD = v_Ma_HD
       AND NOT EXISTS (
            SELECT 1
              FROM DIEM_THANH_PHAN d
             WHERE d.Ma_De_Tai = NEW.Ma_De_Tai
               AND d.Ma_GV = tv.Ma_GV
               AND d.Ma_Loai_Diem = 'HD'
       );

    IF v_Missing > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chưa đủ điểm hội đồng của tất cả thành viên.';
    END IF;

    SELECT AVG(Diem)
      INTO v_AvgGVPB
      FROM DIEM_THANH_PHAN
     WHERE Ma_De_Tai = NEW.Ma_De_Tai
       AND Ma_Loai_Diem = 'GVPB';

    SELECT AVG(Diem)
      INTO v_AvgHD
      FROM DIEM_THANH_PHAN
     WHERE Ma_De_Tai = NEW.Ma_De_Tai
       AND Ma_Loai_Diem = 'HD';

    IF v_AvgGVPB IS NULL OR v_AvgHD IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Thiếu loại điểm bắt buộc GVPB hoặc HD.';
    END IF;

    SET NEW.Diem_Cuoi = (SELECT ROUND(AVG(Diem), 2) FROM DIEM_THANH_PHAN WHERE Ma_De_Tai = NEW.Ma_De_Tai);
    SET NEW.Trang_Thai_Cong_Bo = 'ChuaCongBo';
    SET NEW.Ngay_Tong_Hop = CURRENT_TIMESTAMP;
    SET NEW.Ngay_Cong_Bo = NULL;
END$$

DROP TRIGGER IF EXISTS trg_DIEM_THANH_PHAN_bu$$
CREATE TRIGGER trg_DIEM_THANH_PHAN_bu
BEFORE UPDATE ON DIEM_THANH_PHAN
FOR EACH ROW
BEGIN
    IF NEW.Ma_Diem <> OLD.Ma_Diem OR NEW.Ma_GV <> OLD.Ma_GV
       OR NEW.Ma_De_Tai <> OLD.Ma_De_Tai OR NEW.Ma_Loai_Diem <> OLD.Ma_Loai_Diem THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Không được đổi Ma_Diem.';
    END IF;

    IF NOT (NEW.Ngay_Nop_Diem <=> OLD.Ngay_Nop_Diem) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ngay_Nop_Diem là timestamp hệ thống, không được sửa.';
    END IF;

    CALL sp_validate_diem(NEW.Ma_GV, NEW.Ma_De_Tai, NEW.Ma_Loai_Diem);
END$$


DROP TRIGGER IF EXISTS trg_BAO_CAO_freeze_bi$$
CREATE TRIGGER trg_BAO_CAO_freeze_bi BEFORE INSERT ON BAO_CAO FOR EACH ROW
BEGIN
 DECLARE topic VARCHAR(20);
 SELECT Ma_De_Tai INTO topic FROM NHOM WHERE Ma_Nhom=NEW.Ma_Nhom;
 IF EXISTS(SELECT 1 FROM DIEM_THANH_PHAN WHERE Ma_De_Tai=topic)
   OR EXISTS(SELECT 1 FROM KET_QUA_DE_TAI WHERE Ma_De_Tai=topic) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Đã chấm điểm; báo cáo dùng để chấm đã bị khóa.';
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_TVN_freeze_bi$$
CREATE TRIGGER trg_TVN_freeze_bi BEFORE INSERT ON THANH_VIEN_NHOM FOR EACH ROW
BEGIN
 IF EXISTS(SELECT 1 FROM NHOM WHERE Ma_Nhom=NEW.Ma_Nhom AND Ma_De_Tai IS NOT NULL)
  OR EXISTS(SELECT 1 FROM DANG_KY_DE_TAI WHERE Active_Group=NEW.Ma_Nhom) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Thành viên nhóm đã khóa khi đăng ký đề tài.';
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_TVN_freeze_bd$$
CREATE TRIGGER trg_TVN_freeze_bd BEFORE DELETE ON THANH_VIEN_NHOM FOR EACH ROW
BEGIN
 IF EXISTS(SELECT 1 FROM NHOM WHERE Ma_Nhom=OLD.Ma_Nhom AND Ma_De_Tai IS NOT NULL)
  OR EXISTS(SELECT 1 FROM DANG_KY_DE_TAI WHERE Active_Group=OLD.Ma_Nhom) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Thành viên nhóm đã khóa khi đăng ký đề tài.';
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_TVN_freeze_bu$$
CREATE TRIGGER trg_TVN_freeze_bu BEFORE UPDATE ON THANH_VIEN_NHOM FOR EACH ROW
BEGIN
 IF EXISTS(SELECT 1 FROM NHOM WHERE Ma_Nhom=OLD.Ma_Nhom AND Ma_De_Tai IS NOT NULL)
  OR EXISTS(SELECT 1 FROM DANG_KY_DE_TAI WHERE Active_Group=OLD.Ma_Nhom) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Thành viên nhóm đã khóa khi đăng ký đề tài.';
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_THONG_BAO_bi$$
CREATE TRIGGER trg_THONG_BAO_bi
BEFORE INSERT ON THONG_BAO
FOR EACH ROW
BEGIN
    DECLARE v_VaiTro VARCHAR(20);
    DECLARE v_MaGV VARCHAR(20);
    DECLARE v_TrangThai VARCHAR(20);

    SELECT Vai_Tro, Ma_GV, Trang_Thai
      INTO v_VaiTro, v_MaGV, v_TrangThai
      FROM TAI_KHOAN
     WHERE Ma_TK = NEW.Ma_TK;

    IF v_TrangThai <> 'HoatDong' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tài khoản đang bị khóa.';
    END IF;

    IF NEW.Pham_Vi = 'NhaTruong' AND v_VaiTro <> 'Admin' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chỉ Admin được đăng thông báo phạm vi NhaTruong.';
    END IF;

    IF NEW.Pham_Vi = 'Khoa'
       AND v_VaiTro <> 'Admin'
       AND NOT (
            v_VaiTro = 'GV'
            AND EXISTS (
                SELECT 1 FROM TRUONG_KHOA WHERE Ma_GV = v_MaGV AND Dang_Giu_Chuc = TRUE
            )
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Thông báo Khoa chỉ do Admin hoặc Trưởng khoa đăng.';
    END IF;

    SET NEW.Ngay_Dang = CURRENT_TIMESTAMP;
END$$

DROP TRIGGER IF EXISTS trg_THONG_BAO_bu$$
CREATE TRIGGER trg_THONG_BAO_bu
BEFORE UPDATE ON THONG_BAO
FOR EACH ROW
BEGIN
    DECLARE v_VaiTro VARCHAR(20);
    DECLARE v_MaGV VARCHAR(20);
    DECLARE v_TrangThai VARCHAR(20);

    SELECT Vai_Tro, Ma_GV, Trang_Thai
      INTO v_VaiTro, v_MaGV, v_TrangThai
      FROM TAI_KHOAN
     WHERE Ma_TK = NEW.Ma_TK;

    IF v_TrangThai <> 'HoatDong' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tài khoản đang bị khóa.';
    END IF;

    IF NEW.Pham_Vi = 'NhaTruong' AND v_VaiTro <> 'Admin' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chỉ Admin được đăng thông báo phạm vi NhaTruong.';
    END IF;

    IF NEW.Pham_Vi = 'Khoa'
       AND v_VaiTro <> 'Admin'
       AND NOT (
            v_VaiTro = 'GV'
            AND EXISTS (
                SELECT 1 FROM TRUONG_KHOA WHERE Ma_GV = v_MaGV AND Dang_Giu_Chuc = TRUE
            )
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Thông báo Khoa chỉ do Admin hoặc Trưởng khoa đăng.';
    END IF;
END$$

DELIMITER ;
