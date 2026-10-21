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
| Duyệt đề tài | PATCH `/topics/{id}/review` | TKhoa; `{accept: true/false}`; chấp thuận xác nhận GVHD bộ môn đã chọn rồi công bố |
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
| Công bố kết quả | PATCH `/grades/topics/{id}/publish` | TKhoa; công bố điểm/thời điểm và tạo email riêng cho từng SV trong cùng transaction, worker gửi sau commit |
| Kết quả nhóm mình | GET `/grades/results/mine?period={maDot}` | SV; period tùy chọn, chỉ kết quả đã công bố của nhóm mình; đợt chưa đăng ký trả 403 |
| Quản lý kết quả | GET `/grades/results` | TKhoa |
| Thông báo | GET, POST `/announcements` | GET: TK; POST: Admin/TKhoa, ThongBaoRequest |
| Tài khoản | GET, POST `/admin/accounts` | Admin; POST: TaiKhoanRequest |
| Ảnh hưởng khi khóa tài khoản | GET `/admin/accounts/{id}/lock-impact` | Admin; danh sách nhiệm vụ còn lại, không trả mật khẩu |
| Khóa/mở tài khoản | PATCH `/admin/accounts/{id}/status` | Admin; `{active: true/false, confirmPendingTasks?: true}`; khóa GV còn nhiệm vụ cần xác nhận |
| Reset mật khẩu | PUT `/admin/accounts/{id}/password` | Admin; `{password}` |
| Thêm bộ môn | POST `/admin/departments` | Admin; `{id, name}` |
| Bổ nhiệm/thu hồi trưởng khoa | PUT `/admin/dean` | Admin; `{teacher, appointed}` |
| Đổi mật khẩu mình | PUT `/session/password` | TK; `{currentPassword, newPassword, confirmation}` |
| Thông tin cá nhân mình | GET `/session/profile` | SV/GV; chỉ hồ sơ của phiên đăng nhập, Admin trả 403; không nhận mã tài khoản cần xem |

DTO có validation nằm trong `src/main/java/com/example/quanlydetai/dto`. Ví dụ DeTaiRequest gồm `period`, `title`, `content`, `department`. Người đề xuất không được gửi `supervisors`; Trưởng bộ môn chọn 1–2 Ma_GV qua DepartmentReviewRequest khi duyệt. DiemRequest gồm `topic`, `type` (`GVPB` hoặc `HD`), `value` (0–10, tối đa 2 số thập phân), `comment`. Không lấy số điểm tổng hợp từ client.

`GET /admin/accounts` trả thêm hai cờ `dean` và `departmentHead` cho chức vụ **đang giữ**. Cả hai có thể cùng `true` khi kiêm nhiệm; sau thu hồi/thay thế, lần tải danh sách tiếp theo phản ánh dữ liệu mới. `role` vẫn là `GV` cho giảng viên, bao gồm Trưởng khoa/Trưởng bộ môn; bộ lọc `role=GV` vẫn trả các tài khoản này. Giao diện quản trị hiển thị tên vai trò tiếng Việt và nhãn chức vụ riêng.

`GET /session/profile` trả `username`, `code` (mã SV/GV), `fullName`, `email`, `role`, `departmentName`, `dean`, `departmentHead`. SV có `departmentName` rỗng và hai cờ chức vụ `false`; GV có tên bộ môn và chức vụ đương nhiệm. Trang `/accounts/profile` dùng cùng service, chỉ đọc thông tin; quản trị viên cập nhật hồ sơ tại trang quản lý tài khoản. Cả HTML và API hồ sơ cá nhân đặt `Cache-Control: no-store`.

Token nộp báo cáo dùng đúng 24 ký tự hex viết thường `[0-9a-f]` (ví dụ UUID bỏ dấu `-`, cắt 24 ký tự), giữ cùng token khi gửi lại cùng yêu cầu để tránh tạo hai phiên bản do double-click. Khi nộp phiên bản mới, tạo token mới. Phần tải trả file riêng tư hoặc redirect URL ký ngắn hạn; không có đường dẫn file công khai.

## Phản hồi

`GET /catalog/lecturers` chỉ trả GV có tài khoản GV đang `HoatDong`, phục vụ phân công. Backend kiểm tra lại trạng thái khi ghi phân công, công bố đề tài và chốt/gán hội đồng, kể cả khi client gửi mã trực tiếp.

Admin dùng `GET /admin/accounts/{id}/lock-impact` để xem `{accountId, username, name, state, tasks}`; mỗi nhiệm vụ có `{role, topicId, topicTitle, councilId, guidance}`. Khi khóa GV đang hoạt động và còn nhiệm vụ, `PATCH /admin/accounts/{id}/status` với `{active:false}` trả 400 và giữ trạng thái cũ. Sau khi xem ảnh hưởng, gửi `{active:false,confirmPendingTasks:true}` để xác nhận khóa. Mở khóa dùng `{active:true}` như trước. Không tự xóa hoặc giảm số phiếu bắt buộc; danh sách nhiệm vụ được kiểm tra lại trong transaction khi khóa.

- GET danh sách: `200` và mảng JSON (có thể rỗng).
- POST tạo: `201`, body rỗng; đọc lại danh sách tương ứng để lấy bản ghi. POST tổng hợp điểm và các cập nhật thành công: `204`.
- Dữ liệu không hợp lệ hoặc vi phạm nghiệp vụ: `400`; chưa đăng nhập/phiên hết hạn: `401`; sai quyền/thiếu CSRF: `403`; route không tồn tại: `404`; method không hỗ trợ: `405`; loại nội dung sai: `415`; file quá lớn: `413`; xung đột database: `409`.
- Lỗi JSON theo ProblemDetail (`application/problem+json`), gồm `status`, `title`, `detail`; lỗi validation có thêm `errors` theo tên trường, không chứa giá trị mật khẩu nhập vào. Lỗi không dự kiến trả mã tra cứu, chi tiết lưu trong server log.

Các danh sách nghiệp vụ cũ chưa phân trang; riêng email kết quả có phân trang tại database như phần bổ sung dưới. Đây là API v1 phục vụ quy mô đồ án/khoa nhỏ; khi dữ liệu lớn cần mở rộng phân trang và bộ lọc cho các module còn lại. Chưa cung cấp Swagger/OpenAPI tự sinh.

Giới hạn file là 20 MiB, toàn bộ multipart request là 21 MiB. File vừa vượt 20 MiB đã được kiểm tra trả JSON 413. Khi toàn request vượt 21 MiB, Tomcat có thể đóng kết nối trước khi client nhận body lỗi; client cần xử lý cả lỗi mạng khi upload quá lớn.

## Bổ sung local 15/10/2026

| Method / đường dẫn (sau /api/v1) | Quyền và dữ liệu |
|---|---|
| GET `/topics`, `/topics/mine`, `/topics/management` | Quyền cũ; thêm q, department, period |
| GET `/topics/department` | Trưởng bộ môn; chỉ đề tài bộ môn mình |
| PATCH `/topics/{id}/department-review` | Trưởng bộ môn; `{accept:true, supervisors:["GV01"]}` hoặc `{accept:false}` |
| PUT `/topics/{id}` | Người đề xuất nháp trong hạn GV; `{title, content, department?}`, hủy xác nhận bộ môn và danh sách GVHD |
| DELETE `/topics/{id}` | Người đề xuất nháp trong hạn, chưa có lịch sử |
| PUT `/admin/accounts/{id}/profile` | Admin; `{name, email, department}` (department cho GV) |
| DELETE `/admin/accounts/{id}` | Admin; tài khoản và hồ sơ chưa có tham chiếu |
| PUT, DELETE `/admin/departments/{id}` | Admin; PUT `{id, name}`, giữ mã |
| PUT `/admin/department-head` | Admin; `{department, teacher, appointed}` |
| PUT, DELETE `/registration-periods/{id}` | Trưởng khoa; PUT DotDangKyForm; đợt có dữ liệu chỉ đổi tên |
| PUT, DELETE `/announcements/{id}` | Admin hoặc Trưởng khoa với thông báo Khoa của mình; PUT ThongBaoRequest |

GET `/admin/accounts` thêm q, role; `/announcements`, `/registration-periods`, `/catalog/departments` thêm q. Công bố qua `/topics/{id}/review` bắt buộc đã được bộ môn duyệt. Các ràng buộc còn lại xem [PRODUCT_RULES.md](PRODUCT_RULES.md). Tìm kiếm chưa phân trang.

## Email kết quả — bổ sung 18/10/2026

| Method / đường dẫn (sau /api/v1) | Quyền và hành vi |
|---|---|
| GET `/result-emails?q=&state=&page=0&size=20` | Trưởng khoa; tìm mã đề tài/SV/email, lọc PENDING/PROCESSING/ACCEPTED/FAILED, phân trang trong DB |
| POST `/result-emails/{id}/retry` | Trưởng khoa + CSRF; chỉ FAILED, đặt lại PENDING/attempts=0; 204 khi thành công |

JSON GET: `{content, page, size, totalElements, totalPages}`. content có id/topic/student/recipient/state/attempts/created/accepted, không có body/receipt/credential. page 0–1000000, size 1–100; q tối đa 200 ký tự, wildcard `%`/`_`/`!` được escape để tìm literal. Sai dữ liệu trả 400, chưa xác thực 401, sai quyền/CSRF 403. Retry không tạo bản ghi mới; bấm lại khi PENDING trả 400. Bộ lọc state không phân biệt chữ hoa/thường. Kiểm thử SMTP xem [EMAIL_LOCAL_TEST.md](EMAIL_LOCAL_TEST.md).

## Bổ sung 19/10/2026

- PUT `/topics/{id}` giữ title/content bắt buộc, department tùy chọn. Người đề xuất không được gửi supervisors; mọi thay đổi hủy duyệt bộ môn và xóa danh sách GVHD.
- GET `/topics`, `/topics/mine`, `/topics/management`, `/topics/department` thêm type/state; endpoint department hỗ trợ period, luôn giới hạn đúng bộ môn.
- PUT `/councils/{id}` nhận HoiDongRequest, DELETE `/councils/{id}`: Trưởng khoa, chỉ DangLap chưa dùng. Không đổi đợt qua PUT.
- PUT `/registration-periods/{id}`: đợt có dữ liệu chỉ sửa tên/gia hạn End_SV và deadline GVPB, không rút ngắn. Có kết quả tổng hợp chỉ đổi tên. Đợt trống vẫn sửa đầy đủ.
- PATCH duyệt bộ môn/công bố đề tài phải trước Begin_SV. Công bố điểm và duyệt đăng ký nhóm không bị áp dụng nhầm hạn này. Xem [PRODUCT_RULES.md](PRODUCT_RULES.md) và migration 08.

## Phân công GVHD theo bộ môn (SQL 09)

- POST `/topics`: `{period,title,content,department}`. Gửi `supervisors` sẽ bị từ chối.
- PUT `/topics/{id}`: `{title,content,department?}`. Sửa nháp xóa xác nhận bộ môn và GVHD.
- PATCH `/topics/{id}/department-review`: `{accept:true,supervisors:["GV01","GV02"]}`.
  Chỉ Trưởng bộ môn đúng phạm vi; bắt buộc 1–2 mã GV khác nhau. Từ chối: `{accept:false}`, không cần supervisors.
- PATCH `/topics/{id}/review`: `{accept:true}` để Trưởng khoa xác nhận và công bố.
  Không thay đổi GVHD ở endpoint này. Trả về proposerId, confirmedBy, confirmedAt trong danh sách đề tài.
