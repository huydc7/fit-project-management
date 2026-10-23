# Hệ thống quản lý đề tài khoa CNTT

Ứng dụng quản lý quy trình đề tài sinh viên của Khoa CNTT, từ đề xuất, đăng ký nhóm và nộp báo cáo đến chấm điểm, công bố kết quả. Hệ thống dùng Java 21, Spring Boot 4.1.1, Spring Data JPA/Hibernate, Spring Security, Thymeleaf, HTML/CSS, JavaScript và MySQL.

## Chức năng chính

- Admin quản lý tài khoản, hồ sơ, bộ môn, bổ nhiệm chức vụ và thông báo chung.
- Trưởng khoa tạo đợt đăng ký, xác nhận đề tài, duyệt đăng ký nhóm, phân công đánh giá và công bố kết quả. Lịch GVPB phải sau hạn đăng ký SV; với KLTN, hạn GVPB phải trước ngày báo cáo hội đồng.
- Giảng viên đề xuất đề tài; Trưởng bộ môn duyệt và chọn 1–2 GVHD. Mỗi GV hướng dẫn tối đa 3 đề tài trong một đợt, kể cả đồng hướng dẫn.
- Sinh viên tạo nhóm, mời và xác nhận thành viên; mỗi SV chỉ thuộc một nhóm trong mỗi đợt. Nhóm cần 3–5 SV khi đăng ký, có đúng một nhóm trưởng đăng ký và nộp báo cáo.
- Từ chối đăng ký nhóm bắt buộc có lý do. Lý do hiển thị riêng trong Thông báo và trang chủ của đúng nhóm trưởng, không đưa vào thông báo chung.
- Lập hội đồng chọn đề tài trước; danh sách chủ tịch, thư ký và thành viên loại GVHD/GVPB của đề tài đó. Tạo, chốt và gán hội đồng trong cùng một transaction.
- Báo cáo PDF/DOCX/ZIP có lịch sử phiên bản và kiểm tra quyền tải. GVPB/hội đồng chấm điểm; Chủ tịch tổng hợp, Trưởng khoa công bố. Sinh viên chỉ tra cứu kết quả nhóm mình theo đợt đã tham gia sau công bố.
- Công bố kết quả tạo email riêng cho từng SV; có hàng đợi, theo dõi và thử lại thư lỗi. Thông báo chung và riêng được tìm kiếm, phân trang và sắp xếp mới nhất trước.

Các chức năng trên dùng cấu trúc database SQL 01–10; không cần thêm bảng/cột cho giới hạn GVHD, thông báo riêng hoặc lựa chọn hội đồng. Quy ước giao diện xem [docs/UI_DESIGN.md](docs/UI_DESIGN.md).

Bắt đầu tại [hướng dẫn local và kiểm thử](docs/LOCAL_TEST.md). Xem [danh mục và dữ liệu thực hành](docs/SAMPLE_DATA.md), [quy tắc nghiệp vụ](docs/PRODUCT_RULES.md), [kiến trúc](docs/ARCHITECTURE.md) và [cloud](docs/CLOUD_SETUP.md). Nếu chưa có tài khoản dịch vụ, làm theo [hướng dẫn deploy từng bước](docs/HUONG_DAN_DEPLOY_TUNG_BUOC.md).

Đưa source lên GitHub, mời thành viên và review qua Pull Request: [quy trình làm việc nhóm](docs/GITHUB_TEAMWORK.md).

Email có tìm/lọc/phân trang và REST; danh sách đề tài tải dữ liệu liên quan theo tập mã để tránh truy vấn lặp theo từng dòng. Xem [thiết kế xử lý và giới hạn](docs/ARCHITECTURE.md).

Thời hạn duyệt trước giai đoạn SV, sửa hội đồng và gia hạn đợt được mô tả trong [quy tắc nghiệp vụ](docs/PRODUCT_RULES.md). Database mới chạy SQL 01–10; database đã có dữ liệu nâng cấp theo [hướng dẫn local](docs/LOCAL_TEST.md).

## Chạy Docker

```powershell
# Chạy tại thư mục gốc repository.
# Chỉ copy khi chưa có .env; không ghi đè cấu hình đang dùng.
Copy-Item .env.example .env
# Điền MYSQL_ROOT_PASSWORD và DB_PASSWORD.
# Dữ liệu thực hành được nhập bằng công cụ riêng; xem docs/SAMPLE_DATA.md.
.\tools\Start-Local.ps1 -Docker
```

Website mặc định http://localhost:8080; MySQL Docker tại 127.0.0.1:3307. Database và báo cáo dùng volume. SQL 01–10 tự nạp khi tạo volume mới lần đầu. Với volume cũ, sao lưu trước rồi chạy lần lượt các migration còn thiếu: `tools/Update-RubricDatabase.ps1` nâng cấp 06, `tools/Update-DepartmentReview.ps1` nâng cấp 07, `tools/Update-TopicReviewDeadline.ps1` nâng cấp 08, SQL 09 theo [hướng dẫn phân công GVHD](docs/DEPARTMENT_SUPERVISOR_WORKFLOW.md), rồi `projects/Database/10_read_query_indexes.sql`. Database đã có 01–09 cần sao lưu và chạy SQL 10. Không xóa volume để nâng cấp. Docker dùng JDK/JRE 21, user app không phải root.

## Chạy Java và MySQL đã cài

Nạp `projects/Database/01_schema_mysql.sql` → `02_logic_mysql.sql` → `03_views_seed_mysql.sql` → `04_workflow_mysql.sql` → `05_department_catalog_mysql.sql` → `06_rubric_email_groups.sql` → `07_department_review.sql` → `08_topic_review_deadline.sql` → `09_department_supervisor_assignment.sql` → `10_read_query_indexes.sql`. CSDL có 24 bảng, 38 trigger, 4 view. Bản gốc đủ 01–03 cần 04/05/06/07/08/09/10; sao lưu trước khi nâng cấp. Hibernate dùng validate.

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

H2 và HTTP giả lập không cần cloud. Test MySQL dùng DB riêng đã nạp 01–10; có thể tạo bằng `docker compose -f compose.test.yaml up -d --wait` (cổng 3308, mật khẩu chỉ dành cho test nằm trong file này). Đặt TEST_DB_URL, TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver, TEST_DB_USERNAME, TEST_DB_PASSWORD, TEST_DDL_MODE=validate, TEST_DB_INIT_SQL="SET time_zone='+07:00'" rồi chạy `mvnw.cmd '-Dtest=FoundationIntegrationTest,WorkflowIntegrationTest,CatalogIntegrationTest,MySqlTriggerIntegrationTest,MySqlConcurrencyIntegrationTest' test`. Không chạy trên DB vận hành. JAR nằm trong target/quanlydetai-0.0.1-SNAPSHOT.jar.

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

Admin tạo hồ sơ/tài khoản, bổ nhiệm Trưởng khoa và Trưởng bộ môn qua UI. Đề tài mới cần Trưởng bộ môn duyệt trước, Trưởng khoa công bố sau. Trưởng khoa là GV có Dang_Giu_Chuc=true, không phải vai trò mới trong TAI_KHOAN. Admin không mặc nhiên có quyền chấm điểm hoặc đọc báo cáo.

## Source và triển khai

24 entity bảng đang sử dụng, 9 interface Spring Data JPA và JpaEntityRepository cho giao dịch nhiều entity (persist/flush/refresh/khóa ghi). Controller dùng DTO; service kiểm tra vai trò, quan hệ, thời gian và trạng thái. Không chứa hồ sơ/đề tài giả trong code nghiệp vụ. SQL view giữ cho truy vấn DB; đã bỏ lớp view và repository không còn sử dụng.

Trưởng khoa công bố kết quả sau khi chủ tịch tổng hợp. Sinh viên xem điểm và nhận xét trực tiếp tại `/student/results?period=...`, chỉ trong các đợt đã tham gia và chỉ sau công bố. Công bố đồng thời tạo một email riêng cho từng SV trong nhóm; worker gửi sau khi transaction hoàn tất. Trưởng khoa theo dõi và thử lại thư lỗi tại `/dean/result-emails`. Chạy `tools/Start-Local.ps1 -Docker -LocalEmail` để xem email local tại http://localhost:8025; hướng dẫn và kiểm thử SMTP ở [EMAIL_LOCAL_TEST.md](docs/EMAIL_LOCAL_TEST.md).

Local lưu file trong uploads hoặc LOCAL_STORAGE_PATH, tải qua endpoint kiểm tra quyền. Cloud dùng Supabase private, signed URL 300 giây, TLS MySQL VERIFY_IDENTITY với CA; prod luôn tắt bộ dữ liệu thực hành. Cấu hình CI/Render có sẵn, chưa đồng nghĩa cloud đã nghiệm thu.

Các biến môi trường cần cấu hình khi deploy:

| Nhóm | Biến chính |
|---|---|
| MySQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CA_CERT` |
| Supabase Storage | `SUPABASE_URL`, `SUPABASE_KEY`, `SUPABASE_BUCKET` |
| Email Brevo | `EMAIL_ENABLED`, `EMAIL_BACKEND`, `EMAIL_FROM`, `EMAIL_API_KEY` |
| Địa chỉ ứng dụng | `PUBLIC_BASE_URL` |

Đặt giá trị thật trong Environment của hosting. Bật email bằng `EMAIL_ENABLED=true`, chọn `EMAIL_BACKEND=brevo`, dùng người gửi đã xác minh và API key hợp lệ. `PUBLIC_BASE_URL` là URL HTTPS công khai của ứng dụng để liên kết trong email trỏ đúng website. Chi tiết cấu hình và TLS xem [hướng dẫn deploy](docs/HUONG_DAN_DEPLOY_TUNG_BUOC.md).

Git lưu mã nguồn ứng dụng và test, SQL, tài liệu dự án, công cụ hỗ trợ, cấu hình Docker/CI/Render và `.env.example`. Không đưa vào Git các file `.env` thật, secret, uploads, target, truststore, backups, log, kịch bản và biên bản kiểm thử riêng trên máy.

## Kiến trúc và API

Xem [giải thích cấu trúc](docs/ARCHITECTURE.md) và [REST API v1](docs/REST_API.md). Controller HTML và REST dùng chung service; công cụ nhập dữ liệu thực hành chỉ ở test classpath, không được đóng gói vào JAR.

Sơ đồ chuẩn: [ERD 24 bảng](projects/Database/ERD_QuanLyDeTai_V2.drawio), [Use case](projects/Database/USE_CASE_FIT.drawio) và [từ điển CSDL](docs/DATABASE_DICTIONARY.md). Bản source chia sẻ không gồm cấu hình riêng, dữ liệu upload, database dump hoặc kết quả build; xem cách xuất ZIP trong [quy trình làm việc nhóm](docs/GITHUB_TEAMWORK.md).

## Luồng đề tài và GVHD (SQL 09)

GV chỉ đề xuất nội dung → Trưởng bộ môn duyệt và chọn 1–2 GVHD → Trưởng khoa xác nhận và công bố.
Database mới cần chạy SQL 01–10. Database đã có 01–08: dừng web, sao lưu, chạy
`projects/Database/09_department_supervisor_assignment.sql`, sau đó chạy
`projects/Database/10_read_query_indexes.sql` trong Workbench rồi khởi động lại. Database đã có 01–09 cần SQL 10.
Migration giữ dữ liệu và quyết định cũ, không tự điền người/ngày xác nhận lịch sử.
Nếu cần thay danh sách của đề tài nháp đã được duyệt ở bản cũ, người đề xuất sửa lại nháp để
hủy xác nhận và danh sách cũ, sau đó bộ môn duyệt và chọn lại. Không nhập lại dữ liệu mẫu để nâng cấp.
Hướng dẫn chi tiết: `docs/DEPARTMENT_SUPERVISOR_WORKFLOW.md`.

Danh sách chính có phân trang 25 dòng/trang (tối đa 100) và lọc tại DB. Xem [kiến trúc truy vấn](docs/ARCHITECTURE.md#truy-vấn-danh-sách-và-phân-trang) và [nghiệm thu SQL 10](docs/LOCAL_TEST.md#nghiệm-thu-truy-vấn-danh-sách-sql-10) trước khi kiểm tra bản mới. SQL 10 chỉ thêm index, giữ nguyên dữ liệu đã nhập.
