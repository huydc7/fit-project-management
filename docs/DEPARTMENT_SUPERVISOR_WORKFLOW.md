# Luồng đề tài: bộ môn chọn GVHD, Trưởng khoa xác nhận

## Hành vi sau thay đổi

1. GV tạo đề tài tại /lecturer/topics, chỉ nhập đợt, tên, nội dung và bộ môn.
2. Trưởng bộ môn tại /department/topics chọn 1–2 GVHD khác nhau rồi duyệt.
3. Trưởng khoa tại /dean/approvals xem danh sách, bấm Xác nhận GVHD và công bố đề tài.
4. Khi từ chối, không cần chọn GVHD. Từ chối chuyển Huy, không phải trả về sửa.
5. Nếu người đề xuất sửa nháp còn hạn GV, xác nhận bộ môn và danh sách GVHD bị hủy; bộ môn phải chọn lại.
6. Sau công bố không sửa GVHD theo luồng này. Quyền phân công GVPB/hội đồng vẫn thuộc Trưởng khoa.

GV đề xuất không tự động là GVHD. Bộ môn có thể chọn GV khác; không áp đặt GVHD cùng bộ môn nếu đề bài chưa yêu cầu.

## Nâng cấp database đang dùng trên máy

Không chạy bộ test tự động trên database đang dùng. Không nhập lại dữ liệu mẫu để nâng cấp.

1. Dừng website bằng Ctrl+C trong PowerShell.
2. Trong MySQL Workbench, kết nối đúng localhost:3306 của .env.native.
3. Sao lưu schema quanlydetai (Server → Data Export, gồm triggers/routines nếu có).
4. Nếu database đã có SQL 01–08, mở duy nhất file:

   D:/MON HOC/ltW/project_Web/fit-project-management/projects/Database/09_department_supervisor_assignment.sql

5. Chạy toàn bộ file, kiểm tra Action Output không có lỗi. Script dùng USE quanlydetai.
6. Database mới hoàn toàn phải chạy 01–09 theo thứ tự. Docker volume mới đã có mount SQL 09; volume cũ vẫn cần chạy migration.
7. Chạy kiểm tra:

```sql
USE quanlydetai;
SHOW COLUMNS FROM DE_TAI LIKE 'Ma_GV_Xac_Nhan';
SHOW COLUMNS FROM DE_TAI LIKE 'Ngay_Xac_Nhan';
SHOW TRIGGERS WHERE `Table` IN ('DUYET_BO_MON','GVHD_DETAI');
```

8. Khởi động từ thư mục gốc dự án:

```powershell
$env:DB_PASSWORD = Read-Host "Nhap mat khau MySQL"
.\tools\Start-Local.ps1 -EnvFile .env.native
```

Mở http://localhost:8081. Dùng đợt mới có thời gian GV hiện tại và Begin_SV trong tương lai để duyệt và công bố trước Begin_SV.

## Dữ liệu cũ

Migration thêm Ma_GV_Xac_Nhan (FK → GV), Ngay_Xac_Nhan và thay/bổ sung trigger.
Không xóa đề tài, GVHD, xác nhận bộ môn hay kết quả cũ. Hai cột mới của dữ liệu cũ giữ NULL vì không suy đoán lịch sử.
Các quyết định bộ môn cũ tiếp tục được giữ. Nếu muốn chọn lại GVHD cho nháp cũ đã duyệt,
người đề xuất sửa nháp trong hạn để hủy xác nhận và danh sách, sau đó bộ môn chọn lại.
Không thay đổi GVHD của đề tài đã công bố/đang thực hiện/hoàn thành.

## Kiểm thử thủ công

- GV tạo mới không có GVHD; gửi supervisors qua request tạo/sửa bị từ chối.
- Bộ môn chọn 1 hoặc 2 GVHD hợp lệ: thành công; rỗng, trùng, >2, mã không tồn tại: thất bại và không lưu một phần.
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

Mặc định H2. MySQL trigger tests chỉ chạy khi TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver và phải dùng database riêng đã nạp 01–09.
Không đặt TEST_DDL_MODE=create-drop khi trỏ MySQL có dữ liệu cần giữ.
