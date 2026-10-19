# Product API

`GET /products` returns JSON containing `message`, `products`, pagination metadata,
the current `filters`, and `next` / `previous` URLs. Follow these URLs to keep all
filters when moving between pages. A null URL means there is no adjacent page.

Optional query parameters:

| Parameter | Meaning |
| --- | --- |
| `keyword` | Match name or description, ignoring case and Vietnamese accents |
| `categoryId` | Category ID from the product data |
| `minPrice` | Inclusive minimum price, in VND |
| `maxPrice` | Inclusive maximum price, in VND |
| `sort` | `name` (default), `price_asc`, `price_desc`, `best_selling` |
| `page` | Zero-based page number, default `0` |
| `size` | Page size, default `12`, allowed range `1` to `100` |

All filters can be combined. Blank keyword returns all matching products.
Invalid filters return JSON with HTTP 400. No matches return HTTP 200 with
`message: "Không tìm thấy sản phẩm phù hợp"` and `products: []`.

Best selling means the total quantity from `DELIVERED` orders. Cancelled,
pending, confirmed, and delivering orders do not contribute. Products with
equal sales or prices are ordered by name, then ID, for stable pagination.

Example:

```text
/products?categoryId=1&minPrice=50000&maxPrice=300000&sort=price_asc&page=0&size=2
```

`GET /products/{id}` returns product details as before.
