# Quy ước URL

Đường dẫn giao diện và API dùng tiếng Anh, chữ thường và dấu gạch nối khi cần. Nhãn menu, nội dung và thông báo cho người dùng vẫn bằng tiếng Việt. Tên class Java, tên template và schema database giữ quy ước hiện có.

| Trang | URL hiện tại |
|---|---|
| Tổng quan | `/` |
| Đăng nhập | `/login` |
| Thông báo | `/announcements` |
| Đổi mật khẩu | `/accounts/password` |
| Quản lý tài khoản | `/admin/accounts` |
| Nhóm của sinh viên | `/student/groups` |
| Đề tài đã công bố | `/student/topics` |
| Nộp báo cáo | `/student/reports` |
| Kết quả của sinh viên | `/student/results` |
| Đề xuất đề tài của giảng viên | `/lecturer/topics` |
| Báo cáo được phân công | `/lecturer/reports` |
| Chấm điểm | `/lecturer/grading` |
| Quản lý đợt đăng ký | `/dean/periods` |
| Duyệt và công bố | `/dean/approvals` |
| Hội đồng | `/dean/councils` |

API dùng cùng từ vựng: `/api/v1/topics`, `/api/v1/groups`, `/api/v1/reports`, `/api/v1/announcements`, `/api/v1/admin/accounts`... Xem REST_API.md để biết toàn bộ endpoint và cách gửi session/CSRF.

Các URL trang tiếng Việt trước đây được giữ dưới dạng **GET redirect 301** đến trang tiếng Anh, với kiểm tra quyền tương ứng. Form và API sử dụng URL tiếng Anh; client/Postman cũ cần cập nhật theo REST_API.md. Đăng nhập, đăng xuất và cơ chế CSRF giữ nguyên.
