# Use case và đặc tả chức năng local

Sơ đồ chỉnh sửa được: [USE_CASE_FIT.drawio](../projects/Database/USE_CASE_FIT.drawio). Một tài khoản GV có thể giữ chức Trưởng khoa/Trưởng bộ môn; GVHD, GVPB và hội đồng là các quyền theo phân công đề tài. Nhóm trưởng là vai trò trong nhóm, không phải loại tài khoản.

| Mã FR | Tác nhân / yêu cầu | Điều kiện chính |
|---|---|---|
| FR01 | Người dùng đăng nhập, đổi mật khẩu | BCrypt; phiên tài khoản bị khóa/đổi mật khẩu phải mất hiệu lực |
| FR02 | Admin quản lý hồ sơ, tài khoản, bộ môn, chức vụ | Chỉ xóa chưa tham chiếu; GV phải thuộc bộ môn được bổ nhiệm |
| FR03 | Trưởng khoa quản lý đợt | Hai cửa sổ GV/SV đúng thứ tự; bảo toàn lịch đợt có dữ liệu |
| FR04 | GV đề xuất/sửa/xóa nháp, tìm đề tài | Người đề xuất trong hạn GV; sửa tên/nội dung/bộ môn hủy duyệt cũ và GVHD |
| FR05 | Trưởng bộ môn duyệt | Đúng bộ môn, đề tài Nhap; chọn 1–2 GVHD và lưu người/ngày duyệt trong cùng transaction |
| FR06 | Trưởng khoa công bố đề tài | Xác nhận 1–2 GVHD do bộ môn chọn; lưu người/ngày xác nhận; cả hai bước duyệt phải trước Begin_SV |
| FR07 | SV lập nhóm, mời/xác nhận | Chỉ trưởng nhóm gửi đăng ký/nộp báo cáo; đủ 3–5 người lúc đăng ký |
| FR08 | Nhóm trưởng đăng ký/rút; Trưởng khoa duyệt nhóm | Đăng ký/rút trong hạn SV; Trưởng khoa đương nhiệm chấp thuận/từ chối, từ chối cần lý do. Giữ chỗ một nhóm/một đề tài; GVHD không tự có quyền duyệt |
| FR09 | Nhóm trưởng nộp báo cáo; GV được phân công xem | Kiểm tra quyền sở hữu, loại/kích thước tệp |
| FR10 | Trưởng khoa lập/sửa/xóa/chốt hội đồng và phân công; GVPB/hội đồng chấm | Chỉ sửa/xóa hội đồng nháp chưa dùng; GVHD không tự chấm; phản biện phải đúng hạn |
| FR11 | Chủ tịch tổng hợp; Trưởng khoa công bố kết quả | Khóa điểm sau tổng hợp; gửi email riêng từng SV trong nhóm và cho tra cứu theo đợt |
| FR12 | SV tra cứu kết quả theo đợt | Chỉ kết quả đã công bố của nhóm mình |
| FR13 | Quản lý/tìm thông báo | Admin toàn phạm vi; Trưởng khoa quản lý thông báo Khoa của mình |

| Mã NFR | Yêu cầu | Cách kiểm chứng / giới hạn |
|---|---|---|
| NFR01 | Xác thực và phân quyền tại service, CSRF | Integration tests có các ca sai vai trò, sai bộ môn, thiếu CSRF |
| NFR02 | Toàn vẹn và bảo toàn dữ liệu | Transaction, FK RESTRICT, khóa giao dịch; migration có backup |
| NFR03 | Không lộ bí mật trong log/API | DTO không có hash; AOP không ghi tham số, nội dung hoặc lỗi thô |
| NFR04 | Giao diện tiếng Việt, URL tiếng Anh | Form dùng nhãn, validation và tìm kiếm không phân biệt dấu |
| NFR05 | Theo dõi lỗi/vận hành | AOP ghi kết quả/thời gian; email có hàng đợi, giới hạn thử lại và trang theo dõi cho Trưởng khoa |
| NFR06 | Chạy lại được trên local | Maven Wrapper, Docker, SQL 01–10, dữ liệu mẫu nhập có chủ đích |
| NFR07 | Hiệu năng | Danh sách chính lọc/phân trang tại DB; đề tài tải liên kết theo tập mã; email có phân trang riêng. Chưa nghiệm thu tải lớn |

Các chức năng chung đăng nhập/đổi mật khẩu/thông báo được mô tả trong bảng để sơ đồ chính tập trung vào nghiệp vụ học thuật. Báo cáo Word được nhóm chuẩn bị riêng; source không thay thế báo cáo. Kết quả kiểm tra local không xác nhận phiên bản hoặc tình trạng hoạt động trên hosting.
