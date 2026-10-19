-- 100 sample products for local development.
-- Safe to run multiple times: rows are skipped when the name already exists.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO categories (name, description, active, version, created_at, updated_at)
SELECT sample.name, sample.description, 1, 0, NOW(), NOW()
FROM (
  SELECT 'Bánh kem' AS name, 'Các loại bánh kem cho sinh nhật và dịp đặc biệt.' AS description
  UNION ALL SELECT 'Bánh ngọt', 'Bánh ngọt dùng cùng trà và cà phê.'
  UNION ALL SELECT 'Bánh mì', 'Bánh mì thủ công được làm mới mỗi ngày.'
  UNION ALL SELECT 'Cookies', 'Cookies thơm giòn với nhiều hương vị.'
) AS sample
WHERE NOT EXISTS (
  SELECT 1 FROM categories existing WHERE existing.name = sample.name
);

-- Keep the original five so databases that ran the old script only add 95 more.
INSERT INTO products
  (name, description, price, stock_quantity, preparation_days, status,
   category_id, version, created_at, updated_at)
SELECT sample.name, sample.description, sample.price, sample.stock, sample.days,
       'SELLING', category.id, 0, NOW(), NOW()
FROM (
  SELECT 'Bánh kem dâu' AS name,
         'Cốt bánh vani mềm, kem tươi và dâu tây.' AS description,
         250000 AS price, 10 AS stock, 1 AS days, 'Bánh kem' AS category_name
  UNION ALL SELECT 'Bánh kem socola', 'Cốt bánh cacao phủ kem socola.', 280000, 8, 1, 'Bánh kem'
  UNION ALL SELECT 'Tiramisu', 'Bánh vị cà phê với lớp kem mascarpone.', 65000, 15, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Cheesecake chanh dây', 'Bánh phô mai với sốt chanh dây chua nhẹ.', 55000, 12, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Bánh su kem', 'Vỏ bánh mềm với nhân kem trứng.', 25000, 20, 0, 'Bánh ngọt'
) AS sample
JOIN categories category ON category.name = sample.category_name
WHERE NOT EXISTS (
  SELECT 1 FROM products existing WHERE existing.name = sample.name
);

-- 19 product lines x 5 flavors = 95 products.
INSERT INTO products
  (name, description, price, stock_quantity, preparation_days, status,
   category_id, version, created_at, updated_at)
SELECT CONCAT(template.product_name, ' ', flavor.flavor_name),
       CONCAT(template.product_name, ' vị ', LOWER(flavor.flavor_name),
              ', làm mới trong ngày từ nguyên liệu được tuyển chọn.'),
       template.base_price + flavor.extra_price,
       12 + MOD(template.template_no * 7 + flavor.flavor_no * 3, 39),
       template.preparation_days,
       'SELLING', category.id, 0, NOW(), NOW()
FROM (
  SELECT 1 AS template_no, 'Bánh kem sinh nhật' AS product_name, 'Bánh kem' AS category_name, 265000 AS base_price, 2 AS preparation_days
  UNION ALL SELECT 2, 'Bánh kem mousse', 'Bánh kem', 295000, 2
  UNION ALL SELECT 3, 'Bánh kem chiffon', 'Bánh kem', 245000, 2
  UNION ALL SELECT 4, 'Bánh kem tiramisu', 'Bánh kem', 315000, 2
  UNION ALL SELECT 5, 'Bánh kem mini', 'Bánh kem', 135000, 1
  UNION ALL SELECT 6, 'Croissant bơ', 'Bánh ngọt', 42000, 1
  UNION ALL SELECT 7, 'Bánh tart', 'Bánh ngọt', 55000, 1
  UNION ALL SELECT 8, 'Cupcake', 'Bánh ngọt', 38000, 1
  UNION ALL SELECT 9, 'Donut', 'Bánh ngọt', 35000, 1
  UNION ALL SELECT 10, 'Bánh cuộn kem', 'Bánh ngọt', 49000, 1
  UNION ALL SELECT 11, 'Bánh mì sourdough', 'Bánh mì', 82000, 2
  UNION ALL SELECT 12, 'Bánh mì brioche', 'Bánh mì', 68000, 1
  UNION ALL SELECT 13, 'Bánh mì baguette', 'Bánh mì', 38000, 1
  UNION ALL SELECT 14, 'Bánh mì hoa cúc', 'Bánh mì', 75000, 1
  UNION ALL SELECT 15, 'Bánh mì cuộn', 'Bánh mì', 45000, 1
  UNION ALL SELECT 16, 'Cookie chocolate chip', 'Cookies', 39000, 1
  UNION ALL SELECT 17, 'Cookie hạnh nhân', 'Cookies', 45000, 1
  UNION ALL SELECT 18, 'Cookie yến mạch', 'Cookies', 42000, 1
  UNION ALL SELECT 19, 'Cookie bơ', 'Cookies', 36000, 1
) AS template
CROSS JOIN (
  SELECT 1 AS flavor_no, 'Dâu tây' AS flavor_name, 0 AS extra_price
  UNION ALL SELECT 2, 'Chocolate', 5000
  UNION ALL SELECT 3, 'Matcha', 10000
  UNION ALL SELECT 4, 'Vani', 15000
  UNION ALL SELECT 5, 'Caramel', 20000
) AS flavor
JOIN categories category ON category.name = template.category_name
WHERE NOT EXISTS (
  SELECT 1 FROM products existing
  WHERE existing.name = CONCAT(template.product_name, ' ', flavor.flavor_name)
);

-- Give every product without an image a local placeholder as its primary image.
INSERT INTO product_images
  (product_id, image_path, display_order, created_at, updated_at)
SELECT product.id, '/assets/product-placeholder.svg', 0, NOW(), NOW()
FROM products product
WHERE NOT EXISTS (
  SELECT 1
  FROM product_images existing
  WHERE existing.product_id = product.id
);

COMMIT;

-- Quick verification (includes any products that existed before this script).
SELECT COUNT(*) AS total_products FROM products;
SELECT COUNT(*) AS total_product_images FROM product_images;
