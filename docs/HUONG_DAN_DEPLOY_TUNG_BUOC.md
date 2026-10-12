# Tạo tài khoản và đưa đồ án FIT lên mạng

Cập nhật 06/10/2026. Dành cho Huy và nhóm chưa có tài khoản cloud. Tên mục trên dashboard có thể thay đổi; các đường dẫn tài liệu chính thức được đặt ngay tại bước tương ứng. Đây là hướng dẫn thực hiện, chưa phải bằng chứng đã deploy.

## 1. Chuẩn bị và thứ tự thực hiện

| Dịch vụ | Dùng để làm gì | Thông tin đưa vào Render |
|---|---|---|
| Aiven MySQL | Tài khoản web, đề tài, nhóm, điểm, hàng đợi email | DB_URL, DB_USERNAME, DB_PASSWORD, DB_CA_CERT |
| Supabase Storage | File báo cáo trong bucket private | SUPABASE_URL, SUPABASE_KEY, SUPABASE_BUCKET |
| Brevo | Email thông báo điểm tới từng sinh viên | EMAIL_FROM, EMAIL_API_KEY |
| Render | Chạy Docker chứa Spring Boot và giao diện Thymeleaf | Các biến trên, profile prod và URL web |

Docker là cách đóng gói ứng dụng; Render là nơi chạy gói đó. Không phải lựa chọn một trong hai. Supabase PostgreSQL đi kèm project không thay thế MySQL của đồ án.

Làm theo thứ tự: kiểm thử local → Aiven → Supabase → Brevo → Render → nghiệm thu HTTPS. Có thể tạo tài khoản Render trước, nhưng chỉ tạo web service sau khi đã có database và bucket.

Chuẩn bị GitHub có quyền vào repository nhóm, email bạn kiểm soát để xác minh các dịch vụ, MySQL Workbench và mật khẩu riêng cho từng dịch vụ. Lưu secret trong trình quản lý mật khẩu hoặc dashboard; không gửi chúng vào chat, chụp lên báo cáo hoặc commit GitHub. Chọn Free khi có lựa chọn, kiểm tra trang xác nhận chi phí trước khi tạo dịch vụ. Nếu không thấy Free, dừng ở bước đó để kiểm tra thay vì tự chọn gói trả phí.

## 2. Kiểm tra local trước

DB mới chạy SQL 01 → 02 → 03 → 04 → 05 → 06 trong `projects/Database`. DB đã có 01–05 chỉ cần nâng cấp 06; không chạy lại bộ seed để xử lý nâng cấp.

Nếu dùng MySQL Docker của dự án, script sau dừng app, sao lưu toàn DB vào `backups`, rồi chạy migration 06. Nó không xóa thành viên, báo cáo hay điểm:

```powershell
cd D:\project_web\final_projects
.\tools\Update-RubricDatabase.ps1
.\tools\Start-Local.ps1 -Docker
```

Nếu dùng MySQL cài trên Windows: dừng tiến trình Spring Boot, xuất backup bằng Workbench, mở `06_rubric_email_groups.sql` trên đúng connection và chạy toàn bộ script. Sau đó chạy lại `Start-Local.ps1 -EnvFile .env.native`. Không dùng `ddl-auto=create/update` để thay migration.

Thử email bằng Mailpit, không gửi ra hộp thư thật:

```powershell
docker compose --profile mail up -d mailpit
```

Thêm/sửa vào `.env` đang dùng, không copy đè toàn file:

```dotenv
EMAIL_ENABLED=true
EMAIL_BACKEND=smtp
EMAIL_FROM=fit@example.test
PUBLIC_BASE_URL=http://localhost:8080
MAIL_HOST=localhost
MAIL_PORT=1025
```

Chạy lại app. Compose tự dùng hostname `mailpit`; Maven native dùng `localhost`. Nếu web native chạy cổng 8081, sửa PUBLIC_BASE_URL tương ứng. Mở [hộp thư local](http://localhost:8025), thực hiện công bố điểm của một nhóm mới. Mỗi thành viên phải có một thư riêng. Trưởng khoa xem hàng đợi tại `/dean/result-emails`; thời gian chờ bình thường khoảng một chu kỳ 15 giây. Mailpit là máy chủ SMTP nhận thư thật trong local, nhưng không chuyển chúng tới Gmail. Đây là bằng chứng kiểm thử, chưa phải nghiệm thu email online.

Nếu không dùng Docker cho bất kỳ thành phần nào, tải Mailpit bản Windows từ [hướng dẫn cài đặt chính thức](https://mailpit.axllent.org/docs/install/), giải nén và chạy `mailpit.exe`; giữ MAIL_HOST=localhost. Không cần cài một mail server công cộng.

Email đã công bố trước migration không được tự gửi lại. Khi đổi từ Mailpit sang Brevo, kiểm tra hàng đợi PENDING/FAILED trước khi bật gửi để không phát các thư thử còn tồn. Không đưa các địa chỉ `example.test` vào kịch bản gửi thật.

## 3. Aiven: tạo MySQL và nạp cấu trúc

### 3.1 Tạo dịch vụ

1. Vào [Aiven Console](https://console.aiven.io/), đăng ký bằng email/GitHub và hoàn tất xác minh.
2. Trong project, vào Services → Create service → MySQL. Chọn tier **Free**, đặt tên gợi nhớ như `fit-projects-mysql` và kiểm tra tóm tắt chi phí trước khi Create.
3. Chờ trạng thái Running. Mở Overview → Quick connect để lấy host, port, user và mật khẩu; tải CA certificate của dịch vụ.

Free không cho chọn cloud/region cụ thể. Gói này hiện có 1 GB lưu trữ, không giới hạn thời gian sử dụng nhưng có thể bị tắt khi không hoạt động. Vì vậy cần truy cập kiểm tra trước buổi báo cáo. [Tạo Aiven MySQL](https://aiven.io/docs/products/mysql/get-started), [giới hạn Free](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier).

### 3.2 Kết nối bằng MySQL Workbench

Tạo connection mới, đặt tên `FIT-Aiven`, phương thức Standard TCP/IP. Điền **đúng host và port trong Quick connect**; port cloud không nhất thiết là 3306. Username ban đầu thường là `avnadmin`, nhưng dùng giá trị dashboard thực tế. Store in Vault để lưu mật khẩu trên máy.

Tab SSL: chọn xác minh chứng thư và hostname nếu Workbench cung cấp lựa chọn đó, chọn file CA tải từ Aiven ở SSL CA File. Test Connection rồi mở connection. Đối chiếu các thông số với [hướng dẫn kết nối Aiven](https://aiven.io/docs/products/mysql/howto/connect-from-mysql-workbench). Lỗi chứng thư cần kiểm tra host/CA, không giải quyết bằng cách tắt TLS.

Trên **database cloud mới**, mở và chạy toàn bộ từng file theo thứ tự:

```text
01_schema_mysql.sql
02_logic_mysql.sql
03_views_seed_mysql.sql
04_workflow_mysql.sql
05_department_catalog_mysql.sql
06_rubric_email_groups.sql
```

Kiểm tra Action Output sau mỗi file. Script có DELIMITER nên chạy toàn bộ bằng Workbench hoặc mysql CLI hỗ trợ script, không dán từng đoạn thân trigger. Nếu gặp lỗi quyền CREATE ROUTINE/TRIGGER hoặc binary logging, kiểm tra cấu hình/quyền của Aiven trước khi tiếp tục; không bỏ các trigger để cho app chạy.

Xác nhận cấu trúc:

```sql
SELECT COUNT(*) AS base_tables FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='quanlydetai' AND TABLE_TYPE='BASE TABLE'; -- 22
SELECT COUNT(*) AS triggers_count FROM information_schema.TRIGGERS
 WHERE TRIGGER_SCHEMA='quanlydetai'; -- 35
SELECT COUNT(*) AS views_count FROM information_schema.VIEWS
 WHERE TABLE_SCHEMA='quanlydetai'; -- 4
```

Tạo service user riêng cho web trong Aiven, cấp quyền dữ liệu cần thiết cho `quanlydetai` và kiểm tra đăng nhập bằng user đó; giữ tài khoản quản lý để chạy migration. Xem [quản lý user Aiven](https://aiven.io/docs/products/mysql/howto/manage-service-users). Không dùng mật khẩu MySQL local cho cloud.

### 3.3 Lưu các giá trị để điền Render

```text
DB_URL=jdbc:mysql://HOST_AIVEN:PORT_AIVEN/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=user_cua_web
DB_PASSWORD=mat_khau_user_cua_web
DB_CA_CERT=toàn bộ nội dung file CA PEM, gồm BEGIN và END CERTIFICATE
```

DB_URL không chứa password. Profile prod và Docker entrypoint đã xử lý truststore riêng cho MySQL, xác minh CA/hostname bằng VERIFY_IDENTITY. Không cần tự điền DB_TRUSTSTORE_URL khi dùng Docker trên Render.

## 4. Supabase: nơi lưu file báo cáo

1. Vào [Supabase Dashboard](https://supabase.com/dashboard), đăng ký/đăng nhập, tạo organization Free và New project. Đặt tên `fit-projects`, chọn region gần Render nếu được; lưu mật khẩu database mà Supabase yêu cầu riêng với mật khẩu Aiven.
2. Chờ project sẵn sàng. Storage → New bucket, nhập `bao-cao-de-tai`. **Tắt Public bucket**.
3. Trong bucket settings, giới hạn 20 MiB và các MIME: `application/pdf`, `application/zip`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`.
4. Lấy Project URL từ Connect hoặc Project Settings/API. Lấy legacy `service_role` JWT trong API keys → Legacy anon, service_role API keys để khớp adapter hiện tại. Không lấy anon/publishable key.
5. Lưu SUPABASE_URL, SUPABASE_KEY và SUPABASE_BUCKET. Project URL có dạng `https://PROJECT_REF.supabase.co`; không nối thêm `/storage/v1`.

Adapter hiện dùng legacy JWT trong Authorization và apikey. Supabase đang chuyển sang secret keys mới; không thay bằng `sb_secret_...` khi chưa cập nhật và kiểm thử adapter. Hai kiểu key hiện cùng tồn tại. Đây là việc cần nâng cấp trước khi vận hành dài hạn. [Storage quickstart](https://supabase.com/docs/guides/storage/quickstart), [API keys](https://supabase.com/docs/guides/getting-started/api-keys), [lộ trình chuyển key](https://supabase.com/docs/guides/getting-started/migrating-to-new-api-keys).

Không cần tạo bảng đề tài trong Supabase, không cần public bucket, không cần mở quyền đọc toàn bucket cho sinh viên. Backend kiểm tra quyền trước khi cấp URL tải có thời hạn.

## 5. Brevo: gửi email công bố điểm

### 5.1 Xác minh tài khoản và địa chỉ gửi

1. Đăng ký tại [Brevo](https://www.brevo.com/), chọn Free và xác minh email. Khai thông tin thực tế của nhóm/dự án theo yêu cầu dịch vụ.
2. Vào Settings → Senders, Domains & Dedicated IPs → Senders → Add a sender. Đặt tên gửi thể hiện đây là đồ án FIT và nhập địa chỉ email **bạn sở hữu**.
3. Mở email xác minh, hoàn tất xác nhận sender. Kiểm tra mục Transactional của tài khoản; nếu chưa được kích hoạt hoặc cần xét duyệt, làm theo hướng dẫn Brevo trước khi thử gửi.

Không tự đặt email trường như `noreply@hcmute.edu.vn` nếu không quản lý địa chỉ đó. Nếu dùng Gmail cá nhân cho demo, xác minh sender theo dashboard; Brevo có thể thay địa chỉ gửi bằng địa chỉ do họ quản lý khi domain chưa được xác thực. Khi vận hành trong khoa, dùng domain được cấp quyền và cấu hình DKIM/DMARC theo hướng dẫn nhà cung cấp. [Tạo sender](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email), [quy định sender/domain](https://help.brevo.com/hc/en-us/articles/14925263522578-Comply-with-Gmail-Yahoo-and-Microsoft-s-requirements-for-email-senders).

### 5.2 Lấy API key

Account → Settings → SMTP & API → API Keys & MCP → Generate a new API key. Xác minh bằng mã gửi tới email nếu được yêu cầu, đặt tên `fit-results-render`, chọn thời hạn còn hiệu lực qua ngày báo cáo, tạo và lưu key ngay. Chọn **API key thông thường**, không chọn SMTP key hoặc MCP key. [Hướng dẫn API key](https://help.brevo.com/hc/en-us/articles/209467485-Create-and-manage-your-API-keys).

Biến cần điền Render:

```dotenv
EMAIL_BACKEND=brevo
EMAIL_FROM=dia_chi_sender_da_xac_minh
EMAIL_API_KEY=api_key_vua_tao
EMAIL_ENABLED=false
```

Để false tới khi web chạy, URL đúng và địa chỉ nhận thử thuộc nhóm. Sau đó bật true ở bước nghiệm thu. Gói Free hiện có 300 lượt gửi/ngày; không phải phù hợp gửi đồng thời kết quả cho toàn khoa quy mô lớn. [Giới hạn Brevo Free](https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan).

Ứng dụng gọi HTTPS API của Brevo. Render Free chặn cổng SMTP 25/465/587 nên không dùng Gmail SMTP làm cấu hình chính trên Render. [Render Free](https://render.com/docs/free), [API transactional email Brevo](https://developers.brevo.com/docs/send-a-transactional-email).

## 6. Render: chạy website

### 6.1 Chuẩn bị GitHub

Các thay đổi trên máy phải được nhóm review, commit và push vào nhánh sẽ deploy. Render chỉ lấy code đã push; nó không đọc thư mục D: của bạn. Kiểm tra GitHub Actions pass. Không đưa `.env*` chứa secret, thư mục `backups`, `uploads` hoặc `target` lên repository.

### 6.2 Tạo web service bằng dashboard

1. Vào [Render Dashboard](https://dashboard.render.com/), đăng ký bằng GitHub và cấp quyền cho repository nhóm.
2. New → Web Service → Git Provider → chọn repo.
3. Điền bảng sau. Dùng **Web Service**, vì Spring Boot dựng cả HTML và xử lý nghiệp vụ.

| Mục | Giá trị cho repository này |
|---|---|
| Name | `fit-projects` hoặc tên chưa được dùng |
| Branch | Nhánh chứa bản nhóm đã kiểm thử |
| Language/Runtime | Docker |
| Root Directory | Để trống, dùng gốc repository có Dockerfile |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Để trống, Dockerfile đã có entrypoint |
| Instance Type | Free |
| Health Check Path | `/actuator/health` |

Không đặt Root Directory thành `projects/quanlydetai`, vì Dockerfile hiện COPY theo đường dẫn từ gốc repository. [Tạo Web Service](https://render.com/docs/web-services), [Docker trên Render](https://render.com/docs/docker), [Root directory](https://render.com/docs/monorepo-support).

### 6.3 Điền Environment Variables

| Key | Value |
|---|---|
| SPRING_PROFILES_ACTIVE | `prod` |
| DB_URL / DB_USERNAME / DB_PASSWORD | Các giá trị Aiven ở bước 3 |
| DB_CA_CERT | Nội dung CA PEM Aiven, giữ đúng xuống dòng |
| STORAGE_BACKEND | `supabase` |
| SUPABASE_URL / SUPABASE_KEY | Bước 4 |
| SUPABASE_BUCKET | `bao-cao-de-tai` |
| EMAIL_BACKEND | `brevo` |
| EMAIL_FROM / EMAIL_API_KEY | Bước 5 |
| EMAIL_ENABLED | `false` lúc triển khai lần đầu |
| PUBLIC_BASE_URL | URL HTTPS thật do Render cấp; cập nhật sau lần tạo service |

Nếu chưa biết URL, có thể tạm bỏ PUBLIC_BASE_URL khi EMAIL_ENABLED=false; sau khi service được tạo, sao chép chính xác URL trên dashboard rồi lưu biến này. Không để link localhost trong email online. Không thêm dấu nháy bao quanh giá trị trên dashboard. PORT để Render cung cấp; Spring đọc sẵn biến này.

Create Web Service và theo dõi build/deploy logs. Dấu hiệu sẵn sàng: trạng thái Live, mở `/actuator/health` trả `UP`, `/login` tải được. `ddl-auto=validate` sẽ ngăn app chạy nếu thiếu bảng, kể cả `EMAIL_KET_QUA`.

Nhóm cũng có thể dùng New → Blueprint với `render.yaml` thay cho các bước nhập service thủ công; **chỉ chọn một cách** để không tạo hai service trùng. Blueprint có EMAIL_ENABLED=false; nếu dùng Blueprint, sau khi bật gửi thật cũng cập nhật giá trị đó trong bản cấu hình đã review để lần sync tiếp theo không tắt lại.

### 6.4 Tạo admin đầu tiên trên cloud

Không có mật khẩu admin hardcode. Không dùng nguyên database mẫu local chứa địa chỉ giả làm dữ liệu gửi thật. Tạo hash từ terminal PowerShell tương tác trên máy:

```powershell
cd D:\project_web\final_projects\projects\quanlydetai
.\mvnw.cmd -q dependency:build-classpath '-Dmdep.outputFile=target/runtime-classpath.txt' -DincludeScope=runtime
java --class-path (Get-Content target/runtime-classpath.txt -Raw).Trim() ../../tools/PasswordHash.java
```

Nhập mật khẩu mới 12–64 ký tự không dấu khi được hỏi. Sao chép BCrypt in ra, rồi trên Workbench connection **Aiven** chạy một lần:

```sql
USE quanlydetai;
INSERT INTO TAI_KHOAN (Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Trang_Thai)
VALUES ('ADMIN01','admin','THAY_BANG_CHUOI_BCRYPT','Admin','HoatDong');
```

Đăng nhập web bằng `admin` và mật khẩu bạn vừa nhập. Admin tạo tài khoản/hồ sơ GV, SV, bổ nhiệm một GV làm Trưởng khoa. Dùng địa chỉ nhận do các thành viên nhóm kiểm soát để thử email. Tài khoản Aiven/Render/Supabase/Brevo và tài khoản admin của web là các tài khoản khác nhau.

## 7. Nghiệm thu online và lấy bằng chứng báo cáo

1. Tạo dữ liệu demo phù hợp FIT, một nhóm 3 SV đã xác nhận. Tạo/duyệt đề tài, duyệt đăng ký, phân công GVPB/hội đồng và nộp báo cáo.
2. Upload PDF lên web; xác nhận object nằm trong bucket private. SV khác nhóm không tải được báo cáo.
3. Nhập đủ điểm, tổng hợp. Trước công bố, SV chưa thấy điểm.
4. Kiểm tra EMAIL_FROM đã verified, EMAIL_API_KEY hợp lệ và PUBLIC_BASE_URL đúng; bật EMAIL_ENABLED=true trên Render, Save/redeploy và đăng nhập lại.
5. Trưởng khoa công bố **một lần**. Kiểm tra `/dean/result-emails`: đúng số thư bằng số thành viên; mỗi thư một người nhận.
6. Kiểm tra Brevo Transactional logs, rồi mở Inbox/Spam của từng tài khoản nhận. Giữ ảnh bằng chứng có che thông tin không cần thiết. “Dịch vụ đã nhận” trong web chưa đủ để kết luận thư đã phát.
7. SV mở link email, đăng nhập, xem điểm theo đợt. SV không thuộc đợt không xem được dữ liệu bằng cách sửa URL.
8. Redeploy/restart web: dữ liệu MySQL và báo cáo phải còn. Phiên đăng nhập trong RAM mất khi app khởi động lại; đăng nhập lại là hành vi hiện tại.
9. Mở URL bằng thiết bị/mạng khác. Ghi URL HTTPS, commit đã deploy, thời điểm và người kiểm tra vào Word. Không dùng ảnh kiểm thử local làm bằng chứng email/cloud đã hoạt động.

Render Free ngủ sau 15 phút không có traffic và cần thời gian khởi động lại. Hàng đợi email được lưu trong MySQL, nhưng worker chỉ xử lý khi app đang chạy. Mở web trước giờ báo cáo khoảng 10 phút và kiểm tra toàn luồng cần demo. Khi vận hành thật trong khoa, cần dịch vụ luôn chạy, theo dõi lỗi và chính sách sao lưu phù hợp. [Giới hạn Render](https://render.com/docs/free).

## 8. Khi có lỗi

| Hiện tượng | Kiểm tra trước |
|---|---|
| Build không thấy Dockerfile/COPY thất bại | Repo/branch đã push chưa; root directory có đang để trống không |
| Schema-validation missing table EMAIL_KET_QUA | Chạy migration 06 trên đúng Aiven DB |
| Communications link failure | Aiven Running, đúng host/port, quyền truy cập mạng |
| Access denied MySQL | Runtime user, mật khẩu, quyền trên quanlydetai |
| PKIX / hostname verification failed | Nội dung CA và host khớp service Aiven; không tắt SSL |
| Upload 401/403 | Đúng legacy service_role, Project URL, bucket private tồn tại |
| Đăng nhập lại sau mỗi deploy | Session hiện lưu trong RAM; đây là giới hạn đã biết |
| Email chờ mãi | EMAIL_ENABLED, trạng thái Render, kết nối DB; worker không chạy khi service ngủ |
| Email FAILED | API key/sender/account activation/quota; xem logs dịch vụ, sửa rồi Trưởng khoa bấm Thử gửi lại |
| Email ACCEPTED nhưng chưa đến | Brevo logs xem Delivered/Bounced/Blocked, kiểm tra Spam và địa chỉ SV |
| Link thư mở localhost | Sửa PUBLIC_BASE_URL trước công bố mới; thư trong queue giữ nội dung snapshot lúc tạo |

Worker tự thử tối đa 5 lần, chờ tăng dần khi thất bại. Nếu dịch vụ đã nhận nhưng phản hồi bị mất hoặc app dừng trước lúc ghi nhận, thử lại có thể tạo thư trùng. Bản này không tuyên bố gửi đúng một lần tuyệt đối; nút công bố lặp không tạo thêm bản ghi queue nhờ giao dịch và khóa UNIQUE.

## 9. Việc bạn có thể làm ngay

Tạo Aiven Free và kết nối Workbench trước. Sau đó tạo Supabase bucket và Brevo sender/API key theo các bước trên. Khi xong chỉ cần cho biết bước đã hoàn tất hoặc nội dung lỗi đã che secret. Không gửi mật khẩu, API key, file `.env` hoặc ảnh dashboard chưa che thông tin nhạy cảm.
