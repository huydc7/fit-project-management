QUAN LY DE TAI - DATABASE MYSQL
==============================

YEU CAU
-------
- MySQL 8.0.16+ de CHECK constraint duoc enforce; Docker/CI dung MySQL 8.0.46.
- Khong dung MariaDB hoac SQL Server cho bo script nay.

DATABASE MOI
------------
Mo tung script trong MySQL Workbench va Execute All theo dung thu tu:
1. 01_schema_mysql.sql
2. 02_logic_mysql.sql
3. 03_views_seed_mysql.sql
4. 04_workflow_mysql.sql
5. 05_department_catalog_mysql.sql
6. 06_rubric_email_groups.sql
7. 07_department_review.sql
8. 08_topic_review_deadline.sql
9. 09_department_supervisor_assignment.sql

Docker Compose khoi tao database moi bang cung bo script 01-09.
Xem ../../docs/LOCAL_TEST.md de chay ung dung va khoi tao Admin.
Bo du lieu thuc hanh la tuy chon: ../../docs/SAMPLE_DATA.md.

DATABASE DA CO DU LIEU
---------------------
Sao luu truoc khi nang cap; khong xoa volume hoac nap lai schema de reset.
Cac script PowerShell duoi day ap dung cho MySQL Docker cua du an:
- Da co 01-05: ../../tools/Update-RubricDatabase.ps1
- Da co 01-06: ../../tools/Update-DepartmentReview.ps1
- Da co 01-07: ../../tools/Update-TopicReviewDeadline.ps1
- Da co 01-08: sao luu, dung web va chay 09_department_supervisor_assignment.sql
Thuc hien cac buoc con thieu theo thu tu. Cac script PowerShell nang cap tu sao luu; SQL 09 chay thu cong sau khi sao luu.
Huong dan SQL 09: ../../docs/DEPARTMENT_SUPERVISOR_WORKFLOW.md.
MySQL cai tren may: tu sao luu va chay migration con thieu trong Workbench.
Huong dan chi tiet: ../../docs/LOCAL_TEST.md.

SO DO CHUAN
-----------
- ERD_QuanLyDeTai_V2.drawio: ERD hien tai, 24 bang.
- USE_CASE_FIT.drawio: tac nhan va chuc nang.
- ../../docs/DATABASE_DICTIONARY.md: cot, khoa va rang buoc.

KIEM TRA SAU SQL 01-09
---------------------
USE quanlydetai;
SHOW FULL TABLES;
SHOW TRIGGERS;
SHOW PROCEDURE STATUS WHERE Db = 'quanlydetai';
SELECT * FROM LOAI_DIEM;
SELECT * FROM VW_DE_TAI_DA_CONG_BO;
SELECT * FROM VW_KET_QUA_DA_CONG_BO;

Ket qua: 24 bang nghiep vu, 4 view, 38 trigger va helper sp_validate_diem.
Dieu kien si so nhom, duyet hai buoc, tinh diem va cong bo:
../../docs/PRODUCT_RULES.md.
Mat_Khau_Hash chi luu BCrypt; khong dat mat khau/API key that vao SQL.
Khong gui database dump hoac file bao cao rieng trong source chia se.
