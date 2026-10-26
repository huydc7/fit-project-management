# Giao diện FIT HCMUTE

Bản giao diện ngày 02/10/2026 sử dụng Thymeleaf, HTML/CSS và JavaScript thuần; không dùng Bootstrap, CDN, font hoặc bộ icon từ bên ngoài. Dấu hiệu FIT là chữ thiết kế cho dự án, không phải logo chính thức của trường.

## Các phần chính

- `templates/layout/base.html`: khung ứng dụng, nội dung chính, liên kết bỏ qua menu cho bàn phím và footer.
- `templates/layout/fragments.html`: menu có icon SVG theo vai trò, trạng thái trang hiện tại, menu tài khoản và đăng xuất có CSRF.
- `templates/auth/login.html`: bố cục một cột, logo/tên trường phía trên khung đăng nhập; thông báo lỗi/phiên, hiện/ẩn mật khẩu và hướng dẫn liên hệ quản trị viên.
- `static/images/hcmute-logo.png`: ảnh trường lấy từ `Logo.png` do người dùng cung cấp; dùng nguyên ảnh trên trang đăng nhập.
- `static/images/hcmute-favicon.png` và `static/favicon.ico`: phần biểu tượng tròn của ảnh trường, đặt trên nền trắng để dễ nhìn ở kích thước tab. ICO có các kích thước 16, 32 và 48 px.
- Chân trang đăng nhập dùng dấu hiệu chữ FIT do dự án thiết kế, kèm tên khoa và HCMUTE. Nút mật khẩu dùng icon mắt/mắt gạch chéo và nhãn hỗ trợ trình đọc màn hình.
- `templates/home.html`: lối tắt theo quyền, ba thông báo gần nhất từ database, trạng thái trống và hướng dẫn quy trình. Không dùng số liệu hoặc hạn nộp giả.
- `static/css/custom.css`: màu, cỡ chữ, bố cục, thành phần form/bảng/thông báo và quy tắc responsive. Muốn đổi màu chủ đạo, sửa biến `--brand` và `--brand-dark` tại `:root`.
- `static/js/app.js`: menu di động, hiện mật khẩu, giao diện sáng/tối, cửa sổ chi tiết, bộ chọn giảng viên và phản hồi lý do từ chối. Form vẫn gửi qua MVC/CSRF; backend kiểm tra lại quyền và dữ liệu khi lưu.

Các trang nhóm, đề tài, tài khoản, báo cáo, hội đồng, chấm điểm và quản lý đợt dùng bộ thành phần chung. Tên trường, URL tiếng Anh, điều kiện phân quyền và ràng buộc nghiệp vụ vẫn theo backend.

## Quy ước

Màu xanh đậm cho nhận diện, trắng/xám nhạt cho nội dung; xanh lá cho thao tác thành công và đỏ cho thao tác có ảnh hưởng như khóa tài khoản. Trạng thái có chữ, không chỉ thể hiện bằng màu. Biểu mẫu có nhãn, focus bàn phím rõ, icon trang trí ẩn khỏi trình đọc màn hình.

Menu chuyển sang nút thu gọn dưới 1001 px. Nội dung về một cột dưới 701 px, bảng rộng cuộn trong vùng riêng. CSS tôn trọng `prefers-reduced-motion`. Assets nằm trong dự án để demo không phụ thuộc Internet.

Trang đăng nhập điều chỉnh kích thước logo và khoảng cách theo chiều cao viewport (`dvh`). Màn hình thấp dùng khoảng cách gọn hơn; dưới 621 px, logo/tên trường xếp cạnh nhau để giữ đủ chỗ cho form và footer. Không khóa cuộn bằng `overflow: hidden`, để nội dung vẫn truy cập được khi bật bàn phím ảo hoặc phóng chữ lớn.

## Xem và chỉnh sửa

Mở `/login` để xem trang đăng nhập hoặc `/` sau khi đăng nhập. Chỉnh HTML trong template, CSS trong `custom.css`; không cần npm hoặc bước build frontend. Nếu chạy Docker, build lại service app để đưa tài nguyên mới vào JAR. Khi chạy trực tiếp qua Spring Boot dev, khởi động lại ứng dụng nếu tài nguyên chưa được cập nhật.

## Danh sách và bộ lọc đề tài

Quản lý đề tài là mục điều hướng riêng, dùng bảng với tên/mã, đợt, bộ môn, GVHD, nhóm và trạng thái. Nút Xem chi tiết mở cửa sổ dialog giữa màn hình, không giãn hàng trong bảng. Dialog có thông tin và thao tác theo quyền, vùng nội dung cuộn riêng, nút đóng, Esc và đóng bằng nền ngoài; giữ focus trong cửa sổ và trả lại nút mở khi đóng. Danh sách toàn khoa chỉ dành cho Trưởng khoa, không mở rộng quyền của giảng viên hoặc Trưởng bộ môn.

Bộ lọc dùng grid 12 cột, nhãn nổi, ô cao 48px và hai hàng trên desktop; tự xuống hàng trên màn hình nhỏ. Không dùng flex-basis làm chiều cao trong form dọc. Lựa chọn đợt dùng dữ liệu thực tế, không suy đoán năm học/học kỳ. Form GET giữ giá trị đang lọc, phân trang giữ query và nút Đặt lại về danh sách gốc. Tái sử dụng bộ lọc ở trang duyệt và danh sách SV.

Tên góc phải và sidebar lấy từ hồ sơ thay cho username. Mũi tên SVG 16px xoay khi mở menu; summary có focus ring cho bàn phím. CSS/JS được đổi phiên bản khi cập nhật để tránh cache giao diện cũ.

Trang Đề xuất đề tài chỉ có form tạo mới. Danh sách và thao tác sửa/xóa nằm trong Quản lý đề tài; chỉnh sửa dùng trang riêng, vẫn kiểm tra người đề xuất, trạng thái và thời hạn ở backend.

Trang Duyệt & công bố là danh sách chờ xử lý: chỉ lấy đề tài Nháp đã có duyệt chuyên môn của Trưởng bộ môn và kết quả Chưa công bố. Không tải đề tài đã công bố, đang thực hiện, hoàn thành hoặc hủy vào phần duyệt đề tài; kết quả đã công bố cũng rời danh sách chờ. Lọc và phân trang thực hiện trên truy vấn database cho cả hai phần. Không có bộ lọc trạng thái để mở rộng phạm vi này. Đề tài quá hạn duyệt vẫn hiện cảnh báo thời hạn, không được bỏ qua kiểm tra khi duyệt. Phân công GVPB và hội đồng nằm trong chi tiết đề tài Đang thực hiện tại Quản lý đề tài.

Chi tiết đề tài hiển thị tên nhóm cùng danh sách SV (mã, họ tên, nhóm trưởng/thành viên); hội đồng hiển thị các GV cùng vai trò chủ tịch, thư ký, ủy viên, mã hội đồng ở dòng phụ. Hiển thị người đề xuất riêng với danh sách 1–2 GVHD do bộ môn lựa chọn. Cửa sổ phản ánh phân công thực tế đã lưu, không tự thay đổi lịch sử hoặc coi đồng hướng dẫn là dữ liệu sai. Các danh sách thành viên được truy vấn theo lô cho các đề tài thuộc trang và phạm vi được phép xem.

Trang Thông báo đặt form đăng trước, thanh tìm kiếm gọn ngay trước danh sách. Form đề xuất không còn câu nhắc tự gán GVHD hoặc giới hạn 3 đề tài mỗi đợt.

## Duyệt chuyên môn và đăng ký nhóm

Tại `/department/topics`, Trưởng bộ môn chọn 1–2 GVHD bằng checkbox có tìm kiếm theo họ tên/mã, bộ đếm và giới hạn hai người. Danh sách chỉ gồm GV thuộc bộ môn phụ trách, có tài khoản hoạt động. Khi chấp thuận, backend kiểm tra lại số lượng, trùng lặp, tài khoản, bộ môn và hạn mức hướng dẫn trước khi ghi. Từ chối dùng form riêng và không yêu cầu chọn GVHD.

Mục Duyệt đăng ký nhóm chỉ hiện với Trưởng khoa, dẫn tới `/dean/registrations`. Trang này giữ bố cục danh sách, phân trang và lỗi cạnh ô lý do; bắt buộc nhập lý do khi từ chối. Menu của GV/GVHD không hiển thị chức năng này nếu tài khoản không đồng thời là Trưởng khoa. Truy cập URL hoặc API trực tiếp vẫn chịu kiểm tra quyền. Trang Duyệt & công bố có liên kết tới trang duyệt nhóm, không trộn đăng ký vào danh sách chờ công bố đề tài/kết quả.

Giao diện tiếp tục giữ lựa chọn đề tài trước khi lập hội đồng, liên kết xem ảnh hưởng trước khi khóa GV, phân trang ngay sau danh sách đợt, tên người đề xuất ở hàng chờ duyệt và tránh lặp lỗi từ chối ở đầu trang.
