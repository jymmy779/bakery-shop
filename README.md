# Bakery Shop

Website giới thiệu và bán bánh ngọt xây dựng bằng Spring Boot, Spring MVC,
Thymeleaf, Spring Data JPA, Spring Security và MySQL.

Quy tắc phân hệ và vị trí đặt code được mô tả tại
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Thành viên cần đọc tài liệu này
trước khi bắt đầu use case.

Phạm vi use case, package và file của từng thành viên được mô tả tại
[`docs/USE_CASE_ASSIGNMENT.md`](docs/USE_CASE_ASSIGNMENT.md).

Các quyết định đã chốt về badge, nội dung chi tiết sản phẩm, quà sinh nhật và
khách hàng thân thiết được ghi tại
[`docs/PRODUCT_DATA_REQUIREMENTS.md`](docs/PRODUCT_DATA_REQUIREMENTS.md).

## Yêu cầu môi trường

- JDK 17
- MySQL 8.x
- Git
- IntelliJ IDEA

Dự án được cấu hình để các thành viên import và chạy trực tiếp bằng IntelliJ IDEA.
Không sử dụng VS Code cho project này.

## Chạy dự án lần đầu

1. Clone repository, chọn `File > Open` và mở thư mục gốc của dự án.
2. Chờ IntelliJ nhận diện và tải xong Maven dependencies từ `pom.xml`.
3. Vào `File > Project Structure > Project`, chọn Project SDK là JDK 17.
4. Vào `Settings > Build, Execution, Deployment > Build Tools > Maven`:
   - `Maven home path`: chọn `Bundled (Maven 3)`.
   - `JDK for importer`: chọn JDK 17.
5. Bật annotation processing cho Lombok:
   `Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable`.
6. Tạo database local:

   ```sql
   CREATE DATABASE IF NOT EXISTS bakery_db
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_unicode_ci;
   ```

7. Mở `src/main/java/com/bakery/BakeryShopApplication.java`, bấm biểu tượng Run
   bên cạnh hàm `main`, sau đó chọn `Modify Run Configuration`.
8. Nếu chưa thấy ô `Environment variables`, bấm `Modify options` và chọn
   `Environment variables`. Nhập tài khoản MySQL của máy đang sử dụng, ví dụ:

   ```text
   DB_USERNAME=root;DB_PASSWORD=<mat-khau-mysql>
   ```

   Nếu MySQL không có mật khẩu, để `DB_PASSWORD` trống. Các biến email chưa bắt
   buộc cho đến khi thực hiện use case gửi mail. Xem danh sách cấu hình tham khảo
   tại `.env.example`; Spring Boot không tự đọc file này. Không commit mật khẩu
   hoặc App Password.

9. Bấm Run tại `BakeryShopApplication`.
10. Truy cập `http://localhost:8080`.

## Tài khoản Admin local

Ở lần chạy đầu tiên, profile `dev` tự tạo một tài khoản Admin nếu email chưa tồn
tại:

```text
Email: admin@bakery.local
Password: admin123
```

Có thể đổi thông tin này trong IntelliJ Run Configuration bằng các biến
`ADMIN_EMAIL`, `ADMIN_PASSWORD` và `ADMIN_NAME`. Dữ liệu bootstrap chỉ tạo tài
khoản chưa tồn tại, không ghi đè tài khoản đã có.

Customer, Staff và Admin đăng nhập tại cùng trang `/login`. Sau khi xác thực,
Customer được chuyển về storefront/profile; Staff và Admin được chuyển vào CMS
theo quyền của tài khoản.

Thư mục `uploads/` cũng được tạo tự động tại thư mục chạy project. File trong
thư mục này được phục vụ qua URL `/uploads/<ten-file>` và không được commit.

## Cấu hình chuyển khoản cho Customer

Trang đặt hàng thành công và chi tiết đơn hàng dùng các biến môi trường sau
trong IntelliJ Run Configuration:

- `PAYMENT_BANK_ID`: BIN ngân hàng hoặc mã ngân hàng được VietQR hỗ trợ.
- `PAYMENT_BANK_NAME`: tên ngân hàng hiển thị cho khách.
- `PAYMENT_ACCOUNT_NUMBER`: số tài khoản nhận tiền, giữ nguyên số 0 ở đầu nếu có.
- `PAYMENT_ACCOUNT_NAME`: tên chủ tài khoản nhận tiền.
- `PAYMENT_QR_TEMPLATE`: mẫu QR, mặc định `qr_only`.

Chưa có tài khoản ngân hàng thì để trống. Ứng dụng vẫn cho đặt đơn, nhưng sẽ
hiển thị thông báo đang cập nhật thông tin chuyển khoản và không tạo QR.
Sau khi thêm hoặc đổi cấu hình, khởi động lại ứng dụng. `.env.example` chỉ là
tham khảo, Spring Boot không tự đọc file `.env`.

QR sử dụng [VietQR Quick Link dạng SVG](https://www.vietqr.io/qr-dong).
Trình duyệt gửi ngân hàng, số tài khoản nhận, tên chủ tài khoản, số tiền và mã
đơn tới VietQR để tải ảnh; không gửi tên, địa chỉ hay số điện thoại của Customer.
Nếu ảnh không tải được, khách vẫn có thể xem/sao chép thông tin để chuyển khoản.
Số tiền và nội dung lấy từ đơn đã lưu; chỉ hiển thị hướng dẫn khi đơn còn
`PENDING`, Payment còn `PENDING` và tổng tiền khớp, là số VND nguyên dương.
Xem QR hoặc sao chép không cập nhật trạng thái thanh toán. Staff/Admin đăng nhập
CMS tại `/cms/login`, vào **Quản lý đơn hàng** (`/cms/orders`), tìm mã đơn và mở
chi tiết. Sau khi đối chiếu giao dịch thực nhận đúng số tiền/nội dung, bấm
**Xác nhận đã nhận đủ tiền**; có thể nhập mã tham chiếu ngân hàng để tra cứu.

Hệ thống chỉ cho xác nhận Order `PENDING` và Payment chuyển khoản `PENDING`,
đồng thời lưu Payment `PAID`, Order `CONFIRMED`, người/thời điểm xác nhận và lịch
sử trong cùng transaction. Gửi trùng hoặc xác nhận đơn đã hủy sẽ bị từ chối.
Trang Checkout thành công và chi tiết đơn của khách tự kiểm tra mỗi 10 giây khi
đang mở, kiểm tra lại khi quay về tab và có nút **Kiểm tra thanh toán**. Sau khi
Staff/Admin xác nhận, trang tải lại để báo **Thanh toán thành công!**, cập nhật
trạng thái và ẩn QR. Đây là đối soát thủ công; chưa kết nối webhook ngân hàng.

## Chạy kiểm thử

Trong IntelliJ, mở `src/test/java/com/bakery/BakeryShopApplicationTests.java`
và bấm Run ở cạnh tên class. Có thể chạy toàn bộ test bằng cách nhấp phải thư mục
`src/test/java` và chọn `Run 'All Tests'`.

Test sử dụng cùng database MySQL local `bakery_db`. Mỗi thành viên có database
riêng trên máy của mình; dữ liệu local không được commit lên Git. Khi viết test
có thay đổi dữ liệu, thành viên phải tự dọn dữ liệu test đã tạo.

## Quy tắc cấu hình

- Cấu hình dùng chung đặt trong `application.properties`.
- Cấu hình local đặt trong `application-dev.properties` và lấy dữ liệu nhạy cảm
  từ biến môi trường.
- Không ghi mật khẩu database, email hoặc API key trực tiếp vào source code.
- File tải lên khi chạy local nằm trong `uploads/` và không được commit.

## Git workflow đề xuất

- Nhánh tính năng: `feature/<ten-chuc-nang>`.
- Nhánh sửa lỗi: `fix/<ten-loi>`.
- Không commit thư mục `target/`, `.idea/`, `uploads/` hoặc file `.env`.
- Trước khi tạo pull request, chạy toàn bộ test trong IntelliJ IDEA.
