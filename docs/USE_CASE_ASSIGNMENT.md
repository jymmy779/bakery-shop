# Phân công phát triển theo use case

Tài liệu này xác định phạm vi code của từng thành viên dựa trên phân công trong
báo cáo. Khi cần thay đổi Entity, Enum, Security hoặc service interface dùng
chung, thành viên trao đổi với nhóm trưởng trước khi sửa.

## Nguyên tắc chung

- Mỗi người tạo một nhánh riêng từ nhánh chung mới nhất.
- Controller chỉ gọi service, không gọi repository trực tiếp.
- Business logic đặt trong class `service.impl`.
- Form đầu vào dùng DTO đã có và `@Valid`.
- Không nhận trực tiếp Entity từ form.
- Không tự đổi URL đã được khai báo trong controller skeleton.
- Không sửa `application*.properties`, `pom.xml`, Entity hoặc Enum chỉ để giải
  quyết tạm một use case.
- Không commit dữ liệu MySQL, mật khẩu, `.idea`, `target` hoặc `uploads`.

## Phạm Tấn Đạt — Authentication và User

Nhánh đề xuất: `feature/auth-user`

Use case:

- Đăng ký Customer.
- Đăng nhập và đăng xuất.
- Quên mật khẩu bằng OTP.
- Xem và cập nhật hồ sơ cá nhân.
- Admin xem, khóa và mở khóa tài khoản Customer.

Phạm vi code chính:

```text
com.bakery.account.controller.web
com.bakery.account.controller.admin.AdminAccountController
com.bakery.account.service.impl
com.bakery.account.repository
templates/auth
templates/account
templates/cms/account
```

Service cần triển khai:

```text
AccountService
PasswordResetService
```

Không sửa `CustomUserDetails`, `CustomUserDetailsService` hoặc
`SecurityConfig` nếu chưa trao đổi với nhóm trưởng.

## Trần Chí Quốc — Product browsing và Cart

Nhánh đề xuất: `feature/product-cart`

Use case:

- Xem danh sách và chi tiết sản phẩm đang bán.
- Tìm kiếm, lọc theo danh mục và phân trang.
- Thêm sản phẩm vào giỏ hàng Session.
- Cập nhật số lượng và xóa sản phẩm khỏi giỏ.
- Kiểm tra lại giá/tồn kho hiển thị trong giỏ.

Phạm vi code chính:

```text
com.bakery.catalog.controller.web.ProductController
com.bakery.cart.controller.web
com.bakery.cart.service.impl
com.bakery.cart.model
templates/product
templates/cart
```

Service cần triển khai:

```text
CartService
```

Phần đọc sản phẩm dùng `ProductQueryService`; phối hợp với người phụ trách Catalog,
không tạo thêm service truy vấn sản phẩm khác.

## Phạm Nhật Hưng — Checkout, Order và Payment

Nhánh đề xuất: `feature/order-payment`

Use case:

- Checkout từ giỏ hàng Session.
- Tạo Order, OrderDetail và Payment trong một transaction.
- Trừ tồn kho khi đặt hàng thành công.
- Xem lịch sử và chi tiết đơn hàng.
- Customer hủy đơn hợp lệ và hoàn tồn kho.
- Staff/Admin cập nhật trạng thái đơn.
- Staff/Admin xác nhận chuyển khoản.

Phạm vi code chính:

```text
com.bakery.order.controller.web
com.bakery.order.controller.admin
com.bakery.order.service.impl
com.bakery.order.repository
templates/order
templates/cms/order
```

Service cần triển khai:

```text
CheckoutService
OrderService
PaymentService
```

Các thao tác tạo đơn, trừ kho, hủy đơn và hoàn kho phải dùng `@Transactional`.

## Nguyễn Trần Thái Sơn — Catalog Admin

Nhánh đề xuất: `feature/catalog-admin`

Use case:

- CRUD danh mục.
- Thêm và cập nhật sản phẩm.
- Upload từ 1 đến 4 ảnh sản phẩm.
- Xóa sản phẩm chưa phát sinh đơn hàng.
- Chuyển sản phẩm đã có đơn hàng sang `STOPPED`.
- Quản lý tồn kho và số ngày chuẩn bị.

Phạm vi code chính:

```text
com.bakery.catalog.controller.admin
com.bakery.catalog.service.impl
com.bakery.catalog.repository
templates/cms/product
templates/cms/category
```

Service cần triển khai:

```text
ProductAdminService
CategoryAdminService
```

Upload ảnh gọi `FileStorageService`; không tự ghi file trực tiếp trong
controller.

## Lương Việt Thái — Nền tảng, Security và tích hợp

Nhánh đề xuất: `feature/platform-integration`

Use case và trách nhiệm:

- Duy trì cấu hình Spring Security và phân quyền.
- Điều hướng Customer/Staff/Admin sau đăng nhập.
- Quản lý tài khoản Staff dành cho Admin.
- Triển khai gửi email đăng ký, OTP và xác nhận đơn hàng.
- Triển khai lưu trữ/xóa file upload.
- Duy trì layout chung, cấu hình và xử lý exception toàn cục.
- Tích hợp các nhánh và xử lý thay đổi ở file dùng chung.

Phạm vi code chính:

```text
com.bakery.config
com.bakery.account.security
com.bakery.notification
com.bakery.storage
com.bakery.common
templates/fragments
templates/cms/staff
```

Service cần triển khai:

```text
EmailService
FileStorageService
```

Phần quản lý Staff sử dụng `AccountService` hiện có, không tạo entity Staff
riêng.

## File nền do nhóm trưởng tích hợp

Các file sau không nên được sửa đồng thời trên nhiều nhánh:

```text
pom.xml
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/java/com/bakery/config/SecurityConfig.java
src/main/java/com/bakery/**/entity/**
src/main/java/com/bakery/**/service/*.java
src/main/resources/templates/fragments/**
```

Nếu service contract thiếu method, thành viên đề xuất chữ ký cho nhóm trưởng bổ
sung trước, sau đó mới triển khai để tránh mỗi nhánh định nghĩa một kiểu.

## Thứ tự tích hợp đề xuất

1. Catalog Admin và Product browsing.
2. Authentication và User.
3. Cart.
4. Checkout, Order và Payment.
5. Email, upload, Security và giao diện dùng chung.
6. Chạy lại toàn bộ use case trên database local sạch.
