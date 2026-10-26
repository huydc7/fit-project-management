# Luồng đề tài và duyệt đăng ký nhóm

## Quy trình hiện tại

1. GV tạo đề tài tại /lecturer/topics, chỉ nhập đợt, tên, nội dung và bộ môn.
2. Trưởng bộ môn tại /department/topics chọn 1–2 GVHD khác nhau, có tài khoản hoạt động và thuộc bộ môn mình quản lý rồi duyệt. Danh sách lựa chọn được lọc tại database; HTML và REST đều kiểm tra lại phạm vi khi lưu.
3. Trưởng khoa tại /dean/approvals xem danh sách, bấm Duyệt và công bố đề tài. Bước này xác nhận danh sách GVHD do bộ môn chọn, không chọn lại GVHD.
4. Khi từ chối, không cần chọn GVHD. Từ chối chuyển Huy, không phải trả về sửa.
5. Nếu người đề xuất sửa nháp còn hạn GV, xác nhận bộ môn và danh sách GVHD bị hủy; bộ môn phải chọn lại.
6. Sau công bố không sửa GVHD theo luồng này. Quyền phân công GVPB/hội đồng vẫn thuộc Trưởng khoa.
7. Nhóm trưởng đăng ký đề tài đã công bố cùng đợt trong cửa sổ SV; đăng ký ChoDuyet giữ chỗ cho nhóm và đề tài. Trưởng khoa mở /dean/registrations để chấp thuận/từ chối. GVHD không tự có quyền duyệt nhóm.
8. Chấp thuận đăng ký gán đề tài cho nhóm và chuyển đề tài DangThucHien. Từ chối bắt buộc lý do, giải phóng giữ chỗ và tạo thông tin riêng cho nhóm trưởng từ lịch sử đăng ký. Đăng ký đã xử lý không được quyết định lần hai.

GV đề xuất không tự động là GVHD. Trưởng bộ môn chỉ được chọn GVHD thuộc cùng bộ môn với đề tài và bộ môn mình quản lý. Quy tắc này áp dụng cho phân công mới; các quyết định cũ được giữ. Trưởng khoa vẫn phân công GVPB và hội đồng trong phạm vi toàn khoa theo quy tắc tránh xung đột hiện có.

## Cấu trúc database và môi trường chạy

Bản khôi phục nghiệp vụ dùng nguyên schema SQL 01–10, không thêm/sửa bảng, cột, trigger hoặc view. Database đã đủ 01–10 chỉ cần build/deploy mã ứng dụng; không chạy lại SQL, không xóa volume và không nhập lại demo để cập nhật. Với Docker local đã có cấu hình và database sẵn sàng, từ gốc repository chạy:

```powershell
docker compose --profile app build app
docker compose --profile app up -d --no-deps app
```

Nếu chạy Java trực tiếp, dừng và khởi động lại ứng dụng với cấu hình local đang dùng. Hosting cần deploy source mới để nhận thay đổi; cập nhật file local không tự đổi phiên bản đang chạy trên hosting.

### Database cũ chưa có SQL 09

Các bước dưới đây chỉ là hướng dẫn nâng cấp schema cũ thực sự thiếu migration, không phải yêu cầu của bản khôi phục nghiệp vụ này. Database đã đủ 01–10 bỏ qua phần này.

Không chạy bộ test tự động trên database đang dùng. Không nhập lại dữ liệu mẫu để nâng cấp.

1. Xác định đúng MySQL đang dùng: Docker mặc định `127.0.0.1:3307`; MySQL cài trên máy lấy host/cổng từ `DB_URL` trong `.env.native` (thường là 3306). Hai database độc lập.
2. Dừng website: Java chạy trực tiếp dùng Ctrl+C; Docker dùng `docker compose --profile app stop app`. Giữ MySQL chạy để sao lưu và nâng cấp.
3. Trong MySQL Workbench, kết nối đúng server ở bước 1 và sao lưu schema `quanlydetai` (Server → Data Export, gồm triggers/routines nếu có).
4. Nếu database đã có SQL 01–08, mở duy nhất file `projects/Database/09_department_supervisor_assignment.sql` trong repository đã clone. Không chạy lại script tạo schema.
5. Chạy toàn bộ file, kiểm tra Action Output không có lỗi. Script dùng `USE quanlydetai`; không đổi tên schema kiểm thử rồi nhập nguyên script vào server chứa dữ liệu cần giữ.
6. Database mới hoàn toàn phải chạy 01–10 theo thứ tự. Docker volume mới đã có mount đủ SQL 01–10; volume cũ không tự nạp lại script khởi tạo. Sau SQL 09 có 24 bảng, 38 trigger và 4 view; SQL 10 thêm index. Chỉ áp dụng các migration còn thiếu theo LOCAL_TEST.md.
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

## Dữ liệu cũ và lịch sử migration 09

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
- Mỗi GVHD tối đa 3 đề tài đã được bộ môn duyệt hoặc đã công bố/đang thực hiện/hoàn thành trong cùng đợt; bỏ qua nháp chưa duyệt và đề tài Huy. Cả hai đồng hướng dẫn đều dùng một suất. Duyệt đồng thời không được vượt hạn mức.
- Trưởng khoa xem và quyết định đăng ký nhóm bằng HTML/API; GVHD/GV thường không đồng thời là Trưởng khoa, SV và Admin bị chặn. Từ chối thiếu lý do giữ nguyên trạng thái; chấp thuận lưu đúng người/ngày duyệt; quyết định lần hai bị chặn.

## Kiểm thử tự động

```powershell
cd projects/quanlydetai
.\mvnw.cmd test
```

Mặc định H2. MySQL trigger tests chỉ chạy khi TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver. Dùng MySQL kiểm thử ở container/server riêng đã nạp SQL 01–10 với schema `quanlydetai`; xem [kiểm thử MySQL](LOCAL_TEST.md). Không dùng server chứa database đang vận hành.
Không đặt TEST_DDL_MODE=create-drop khi trỏ MySQL có dữ liệu cần giữ.

## Giao diện duyệt nhóm hiện tại

Trưởng khoa duyệt đăng ký nhóm tại `/dean/registrations`; POST quyết định tới
`/dean/registrations/{id}/review`. Chỉ Trưởng khoa đương nhiệm được xem danh sách
và quyết định, kể cả khi người gọi là GVHD của đề tài. API giữ GET
`/api/v1/registrations/pending` và PATCH `/api/v1/registrations/{id}/review`.
Từ chối bắt buộc lý do không trắng, tối đa 1.000 ký tự; đăng ký đã xử lý không được quyết định lần hai. Backend lấy mã người duyệt từ phiên xác thực, không nhận mã người duyệt do client gửi. Đăng ký nộp đúng hạn có thể được duyệt sau hạn SV.

Giao diện mới giữ bảng quản lý đề tài, cửa sổ chi tiết, danh sách thành viên nhóm/hội đồng,
bộ lọc gọn và hiển thị họ tên. Bộ môn chọn GVHD bằng checkbox có tìm kiếm, tối đa hai người.
Trang Duyệt & công bố chỉ hiển thị đề tài/kết quả chờ xử lý và có liên kết tới trang duyệt nhóm riêng.

Khôi phục hai luồng nghiệp vụ này chỉ thay mã ứng dụng. Database đã đủ SQL 01–10
không cần nâng cấp, không nhập lại mẫu và không thay đổi phân công/quyết định đã có.
Nháp có GVHD từ phiên bản tự gán trước đó chỉ được thay danh sách khi bộ môn thực hiện
duyệt theo lựa chọn mới, hoặc bị xóa danh sách khi người đề xuất sửa nháp đúng quy trình.
