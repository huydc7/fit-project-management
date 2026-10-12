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
- `static/js/app.js`: menu di động và nút hiện mật khẩu. Form vẫn gửi qua MVC/CSRF; menu và chức năng chính dùng được khi tắt JavaScript.

Các trang nhóm, đề tài, tài khoản, báo cáo, hội đồng, chấm điểm và quản lý đợt dùng bộ thành phần chung. Tên trường, URL tiếng Anh, điều kiện phân quyền và ràng buộc nghiệp vụ vẫn theo backend.

## Quy ước

Màu xanh đậm cho nhận diện, trắng/xám nhạt cho nội dung; xanh lá cho thao tác thành công và đỏ cho thao tác có ảnh hưởng như khóa tài khoản. Trạng thái có chữ, không chỉ thể hiện bằng màu. Biểu mẫu có nhãn, focus bàn phím rõ, icon trang trí ẩn khỏi trình đọc màn hình.

Menu chuyển sang nút thu gọn dưới 1001 px. Nội dung về một cột dưới 701 px, bảng rộng cuộn trong vùng riêng. CSS tôn trọng `prefers-reduced-motion`. Assets nằm trong dự án để demo không phụ thuộc Internet.

Trang đăng nhập điều chỉnh kích thước logo và khoảng cách theo chiều cao viewport (`dvh`). Màn hình thấp dùng khoảng cách gọn hơn; dưới 621 px, logo/tên trường xếp cạnh nhau để giữ đủ chỗ cho form và footer. Không khóa cuộn bằng `overflow: hidden`, để nội dung vẫn truy cập được khi bật bàn phím ảo hoặc phóng chữ lớn.

## Xem và chỉnh sửa

Mở `/login` để xem trang đăng nhập hoặc `/` sau khi đăng nhập. Chỉnh HTML trong template, CSS trong `custom.css`; không cần npm hoặc bước build frontend. Nếu chạy Docker, build lại service app để đưa tài nguyên mới vào JAR. Khi chạy trực tiếp qua Spring Boot dev, khởi động lại ứng dụng nếu tài nguyên chưa được cập nhật.
