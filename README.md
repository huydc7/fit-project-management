# Hệ thống quản lý đề tài khoa CNTT

Ứng dụng MVC dùng Java 21, Spring Boot, Spring Data JPA/Hibernate, Spring Security, Thymeleaf, HTML/CSS thuần, JavaScript thuần và MySQL. Bản đầu hỗ trợ đợt đăng ký, đề xuất/duyệt đề tài, nhóm và lời mời, duyệt đăng ký, báo cáo, phản biện/hội đồng, chấm/tổng hợp/công bố, tài khoản và thông báo. Quy ước giao diện xem [docs/UI_DESIGN.md](docs/UI_DESIGN.md).

Bắt đầu tại [hướng dẫn local](docs/LOCAL_TEST.md). Xem [danh mục và dữ liệu thực hành](docs/SAMPLE_DATA.md), [quy tắc nghiệp vụ](docs/PRODUCT_RULES.md), [bàn giao kỹ thuật](docs/HUY_HANDOFF.md), [kiểm chứng](docs/VERIFY_HUY.md), [cloud](docs/CLOUD_SETUP.md). Nếu chưa có tài khoản dịch vụ, làm theo [hướng dẫn deploy từng bước](docs/HUONG_DAN_DEPLOY_TUNG_BUOC.md).

Đưa source lên GitHub, mời thành viên và review qua Pull Request: [quy trình làm việc nhóm](docs/GITHUB_TEAMWORK.md).

## Chạy Docker

```powershell
cd D:\project_web\final_projects
# Chỉ copy khi chưa có .env; không ghi đè cấu hình đang dùng.
Copy-Item .env.example .env
# Điền MYSQL_ROOT_PASSWORD và DB_PASSWORD.
# Dữ liệu thực hành được nhập bằng công cụ riêng; xem docs/SAMPLE_DATA.md.
.\tools\Start-Local.ps1 -Docker
```

Website mặc định http://localhost:8080; MySQL Docker tại 127.0.0.1:3307. Database và báo cáo dùng volume. SQL 01–06 tự nạp ở lần tạo volume đầu tiên; volume cũ đã có 01–05 cần `tools/Update-RubricDatabase.ps1` để sao lưu và nâng cấp 06 trước khi chạy bản mới. Không xóa volume để nâng cấp. Docker dùng JDK/JRE 21, user app không phải root.

## Chạy Java và MySQL đã cài

Nạp `projects/Database/01_schema_mysql.sql` → `02_logic_mysql.sql` → `03_views_seed_mysql.sql` → `04_workflow_mysql.sql` → `05_department_catalog_mysql.sql` → `06_rubric_email_groups.sql`. CSDL có 22 bảng, 35 trigger, 4 view. Bản gốc đủ 01–03 cần 04/05/06; sao lưu trước khi nâng cấp. Hibernate dùng validate.

Tạo `.env.native` riêng với DB_URL trỏ localhost:3306/quanlydetai, DB_USERNAME/DB_PASSWORD của MySQL trên máy, PORT=8081 nếu Docker đang dùng 8080, STORAGE_BACKEND=local. Có thể bật dữ liệu thực hành theo SAMPLE_DATA.md. Chạy:

```powershell
.\tools\Start-Local.ps1 -EnvFile .env.native
```

Script đọc đúng file cấu hình được chọn; Maven/IDE không tự đọc .env. CSS/JS đóng gói local, không phụ thuộc CDN.

## Dữ liệu

Bốn bộ môn: Công nghệ phần mềm, Hệ thống thông tin, Mạng và An ninh mạng, Trí tuệ nhân tạo. Catalog là dữ liệu nghiệp vụ. Bộ thực hành giả lập nằm trong một JSON ngoài source/JAR, mặc định tắt và chỉ được nạp ở profile dev. Fixture kiểm thử nằm trong src/test, không nạp vào DB vận hành.

Tài khoản thực hành: admin, truongkhoa, gv.hoangnam, gv.minhanh, gv.thanhbinh, gv.thuha và các tài khoản khác ghi trong SAMPLE_DATA.md; SV từ sv.22110001 đến sv.22110018. Tài khoản mới dùng SAMPLE_DATA_PASSWORD; cập nhật bộ cũ giữ mật khẩu và trạng thái đã có.

## Kiểm thử

```powershell
cd projects/quanlydetai
.\mvnw.cmd clean verify
```

H2, SMTP và HTTP giả lập không cần cloud. Test MySQL dùng DB riêng đã nạp 01–06; có thể tạo bằng `docker compose -f compose.test.yaml up -d --wait` (cổng 3308, mật khẩu chỉ dành cho test nằm trong file này). Đặt TEST_DB_URL, TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver, TEST_DB_USERNAME, TEST_DB_PASSWORD, TEST_DDL_MODE=validate, TEST_DB_INIT_SQL="SET time_zone='+07:00'" rồi chạy `mvnw.cmd '-Dtest=FoundationIntegrationTest,WorkflowIntegrationTest,MySqlTriggerIntegrationTest' test`. Không chạy trên DB vận hành. JAR nằm trong target/quanlydetai-0.0.1-SNAPSHOT.jar.

## Admin đầu tiên khi không dùng bộ thực hành

Từ thư mục backend, tạo BCrypt trong terminal tương tác:

```powershell
.\mvnw.cmd -q dependency:build-classpath '-Dmdep.outputFile=target/runtime-classpath.txt' -DincludeScope=runtime
java --class-path (Get-Content target/runtime-classpath.txt -Raw).Trim() ../../tools/PasswordHash.java
```

Thêm qua Workbench:

```sql
INSERT INTO TAI_KHOAN (Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Trang_Thai)
VALUES ('ADMIN01','admin','<BCrypt vừa tạo>','Admin','HoatDong');
```

Admin tạo hồ sơ/tài khoản và bổ nhiệm Trưởng khoa qua UI. Trưởng khoa là GV có Dang_Giu_Chuc=true, không phải vai trò mới trong TAI_KHOAN. Admin không mặc nhiên có quyền chấm điểm hoặc đọc báo cáo.

## Source và triển khai

22 entity bảng, 8 interface Spring Data JPA và JpaEntityRepository cho giao dịch nhiều entity (persist/flush/refresh/khóa ghi). Controller dùng DTO; service kiểm tra vai trò, quan hệ, thời gian và trạng thái. Không chứa hồ sơ/đề tài giả trong code nghiệp vụ. SQL view giữ cho truy vấn DB; đã bỏ lớp view và repository không còn sử dụng.

Công bố kết quả tạo một email cho mỗi thành viên trong cùng transaction; worker gửi sau commit, có thử lại và trang `/dean/result-emails`. Mặc định `EMAIL_ENABLED=false`; local dùng Mailpit, Render dùng Brevo HTTPS. Xem cấu hình và nghiệm thu trong hướng dẫn deploy từng bước. SV tra cứu điểm bằng `/student/results?period=...`, chỉ trong các đợt đã đăng ký.

Local lưu file trong uploads hoặc LOCAL_STORAGE_PATH, tải qua endpoint kiểm tra quyền. Cloud dùng Supabase private, signed URL 300 giây, TLS MySQL VERIFY_IDENTITY với CA; prod luôn tắt bộ dữ liệu thực hành. Cấu hình CI/Render có sẵn, chưa đồng nghĩa cloud đã nghiệm thu.

Không commit .env, .env.native, secret, uploads, target, truststore hoặc backups.

## Kiến trúc và API

Xem [giải thích cấu trúc](docs/ARCHITECTURE.md) và [REST API v1](docs/REST_API.md). Controller HTML và REST dùng chung service; công cụ nhập dữ liệu thực hành chỉ ở test classpath, không được đóng gói vào JAR.
