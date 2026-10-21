# Theo dõi tiến độ và checkpoint dự án

Tài liệu này là nguồn tổng hợp tiến độ chung của nhóm. Mỗi thành viên phải cập nhật
phần việc của mình khi tạo pull request hoặc khi trạng thái công việc thay đổi. Nhóm
trưởng dùng các cập nhật này để chốt checkpoint cuối mỗi sprint và làm dữ liệu cho
báo cáo môn học.

> Việc chưa được merge không có nghĩa là thành viên chưa làm. Bảng dưới đây chỉ ghi
> nhận những gì có thể đối chiếu trên Git tại thời điểm checkpoint.

## Quy ước trạng thái

| Trạng thái | Ý nghĩa |
| --- | --- |
| `Chưa bắt đầu` | Chưa có phần việc hoặc nhánh được ghi nhận |
| `Đang làm` | Đã bắt đầu nhưng chưa tạo pull request |
| `Chờ review` | Đã có pull request, chưa merge vào `master` |
| `Đã merge` | Đã merge vào `master`, chưa nghiệm thu đầy đủ |
| `Hoàn thành` | Đã merge, chạy kiểm thử/demo đạt và đáp ứng tiêu chí hoàn thành |
| `Bị chặn` | Không thể tiếp tục vì phụ thuộc hoặc lỗi cần người khác xử lý |

Không tự ghi phần trăm hoàn thành theo cảm tính. Một use case chỉ được đánh dấu
`Hoàn thành` khi đáp ứng đủ các điều kiện sau:

- Code đã được merge vào `master`.
- Luồng chính đã được chạy thử hoặc có test tương ứng.
- Không còn lỗi nghiêm trọng đã biết.
- Giao diện, validation và phân quyền liên quan đã hoạt động đúng.
- Có bằng chứng đối chiếu: số PR, commit, test hoặc ảnh demo.

## Cách cập nhật

1. Khi tạo PR, thành viên thêm một dòng vào **Nhật ký công việc** trong chính PR đó.
2. Khi PR được merge hoặc bị yêu cầu sửa, cập nhật lại trạng thái của dòng vừa thêm.
3. Cuối tuần, nhóm trưởng chốt bảng **Checkpoint sprint**, ghi rõ phần còn thiếu và
   mục tiêu của sprint tiếp theo.
4. Chỉ thêm mục mới; không xóa lịch sử cũ. Nếu kết quả bị revert, thêm ghi chú revert
   và chuyển trạng thái hiện tại về `Chờ review` hoặc `Đang làm`.

Mẫu một dòng cập nhật:

```text
| YYYY-MM-DD | Họ tên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
```

## Checkpoint sprint

### Sprint 1 — 19/10/2026 đến 25/10/2026

**Mốc đối chiếu ban đầu:** `master` tại commit `89404e6` ngày 20/10/2026.

**Mục tiêu sprint:** triển khai các use case đã phân công, tích hợp lần lượt lên
`master` và có thể demo được các luồng đã hoàn thành.

| Hạng mục | Phụ trách | Trạng thái hiện tại | Kết quả/bằng chứng | Còn thiếu hoặc bước tiếp theo |
| --- | --- | --- | --- | --- |
| Authentication và User | Phạm Tấn Đạt | Chưa có bản merge được ghi nhận | Chưa có PR merge trong ngày 19/10 | Cập nhật nhánh/PR và các use case đã làm |
| Product browsing | Trần Chí Quốc | Đã merge | PR #2, commit `af86b21`; có code danh sách, chi tiết, tìm kiếm, lọc, phân trang và test | Chạy nghiệm thu với MySQL local, sửa các lỗi phát hiện nếu có |
| Cart | Trần Chí Quốc | Chưa có bản merge được ghi nhận | Chưa thấy phần xử lý Cart được merge trong ngày 19/10 | Triển khai thêm/cập nhật/xóa sản phẩm trong giỏ Session |
| Checkout, Order và Payment | Phạm Nhật Hưng | Chưa có bản merge được ghi nhận | Chưa có PR merge trong ngày 19/10 | Cập nhật nhánh/PR và các use case đã làm |
| Catalog Admin | Nguyễn Trần Thái Sơn | Chờ review lại | PR #4 từng merge tại `42dc259`, sau đó bị revert bởi PR #5 ngày 20/10 | Sửa trên nhánh, review và merge lại; chỉ đánh dấu hoàn thành sau khi demo CRUD đạt |
| Giao diện chung và dữ liệu mẫu | Lương Việt Thái | Đã merge | PR #3, commit `272b002`; cập nhật layout, trang chủ, auth/cart và dữ liệu mẫu | Kiểm tra lại khi ghép với logic thật của từng module |
| Security, Email và File Storage | Lương Việt Thái | Đang làm | Đã có cấu hình và service contract nền trong project | Bổ sung implementation, tích hợp và kiểm thử theo use case |

**Vấn đề/rủi ro đang theo dõi:**

- Catalog Admin đã từng merge nhưng bị revert, vì vậy chưa được tính là hoàn thành.
- Các trang giao diện đã merge có thể vẫn đang dùng dữ liệu mẫu; cần kiểm tra lại sau
  khi nối service và database.
- Các phần chưa có PR merge cần thành viên tự bổ sung branch/PR để checkpoint phản ánh
  đúng tiến độ thực tế.

**Kết luận sprint:** Chưa chốt. Cập nhật vào cuối ngày 25/10/2026.

**Mục tiêu sprint tiếp theo:** Chưa chốt. Điền sau buổi checkpoint Sprint 1.

## Nhật ký công việc

### 19/10/2026

| Thành viên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
| --- | --- | --- | --- | --- | --- | --- |
| Trần Chí Quốc | Product browsing | Bổ sung API danh sách và chi tiết sản phẩm; tìm kiếm không dấu; lọc danh mục/giá; sắp xếp; phân trang; dữ liệu mẫu và test liên quan | Đã merge | PR #2, `af86b21` | Có test controller, service và integration trong source | Nghiệm thu với MySQL local; tiếp tục phần Cart |
| Lương Việt Thái | Giao diện chung và dữ liệu mẫu | Cập nhật layout, CSS, trang chủ, các trang auth/cart và tài nguyên giao diện | Đã merge | PR #3, `272b002` | Cần demo lại sau khi nối logic thật | Tiếp tục Security, Email, File Storage và hỗ trợ tích hợp |
| Nguyễn Trần Thái Sơn | Catalog Admin | Bổ sung controller/service CRUD danh mục, sản phẩm và giao diện CMS | Chờ review lại | PR #4, `42dc259`; bị revert bởi PR #5 ngày 20/10 | Chưa được nghiệm thu trên `master` hiện tại | Sửa lỗi/xung đột, tạo PR mới và demo CRUD |

### 21/10/2026

| Thành viên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
| --- | --- | --- | --- | --- | --- | --- |
| Lương Việt Thái | Giao diện chung, đăng nhập Customer và CMS | Sửa lỗi render trang chủ và border header; tách `/login` cho Customer, thêm `/cms/login` với layout riêng cho Staff/Admin, phân luồng Security theo vai trò và thêm tài khoản/nút đăng nhập demo | Đang làm | `master`; Chưa có | Maven compile/context test đạt; toàn bộ suite còn 4 test `ProductControllerTest` lỗi 404 không thuộc thay đổi này | Restart ứng dụng và demo `/`, `/login`, `/cms/login`; xử lý riêng 4 test Product Controller |

## Mẫu checkpoint sprint mới

Sao chép khối dưới đây và đặt phía trên sprint cũ:

```markdown
### Sprint N — DD/MM/YYYY đến DD/MM/YYYY

**Mốc đối chiếu:** `master` tại commit `<commit>` ngày DD/MM/YYYY.

**Mục tiêu sprint:**

| Hạng mục | Phụ trách | Trạng thái hiện tại | Kết quả/bằng chứng | Còn thiếu hoặc bước tiếp theo |
| --- | --- | --- | --- | --- |
| ... | ... | ... | ... | ... |

**Vấn đề/rủi ro đang theo dõi:**

- ...

**Kết luận sprint:** Đạt/Chưa đạt mục tiêu vì ...

**Mục tiêu sprint tiếp theo:** ...
```

## Quy tắc dùng cho báo cáo cuối kỳ

- Lấy số liệu từ các checkpoint đã chốt, không suy đoán lại theo trí nhớ.
- Khi ghi một chức năng hoàn thành, dẫn số PR/commit và bằng chứng test hoặc demo.
- Ghi cả thay đổi bị revert, lỗi lớn và cách xử lý; đây cũng là bằng chứng về quá
  trình phát triển và quản lý rủi ro của nhóm.
- Ảnh demo có thể lưu trong thư mục riêng sau này; ghi đường dẫn ảnh vào cột
  `Test hoặc demo`, không chèn ảnh dung lượng lớn trực tiếp vào file này.
