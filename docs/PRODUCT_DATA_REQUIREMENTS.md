# Yêu cầu bổ sung dữ liệu sản phẩm và khách hàng

Tài liệu này ghi lại các quyết định dữ liệu đã chốt để giao diện storefront và CMS
có thể kết nối với dữ liệu thật. Phần badge và nội dung chi tiết sản phẩm đã được
triển khai trên branch `Thai`; phần khách hàng thân thiết và quà sinh nhật vẫn là
định hướng, chưa thay đổi `Account` hoặc database liên quan.

## 1. Badge sản phẩm

Chỉ sử dụng tối đa bốn loại badge:

| Mã lưu trong database | Nhãn hiển thị |
| --- | --- |
| `NEW` | Mới |
| `BEST_SELLER` | Bán chạy |
| `LIMITED` | Phiên bản giới hạn |
| `SEASONAL` | Theo mùa |

Mỗi sản phẩm có tối đa một badge. Sản phẩm không cần badge sẽ lưu `null`.
Database chỉ lưu mã; tên hiển thị, màu sắc và icon được xử lý cố định tại giao diện.

Enum dự kiến:

```java
public enum ProductBadge {
    NEW,
    BEST_SELLER,
    LIMITED,
    SEASONAL
}
```

Trường dự kiến trong `Product`:

```java
@Enumerated(EnumType.STRING)
@Column(length = 20)
private ProductBadge badge;
```

Quy tắc hiển thị:

- Không có badge thì không render badge hoặc khoảng trống dành cho badge.
- Không nhận chuỗi badge tùy ý ngoài bốn giá trị đã chốt.
- Các nhãn mô tả như “Socola 65%”, “Dâu hữu cơ” hoặc “Men tươi” không phải
  badge hệ thống.
- Badge chỉ thể hiện trạng thái marketing, không thay thế trạng thái bán hàng
  `ProductStatus`.

## 2. Nội dung chi tiết sản phẩm

Bổ sung ba trường tùy chọn dạng `TEXT` vào `Product`:

```java
@Column(columnDefinition = "TEXT")
private String ingredients;

@Column(name = "storage_instructions", columnDefinition = "TEXT")
private String storageInstructions;

@Column(name = "allergen_info", columnDefinition = "TEXT")
private String allergenInfo;
```

Ý nghĩa:

- `ingredients`: thành phần và đặc điểm hương vị.
- `storageInstructions`: cách thưởng thức, hâm nóng và bảo quản.
- `allergenInfo`: thành phần có nguy cơ gây dị ứng.

Các trường này không bắt buộc. Khi trường không có dữ liệu, giao diện phải ẩn toàn
bộ accordion tương ứng và không hiển thị nội dung mẫu.

Trong DTO, giới hạn đề xuất:

```java
@Size(max = 10000)
private String ingredients;

@Size(max = 10000)
private String storageInstructions;

@Size(max = 5000)
private String allergenInfo;
```

Phần “Cam kết của LUNE” là nội dung chung của thương hiệu, được giữ cố định trên
giao diện và không lưu lặp lại theo từng sản phẩm.

Trước mắt lưu nội dung dạng văn bản thuần. Không tạo bảng nguyên liệu riêng và
không lưu HTML do CMS chưa có nhu cầu quản lý cấu trúc phức tạp.

## 3. Phạm vi cần cập nhật khi triển khai

- Entity `Product`.
- Enum `ProductBadge`.
- `ProductCreateRequest` và `ProductUpdateRequest`.
- `ProductSummaryResponse` để hiển thị badge trên card.
- `ProductDetailResponse` để hiển thị badge và nội dung chi tiết.
- Mapping trong `ProductAdminServiceImpl` và `ProductQueryServiceImpl`.
- Form thêm/sửa sản phẩm trong CMS.
- API danh sách và chi tiết sản phẩm.
- Template card và trang chi tiết sản phẩm.
- Database schema và `docs/sample-cakes.sql`.
- Validation, unit test và integration test liên quan.

Các service interface hiện nhận/trả DTO nên không cần đổi chữ ký chỉ để bổ sung
các field trên.

Project hiện dùng `spring.jpa.hibernate.ddl-auto=update` cho môi trường local. Khi
triển khai có thể khởi động ứng dụng để Hibernate tự bổ sung cột hoặc chạy đúng một
lần file `docs/migrations/2026-10-25-product-details.sql`. Không chạy migration nếu
các cột đã tồn tại. Câu lệnh tương ứng:

```sql
ALTER TABLE products
    ADD COLUMN badge VARCHAR(20) NULL,
    ADD COLUMN ingredients TEXT NULL,
    ADD COLUMN storage_instructions TEXT NULL,
    ADD COLUMN allergen_info TEXT NULL;
```

## 4. Quyết định về khách hàng thân thiết và quà sinh nhật

### 4.1. Nên làm: quà sinh nhật tối giản

Quà sinh nhật có giá trị với trải nghiệm khách hàng và phù hợp với nghiệp vụ tiệm
bánh. Tuy nhiên chỉ triển khai sau khi use case hồ sơ khách hàng và cơ chế ưu đãi
đã được nhóm chốt.

Phạm vi tối thiểu đề xuất:

- Bổ sung ngày sinh tùy chọn `dateOfBirth` vào hồ sơ `Account`.
- Không bắt buộc nhập ngày sinh khi đăng ký.
- Có quy tắc giới hạn thay đổi ngày sinh để tránh lợi dụng ưu đãi.
- Mỗi tài khoản chỉ nhận một quyền lợi sinh nhật trong một năm.
- Quyền lợi có thời hạn rõ ràng và được kiểm tra ở server.
- Không chỉ dựa vào giao diện hoặc một cờ boolean để xác nhận đã nhận quà.

Chưa chốt loại quyền lợi cụ thể. Nhóm cần chọn một trong các phương án như mã giảm
giá, sản phẩm tặng kèm hoặc ưu đãi phí giao hàng trước khi thiết kế bảng dữ liệu.

### 4.2. Có thể làm sau: nhãn khách hàng thân thiết

Nếu CMS cần nhận biết khách quen, trạng thái khách hàng thân thiết nên được suy ra
từ dữ liệu đơn hàng đã hoàn tất, ví dụ số đơn hoặc tổng chi tiêu trong một khoảng
thời gian. Không thêm trường `boolean loyalCustomer` vào `Account`, vì giá trị này
dễ sai lệch và phải cập nhật thủ công.

Chỉ triển khai khi nhóm chốt đủ:

- Đơn hàng/trạng thái nào được tính.
- Ngưỡng số đơn hoặc tổng chi tiêu.
- Khoảng thời gian xét điều kiện.
- Quyền lợi thực tế của khách hàng thân thiết.

### 4.3. Không làm trong phạm vi hiện tại: điểm thưởng và hạng thành viên

Chưa triển khai hệ thống điểm thưởng hoặc các hạng thành viên. Hai chức năng này
đòi hỏi sổ giao dịch điểm, quy tắc cộng/trừ/hoàn điểm, thời hạn điểm, xử lý hủy đơn,
điều kiện lên/xuống hạng và giao diện quản trị riêng. Chi phí triển khai và kiểm thử
không tương xứng với phạm vi hiện tại của dự án.

## 5. Thứ tự ưu tiên đã chốt

1. Hoàn thiện dữ liệu sản phẩm: badge và ba nhóm nội dung chi tiết.
2. Hoàn thiện hồ sơ khách hàng và lịch sử đơn hàng.
3. Thiết kế quà sinh nhật tối giản sau khi có cơ chế ưu đãi.
4. Chỉ bổ sung nhãn khách hàng thân thiết nếu CMS thực sự cần phân nhóm khách.
5. Không triển khai điểm thưởng và hạng thành viên trong phiên bản hiện tại.

