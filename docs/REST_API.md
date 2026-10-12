# REST API v1

Base URL local: `http://localhost:8080/api/v1`. API dùng cùng service, tài khoản và phiên đăng nhập với Thymeleaf. Không trả JPA entity hoặc hash mật khẩu. Request JSON dùng `Content-Type: application/json`; báo cáo dùng multipart. Ngày giờ nghiệp vụ theo múi giờ Việt Nam.

## Đăng nhập và CSRF

1. `GET /api/v1/session/csrf` để nhận `{headerName, parameterName, token}` và cookie phiên. Endpoint này cho phép chưa đăng nhập, không trả thông tin tài khoản.
2. Giữ cookie, `POST /login` với body **form-urlencoded** gồm `username`, `password`, `_csrf` (token vừa nhận). Đây là endpoint đăng nhập form sẵn có, phản hồi redirect; đăng nhập lỗi redirect về `/login?error`.
3. Sau đăng nhập, gọi lại `GET /api/v1/session/csrf` với cookie mới vì đăng nhập xoay phiên và token.
4. Với mọi POST/PUT/PATCH/DELETE, gửi cookie và header tên `headerName` (mặc định `X-CSRF-TOKEN`) chứa `token`. GET chỉ cần cookie. Không tắt CSRF khi thử Postman.
5. Đăng xuất dùng `POST /logout` kèm CSRF. Đổi mật khẩu thành công qua `PUT /api/v1/session/password` làm hết phiên và yêu cầu đăng nhập lại.

Trong trình duyệt, đăng nhập qua trang `/login` rồi mở DevTools Console trên chính website để thử:

```javascript
const response = await fetch('/api/v1/catalog/departments');
console.log(await response.json());
```

Ví dụ tạo nhóm (thực sự ghi dữ liệu, chỉ chạy khi muốn tạo nhóm, tài khoản SV và đợt còn hạn):

```javascript
const csrf = await fetch('/api/v1/session/csrf').then(r => r.json());
const response = await fetch('/api/v1/groups', {
  method: 'POST',
  headers: {'Content-Type': 'application/json', [csrf.headerName]: csrf.token},
  body: JSON.stringify({period: 'MA_DOT_THUC_TE', name: 'Nhóm Công nghệ phần mềm'})
});
console.log(response.status); // 201 khi tạo thành công
```

API không dùng JWT/HTTP Basic. Client khác origin chưa được bật CORS. Không gửi mã SV/GV của người thực hiện trong body; danh tính được lấy từ phiên. Các mã nhóm, đề tài, người được mời/phân công là đối tượng của thao tác và vẫn phải qua kiểm tra quyền service.

## Endpoint

Các đường dẫn trong bảng đều có tiền tố `/api/v1`. `TK` là tài khoản đã đăng nhập; `TKhoa` là Trưởng khoa đang giữ chức. Quyền còn phụ thuộc quan hệ, trạng thái và thời hạn.

| Nghiệp vụ | Method và đường dẫn | Quyền / body |
|---|---|---|
| Danh mục | GET `/catalog/periods`, `/catalog/departments`, `/catalog/lecturers` | TK |
| Đợt đăng ký | GET, POST `/registration-periods` | TKhoa; POST: DotDangKyForm |
| Đề tài công bố | GET `/topics` | SV |
| Đề tài hướng dẫn/đề xuất | GET `/topics/mine` | GV |
| Quản lý đề tài | GET `/topics/management` | TKhoa |
| Đề xuất đề tài | POST `/topics` | GV; DeTaiRequest |
| Duyệt đề tài | PATCH `/topics/{id}/review` | TKhoa; `{accept: true/false}` |
| Nhóm của mình | GET, POST `/groups` | SV; POST: `{period, name}` |
| Mời thành viên | POST `/groups/{id}/invitations` | Trưởng nhóm; `{student}` là Ma_SV |
| Xóa thành viên | DELETE `/groups/{id}/members/{student}` | Trưởng nhóm; còn được sửa |
| Lời mời của mình | GET `/invitations` | SV |
| Trả lời lời mời | PATCH `/invitations/{id}` | Người được mời; `{accept: true/false}` |
| Đăng ký đề tài | POST `/registrations` | Trưởng nhóm; `{group, topic}` |
| Rút đăng ký | PATCH `/registrations/{id}/withdraw` | Trưởng nhóm |
| Đăng ký chờ duyệt | GET `/registrations/pending` | TKhoa |
| Duyệt đăng ký | PATCH `/registrations/{id}/review` | TKhoa; `{accept, reason}` |
| Báo cáo nhóm mình | GET `/reports/mine` | SV |
| Báo cáo được phân công | GET `/reports/assigned` | GV |
| Nộp báo cáo | POST `/reports` | Trưởng nhóm; multipart `group`, `token`, `file` |
| Tải báo cáo | GET `/reports/{id}/download` | ACL theo nhóm/GV được phân công/TKhoa |
| Hội đồng | GET, POST `/councils` | TKhoa; POST: HoiDongRequest |
| Chốt hội đồng | PATCH `/councils/{id}/finalize` | TKhoa |
| Phân công phản biện | PUT `/councils/topics/{id}/reviewer` | TKhoa; `{teacher}` là Ma_GV |
| Gán hội đồng | PUT `/councils/topics/{id}` | TKhoa; `{council}` |
| Phiếu chấm của mình | GET `/grades/ballots` | GV được phân công |
| Lưu điểm | PUT `/grades/ballots` | GV được phân công; DiemRequest |
| Tổng hợp điểm | POST `/grades/topics/{id}/aggregate` | Chủ tịch hội đồng; `{comment}` |
| Công bố kết quả | PATCH `/grades/topics/{id}/publish` | TKhoa; tạo email cá nhân trong cùng transaction, worker gửi sau commit |
| Kết quả nhóm mình | GET `/grades/results/mine?period={maDot}` | SV; period tùy chọn, chỉ kết quả đã công bố của nhóm mình; đợt chưa đăng ký trả 403 |
| Quản lý kết quả | GET `/grades/results` | TKhoa |
| Thông báo | GET, POST `/announcements` | GET: TK; POST: Admin/TKhoa, ThongBaoRequest |
| Tài khoản | GET, POST `/admin/accounts` | Admin; POST: TaiKhoanRequest |
| Khóa/mở tài khoản | PATCH `/admin/accounts/{id}/status` | Admin; `{active: true/false}` |
| Reset mật khẩu | PUT `/admin/accounts/{id}/password` | Admin; `{password}` |
| Thêm bộ môn | POST `/admin/departments` | Admin; `{id, name}` |
| Bổ nhiệm/thu hồi trưởng khoa | PUT `/admin/dean` | Admin; `{teacher, appointed}` |
| Đổi mật khẩu mình | PUT `/session/password` | TK; `{currentPassword, newPassword, confirmation}` |

DTO có validation nằm trong `src/main/java/com/example/quanlydetai/dto`. Ví dụ DeTaiRequest gồm `period`, `title`, `content`, `department`, `supervisors` (1–2 Ma_GV). DiemRequest gồm `topic`, `type` (`GVPB` hoặc `HD`), `value` (0–10, tối đa 2 số thập phân), `comment`. Không lấy số điểm tổng hợp từ client.

Token nộp báo cáo dùng đúng 24 ký tự hex viết thường `[0-9a-f]` (ví dụ UUID bỏ dấu `-`, cắt 24 ký tự), giữ cùng token khi gửi lại cùng yêu cầu để tránh tạo hai phiên bản do double-click. Khi nộp phiên bản mới, tạo token mới. Phần tải trả file riêng tư hoặc redirect URL ký ngắn hạn; không có đường dẫn file công khai.

## Phản hồi

- GET danh sách: `200` và mảng JSON (có thể rỗng).
- POST tạo: `201`, body rỗng; đọc lại danh sách tương ứng để lấy bản ghi. POST tổng hợp điểm và các cập nhật thành công: `204`.
- Dữ liệu không hợp lệ hoặc vi phạm nghiệp vụ: `400`; chưa đăng nhập/phiên hết hạn: `401`; sai quyền/thiếu CSRF: `403`; route không tồn tại: `404`; method không hỗ trợ: `405`; loại nội dung sai: `415`; file quá lớn: `413`; xung đột database: `409`.
- Lỗi JSON theo ProblemDetail (`application/problem+json`), gồm `status`, `title`, `detail`; lỗi validation có thêm `errors` theo tên trường, không chứa giá trị mật khẩu nhập vào. Lỗi không dự kiến trả mã tra cứu, chi tiết lưu trong server log.

Danh sách hiện chưa phân trang. Đây là API v1 phục vụ quy mô đồ án/khoa nhỏ; khi dữ liệu lớn, bổ sung phân trang và bộ lọc vào repository/service trước khi mở rộng client. Chưa cung cấp Swagger/OpenAPI tự sinh.

Giới hạn file là 20 MiB, toàn bộ multipart request là 21 MiB. File vừa vượt 20 MiB đã được kiểm tra trả JSON 413. Khi toàn request vượt 21 MiB, Tomcat có thể đóng kết nối trước khi client nhận body lỗi; client cần xử lý cả lỗi mạng khi upload quá lớn.
