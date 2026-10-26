# Quy ước URL

Đường dẫn giao diện và API dùng tiếng Anh, chữ thường và dấu gạch nối khi cần. Nhãn menu, nội dung và thông báo cho người dùng vẫn bằng tiếng Việt. Tên class Java, tên template và schema database giữ quy ước hiện có.

| Trang | URL hiện tại |
|---|---|
| Tổng quan | `/` |
| Đăng nhập | `/login` |
| Thông báo | `/announcements` |
| Đổi mật khẩu | `/accounts/password` |
| Thông tin cá nhân (SV/GV) | `/accounts/profile` |
| Quản lý tài khoản | `/admin/accounts` |
| Nhóm của sinh viên | `/student/groups` |
| Đề tài đã công bố | `/student/topics` |
| Nộp báo cáo | `/student/reports` |
| Kết quả của sinh viên | `/student/results` |
| Đề xuất đề tài của giảng viên | `/lecturer/topics` |
| Quản lý đề tài toàn khoa (Trưởng khoa) | `/dean/topics` |
| Quản lý đề tài thuộc bộ môn (Trưởng bộ môn) | `/department/topic-management` |
| Quản lý đề tài đã đề xuất/hướng dẫn (GV) | `/lecturer/topic-management` |
| Sửa nháp của người đề xuất | `/lecturer/topics/{id}/edit` |
| Duyệt đề tài của bộ môn | `/department/topics` |
| Duyệt đăng ký đề tài của nhóm (Trưởng khoa) | `/dean/registrations` |
| Báo cáo được phân công | `/lecturer/reports` |
| Chấm điểm | `/lecturer/grading` |
| Theo dõi email kết quả | `/dean/result-emails` |
| Sửa hội đồng đang lập | `/dean/councils/{id}/edit` |
| Quản lý đợt đăng ký | `/dean/periods` |
| Duyệt và công bố | `/dean/approvals` |
| Hội đồng | `/dean/councils` |

API dùng cùng từ vựng: `/api/v1/topics`, `/api/v1/groups`, `/api/v1/reports`, `/api/v1/announcements`, `/api/v1/admin/accounts`... Xem REST_API.md để biết toàn bộ endpoint và cách gửi session/CSRF.

Trưởng bộ môn chọn 1–2 GVHD và duyệt chuyên môn bằng POST `/department/topics/{id}/review`. Trưởng khoa xác nhận/công bố đề tài bằng POST `/dean/topics/{id}/review`, duyệt đăng ký nhóm bằng POST `/dean/registrations/{id}/review`. Giao diện và API cùng kiểm tra quyền tại service; vai trò GVHD không tự cấp quyền duyệt nhóm. Form Đề xuất đề tài chỉ tạo mới, danh sách và thao tác sửa/xóa ở Quản lý đề tài.

Các URL trang tiếng Việt trước đây được giữ dưới dạng **GET redirect 301** đến trang tiếng Anh, với kiểm tra quyền tương ứng. Form và API sử dụng URL tiếng Anh; client/Postman cũ cần cập nhật theo REST_API.md. Đăng nhập, đăng xuất và cơ chế CSRF giữ nguyên.
