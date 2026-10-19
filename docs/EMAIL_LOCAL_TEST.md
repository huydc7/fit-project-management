# Kiểm thử email công bố kết quả local

Đã khôi phục theo yêu cầu Huy ngày 18/10/2026. Công bố ghi kết quả và hàng đợi cho từng SV trong cùng transaction; worker gửi riêng từng người qua JavaMailSender SMTP. Mailpit là hộp thư kiểm thử nhận SMTP thật, không phát thư ra Internet.

## Chạy cùng website

Tại root `D:\project_web\final_projects`:

```powershell
.\tools\Start-Local.ps1 -Docker -LocalEmail
```

Website http://localhost:8080; hộp thư http://localhost:8025. MySQL và báo cáo giữ volume hiện có. Switch này bật EMAIL_ENABLED, chọn SMTP local và xóa credential email bên ngoài trong môi trường tiến trình; không sửa `.env`. Khi khởi động lần sau cần giữ `-LocalEmail` để tiếp tục bật gửi.

Nếu chạy Java với MySQL trên máy:

```powershell
.\tools\Start-Local.ps1 -EnvFile .env.native -LocalEmail
```

Docker Desktop phải chạy để khởi động hộp thư; Java vẫn chạy bằng Maven trên máy. Điền PUBLIC_BASE_URL đúng URL/PORT của web trong `.env.native`, ví dụ http://localhost:8081. Không ghi đè file cấu hình hiện có.

## Nghiệm thu thủ công

1. Chuẩn bị nhóm 3–5 SV, đúng email từng hồ sơ. Hoàn tất duyệt hai bước, duyệt nhóm, nộp báo cáo, chấm và tổng hợp. Trước công bố, SV chưa thấy điểm.
2. Đăng nhập Trưởng khoa → trang Duyệt, phân công và công bố → công bố kết quả một lần.
3. Mở **Theo dõi email kết quả** (`/dean/result-emails`): tìm theo mã SV/mã đề tài/email, lọc trạng thái và phân trang. Với đề tài vừa công bố, số bản ghi bằng số thành viên; chờ worker khoảng 15 giây khi không có hàng đợi tồn.
4. Mailpit hiển thị từng thư riêng, một To, không CC/BCC; kiểm tra tên, đề tài, đợt, điểm và link. Mỗi SV đăng nhập theo link để tra cứu đúng đợt. SV ngoài nhóm/đợt bị chặn.
5. Công bố lặp phải báo đã công bố và không tạo bản ghi mới. Khi vận chuyển lỗi, thư thử lại tối đa 5 lần rồi FAILED; Trưởng khoa có nút thử gửi lại sau khi sửa cấu hình.

Nếu EMAIL_ENABLED=false, website vẫn công bố điểm và lưu thư PENDING; đây chưa phải gửi thành công. ACCEPTED là máy chủ nhận thư đã chấp nhận, không phải xác nhận Inbox/đã đọc. Nội dung thư là snapshot khi công bố; đặt URL đúng trước bước này. Kết quả công bố từ trước không tự được gửi lại; hàng đợi PENDING/PROCESSING cũ có thể được xử lý khi bật worker.

## Bộ kiểm thử SMTP riêng

```powershell
.\tools\Test-LocalEmail.ps1
# Nếu đã có cache Maven trên máy:
.\tools\Test-LocalEmail.ps1 -MavenRepository D:/project_web/.m2 -Offline
```

Mailpit riêng tại http://localhost:18025 (SMTP 11025), dữ liệu học vụ H2 trong bộ nhớ, địa chỉ `@example.test`; không dùng database của nhóm. Ba ca kiểm tra: vận chuyển ba thư riêng tiếng Việt/không gửi lại thư ACCEPTED; rollback không phát thư; gọi DiemService.publish thực sự commit rồi tạo/gửi đủ ba thư, chặn công bố lặp. WorkflowIntegrationTest còn kiểm tra form/API, quyền và tra cứu điểm theo đợt trên H2/MySQL riêng.

```powershell
docker compose -f compose.test.yaml stop mailpit-test
```

Khi triển khai thật sau này: cấu hình SMTP có quyền gửi hoặc Brevo HTTPS, sender đã xác minh, PUBLIC_BASE_URL là URL HTTPS của web; kiểm chứng Inbox/Spam từng người nhận. Cấu hình mặc định giữ EMAIL_ENABLED=false cho tới khi hoàn tất. Không commit secret.

REST dùng cùng quyền Trưởng khoa: GET `/api/v1/result-emails?q=&state=FAILED&page=0&size=20`, POST `/api/v1/result-emails/{id}/retry` kèm CSRF. Không trả nội dung thư qua danh sách. Tham số và phản hồi xem [REST_API.md](REST_API.md).
