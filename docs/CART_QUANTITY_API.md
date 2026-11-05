# API cập nhật số lượng giỏ hàng

`PATCH /cart/items/{productId}` — Guest và Customer, giỏ lưu trong HTTP Session
với attribute `cart` (kiểu `Cart`). Gửi cookie cùng Session của giỏ.

Request `Content-Type: application/json`:

```json
{"quantity": 2}
```

Response HTTP 200 (ví dụ):

```json
{
  "items": [{
    "productId": 1,
    "productName": "Bánh kem",
    "imagePath": null,
    "unitPrice": 120000,
    "quantity": 2,
    "lineTotal": 240000,
    "availableStock": 5,
    "preparationDays": 1,
    "available": true,
    "availabilityMessage": null
  }],
  "totalQuantity": 2,
  "totalAmount": 240000,
  "maximumPreparationDays": 1,
  "checkoutAllowed": true
}
```

HTTP 400: thiếu quantity, quantity dưới 1 hoặc body không hợp lệ.
HTTP 404: chưa có giỏ Session, mặt hàng không thuộc giỏ hoặc sản phẩm đã bị xóa.
HTTP 409: vượt tồn kho hiện tại hoặc sản phẩm ngừng bán.
Lỗi từ controller trả JSON `{"message":"Nội dung lỗi"}`.
Request bị từ chối không sửa số lượng/giá trong giỏ.

Giữ CSRF đang bật: client lấy token và header name từ request attribute `_csrf`
khi render trang Thymeleaf; gửi token qua header đó (thường `X-CSRF-TOKEN`).
Thiếu/sai CSRF token bị Security từ chối HTTP 403 trước controller.

Mỗi lần cập nhật đọc lại giá, trạng thái và tồn kho của sản phẩm từ
`ProductQueryService`; tính lại thành tiền và tổng cả giỏ bằng `BigDecimal`.
Giỏ không giữ chỗ hoặc trừ kho; checkout vẫn cần kiểm tra tồn kho trong transaction.

## Lưu giỏ hàng Session

Các API JSON hiện có (không yêu cầu đăng nhập):

| Method | URL | Nội dung |
| --- | --- | --- |
| GET | `/cart/items` | Xem giỏ, giỏ mới có `items: []`, tổng tiền 0 |
| POST | `/cart/items` | Thêm bánh; body `{"productId":1,"quantity":2}`; nếu có sẵn thì cộng số lượng |
| PATCH | `/cart/items/1` | Đặt lại số lượng; body `{"quantity":3}` |
| DELETE | `/cart/items/1` | Xóa bánh khỏi giỏ |

Các thao tác đều trả `CartResponse`. Giữ cookie `JSESSIONID` qua các request;
Postman dùng cookie jar, trình duyệt dùng cùng origin. Thao tác ghi cần CSRF token.
Không tạo bảng giỏ hàng và không ghi giỏ xuống database. `CartServiceImpl` triển
khai contract CartService hiện có; `SessionCartServiceImpl` quản lý Session và
khóa các thao tác trong cùng Session để tránh mất cập nhật.

Reload/chuyển trang không xóa giỏ. GET đọc lại giá, ảnh, tên, kho và trạng thái
hiện tại để trả dữ liệu giỏ; không tự đổi số lượng khách đã chọn. Bánh ngừng bán,
hết hàng, vượt kho hoặc bị xóa vẫn hiện trong response với `available=false` và
`availabilityMessage`, để khách giảm số lượng hoặc xóa. `checkoutAllowed=false`
khi giỏ rỗng hoặc còn dòng không hợp lệ. GET vẫn trả HTTP 200 cho giỏ có dòng lỗi;
thao tác thêm/sửa không hợp lệ tiếp tục trả 409. `CartService.refresh` kiểm tra
lại toàn giỏ trước callback tạo đơn; checkout thực tế vẫn cần kiểm tra/trừ kho
trong transaction.

Trang `/cart` render giỏ Session thật bằng Thymeleaf. `cart.js` gọi PATCH/DELETE
kèm CSRF để tăng, giảm, nhập số lượng hoặc xóa dòng, rồi reload giỏ và tổng tiền.
Không hiển thị dữ liệu mẫu khi giỏ trống. Restart app có thể làm mất Session;
sau restart cần thêm bánh lại để demo.

Timeout hiện có là `server.servlet.session.timeout=${SESSION_TIMEOUT:30m}`.
Servlet container tự hủy Session khi không tương tác quá thời hạn; không cần bộ
đếm riêng. Session mới nhận giỏ trống. Kiểm thử mô phỏng invalidation; chưa chờ
timeout thực tế trên Tomcat.

Đăng nhập Customer hợp lệ dùng SecurityConfig hiện có: Spring Security đổi mã
Session chống session fixation và giữ attribute `cart`. Không có giỏ lưu riêng
theo tài khoản để gộp; cùng giỏ Guest tiếp tục được sử dụng sau đăng nhập.
Logout hủy Session theo cấu hình hiện có; không đồng bộ qua thiết bị khác.

## Điểm tích hợp đặt hàng

Checkout hiện chỉ có GET trang và service interface, chưa có implementation đặt
đơn. Phân hệ Order cần gọi helper sau khi triển khai checkout:

```java
var order = sessionCarts.checkout(session,
    cart -> checkoutService.checkout(accountId, cart, request));
```

`checkoutService` phải là Spring transactional proxy; return của nó chỉ được xem
là thành công khi đã commit. Nếu có transaction bao ngoài, helper đăng ký
`afterCommit` để chỉ xóa attribute `cart` sau commit. Nếu đặt hàng ném exception
hoặc rollback, helper giữ giỏ; không invalidate cả Session. Callback chỉ được trả
thành công khi đã tạo đơn thật, không trả null/kết quả lỗi thay vì ném exception.
Không có API cho client tự báo đặt hàng thành công hoặc tự xóa giỏ sau checkout.

Đây mới là điểm tích hợp đã kiểm thử bằng callback và transaction synchronization,
chưa nối luồng đặt đơn thực tế. Phân hệ Order vẫn phải kiểm tra giỏ rỗng, xác thực
Customer, kiểm tra/trừ kho và chống gửi đơn trùng trong transaction. Xóa Session
không thay thế cơ chế chống trùng của Order. Giao diện giỏ đã nối API;
luồng tạo đơn thực tế vẫn cần thành viên phụ trách Order triển khai.
