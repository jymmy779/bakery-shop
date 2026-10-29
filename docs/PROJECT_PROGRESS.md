# Theo dõi tiến độ và checkpoint dự án

Tài liệu này là nguồn tổng hợp tiến độ chung của nhóm. Mỗi thành viên phải cập nhật
phần việc của mình khi tạo pull request hoặc khi trạng thái công việc thay đổi. Nhóm
trưởng dùng các cập nhật này để chốt checkpoint cuối mỗi sprint và làm dữ liệu cho
báo cáo môn học.

> Việc chưa được merge không có nghĩa là thành viên chưa làm. Bảng dưới đây chỉ ghi
> nhận những gì có thể đối chiếu trên Git tại thời điểm checkpoint.

## Quy ước trạng thái

| Trạng thái     | Ý nghĩa                                                         |
| -------------- | --------------------------------------------------------------- |
| `Chưa bắt đầu` | Chưa có phần việc hoặc nhánh được ghi nhận                      |
| `Đang làm`     | Đã bắt đầu nhưng chưa tạo pull request                          |
| `Chờ review`   | Đã có pull request, chưa merge vào `master`                     |
| `Đã merge`     | Đã merge vào `master`, chưa nghiệm thu đầy đủ                   |
| `Hoàn thành`   | Đã merge, chạy kiểm thử/demo đạt và đáp ứng tiêu chí hoàn thành |
| `Bị chặn`      | Không thể tiếp tục vì phụ thuộc hoặc lỗi cần người khác xử lý   |

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

| Hạng mục                        | Phụ trách            | Trạng thái hiện tại             | Kết quả/bằng chứng                                                                      | Còn thiếu hoặc bước tiếp theo                                                                |
| ------------------------------- | -------------------- | ------------------------------- | --------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Authentication và User          | Phạm Tấn Đạt         | Chưa có bản merge được ghi nhận | Chưa có PR merge trong ngày 19/10                                                       | Cập nhật nhánh/PR và các use case đã làm                                                     |
| Product browsing                | Trần Chí Quốc        | Đã merge                        | PR #2, commit `af86b21`; có code danh sách, chi tiết, tìm kiếm, lọc, phân trang và test | Chạy nghiệm thu với MySQL local, sửa các lỗi phát hiện nếu có                                |
| Cart                            | Trần Chí Quốc        | Chưa có bản merge được ghi nhận | Chưa thấy phần xử lý Cart được merge trong ngày 19/10                                   | Triển khai thêm/cập nhật/xóa sản phẩm trong giỏ Session                                      |
| Checkout, Order và Payment      | Phạm Nhật Hưng       | Chưa có bản merge được ghi nhận | Chưa có PR merge trong ngày 19/10                                                       | Cập nhật nhánh/PR và các use case đã làm                                                     |
| Catalog Admin                   | Nguyễn Trần Thái Sơn | Đã merge                        | PR #12, commit `87b1373`, merge commit `2f9378b`; theo bàn giao có 39 test đạt          | Nghiệm thu runtime các luồng CRUD, trạng thái và quản lý ảnh trên CMS; hoàn thiện UI còn lại |
| Giao diện chung và dữ liệu mẫu  | Lương Việt Thái      | Đã merge                        | PR #3, commit `272b002`; cập nhật layout, trang chủ, auth/cart và dữ liệu mẫu           | Kiểm tra lại khi ghép với logic thật của từng module                                         |
| Security, Email và File Storage | Lương Việt Thái      | Đang làm                        | Đã có cấu hình và service contract nền trong project                                    | Bổ sung implementation, tích hợp và kiểm thử theo use case                                   |

**Vấn đề/rủi ro đang theo dõi:**

- Bản Catalog Admin tại PR #4 từng bị revert; phiên bản thay thế đã được hoàn thiện và merge bằng PR #12. Hiện vẫn cần nghiệm thu runtime trước khi chuyển sang `Hoàn thành`.
- Các trang giao diện đã merge có thể vẫn đang dùng dữ liệu mẫu; cần kiểm tra lại sau
  khi nối service và database.
- Các phần chưa có PR merge cần thành viên tự bổ sung branch/PR để checkpoint phản ánh
  đúng tiến độ thực tế.

**Kết luận sprint:** Chưa chốt. Cập nhật vào cuối ngày 25/10/2026.

**Mục tiêu sprint tiếp theo:** Chưa chốt. Điền sau buổi checkpoint Sprint 1.

## Nhật ký công việc

### 29/10/2026

| Thành viên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
| --- | --- | --- | --- | --- | --- | --- |
| Nguyễn Trần Thái Sơn | Catalog Admin — hoàn thiện giao diện CMS và dữ liệu mẫu | Sidebar CMS cho cả Admin và Staff (Staff không thấy mục Nhân viên/Tài khoản theo `SecurityConfig`); nút trạng thái Đang bán/Ngừng bán và Hoạt động/Ngừng hoạt động dạng viên thuốc xanh/đỏ; thêm `docs/seed-catalog-products.sql` (6 danh mục, 19 sản phẩm, 23 ảnh, chạy lặp lại an toàn) | Chờ review | Branch `feature/admin-son`; PR: xem PR của branch này; commit: Chưa có | `mvnw.cmd clean test` sau khi merge master: 80/81 đạt (lỗi còn lại là `SessionCartIntegrationTest` của Quốc: test còn kỳ vọng `/` trong khi master redirect `/profile`); chạy `seed-catalog-products.sql` hai lần trên DB trống: không trùng dữ liệu, cả 23 URL `/uploads/...` trả 200 khi có đủ file trong `uploads`; chưa xem giao diện trên trình duyệt thật | Giải nén đúng thư mục `uploads` trước khi dùng file SQL; ảnh Eclair Caramel Muối chưa có nên sản phẩm này chưa được thêm; đối chiếu thành phần/dị ứng với công thức thật |
| Nguyễn Trần Thái Sơn | Catalog Admin — tiếp thu góp ý giao diện PR #20 | Tách danh sách sản phẩm khỏi trang thêm/sửa; thêm trang form riêng với xem trước và bỏ ảnh mới, xóa ảnh đã lưu, fallback ảnh lỗi và bảng responsive; thêm alias GET `/cms/products/add`; sửa đăng xuất CMS thành POST kèm CSRF; đối chiếu các link sidebar với route/view chức năng hiện có | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | `mvnw.cmd '-Dtest=ProductAdminControllerTest,ProductAdminServiceImplTest,AccountSecurityIntegrationTest' test`: 28/28 đạt; `git diff --check` đạt | Chờ review và demo UI trên trình duyệt; một số trang ngoài Catalog hiện vẫn là placeholder, cần thành viên phụ trách hoàn thiện |
| Nguyễn Trần Thái Sơn | CMS — sửa nút Sửa và bổ sung tổng quan | Sửa binding form gây lỗi 500 khi mở trang Sửa sản phẩm; thêm dashboard dữ liệu thật gồm tổng/đang bán/ngừng bán, danh mục, tài khoản và doanh thu đơn giao thành công đã thanh toán; bổ sung kiểm tra render và đích các nút CMS | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | `mvnw.cmd '-Dtest=ProductAdminControllerTest,ProductAdminPageIntegrationTest,CmsDashboardServiceImplTest,CmsDashboardPageIntegrationTest,AccountSecurityIntegrationTest' test`: 21/21 đạt; `git diff --check` đạt | Cần reload/restart ứng dụng để xem giao diện mới; dashboard đếm toàn bộ tài khoản và doanh thu thanh toán đơn đã giao |
| Nguyễn Trần Thái Sơn | CMS — thêm tra cứu danh sách sản phẩm và danh mục | Đổi gợi ý ô lọc sản phẩm thành “Tra cứu tên sản phẩm...”, thêm biểu tượng kính lúp; thêm tra cứu tên danh mục tức thời, không phân biệt dấu, và thông báo khi không có kết quả | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | `mvnw.cmd '-Dtest=ProductAdminPageIntegrationTest,ProductAdminControllerTest,CategoryAdminControllerTest,ProductQueryServiceTest,ProductControllerTest' test`: 26/26 đạt; `git diff --check` đạt | Chờ review; chưa demo trực tiếp trên trình duyệt |
| Nguyễn Trần Thái Sơn | CMS — đồng nhất thông tin tài khoản trên sidebar | Mở rộng model advice cung cấp tên hiển thị tới các trang admin của account, catalog, dashboard và order; sidebar dùng cùng tên/ảnh chữ cái đầu trên các mục CMS thay vì lúc hiện họ tên, lúc hiện email | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | `mvnw.cmd '-Dtest=ProductAdminPageIntegrationTest,CmsDashboardPageIntegrationTest,CmsSidebarAdviceTest' test`: 8/8 đạt, kiểm tra cùng tên hiển thị trên trang sản phẩm, danh mục và dashboard | Chờ review; restart ứng dụng nếu đang chạy phiên bản cũ |
| Nguyễn Trần Thái Sơn | CMS — khôi phục trạng thái và tiến độ lưu sản phẩm | Thêm thông báo đang tải ảnh, tiến độ truyền file thực tế, trạng thái đang lưu/thành công/lỗi khi thêm hoặc cập nhật; khi validation lỗi, hiển thị lại form và lỗi server | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | `mvnw.cmd '-Dtest=ProductAdminPageIntegrationTest,ProductAdminControllerTest,ProductAdminServiceImplTest' test`: 25/25 đạt; `git diff --check` đạt | Chờ review; chưa demo trực tiếp trên trình duyệt |
| Nguyễn Trần Thái Sơn | CMS — sửa đăng xuất, thông báo CRUD và xóa ảnh | Đăng xuất CMS quay về `/cms/login?logout`; tách form xóa ảnh khỏi form lưu sản phẩm (form lồng nhau); mọi thao tác đổi trạng thái/xóa/CRUD danh mục hiện thông báo giữa màn hình tối thiểu 1 giây, thông báo lỗi màu đỏ | Chờ review | Branch `feature/admin-son`; PR #20; commit: Chưa có | Demo trình duyệt: đổi trạng thái sản phẩm hiện overlay "Đang xử lý..." rồi thông báo thành công; chạy `mvnw.cmd clean test` (chỉ `SessionCartIntegrationTest` của Quốc lỗi sẵn có) | Chờ review; chưa demo hết CRUD danh mục và lưu có ảnh |

### 26/10/2026
| Thành viên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
| ---------- | ------------------ | -------------- | ---------- | ---------------- | -------------- | ------------------------ |
| Lương Việt Thái | Tích hợp điều hướng sau đăng nhập | Cập nhật success handler để Customer ưu tiên quay lại saved request nội bộ ngoài CMS và mặc định về `/profile`; loại tham số kỹ thuật `continue` nhưng giữ query thật; Admin/Staff luôn về dashboard `/cms`; bổ sung integration test cho redirect Customer, dashboard Admin và chặn redirect chéo khu vực | Chờ review | Branch `review-pr-19`; PR #19 | `AccountSecurityIntegrationTest` đạt 8/8 bằng IntelliJ/JDK 17; `git diff --check` đạt | Chờ review và nghiệm thu redirect runtime trước khi merge |

### 25/10/2026

| Thành viên | Use case/phần việc | Kết quả cụ thể | Trạng thái | Branch/PR/commit | Test hoặc demo | Việc tiếp theo/vướng mắc |
| ---------- | ------------------ | -------------- | ---------- | ---------------- | -------------- | ------------------------ |
| Lương Việt Thái | Thiết kế giao diện storefront | Hoàn thiện prototype trang chủ, danh sách, chi tiết, cart, đăng nhập và đăng ký khách hàng; các trang auth dùng header/footer storefront chung, form đăng ký bind đúng `RegisterRequest`, giữ style input nguyên bản và tăng độ rõ của border, có validation UI, hiện/ẩn và đối chiếu mật khẩu; khởi tạo sớm deferred CSRF token cho template dài; dữ liệu cart hiện vẫn là mẫu giao diện | Đang làm | `Thai`; Chưa có | Đã kiểm tra cấu trúc template, field binding, JavaScript và `git diff`; Maven Wrapper không khởi chạy được trong môi trường hiện tại nên chưa test runtime | Bổ sung POST `/register` khi triển khai use case tài khoản, chạy demo trực tiếp các trang auth và tích hợp dữ liệu thật cho cart |
| Phạm Tấn Đạt | Đăng ký Customer — backend endpoint | Tạo `AccountServiceImpl`, xử lý chuẩn hóa email, kiểm tra email trùng, đối chiếu mật khẩu, BCrypt password, gán `ROLE_CUSTOMER`/`ACTIVE`; thêm POST `/register` và query repository lọc theo role để chuẩn bị cho danh sách Customer | Chờ review | PR #19 | `mvnw.cmd -q -DskipTests compile` đạt; `AccountServiceImplTest, AuthControllerTest` đạt; đăng ký và đăng nhập Customer đã demo bằng Postman ngày 26/10/2026 | Chờ review và chạy lại test sau thay đổi tích hợp Security |
| Phạm Tấn Đạt | Xem và cập nhật profile Customer — backend endpoint | Nối GET/POST `/profile`; lấy account ID từ `CustomUserDetails`, chỉ cập nhật họ tên, số điện thoại và địa chỉ; thêm test controller cho tải profile, cập nhật hợp lệ và validation lỗi | Chờ review | PR #19 | `mvnw.cmd -q -Dtest=AccountServiceImplTest,AuthControllerTest,ProfileControllerTest test` đạt; cập nhật và đọc lại dữ liệu profile đã demo bằng Postman ngày 26/10/2026 | Chờ review và tích hợp giao diện profile sau khi endpoint ổn định |
| Phạm Tấn Đạt | Admin xem và khóa/mở Customer — backend endpoint | Nối GET `/cms/accounts` với phân trang và lọc `ROLE_CUSTOMER`; nối POST `/cms/accounts/{id}/status`; chỉ cho phép đổi status Customer, hỗ trợ `ACTIVE`/`LOCKED`, thêm test service/controller và integration test với SecurityConfig thật | Chờ review | PR #19 | Unit/controller test: 15/15 đạt; `AccountSecurityIntegrationTest`: 5/5 đạt với MySQL local; endpoint danh sách và khóa/mở Customer đã demo bằng Postman ngày 26/10/2026 | Chờ review; hoàn thiện UI CMS sau khi endpoint ổn định |
| Lương Việt Thái | Thiết kế giao diện storefront và dữ liệu chi tiết sản phẩm | Hoàn thiện prototype các trang storefront và layout fragment chung; bổ sung `ProductBadge` cùng các trường thành phần, bảo quản và dị ứng; đồng bộ DTO, mapping, API, CMS và accordion chi tiết; chuẩn hóa SQL mẫu thành sáu danh mục khớp UI; bổ sung public API `GET /api/categories`, sửa link phân trang sản phẩm dùng đúng `/api/products` và cập nhật tài liệu API; Customer đăng nhập thành công được chuyển tới `/profile` và icon tài khoản phân luồng theo vai trò | Đang làm | `Thai`; Chưa có | Đã kiểm tra cấu trúc template, route, field binding và `git diff`; đã bổ sung test mapping/controller nhưng chưa chạy được: Maven Wrapper lỗi khởi động, Maven hệ thống bị từ chối đọc file bảo mật của JDK | Chạy migration/dữ liệu mẫu và test Catalog bằng JDK 17 trong IntelliJ; tiếp tục nối profile, tìm kiếm và cart với dữ liệu thật |
| Nguyễn Trần Thái Sơn | Catalog Admin — nâng cấp giao diện CMS | Thêm sidebar CMS (khối tên tài khoản ở đầu, đăng xuất; Admin thấy đủ menu Tổng quan/Sản phẩm/Danh mục/Đơn hàng/Nhân viên/Tài khoản, Staff chỉ thấy Tổng quan/Sản phẩm/Danh mục/Đơn hàng theo phân quyền SecurityConfig) cho trang Sản phẩm và Danh mục; header ngang cũ không còn dùng ở hai trang này; modal xem trước ảnh sản phẩm (mũi tên, số thứ tự, Esc, phím ←/→); đổi nhãn badge `LIMITED` thành "Giới hạn"; ảnh chọn để upload hiện thumbnail vuông 1:1 (bấm để phóng to) thay cho tên file; nút trạng thái Đang bán/Ngừng bán (sản phẩm) và Hoạt động/Ngừng hoạt động (danh mục) dạng viên thuốc: xanh nhạt chữ xanh đậm / đỏ nhạt chữ đỏ; sửa lỗi trùng `display_order` khi thêm ảnh sau khi xóa ảnh ở giữa; cập nhật sản phẩm chạy nền với tag "Đang cập nhật..." tối thiểu 1 giây và giữ nguyên form sửa cùng trang danh sách hiện tại | Chờ review | Branch `feature/admin-son`; PR/commit: Chưa có | `mvnw.cmd clean test`: 56/57 đạt (lỗi còn lại là `SessionCartIntegrationTest` của Quốc); đã render thử trang Sản phẩm/Danh mục với Admin và Staff bằng test tạm (đã xóa); chưa xem trên trình duyệt thật | Kiểm tra giao diện thực tế desktop/mobile; áp dụng sidebar cho các trang CMS khác nếu nhóm đồng ý |
| Nguyễn Trần Thái Sơn | Catalog Admin — đồng bộ CMS với badge và thông tin chi tiết sản phẩm | Sau khi merge master, đối chiếu DTO/service/form với `ProductBadge` và 3 trường chi tiết (khớp); hiển thị badge ở bảng sản phẩm CMS; sản phẩm thuộc danh mục ngừng hoạt động vẫn chọn đúng danh mục khi sửa | Chờ review | Branch `feature/admin-son`; PR/commit: Chưa có | `mvnw.cmd clean test`: 54/55 đạt; 1 lỗi `SessionCartIntegrationTest` (không thuộc phạm vi Sơn). Chưa demo trên trình duyệt | Báo Quốc cập nhật test redirect `/profile`; sidebar CMS dành cho Admin chờ xác nhận kế hoạch |

### 24/10/2026

| Thành viên    | Use case/phần việc                    | Kết quả cụ thể                                                                                                                                                                                                                                                                 | Trạng thái | Branch/PR/commit                    | Test hoặc demo                                                                                                                                                                                                                                       | Việc tiếp theo/vướng mắc                                                                                                                                            |
| ------------- | ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Trần Chí Quốc | Lưu giỏ hàng bằng Session — API JSON  | Triển khai CartServiceImpl và SessionCartServiceImpl; GET/POST/PATCH/DELETE `/cart/items`; giỏ độc lập theo Session, cộng số lượng có kiểm tra kho; giữ giỏ khi đăng nhập Customer qua Security hiện có; helper checkout xóa giỏ sau thành công/commit và giữ khi lỗi/rollback | Đang làm   | Branch `master`; PR/commit: Chưa có | Maven JDK 17: `-Dtest=CartQuantityServiceImplTest,CartQuantityControllerTest,SessionCartIntegrationTest test` đạt 15 test; compile đạt; kiểm thử MockMvc với SecurityConfig thật, mock Catalog và tài khoản test; timeout mô phỏng bằng invalidation | Checkout chưa có implementation nên helper chưa được nối vào đặt đơn thật; chưa demo MySQL/Tomcat timeout thực tế hoặc nối UI; chi tiết `docs/CART_QUANTITY_API.md` |
| Trần Chí Quốc | Cập nhật số lượng giỏ hàng — API JSON | Thêm PATCH `/cart/items/{productId}` dùng Session `cart`; kiểm tra số lượng tối thiểu 1, tồn kho/trạng thái từ ProductQueryService; cập nhật giá và tính thành tiền/tổng giỏ; trả lỗi JSON 400/404/409; tài liệu `docs/CART_QUANTITY_API.md`                                   | Đang làm   | Branch `master`; PR/commit: Chưa có | Maven với JDK 17: `-Dtest=CartQuantityServiceImplTest,CartQuantityControllerTest test` đạt 8 test; compile đạt trong vòng test; chưa demo MySQL hoặc kiểm thử Security tích hợp                                                                      | Cần luồng thêm sản phẩm vào Session để demo; tích hợp CartService và UI cập nhật tức thì; gửi CSRF token khi gọi API                                                |

### 22/10/2026

| Thành viên      | Use case/phần việc                             | Kết quả cụ thể                                                                                                                                                                                                                                                             | Trạng thái | Branch/PR/commit                                 | Test hoặc demo                                                                                                                                                                                   | Việc tiếp theo/vướng mắc                                                                                                                                                                                                                        |
| --------------- | ---------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Lương Việt Thái | Chuẩn hóa service Catalog                      | Tách `ProductQueryService`/`ProductQueryServiceImpl` cho storefront; đổi service CMS thành `ProductAdminService`/`ProductAdminServiceImpl` và `CategoryAdminService`/`CategoryAdminServiceImpl`; đồng bộ controller, test và tài liệu, không đổi nghiệp vụ                 | Đã merge   | PR #14, commit `b93274f`, merge commit `8595e2b` | Chưa chạy lại test do môi trường terminal không khởi động được Maven Wrapper và không có Maven cài sẵn                                                                                           | Chạy toàn bộ test bằng IntelliJ; tiếp tục hoàn thiện UI                                                                                                                                                                                         |
| Lương Việt Thái | UI storefront sản phẩm và quyền truy cập Guest | Public các trang storefront; nối danh sách responsive với API Catalog; tách `product-list.js`; làm đầy trang chủ bằng khu vực dịp sử dụng và combo mẫu; tách riêng giao diện `/about`, `/contact`; chuẩn hóa nhãn sản phẩm và mở rộng SQL lên 60 sản phẩm thuộc 6 danh mục | Đang làm   | Branch `Thai`; PR/commit: Chưa có                | `git diff --check` và kiểm tra cú pháp `product-list.js` đạt; SQL chưa chạy trên MySQL; Maven test chưa chạy được do wrapper lỗi khởi động và Maven hệ thống bị từ chối đọc file bảo mật của JDK | Chạy SQL mẫu, demo runtime các trang public và nối chi tiết/giỏ hàng/liên hệ với logic thật. OPEN QUESTION/backlog: sản phẩm nổi bật và đánh giá sản phẩm cần nhóm duyệt, cập nhật requirement, phân công, class/database diagram trước khi làm |

### 21/10/2026

| Thành viên           | Use case/phần việc                         | Kết quả cụ thể                                                                                                                                                                                                           | Trạng thái | Branch/PR/commit                                      | Test hoặc demo                                                                                                             | Việc tiếp theo/vướng mắc                                            |
| -------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| Nguyễn Trần Thái Sơn | Catalog Admin                              | Chuyển trang sản phẩm/danh mục sang bảng CMS; đặt form thêm/chỉnh sửa trong sidebar; dọn route và method dư thừa, cập nhật test theo service contract                                                                    | Đã merge   | PR #12, commit cuối `87b1373`, merge commit `2f9378b` | Giai đoạn này ghi nhận `mvnw clean test`: 29 test đạt, gồm integration MySQL local                                         | Được hoàn thiện tiếp trong commit cuối của PR #12                   |
| Nguyễn Trần Thái Sơn | Catalog Admin                              | Hoàn thiện xóa hoặc ngừng bán theo lịch sử đơn hàng; toggle trạng thái sản phẩm/danh mục; quản lý từ 1 đến 4 ảnh; carousel, xóa từng ảnh và chặn xóa ảnh cuối; chuẩn hóa nhãn tiếng Việt và giới hạn CMS cho Staff/Admin | Đã merge   | PR #12, commit `87b1373`, merge commit `2f9378b`      | Theo bàn giao PR: `mvnw.cmd clean test`, 39 test đạt, 0 failure, 0 error; người tích hợp chưa chạy lại do môi trường Maven | Nghiệm thu runtime các luồng CMS và xử lý các lỗi UI còn lại nếu có |
| Lương Việt Thái      | Giao diện chung, đăng nhập Customer và CMS | Sửa lỗi render trang chủ và border header; tách `/login` cho Customer, thêm `/cms/login` với layout riêng cho Staff/Admin, phân luồng Security theo vai trò và thêm tài khoản/nút đăng nhập demo                         | Đã merge   | PR #13, commit `eef59ab`, merge commit `81f36b1`      | Maven compile/context test đạt tại thời điểm triển khai; toàn bộ suite khi đó còn 4 test `ProductControllerTest` lỗi 404   | Nghiệm thu lại đăng nhập Customer/CMS sau khi tích hợp các module   |

### 19/10/2026

| Thành viên           | Use case/phần việc             | Kết quả cụ thể                                                                                                                       | Trạng thái     | Branch/PR/commit                                 | Test hoặc demo                                          | Việc tiếp theo/vướng mắc                                                             |
| -------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ | -------------- | ------------------------------------------------ | ------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Trần Chí Quốc        | Product browsing               | Bổ sung API danh sách và chi tiết sản phẩm; tìm kiếm không dấu; lọc danh mục/giá; sắp xếp; phân trang; dữ liệu mẫu và test liên quan | Đã merge       | PR #2, `af86b21`                                 | Có test controller, service và integration trong source | Nghiệm thu với MySQL local; tiếp tục phần Cart                                       |
| Lương Việt Thái      | Giao diện chung và dữ liệu mẫu | Cập nhật layout, CSS, trang chủ, các trang auth/cart và tài nguyên giao diện                                                         | Đã merge       | PR #3, `272b002`                                 | Cần demo lại sau khi nối logic thật                     | Tiếp tục Security, Email, File Storage và hỗ trợ tích hợp                            |
| Nguyễn Trần Thái Sơn | Catalog Admin                  | Bổ sung controller/service CRUD danh mục, sản phẩm và giao diện CMS                                                                  | Chờ review lại | PR #4, `42dc259`; bị revert bởi PR #5 ngày 20/10 | Chưa được nghiệm thu trên `master` tại thời điểm đó     | Lịch sử được giữ lại; phần Catalog Admin sau đó được hoàn thiện và merge bằng PR #12 |

## Mẫu checkpoint sprint mới

Sao chép khối dưới đây và đặt phía trên sprint cũ:

```markdown
### Sprint N — DD/MM/YYYY đến DD/MM/YYYY

**Mốc đối chiếu:** `master` tại commit `<commit>` ngày DD/MM/YYYY.

**Mục tiêu sprint:**

| Hạng mục | Phụ trách | Trạng thái hiện tại | Kết quả/bằng chứng | Còn thiếu hoặc bước tiếp theo |
| -------- | --------- | ------------------- | ------------------ | ----------------------------- |
| ...      | ...       | ...                 | ...                | ...                           |

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
