# Phần báo cáo của Huy

Hệ thống quản lý đề tài sinh viên của khoa CNTT. Nội dung gồm Chương 1, Chương 2 và phụ lục cài đặt theo phân công của nhóm. Phạm vi thực hiện của Huy là nền tảng backend, bảo mật, ánh xạ dữ liệu, lưu trữ file và chuẩn bị triển khai. Luồng bản đầu đã tích hợp và có kiểm thử tự động; đường dẫn online và ảnh giao diện cần cập nhật sau khi nhóm nghiệm thu local và triển khai.

## Chương 1 Tổng quan

### 1.1 Bối cảnh và vấn đề

Hoạt động tổ chức đề tài sinh viên gồm nhiều bước liên quan: mở đợt, đề xuất đề tài, phê duyệt, lập nhóm, đăng ký, nộp báo cáo, phân công giảng viên đánh giá và công bố kết quả. Khi các bước được quản lý bằng nhiều bảng tính hoặc trao đổi rời rạc, người tham gia khó biết trạng thái mới nhất và người quản lý khó đối chiếu quyền thực hiện từng thao tác.

Nhóm xây dựng website để tập trung quy trình và dữ liệu trong phạm vi một khoa. Sinh viên, giảng viên, Trưởng khoa và quản trị viên làm việc trên cùng một hệ thống với quyền khác nhau. Một thao tác hợp lệ cần thỏa mãn cả quyền người dùng, quan hệ với dữ liệu và trạng thái nghiệp vụ. Ví dụ, tài khoản có vai trò giảng viên chưa đủ điều kiện tổng hợp điểm của mọi đề tài; người thực hiện phải là Chủ tịch hội đồng tương ứng.

### 1.2 Mục tiêu

Mục tiêu của đồ án là triển khai luồng quản lý đề tài trên web theo đề bài, bảo đảm dữ liệu được lưu nhất quán và người dùng nhận được thông báo khi thao tác không hợp lệ. Sản phẩm cần cài đặt lại được từ mã nguồn, có dữ liệu mẫu phục vụ trình diễn và có môi trường online để giảng viên kiểm tra.

Định hướng sau môn học là thử nghiệm tại khoa. Việc vận hành chính thức cần xác nhận quy chế đăng ký, công thức điểm, chính sách dữ liệu và quy trình hỗ trợ với người phụ trách nghiệp vụ. Bản đồ án không tự được coi là hệ thống đã vận hành thực tế khi chưa có các bước xác nhận này.

### 1.3 Phạm vi và người sử dụng

Sinh viên sử dụng chức năng nhóm, đăng ký đề tài, nộp báo cáo và xem kết quả của mình. Giảng viên đề xuất hoặc hướng dẫn đề tài và đánh giá khi được phân công. Trưởng khoa là chức vụ của một giảng viên, phụ trách tổ chức đợt và các bước phê duyệt/công bố. Quản trị viên quản lý tài khoản và các chức năng quản trị được giao; quyền Admin không tự thay thế quyền học thuật của Trưởng khoa.

Phạm vi bản đầu giới hạn ở một khoa và các loại đợt trong đề bài. Chat, SMS, SSO, ứng dụng di động, AI và phát hiện đạo văn là hướng mở rộng sau này, không dùng làm điều kiện hoàn thành bản đầu.

### 1.4 Phân công và phương pháp thực hiện

Huy phụ trách cấu hình Spring Boot, entity/repository, xác thực, quyền truy cập theo URL, adapter storage, xử lý lỗi dùng chung và cấu hình triển khai. Quỳnh phụ trách service nghiệp vụ, DTO/controller và phân quyền dữ liệu. Khải phụ trách giao diện và phối hợp ghép Thymeleaf. Khánh và Nam phụ trách schema, trigger, dữ liệu mẫu và các phần kiểm thử/báo cáo cơ sở dữ liệu theo kế hoạch nhóm.

Nhóm triển khai theo MVC phân tầng. Mỗi chức năng được kiểm tra từ dữ liệu đầu vào, quyền thao tác đến kết quả lưu trữ. Các thay đổi được quản lý bằng Git. Kiểm thử tự động và hướng dẫn chạy lại giúp phát hiện lỗi khi tích hợp phần việc của nhiều thành viên.

## Chương 2 Cơ sở lý thuyết và áp dụng

### 2.1 Kiến trúc MVC và phân tầng

Model biểu diễn dữ liệu và trạng thái xử lý; View trình bày HTML bằng Thymeleaf; Controller nhận request, kiểm tra form và chọn view hoặc redirect. Trong dự án, nghiệp vụ được tách thêm sang Service, còn Repository thực hiện truy cập dữ liệu thông qua Spring Data JPA.

Luồng tạo đợt minh họa: trình duyệt gửi form có token CSRF; Spring Security kiểm tra phiên và quyền Trưởng khoa; Controller xác thực DotDangKyForm; Service kiểm tra chức vụ, thời gian và mã đợt; EntityManager thực hiện INSERT; Controller redirect và hiển thị kết quả. Người tạo đợt lấy từ AccountPrincipal của phiên đăng nhập để tránh giả mạo bằng trường form.

### 2.2 IoC và Dependency Injection

Inversion of Control giao việc tạo và quản lý đối tượng cho Spring container. Dependency Injection đưa các phụ thuộc cần thiết vào đối tượng qua constructor. Ví dụ, DotDangKyService nhận DotDangKyRepository, BusinessSecurityService và EntityManager qua constructor. Controller không tự mở kết nối JDBC hoặc tự tạo service mới.

Cách tổ chức này làm rõ trách nhiệm từng lớp, cho phép thay thế adapter khi kiểm thử và giảm phụ thuộc vào chi tiết khởi tạo. Các bean không lưu trạng thái riêng của người dùng trong field dùng chung; thông tin người dùng nằm trong phiên và được truyền vào từng thao tác.

### 2.3 Spring MVC và Thymeleaf

Spring MVC ánh xạ đường dẫn HTTP tới phương thức controller bằng các annotation như GetMapping và PostMapping. Dữ liệu nhập được binding vào FormDTO. BindingResult chứa lỗi để trả lại form cho người dùng sửa, còn RedirectAttributes mang thông báo qua một lần redirect sau thao tác thành công.

Thymeleaf hiển thị dữ liệu bằng th:text và lặp danh sách bằng th:each. Layout dùng chung giữ cấu trúc điều hướng nhất quán. Menu theo role giúp giao diện phù hợp người dùng, nhưng bảo vệ thực sự vẫn nằm ở Spring Security và service. Hệ thống không truyền entity trực tiếp ra view; ViewDTO chứa đúng các trường cần trình bày.

### 2.4 Spring Data JPA và Hibernate

JPA quy định ánh xạ đối tượng Java tới cơ sở dữ liệu quan hệ. Hibernate là implementation được Spring Boot sử dụng trong dự án. Entity ánh xạ bảng, Column chỉ rõ tên cột và Id biểu diễn khóa chính. Năm bảng liên kết có khóa ghép được ánh xạ bằng IdClass. Spring Data JPA cung cấp JpaRepository để giảm mã lặp cho truy vấn thông thường.

Nền tảng ánh xạ 22 bảng; giữ 8 interface Spring Data JPA có sử dụng và dùng JPA EntityManager cho các giao dịch khác. Lớp ánh xạ view không còn được gọi đã bỏ, nhưng bốn SQL view vẫn giữ trong database. Quan hệ ManyToOne khai báo LAZY, không dùng Lombok Data và không cascade xóa dữ liệu học thuật. Equals/hashCode chỉ sử dụng loại entity và khóa định danh, không truy cập đồ thị quan hệ. Cột FK được ghi qua trường scalar; quan hệ điều hướng tương ứng chỉ đọc. EMAIL_KET_QUA giữ thông báo công bố điểm gửi tới từng sinh viên.

Open Session in View được tắt. Service tải đủ dữ liệu và chuyển sang DTO trong transaction; danh sách đề tài dùng EntityGraph để lấy bộ môn cùng truy vấn. Hibernate dùng ddl-auto=validate để đối chiếu schema, còn SQL của nhóm quản lý bảng/trigger. Không dùng update hoặc create trên database đang vận hành.

### 2.5 Transaction và ràng buộc dữ liệu

Transaction nhóm các thay đổi liên quan để commit hoặc rollback cùng nhau. Các kiểm tra nghiệp vụ ở Java cung cấp phản hồi sớm; khóa ngoại, CHECK, UNIQUE và trigger ở database là lớp bảo vệ dữ liệu khi có nhiều nguồn ghi hoặc nhiều thao tác đồng thời.

Với khóa tự gán, JpaRepository.save có thể thực hiện merge khi đối tượng đã có ID. Vì vậy thao tác tạo đợt sử dụng persist để biểu đạt INSERT, tránh cập nhật nhầm đợt tồn tại. Kiểm tra exists trước INSERT giúp báo lỗi sớm nhưng không thay thế ràng buộc khóa chính khi hai request đến cùng lúc.

### 2.6 Spring Security

CustomUserDetailsService tải tài khoản từ TAI_KHOAN và BCryptPasswordEncoder so khớp mật khẩu với hash. Không lưu mật khẩu rõ trong mã nguồn hoặc database. Sau đăng nhập, Spring Security quản lý phiên và đổi session ID để giảm rủi ro session fixation. Logout dùng POST với CSRF, hủy session và cookie.

Lớp URL giới hạn các tuyến sv, gv, truong-khoa và admin. Các tuyến ngoài danh sách được từ chối mặc định. AccountStatusFilter đọc lại trạng thái tài khoản và quyền từ database ở mỗi request; khóa tài khoản sẽ vô hiệu hóa phiên hiện có, thu hồi chức vụ sẽ loại quyền Trưởng khoa ở request tiếp theo.

Lớp dữ liệu kiểm tra quan hệ cụ thể như nhóm trưởng, GVHD hoặc Chủ tịch hội đồng. ID của người thao tác phải lấy từ principal đáng tin cậy, không dùng một mã do client tự gửi để nhận diện người dùng. Các predicate nền tảng cần được kết hợp với quy tắc thời hạn và trạng thái trong service nghiệp vụ.

### 2.7 Bean Validation và xử lý lỗi

Bean Validation mô tả yêu cầu đầu vào bằng NotBlank, NotNull, Size và Pattern. DotDangKyForm áp dụng các ràng buộc này; Controller dùng Valid và BindingResult để trả lỗi. Service tiếp tục kiểm tra quan hệ thời gian giữa các giai đoạn và điều kiện theo loại đợt.

Lỗi trigger MySQL có thể bị bọc trong nhiều lớp exception. DatabaseErrorMessages duyệt cause và nextException, chỉ lấy thông báo của SQLState 45000 do trigger nghiệp vụ phát ra. Lỗi kỹ thuật khác được thay bằng thông báo chung. GlobalExceptionHandler trả mã HTTP phù hợp và không đưa stacktrace hoặc SQL vào HTML.

### 2.8 Lưu trữ báo cáo

SupabaseStorageService đóng vai trò adapter gọi Storage API. File được kiểm tra dung lượng nhỏ hơn 20 MiB, phần mở rộng PDF/DOCX/ZIP và dấu hiệu cấu trúc nội dung. File nén được kiểm tra trong giới hạn số mục và tổng dung lượng đọc; không giải nén ra đĩa. Cơ chế này không thay thế antivirus.

Object key dùng UUID thay tên file của người dùng. Bucket private; sau khi service nghiệp vụ xác nhận quyền xem, adapter tạo signed URL tồn tại 300 giây. Database lưu object key thay URL công khai hoặc URL đã ký. Khi upload thành công nhưng lưu báo cáo vào database thất bại, BaoCaoService dùng transaction callback để dọn object khi rollback; nếu xóa thất bại, ghi object key để đối soát.

### 2.9 Docker và CI

Dockerfile build JAR bằng Maven/JDK 21 rồi chạy bằng JRE 21 dưới user không phải root. Dữ liệu MySQL và file báo cáo nằm ngoài container ứng dụng. Docker Compose cung cấp MySQL local với named volume và nạp script khi volume được tạo lần đầu.

GitHub Actions được cấu hình chạy test H2, nạp SQL vào MySQL 8.0 riêng, chạy kiểm thử MySQL và build Docker image. Render Blueprint mô tả các biến môi trường và chỉ tự deploy sau khi checks đạt. Cấu hình CI/CD cần một lần chạy thực tế trên tài khoản nhóm trước khi được ghi nhận là đã nghiệm thu.

### 2.10 Công bố kết quả và gửi email

Theo rubric cập nhật, công bố kết quả gồm tra cứu theo đợt và thông báo tới từng sinh viên trong nhóm. DiemService cập nhật trạng thái kết quả và lưu các thông báo EMAIL_KET_QUA trong cùng transaction. Nếu lưu thông báo thất bại, công bố cũng rollback. Worker lấy các thông báo đã commit và gọi dịch vụ gửi ngoài transaction để không giữ khóa database khi chờ mạng.

Local sử dụng JavaMailSender qua SMTP tới Mailpit; cấu hình cloud dùng HTTPS API Brevo. Worker thử lại có giới hạn và khôi phục công việc đang xử lý sau khi tiến trình dừng. Trưởng khoa xem trạng thái và thử lại thư thất bại. Dịch vụ chấp nhận thư chưa phải bằng chứng thư tới Inbox; cần kiểm tra hộp thư và log nhà cung cấp khi nghiệm thu online. Trường hợp phản hồi gửi bị mất có thể tạo thư trùng khi retry.

Nhóm đang lập có thể có 1–2 người; khi đăng ký đề tài cần 3–5 sinh viên đã xác nhận và đúng một nhóm trưởng. Service và trigger được đồng bộ qua migration 06. Dữ liệu lịch sử trước migration được giữ, không tự gửi lại kết quả đã công bố.

## Phụ lục Cài đặt và triển khai

Mã nguồn backend ở projects/quanlydetai. Môi trường chuẩn là JDK 21 và MySQL 8.0.16 trở lên. Nạp lần lượt sáu script 01–06 trong projects/Database, cấu hình DB_URL, DB_USERNAME, DB_PASSWORD và chạy bằng tools/Start-Local.ps1 để đọc đúng file môi trường. Bộ hồ sơ/đề tài giả lập nằm trong JSON ngoài Java, không phải dữ liệu thật của HCMUTE; profile prod không nạp. Thông tin chi tiết, lệnh tạo BCrypt và cách dùng Compose được ghi trong README ở root repository và SAMPLE_DATA.md.

Hướng dẫn tạo Aiven MySQL, Supabase bucket private, cấu hình TLS và triển khai Render nằm trong docs/CLOUD_SETUP.md. Quy ước service/entity/storage để tích hợp nhóm nằm trong docs/HUY_HANDOFF.md. Nhật ký kiểm chứng nằm trong docs/VERIFY_HUY.md. Đường dẫn demo và thời điểm nghiệm thu online được bổ sung sau khi triển khai thực tế.

## Tài liệu tham khảo

1. Spring Security UserDetailsService — https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html
2. Spring Security CSRF — https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
3. Spring Data JPA — https://docs.spring.io/spring-data/jpa/reference/
4. Supabase Storage buckets — https://supabase.com/docs/guides/storage/buckets/fundamentals
5. Render Docker — https://render.com/docs/docker
6. Render Blueprint specification — https://render.com/docs/blueprint-spec
7. Vercel Container Images — https://vercel.com/docs/functions/container-images
8. Aiven TLS certificates — https://aiven.io/docs/platform/concepts/tls-ssl-certificates
