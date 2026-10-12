# Đối chiếu TodoList và hệ thống quản lý đề tài sinh viên

Ngày đánh giá: 01/10/2026. Phạm vi: đọc đủ 10 trang `Lab 3 - Spring Data JPA Basic.pdf`, source TodoList tại `D:/project_web/todolist/todolist`, đề bài đồ án và source hiện tại tại `D:/project_web/final_projects`. Đây là đánh giá tĩnh sau lần tách controller/API; không thay đổi code nghiệp vụ hoặc database và không chạy lại bộ kiểm thử trong lượt đánh giá này.

## 1. Bài TodoList dạy điều gì?

Lab 3 tiếp nối giao diện Thymeleaf của Lab 1 và REST API của Lab 2. Nội dung chính là chuyển lưu trữ Todo từ ArrayList/AtomicLong sang MySQL bằng Spring Data JPA:

- Thêm dependency Spring Data JPA và MySQL Connector.
- Cấu hình datasource và Hibernate tạo/cập nhật schema trong môi trường bài lab.
- Ánh xạ Todo bằng @Entity, @Table, @Id, @GeneratedValue.
- TodoRepository kế thừa JpaRepository<Todo, Long>.
- TodoService sử dụng repository và @Transactional cho create/read/update/delete.
- Hai controller dùng chung TodoService, không cần đổi khi thay cơ chế lưu trữ.
- Kiểm tra một dữ liệu xuất hiện trên HTML, REST và MySQL; dữ liệu vẫn còn sau restart.

Source TodoList được cung cấp còn có User, UserRepository, CustomUserDetailsService và SecurityConfig với USER/ADMIN, form login và HTTP Basic. Các phần này nằm ngoài trọng tâm lưu trữ của PDF Lab 3. TodoRestController hiện trả trực tiếp entity Todo; test hiện có chỉ kiểm tra contextLoads. Service xử lý danh sách Todo chung, chưa lọc Todo theo chủ sở hữu.

## 2. Dự án hiện tại đã đủ tầng chưa?

**Có đủ ba tầng chính của ứng dụng phân lớp: giao tiếp, nghiệp vụ, truy cập dữ liệu.** Entity, DTO, security và exception là các thành phần hỗ trợ; không cần coi mỗi thư mục là một tầng độc lập. Mức độ đóng gói giữa các tầng vẫn còn cần cải thiện.

| Thành phần | TodoList | Quản lý đề tài hiện tại | Nhận xét |
|---|---|---|---|
| Giao diện / giao tiếp HTTP | TodoController + templates, TodoRestController | Controller HTML theo nghiệp vụ + controller/api/v1 + templates | Có đủ; hai loại controller gọi cùng service |
| Dữ liệu vào/ra | TodoRequest; API trả Todo | Request/Response như DeTaiRequest, DeTaiResponse, NhomResponse | Có; response tránh trả entity/hash/đồ thị JPA |
| Nghiệp vụ | TodoService CRUD | DeTaiService, DotDangKyService, NhomService, BaoCaoService, HoiDongService, DiemService, TaiKhoanService, ThongBaoService | Có; chứa kiểm tra quyền, thời gian, trạng thái và transaction |
| Truy cập dữ liệu | TodoRepository extends JpaRepository | Các interface Spring Data JPA + JpaEntityRepository dùng EntityManager | Có; còn JPQL trong service và EntityManager trực tiếp ở DotDangKyService |
| Mô hình lưu trữ | Todo/User trong model | 21 entity bảng trong entity; quan hệ và khóa ghép | Có; tên entity/model không làm thay đổi vai trò |
| Bảo mật | USER/ADMIN, form login, Basic; bỏ CSRF cho API | SV/GV/Admin, quyền Trưởng khoa, kiểm tra quan hệ đối tượng, session và CSRF cho cả API | Có; phục vụ nghiệp vụ nhiều vai trò |
| Lỗi | TodoNotFoundException | BusinessException, GlobalExceptionHandler, ApiExceptionHandler, lỗi filter JSON | Có; nên phân biệt lỗi không tìm thấy với lỗi quy tắc nghiệp vụ |
| Lưu file / hạ tầng | Không có báo cáo | ReportStorageService, SupabaseStorageService, ReportFileValidator | Có; cần thiết cho báo cáo riêng tư |
| Kiểm thử và vận hành | contextLoads; bước kiểm tra thủ công trong lab | Bộ test nghiệp vụ/HTTP/storage, SQL, Docker/Compose, CI, profile dev/prod, health | Có cấu hình và kiểm chứng local; cloud và CI thực tế không mặc nhiên được xác nhận |

Luồng ví dụ:

```text
Form HTML -> DeTaiController -----+
                                 +-> DeTaiService -> repository/JPA -> MySQL
JSON API  -> DeTaiRestController -+
```

Trong DeTaiService.create, hệ thống lấy GV từ phiên đăng nhập, kiểm tra thời gian của đợt, xác nhận bộ môn và 1–2 GVHD, rồi tạo DE_TAI và GVHD_DETAI trong một transaction. Đây là xử lý nghiệp vụ nhiều bảng, vượt phạm vi CRUD một Todo.

Không cần thêm interface cho mọi service, Mapper riêng cho mọi DTO, DomainService hay microservice chỉ để đủ tên tầng. Chỉ thêm khi có trách nhiệm rõ hoặc nhu cầu tái sử dụng/thay thế thực tế.

## 3. Những phần đã mở rộng so với bài basic

| Hướng mở rộng | TodoList | Hiện tại |
|---|---|---|
| Dữ liệu | Hai entity Todo và User trong source cung cấp | 22 bảng/entity: tài khoản, GV, SV, bộ môn, đợt, đề tài, nhóm, lời mời, đăng ký, báo cáo, hội đồng, điểm, kết quả, thông báo, email kết quả và các liên kết |
| Quy trình | Tạo/sửa/xóa việc; completed true/false | Tạo đợt -> GV đề xuất -> duyệt/công bố đề tài -> nhóm đăng ký -> duyệt nhóm -> báo cáo -> phân công/chấm -> tổng hợp -> công bố kết quả |
| Thời gian | dueDate là dữ liệu ngày | Cửa sổ GV/SV riêng; bốn loại đợt; quy định trường deadline GVPB và ngày hội đồng theo loại; kiểm tra hạn phản biện |
| Nhóm | Không có | Tối đa 3 SV, một nhóm trưởng, lời mời phải được chấp nhận, một nhóm/SV trong cùng đợt |
| Đăng ký | CRUD bản ghi độc lập | Chờ duyệt, chấp thuận, từ chối có lý do, rút đăng ký và giải phóng giữ chỗ |
| Hội đồng / điểm | Không có | Hội đồng 3–5 GV, chủ tịch/thư ký khác nhau; GVHD không chấm đề tài mình hướng dẫn; phản biện độc lập với hội đồng; điểm 0–10, tối đa 2 số thập phân; đủ phiếu mới tổng hợp, trung bình cộng và làm tròn 2 số |
| Phân quyền dữ liệu | Người đăng nhập dùng chung danh sách Todo | SV xem nhóm/kết quả của mình; GV xem nội dung được phân công; Trưởng khoa duyệt/công bố; Admin quản lý tài khoản |
| Báo cáo | Không có | PDF/DOCX/ZIP, giới hạn 20 MiB, phiên bản nộp, token chống gửi lặp, chỉ trưởng nhóm nộp, khóa báo cáo khi bắt đầu chấm, tải theo quyền |
| Giao dịch cạnh tranh | CRUD cơ bản trong transaction | Khóa ghi và UNIQUE để hạn chế hai nhóm cùng giữ một đề tài; nhiều bản ghi ghi cùng transaction; dọn file khi transaction database rollback |
| Tài khoản | Hai tài khoản mẫu hardcode trong SecurityConfig | Tạo, khóa/mở, reset/đổi mật khẩu; BCrypt; thu hồi chức Trưởng khoa và làm hết phiên khi credential thay đổi |
| API | CRUD Todo trả entity và id tạo mới | API v1 theo từng nghiệp vụ, DTO và lỗi JSON; dùng phiên + CSRF; một số endpoint tạo trả 201 rỗng |
| Dữ liệu mẫu | initUsers trong ứng dụng | JSON ngoài JAR; công cụ nhập riêng ở test classpath; không tự chạy khi mở web |
| Database / đóng gói | ddl-auto=update trong bài lab | ddl-auto=validate, SQL 01–06, constraint/trigger/view, volume MySQL/file, Docker và cấu hình cloud |

Hỗ trợ bốn loại đợt không có nghĩa đã có bốn quy trình cấu hình tùy ý. Phần lớn service hiện dùng chung luồng đăng ký/chấm; sự khác biệt theo loại tập trung ở trường lịch và kiểm tra liên quan.

## 4. Những điểm chưa đạt mức vận hành lâu dài

### Ưu tiên trước khi mở rộng dữ liệu

1. **Đóng gói truy vấn theo nghiệp vụ.** JpaEntityRepository hiện là wrapper dùng chung; JPQL vẫn xuất hiện ở DeTaiService, NhomService, DiemService... DotDangKyService dùng trực tiếp EntityManager để persist. Có tầng truy cập dữ liệu nhưng chưa giấu đầy đủ chi tiết truy vấn. Chuyển dần sang repository chuyên biệt, giữ semantics persist/lock/flush/refresh đang cần cho trigger và cạnh tranh.
2. **Phân trang và truy vấn theo quyền ngay trong database.** BaoCaoService.assigned tải tất cả báo cáo rồi lọc bằng Java. DeTaiService.view chạy nhiều truy vấn cho từng đề tài; tương tự với nhóm/hội đồng. Có thể phát sinh lượng truy vấn tăng theo số bản ghi. Chưa đo benchmark nên không khẳng định thời gian chậm cụ thể.
3. **Chuẩn hóa hợp đồng API.** POST tạo thường trả 201 rỗng; Todo basic trả bản ghi có id. Nên trả response có id/Location cho client dễ sử dụng, bổ sung phân trang, mô tả API và mã lỗi. Bản ghi không tồn tại hiện có thể trở thành BusinessException rồi trả 400; nên tách ResourceNotFoundException -> 404 khi phù hợp.

### Ưu tiên trước khi khoa dùng thực tế

4. **Rà soát trách nhiệm giữa Java và SQL trigger.** Nhiều ràng buộc được kiểm tra ở cả service và database. Cần ghi rõ điều kiện nghiệp vụ nào do service quyết định, constraint nào bảo vệ toàn vẹn dữ liệu, giữ hai phía đồng nhất khi đổi quy định. Đây là yêu cầu bảo trì, không phải thiếu một tầng mới.
5. **Quản lý migration và trạng thái.** SQL hiện được nạp theo script thủ công/init container, chưa có Flyway/Liquibase quản lý version tự động. Nhiều trạng thái dùng chuỗi; enum/hằng số và bảng chuyển trạng thái có thể giảm lỗi sửa code.
6. **Nhật ký và vận hành.** Có một số trường người/thời điểm duyệt, nộp, tổng hợp; chưa có audit log tổng quát cho mọi thay đổi quan trọng. Cần kiểm chứng backup/restore, upload Supabase thật, giám sát và môi trường cloud trước khi nhận dữ liệu thật của khoa.
7. **Chốt cách hiểu quy định với khoa.** Một nhóm duy nhất hiện được giới hạn theo đợt đăng ký. Đề bài viết một nhóm trong suốt quá trình thực hiện đề tài. Cần thống nhất cách áp dụng khi nhiều đợt diễn ra đồng thời và các trường hợp học lại/chuyển nhóm. Không tự mở rộng nghĩa của quy định chỉ từ tên field.

## 5. Giới hạn kết luận

Dự án có đủ nền tảng phân lớp và đã mở rộng đáng kể thành hệ thống xử lý quy trình học thuật. Điều này không đồng nghĩa mọi quy định thực tế của khoa đã được nghiệm thu hay mọi tải đồng thời đã được kiểm chứng.

Theo nhật ký kiểm chứng gần nhất, có 48 ca được báo cáo: 47 đạt, 1 bỏ qua do chưa cấu hình MySQL kiểm thử riêng ở lần refactor; 21 ca luồng/API được chạy lại sau sửa phản hồi multipart. Source TodoList hiện chỉ có test contextLoads. Không chạy lại test hoặc khởi động lại Docker trong lượt đánh giá này. Chi tiết kiểm chứng tại VERIFY_HUY.md.

Chat, email/SMS tự động, SSO, ứng dụng di động, AI, phát hiện đạo văn, đa khoa/đa trường và cấu hình quy trình tùy ý vẫn thuộc giai đoạn sau.
