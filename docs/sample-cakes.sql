-- Sample data for testing the cake list. Safe to run again by name.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO categories (name, description, active, version, created_at, updated_at)
SELECT 'Bánh kem', 'Các loại bánh kem cho dịp đặc biệt', 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Bánh kem');

INSERT INTO categories (name, description, active, version, created_at, updated_at)
SELECT 'Bánh ngọt', 'Bánh ngọt dùng cùng trà và cà phê', 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Bánh ngọt');

INSERT INTO products (name, description, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT sample.name, sample.description, sample.price, sample.stock, sample.days, 'SELLING', category.id, 0, NOW(), NOW()
FROM (
  SELECT 'Bánh kem dâu' AS name, 'Cốt bánh vani mềm, kem tươi và dâu tây.' AS description, 250000 AS price, 10 AS stock, 1 AS days, 'Bánh kem' AS category_name
  UNION ALL SELECT 'Bánh kem socola', 'Cốt bánh cacao phủ kem socola.', 280000, 8, 1, 'Bánh kem'
  UNION ALL SELECT 'Tiramisu', 'Bánh vị cà phê với lớp kem mascarpone.', 65000, 15, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Cheesecake chanh dây', 'Bánh phô mai với lớp sốt chanh dây chua nhẹ.', 55000, 12, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Bánh su kem', 'Vỏ bánh mềm với nhân kem trứng.', 25000, 0, 0, 'Bánh ngọt'
) AS sample
JOIN categories category ON category.name = sample.category_name
WHERE NOT EXISTS (SELECT 1 FROM products existing WHERE existing.name = sample.name);

COMMIT;
