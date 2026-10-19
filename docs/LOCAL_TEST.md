# Chạy và kiểm thử local

Ứng dụng dùng MySQL thật ở cả hai cách chạy. MySQL Docker ở cổng 3307; MySQL cài trên máy thường ở cổng 3306. Hai database độc lập. Trong lần làm việc này, website Docker chạy ở http://localhost:8080.

## Docker

1. Mở Docker Desktop, đợi engine chạy.
2. Tại D:\project_web\final_projects, tạo .env từ .env.example nếu chưa có. Điền MYSQL_ROOT_PASSWORD và DB_PASSWORD; giữ DB_USERNAME=quanlydetai, MYSQL_PORT=3307, PORT=8080, STORAGE_BACKEND=local.
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

Ứng dụng cần chạy qua bước ApplicationRunner; chỉ dòng Started chưa đảm bảo nhập dữ liệu thành công. Kiểm tra http://localhost:8080/actuator/health trả UP và đăng nhập thực tế. Nếu mật khẩu mẫu không hợp lệ, sửa .env rồi tạo lại container bằng `docker compose --profile app up -d app`; restart không nạp biến mới.

Dừng bằng `docker compose --profile app down`, dữ liệu còn trong volume. Chạy lại bằng `docker compose --profile app up -d`. Khi sửa source, thêm --build. Không dùng down -v để nâng cấp. SQL 01–08 tự nạp chỉ khi tạo volume lần đầu; volume cũ cần nạp migration thích hợp bằng Workbench/CLI.

## Xem MySQL Docker bằng Workbench

Tạo connection Standard TCP/IP: hostname 127.0.0.1, port 3307, username từ DB_USERNAME, password từ DB_PASSWORD, default schema quanlydetai. Đây không phải mật khẩu đăng nhập web. Test Connection rồi mở SCHEMAS → quanlydetai → Tables; nhấp phải bảng → Select Rows – Limit 1000.

## MySQL đã cài + Java

Dùng JDK 21 và MySQL 8.0.16+. Nạp toàn bộ script 01–08 từ projects/Database vào DB phát triển. Kiểm tra Output không có lỗi. Database cũ đủ 01–03 cần sao lưu rồi nạp 04/05/06/07; DB đủ 01–05 nâng cấp 06 rồi 07, không đổi Hibernate thành create/update. Docker có script `tools/Update-RubricDatabase.ps1` sao lưu trước migration. Đăng ký đề tài mới cần 3–5 SV; xem [hướng dẫn triển khai](HUONG_DAN_DEPLOY_TUNG_BUOC.md).

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

1. gv.hoangnam đề xuất vào một đợt còn trong thời gian GV; Trưởng bộ môn đã được Admin bổ nhiệm duyệt đúng bộ môn trước, rồi truongkhoa duyệt công bố. Thử đề xuất vào đợt đã qua giai đoạn GV để kiểm tra chặn.
2. sv.22110001 tạo nhóm ở đợt Đồ án môn học học kỳ I, mời hai SV và từng người xác nhận lời mời. Nhóm cần 3–5 người khi đăng ký; mỗi SV chỉ có một nhóm mỗi đợt.
3. Nhóm trưởng đăng ký đề tài Cổng đăng ký và quản lý đề tài sinh viên. truongkhoa chấp thuận. Thử rút/từ chối với yêu cầu khác để xác nhận giải phóng giữ chỗ.
4. truongkhoa phân công gv.minhanh phản biện; hội đồng gồm gv.thanhbinh chủ tịch, gv.thuha thư ký, gv.quochuy thành viên. Chốt rồi gán đề tài. GVHD gv.hoangnam không được chấm.
5. Nhóm trưởng nộp PDF/DOCX/ZIP dưới 20 MiB; thử nhiều phiên bản, tải đúng file và kiểm tra người ngoài nhóm không tải được.
6. Phản biện chấm 9, mỗi thành viên hội đồng chấm 6, có nhận xét. Điểm -1, 10.01 hoặc 6.123 bị chặn. Sau phiếu đầu tiên, không nộp báo cáo mới.
7. Chủ tịch tổng hợp khi đủ phiếu: điểm 6,75. Thư ký không được tổng hợp; sau tổng hợp các phiếu bị khóa.
8. Trưởng khoa công bố trước Begin_SV. Thành viên nhóm xem kết quả theo đợt; SV ngoài nhóm không xem hoặc tải bằng cách đổi ID. Trước công bố, nhóm chưa thấy điểm. Nếu chạy với `-LocalEmail`, mở http://localhost:8025: mỗi thành viên nhận một thư có điểm và link đúng đợt. Trưởng khoa theo dõi tại `/dean/result-emails`; sinh viên vẫn tra cứu trên website.
9. admin thử tạo, khóa/mở và reset mật khẩu tài khoản; kiểm tra phiên cũ hết quyền. Admin đăng thông báo Nhà trường; Trưởng khoa chỉ đăng Khoa.

Mỗi lần đổi vai trò, đăng xuất hoặc dùng trình duyệt/profile khác. Đợt và đề tài cũ giữ mã và thời gian đã có; đối chiếu tên trên màn hình, không giả định mã mới đã thay toàn bộ mã cũ.

## Nghiệm thu trước cloud

- Luồng trên chạy trên MySQL của nhóm; kiểm tra quyền, ngoài hạn, nhóm dưới 3 người khi đăng ký, thành viên thứ sáu, đăng ký trùng, thiếu phiếu, GVHD tự chấm và điểm trước công bố.
- File/database còn sau restart; kiểm tra upload/download và định dạng/dung lượng sai.
- Chạy clean verify; dùng DB kiểm thử riêng cho test MySQL theo README, không dùng DB vận hành.
- Kiểm tra giao diện màn hình nhỏ; chụp ảnh bằng dữ liệu mà nhóm đã nghiệm thu.
- Thống nhất PRODUCT_RULES; prod tắt dữ liệu thực hành, cấu hình cloud theo CLOUD_SETUP.

Các bước trên kiểm tra môi trường local. Triển khai cloud cần nghiệm thu riêng theo [CLOUD_SETUP.md](CLOUD_SETUP.md).

## Duyệt hai bước từ 15/10/2026

Database đã có 01–06: chạy `tools/Update-DepartmentReview.ps1` trước khi rebuild ứng dụng. Script sao lưu rồi thêm hai bảng; không nạp lại mẫu. Sau nâng cấp, Admin bổ nhiệm Trưởng bộ môn. Kiểm tra đề tài nháp phải được duyệt đúng bộ môn trước khi Trưởng khoa công bố; thu hồi chức vụ phải mất quyền ở phiên đang đăng nhập.

## Email công bố kết quả

Chạy `tools/Start-Local.ps1 -Docker -LocalEmail` để bật SMTP vào Mailpit trên máy; hộp thư http://localhost:8025. Biến chỉ được ghi đè trong tiến trình, không đổi `.env`. Bản native dùng `tools/Start-Local.ps1 -EnvFile .env.native -LocalEmail`; Docker chỉ chạy hộp thư. Với PORT khác 8080, đặt PUBLIC_BASE_URL đúng trong file cấu hình trước khi công bố. Xem [kịch bản email](EMAIL_LOCAL_TEST.md).

## Nâng cấp 19/10/2026

Database có 01–07 cần chạy `tools/Update-TopicReviewDeadline.ps1` (backup và áp dụng 08), rồi rebuild. Không cần chạy lại 06/07 nếu đã có. Xem [PRODUCT_RULES.md](PRODUCT_RULES.md) cho sửa hội đồng, sửa bộ môn/GVHD của nháp, gia hạn và quy tắc duyệt trước Begin_SV. Thử cả trước, đúng và sau Begin_SV: chỉ thời điểm trước mốc mới được duyệt/công bố đề tài.
