# Ma trận thao tác đối chiếu rubric

Kiểm tra source ngày 06/10/2026. “Có” chỉ tính khi có controller/service cho người dùng thực hiện, không tính khả năng tự sửa bảng trong Workbench. Khóa tài khoản, duyệt đề tài hoặc rút đăng ký không đồng nghĩa đã có toàn bộ CRUD của danh mục.

| Đối tượng | Tạo | Xem danh sách | Sửa hồ sơ/nội dung | Xóa | Tìm kiếm/lọc chuyên biệt |
|---|---|---|---|---|---|
| Đề tài | Có: GV đề xuất | Có: theo vai trò | Chưa có sửa nội dung | Chưa có xóa; từ chối chuyển Huy | Chưa có từ khóa/bộ lọc danh sách |
| Đợt đăng ký | Có: Trưởng khoa | Có | Chưa có | Chưa có | Chưa có |
| Giảng viên | Có qua tạo tài khoản GV | Hồ sơ trong danh sách tài khoản/lựa chọn | Chưa có sửa họ tên/email/bộ môn | Chưa có; có khóa tài khoản | Chưa có tìm theo tên/mã |
| Sinh viên | Có qua tạo tài khoản SV | Hồ sơ trong danh sách tài khoản | Chưa có sửa họ tên/email | Chưa có; có khóa tài khoản | Chưa có tìm theo tên/mã |
| Bộ môn | Có: Admin | Có trong danh mục chọn | Chưa có | Chưa có | Chưa có |
| Thông báo | Có theo phạm vi vai trò | Có | Chưa có | Chưa có | Chưa có |
| Nhóm/thành viên | Có: trưởng nhóm, mời/xác nhận | Có theo người tham gia | Có thay thành viên trước khi khóa; chưa đổi tên nhóm | Có bỏ thành viên hợp lệ; không xóa toàn lịch sử | Theo thành viên hiện tại |
| Đăng ký đề tài | Có | Có theo quyền | Có duyệt/từ chối/rút theo trạng thái | Rút giải phóng giữ chỗ, giữ lịch sử | Theo nhóm/trạng thái chờ duyệt |
| Kết quả | Có tổng hợp/công bố | Có theo quyền | Điểm bị khóa sau tổng hợp | Không xóa kết quả đã công bố | Có lọc đợt của SV |
| Email kết quả | Tự tạo khi công bố | 100 thư gần nhất cho Trưởng khoa | Thử lại khi FAILED | Không có xóa trên UI | Chưa có bộ lọc/phân trang |

## Nguồn kiểm tra

- `controller/DeTaiController.java`, `service/DeTaiService.java` và REST controller tương ứng.
- `controller/DotDangKyController.java`, `service/DotDangKyService.java`.
- `controller/TaiKhoanController.java`, `service/TaiKhoanService.java`, `ReferenceDataService.java`.
- `controller/ThongBaoController.java`, `service/ThongBaoService.java`.
- `NhomService`, `DiemService`, `ResultEmailService` và các template hiện có.
- Các endpoint cụ thể nằm trong `REST_API.md`. Giao diện hiện không có tìm kiếm phía JavaScript để thay cho các ô “chưa có” trong bảng.

## Thứ tự khắc phục đề xuất

1. **Tìm kiếm và sửa nội dung:** tìm đề tài theo tên/mã/đợt/bộ môn; tìm hồ sơ theo mã/tên/email; sửa thông tin liên hệ, tên bộ môn và thông báo. Quyền phải được kiểm tra ở service cho cả form và REST.
2. **Sửa đợt và đề tài theo trạng thái:** GV chỉ sửa đề xuất của mình trước khi duyệt; sửa thời gian đợt phải xét các đăng ký đã có. Không dùng sửa tự do để vô tình làm đăng ký hợp lệ thành ngoài hạn.
3. **Xóa có kiểm soát:** chỉ cho xóa dữ liệu nháp/chưa tham chiếu khi phù hợp yêu cầu; dữ liệu đã gắn nhóm/điểm/báo cáo cần giữ lịch sử. Hiển thị lý do không được xóa, không dùng cascade xóa toàn bộ.
4. **Trưởng bộ môn:** xác nhận nhiệm vụ, phạm vi bộ môn và quan hệ với Trưởng khoa trước khi thiết kế quyền. Chưa tự cấp thêm quyền học thuật khi chưa có mô tả.
5. **Bằng chứng nghiệm thu:** mỗi thao tác bổ sung cần ca đúng quyền, sai quyền, lỗi dữ liệu và trường hợp bị khóa bởi trạng thái/FK. Cập nhật bảng này theo source thực tế sau khi triển khai.

Ma trận này ghi rõ khoảng trống để nhóm lập kế hoạch; không phải tuyên bố đã đạt toàn bộ tiêu chí CRUD của rubric. Phần được hoàn thành trong lượt tiếp tục local là kiểm chứng SMTP, đồng bộ ERD/từ điển/báo cáo và cung cấp quy trình thử lại được.
