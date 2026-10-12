# Kiểm chứng email bằng SMTP local

Không cần tài khoản cloud. Mailpit nhận thư qua SMTP và hiển thị hộp thư trên máy; không chuyển thư tới người nhận ngoài Internet.

## Chạy trên Windows

Mở Docker Desktop, chờ engine hoạt động. Máy cần JDK 21 trở lên để chạy Maven Wrapper. Trong PowerShell:

```powershell
cd D:\project_web\final_projects
.\tools\Test-LocalEmail.ps1
```

Lần đầu cần Internet để tải image và dependency. Nếu máy đang dùng Maven cache riêng của workspace:

```powershell
.\tools\Test-LocalEmail.ps1 -MavenRepository D:/project_web/.m2
```

Script chỉ khởi động `mailpit-test` trong `compose.test.yaml`, không khởi động MySQL kiểm thử. SMTP dùng loopback cổng **11025**, giao diện hộp thư dùng **18025**. Nếu cổng đang được phần mềm khác sử dụng, kiểm tra ứng dụng chiếm cổng trước khi chạy lại.

## Kết quả mong đợi

- Maven báo **Tests run: 2, Failures: 0, Errors: 0, Skipped: 0**.
- Mở [hộp thư Mailpit](http://localhost:18025): mỗi lượt kiểm thử thành công thêm ba thư cho ba địa chỉ giả lập `@example.test`, mỗi thư chỉ có một người nhận.
- Nội dung có tiếng Việt, điểm 8,25 và liên kết tra cứu theo đợt kiểm thử. Liên kết này thuộc dữ liệu kiểm thử, không phải một kết quả tồn tại trên database đang chạy của nhóm.
- Chạy worker lần nữa trong cùng ca kiểm thử không gửi thêm thư đã được xác nhận. Giao dịch tạo hàng đợi bị rollback không phát thư.

Mailpit được giữ chạy để xem thư. Dừng sau khi xem:

```powershell
docker compose -f compose.test.yaml stop mailpit-test
```

## Phạm vi và dữ liệu

`MailpitDeliveryIT` dùng H2 riêng trong bộ nhớ, chèn bản ghi email giả lập và gọi worker thật qua `JavaMailSender` tới SMTP Mailpit. Không đọc mật khẩu tài khoản nhóm, không sửa `.env`, không bật gửi email trên app chính, không kết nối MySQL đang lưu dữ liệu của nhóm.

Hai ca này kiểm chứng hàng đợi → worker → SMTP, nội dung thư, người nhận và rollback. Việc công bố điểm tạo đúng hàng đợi được kiểm tra riêng trong `WorkflowIntegrationTest`. Chưa coi đây là một lần thao tác trình duyệt công bố điểm rồi nhận thư ngoài Internet; cũng chưa nghiệm thu Brevo/cloud hay khả năng phục hồi hàng đợi qua restart.

Hậu tố `IT` chỉ được chạy khi chỉ định `-Dtest=MailpitDeliveryIT`; bộ `mvnw verify` mặc định không tự chạy ca cần Mailpit. Khi chạy trực tiếp mà Mailpit không sẵn sàng, ca kiểm thử thất bại, không bỏ qua để báo đạt.

Đừng nhầm với profile `mail` của `compose.yaml` dùng cổng 1025/8025 để thử ứng dụng thủ công. Hướng dẫn này sử dụng hộp thư kiểm thử riêng, không cần đổi cấu hình ứng dụng chính.

Kết quả ngày 06/10/2026: **2/2 đạt**, Mailpit nhận ba thư; nhật ký tại [VERIFY_HUY.md](VERIFY_HUY.md). API đối chiếu nội dung thư theo [tài liệu Mailpit](https://mailpit.axllent.org/docs/api-v1/).
