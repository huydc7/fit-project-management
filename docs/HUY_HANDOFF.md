# Bàn giao mã nguồn bản đầu

Theo yêu cầu tiếp tục hoàn thiện phần còn lại, nền tảng của Huy đã được nối vào các luồng nghiệp vụ của toàn bản đầu. Phân công gốc vẫn nằm ở kế hoạch nhóm; nhóm đối chiếu thay đổi/Git để cập nhật khối lượng thực tế trước nộp báo cáo.

## Modules

- DeTaiService: đề xuất trong giai đoạn GV, 1–2 GVHD, Trưởng khoa duyệt, danh sách theo quyền và trạng thái giữ chỗ.
- NhomService: tạo nhóm, lời mời/xác nhận, 3–5 SV khi đăng ký đề tài theo rubric mới (nhóm đang lập được 1–2 SV), đăng ký/giữ chỗ/rút, Trưởng khoa duyệt, khóa thành viên. Khóa đợt để serialize các thay đổi nhóm trong đợt; nhóm/đề tài/đăng ký có khóa và UNIQUE bảo vệ.
- BaoCaoService + ReportStorageService: quyền nhóm trưởng, kiểm tra file, reference theo backend, token chống nộp trùng, version bất biến, freeze khi có điểm, kiểm tra người tải, bù xóa sau rollback. SupabaseStorageService thực hiện HTTP, local phục vụ kiểm thử không cần cloud.
- HoiDongService: 3–5 người khác nhau, đúng chủ tịch/thư ký, chốt, phân công phản biện/hội đồng cùng đợt, chặn GVHD và xung đột GVPB/hội đồng. Phân công khóa khi có điểm.
- DiemService: phiếu 0–10, kiểm tra người được phân công và hạn GVPB, tổng hợp đủ phiếu bởi chủ tịch, AVG_REVIEWERS_V1, khóa điểm; Trưởng khoa công bố; SV đọc kết quả đã công bố của nhóm mình.
- TaiKhoanService: tạo tài khoản/hồ sơ, bộ môn, chức vụ, khóa/mở và reset password; người dùng đổi password bằng mật khẩu hiện tại. Credential version khiến phiên cũ hết hiệu lực sau đổi/reset; Dang_Giu_Chuc thu hồi quyền mà không xóa lịch sử.
- ThongBaoService: mọi người đã đăng nhập đọc; Admin đăng Nhà trường/Khoa, Trưởng khoa chỉ Khoa. Nội dung dùng th:text.
- Controller theo nghiệp vụ (DeTai, Nhom, BaoCao, HoiDong, Diem, TaiKhoan, ThongBao), REST API v1 và các trang SV/GV/Trưởng khoa/Admin, menu theo role và responsive; URL mới đã có matcher. Form lỗi giữ dữ liệu; password không render lại vào HTML.

## Quy ước

22 entity bảng và 8 interface Spring Data JPA, có ResultEmail/ResultEmailRepository cho hàng đợi email kết quả. Các repository rỗng và entity view không còn được gọi đã bỏ. Scalar FK là nguồn ghi. JPA EntityManager dùng persist khi tạo, flush/refresh để lấy giá trị trigger. Controller dùng DTO và không truyền mật khẩu hash ra view.

CurrentUserService lấy actor từ SecurityContext và kiểm tra tài khoản; JpaEntityRepository truy vấn tham số hóa, tìm/khóa dữ liệu trong transaction; ReferenceDataService tải lựa chọn; AcademicRules kiểm tra thời gian và điều kiện nghiệp vụ. Mã SV/GV trong request chỉ là đối tượng thao tác; không dùng làm actor.

Điểm, phân công và nộp báo cáo khóa DE_TAI để serialize. Đăng ký có hai UNIQUE giữ một nhóm/một đề tài đang hoạt động. Report callback dọn file chỉ khi transaction ROLLED_BACK, không dọn khi COMMIT đã thành công hoặc outcome chưa xác định. Signed URL không ghi vào DB/log; thời gian sống 300 giây.

BusinessSecurityService còn các predicate dùng chung. Khi thêm chức năng, kiểm tra role, quan hệ, thời gian và state trong service; không dựa vào ẩn nút/menu. Bổ sung URL matcher cho route mới. Password BCrypt giới hạn tối đa 72 byte UTF-8; môi trường demo lấy password từ biến môi trường.

## Database và cấu hình

Chạy đủ 01–06; CSDL gốc đã có dùng migration 04/05/06. Giữ ddl-auto=validate. Tham khảo PRODUCT_RULES về 22 bảng/35 trigger, công thức mới và dữ liệu cũ không tự chuyển đổi. Dữ liệu thực hành nằm ngoài source/JAR và chỉ được nạp có chủ đích ở dev, xem SAMPLE_DATA. Hướng dẫn local trong LOCAL_TEST, cloud từng bước trong HUONG_DAN_DEPLOY_TUNG_BUOC, kết quả cập nhật rubric trong RUBRIC_UPDATE.

Cloud Supabase/Aiven/Render chưa có tài khoản thật. Cấu hình triển khai có sẵn nhưng không đồng nghĩa đã deploy hoặc CI GitHub đã chạy. Bộ test storage dùng HTTP giả lập; sau khi tạo bucket phải test thật upload/download, TLS, restart và backup/restore.

Các màn hình bản đầu đã nối; ảnh cho Chương 5 cần lấy từ môi trường mà nhóm nghiệm thu, có URL/commit và đúng dữ liệu demo. Report Word, URL cloud và ZIP tên nhóm/MSSV là bước bàn giao cuối sau kiểm thử local.
