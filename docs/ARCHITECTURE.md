# Cấu trúc source và lý do điều chỉnh

Mẫu Todo và dự án này cùng sử dụng Spring MVC, Spring Data JPA, Spring Security. `@Controller` trả tên template HTML là đúng với Thymeleaf. `@RestController` trả JSON cho JavaScript, Postman hoặc ứng dụng khác. REST không tự làm một sản phẩm tốt hơn; cần đúng hợp đồng dữ liệu, kiểm tra quyền và quy tắc nghiệp vụ.

## Cấu trúc hiện tại

```text
src/main/java/com/example/quanlydetai/
  config/                 cấu hình Spring Security cho HTML và API
  controller/             controller HTML theo nghiệp vụ
    api/v1/               RestController theo nghiệp vụ, trả DTO JSON
    support/              phản hồi form và tải tệp dùng chung
  dto/                    Request/Response cụ thể; DotDangKyForm/View hiện có
  service/                nghiệp vụ, transaction, kiểm tra trạng thái và thời gian
  repository/             Spring Data JpaRepository và thao tác JPA tùy chỉnh
  entity/                 ánh xạ bảng MySQL; tương đương tên model trong mẫu Todo
  security/               principal, kiểm tra phiên và người đang thao tác
  exception/              lỗi HTML và lỗi JSON được xử lý riêng
  storage/                kiểm tra, lưu và tải báo cáo
src/main/resources/
  templates/              giao diện Thymeleaf
  static/                 CSS/JS/vendor
src/test/java/.../
  tools/sample/           công cụ nhập dữ liệu thực hành và kiểm thử importer
```

Controller HTML nhận form, gọi service rồi render/redirect. REST nhận JSON (multipart cho báo cáo), gọi cùng service rồi trả DTO/status. Service giữ transaction và quyền theo từng đối tượng; không tin mã SV/GV trong request như danh tính người đăng nhập. Repository xử lý lưu trữ. Entity không được trả trực tiếp ra API để tránh lộ hash mật khẩu hoặc quan hệ JPA.

## Những chỗ đã thay đổi

- Bỏ lớp gom `WorkflowController`: chia theo đề tài, nhóm, báo cáo, hội đồng, điểm, tài khoản, thông báo. Trang tổng hợp duyệt dùng DuyetDeTaiController.
- Bỏ `WorkflowForms/WorkflowViews`: DTO riêng như DeTaiRequest, DeTaiResponse, NhomRequest, NhomResponse. Một DTO có thể dùng cả form và API nếu cùng quy tắc kiểm tra.
- Tách `WorkflowSupport`: CurrentUserService chỉ lấy/kiểm tra người thao tác; AcademicRules kiểm tra thời gian và điều kiện; ReferenceDataService đọc danh mục; JpaEntityRepository giữ các thao tác JPA cần cho giao dịch nhiều bảng.
- `config` chỉ còn cấu hình ứng dụng. Importer dữ liệu thực hành chuyển sang test classpath, chạy bằng tools/Import-SampleData.ps1, không có trong JAR triển khai.
- REST có tiền tố `/api/v1`, phân quyền ở service, JSON lỗi theo ProblemDetail; session và CSRF giống giao diện.

## JPA và số lượng file

Dự án **đang dùng Spring Data JPA**. Bảy interface kế thừa JpaRepository đã có vẫn được sử dụng. Các giao dịch nhiều bảng dùng thêm EntityManager qua JpaEntityRepository để giữ khóa ghi, persist, flush và refresh giá trị do trigger thiết lập. Đây là repository tùy chỉnh, không phải thay thế JPA. Một phần JPQL còn nằm trong service; nếu phát triển lớn hơn, có thể chuyển từng nhóm truy vấn sang repository chuyên biệt khi cần tối ưu hoặc tái sử dụng.

Không cần đổi `entity` thành `model` chỉ để giống mẫu; cả hai cách đặt tên đều hợp lệ, entity nói rõ mục đích ánh xạ database. Dự án có 22 bảng (gồm hàng đợi email kết quả) nên nhiều entity/DTO hơn Todo là bình thường. File nhỏ có trách nhiệm rõ dễ tìm hơn một file hàng trăm dòng chứa tất cả nghiệp vụ.

URL giao diện và API dùng tiếng Anh theo [URLS.md](URLS.md); các trang cũ có chuyển hướng GET, còn form/API dùng URL mới. Tên trường DTO và schema MySQL giữ nguyên. Refactor không nhập lại dữ liệu mẫu, không đổi mật khẩu, không xóa nhóm/báo cáo/điểm đã nhập. `04_workflow_mysql.sql` là tên migration lịch sử; giữ tên để không làm hỏng quy trình cài database.
