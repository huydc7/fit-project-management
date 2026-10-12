# Nhật ký kiểm chứng bản local

## Chuẩn bị commit bàn giao nhóm — 12/10/2026

- Chạy Maven Wrapper `-B -ntp -Dmaven.repo.local=D:/project_web/.m2 verify`: **BUILD SUCCESS**, 59 ca, 56 thực thi đạt, 3 ca trigger MySQL bỏ qua có điều kiện. Lượt này dùng H2; không kiểm chứng lại MySQL/SMTP thật. Log ngoài repo: `D:/project_web/tmp/github-baseline-verify.log`.
- Kiểm tra bản chuẩn bị commit không chứa `.env`, backup, uploads hoặc target. Kiểm tra mẫu token/khóa và đối chiếu với secret trong cấu hình local không phát hiện trùng trong 225 file được kiểm tra; đây là kiểm tra có phạm vi, không chứng nhận loại bỏ mọi dữ liệu nhạy cảm khỏi lịch sử Git cũ.
- Giữ lịch sử nhóm, cấu hình tác giả riêng repo theo tên/email Huy cung cấp. Bổ sung hướng dẫn GitHub làm việc nhóm và mẫu Pull Request; không sửa nghiệp vụ hay dữ liệu đang chạy trong lượt này.
- Chưa nghiệm thu GitHub Actions hoặc push repo mới tại thời điểm ghi nhật ký. Remote gốc vẫn là Khaibuine/final_projects. Computer Use dừng vì không xác định chắc URL Chrome; cần người dùng tạo repo trên huydc7 và cung cấp URL để push đúng đích.

## Tiếp tục local — chiều 06/10/2026

- Khởi động lại Docker Desktop; app chính tại cổng 8080 có health `UP`, MySQL Docker tại cổng 3307 giữ nguyên dữ liệu. Không thực hiện migration, nạp mẫu hay thay mật khẩu trong lượt buổi chiều.
- Thêm Mailpit cô lập trong `compose.test.yaml`: SMTP loopback 11025, hộp thư 18025. `tools/Test-LocalEmail.ps1 -MavenRepository D:/project_web/.m2` chạy **MailpitDeliveryIT: 2/2 đạt, 0 lỗi/thất bại/bỏ qua**. Đây là lượt chạy riêng, không cộng vào con số 59 của bộ mặc định buổi sáng.
- Worker thật gửi qua JavaMailSender/SMTP; Mailpit nhận ba thư riêng, mỗi thư chỉ một người nhận, tiếng Việt và liên kết theo đợt đúng. Gọi worker lần nữa không tăng số thư. Giao dịch tạo hàng đợi bị rollback không phát thư. Ca kiểm thử dùng H2 trong bộ nhớ và địa chỉ `@example.test`, không sử dụng database nghiệp vụ.
- Lượt này kiểm chứng từ bản ghi hàng đợi giả lập tới SMTP; luồng công bố điểm tạo hàng đợi nằm trong WorkflowIntegrationTest đã chạy trước đó. Không coi đây là kiểm thử xuyên suốt thao tác trình duyệt → công bố → email Internet.
- Không sửa code chạy chính hoặc `.env`; email trên app chính vẫn tắt. Mailpit giữ chạy để nhóm xem thư tại `http://localhost:18025`. Hướng dẫn tái lập/dừng ở [EMAIL_LOCAL_TEST.md](EMAIL_LOCAL_TEST.md). Log: `D:/project_web/tmp/mailpit-verification.log`.
- Đồng bộ ERD, từ điển dữ liệu và báo cáo Markdown với 22 bảng. ERD thêm trang chi tiết công bố/email; XML đã kiểm tra ID duy nhất và tham chiếu cạnh hợp lệ, chưa xuất ảnh để nghiệm thu bố cục sơ đồ.
- Đã ghi nhận thiếu sót thao tác sửa/xóa/tìm kiếm trong [CRUD_RUBRIC_MATRIX.md](CRUD_RUBRIC_MATRIX.md). Chưa triển khai các thao tác thiếu trong lượt này; chưa kết luận đạt toàn bộ rubric.
- Không tạo tài khoản cloud, gửi email bên ngoài, commit, push hoặc deploy. Bộ test toàn dự án không chạy lại vì lượt này chỉ bổ sung kiểm thử, công cụ local và tài liệu.

## Cập nhật rubric, email và triển khai — 06/10/2026

- `mvnw verify`: 59 ca, 0 failure/error; 56 ca thực thi đạt và 3 ca trigger MySQL được bỏ qua có điều kiện trên H2. Không gọi đây là 59 ca đều đã chạy.
- MySQL 8.0.46 cô lập cổng 3308, nạp SQL 01–06 và `ddl-auto=validate`: FoundationIntegrationTest + WorkflowIntegrationTest + MySqlTriggerIntegrationTest **35/35 đạt, skipped=0**. Ba ca trigger gồm lỗi hội đồng, nhóm 2/3 người khi chọn đề tài và giới hạn 5/6 người. Container kiểm thử đã dừng sau khi chạy.
- H2 có 7 ca mới về SMTP/HTTPS transport, worker bật/tắt, lỗi từng người nhận, lease, retry/backoff và rollback hàng đợi. Workflow xác nhận tạo 3 thư cho 3 SV, công bố lặp không tạo thêm, lọc đợt và chặn người ngoài nhóm. Không gửi email ra Internet trong kiểm thử.
- Phát hiện và sửa lỗi charset khi Docker init SQL mặc định latin1; các script chỉ định `SET NAMES utf8mb4`. MySQL suite được chạy lại sau sửa và đạt.
- Phát hiện mail health indicator tự kiểm tra SMTP dù email đang tắt hoặc dùng HTTPS; tách SMTP tùy chọn khỏi health của website. Chạy lại FoundationIntegrationTest **11/11 đạt**, có kiểm tra health UP khi không có SMTP.
- Maven local dùng JDK 27 với release target 21. Docker build bằng Java 21 và chạy JRE 21 thành công. HTTP thật: `/actuator/health` UP, `/login` 200. Gửi email trên app local hiện tắt (`EMAIL_ENABLED=false`); không cần tài khoản cloud để chạy web.
- Đã sao lưu DB Docker vào `backups/before-rubric-20261006-092920.sql`, áp dụng migration 06, rebuild/restart app. Đối chiếu các INSERT trong dump trước/sau: 10 khối dữ liệu mỗi bản, **0 khác biệt**. Sau migration có 22 bảng, 35 trigger, 4 view. Không sửa dữ liệu đã nhập hoặc mật khẩu, không nạp lại mẫu.
- Log cục bộ: `D:/project_web/tmp/rubric-verify.log`, `rubric-mysql.log`, `rubric-health-test.log`, `rubric-docker.log`. Log/backup không đưa vào Git. `git diff --check` đạt.
- Chưa có tài khoản cloud nên **chưa nghiệm thu Aiven/Supabase/Brevo thật hoặc URL HTTPS**. Nhóm thực hiện theo [hướng dẫn từng bước](HUONG_DAN_DEPLOY_TUNG_BUOC.md). Phạm vi đã sửa và điểm rubric còn cần xác nhận ở [RUBRIC_UPDATE](RUBRIC_UPDATE.md). Không tự commit/push/deploy trong lượt này.

## Login vừa chiều cao màn hình — 02/10/2026

- Giữ nguyên form, icon và nhận diện. Logo, padding và khoảng cách dùng chiều cao viewport; màn hình thấp có bố cục gọn riêng, không dùng khóa cuộn để che nội dung.
- Kiểm tra trực tiếp trình duyệt: 1536×730, 1366×650, khoảng 1093×520, 390×844, 375×667 và 320×568. Trang login mặc định không cuộn ngang/dọc; logo và footer nằm trong viewport. 1366×650 với thông báo lỗi hoặc mở hỗ trợ cũng vừa khung nhìn.
- Docker app build/khởi động thành công; `git diff --check` đạt. Thay đổi chỉ HTML/CSS nên kiểm tra bằng trình duyệt; không chạy lại kiểm thử backend. Ảnh mới: [ui-login.png](images/ui-login.png).

## Favicon, icon mật khẩu và nhận diện FIT — 02/10/2026

- Favicon dùng phần biểu tượng tròn của ảnh trường trên nền trắng, có PNG 32 px và ICO 16/32/48 px. Thêm phiên bản URL tài nguyên mới để tránh cache cũ.
- Nút mật khẩu dùng icon mắt/mắt gạch chéo; cập nhật nhãn hỗ trợ và trạng thái khi hiện/ẩn. Chân trang có dấu chữ FIT thiết kế riêng cho dự án và tên khoa; tiêu đề mô tả quản lý đề tài & đồ án sinh viên.
- `FoundationIntegrationTest`: **11/11 đạt**, `git diff --check` không báo lỗi. Docker app build/khởi động thành công, health `UP`; favicon PNG và ICO tải không cần đăng nhập trả HTTP 200.
- Kiểm tra trình duyệt ở 1366 px và 390 px không tràn ngang. Bấm chuột để hiện, Enter để ẩn mật khẩu: kiểu input, icon và nhãn đổi đúng; form còn CSRF. Ảnh: [ui-login.png](images/ui-login.png).

## Login đơn giản và logo trường — 02/10/2026

- Dùng ảnh `Logo.png` do người dùng cung cấp cho logo trang đăng nhập và favicon trên login, khung trang nội bộ và trang lỗi. Bỏ cột xanh, khẩu hiệu và CSS trang trí cũ; thay bằng form một cột trên nền xám nhạt.
- `FoundationIntegrationTest`: **11/11 đạt**. Docker build/khởi động thành công, health `UP`; ảnh logo tải khi chưa đăng nhập trả 200 và `image/png`.
- Kiểm tra trình duyệt ở 1366 px và 390 px: logo hiển thị, form có token CSRF, không tràn ngang, favicon tham chiếu đúng ảnh trường. Ảnh mới: [ui-login.png](images/ui-login.png).

## Giao diện HTML/CSS thuần — 02/10/2026

- Thay Bootstrap bằng CSS riêng và JavaScript thuần. Trang đăng nhập, tổng quan, khung điều hướng, form, bảng và thông báo dùng cùng bộ giao diện. Trang tổng quan lấy tối đa ba thông báo hiện có, không tạo số liệu mẫu.
- Chạy `mvnw.cmd -Dtest=FoundationIntegrationTest,WorkflowIntegrationTest test`: **32/32 đạt** (11 nền tảng, 21 nghiệp vụ/API). Đây là bộ kiểm thử liên quan đến thay đổi; không chạy lại bộ trigger MySQL trong lượt này.
- Build Docker Java 21 thành công; kiểm tra HTTP thực cả bốn vai trò: đăng nhập, trang HTML, JSON API và chuyển hướng URL cũ thành công; sinh viên vào chức năng quản trị vẫn nhận 403. Không thay đổi bản ghi nghiệp vụ hoặc dữ liệu mẫu.
- Kiểm tra trực quan trên trình duyệt ở 1440 px và 390 px: đăng nhập/tổng quan hiển thị đúng; menu di động mở và điều hướng được; form nhóm không tràn ngang; bảng tài khoản rộng được cuộn trong vùng bảng. Kiểm tra hiện/ẩn mật khẩu và đăng xuất thành công. Stylesheet và script được tải từ ứng dụng, không còn Bootstrap.
- Quy ước và vị trí file: [UI_DESIGN.md](UI_DESIGN.md). Ảnh tổng quan: [ui-overview.png](images/ui-overview.png).

## Cập nhật URL tiếng Anh — 01/10/2026

- `mvnw.cmd verify`: **50 ca, 49 đạt, 1 bỏ qua**, không có lỗi/thất bại. Ca MySqlTriggerIntegrationTest bỏ qua vì chưa cấu hình database kiểm thử MySQL riêng. Hai ca mới kiểm tra chuyển hướng trang cũ, phân quyền URL và menu/form tiếng Anh với nhãn tiếng Việt.
- Sau rà soát hậu tố của form có tham số, đối chiếu toàn bộ **32 URL khác nhau trong template** với mapping controller: khớp. `git diff --check` không có lỗi.
- Docker build thành công trên Java 21. MySQL hiện có đã dừng trước lượt kiểm tra nên lần đầu app không kết nối được; bật lại container MySQL cùng volume cũ, sau đó app khởi động thành công, health `UP`.
- Kiểm tra HTTP thực trên bản Docker mới: đăng nhập đủ admin, trưởng khoa, giảng viên, sinh viên; tất cả 11 trang theo vai trò cùng trang thông báo/đổi mật khẩu trả 200. Bốn endpoint JSON đại diện trả 200. Các trang cũ đại diện trả 301 về URL tiếng Anh; sinh viên truy cập trang admin cũ/mới hoặc API admin đều nhận 403.
- Không sửa dữ liệu nghiệp vụ, không nhập lại mẫu hoặc thay đổi mật khẩu. URL mới và phạm vi tương thích xem [URLS.md](URLS.md); client API cần cập nhật theo [REST_API.md](REST_API.md).

## Cập nhật sau tách kiến trúc và REST API — 01/10/2026, 20:52

- `mvnw.cmd verify`: **48 ca, 47 đạt, 1 bỏ qua**, không có lỗi/thất bại. Ca bỏ qua là MySqlTriggerIntegrationTest vì lượt này không cấu hình database MySQL kiểm thử riêng. Không coi kết quả MySQL ở nhật ký cũ là kiểm chứng lại bản refactor.
- Sau bổ sung xử lý JSON cho lỗi multipart xảy ra trước controller, chạy lại `-Dtest=WorkflowIntegrationTest package`: **21/21 đạt**. Gồm 14 luồng cũ và 7 ca API: JSON 401/403, CSRF, kiểm tra dữ liệu, quyền theo nhóm, giữ quy tắc thời gian, phiên bị khóa, DTO không lộ hash, token/cookie thật qua đăng nhập và lỗi route 404/405.
- Docker build thành công trên Java 21; chỉ tạo lại container app bằng `up -d --no-deps app`. MySQL và các volume được giữ nguyên. Health UP; bốn tài khoản admin/truongkhoa/gv.hoangnam/sv.22110001 đăng nhập bằng mật khẩu hiện có, các trang HTML và endpoint JSON tương ứng trả 200.
- Nộp file thử kích thước 20 MiB + 1 byte qua HTTP thực: trả `application/problem+json`, status 413 trước khi tạo báo cáo. Không tạo nhóm/báo cáo/điểm trên database vận hành trong lượt refactor.
- Kiểm tra nội dung JAR: không có các lớp Workflow cũ, config/sample, tools/sample hoặc JSON mẫu. Công cụ standalone `spring-boot:test-run` đã nhập bộ mẫu thành công trên H2 riêng và tự đóng context. Script PowerShell mới qua kiểm tra cú pháp; Java qua kiểm tra định dạng.
- Web không tự nhập mẫu nữa; Compose không truyền password mẫu hoặc mount JSON. Các bước và cấu trúc mới xem ARCHITECTURE.md, REST_API.md và SAMPLE_DATA.md.

## Nhật ký trước lần tách kiến trúc

Ngày 01/10/2026. Windows 11, Maven 3.9.16, bộ test local chạy JDK 27 với Java release target 21. Docker/CI được cấu hình dùng JDK 21; ứng dụng Docker đã khởi động và đăng nhập thành công bằng Java 21.0.12.1 lúc 19:08.

## Kết quả đã xác nhận

- Bộ test mặc định sau dọn source và dữ liệu: 41 ca, **40 đạt, 1 bỏ qua** vì ca trigger chỉ bật khi cấu hình MySQL. Không có lỗi hoặc thất bại. Test dùng H2 và HTTP storage giả lập.
- Bộ tích hợp MySQL thật: **24/24 đạt**, gồm FoundationIntegrationTest (9), WorkflowIntegrationTest (14) và MySqlTriggerIntegrationTest (1). MySQL 8.0.46 chạy riêng trên loopback cổng 33317, nạp SQL 01–04; Hibernate dùng `validate`. Không sửa service hoặc database MySQL sẵn có của người dùng.
- Schema thực tế: **21 bảng, 35 trigger, 4 view**. Hai bảng bổ sung là đăng ký đề tài và lời mời nhóm; 19 bảng gốc vẫn giữ. Sau dọn source, còn 21 entity bảng và 7 Spring Data repository có sử dụng; repository rỗng và entity/repository view không còn được gọi đã bỏ. Các giao dịch khác dùng JPA EntityManager.
- `mvnw clean verify` đạt lúc 19:47 sau dọn source, đóng gói JAR mới và không giữ class đã xóa trong target. Docker build bằng JDK 21 đạt lúc 20:03; đã thay container app và khởi động thành công lúc 20:05. Kiểm tra JAR không chứa DemoInitializer, lớp query/view đã bỏ, file JSON dữ liệu dùng thử hoặc các class fixture kiểm thử.
- Maven Wrapper đã chạy lệnh package trực tiếp thành công. Sửa lỗi truy cập `Target[0]` khi cache Maven là thư mục thông thường trên Windows.
- JAR cuối khởi động và validate schema trên MySQL thật, đăng nhập tài khoản SV mẫu thành công. Kiểm tra trực quan trang Nhóm của tôi ở viewport nhỏ khoảng 413 px và desktop 1366 × 900: menu thu gọn/mở hoạt động, điều hướng đúng trang, form đọc được. Các màn hình vai trò khác được kiểm tra render qua MockMvc; chưa coi đó là nghiệm thu trực quan toàn bộ ứng dụng.
- CSS/JS Bootstrap đã nằm trong static assets của ứng dụng; không cần CDN khi demo local.
- PowerShell Start-Local.ps1 đã qua kiểm tra cú pháp. Kiểm tra mật khẩu demo trước khi khởi động đã xác nhận 4 trường hợp không hợp lệ bị chặn và 3 trường hợp hợp lệ/tắt seed được chấp nhận. Docker Compose đã qua kiểm tra cấu hình.
- `git diff --check` không có lỗi whitespace. Không commit/push/deploy tự động.

## Phạm vi các ca test

| Bộ test | Số ca | Nội dung chính |
|---|---:|---|
| FoundationIntegrationTest | 9 | Đăng nhập BCrypt thật qua repository, quyền vai trò, khóa tài khoản/thu hồi chức vụ trong phiên cũ, CSRF, chống giả người tạo hoặc ghi đè mã đợt, validation, equals/hashCode với proxy |
| WorkflowIntegrationTest | 14 | Luồng nhóm–đăng ký–duyệt–báo cáo–hội đồng–chấm–công bố; xác nhận lời mời và giới hạn nhóm; giải phóng giữ chỗ; thời gian đề xuất; duyệt muộn yêu cầu hợp lệ; GVHD không tự chấm; ACL file/kết quả; điểm 6,75; khóa sửa sau tổng hợp; tài khoản/mật khẩu/thu hồi chức vụ; phạm vi thông báo và escape nội dung; render các route vai trò |
| MySqlTriggerIntegrationTest | 1 | Trigger hội đồng phát SIGNAL 45000; lỗi đi qua Hibernate được chuyển thành thông báo an toàn |
| DatabaseErrorMessagesTest | 3 | Nested SQLException/nextException, che SQL nội bộ, tránh vòng lặp cause |
| ReportFileValidatorTest | 5 | PDF/MIME, extension giả, rỗng/quá lớn, cấu trúc DOCX/ZIP, đường dẫn nguy hiểm và macro |
| SupabaseStorageServiceTest | 4 | Upload bằng UUID, ký link 300 giây, xóa đúng key, chặn key giả và che chi tiết lỗi HTTP |
| Context test | 1 | Khởi tạo Spring/Hibernate/repository |
| SampleDataIntegrationTest | 2 | Đọc bộ khoa CNTT: bốn bộ môn, đủ bốn loại đợt, 12 đề tài; refresh không tạo trùng và giữ mật khẩu, trạng thái khóa, lịch đợt, SV/nhóm/thành viên do người dùng nhập |
| SampleDataProfileTest | 2 | Bộ nạp mặc định tắt ở dev và không đăng ký ở prod dù cố bật biến cấu hình |

Các test giao dịch tạo fixture riêng rồi rollback. Luồng tích hợp MySQL xác nhận cả tương tác giữa các trigger và service; không chỉ đối chiếu SQL tĩnh. Kiểm thử end-to-end nghiệp vụ ở đây dùng MockMvc, không phải toàn bộ thao tác trình duyệt tự động.

## Các phần cần nghiệm thu tiếp

Cập nhật dữ liệu và source lúc 20:07: đã sao lưu MySQL vào backups/quanlydetai-before-faculty-20261001.sql (được Git/Docker ignore). Script 05 chuyển hai bộ môn mẫu sang danh mục bốn bộ môn mà không xóa GV/đề tài; chạy lại script đạt. Bộ nhập JSON chạy bằng JAR mới trên Java 21/MySQL 8.0.46 đã bổ sung dữ liệu, giữ ID cũ nơi có liên kết. CSDL có 9 GV, 18 SV, 28 tài khoản, 5 đợt và 12 đề tài (ba đề tài mỗi bộ môn).

Đối chiếu SHA-256 tổng hợp theo khóa tài khoản cũ xác nhận hash mật khẩu, vai trò và trạng thái không thay đổi; đối chiếu đợt cũ xác nhận loại và mọi mốc thời gian không thay đổi. Kiểm tra lại sau khi tạo lại container MySQL/app vẫn trùng. Nhóm, báo cáo và điểm trước/sau đều chưa có bản ghi; không suy rộng điều đó thành đã kiểm tra bảo toàn một database vận hành có báo cáo thật. Ca H2 có thêm nhóm/thành viên do người dùng nhập để kiểm tra bảo toàn.

Đã xác nhận health UP; đăng nhập admin, truongkhoa, gv.hoangnam và sv.22110001 bằng mật khẩu hiện có, mỗi trang chức năng vai trò trả HTTP 200. SAMPLE_DATA_ENABLED và SAMPLE_DATA_REFRESH đã tắt trong cấu hình thực tế sau nhập. JSON nằm ngoài JAR, được mount read-only khi chạy Docker local. Chỉ tên hồ sơ/đề tài là dữ liệu giả lập; không phải danh sách nhân sự chính thức của HCMUTE. Chi tiết tại SAMPLE_DATA.md.

Cập nhật Docker lúc 19:08: MySQL 8.0.46 healthy, app chạy Java 21.0.12.1, schema validation thành công, seed tạo đủ tài khoản mẫu. Đã xác nhận health UP, login HTTP 200, đăng nhập demo.sv1 và trang nhóm có đợt DEMO_SV bằng HTTP/session/CSRF thật. Lỗi app Exited (1) trước đó do DEMO_PASSWORD chỉ có 11 ký tự; sau khi người dùng sửa .env, đã tạo lại container app để nạp biến mới. Không sửa mật khẩu thay người dùng, không in secret và không xóa volume. Container được giữ chạy cho người dùng kiểm thử tiếp.

Nhóm cần chạy checklist trong LOCAL_TEST.md trên môi trường của mình, đặc biệt upload/download và lưu dữ liệu qua restart. Chưa kiểm thử tải lớn, tranh chấp giao dịch đồng thời, phục hồi backup, toàn bộ nghiệp vụ trong Docker, GitHub Actions thực tế, Aiven TLS hoặc Supabase thật. Không suy rộng các kết quả local thành cam kết vận hành production.

Website online, báo cáo Word theo mẫu môn học và gói ZIP nộp bài chưa được tạo/xác nhận. Thực hiện cloud sau nghiệm thu local; ghi nhận URL, commit triển khai, kiểm tra restart, tài khoản demo đã xử lý và storage/CA đã cấu hình. Không điền giả mã nhóm, MSSV hoặc URL online.

Các thư mục/database và tiến trình preview ban đầu dùng để kiểm thử được dọn sau khi xác minh; bản JAR trong target vẫn còn để nhóm dùng. Các mục target trong staging là thao tác bỏ theo dõi artifact build; Maven vẫn sinh target bình thường. Các file Java bị xóa trong lượt dọn source là lớp demo/query/view và repository không còn sử dụng, được mô tả trong SAMPLE_DATA.md. Container Docker chính được giữ chạy cho nhóm kiểm thử tiếp.
