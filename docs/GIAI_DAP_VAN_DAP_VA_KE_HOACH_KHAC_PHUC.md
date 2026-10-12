# Giải đáp vấn đáp và kế hoạch khắc phục trước báo cáo

**Dự án:** Quản lý đề tài, đồ án sinh viên khoa FIT HCMUTE
**Ngày đối chiếu:** 06/10/2026
**Mốc báo cáo:** 26/10/2026, theo thông tin Huy đã cung cấp
**Trạng thái:** Đề xuất để Huy xem xét và phê duyệt. Chưa triển khai các bước khắc phục.

Tài liệu được đối chiếu với source, SQL, cấu hình test, báo cáo Surefire đang có và hai PDF đề bài. Lượt này chỉ đọc và tạo tài liệu mới; không sửa source/cấu hình, không chạy test, không thay đổi dữ liệu, không push hoặc deploy. Thông tin hosting được kiểm tra từ tài liệu chính thức tại thời điểm lập tài liệu.

## 1. Đánh giá lại bốn nhận xét

| Nhận xét | Kết luận sau đối chiếu | Cần làm tiếp |
|---|---|---|
| Code nâng cao nên dễ bị hỏi kiến trúc | Có cơ sở cần chuẩn bị vấn đáp; độ phức tạp tự nó không chứng minh sao chép. Một số mô tả kỹ thuật trong nhận xét chưa đúng source. | Giải thích được luồng thực tế, lý do lựa chọn và giới hạn; trình bày đúng phần nhóm thực hiện và tài liệu/công cụ đã tham khảo. |
| Test MySQL đang bị skip | Đúng với XML hiện có. Điều kiện bật test là biến `TEST_DB_DRIVER`, không phải việc web đã kết nối MySQL. | Chạy trên MySQL kiểm thử riêng, đặt `TEST_DDL_MODE=validate`, lưu bằng chứng mới. |
| Chưa có URL online được xác nhận | Hồ sơ đã đọc chưa cung cấp bằng chứng URL HTTPS hoạt động. Có Dockerfile, Blueprint và hướng dẫn không đồng nghĩa đã deploy thành công. | Triển khai sau khi duyệt; nghiệm thu URL và các luồng từ máy khác. |
| Render Free ngủ và mất session | Phù hợp với mô hình session hiện tại. Chuẩn bị trước demo là cần thiết, nhưng mở một tab rồi để yên không bảo đảm dịch vụ tiếp tục chạy. | Diễn tập khởi động lại, đăng nhập lại và kiểm tra dữ liệu/file vẫn còn; chuẩn bị phương án local dự phòng. |

**Ưu tiên:** Kiểm thử MySQL và bằng chứng deploy trước; ôn vấn đáp song song. Chưa nên viết lại kiến trúc chỉ để code trông đơn giản hơn.

## 2. Câu hỏi kiến trúc và câu trả lời đúng với code

### 2.1. Vì sao Trưởng khoa không là giá trị vai trò riêng trong TAI_KHOAN?

**Trả lời ngắn khi vấn đáp:**

> Tài khoản lưu vai trò nền tảng như sinh viên, giảng viên, quản trị viên. Trưởng khoa là chức vụ bổ sung của một giảng viên. Khi giảng viên đang giữ chức, hệ thống cấp thêm quyền Trưởng khoa; khi thôi giữ chức, người đó vẫn sử dụng tài khoản giảng viên cũ.

**Bằng chứng:** `CustomUserDetailsService` cấp `ROLE_GV` từ tài khoản và bổ sung `ROLE_TRUONGKHOA` khi bản ghi `TRUONG_KHOA` có `Dang_Giu_Chuc=true`. Vì vậy Trưởng khoa **vẫn là một authority trong Spring Security**, chỉ không là giá trị vai trò gốc trong `TAI_KHOAN`.

`AccountStatusFilter` đọc lại trạng thái/quyền ở request của người đã đăng nhập. Việc thu hồi chức vụ ảnh hưởng phiên hiện có mà không phải đổi tài khoản đăng nhập. `BusinessSecurityService` cũng kiểm tra chức vụ khi xử lý nghiệp vụ.

**Nếu bị hỏi sâu:** Cờ hiện tại chỉ mô tả có/không giữ chức. Schema chưa có ngày bắt đầu, kết thúc, số quyết định hay bảng lịch sử từng nhiệm kỳ. Không nên nói sản phẩm đã quản lý đầy đủ nhiệm kỳ. Giữ bản ghi cũ để các đợt đã tạo còn tham chiếu được cũng không đồng nghĩa đã có lịch sử bổ nhiệm hoàn chỉnh.

**Câu hỏi tự luyện:** Nếu quyền bị thu hồi trong lúc người dùng đang mở trang duyệt, request duyệt tiếp theo sẽ được xử lý thế nào? Hãy chỉ ra cả kiểm tra quyền phía server, không chỉ việc ẩn menu.

### 2.2. Vì sao có AcademicRules?

**Trả lời ngắn:**

> Lớp này dùng chung việc kiểm tra khoảng thời gian đăng ký, lấy thời gian theo múi giờ Việt Nam và kiểm tra điều kiện để ném lỗi nghiệp vụ nhất quán. Service gọi nó để tránh lặp lại các kiểm tra chung.

**Cần sửa cách diễn đạt trong nhận xét:** Source hiện tại có `studentWindow`, `teacherWindow`, `inWindow`, `now`, `check`, `id`. Đây chưa phải nơi tập trung toàn bộ hằng số hay toàn bộ quy chế học thuật. Điều kiện số lượng giảng viên, thành viên hội đồng và những ràng buộc khác vẫn có ở các service và SQL trigger.

**Nếu bị hỏi sâu:** Hai đầu khoảng thời gian được tính bao gồm: thời điểm bằng `begin` hoặc `end` vẫn được nhận. `now()` đang gọi đồng hồ hệ thống; chưa dùng `Clock` được truyền vào để giả lập thời gian trong test.

**Đề xuất sau này, chưa thực hiện:** Có thể gom các giới hạn dùng lặp và truyền `Clock` khi có nhu cầu bảo trì/kiểm thử rõ ràng. Chưa cần mở rộng thành bộ máy cấu hình quy chế trước báo cáo.

### 2.3. Vì sao tắt OSIV?

**Trả lời ngắn:**

> OSIV giữ persistence context mở đến giai đoạn xử lý view. Nhóm tắt nó để việc truy vấn và chuẩn bị dữ liệu diễn ra ở service trong transaction, rồi trả dữ liệu đã đủ cho controller/view. Cách này hạn chế truy vấn lazy phát sinh bất ngờ khi render HTML hoặc JSON.

**Bằng chứng:** `application.properties` đặt `spring.jpa.open-in-view=false`; service dùng transaction và các DTO/view dữ liệu.

**Điểm cần nói chính xác:** OSIV không mặc nhiên gây rò rỉ kết nối database trong mọi ứng dụng. Tắt OSIV cũng không tự động giải quyết N+1 hoặc ép Hibernate tải sẵn mọi quan hệ. Lập trình viên vẫn phải truy vấn đủ dữ liệu, dựng DTO trong phạm vi phù hợp và kiểm tra số truy vấn nếu có vấn đề hiệu năng.

**Câu hỏi tự luyện:** Nếu trả entity có quan hệ lazy rồi template đọc quan hệ sau khi transaction đã kết thúc, có thể gặp lỗi gì? Cách sửa phù hợp là điều chỉnh truy vấn/DTO, không bật lại OSIV theo thói quen.

### 2.4. Vì sao dùng pessimistic locking?

**Trả lời ngắn:**

> Khi hai request cùng thay đổi một đề tài hoặc nhóm, cả hai có thể đọc cùng trạng thái ban đầu rồi cùng quyết định thao tác hợp lệ. Nhóm khóa bản ghi cần bảo vệ trong transaction để tuần tự hóa các thao tác cạnh tranh, sau đó kiểm tra điều kiện và cập nhật.

**Bằng chứng:** `JpaEntityRepository.lock()` dùng `LockModeType.PESSIMISTIC_WRITE`. Các service như `NhomService`, `DeTaiService`, `DiemService` gọi phương thức này trong transaction.

**Giới hạn phải hiểu:** Khóa bản ghi không tự bảo vệ mọi quy tắc trên mọi bảng; còn cần chọn đúng bản ghi khóa, thứ tự khóa, constraint/trigger và xử lý lỗi. Khóa có thể làm request chờ hoặc gây deadlock. Không nên giữ transaction dài khi tải file hoặc gọi dịch vụ mạng. Một số thao tác khóa cả đợt nên có thể tuần tự hóa nhiều request hơn cần thiết; đây là điểm cần đo trước khi vận hành đông người.

**Chưa có bằng chứng để tuyên bố:** Việc nhìn thấy lệnh khóa không chứng minh mọi tình huống truy cập đồng thời đã được kiểm thử. Nên bổ sung một ca hai nhóm tranh đăng ký cùng đề tài nếu Huy duyệt phần thêm test.

### 2.5. Custom repository có nghĩa là dự án không dùng Spring Data JPA?

**Trả lời ngắn:**

> Dự án có dùng Spring Data JPA. Các repository theo entity, chẳng hạn `TaiKhoanRepository`, kế thừa `JpaRepository`. Lớp `JpaEntityRepository` là phần bổ sung dùng EntityManager cho những thao tác chung như tìm bắt buộc có dữ liệu, khóa bản ghi, persist, flush và refresh.

Tên lớp thực tế là `JpaEntityRepository`, không phải `CustomJpaEntityRepository`.

`insert()` hiện flush để đẩy lệnh xuống DB, rồi refresh để đọc lại giá trị DB/trigger có thể cập nhật. Đổi lại, repository tổng quát và chuỗi JPQL khiến truy vấn khó lần theo hơn các repository cụ thể. Khi vấn đáp cần mở được một truy vấn thật và giải thích tham số, thay vì chỉ nói đây là kiến trúc nâng cao.

**Không cần đổi ngay:** Chưa có bằng chứng lớp này gây lỗi để bắt buộc viết lại trước báo cáo. Refactor nếu được duyệt sau cần giữ hành vi và có kiểm thử đi kèm.

### 2.6. RFC 7807 / ProblemDetail dùng làm gì?

**Trả lời ngắn:**

> API dùng định dạng lỗi thống nhất để client biết mã HTTP và nội dung lỗi, thay vì mỗi endpoint trả JSON theo một kiểu. Dự án dùng ProblemDetail sẵn có của Spring cho phản hồi application/problem+json.

`ApiExceptionHandler` xử lý lỗi validation, thiếu quyền, lỗi DB và lỗi không dự kiến; lỗi không dự kiến có mã tra cứu thay vì trả stack trace xuống client.

**Cập nhật thuật ngữ:** RFC 9457 đã thay thế RFC 7807. Báo cáo nên ghi “Problem Details theo RFC 9457, kế thừa RFC 7807”. Đây là sử dụng hỗ trợ của framework, không phải nhóm tự xây một chuẩn giao thức mới. Nguồn: [IETF RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html), [Spring Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).

### 2.7. Vì sao Docker nhiều stage? Có Dynamic Script Engine không?

**Docker:** Stage đầu dùng Maven/JDK để build JAR. Stage chạy dùng JRE và JAR cần thiết, không mang theo toàn bộ công cụ build. Container chạy bằng user `app` thay vì root. Đây là tách môi trường build và chạy, không phải microservices.

Dockerfile dùng `-DskipTests` khi đóng gói; bản thân Docker build thành công không chứng minh test đã đạt. Workflow CI hiện tách bước kiểm thử rồi mới kiểm tra đóng gói Docker.

**Dynamic Script Engine:** Trong phạm vi source, tools và workflow đã tìm kiếm, chưa thấy `ScriptEngine`, `javax.script` hoặc thành phần có tên này. PowerShell hỗ trợ chạy local/import và SQL có procedure/trigger không tự biến ứng dụng thành hệ thống thực thi script động. Không đưa kỹ thuật này vào báo cáo nếu chưa chỉ ra được file và cách sử dụng cụ thể.

### 2.8. Ba câu hỏi bổ sung dễ bị hỏi

| Câu hỏi | Ý chính cần nắm |
|---|---|
| Tại sao controller MVC và REST cùng tồn tại? | Thymeleaf cần controller trả trang HTML; REST trả dữ liệu JSON. Cả hai gọi cùng service, không viết hai bộ luật nghiệp vụ độc lập. |
| Tại sao vừa kiểm tra ở service vừa có trigger? | Service kiểm tra quyền/quy trình và thông báo dễ hiểu; trigger bảo vệ tính hợp lệ khi dữ liệu được ghi từ nhiều đường. Cần tránh hai tầng dùng quy tắc mâu thuẫn. |
| Điểm cuối tính thế nào? | Đề gốc nêu trung bình cộng các điểm thành phần. `DiemService` hiện tổng hợp các phiếu cần thiết rồi tính trung bình. Không tuyên bố đây là công thức chuẩn của mọi trường; phải giải thích nhóm xác định “điểm thành phần” và làm tròn như thế nào. |

**Cách ôn:** Mỗi thành viên phải lần theo một thao tác từ controller → service → repository → DB → phản hồi; giải thích một đường thành công và một đường bị chặn. Huy tập trung bảo mật, phiên, cấu hình, lưu file và triển khai; các luồng nghiệp vụ/chấm điểm cần thành viên phụ trách cùng chuẩn bị. Không học thuộc câu trả lời trái với code.

## 3. Test MySQL bị skip: nguyên nhân và cách khắc phục đề xuất

### 3.1. Bằng chứng hiện có

- XML của `MySqlTriggerIntegrationTest` ghi 1 test, 1 skipped, 0 failure/error; file có thời điểm sửa 01/10/2026.
- Tổng các XML hiện có là 50 ca, 49 không bị skip, 1 skip, không có failure/error. Các file được tạo ở những lượt chạy khác nhau ngày 01–02/10; không được trình bày thành một lượt chạy mới ngày 06/10.
- Test được bật bởi `@EnabledIfEnvironmentVariable(named="TEST_DB_DRIVER", matches="com.mysql.cj.jdbc.Driver")`. Profile test mặc định chọn H2 nếu không truyền biến test.
- `Start-Local.ps1` đặt profile dev và gọi `spring-boot:run`; nó không đặt các biến `TEST_*` và không chạy Maven test.
- Có 35 tên trigger khác nhau được khai báo trong các file SQL. Đây là kết quả đọc source, chưa phải kiểm đếm trigger đã được cài trong database đang chạy.

### 3.2. Vì sao không chạy thẳng trên database demo?

`application-test.properties` mặc định `spring.jpa.hibernate.ddl-auto=create-drop`. Nếu chỉ đổi kết nối sang MySQL mà quên `TEST_DDL_MODE`, Hibernate có thể tạo/xóa các bảng được ánh xạ và ảnh hưởng dữ liệu. Có `@Transactional` không làm các thao tác DDL trở nên an toàn.

**Phương án đề xuất:** MySQL 8.0.46 kiểm thử riêng, ví dụ container khác trên cổng 3308, có database tên `quanlydetai` nhưng hoàn toàn tách với database demo trên cổng 3307. Tên giống nhau là để giữ nguyên SQL đang có `CREATE DATABASE/USE quanlydetai`; không thể cho rằng truyền tên DB khác qua JDBC sẽ đổi các câu `USE` trong script.

### 3.3. Trình tự sau khi Huy phê duyệt

1. Sao lưu DB và file báo cáo local đang sử dụng; xác định rõ MySQL demo và MySQL test.
2. Tạo MySQL test riêng. Chạy SQL 01–05 theo thứ tự; kiểm tra lỗi script, bảng, trigger, view và procedure. Không nhập dữ liệu thật.
3. Đặt `TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver`, `TEST_DDL_MODE=validate`, `TEST_DB_INIT_SQL=SET time_zone='+07:00'` và các biến kết nối test.
4. Chạy riêng `MySqlTriggerIntegrationTest`; sau khi đạt, chạy ba bộ tích hợp như workflow CI hiện có.
5. Lưu console log, XML, commit được test, phiên bản MySQL và kết quả kiểm đếm trigger. Chụp màn hình sau khi kiểm tra kết quả thực.
6. Nếu có lỗi, ghi lại để Huy duyệt phạm vi sửa; không xóa annotation hoặc bỏ assertion chỉ để hết màu vàng.

**Ví dụ lệnh dự kiến, chưa thực thi:** Chỉ dùng sau khi cổng 3308 đã được xác nhận là MySQL test riêng. Mật khẩu nhập qua hộp credential, không đưa vào báo cáo hay dòng lệnh.

```powershell
Set-Location D:\project_web\final_projects\projects\quanlydetai
$taskTestCredential = Get-Credential -UserName root -Message 'Tài khoản MySQL kiểm thử riêng trên cổng 3308'
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3308/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh'
$env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
$env:TEST_DB_USERNAME = $taskTestCredential.UserName
$env:TEST_DB_PASSWORD = $taskTestCredential.GetNetworkCredential().Password
$env:TEST_DDL_MODE = 'validate'
$env:TEST_DB_INIT_SQL = "SET time_zone='+07:00'"
try {
    .\mvnw.cmd -B -ntp '-Dtest=MySqlTriggerIntegrationTest' test
    # Chỉ chạy bước tiếp theo nếu lần đầu đạt và dữ liệu test còn phù hợp.
    if ($LASTEXITCODE -eq 0) {
        .\mvnw.cmd -B -ntp '-Dtest=FoundationIntegrationTest,WorkflowIntegrationTest,MySqlTriggerIntegrationTest' test
    }
} finally {
    # Dùng cửa sổ PowerShell riêng cho ví dụ này; bỏ cấu hình test khi hoàn tất.
    'TEST_DB_URL','TEST_DB_DRIVER','TEST_DB_USERNAME','TEST_DB_PASSWORD','TEST_DDL_MODE','TEST_DB_INIT_SQL' |
        ForEach-Object { Remove-Item -LiteralPath "Env:$_" -ErrorAction SilentlyContinue }
    $taskTestCredential = $null
}
```

### 3.4. “Tất cả xanh” chưa đủ chứng minh điều gì?

Test trigger hiện chỉ kiểm tra lỗi khi chuyển hội đồng chưa đủ thành viên sang `DaChot` và xem thông báo DB có đi qua Hibernate đúng hay không. Một test đạt không chứng minh 35 trigger đều đúng, không đo tải và không chứng minh mọi tình huống cạnh tranh.

H2 vẫn hữu ích cho unit/HTTP integration test. Một test đặc thù MySQL bị skip ở lượt H2 là có chủ đích; tiêu chí là nó **được chạy và đạt ở lượt MySQL riêng**, không phải ép mọi lượt H2 có zero skip. `.github/workflows/backend.yml` đã có bước MySQL tương ứng, nhưng chưa có bằng chứng workflow trên GitHub đã chạy thành công.

**Bổ sung test nếu được duyệt:** Hai nhóm tranh cùng đề tài; chốt hội đồng sai thành phần; giảng viên chấm đề tài mình hướng dẫn; sửa điểm/báo cáo khi đã công bố. Chọn theo lỗ hổng của kiểm thử hiện có, không tạo 35 test chỉ để tương ứng số trigger.

## 4. Deploy online: điều kiện đạt và kế hoạch

### 4.1. Yêu cầu thực sự của đề

PDF “Yêu cầu khi nộp đồ án cuối kỳ”, trang 1, mục 3d yêu cầu triển khai trên một hosting free. Đề không bắt buộc Render/Aiven và không bắt buộc đặt URL ở trang bìa. Đề xuất ghi URL và mã QR ở phần giới thiệu/phụ lục; thêm ở bìa nếu bố cục phù hợp.

**Phương án đề nghị cho kiến trúc hiện tại:** Render Free chạy Spring Boot/Thymeleaf trong Docker; Aiven MySQL giữ dữ liệu; Supabase bucket private giữ báo cáo. Không đổi framework hoặc tách frontend trong giai đoạn này.

Aiven hiện có MySQL Free, nhưng dịch vụ miễn phí có thể bị tắt khi thiếu hoạt động; phải kiểm tra trạng thái thực trong dashboard. Nguồn: [Aiven Service pricing](https://aiven.io/docs/platform/concepts/service-pricing).

### 4.2. Các bước sau phê duyệt

1. Huy xác nhận tài khoản cloud/repository và branch/commit sẽ dùng; kiểm tra diff hiện có trước khi push. Không đưa `.env`, mật khẩu mẫu đã chia sẻ trong chat hoặc secret vào hồ sơ công khai. Tạo tài khoản demo online với mật khẩu riêng.
2. Tạo MySQL cloud mới, cấu hình TLS xác minh CA/hostname; chạy SQL 01–05 và kiểm đếm các đối tượng. Xác minh quyền tạo trigger/procedure trên dịch vụ thật; nếu bị chặn, ghi rõ lỗi rồi đề xuất cách xử lý, không bỏ qua script.
3. Tạo bucket private. Dùng dữ liệu giả lập phù hợp FIT; giữ nguyên database local và dữ liệu người dùng đã nhập. Kiểm tra dung lượng và loại file phù hợp cấu hình ứng dụng.
4. Review Dockerfile, `render.yaml`, profile prod và biến môi trường. Prod hiện mặc định storage Supabase; đặt `STORAGE_BACKEND=supabase` tường minh trên hosting và kiểm tra adapter thực sự được dùng.
5. Deploy đúng commit đã qua kiểm tra. Theo dõi log khởi động, schema validation, HTTPS và health. Nếu lần chạy cloud phát hiện lỗi cần chỉnh code/cấu hình repository, trình phạm vi sửa cho Huy trước.
6. Nghiệm thu theo bảng dưới; lưu URL, commit, thời điểm kiểm tra và bằng chứng. Chỉ kết luận đã deploy khi URL hoạt động từ máy khác.

| Kiểm tra nghiệm thu | Bằng chứng cần có |
|---|---|
| URL public HTTPS và health | Mở trên trình duyệt khác/mạng khác; không chỉ localhost hoặc ảnh dashboard |
| Bốn vai trò | Admin, Trưởng khoa, GV, SV đăng nhập và vào đúng trang; thử truy cập sai quyền bị chặn |
| Một luồng nghiệp vụ hoàn chỉnh | Tạo đợt → đề xuất/duyệt → nhóm/đăng ký → báo cáo → phân công/chấm/tổng hợp → công bố; dùng đợt được tạo để demo đúng thời gian, không đổi đồng hồ hệ thống |
| Lưu báo cáo | Upload và tải được; người sai quyền bị chặn; URL ký hết hạn không dùng tiếp được |
| Sau restart/redeploy | MySQL và file còn; session cũ không được coi là hợp lệ nếu bị mất; đăng nhập lại hoạt động |
| Truy xuất bản triển khai | Commit SHA, CI/test log, cấu hình không chứa secret, hướng dẫn tái tạo |

**Không tự nâng cấp trả phí.** Nếu miễn phí không đủ tài nguyên/quyền DB, trình bằng chứng và lựa chọn thay thế để Huy quyết định. Một URL miễn phí đạt yêu cầu môn học chưa đủ để tuyên bố sản phẩm sẵn sàng vận hành chính thức trong khoa.

## 5. Cold start và session: giải đáp, vận hành demo

Render Free dừng dịch vụ sau 15 phút không có traffic nhận vào; đánh thức thường khoảng một phút. File local có thể mất khi dừng/restart/redeploy; Free không có persistent disk. Nguồn: [Render Free](https://render.com/docs/free).

**Suy luận từ code:** Dự án chưa cấu hình Spring Session/JDBC/Redis và session đang ở một instance. Khi tiến trình bị thay thế, session trong RAM không còn. Cookie trên trình duyệt không khôi phục được session; cần đăng nhập lại. Dữ liệu MySQL và object ở storage ngoài là hai phần độc lập với session.

**Phân biệt:** `server.servlet.session.timeout=30m` là thời hạn không hoạt động của phiên ứng dụng; nó không ngăn hosting dừng sau 15 phút. Secure/HttpOnly/SameSite và xoay session ID giúp bảo vệ cookie/phiên, không lưu phiên qua restart.

Supabase Free cũng có thể pause project hoạt động thấp trong khoảng 7 ngày; cần kiểm tra cả storage trước demo. Nguồn: [Supabase Production Checklist](https://supabase.com/docs/guides/deployment/going-into-prod).

### Kịch bản trước buổi bảo vệ

| Thời điểm | Việc chuẩn bị |
|---|---|
| Trước 1 ngày | Kiểm tra ba dịch vụ, lượt deploy, tài khoản, đợt đăng ký hợp lệ; sao lưu DB/file; thử toàn bộ luồng và phương án local |
| Trước 10–15 phút | Truy cập URL, đợi health và login hoạt động; kiểm tra đăng nhập và tải một báo cáo trên cloud |
| Trước khi thầy thao tác | Xác nhận lại dịch vụ còn đáp ứng; chuẩn bị phiên mới cho vai trò đầu tiên, thay vì dựa vào phiên cũ từ trước |
| Khi có sự cố | Ghi nhận màn hình/log; thử đăng nhập lại khi cần; nếu cloud không sẵn sàng thì dùng local/video để tiếp tục trình bày và giải thích đúng tình trạng URL |

Mở một tab nhưng không gửi request không phải là bảo đảm hoạt động. Các bước trên là chuẩn bị của người thuyết trình, không phải cam kết uptime. Nếu dịch vụ hoặc session mất giữa thao tác, kiểm tra danh sách dữ liệu trước khi gửi lại để tránh thao tác trùng. Video/local dự phòng không thay thế yêu cầu phải có bản deploy online.

**Sau báo cáo nếu hướng tới vận hành trong khoa:** Xem xét compute luôn chạy, backup/phục hồi, log/cảnh báo, phân tách tài khoản vận hành và migration; nếu cần giữ phiên qua restart hoặc nhiều instance, đánh giá Spring Session JDBC/Redis. Lưu phiên ngoài không loại bỏ cold start. Các thay đổi này chưa thuộc đề xuất triển khai trước mắt và cần quyết định riêng.

## 6. Kế hoạch dự kiến đến 26/10/2026

Mốc dưới đây là mục tiêu, không phải tiến độ đã hoàn thành. Nếu ngày duyệt muộn hơn, điều chỉnh lịch và ưu tiên phần bắt buộc trước. Thời lượng là ước lượng giờ làm tập trung, chưa gồm thời gian chờ dịch vụ/giảng viên.

| Giai đoạn | Thời gian đề nghị | Phụ trách đề nghị | Công việc | Tiêu chí kết thúc |
|---|---|---|---|---|
| A. Chốt bằng chứng và cách giải thích | 06–08/10; 2–3 giờ | Huy + cả nhóm | Đọc tài liệu này, đối chiếu nhiệm vụ thành viên, thống nhất thuật ngữ và luồng cần bảo vệ | Mỗi người giải thích được phần phụ trách; danh sách phạm vi được duyệt |
| B. MySQL kiểm thử | 07–09/10; 3–5 giờ | Huy + người phụ trách DB/nghiệp vụ | Sao lưu; MySQL test riêng; SQL; test trigger và các bộ tích hợp | Trigger test thực sự chạy, bộ được chọn không fail/error/skip; lưu log và môi trường |
| C. Deploy bản đầu | 09–12/10; 4–6 giờ | Huy, nhóm hỗ trợ seed/nghiệp vụ | Cloud DB/storage; review commit; CI; Render | URL HTTPS, bốn vai trò và lưu file hoạt động; có log/commit |
| D. Nghiệm thu và sửa có phê duyệt | 12–16/10; 3–5 giờ | Cả nhóm | Luồng hoàn chỉnh, lỗi quyền, thời gian, restart và cold start | Danh sách lỗi nghiêm trọng đã xử lý; giới hạn được ghi rõ |
| E. Báo cáo và diễn tập | 17–20/10; 3–4 giờ + thời gian viết | Cả nhóm | Chụp ảnh đúng bản deploy; Word, ERD/use case, hướng dẫn; hỏi chéo kiến trúc | Hồ sơ khớp code và bằng chứng; từng người thao tác được |
| F. Chốt bản và dự phòng | 21–23/10 | Huy + cả nhóm | Chốt commit/release, sao lưu, kiểm tra URL và bộ nộp | Bản ổn định, không thêm chức năng mới; đạt mục tiêu xong sớm |
| G. Khoảng đệm | 24–25/10 | Cả nhóm | Chỉ xử lý lỗi cản demo; kiểm tra lại dịch vụ và định dạng ZIP | Sẵn sàng báo cáo ngày 26/10 |

**Thứ tự khi thiếu thời gian:** MySQL test thật → URL deploy → luồng demo và lưu file → hồ sơ bằng chứng → ôn vấn đáp. Không ưu tiên refactor thẩm mỹ, Redis, AI, chat hay tính năng ngoài phạm vi.

## 7. Danh sách quyết định để Huy phê duyệt

| Mã | Phạm vi cần duyệt | Đề nghị | Trạng thái |
|---|---|---|---|
| A1 | Tạo MySQL test riêng và chạy test sẵn có; không đụng DB demo | Duyệt trước | Chờ Huy |
| A2 | Bổ sung test trọng yếu nếu còn thiếu; mọi sửa code phải review diff trước | Làm theo bằng chứng sau A1 | Chờ Huy |
| A3 | Render Free + Aiven MySQL Free + Supabase Free; cấu hình cloud, review/push đúng branch và deploy | Duyệt sau khi test đạt và tài khoản sẵn sàng | Chờ Huy |
| A4 | Cập nhật hồ sơ báo cáo và diễn tập restart/cold start | Thực hiện cùng nghiệm thu | Chờ Huy |
| A5 | Refactor repository/quy chế, thêm session store hoặc compute trả phí | Để sau báo cáo, trừ khi có lỗi bắt buộc | Chưa đề nghị thực hiện |

**Đề xuất phê duyệt trước mắt:** A1, sau đó đánh giá kết quả để quyết định A2/A3. Tài liệu này không tự cấp quyền thực hiện các thay đổi. Không có test mới, dịch vụ mới hoặc bản deploy nào được tạo trong lượt lập tài liệu.

## 8. Nguồn và cách kiểm tra lại

Các đường dẫn source dưới đây tính từ `D:\project_web\final_projects`:

| Nội dung | Nguồn nội bộ |
|---|---|
| Vai trò/chức vụ, thay đổi quyền phiên hiện có | `projects/quanlydetai/src/main/java/com/example/quanlydetai/security/CustomUserDetailsService.java`, `AccountStatusFilter.java`; `service/BusinessSecurityService.java`; `projects/Database/01_schema_mysql.sql` |
| Quy tắc chung, khóa và JPA | `service/AcademicRules.java`; `repository/JpaEntityRepository.java`, `TaiKhoanRepository.java`; `service/NhomService.java`, `DeTaiService.java`, `DiemService.java` trong package ứng dụng |
| OSIV, session, prod/storage | `projects/quanlydetai/src/main/resources/application.properties`, `application-prod.properties`; `projects/quanlydetai/pom.xml` |
| Test MySQL và báo cáo cũ | `projects/quanlydetai/src/test/java/com/example/quanlydetai/MySqlTriggerIntegrationTest.java`; `src/test/resources/application-test.properties`; `target/surefire-reports/TEST-*.xml` |
| Cách chạy và CI | `tools/Start-Local.ps1`; `.github/workflows/backend.yml`; `Dockerfile`; `render.yaml`; `docs/CLOUD_SETUP.md` |
| Trigger và schema | `projects/Database/01_schema_mysql.sql` đến `05_department_catalog_mysql.sql` |

PDF gốc ở thư mục cha: “Đồ án cuối kỳ.pdf” (quy trình/điểm) và “Yêu cầu khi nộp đồ án cuối kỳ.pdf” (hosting miễn phí, Word, cấu trúc và quy cách nộp).

Nguồn bên ngoài đã dẫn ngay tại nội dung liên quan. Các giới hạn free tier có thể thay đổi; kiểm tra lại dashboard và tài liệu nhà cung cấp khi thực hiện. Nội dung này là hồ sơ chuẩn bị/phê duyệt, chưa phải biên bản nghiệm thu sản phẩm.
