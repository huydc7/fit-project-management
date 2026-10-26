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

Dự án **đang dùng Spring Data JPA** với 10 interface repository, gồm chức vụ Trưởng bộ môn và hàng đợi email kết quả. Các giao dịch nhiều bảng dùng thêm EntityManager qua JpaEntityRepository để giữ khóa ghi, persist, flush và refresh giá trị do trigger thiết lập. Đây là repository tùy chỉnh, không phải thay thế JPA. Một phần JPQL còn nằm trong service; nếu phát triển lớn hơn, có thể chuyển từng nhóm truy vấn sang repository chuyên biệt khi cần tối ưu hoặc tái sử dụng.

Package `entity` chứa 24 lớp ánh xạ các bảng sau SQL 01–10, gồm EMAIL_KET_QUA cho hàng đợi email kết quả. DTO tách dữ liệu giao tiếp HTTP khỏi entity lưu trữ.

URL giao diện và API dùng tiếng Anh theo [URLS.md](URLS.md); các trang cũ có chuyển hướng GET, còn form/API dùng URL mới. Nâng cấp schema phải bảo toàn tài khoản, nhóm, báo cáo và điểm đã có, không nhập lại dữ liệu mẫu để ghi đè dữ liệu vận hành. `04_workflow_mysql.sql` là tên migration lịch sử, giữ nguyên để tương thích quy trình cài database.

Danh sách đề tài fetch đợt/bộ môn trong truy vấn chính và tải GVHD, GVPB, hội đồng, nhóm, giữ chỗ và xác nhận bộ môn theo tập mã trên trang để tránh truy vấn lặp theo từng dòng. Các danh sách chính áp dụng bộ lọc và phân trang tại repository, sau đó service dựng DTO. Email kết quả tiếp tục dùng ResultEmailService cho HTML/REST. Chi tiết truy vấn và phạm vi dữ liệu được mô tả ở phần phân trang dưới.


## Truy vấn danh sách và phân trang

Controller chỉ nhận bộ lọc/trang và gọi một phương thức service. Service xác định phạm vi dữ liệu từ phiên đăng nhập (SV, GV phụ trách, bộ môn, Trưởng khoa); tham số từ URL chỉ thu hẹp phạm vi đó. Các `*QueryRepository` thực hiện JOIN/EXISTS, tìm kiếm, COUNT và LIMIT/OFFSET tại database. `QuerySupport` dùng chung việc gắn tham số và phân trang; không chứa chính sách học thuật.

Danh sách đề tài, tài khoản, báo cáo, đợt, thông báo, nhóm, lời mời, hội đồng, nhiệm vụ chấm và kết quả mặc định 25 dòng/trang, tối đa 100. DTO và các danh sách liên quan được dựng sau khi đã lấy trang. Tài khoản fetch hồ sơ SV/GV một lần; đề tài, nhóm và hội đồng nạp các liên kết theo tập mã trên trang. Trang Trưởng khoa có tham số trang độc lập cho đề tài và kết quả. Trang duyệt đăng ký nhóm của Trưởng khoa tại `/dean/registrations` có phân trang riêng; chỉ người có chức vụ Trưởng khoa đương nhiệm được xem danh sách và quyết định.

Tìm kiếm giữ cách so khớp chuỗi con tiếng Việt không phân biệt hoa/thường và dấu. Repository chuẩn hóa bằng REGEXP_REPLACE trong DB; `%`, `_`, `!` trong từ khóa được escape và từ khóa luôn được bind. Cách này dùng được trên MySQL 8 và H2 kiểm thử, không cần thêm cột dữ liệu hoặc nhập lại dữ liệu mẫu. LIKE chuỗi con và hàm chuẩn hóa không tận dụng B-tree cho chính từ khóa; không khẳng định mọi tìm kiếm đều dùng index. SQL 10 bổ sung index cho một số điều kiện bằng và thứ tự danh sách, cần dùng EXPLAIN với dữ liệu thực tế để đánh giá kế hoạch.

Nhiệm vụ chấm dùng COUNT trong truy vấn và chỉ hiển thị bản báo cáo mới nhất của mỗi nhóm. Lịch sử các phiên bản vẫn xem được ở trang báo cáo có phân trang. Việc tổng hợp điểm vẫn nạp các phiếu (giới hạn theo sĩ số hội đồng) để kiểm tra đúng người/loại phiếu và tính điểm; không thay kiểm tra nghiệp vụ bằng việc chỉ đếm phiếu.

Các danh mục nhỏ dùng làm lựa chọn (bộ môn, đợt, GV hoạt động) được nạp riêng; không dùng kết quả trang danh sách làm toàn bộ danh mục. Transaction, khóa ghi, UNIQUE và luồng email công bố giữ nguyên. Kiểm thử tải bản ghi, số câu SQL và đăng ký đồng thời nằm trong bộ test; chúng không thay thế kiểm thử tải thực tế.

Lựa chọn nhóm trên form đăng ký/nộp báo cáo được truy vấn riêng theo quyền và điều kiện hợp lệ; không lấy từ trang đầu của lịch sử nhóm. Vì vậy nhóm đang làm việc vẫn có thể được chọn ngay cả khi SV có hơn 25 nhóm ở các đợt khác.
