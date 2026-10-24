# Kiến trúc dự án

Project tổ chức mã nguồn theo **phân hệ nghiệp vụ**. Mỗi thành viên làm use case
trong phân hệ được giao, tránh tạo package dùng chung tùy ý hoặc đặt business
logic vào controller.

## Các phân hệ

```text
com.bakery
├── account       Tài khoản, đăng ký, hồ sơ, OTP và phân quyền
├── catalog       Danh mục, sản phẩm và hình ảnh sản phẩm
├── cart          Giỏ hàng lưu trong HTTP Session
├── order         Checkout, đơn hàng, chi tiết đơn và thanh toán
├── notification  Gửi email và các thông báo hệ thống
├── storage       Lưu trữ và phục vụ file upload
├── common        Exception, validation và tiện ích dùng chung
└── config        Cấu hình Spring dùng chung
```

Không tạo một phân hệ `admin` riêng cho toàn bộ nghiệp vụ. Controller CMS được
đặt bên trong đúng phân hệ, ví dụ:

```text
catalog/controller/admin/AdminProductController
order/controller/admin/AdminOrderController
account/controller/admin/AdminAccountController
```

Cách này giúp thành viên phụ trách sản phẩm không phải sửa chung một
`AdminController` lớn với thành viên phụ trách đơn hàng.

## Cấu trúc bên trong một phân hệ

Chỉ tạo package khi thực sự có class sử dụng:

```text
<feature>/
├── controller/
│   ├── admin/       Endpoint dành cho Staff/Admin nếu có
│   └── web/         Endpoint giao diện khách hàng
├── dto/             Form request và dữ liệu response/view
├── entity/          JPA entity; cart dùng model thay vì entity
├── model/           Object không lưu database
├── repository/      Spring Data repository
└── service/         Business logic và transaction boundary
```

## Quy tắc phụ thuộc

Luồng gọi chuẩn:

```text
Controller -> Service -> Repository -> Database
```

- Controller không gọi repository trực tiếp.
- Entity không được dùng làm form nhận dữ liệu từ người dùng.
- DTO không chứa business logic.
- Repository chỉ truy vấn dữ liệu, không điều phối use case.
- Transaction bắt đầu ở service.
- Phân hệ khác chỉ gọi service công khai; không gọi repository của nhau.
- `common` không được phụ thuộc ngược vào một phân hệ nghiệp vụ.
- `cart` lưu trong HTTP Session, không tạo bảng Cart hoặc CartItem.

## Quy tắc đặt tên

```text
<UseCase>Request       Dữ liệu form gửi lên
<UseCase>Response      Dữ liệu trả về hoặc hiển thị
<Entity>Repository     Truy cập dữ liệu
<Domain>Service        Xử lý nghiệp vụ
<Feature>Controller    Controller phía khách hàng
Admin<Feature>Controller Controller CMS
```

Các service nghiệp vụ dùng interface đã được định nghĩa sẵn làm hợp đồng giữa
các phân hệ. Implementation đặt trong package `service.impl` và dùng hậu tố
`Impl`, ví dụ `ProductAdminServiceImpl`.

## Quy tắc làm việc nhóm

- Một use case nên nằm trọn trong một phân hệ.
- Không tự ý đổi entity/enum dùng chung khi chưa báo nhóm.
- Không commit secret, file `.env`, file upload, `.idea` hoặc `target`.
- Không sửa đồng thời `SecurityConfig` từ nhiều nhánh; người phụ trách nền tảng
  là người tích hợp các rule phân quyền.
- Mọi pull request phải chạy được test context trong IntelliJ.
