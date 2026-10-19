-- Additive migration: preserves existing topics, accounts, registrations and results.
SET NAMES utf8mb4;
USE quanlydetai;
CREATE TABLE IF NOT EXISTS TRUONG_BO_MON (
    Ma_Bo_Mon VARCHAR(20) NOT NULL PRIMARY KEY,
    Ma_GV VARCHAR(20) NOT NULL,
    Dang_Giu_Chuc BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT FK_TBM_BM FOREIGN KEY (Ma_Bo_Mon) REFERENCES BO_MON(Ma_Bo_Mon) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT FK_TBM_GV FOREIGN KEY (Ma_GV) REFERENCES GV(Ma_GV) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS DUYET_BO_MON (
    Ma_De_Tai VARCHAR(20) NOT NULL PRIMARY KEY,
    Ma_GV VARCHAR(20) NOT NULL,
    Ngay_Duyet DATETIME NOT NULL,
    CONSTRAINT FK_DBM_DT FOREIGN KEY (Ma_De_Tai) REFERENCES DE_TAI(Ma_De_Tai) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT FK_DBM_GV FOREIGN KEY (Ma_GV) REFERENCES GV(Ma_GV) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
-- No fabricated approvals or automatic appointments. Admin assigns actual department heads.
-- Existing published topics remain published. Drafts must pass the new application review gate.
