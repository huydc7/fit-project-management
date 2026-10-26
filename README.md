# Hệ thống quản lý đề tài khoa CNTT

Ứng dụng quản lý quy trình đề tài sinh viên của Khoa CNTT, từ đề xuất, đăng ký nhóm và nộp báo cáo đến chấm điểm, công bố kết quả. Hệ thống dùng Java 21, Spring Boot 4.1.1, Spring Data JPA/Hibernate, Spring Security, Thymeleaf, HTML/CSS, JavaScript và MySQL.

## Chức năng chính

- Admin quản lý tài khoản, hồ sơ, bộ môn, bổ nhiệm chức vụ và thông báo chung.
- Trưởng khoa tạo đợt đăng ký, xác nhận đề tài, phân công đánh giá và công bố kết quả. Lịch GVPB phải sau hạn đăng ký SV; với KLTN, hạn GVPB phải trước ngày báo cáo hội đồng.
- Giảng viên đề xuất được tự động gán làm GVHD. Người đề xuất là GVHD duy nhất; Trưởng bộ môn chỉ duyệt chuyên môn và chuyển Trưởng khoa. Mỗi GV hướng dẫn tối đa 3 đề tài trong một đợt, kể cả nháp chưa hủy.
- Sinh viên tạo nhóm, mời và xác nhận thành viên; mỗi SV chỉ thuộc một nhóm trong mỗi đợt. Nhóm trưởng được sửa tên nhóm trong thời gian đăng ký, trước khi đăng ký chờ duyệt hoặc được chấp thuận. Nhóm cần 3–5 SV khi đăng ký, có đúng một nhóm trưởng đăng ký và nộp báo cáo.
- GVHD duyệt đăng ký nhóm tại **Duyệt đăng ký nhóm**; chỉ xem và quyết định đăng ký của đề tài mình hướng dẫn. Trưởng khoa không có quyền duyệt nếu không đồng thời là GVHD. Từ chối đăng ký nhóm bắt buộc có lý do. Lý do hiển thị riêng trong Thông báo và trang chủ của đúng nhóm trưởng, không đưa vào thông báo chung.
- Lập hội đồng chọn đề tài trước; danh sách chủ tịch, thư ký và thành viên loại GVHD/GVPB của đề tài đó. Tạo, chốt và gán hội đồng trong cùng một transaction.
- Báo cáo PDF/DOCX/ZIP có lịch sử phiên bản và kiểm tra quyền tải. Chỉ cho chấm điểm sau khi phân công đủ GVPB và hội đồng đã chốt; từ phiếu điểm đầu tiên, phân công và phiên bản báo cáo bị khóa. Chủ tịch tổng hợp, Trưởng khoa công bố. Sinh viên chỉ tra cứu kết quả nhóm mình theo đợt đã tham gia sau công bố.
- Công bố kết quả tạo email riêng cho từng SV; có hàng đợi, theo dõi và thử lại thư lỗi. Thông báo chung và riêng được tìm kiếm, phân trang và sắp xếp mới nhất trước.

Các chức năng trên dùng cấu trúc database SQL 01–10; không cần thêm bảng/cột cho giới hạn GVHD, thông báo riêng hoặc lựa chọn hội đồng. Quy ước giao diện xem [docs/UI_DESIGN.md](docs/UI_DESIGN.md).

Bắt đầu tại [hướng dẫn local và kiểm thử](docs/LOCAL_TEST.md). Xem [danh mục và dữ liệu thực hành](docs/SAMPLE_DATA.md), [quy tắc nghiệp vụ](docs/PRODUCT_RULES.md), [kiến trúc](docs/ARCHITECTURE.md) và [cloud](docs/CLOUD_SETUP.md). Nếu chưa có tài khoản dịch vụ, làm theo [hướng dẫn deploy từng bước](docs/HUONG_DAN_DEPLOY_TUNG_BUOC.md).

Đưa source lên GitHub, mời thành viên và review qua Pull Request: [quy trình làm việc nhóm](docs/GITHUB_TEAMWORK.md).

Email có tìm/lọc/phân trang và REST; danh sách đề tài tải dữ liệu liên quan theo tập mã để tránh truy vấn lặp theo từng dòng. Xem [thiết kế xử lý và giới hạn](docs/ARCHITECTURE.md).

Thời hạn duyệt trước giai đoạn SV, sửa hội đồng và gia hạn đợt được mô tả trong [quy tắc nghiệp vụ](docs/PRODUCT_RULES.md). Database mới chạy SQL 01–10; database đã có dữ liệu nâng cấp theo [hướng dẫn local](docs/LOCAL_TEST.md).

## Luồng đề tài và điều kiện khóa

| Bước | Người thực hiện | Trạng thái và điều kiện |
|---|---|---|
| Đề xuất | Giảng viên | Tạo trong thời gian GV đăng ký, đề tài ở `Nhap`, người đề xuất được gán GVHD trong cùng transaction |
| Duyệt bộ môn | Trưởng bộ môn | Duyệt chuyên môn, giữ người đề xuất là GVHD duy nhất; đề tài vẫn `Nhap` |
| Công bố đề tài | Trưởng khoa | Bộ môn đã duyệt, hoàn tất trước giai đoạn SV; chuyển `DaCongBo` |
| Đăng ký nhóm | Nhóm trưởng, sau đó GVHD của đề tài duyệt | Nhóm đủ 3–5 SV; khi chấp thuận, đề tài chuyển `DangThucHien` |
| Chuẩn bị chấm | Trưởng khoa và nhóm trưởng | Phân công một GVPB, gán hội đồng đã chốt và nộp báo cáo; các việc này có thể làm trước/sau nhau |
| Chấm và tổng hợp | GVPB, hội đồng, chủ tịch | Chỉ lưu điểm khi đủ phân công và báo cáo. Chủ tịch tổng hợp khi đủ mọi phiếu; đề tài thành `HoanThanh`, kết quả `ChuaCongBo` |
| Công bố kết quả | Trưởng khoa | Kết quả chuyển `DaCongBo`; SV của đúng nhóm được tra cứu và nhận email riêng |

Phiếu điểm đầu tiên khóa phân công và phiên bản báo cáo dùng để chấm. Yêu cầu chấm bị từ chối do thiếu phân công không tạo phiếu hoặc khóa báo cáo. Sau khi hoàn tất phân công, đề tài tiếp tục chấm bình thường. Điểm cuối là trung bình cộng từng phiếu, cùng trọng số, làm tròn hai chữ số; ví dụ 9, 6, 6, 6 cho 6,75.

Nhóm trưởng sửa tên tại **Nhóm của tôi → Chỉnh sửa tên nhóm**, tối đa 150 ký tự và không để trắng. Chỉ sửa khi còn thời gian SV và nhóm chưa có đăng ký chờ duyệt/chưa được chấp thuận; sau từ chối hoặc rút đăng ký có thể sửa lại nếu còn hạn. Giữ nguyên mã nhóm, đợt và nhóm trưởng. Các quy tắc này được kiểm tra ở service dùng chung cho HTML/API, dùng schema SQL 01–10 hiện có và không cần migration mới khi database đã đủ 01–10.

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

Test hồi quy bao gồm hai trường hợp chấm khi chỉ có GVPB hoặc chỉ có hội đồng, bổ sung phân công rồi hoàn tất công bố, và đổi tên nhóm theo quyền/trạng thái/thời gian. Kiểm thử MockMvc hoặc gửi HTTP trực tiếp không thay thế kiểm thử thao tác và hiển thị trong trình duyệt.

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

24 entity bảng đang sử dụng, 10 interface Spring Data JPA và JpaEntityRepository cho giao dịch nhiều entity (persist/flush/refresh/khóa ghi). Controller dùng DTO; service kiểm tra vai trò, quan hệ, thời gian và trạng thái. Không chứa hồ sơ/đề tài giả trong code nghiệp vụ. SQL view giữ cho truy vấn DB; đã bỏ lớp view và repository không còn sử dụng.

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

`SUPABASE_URL` là URL gốc của project dạng `https://<project-ref>.supabase.co`, không kèm `/rest/v1/` hoặc `/storage/v1/`. Các giá trị trong Environment của Render được hosting truyền vào container; không cần đưa file `.env` thật lên GitHub để deploy.

Git lưu mã nguồn ứng dụng và test, SQL, tài liệu dự án, công cụ hỗ trợ, cấu hình Docker/CI/Render và `.env.example`. Không đưa vào Git các file `.env` thật, secret, uploads, target, truststore, backups, log, kịch bản và biên bản kiểm thử riêng trên máy.

## Kiến trúc và API

Xem [giải thích cấu trúc](docs/ARCHITECTURE.md) và [REST API v1](docs/REST_API.md). Controller HTML và REST dùng chung service; công cụ nhập dữ liệu thực hành chỉ ở test classpath, không được đóng gói vào JAR.

Sơ đồ chuẩn: [ERD 24 bảng](projects/Database/ERD_QuanLyDeTai_V2.drawio), [Use case](projects/Database/USE_CASE_FIT.drawio) và [từ điển CSDL](docs/DATABASE_DICTIONARY.md). Bản source chia sẻ không gồm cấu hình riêng, dữ liệu upload, database dump hoặc kết quả build; xem cách xuất ZIP trong [quy trình làm việc nhóm](docs/GITHUB_TEAMWORK.md).

## Luồng đề tài và GVHD (SQL 09)

GV đề xuất và tự động nhận vai trò GVHD → Trưởng bộ môn duyệt chuyên môn và chuyển Trưởng khoa → Trưởng khoa công bố → Nhóm trưởng đăng ký → GVHD duyệt nhận nhóm.
Database mới cần chạy SQL 01–10. Database đã có 01–08: dừng web, sao lưu, chạy
`projects/Database/09_department_supervisor_assignment.sql`, sau đó chạy
`projects/Database/10_read_query_indexes.sql` trong Workbench rồi khởi động lại. Database đã có 01–09 cần SQL 10.
Migration giữ dữ liệu và quyết định cũ, không tự điền người/ngày xác nhận lịch sử.
Nếu cần thay danh sách của đề tài nháp đã được duyệt ở bản cũ, người đề xuất sửa lại nháp để
hủy xác nhận và danh sách cũ, sau đó bộ môn duyệt và chọn lại. Không nhập lại dữ liệu mẫu để nâng cấp.
Hướng dẫn chi tiết: `docs/DEPARTMENT_SUPERVISOR_WORKFLOW.md`.

Danh sách chính có phân trang 25 dòng/trang (tối đa 100) và lọc tại DB. Xem [kiến trúc truy vấn](docs/ARCHITECTURE.md#truy-vấn-danh-sách-và-phân-trang) và [nghiệm thu SQL 10](docs/LOCAL_TEST.md#nghiệm-thu-truy-vấn-danh-sách-sql-10) trước khi kiểm tra bản mới. SQL 10 chỉ thêm index, giữ nguyên dữ liệu đã nhập.

## Luồng GVHD duyệt nhóm

Giảng viên đề xuất tại `/lecturer/topics` chỉ nhập đợt, tên, nội dung và bộ môn của mình; không gửi danh sách GVHD từ form/API. Backend lấy mã GV từ phiên đăng nhập và lưu đề tài cùng phân công GVHD trong một transaction. Sửa nháp hủy duyệt bộ môn nhưng giữ người đề xuất là GVHD duy nhất.

GVHD xem `/lecturer/registrations` hoặc GET `/api/v1/registrations/pending` và chấp thuận/từ chối đúng đề tài mình hướng dẫn. POST HTML chuyển sang `/lecturer/registrations/{id}/review`; PATCH API giữ `/api/v1/registrations/{id}/review`. Backend kiểm tra quan hệ GVHD, không chỉ quyền GV. GVHD là người đề xuất quyết định; đăng ký đã xử lý không được quyết định lần hai.

Bản cập nhật dùng schema SQL 01–10 hiện có, không cần migration mới. Database đang dùng giữ nguyên đề tài, phân công và quyết định cũ; không tự gán lại GVHD cho lịch sử đã công bố. Chỉ đồng bộ source chưa cập nhật ứng dụng Docker hoặc Render: cần build/deploy bản mới để sử dụng luồng này.

## Quản lý đề tài và bộ lọc



Mục **Quản lý đề tài** tách khỏi form đề xuất, hiển thị danh sách dạng bảng và phân trang. Nút **Xem chi tiết** mở cửa sổ nhỏ có nội dung, thông tin và thao tác theo quyền; đóng bằng nút Đóng, Esc hoặc bấm nền ngoài. Trưởng khoa xem toàn khoa tại `/dean/topics`; Trưởng bộ môn xem bộ môn phụ trách tại `/department/topic-management`; giảng viên xem đề tài đề xuất/hướng dẫn tại `/lecturer/topic-management`. Quyền được kiểm tra ở cả URL và service; các bộ lọc chỉ thu hẹp phạm vi.

Lọc theo đợt đăng ký, bộ môn, loại, trạng thái và từ khóa. Nhãn luôn hiển thị trên ô chọn; nút Tìm kiếm tải kết quả từ database, Đặt lại xóa các điều kiện. Chuyển trang giữ bộ lọc. Không thêm trường năm học/học kỳ giả vì database hiện chỉ quản lý đợt.

Góc tài khoản và chân sidebar hiển thị họ tên hồ sơ SV/GV. Admin không có hồ sơ cá nhân nên hiển thị Quản trị viên. Tên được làm mới theo phiên xác thực ở request tiếp theo, không đổi username dùng đăng nhập; tên được escape khi hiển thị. Menu dùng mũi tên SVG và có trạng thái focus/mở rõ ràng. CSS/JS có phiên bản mới để trình duyệt tải lại giao diện.

Trang Đề xuất đề tài chỉ có form tạo mới. Danh sách và thao tác sửa/xóa nằm trong Quản lý đề tài; chỉnh sửa dùng trang riêng, vẫn kiểm tra người đề xuất, trạng thái và thời hạn ở backend.

Trang **Duyệt & công bố** (`/dean/approvals`) chỉ hiển thị đề tài đã được Trưởng bộ môn duyệt đang chờ Trưởng khoa xác nhận và kết quả đã tổng hợp chưa công bố. Sau khi xử lý, mục đó rời danh sách chờ; dữ liệu vẫn còn trong hệ thống. Phân công GVPB/hội đồng chuyển vào chi tiết đề tài Đang thực hiện tại **Quản lý đề tài**.
