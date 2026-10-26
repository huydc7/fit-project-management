# Luồng đề tài: người đề xuất là GVHD, GVHD duyệt nhóm

## Quy trình hiện tại

1. GV tạo đề tài tại `/lecturer/topics`, nhập đợt, tên, nội dung và bộ môn của mình.
2. Backend lấy mã GV từ phiên đăng nhập, tự lưu phân công GVHD cùng đề tài. Nếu vượt hạn mức 3 đề tài trong đợt thì không lưu một phần. Nháp chưa hủy cũng tính hạn mức.
3. Trưởng bộ môn tại `/department/topics` chỉ duyệt nội dung chuyên môn và chuyển Trưởng khoa. Không nhập danh sách GVHD, không thêm đồng hướng dẫn. GVHD duy nhất là người đề xuất, có tài khoản đang hoạt động và cùng bộ môn.
4. Trưởng khoa tại `/dean/approvals` xác nhận và công bố trước Begin_SV.
5. Nhóm trưởng đăng ký đề tài trong thời gian SV. GVHD vào **Duyệt đăng ký nhóm** (`/lecturer/registrations`) để chấp thuận/từ chối. GVHD là người đề xuất quyết định; quyết định sau bị chặn vì đã xử lý. Trưởng khoa không được duyệt nếu không phải GVHD của đề tài.
6. Từ chối nhóm bắt buộc lý do; vẫn giữ cơ chế giữ chỗ, rút đăng ký và thông báo riêng cho nhóm trưởng. Đăng ký gửi đúng hạn có thể duyệt sau hạn SV.
7. Sửa nháp trong thời gian GV hủy duyệt bộ môn, giữ người đề xuất là GVHD duy nhất. Sau công bố không sửa GVHD theo luồng này. Phân công GVPB/hội đồng vẫn thuộc Trưởng khoa.

Bản cập nhật dùng schema SQL 01–10 hiện có, không thêm bảng/cột/trigger và không cần migration mới nếu database đã đủ 01–10. Các bước dưới đây chỉ dành cho database cũ chưa có SQL 09; sau đó chạy SQL 10 theo LOCAL_TEST.md.

## Nâng cấp database đang dùng trên máy

Không chạy bộ test tự động trên database đang dùng. Không nhập lại dữ liệu mẫu để nâng cấp.

1. Xác định đúng MySQL đang dùng: Docker mặc định `127.0.0.1:3307`; MySQL cài trên máy lấy host/cổng từ `DB_URL` trong `.env.native` (thường là 3306). Hai database độc lập.
2. Dừng website: Java chạy trực tiếp dùng Ctrl+C; Docker dùng `docker compose --profile app stop app`. Giữ MySQL chạy để sao lưu và nâng cấp.
3. Trong MySQL Workbench, kết nối đúng server ở bước 1 và sao lưu schema `quanlydetai` (Server → Data Export, gồm triggers/routines nếu có).
4. Nếu database đã có SQL 01–08, mở duy nhất file `projects/Database/09_department_supervisor_assignment.sql` trong repository đã clone. Không chạy lại script tạo schema.
5. Chạy toàn bộ file, kiểm tra Action Output không có lỗi. Script dùng `USE quanlydetai`; không đổi tên schema kiểm thử rồi nhập nguyên script vào server chứa dữ liệu cần giữ.
6. Database mới hoàn toàn phải chạy 01–09 theo thứ tự. Docker volume mới đã có mount SQL 09; volume cũ vẫn cần chạy migration. Sau SQL 09 có 24 bảng, 38 trigger và 4 view.
7. Chạy kiểm tra:

```sql
USE quanlydetai;
SHOW COLUMNS FROM DE_TAI LIKE 'Ma_GV_Xac_Nhan';
SHOW COLUMNS FROM DE_TAI LIKE 'Ngay_Xac_Nhan';
SHOW TRIGGERS WHERE `Table` IN ('DUYET_BO_MON','GVHD_DETAI');
```

8. Khởi động từ thư mục gốc dự án bằng một trong hai cách phù hợp:

```powershell
# MySQL cài trên máy, cấu hình DB_URL/DB_USERNAME/DB_PASSWORD trong .env.native:
.\tools\Start-Local.ps1 -EnvFile .env.native

# Hoặc website và MySQL Docker:
.\tools\Start-Local.ps1 -Docker
```

Mở `http://localhost:<PORT>` theo file env đã chọn (mặc định 8080). Không ghi đè file env đang dùng. Dùng đợt mới có thời gian GV hiện tại và Begin_SV trong tương lai để duyệt và công bố trước Begin_SV.

## Dữ liệu đã có

Không tự thay GVHD hoặc người duyệt trong lịch sử cũ. Quy tắc tự gán áp dụng khi tạo đề tài mới. Đề tài cũ chưa công bố được sửa trong thời gian GV sẽ gán lại người đề xuất; đề tài đã công bố vẫn giữ phân công đã có. Không xóa database hoặc nhập lại demo để cập nhật source.

## Kiểm thử

- Tạo HTML/API tự có đúng GVHD là người đăng nhập; gửi supervisors tùy ý bị từ chối.
- Tạo đề tài thứ tư trong đợt bị từ chối và không để lại đề tài thiếu GVHD; hủy một đề tài giải phóng suất. Đợt khác có hạn mức riêng.
- Duyệt chuyên môn chỉ gửi quyết định accept. Gửi supervisors qua HTML/API bị từ chối, kể cả gửi lại đúng người đề xuất. Không ghi duyệt hoặc thay phân công khi request không hợp lệ.
- Sửa nháp giữ người đề xuất là GVHD duy nhất, xóa duyệt chuyên môn cũ; phải duyệt lại trước công bố.
- GVHD xem và duyệt đăng ký của mình; GV khác/Trưởng khoa không hướng dẫn/SV/Admin không được duyệt bằng URL hoặc API giả mạo.
- Sau chấp thuận, lưu đúng Ma_GV_Duyet và chuyển đề tài DangThucHien; từ chối lưu lý do và giải phóng giữ chỗ. Quyết định lần hai bị chặn.
- Danh sách phân trang, hội đồng theo đề tài, phản hồi lỗi cạnh lý do và form thời gian loại đợt vẫn hoạt động.

Chạy `mvnw.cmd verify` bằng Java 21; kiểm thử MySQL trên container riêng trong compose.test.yaml. Không chạy test trên database vận hành.

Nháp cũ chưa được bộ môn duyệt có danh sách GVHD khác quy tắc mới sẽ được chuẩn hóa về đúng người đề xuất khi duyệt. Không thay phân công của đề tài đã công bố hoặc đã có quyết định duyệt cũ.
