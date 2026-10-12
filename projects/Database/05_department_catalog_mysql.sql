-- Required faculty catalog based on the four specializations selected by the project team.
-- This is project configuration, not an assertion of the university's official organization.
-- Safe to rerun. Existing topics and teachers keep their identities and all academic history.
SET NAMES utf8mb4;
USE quanlydetai;
START TRANSACTION;

-- Free the unique department names before consolidating the former sample catalog.
UPDATE BO_MON SET Ten_Bo_Mon = '[Danh mục cũ] Công nghệ phần mềm' WHERE Ma_Bo_Mon = 'DEMO_CNPM';
UPDATE BO_MON SET Ten_Bo_Mon = '[Danh mục cũ] Hệ thống thông tin' WHERE Ma_Bo_Mon = 'DEMO_HTTT';

INSERT INTO BO_MON (Ma_Bo_Mon, Ten_Bo_Mon) VALUES
    ('CNPM', 'Công nghệ phần mềm'),
    ('HTTT', 'Hệ thống thông tin'),
    ('MANG_ANM', 'Mạng và An ninh mạng'),
    ('TTNT', 'Trí tuệ nhân tạo')
ON DUPLICATE KEY UPDATE Ten_Bo_Mon = VALUES(Ten_Bo_Mon);

UPDATE GV SET Ma_Bo_Mon = 'CNPM' WHERE Ma_Bo_Mon = 'DEMO_CNPM';
UPDATE GV SET Ma_Bo_Mon = 'HTTT' WHERE Ma_Bo_Mon = 'DEMO_HTTT';
UPDATE DE_TAI SET Ma_Bo_Mon = 'CNPM' WHERE Ma_Bo_Mon = 'DEMO_CNPM';
UPDATE DE_TAI SET Ma_Bo_Mon = 'HTTT' WHERE Ma_Bo_Mon = 'DEMO_HTTT';
DELETE FROM BO_MON WHERE Ma_Bo_Mon IN ('DEMO_CNPM', 'DEMO_HTTT');
COMMIT;
