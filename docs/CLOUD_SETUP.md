# Hướng dẫn tạo dịch vụ cloud

Cập nhật email và migration ngày 06/10/2026. Nhóm chưa tạo tài khoản cloud; cấu hình trong repository là bản chuẩn bị, chưa có website online được xác nhận. Bắt đầu bằng [hướng dẫn tạo tài khoản và deploy từng bước](HUONG_DAN_DEPLOY_TUNG_BUOC.md), gồm Aiven, Supabase, Brevo và Render.

## Lựa chọn hosting và câu hỏi Vercel

Dự án là Spring Boot + Thymeleaf: Java dựng HTML phía server. Docker image đóng gói cả giao diện và backend. Không thể chỉ đưa thư mục template lên static hosting để có website hoạt động.

Vercel hiện có Container Images Beta trên các plan và có thể chạy OCI image có HTTP server. Java không nằm trong danh sách runtime Functions chính thức, nhưng có đường triển khai container. Tuy nhiên Functions container tự scale down sau 5 phút không có traffic ở production, nên Vercel không bảo đảm mục tiêu backend luôn chạy. Functions còn có giới hạn body 4,5 MB, trong khi kế hoạch upload báo cáo của nhóm là <20 MiB qua backend. Nếu chọn Vercel phải thiết kế lại upload trực tiếp có xác thực và lưu session dùng chung giữa các instance; đây không phải đổi tên hosting là xong.

Khuyến nghị cho bản hiện tại: dùng Docker trên Render Free nếu yêu cầu bắt buộc hosting miễn phí; chấp nhận cold start và diễn tập trước báo cáo. Nếu ưu tiên không ngủ, dùng paid compute của Render hoặc VPS chạy container liên tục. Chỉ nâng cấp khi nhóm đã thống nhất ngân sách. Không cần tách frontend sang React/Next.js để hoàn thành đồ án.

Nguồn: [Vercel Container Images](https://vercel.com/docs/functions/container-images), [Vercel Functions limits](https://vercel.com/docs/functions/limitations), [Render Free](https://render.com/docs/free).

## Bước 1 Tạo Aiven MySQL

1. Đăng ký/đăng nhập Aiven Console bằng tài khoản nhóm được phép quản lý. Tạo project rồi tạo dịch vụ MySQL. Kiểm tra Free tier còn được cung cấp cho tài khoản/region; không chọn trial trả phí nhầm với Free.
2. Free tier không cho chọn cloud/region cụ thể. Lưu service host, port, username, password và tải CA certificate từ trang kết nối. Không gửi password lên chat/GitHub.
3. Kết nối bằng MySQL Workbench hoặc mysql CLI với TLS và CA của Aiven. Tạo database `quanlydetai`, chạy lần lượt các script 01–06 trên DB mới. Script có DELIMITER nên dùng công cụ hỗ trợ MySQL script.
4. Kiểm tra đủ 22 bảng, 35 trigger, 4 view và procedure liên quan. Nếu thiếu quyền CREATE ROUTINE/TRIGGER, xử lý quyền/cấu hình dịch vụ trước khi deploy app; không bỏ qua script lỗi.
5. Tạo admin bằng BCrypt theo README; nhập hồ sơ cần thiết qua UI. Tài khoản runtime nên được cấp quyền dữ liệu cần thiết trên database; không dùng tài khoản migration để vận hành lâu dài.
6. Trên Render, tạo biến `DB_CA_CERT` chứa toàn bộ nội dung CA PEM tải từ Aiven, gồm cả dòng BEGIN/END CERTIFICATE. Entry point Docker tạo truststore tạm riêng cho JDBC từ chứng thư này; chứng thư CA không phải mật khẩu. Profile prod yêu cầu VERIFY_IDENTITY để xác minh cả CA và hostname. Không sửa DB_URL để tắt hoặc hạ mức SSL.

DB_URL mẫu, thay HOST và PORT:

```text
jdbc:mysql://HOST:PORT/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh
```

Nếu chạy JAR production trực tiếp ngoài Docker, tạo PKCS12 truststore từ CA bằng keytool, rồi đặt DB_TRUSTSTORE_URL=file:/đường/dẫn/aiven-truststore.p12 và DB_TRUSTSTORE_PASSWORD. Không thay truststore HTTPS toàn JVM vì Supabase vẫn cần hệ CA công khai.

Nguồn: [Aiven MySQL](https://aiven.io/docs/products/mysql/get-started), [Aiven TLS](https://aiven.io/docs/platform/concepts/tls-ssl-certificates), [MySQL Connector/J SSL](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).

## Bước 2 Tạo Supabase Storage

1. Tạo Supabase project; database PostgreSQL kèm project không thay thế MySQL của ứng dụng.
2. Trong Storage tạo bucket `bao-cao-de-tai`, chọn private. Giới hạn bucket 20 MiB; cho phép application/pdf, application/zip và application/vnd.openxmlformats-officedocument.wordprocessingml.document.
3. Lấy Project URL. Adapter hiện dùng legacy service_role JWT cho header Authorization Bearer và apikey; lấy trong API Keys/legacy keys của project. Không dùng anon/publishable key thay service role, không đưa service_role xuống frontend.
4. Lưu SUPABASE_URL, SUPABASE_KEY, SUPABASE_BUCKET vào dashboard hosting. Không đặt chúng trong HTML hoặc commit `.env`.
5. Luồng báo cáo đã tích hợp; sau khi cấu hình cloud, thử upload, ký URL, tải trước/sau hết hạn và kiểm tra người sai quyền. Test local hiện dùng HTTP giả lập, chưa chứng minh bucket thật đã cấu hình đúng.

Nguồn: [Supabase Storage quickstart](https://supabase.com/docs/guides/storage/quickstart), [Private buckets](https://supabase.com/docs/guides/storage/buckets/fundamentals).

## Bước 3 Triển khai Render

1. Đăng nhập Render, kết nối GitHub repo nhóm. Review và push phần code này trước; hiện chưa push tự động.
2. Tạo Blueprint từ `render.yaml`, hoặc Web Service runtime Docker. Build context là root repository, Dockerfile là `./Dockerfile`.
3. Điền SPRING_PROFILES_ACTIVE=prod, DB_URL, DB_USERNAME, DB_PASSWORD, DB_CA_CERT, SUPABASE_URL, SUPABASE_KEY và SUPABASE_BUCKET, STORAGE_BACKEND=supabase. JAR không chứa bộ nhập dữ liệu mẫu. Cấu hình truststore như bước 1. Không để thông tin mật khẩu trong DB_URL nếu có biến riêng.
4. Blueprint đang để plan free và autoDeployTrigger=checksPass; CI cần đạt trước khi tự deploy. Nếu chọn paid compute để không ngủ, sửa plan theo lựa chọn trong dashboard và cập nhật Blueprint tránh lệch cấu hình.
5. Kiểm tra `/actuator/health`, đăng nhập bằng dữ liệu thật, tạo đợt và xem log khi lỗi. ddl-auto=validate sẽ dừng ứng dụng nếu schema lệch.
6. Thử restart/redeploy: dữ liệu MySQL và object Supabase phải còn. Session đang nằm trong bộ nhớ một instance, nên người dùng cần đăng nhập lại sau restart. Nếu scale nhiều instance, bổ sung session store dùng chung trước.
7. Ghi URL demo, commit deploy, thời điểm kiểm tra vào phụ lục báo cáo. Sao lưu MySQL và báo cáo trước buổi demo; thử phục hồi vào môi trường riêng.

Render Free ngủ sau 15 phút không có traffic và có thể cần khoảng một phút để khởi động lại. Paid service compute loại bỏ hạn chế free này; nâng workspace plan không tự nâng compute. Không dùng cron ping làm điều kiện bảo đảm demo.

Nguồn: [Render Docker](https://render.com/docs/docker), [Blueprint](https://render.com/docs/blueprint-spec), [Render Free](https://render.com/docs/free).

## Điều kiện trước khi kết luận deploy hoàn tất

- URL HTTPS truy cập từ máy khác; đăng nhập và phân quyền đúng.
- Kết nối Aiven có xác minh TLS, schema/trigger đầy đủ và seed sử dụng được.
- Bucket private; không có secret/public URL báo cáo trong repo hoặc HTML.
- Luồng đã triển khai chạy sau restart; có README để thành viên khác tái tạo.
- Ghi rõ hạn chế của plan được chọn và các luồng ứng dụng còn đang phát triển.
