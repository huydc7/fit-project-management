# Luồng đề tài: bộ môn chọn GVHD, Trưởng khoa xác nhận

## Hành vi sau thay đổi

1. GV tạo đề tài tại /lecturer/topics, chỉ nhập đợt, tên, nội dung và bộ môn.
2. Trưởng bộ môn tại /department/topics chọn 1–2 GVHD khác nhau, có tài khoản hoạt động và thuộc bộ môn mình quản lý rồi duyệt. Danh sách lựa chọn được lọc tại database; HTML và REST đều kiểm tra lại phạm vi khi lưu.
3. Trưởng khoa tại /dean/approvals xem danh sách, bấm Xác nhận GVHD và công bố đề tài.
4. Khi từ chối, không cần chọn GVHD. Từ chối chuyển Huy, không phải trả về sửa.
5. Nếu người đề xuất sửa nháp còn hạn GV, xác nhận bộ môn và danh sách GVHD bị hủy; bộ môn phải chọn lại.
6. Sau công bố không sửa GVHD theo luồng này. Quyền phân công GVPB/hội đồng vẫn thuộc Trưởng khoa.

GV đề xuất không tự động là GVHD. Trưởng bộ môn chỉ được chọn GVHD thuộc cùng bộ môn với đề tài và bộ môn mình quản lý. Quy tắc này áp dụng cho phân công mới; các quyết định cũ được giữ. Trưởng khoa vẫn phân công GVPB và hội đồng trong phạm vi toàn khoa theo quy tắc tránh xung đột hiện có.

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

## Dữ liệu cũ

Migration thêm Ma_GV_Xac_Nhan (FK → GV), Ngay_Xac_Nhan và thay/bổ sung trigger.
Không xóa đề tài, GVHD, xác nhận bộ môn hay kết quả cũ. Hai cột mới của dữ liệu cũ giữ NULL vì không suy đoán lịch sử.
Các quyết định bộ môn cũ tiếp tục được giữ. Nếu muốn chọn lại GVHD cho nháp cũ đã duyệt,
người đề xuất sửa nháp trong hạn để hủy xác nhận và danh sách, sau đó bộ môn chọn lại.
Không thay đổi GVHD của đề tài đã công bố/đang thực hiện/hoàn thành.

## Kiểm thử thủ công

- GV tạo mới không có GVHD; gửi supervisors qua request tạo/sửa bị từ chối.
- Bộ môn chọn 1 hoặc 2 GVHD hợp lệ cùng bộ môn: thành công; rỗng, trùng, >2, mã không tồn tại, khác bộ môn hoặc tài khoản không hoạt động: thất bại và không lưu một phần.
- Bộ môn khác không được duyệt; Trưởng khoa không công bố trước duyệt bộ môn.
- Nội dung sửa sau duyệt: không còn DUYET_BO_MON/GVHD_DETAI; phải duyệt lại.
- Trưởng khoa công bố: GVHD không đổi, DE_TAI có Ma_GV_Xac_Nhan và Ngay_Xac_Nhan.
- Quá hạn Begin_SV: bộ môn và khoa không duyệt/công bố mới.
- Sau công bố: không sửa nội dung/GVHD; GVHD không được phản biện/chấm cùng đề tài.
- Từ chối nháp chưa có GVHD vẫn hoạt động.

## Kiểm thử tự động

```powershell
cd projects/quanlydetai
.\mvnw.cmd test
```

Mặc định H2. MySQL trigger tests chỉ chạy khi TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver. Dùng MySQL kiểm thử ở container/server riêng đã nạp SQL 01–09 với schema `quanlydetai`; xem [kiểm thử MySQL](LOCAL_TEST.md). Không dùng server chứa database đang vận hành.
Không đặt TEST_DDL_MODE=create-drop khi trỏ MySQL có dữ liệu cần giữ.
