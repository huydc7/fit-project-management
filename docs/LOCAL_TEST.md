# Chạy và kiểm thử local

Ứng dụng dùng MySQL thật ở cả hai cách chạy. MySQL Docker ở cổng 3307; MySQL cài trên máy thường ở cổng 3306. Hai database độc lập. Với cấu hình mặc định, website Docker chạy ở http://localhost:8080.

## Docker

1. Mở Docker Desktop, đợi engine chạy.
2. Tại thư mục gốc repository, tạo .env từ .env.example nếu chưa có. Điền MYSQL_ROOT_PASSWORD và DB_PASSWORD; giữ DB_USERNAME=quanlydetai, MYSQL_PORT=3307, PORT=8080, STORAGE_BACKEND=local.
3. Nếu cần dữ liệu thực hành: điền SAMPLE_DATA_PASSWORD trong .env, khởi động MySQL rồi chạy `tools/Import-SampleData.ps1 -DockerDatabase`. Web không tự nhập dữ liệu; xem SAMPLE_DATA.md.
4. Chạy:

```powershell
.\tools\Start-Local.ps1 -Docker
```

Nếu PowerShell chặn script, chỉ cho phép trong tiến trình gọi:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Start-Local.ps1 -Docker
```

Kiểm tra:

```powershell
docker compose --profile app ps -a
docker compose logs --tail 100 app
docker compose logs --tail 100 mysql
```

Kiểm tra http://localhost:8080/actuator/health trả UP và đăng nhập thực tế. Web không tự nhập dữ liệu mẫu. Thay đổi SAMPLE_DATA_PASSWORD không đổi mật khẩu của tài khoản đã tồn tại; dùng chức năng đặt lại mật khẩu của Admin khi cần. Khi thay cấu hình .env, tạo lại container bằng `docker compose --profile app up -d app`; restart không nạp biến mới.

Dừng bằng `docker compose --profile app down`, dữ liệu còn trong volume. Chạy lại bằng `docker compose --profile app up -d`. Khi sửa source, thêm --build. Không dùng down -v để nâng cấp. SQL 01–10 tự nạp chỉ khi tạo volume lần đầu; volume cũ cần nạp migration thích hợp bằng Workbench/CLI. Xem [nâng cấp phân công GVHD](DEPARTMENT_SUPERVISOR_WORKFLOW.md#nâng-cấp-database-đang-dùng-trên-máy) cho SQL 09.

## Xem MySQL Docker bằng Workbench

Tạo connection Standard TCP/IP: hostname 127.0.0.1, port 3307, username từ DB_USERNAME, password từ DB_PASSWORD, default schema quanlydetai. Đây không phải mật khẩu đăng nhập web. Test Connection rồi mở SCHEMAS → quanlydetai → Tables; nhấp phải bảng → Select Rows – Limit 1000.

## MySQL đã cài + Java

Dùng JDK 21 và MySQL 8.0.16+. Nạp toàn bộ script 01–10 từ projects/Database vào DB phát triển. Kiểm tra Output không có lỗi. Database cũ cần sao lưu rồi chạy các script còn thiếu theo thứ tự đến 09; DB đủ 01–08 chỉ chạy `09_department_supervisor_assignment.sql`, không đổi Hibernate thành create/update. Docker có script `tools/Update-RubricDatabase.ps1` sao lưu trước migration. Đăng ký đề tài mới cần 3–5 SV; xem [hướng dẫn triển khai](HUONG_DAN_DEPLOY_TUNG_BUOC.md).

Dùng .env.native riêng để giữ cấu hình Docker:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=<mật khẩu MySQL trên máy>
PORT=8081
STORAGE_BACKEND=local
```

Nếu cần bộ thực hành, cấu hình thêm theo SAMPLE_DATA.md. Chạy tại root repository:

```powershell
.\tools\Start-Local.ps1 -EnvFile .env.native
```

Mở http://localhost:8081. Ctrl+C để dừng web. MySQL trên máy tiếp tục chạy. Maven cần mạng ở lần tải dependency đầu tiên; chạy Maven/IDE trực tiếp không tự đọc các file .env.

## Đăng nhập và thử nghiệp vụ

Tài khoản mẫu và chính sách dữ liệu được ghi trong SAMPLE_DATA.md. Với database nâng cấp từ bộ cũ, tên đăng nhập đổi nhưng mật khẩu, trạng thái và mã hồ sơ cũ được giữ. Khi mời SV hoặc phân công GV, lấy mã hồ sơ thực tế từ trang Admin hoặc Workbench; không dùng username làm mã.

1. gv.hoangnam chỉ đề xuất nội dung vào đợt còn hạn GV; Trưởng bộ môn đã được bổ nhiệm duyệt đúng bộ môn và chọn 1–2 GVHD, rồi truongkhoa xác nhận và công bố. Thử đề xuất vào đợt đã qua giai đoạn GV để kiểm tra chặn.
2. sv.22110001 tạo nhóm ở đợt Đồ án môn học học kỳ I, mời hai SV và từng người xác nhận lời mời. Nhóm cần 3–5 người khi đăng ký; mỗi SV chỉ có một nhóm mỗi đợt.
3. Nhóm trưởng đăng ký đề tài Cổng đăng ký và quản lý đề tài sinh viên. truongkhoa chấp thuận. Thử rút/từ chối với yêu cầu khác để xác nhận giải phóng giữ chỗ.
4. truongkhoa phân công gv.minhanh phản biện; hội đồng gồm gv.thanhbinh chủ tịch, gv.thuha thư ký, gv.quochuy thành viên. Chốt rồi gán đề tài. GVHD gv.hoangnam không được chấm.
5. Nhóm trưởng nộp PDF/DOCX/ZIP dưới 20 MiB; thử nhiều phiên bản, tải đúng file và kiểm tra người ngoài nhóm không tải được.
6. Phản biện chấm 9, mỗi thành viên hội đồng chấm 6, có nhận xét. Điểm -1, 10.01 hoặc 6.123 bị chặn. Sau phiếu đầu tiên, không nộp báo cáo mới.
7. Chủ tịch tổng hợp khi đủ phiếu: điểm 6,75. Thư ký không được tổng hợp; sau tổng hợp các phiếu bị khóa.
8. Trưởng khoa công bố kết quả sau khi Chủ tịch tổng hợp. Thành viên nhóm xem kết quả theo đợt; SV ngoài nhóm không xem hoặc tải bằng cách đổi ID. Trước công bố, nhóm chưa thấy điểm. Nếu chạy với `-LocalEmail`, mở http://localhost:8025: mỗi thành viên nhận một thư có điểm và link đúng đợt. Trưởng khoa theo dõi tại `/dean/result-emails`; sinh viên vẫn tra cứu trên website.
9. admin thử tạo, khóa/mở và reset mật khẩu tài khoản; kiểm tra phiên cũ hết quyền. Admin đăng thông báo Nhà trường; Trưởng khoa chỉ đăng Khoa.

Mỗi lần đổi vai trò, đăng xuất hoặc dùng trình duyệt/profile khác. Đợt và đề tài cũ giữ mã và thời gian đã có; đối chiếu tên trên màn hình, không giả định mã mới đã thay toàn bộ mã cũ.

## Nghiệm thu trước cloud

Kiểm tra phân công GV: khóa một tài khoản hoặc tạo hồ sơ GV chưa có tài khoản trong database kiểm thử. GV đó không xuất hiện trong danh mục phân công; gửi mã trực tiếp qua API vẫn bị từ chối, phân công cũ giữ nguyên. Thử khóa sau khi bộ môn đã duyệt GVHD và sau khi hội đồng đã lập/chốt: công bố hoặc gán hội đồng phải kiểm tra lại tài khoản. Tại Admin → Khóa / Xem ảnh hưởng, kiểm tra nhiệm vụ còn lại và yêu cầu xác nhận trước khi khóa. GV đã nộp phiếu không còn báo thiếu phiếu; chủ tịch vẫn được cảnh báo đến khi tổng hợp. Mở khóa và kiểm tra GV nhận phân công/đăng nhập lại được.

- Luồng trên chạy trên MySQL của nhóm; kiểm tra quyền, ngoài hạn, nhóm dưới 3 người khi đăng ký, thành viên thứ sáu, đăng ký trùng, thiếu phiếu, GVHD tự chấm và điểm trước công bố.
- File/database còn sau restart; kiểm tra upload/download và định dạng/dung lượng sai.
- Chạy clean verify; dùng DB kiểm thử riêng cho test MySQL theo README, không dùng DB vận hành.
- Kiểm tra giao diện màn hình nhỏ; chụp ảnh bằng dữ liệu mà nhóm đã nghiệm thu.
- Thống nhất PRODUCT_RULES; prod tắt dữ liệu thực hành, cấu hình cloud theo CLOUD_SETUP.

Các bước trên kiểm tra môi trường local. Triển khai cloud cần nghiệm thu riêng theo [CLOUD_SETUP.md](CLOUD_SETUP.md).

## Nâng cấp duyệt bộ môn (SQL 07)

Database đã có 01–06: chạy `tools/Update-DepartmentReview.ps1`, tiếp tục nâng cấp 08 và 09 trước khi rebuild ứng dụng hiện tại. Script PowerShell sao lưu rồi thêm hai bảng; không nạp lại mẫu. Sau nâng cấp, Admin bổ nhiệm Trưởng bộ môn. Kiểm tra đề tài nháp phải được duyệt đúng bộ môn trước khi Trưởng khoa công bố; thu hồi chức vụ phải mất quyền ở phiên đang đăng nhập.

## Email công bố kết quả

Chạy `tools/Start-Local.ps1 -Docker -LocalEmail` để bật SMTP vào Mailpit trên máy; hộp thư http://localhost:8025. Biến chỉ được ghi đè trong tiến trình, không đổi `.env`. Bản native dùng `tools/Start-Local.ps1 -EnvFile .env.native -LocalEmail`; Docker chỉ chạy hộp thư. Với PORT khác 8080, đặt PUBLIC_BASE_URL đúng trong file cấu hình trước khi công bố. Xem [kịch bản email](EMAIL_LOCAL_TEST.md).

## Nâng cấp thời hạn duyệt và phân công GVHD (SQL 08–09)

Database có 01–07 cần chạy `tools/Update-TopicReviewDeadline.ps1` (backup và áp dụng 08), sau đó sao lưu và chạy `09_department_supervisor_assignment.sql` theo [hướng dẫn phân công GVHD](DEPARTMENT_SUPERVISOR_WORKFLOW.md), rồi rebuild. Database đã có 01–08 chỉ cần bước 09. Không chạy lại migration đã có hoặc nhập lại mẫu để nâng cấp. Xem [PRODUCT_RULES.md](PRODUCT_RULES.md) cho sửa hội đồng, gia hạn và quy tắc duyệt trước Begin_SV. Thử cả trước, đúng và sau Begin_SV: chỉ thời điểm trước mốc mới được duyệt/công bố đề tài.


## Nghiệm thu truy vấn danh sách (SQL 10)

Với DB đã đủ 01–09, sao lưu rồi chạy `projects/Database/10_read_query_indexes.sql` bằng Workbench trên schema `quanlydetai`. Script chỉ thêm index, có thể chạy lại; không nạp lại mẫu, không xóa volume. Code vẫn chạy trên schema 09; index 10 hỗ trợ hiệu năng và không thêm cột bắt buộc. Volume mới sẽ tự chạy 01–10 qua Compose. Dừng web lúc chạy migration để giảm tranh chấp DDL.

Kiểm tra bộ lọc đề tài theo từ khóa không dấu, bộ môn, đợt, loại và trạng thái cùng lúc; chuyển trang phải giữ bộ lọc. Trang duyệt phải chuyển riêng danh sách đề tài/đăng ký/kết quả. Thử SV, GVHD, GVPB, thành viên hội đồng, GV không liên quan và Trưởng bộ môn để xác nhận tổng số và dữ liệu không vượt quyền. Danh sách tài khoản giữ chức vụ; báo cáo giữ lịch sử và chấm trên phiên bản mới nhất.

`CatalogIntegrationTest` đo số entity/SQL trên trang 7 dòng giữa 42 bản ghi; `WorkflowIntegrationTest` kiểm tra trang 5 báo cáo giữa 40 phiên bản, cùng kiểm tra quyền và phiên bản báo cáo mới nhất. Các fixture nằm trong DB kiểm thử và rollback. Test đồng thời MySQL dùng schema/container test riêng theo cấu hình dưới đây, không chạy trên DB vận hành.


Chạy các test MySQL trong PowerShell tại `projects/quanlydetai`, sau khi container `mysql-test` đã khởi tạo đủ 01–10 (root repository: `docker compose -f compose.test.yaml up -d --wait mysql-test`):

```powershell
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:3308/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh&sslMode=DISABLED'
$env:TEST_DB_DRIVER='com.mysql.cj.jdbc.Driver'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD='isolated-test-only'
$env:TEST_DDL_MODE='validate'
$env:TEST_DB_INIT_SQL="SET time_zone='+07:00'"
.\mvnw.cmd '-Dtest=FoundationIntegrationTest,CatalogIntegrationTest,WorkflowIntegrationTest,MySqlTriggerIntegrationTest,MySqlConcurrencyIntegrationTest' test
```

Mật khẩu trên chỉ thuộc container test trong `compose.test.yaml`. Test đồng thời chỉ cho chạy khi URL trỏ localhost/127.0.0.1 cổng 3308, mở hai thread với hai transaction thật. Hai tình huống: hai nhóm cùng giữ một đề tài; một nhóm cùng giữ hai đề tài. Mỗi tình huống chỉ được có một đăng ký thành công; fixture được dọn theo mã riêng sau test. Xóa các biến `TEST_DB_*`/`TEST_DDL_MODE` ở phiên PowerShell hoặc mở phiên mới trước khi chạy lại bộ H2. Trong GitHub Actions, MySQL test cũng dùng cổng 3308 và khởi tạo 01–10.

Teardown của test đồng thời phải dọn lịch sử công bố bất biến: chỉ tại connection/schema test cổng 3308, nó tạm tắt kiểm tra FK để xóa đúng các mã `QC_*` rồi bật lại trong `finally`. Không đổi định nghĩa trigger/constraint. Khi hai transaction nghiệp vụ chạy, toàn bộ FK/UNIQUE/trigger vẫn được bật. Không sao chép đoạn teardown này vào code ứng dụng hoặc chạy trên DB vận hành.

Nếu container test đã tồn tại từ bản code cũ, `up -d` không tự chạy lại script khởi tạo. Kết nối Workbench vào 127.0.0.1:3308 với thông tin test ở trên và chạy các migration còn thiếu, nhất là 09 (hai cột xác nhận đề tài) và 10, rồi chạy test. Không dùng connection cổng 3307/3306 của database ứng dụng cho thao tác này.
