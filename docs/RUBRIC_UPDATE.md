# Đối chiếu và cập nhật theo rubric ngày 06/10/2026

Nguồn đối chiếu: `D:\project_web\Rubric.pdf`, đặc biệt tiêu chí 4 trang 3. Văn bản rubric mới được ưu tiên cho ba điểm người dùng yêu cầu thực hiện. Chưa có tài khoản cloud của nhóm tại thời điểm cập nhật.

## Kết luận đối với ba nhận xét của nhóm

| Điểm | Nhận xét | Điều chỉnh |
|---|---|---|
| Email cho từng SV | Đúng: đổi trạng thái công bố hoặc in log chưa chứng minh gửi email | Thêm JavaMailSender SMTP và Brevo HTTPS; lưu từng email trong MySQL; có worker thử lại và màn hình Trưởng khoa theo dõi |
| Nhóm 3–5 SV | Rubric khác đề gốc tối đa 3 | Đăng ký mới phải có 3–5 người đã xác nhận; đúng 1 trưởng nhóm. Nhóm đang lập được 1–2 người. Đồng bộ service, giao diện và trigger bằng migration 06 |
| Deploy free | Trang đầu ghi không bắt buộc nhưng mức xuất sắc vẫn có điều kiện deploy | Chuẩn bị Docker/Render, hướng dẫn Aiven/Supabase/Brevo; chỉ ghi hoàn tất khi có HTTPS và bằng chứng nghiệm thu thật |

Ngoài ba điểm này, đã thêm **tra cứu kết quả theo đợt đăng ký** trên HTML và API. Danh sách đợt chỉ lấy từ nhóm đã có đề tài của SV; sửa URL sang đợt chưa tham gia trả 403. SV ngoài nhóm không xem kết quả của nhóm khác.

## Cách triển khai để giải thích khi vấn đáp

- `DiemService.publish` khóa đề tài/kết quả, cập nhật trạng thái và gọi `ResultEmailService.enqueue` trong cùng transaction. Nếu ghi hàng đợi thất bại, công bố cũng rollback. Gửi mạng không nằm trong transaction này.
- `EMAIL_KET_QUA` giữ snapshot người nhận, nội dung, trạng thái và số lần thử. UNIQUE `(Ma_De_Tai, Ma_SV)` ngăn tạo thêm thông báo khi công bố lặp.
- `EmailDeliveryWorker` lấy từng thư đã commit, đánh dấu đang gửi rồi gọi SMTP/HTTPS ngoài transaction. Khoảng chờ mặc định 15 giây; tối đa 10 thư mỗi lượt, tối đa 5 lần thử mỗi thư. Có lease 2 phút để khôi phục thư đang xử lý khi app dừng đột ngột.
- Trưởng khoa mở `/dean/result-emails` để xem 100 thư mới nhất, thử lại thư FAILED. Địa chỉ nhận không hiển thị cho SV hoặc GV thông thường. Email gửi riêng từng SV, không đưa cả danh sách vào To/CC.
- `ACCEPTED` nghĩa là server SMTP/API chấp nhận yêu cầu. Nhóm vẫn cần bằng chứng Brevo logs và Inbox để xác nhận phát thành công. Nếu phản hồi gửi bị mất, retry có thể gửi trùng; không cam kết exactly-once.
- `AcademicRules.MIN_GROUP_MEMBERS/MAX_GROUP_MEMBERS` tập trung kiểm tra phía Java. Nếu đổi sĩ số lần nữa, phải cập nhật cả trigger SQL, UI và tests; không thể chỉ sửa một hằng số rồi khẳng định mọi tầng đã đồng bộ.

## Dữ liệu và nâng cấp

Migration `06_rubric_email_groups.sql` thêm một bảng và thay hai trigger. Sau đủ 01–06: **22 bảng, 35 trigger, 4 view**. Mỗi script khai báo `SET NAMES utf8mb4` để Docker/mysql CLI không đọc tiếng Việt bằng latin1.

Không thay mật khẩu, thành viên, báo cáo, điểm hoặc ngày đợt đã nhập. Nhóm dưới 3 người đã được duyệt trước migration được giữ nguyên lịch sử. Đăng ký chưa duyệt dưới 3 người không được duyệt mới: cần rút, bổ sung người và đăng ký lại trong thời gian cho phép. Không tự phát email cho kết quả đã công bố trước migration.

Script `tools/Update-RubricDatabase.ps1` dành cho MySQL Docker của repo: dừng app, dump có routines/triggers, lưu backup vào `backups`, áp dụng 06 rồi yêu cầu chạy bản app mới. DB native/cloud dùng Workbench sao lưu và chạy 06 trên đúng connection. Không xóa volume, không dùng Hibernate create/update trên DB nhóm.

## Cloud còn cần người dùng thực hiện

Làm theo [hướng dẫn từng bước](HUONG_DAN_DEPLOY_TUNG_BUOC.md). Chưa xác nhận Aiven TLS/quyền trigger, bucket thật, Brevo sender/API key hoặc URL HTTPS vì nhóm chưa tạo tài khoản. Kiểm thử mock/local không thay thế các bước này.

Gợi ý lịch tới báo cáo 26/10: 06–08/10 tạo tài khoản và thử local; 09–12/10 deploy và gửi thư thật tới thành viên nhóm; 13–18/10 sửa lỗi nghiệm thu/viết Word; 19–23/10 diễn tập và chốt bản; 24–25/10 chỉ kiểm tra dịch vụ và backup.

## Các mục rubric cần xác nhận hoặc hoàn thiện tiếp

1. Tiêu chí mô hình còn nêu **Trưởng bộ môn**. Code hiện có Admin, GV, SV và quyền Trưởng khoa theo chức vụ; chưa có mô hình phân quyền Trưởng bộ môn riêng. Cần giảng viên xác nhận người này được xem/duyệt gì và phạm vi bộ môn trước khi bổ sung quyền. Không gán toàn quyền Trưởng khoa theo suy đoán.
2. Rubric nêu thêm/xóa/sửa/tìm kiếm nhiều danh mục. Đã lập [ma trận thao tác thực có](CRUD_RUBRIC_MATRIX.md): nhiều màn hình hiện ưu tiên tạo, duyệt và chuyển trạng thái, chưa đủ CRUD. Ma trận là kết quả rà soát, không phải các chức năng đã được bổ sung. Xóa dữ liệu học thuật đã được tham chiếu cần quy tắc nghiệp vụ, không thêm nút xóa hàng loạt.
3. Giữ lựa chọn “một SV/một nhóm **mỗi đợt**” từ thiết kế ban đầu. Nếu thầy muốn giới hạn qua nhiều đợt đang diễn ra cùng lúc, cần xác nhận cách xác định đợt hoạt động rồi đổi cả service/SQL/tests.
4. Đã cập nhật ERD 22 bảng (thêm trang chi tiết công bố/email), DATABASE_DICTIONARY và bản thảo BAO_CAO_HUY.md cho EMAIL_KET_QUA và nhóm 3–5 người. Còn cần đưa nội dung vào Word cuối cùng và cập nhật use case; giải thích nhóm đang lập so với nhóm đủ điều kiện đăng ký. Ghi rõ cấu hình free chỉ phục vụ demo có giới hạn.
5. Chưa kết luận dự án đủ toàn bộ mức xuất sắc chỉ từ ba thay đổi này. Điểm mô hình, báo cáo, thuyết trình, đóng góp cá nhân và triển khai thật cần bằng chứng riêng.

## Kiểm chứng

SMTP local qua Mailpit đã được kiểm chứng thêm **2/2 ca đạt** ngày 06/10/2026, nhận ba thư riêng cho ba địa chỉ giả lập. Xem [cách chạy lại và giới hạn kiểm chứng](EMAIL_LOCAL_TEST.md). Không gửi email ra Internet, không bật email hoặc đổi dữ liệu trên app chính.

Kết quả kiểm thử và tình trạng Docker cuối lượt được ghi vào mục cập nhật rubric trong `VERIFY_HUY.md`. Bộ MySQL dùng container riêng `fit-rubric-test-mysql-test-1`, cổng 3308, database `quanlydetai`, không chạy kiểm thử phá dữ liệu trên MySQL local của nhóm.
