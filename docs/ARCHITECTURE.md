# Kiến trúc và cấu trúc source

Ứng dụng đơn khối dùng Spring MVC, Spring Data JPA, Spring Security và Thymeleaf. `@Controller` phục vụ trang HTML; `@RestController` cung cấp JSON cho JavaScript hoặc client API. Hai lớp controller dùng chung service và quy tắc nghiệp vụ.

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
  entity/                 ánh xạ các bảng MySQL
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

## Phân chia nghiệp vụ

- Controller chia theo đề tài, nhóm, báo cáo, hội đồng, điểm, tài khoản và thông báo. Trang tổng hợp duyệt dùng DuyetDeTaiController.
- DTO riêng theo chức năng như DeTaiRequest, DeTaiResponse, NhomRequest, NhomResponse. Một DTO có thể dùng cả form và API nếu cùng quy tắc kiểm tra.
- CurrentUserService lấy/kiểm tra người thao tác; AcademicRules kiểm tra thời gian và điều kiện; ReferenceDataService đọc danh mục; JpaEntityRepository giữ các thao tác JPA cần cho giao dịch nhiều bảng.
- `config` chứa cấu hình ứng dụng. Importer dữ liệu thực hành ở test classpath, chạy bằng tools/Import-SampleData.ps1, không có trong JAR triển khai.
- REST có tiền tố `/api/v1`, phân quyền ở service, JSON lỗi theo ProblemDetail; session và CSRF giống giao diện.

## Tầng truy cập dữ liệu

Dự án **đang dùng Spring Data JPA** với chín interface repository, gồm chức vụ Trưởng bộ môn. Các giao dịch nhiều bảng dùng thêm EntityManager qua JpaEntityRepository để giữ khóa ghi, persist, flush và refresh giá trị do trigger thiết lập. Đây là repository tùy chỉnh, không phải thay thế JPA. Một phần JPQL còn nằm trong service; nếu phát triển lớn hơn, có thể chuyển từng nhóm truy vấn sang repository chuyên biệt khi cần tối ưu hoặc tái sử dụng.

Package `entity` chứa 24 lớp ánh xạ các bảng sau SQL 01–09, gồm EMAIL_KET_QUA cho hàng đợi email kết quả. DTO tách dữ liệu giao tiếp HTTP khỏi entity lưu trữ.

URL giao diện và API dùng tiếng Anh theo [URLS.md](URLS.md); các trang cũ có chuyển hướng GET, còn form/API dùng URL mới. Nâng cấp schema phải bảo toàn tài khoản, nhóm, báo cáo và điểm đã có, không nhập lại dữ liệu mẫu để ghi đè dữ liệu vận hành. `04_workflow_mysql.sql` là tên migration lịch sử, giữ nguyên để tương thích quy trình cài database.

Danh sách đề tài fetch đợt/bộ môn trong truy vấn chính và tải GVHD, GVPB, hội đồng, nhóm, giữ chỗ và xác nhận bộ môn theo tập mã để tránh truy vấn lặp theo từng dòng. Kiểm thử 1 và 21 đề tài cùng 9 SQL; số liệu này không thay cho nghiệm thu tải production. Email kết quả có tìm/lọc/phân trang tại repository, HTML và REST dùng cùng ResultEmailService. Các danh sách khác chủ yếu tìm kiếm trong tập dữ liệu đã giới hạn theo quyền, chưa phân trang toàn bộ module.
