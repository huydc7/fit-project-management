# Từ điển CSDL bản đầu

Đối chiếu các script 01–10: 24 bảng. Ràng buộc đầy đủ nằm trong SQL; quy tắc ứng dụng ở PRODUCT_RULES.md. FK gồm nhiều cột được ghi riêng dưới bảng. Bảng EMAIL_KET_QUA được bổ sung theo rubric ngày 06/10/2026.

## BO_MON

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Bo_Mon | VARCHAR(20) | Không | PK |
| Ten_Bo_Mon | VARCHAR(150) | Không |  |

## GV

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_GV | VARCHAR(20) | Không | PK |
| Ho_Ten | VARCHAR(150) | Không |  |
| Email | VARCHAR(255) | Không |  |
| SDT | VARCHAR(20) | Có |  |
| Ma_Bo_Mon | VARCHAR(20) | Không | FK → BO_MON |

- `(Ma_Bo_Mon)` → `BO_MON(Ma_Bo_Mon)`

## SV

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_SV | VARCHAR(20) | Không | PK |
| Ho_Ten | VARCHAR(150) | Không |  |
| Email | VARCHAR(255) | Không |  |
| SDT | VARCHAR(20) | Có |  |

## TRUONG_KHOA

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_GV | VARCHAR(20) | Không | PK, FK → GV |
| Dang_Giu_Chuc | BOOLEAN | Không |  |

- `(Ma_GV)` → `GV(Ma_GV)`

## DOT_DANG_KY

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Dot | VARCHAR(20) | Không | PK |
| Ma_Truong_Khoa | VARCHAR(20) | Không | FK → TRUONG_KHOA |
| Ten_Dot_Dang_Ky | VARCHAR(200) | Không |  |
| Loai | VARCHAR(20) | Không |  |
| Begin_GV | DATETIME | Không |  |
| End_GV | DATETIME | Không |  |
| Begin_SV | DATETIME | Không |  |
| End_SV | DATETIME | Không |  |
| Deadline_GVPB_Nop_Diem | DATETIME | Có |  |
| Time_BC_HD | DATETIME | Có |  |

- `(Ma_Truong_Khoa)` → `TRUONG_KHOA(Ma_GV)`

## DE_TAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_De_Tai | VARCHAR(20) | Không | PK |
| Ten_De_Tai | VARCHAR(300) | Không |  |
| Ma_Bo_Mon | VARCHAR(20) | Không | FK → BO_MON |
| Noi_Dung | TEXT | Không |  |
| Trang_Thai | VARCHAR(30) | Không |  |
| Ma_Dot | VARCHAR(20) | Không | FK → DOT_DANG_KY |
| Ma_GV_Dang_Ky | VARCHAR(20) | Không | FK → GV |
| Ngay_Dang_Ky | DATETIME | Không |  |

- `(Ma_Bo_Mon)` → `BO_MON(Ma_Bo_Mon)`
- `(Ma_Dot)` → `DOT_DANG_KY(Ma_Dot)`
- `(Ma_GV_Dang_Ky)` → `GV(Ma_GV)`

## GVHD_DETAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_GV | VARCHAR(20) | Không | PK, FK → GV |
| Ma_De_Tai | VARCHAR(20) | Không | PK, FK → DE_TAI |

- `(Ma_GV)` → `GV(Ma_GV)`
- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`

## GVPB_DETAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_GV | VARCHAR(20) | Không | PK, FK → GV |
| Ma_De_Tai | VARCHAR(20) | Không | PK, FK → DE_TAI |
| Ngay_Phan_Cong | DATETIME | Không |  |

- `(Ma_GV)` → `GV(Ma_GV)`
- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`

## NHOM

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Nhom | VARCHAR(20) | Không | PK |
| Ten_Nhom | VARCHAR(150) | Không |  |
| Ma_Dot | VARCHAR(20) | Không | FK → DOT_DANG_KY, FK → DE_TAI |
| Ma_De_Tai | VARCHAR(20) | Có | FK → DE_TAI |
| Ngay_Chon_De_Tai | DATETIME | Có |  |

- `(Ma_Dot)` → `DOT_DANG_KY(Ma_Dot)`
- `(Ma_De_Tai, Ma_Dot)` → `DE_TAI(Ma_De_Tai, Ma_Dot)`

## THANH_VIEN_NHOM

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Nhom | VARCHAR(20) | Không | PK, FK → NHOM |
| Ma_SV | VARCHAR(20) | Không | PK, FK → SV |
| Vai_Tro | VARCHAR(30) | Không |  |

- `(Ma_Nhom)` → `NHOM(Ma_Nhom)`
- `(Ma_SV)` → `SV(Ma_SV)`

## HOI_DONG

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_HD | VARCHAR(20) | Không | PK |
| Ma_Dot | VARCHAR(20) | Không | FK → DOT_DANG_KY |
| Trang_Thai | VARCHAR(20) | Không |  |

- `(Ma_Dot)` → `DOT_DANG_KY(Ma_Dot)`

## HOI_DONG_DE_TAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_HD | VARCHAR(20) | Không | PK, FK → HOI_DONG |
| Ma_De_Tai | VARCHAR(20) | Không | PK, FK → DE_TAI |

- `(Ma_HD)` → `HOI_DONG(Ma_HD)`
- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`

## THANHVIEN_HOIDONG

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_HD | VARCHAR(20) | Không | PK, FK → HOI_DONG |
| Ma_GV | VARCHAR(20) | Không | PK, FK → GV |
| Vai_Tro | VARCHAR(20) | Không |  |

- `(Ma_HD)` → `HOI_DONG(Ma_HD)`
- `(Ma_GV)` → `GV(Ma_GV)`

## LOAI_DIEM

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Loai_Diem | VARCHAR(20) | Không | PK |
| Ten_Loai_Diem | VARCHAR(150) | Không |  |

## DIEM_THANH_PHAN

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Diem | VARCHAR(30) | Không | PK |
| Ma_GV | VARCHAR(20) | Không | FK → GV |
| Ma_De_Tai | VARCHAR(20) | Không | FK → DE_TAI |
| Ma_Loai_Diem | VARCHAR(20) | Không | FK → LOAI_DIEM |
| Diem | DECIMAL(5,2) | Không |  |
| Ngay_Cham | DATETIME | Không |  |
| Ngay_Nop_Diem | DATETIME | Không |  |
| Nhan_Xet | TEXT | Có |  |

- `(Ma_GV)` → `GV(Ma_GV)`
- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`
- `(Ma_Loai_Diem)` → `LOAI_DIEM(Ma_Loai_Diem)`

## KET_QUA_DE_TAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_De_Tai | VARCHAR(20) | Không | PK, FK → DE_TAI |
| Diem_Cuoi | DECIMAL(5,2) | Không |  |
| Nhan_Xet_Tong_Hop | TEXT | Có |  |
| Ket_Qua | VARCHAR(100) | Có |  |
| Ma_GV_Tong_Hop | VARCHAR(20) | Không | FK → GV |
| Trang_Thai_Cong_Bo | VARCHAR(20) | Không |  |
| Ngay_Tong_Hop | DATETIME | Không |  |
| Ngay_Cong_Bo | DATETIME | Có |  |

- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`
- `(Ma_GV_Tong_Hop)` → `GV(Ma_GV)`

## TAI_KHOAN

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_TK | VARCHAR(30) | Không | PK |
| Ma_SV | VARCHAR(20) | Có | FK → SV |
| Ma_GV | VARCHAR(20) | Có | FK → GV |
| Ten_Dang_Nhap | VARCHAR(100) | Không |  |
| Mat_Khau_Hash | VARCHAR(255) | Không |  |
| Vai_Tro | VARCHAR(20) | Không |  |
| Trang_Thai | VARCHAR(20) | Không |  |
| Ngay_Tao | DATETIME | Không |  |
| Lan_Dang_Nhap_CuoiCung | DATETIME | Có |  |

- `(Ma_SV)` → `SV(Ma_SV)`
- `(Ma_GV)` → `GV(Ma_GV)`

## THONG_BAO

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_TB | VARCHAR(30) | Không | PK |
| Tieu_De | VARCHAR(300) | Không |  |
| Noi_Dung | TEXT | Không |  |
| Ngay_Dang | DATETIME | Không |  |
| Pham_Vi | VARCHAR(20) | Không |  |
| Ma_TK | VARCHAR(30) | Không | FK → TAI_KHOAN |

- `(Ma_TK)` → `TAI_KHOAN(Ma_TK)`

## BAO_CAO

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_BC | VARCHAR(30) | Không | PK |
| Ma_Nhom | VARCHAR(20) | Không | FK → NHOM |
| Ma_SV_Nguoi_Nop | VARCHAR(20) | Không | FK → SV |
| Ngay_Nop | DATETIME | Không |  |
| File_Dinh_Kem | VARCHAR(500) | Không |  |
| Lan_Nop | INT | Không |  |

- `(Ma_Nhom)` → `NHOM(Ma_Nhom)`
- `(Ma_SV_Nguoi_Nop)` → `SV(Ma_SV)`

## DANG_KY_DE_TAI

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_DK | VARCHAR(30) | Không | PK |
| Ma_Nhom | VARCHAR(20) | Không | FK → NHOM |
| Ma_De_Tai | VARCHAR(20) | Không | FK → DE_TAI |
| Trang_Thai | VARCHAR(20) | Không |  |
| Ngay_Dang_Ky | DATETIME | Không |  |
| Ly_Do | VARCHAR(1000) | Có |  |
| Ma_GV_Duyet | VARCHAR(20) | Có | FK → GV |
| Ngay_Duyet | DATETIME | Có |  |
| Active_Group | VARCHAR(20) | Có |  |
| Active_Topic | VARCHAR(20) | Có |  |

- `(Ma_Nhom)` → `NHOM(Ma_Nhom)`
- `(Ma_De_Tai)` → `DE_TAI(Ma_De_Tai)`
- `(Ma_GV_Duyet)` → `GV(Ma_GV)`

## LOI_MOI_NHOM

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Loi_Moi | VARCHAR(30) | Không | PK |
| Ma_Nhom | VARCHAR(20) | Không | FK → NHOM |
| Ma_SV | VARCHAR(20) | Không | FK → SV |
| Trang_Thai | VARCHAR(20) | Không |  |
| Ngay_Moi | DATETIME | Không |  |

- `(Ma_Nhom)` → `NHOM(Ma_Nhom)`
- `(Ma_SV)` → `SV(Ma_SV)`

## EMAIL_KET_QUA — hàng đợi email kết quả

Mỗi kết quả công bố tạo một bản ghi cho mỗi SV. UNIQUE đề tài/SV tránh tạo lại khi công bố lặp. Worker xử lý PENDING → PROCESSING → ACCEPTED hoặc thử lại có giới hạn → FAILED. ACCEPTED nghĩa là dịch vụ đã nhận, chưa chứng minh thư tới Inbox. Dữ liệu cũ được giữ; không tự tạo thư cho kết quả đã công bố trước khi khôi phục module.

Lưu thông báo cá nhân cùng transaction công bố điểm; worker gửi sau khi transaction commit. Nội dung và người nhận là snapshot tại thời điểm công bố.

| Cột | Kiểu SQL | Cho phép NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Email | VARCHAR(32) | Không | PK |
| Ma_De_Tai | VARCHAR(20) | Không | FK → KET_QUA_DE_TAI |
| Ma_SV | VARCHAR(20) | Không | FK → SV |
| Nguoi_Nhan | VARCHAR(255) | Không | Email người nhận |
| Tieu_De | VARCHAR(255) | Không | Tiêu đề |
| Noi_Dung | TEXT | Không | Nội dung plain text |
| Trang_Thai | VARCHAR(20) | Không | PENDING / PROCESSING / ACCEPTED / FAILED |
| So_Lan_Thu | INT | Không | Mặc định 0 |
| Ngay_Tao | DATETIME | Không | Thời điểm tạo thông báo |
| Lan_Thu_Tiep | DATETIME | Không | Lịch retry hoặc hết hạn lease xử lý |
| Ngay_Chap_Nhan | DATETIME | Có | Dịch vụ gửi đã tiếp nhận |
| Ma_Dich_Vu | VARCHAR(512) | Có | Mã xác nhận dịch vụ |

- `(Ma_De_Tai)` → `KET_QUA_DE_TAI(Ma_De_Tai)`; một kết quả có thể có 0..n thông báo (kết quả chưa công bố hoặc lịch sử trước migration chưa có thư).
- `(Ma_SV)` → `SV(Ma_SV)`; một sinh viên có 0..n thông báo qua các đề tài.
- UNIQUE `(Ma_De_Tai, Ma_SV)` ngăn tạo nhiều bản ghi công bố cho cùng sinh viên/đề tài.
- INDEX `(Trang_Thai, Lan_Thu_Tiep)` hỗ trợ worker chọn thư đến hạn. CHECK giới hạn bốn trạng thái.
- ACCEPTED không có nghĩa thư chắc chắn tới Inbox. Nhà cung cấp đã nhận nhưng phản hồi bị mất vẫn có khả năng gửi trùng khi retry.

## TRUONG_BO_MON (migration 07)

| Cột | Kiểu SQL | NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_Bo_Mon | VARCHAR(20) | Không | PK, FK → BO_MON.Ma_Bo_Mon |
| Ma_GV | VARCHAR(20) | Không | FK → GV.Ma_GV |
| Dang_Giu_Chuc | BOOLEAN DEFAULT TRUE | Không | Quyền đương nhiệm |

Mỗi bộ môn tối đa một bản ghi chức vụ; ứng dụng kiểm tra GV thuộc bộ môn. Không phải bảng lịch sử nhiệm kỳ.

## DUYET_BO_MON (migration 07)

| Cột | Kiểu SQL | NULL | Khóa/liên kết |
|---|---|---|---|
| Ma_De_Tai | VARCHAR(20) | Không | PK, FK → DE_TAI.Ma_De_Tai |
| Ma_GV | VARCHAR(20) | Không | FK → GV.Ma_GV, người duyệt |
| Ngay_Duyet | DATETIME | Không | Thời điểm duyệt |

Mỗi đề tài tối đa một xác nhận hiện tại; sửa nháp hủy xác nhận. FK của cả hai bảng dùng RESTRICT, không xóa dây chuyền lịch sử. SQL 07 chỉ thêm bảng/FK; sau toàn bộ SQL 01–10 có 38 trigger, 4 view.

## Bổ sung SQL 09: xác nhận danh sách GVHD

DE_TAI thêm Ma_GV_Xac_Nhan VARCHAR(20) NULL, FK tới GV(Ma_GV), và Ngay_Xac_Nhan DATETIME NULL.
Dữ liệu cũ giữ NULL, không suy đoán người xác nhận. GVHD_DETAI giữ danh sách do bộ môn chọn;
DUYET_BO_MON ghi người/ngày quyết định. Khi còn xác nhận bộ môn, trigger khóa thêm/xóa GVHD.
Sửa đề tài nháp phải hủy xác nhận trước rồi xóa danh sách. Duyệt bộ môn mới yêu cầu đủ 1–2 GVHD.


## Index phục vụ danh sách (migration 10)

`10_read_query_indexes.sql` bổ sung bảy index, không đổi bảng/cột/dữ liệu nghiệp vụ: đề tài theo trạng thái, đợt–trạng thái, bộ môn–trạng thái; tài khoản theo vai trò–tên đăng nhập; báo cáo/thông báo theo ngày; đăng ký theo trạng thái–ngày. Các cột mã cuối index là khóa sắp xếp phụ, giúp chuyển trang có thứ tự xác định khi nhiều dòng trùng ngày. UNIQUE hiện hữu trên báo cáo `(Ma_Nhom,Lan_Nop)` đã hỗ trợ MAX phiên bản, không tạo index trùng cho cặp đó. Cần xem EXPLAIN với dữ liệu thực; tìm chuỗi con không dấu không được bảo đảm dùng B-tree.
