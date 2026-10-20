# Quy tắc bản đầu và thay đổi CSDL

Các lựa chọn dưới đây là quy tắc triển khai cho bản đồ án; công thức do nhóm thiết kế theo quyền lựa chọn đã xác nhận. Khi đưa vào khoa, người phụ trách nghiệp vụ cần xác nhận quy tắc này. Không tuyên bố đây là quy chế chung của mọi trường.

## Quy trình

- Một khoa, bốn loại đợt MonHoc/NCKH/TLCN/KLTN. Giai đoạn GV trước giai đoạn SV. Hạn GVPB chỉ TLCN/KLTN; ngày hội đồng chỉ KLTN.
- GV chỉ đề xuất nội dung/bộ môn trong thời gian GV, không chọn GVHD. Trưởng bộ môn duyệt trong phạm vi bộ môn và chọn 1–2 GVHD; Trưởng khoa xác nhận danh sách rồi công bố trong cùng transaction. Cả hai phải hoàn tất trước Begin_SV (chặn cả thời điểm bằng Begin_SV). Một trong hai bước có thể từ chối, chuyển trạng thái Huy. Sửa nháp hủy xác nhận bộ môn và xóa danh sách GVHD; bộ môn phải duyệt và chọn lại. Nhóm chỉ chọn đề tài công bố cùng đợt trong thời gian SV.
- Một SV thuộc tối đa một nhóm **trong mỗi đợt**, theo thiết kế SQL ban đầu. Theo rubric cập nhật, đăng ký đề tài cần 3–5 SV đã xác nhận, đúng một nhóm trưởng. Nhóm đang lập có thể có 1–2 SV. Lời mời cần SV xác nhận. Nhóm trưởng được giữ nguyên trong bản đầu.
- Trưởng khoa duyệt đăng ký nhóm. Một đăng ký đang chờ hoặc đã chấp thuận giữ một nhóm/một đề tài bằng UNIQUE; từ chối hoặc rút giải phóng chỗ. Rút chỉ khi chờ duyệt và còn giai đoạn SV. Đăng ký nộp đúng hạn có thể được duyệt sau hạn.
- Thành viên bị khóa khi chờ duyệt hoặc đã được chấp thuận. Khi bị từ chối/rút, nhóm chưa được gán đề tài nên có thể chỉnh thành viên và đăng ký lại trong thời gian SV.
- Tất cả loại đợt trong bản đầu dùng một GVPB và hội đồng 3–5 người, đúng một chủ tịch và một thư ký, theo SQL gốc. GVHD không chấm đề tài mình hướng dẫn. GVPB độc lập với thành viên hội đồng của cùng đề tài.
- Nhóm trưởng nộp báo cáo sau khi được duyệt. Mỗi lần nộp là một phiên bản bất biến. Khi có phiếu điểm đầu tiên, nộp bản mới bị khóa; mọi người chấm cùng bản mới nhất tại thời điểm đó. Các bản trước vẫn được giữ để đối chiếu.

## Điểm và công bố

`AVG_REVIEWERS_V1`: trung bình cộng **từng phiếu của từng người**, bao gồm một GVPB và tất cả thành viên hội đồng. Mỗi người trọng số như nhau. Dùng BigDecimal, làm tròn HALF_UP hai chữ số sau khi tính tổng/chia. MySQL dùng ROUND(AVG(Diem),2) trên số DECIMAL với cùng quy tắc.

Ví dụ GVPB=9, hội đồng=6,6,6 → (9+6+6+6)/4=6,75. Công thức cũ AVG(AVG_GVPB,AVG_HD) cho 7,50; đã thay trong trigger cho **kết quả tạo mới**. Migration không thay kết quả đã tồn tại.

Chỉ nhận điểm 0–10, tối đa hai chữ số thập phân và nhận xét. GVPB cần nộp trước hạn khi đợt có hạn. Chủ tịch chỉ tổng hợp khi đủ mọi phiếu; tổng hợp khóa điểm và hoàn thành đề tài. Trưởng khoa công bố sau tổng hợp. Mọi thành viên nhóm xem chung điểm cuối và nhận xét tổng hợp, sau khi công bố; không tự gán ngưỡng đậu/rớt hoặc xếp loại.

## Schema sau nâng cấp

Giữ 19 bảng gốc, thêm `DANG_KY_DE_TAI`, `LOI_MOI_NHOM`, `EMAIL_KET_QUA`, `TRUONG_BO_MON`, `DUYET_BO_MON`: tổng cộng **24 bảng**, 4 view và 36 trigger. DB mới chạy script 01–08 theo thứ tự. Script 06 bổ sung hàng đợi email và thay hai trigger sĩ số; không sửa thành viên/điểm đã có. Nhóm dưới 3 SV đã được duyệt trước migration giữ nguyên lịch sử; đăng ký mới áp dụng 3–5. Nhóm cũ đang chờ duyệt nhưng thiếu người phải rút để bổ sung thành viên trong thời gian SV đăng ký, rồi nộp lại.

Script 05 bổ sung danh mục bốn bộ môn theo lựa chọn của nhóm và hợp nhất các liên kết từ danh mục mẫu cũ. Dữ liệu thực hành khoa CNTT nằm ngoài source Java/JAR, mặc định tắt, chỉ được nhập ở dev; không tự cập nhật database đã có tài khoản. Thao tác refresh có chủ đích giữ ID/quan hệ, mật khẩu/trạng thái và lịch đợt cũ, xem SAMPLE_DATA.md.

`TRUONG_KHOA.Dang_Giu_Chuc` cho phép thu hồi quyền mà vẫn giữ FK lịch sử đợt. `DIEM_THANH_PHAN` có CHECK 0–10. Đăng ký có các trường Active_Group/Active_Topic UNIQUE và CHECK phù hợp trạng thái để giữ chỗ an toàn. Thành viên và báo cáo có trigger khóa khi quy trình tiến triển.

`BAO_CAO.File_Dinh_Kem` lưu `local:reports/<uuid>.<ext>` hoặc `supabase:reports/<uuid>.<ext>`. Không lưu URL public hoặc signed URL. `Ma_BC` là token lần nộp 24 ký tự; gửi lại cùng token đã lưu không tạo phiên bản mới. File upload được bù xóa sau rollback CSDL; nếu cloud không xóa được, log ghi object key để đối soát. Upload timeout vẫn có thể để lại object mồ côi, cần đối soát storage trước vận hành lâu dài.

Nâng cấp CSDL cũ không di chuyển URL báo cáo hoặc đổi điểm đã công bố; các dữ liệu đó cần kế hoạch chuyển đổi riêng nếu nhóm đã có dữ liệu thật. Dữ liệu demo mới dùng đúng định dạng hiện tại.

## Phạm vi còn sau nghiệm thu local

Deploy bằng tài khoản cloud thật, test Supabase/Aiven thật, kiểm tra restart/restore và ghi URL; ảnh chụp giao diện sau khi nhóm chạy local; tổng hợp Word/ERD/ZIP theo yêu cầu nộp. Kết quả được công bố và tra cứu trên website; email kết quả từng SV được khôi phục theo rubric ngày 18/10/2026. Chat, SMS, các email tự động khác, SSO, mobile, AI, đa khoa/đa trường, thống kê nâng cao, import Excel và cấu hình quy trình tùy ý để sau bản đầu.

## Công bố, email và tra cứu kết quả

Chủ tịch tổng hợp đủ phiếu rồi Trưởng khoa công bố. Một transaction ghi trạng thái DaCongBo, thời điểm và một email riêng cho từng SV của nhóm. Nếu transaction rollback, không còn thư để gửi. UNIQUE (Ma_De_Tai, Ma_SV) và khóa kết quả chặn công bố lặp tạo thêm thư. Không tự tạo thư cho kết quả đã công bố từ trước.

Worker gửi sau commit qua SMTP hoặc HTTPS Brevo. Khi tắt gửi, thư vẫn ở hàng đợi. Mỗi thư có tên SV, đề tài, đợt, điểm và link `/student/results?period=...`. Chỉ nhóm mình và kết quả đã công bố được xem sau đăng nhập; email không cấp quyền truy cập mới.

Worker thử tối đa 5 lần, giãn cách tăng dần; quá giới hạn thành FAILED để Trưởng khoa thử lại tại `/dean/result-emails`. ACCEPTED chỉ chứng minh dịch vụ nhận thư, không chứng minh Inbox. Nếu phản hồi gửi bị mất, retry có thể tạo thư trùng; không cam kết SMTP exactly-once. Có 24 entity, 24 bảng vật lý và 9 repository Spring Data sau SQL 01–08. Database đã có SQL 06 không cần đổi schema để khôi phục tính năng.

## Chức vụ Trưởng bộ môn

Admin bổ nhiệm GV thuộc đúng bộ môn, một người đương nhiệm mỗi bộ môn. ROLE_TRUONGBOMON được suy ra từ TRUONG_BO_MON; thu hồi được kiểm tra lại trên mỗi request. DUYET_BO_MON ghi xác nhận hiện tại, không phải lịch sử mọi lần duyệt. Migration 07 không tự bổ nhiệm hoặc tạo phiếu duyệt cho dữ liệu cũ. Quy tắc hai bước được thực thi trong service; SQL 07 thêm bảng/FK, không thêm trigger.

Bản đầu cho phép Trưởng bộ môn duyệt đề xuất do chính mình tạo; bước Trưởng khoa vẫn bắt buộc. Người kiêm cả hai chức vụ chưa bị ràng buộc phải có người duyệt khác. Nếu khoa yêu cầu người duyệt độc lập, cần bổ sung quy trình ủy quyền. Admin chỉ sửa thông tin hồ sơ, không đổi mã, username hoặc vai trò qua API sửa hồ sơ; phải thu hồi chức Trưởng bộ môn trước khi chuyển bộ môn. Tài khoản/hồ sơ đã có liên kết lịch sử chỉ được khóa, không xóa.

## Sửa đề tài, hội đồng và gia hạn đợt

Đề xuất nháp sửa được bộ môn và nội dung, không sửa GVHD; hủy duyệt cũ và danh sách GVHD; hội đồng DangLap chưa dùng được sửa/xóa. Đợt có dữ liệu được gia hạn End_SV/hạn GVPB, không rút ngắn hoặc đổi các mốc bắt đầu, loại/ngày hội đồng; sau có kết quả tổng hợp chỉ đổi tên. SQL 08 bổ sung kiểm tra hạn duyệt/công bố, không đổi dữ liệu cũ. Chi tiết endpoint xem [REST_API.md](REST_API.md).
