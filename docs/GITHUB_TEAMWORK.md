# Đưa dự án lên GitHub và làm việc nhóm

Thư mục Git của dự án là `final_projects`, không phải thư mục cha `D:\project_web`. Repository hiện tại có lịch sử của nhóm; giữ lịch sử này khi đưa lên repo mới để lưu đóng góp trước đó.

## Tạo repository

Trong GitHub, tạo repository dưới tài khoản chủ nhóm. Có thể đặt tên `fit-project-management`. Chọn Private trong giai đoạn nhóm đang review. Bỏ chọn khởi tạo README, .gitignore và license vì source local đã có README/.gitignore; repository mới cần rỗng để push lịch sử hiện tại.

Sau khi tạo, sao chép URL HTTPS repository. Không gửi token/mật khẩu trong chat hoặc ghi vào URL remote.

## Cấu hình tác giả và commit

Trong PowerShell tại thư mục dự án, kiểm tra cấu hình:

```powershell
cd D:\project_web\final_projects
git status
git remote -v
git config user.name
git config user.email
```

Nếu chưa có tác giả, đặt tên và email của chính người tạo commit. Hai giá trị dưới là chỗ giữ chỗ, cần thay trước khi chạy. Email noreply lấy từ GitHub Settings → Emails nếu muốn giữ riêng email cá nhân.

```powershell
git config --local user.name "TEN_CUA_BAN"
git config --local user.email "EMAIL_CUA_BAN"
git add --all
git diff --cached --stat
git diff --cached --check
git commit -m "Complete local FIT project management baseline"
```

Trước commit, kiểm tra `.env`, `.env.native`, backup, uploads và file build không nằm trong danh sách file thêm. `.env.example` chứa cấu hình mẫu được giữ lại. File target đã bị theo dõi ở lịch sử cũ cần được bỏ theo dõi trong commit mới; thao tác này không xóa file build trên máy.

## Push tới repo mới

Repo cũ có remote `origin`. Có thể giữ remote này và thêm remote `team` cho repo mới, tránh ghi đè địa chỉ cũ. Thay `URL_REPOSITORY_MOI` bằng URL thực đã tạo:

```powershell
git remote add team URL_REPOSITORY_MOI
git push -u team main
```

Nếu `team` đã tồn tại, kiểm tra `git remote -v` trước; không thêm lại. Nếu Git mở cửa sổ đăng nhập, đăng nhập tài khoản có quyền ghi repo mới. Không dùng force push để xử lý lỗi repository không rỗng; xem lại lịch sử hai bên trước khi hợp nhất.

## Mời thành viên

Mở repository trên GitHub → Settings → Collaborators → Add people. Chọn đúng username của từng bạn và gửi lời mời; thành viên cần chấp nhận trước khi clone private repo hoặc push. [Hướng dẫn chính thức](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository).

Bạn bè clone repository mới; remote của bản clone thường tên `origin`. Mỗi người tạo `.env` riêng từ `.env.example`, khởi tạo database trên máy và nhập bộ dữ liệu thực hành theo [LOCAL_TEST.md](LOCAL_TEST.md) và [SAMPLE_DATA.md](SAMPLE_DATA.md). Source không chứa database/volume Docker đang dùng của Huy.

## Review và chỉnh sửa

Mỗi nhiệm vụ dùng một nhánh, ví dụ `feature/topic-search` hoặc `fix/result-filter`. Không sửa trực tiếp main theo quy ước làm việc của nhóm:

```powershell
git switch main
git pull --ff-only
git switch -c feature/topic-search
# Thực hiện thay đổi và kiểm tra
git add --all
git commit -m "Add topic search"
git push -u origin feature/topic-search
```

Sau push, mở Pull Request vào main, mô tả thay đổi và kết quả kiểm thử, chọn bạn khác review. Chỉ merge khi đã xử lý góp ý và kiểm tra phù hợp đạt. Quy ước không push main trực tiếp cần nhóm tuân thủ; chưa tự cấu hình bảo vệ nhánh trên GitHub.

Workflow `.github/workflows/backend.yml` chạy khi push main hoặc mở Pull Request: kiểm thử H2, MySQL riêng nạp SQL 01–10 và build Docker Java 21. Kiểm tra kết quả thực ở tab Actions; kết quả local không bảo đảm workflow cloud đã chạy thành công.

Nội dung review ưu tiên theo [USE_CASES.md](USE_CASES.md) và [PRODUCT_RULES.md](PRODUCT_RULES.md), phân quyền service, quy tắc nhóm và bảo toàn dữ liệu. Thay đổi schema phải có hướng dẫn migration và backup, không yêu cầu các thành viên xóa volume để nâng cấp.

## Xuất ZIP source sau khi commit

Tại thư mục gốc repository, sau khi đã commit đủ thay đổi:

```powershell
git archive --format=zip --output=../fit-project-management-source.zip HEAD
```

Lệnh xuất nội dung của commit HEAD, không gồm lịch sử `.git`, file local bị bỏ qua hoặc thay đổi chưa commit. Kiểm tra `git status` trước khi xuất để tránh gửi nhầm bản cũ. Không nén toàn bộ thư mục đang làm việc vì `.env` vẫn cần ở máy local; `.gitignore` chỉ có tác dụng với Git. SQL 01–10, bộ dữ liệu thực hành, ERD chuẩn và tài liệu kỹ thuật đi cùng source; mật khẩu/API key thật và dữ liệu học vụ riêng không nằm trong gói chia sẻ.

ZIP source chỉ là một phần của bài nộp. Bài nộp cuối kỳ còn cần báo cáo Word, file báo cáo kết quả của nhóm và các tài liệu liên quan theo đề bài/Rubric. Đặt tên ZIP cuối cùng theo mẫu của giảng viên, không dùng tên ZIP source ví dụ ở trên để nộp trực tiếp.
