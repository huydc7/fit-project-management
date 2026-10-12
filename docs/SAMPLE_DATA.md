# Danh mục khoa CNTT và dữ liệu thực hành

Danh mục dự án dùng bốn hướng chuyên môn nhóm lựa chọn theo bối cảnh HCMUTE: `CNPM` — Công nghệ phần mềm, `HTTT` — Hệ thống thông tin, `MANG_ANM` — Mạng và An ninh mạng, `TTNT` — Trí tuệ nhân tạo. Đây là cấu hình của đồ án; không coi các hồ sơ hoặc lịch dùng thử là thông tin chính thức của trường.

## Phân biệt dữ liệu

- **Danh mục nghiệp vụ:** `05_department_catalog_mysql.sql` tạo bốn bộ môn. Cùng với hai loại điểm trong script 03, đây là cấu hình cần thiết khi vận hành, không tạo tài khoản hoặc đề tài giả.
- **Dữ liệu thực hành:** chỉ một file `projects/Database/sample-data/fit-hcmute.json`, nằm ngoài source Java và ngoài JAR. Gồm 9 GV, 18 SV, một Admin, 5 đợt thuộc đủ bốn loại và 12 đề tài. Mỗi đề tài có mục tiêu, phạm vi, yêu cầu và sản phẩm bàn giao.
- **Fixture kiểm thử:** nằm trong `src/test`, dùng database kiểm thử riêng và rollback; không đưa vào database đang vận hành.

Tên người, mã sinh viên và email trong bộ thực hành đều giả lập. Email dùng miền `example.test`; không gửi thông báo tới người thật. Không có mật khẩu trong file JSON. Thời gian đợt mới tính theo ngày nhập dữ liệu ở múi giờ Việt Nam để chạy thử luồng; không phải lịch chính thức.

## Nhập dữ liệu thực hành bằng công cụ riêng

Web không có bộ nạp dữ liệu mẫu trong JAR. `SAMPLE_DATA_ENABLED` cũ không còn tác dụng khi chạy web. Để nhập có chủ đích vào **database local** đã có schema 01–06, đặt `SAMPLE_DATA_PASSWORD` trong file `.env` (12–64 ký tự, tối đa 72 byte UTF-8), rồi chạy từ root repository với JDK 21:

```powershell
# MySQL Docker đang chạy, cổng MYSQL_PORT trong .env:
.\tools\Import-SampleData.ps1 -DockerDatabase

# Hoặc MySQL cài trên máy, lấy DB_URL/DB_USERNAME/DB_PASSWORD từ .env.native:
.\tools\Import-SampleData.ps1 -EnvFile .env.native
```

Công cụ chạy Maven với test classpath, dùng main riêng `tools.sample.SampleDataImportApplication`, không mở web server và thoát sau transaction. Không chạy bộ kiểm thử trên database này. Importer mặc định bỏ qua toàn bộ việc nhập nếu database đã có tài khoản; chỉ nhận profile dev. Tài khoản hiện có vẫn đăng nhập bình thường sau khi nâng cấp source, không cần nhập lại.

## Tài khoản thực hành

| Username | Họ tên giả lập / vai trò |
|---|---|
| `admin` | Quản trị viên |
| `truongkhoa` | Nguyễn Minh Quân — GV và Trưởng khoa |
| `gv.hoangnam` | Trần Hoàng Nam — Công nghệ phần mềm |
| `gv.minhanh` | Lê Minh Anh — Hệ thống thông tin |
| `gv.thanhbinh` | Phạm Thanh Bình — Mạng và An ninh mạng |
| `gv.thuha` | Võ Thu Hà — Trí tuệ nhân tạo |
| `gv.quochuy` | Đặng Quốc Huy — Công nghệ phần mềm |
| `gv.khanhlinh` | Bùi Khánh Linh — Hệ thống thông tin |
| `gv.ducduy` | Ngô Đức Duy — Mạng và An ninh mạng |
| `gv.minhchau` | Trịnh Minh Châu — Trí tuệ nhân tạo |
| `sv.22110001` … `sv.22110018` | 18 SV với tên và mã giả lập |

Tài khoản mới lấy mật khẩu từ SAMPLE_DATA_PASSWORD. Tài khoản đã có trước nâng cấp giữ nguyên mật khẩu, trạng thái khóa/mở và vai trò; không tự reset hoặc mở khóa.

## Cập nhật bộ mẫu cũ mà giữ dữ liệu đã nhập

Sao lưu trước khi chạy script 05 và chạy `tools/Import-SampleData.ps1 -DockerDatabase -RefreshExisting` cho **một lần nhập có chủ đích**. Tính năng này chỉ dành cho local và chỉ xử lý các mã được liệt kê trong JSON. Nó đổi tên/email mẫu, username, nội dung đề tài mẫu; bổ sung hồ sơ/đợt/đề tài còn thiếu. Không tạo sẵn nhóm, báo cáo, hội đồng, phiếu điểm hoặc kết quả.

Khóa định danh cũ được giữ nếu đã tồn tại, kể cả mã có tiền tố DEMO_. Đây là để giữ nguyên FK và lịch sử, không phải tiếp tục sinh dữ liệu demo trong code. Đợt cũ chỉ đổi tên, giữ toàn bộ hạn, loại và người tạo. Đề tài cũ giữ trạng thái, đợt, GVHD và quan hệ nhóm. Các bản ghi ngoài danh sách JSON không được cập nhật. Trong bộ danh mục cũ, hai bộ môn được hợp nhất sang CNPM/HTTT với toàn bộ liên kết GV/đề tài được giữ.

Cờ RefreshExisting chỉ có hiệu lực cho lần chạy lệnh đó. Không dùng refresh như tác vụ chạy mỗi lần mở ứng dụng. Kiểm tra trong Workbench:

```sql
USE quanlydetai;
SELECT Ma_Bo_Mon, Ten_Bo_Mon FROM BO_MON;
SELECT Ma_De_Tai, Ten_De_Tai, Ma_Bo_Mon, Trang_Thai FROM DE_TAI;
SELECT Ten_Dang_Nhap, Ma_GV, Ma_SV, Trang_Thai FROM TAI_KHOAN;
```

Khi mời SV hoặc phân công GV, dùng **Ma_SV/Ma_GV thực tế** trong bảng trên hoặc giao diện Admin. Database mới dùng mã sạch trong JSON; database nâng cấp giữ mã cũ. Không dùng username thay cho mã hồ sơ.

## Tổ chức source

Importer và DTO của file JSON ở `src/test/java/com/example/quanlydetai/tools/sample`, được import tường minh bởi công cụ và kiểm thử. Không có component tự quét hay thư mục `config/sample` trong source ứng dụng. Dữ liệu JSON vẫn nằm ở `projects/Database/sample-data`.

Các fixture kiểm thử bảo vệ phân quyền, thời gian, giới hạn nhóm, tính điểm và bảo toàn dữ liệu; chúng không được đóng gói vào JAR. Xem ARCHITECTURE.md để phân biệt controller HTML, REST, service, repository và entity.
