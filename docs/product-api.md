# Public Catalog API

Các endpoint trong tài liệu này là public, không yêu cầu đăng nhập.

## Danh mục đang hoạt động

```http
GET /api/categories
```

Trả về mảng danh mục đang hoạt động để storefront tạo bộ lọc động. Mỗi phần tử
gồm `id`, `name`, `description` và `active`.

## Danh sách sản phẩm

```http
GET /api/products
```

Response gồm `message`, `products`, thông tin phân trang, `filters`, `next` và
`previous`. `next`/`previous` là URL API đầy đủ phần path `/api/products`; giá trị
`null` nghĩa là không còn trang kế tiếp hoặc trước đó.

Tham số tùy chọn:

| Tham số | Ý nghĩa |
| --- | --- |
| `keyword` | Tìm theo tên hoặc mô tả, không phân biệt hoa thường và dấu tiếng Việt |
| `categoryId` | ID lấy từ `GET /api/categories` |
| `minPrice` | Giá tối thiểu, tính bằng VND |
| `maxPrice` | Giá tối đa, tính bằng VND |
| `sort` | `name`, `price_asc`, `price_desc` hoặc `best_selling` |
| `page` | Số trang bắt đầu từ `0`, mặc định `0` |
| `size` | Số phần tử, mặc định `12`, từ `1` đến `100` |

Mỗi sản phẩm tóm tắt có thông tin cơ bản, danh mục, ảnh chính và `badge`. Badge có
thể là `NEW`, `BEST_SELLER`, `LIMITED`, `SEASONAL` hoặc `null`.

Ví dụ:

```text
/api/products?categoryId=1&minPrice=50000&maxPrice=300000&sort=price_asc&page=0&size=12
```

Filter không hợp lệ trả HTTP `400`. Không có kết quả vẫn trả HTTP `200`,
`products: []` và thông báo tương ứng.

## Chi tiết sản phẩm

```http
GET /api/products/{productId}
```

Ngoài thông tin cơ bản và danh sách ảnh, response chi tiết có:

- `badge`
- `ingredients`
- `storageInstructions`
- `allergenInfo`

Các trường tùy chọn có thể là `null`. Storefront phải ẩn phần giao diện tương ứng
thay vì hiển thị nội dung mẫu. ID không tồn tại trả HTTP `404` dạng JSON.
