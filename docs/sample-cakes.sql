-- Sample catalog data for UI, search, filter and pagination testing.
-- Safe to run repeatedly: categories and products are inserted only when their names do not exist.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO categories (name, description, active, version, created_at, updated_at)
SELECT sample.name, sample.description, 1, 0, NOW(), NOW()
FROM (
  SELECT 'Bánh kem' AS name, 'Các loại bánh kem cho sinh nhật và dịp đặc biệt' AS description
  UNION ALL SELECT 'Bánh ngọt', 'Bánh ngọt dùng cùng trà và cà phê'
  UNION ALL SELECT 'Bánh mì', 'Bánh mì tươi nướng mới mỗi ngày'
  UNION ALL SELECT 'Bánh quy', 'Bánh quy giòn thơm đóng gói tiện lợi'
  UNION ALL SELECT 'Cookies', 'Cookies mềm và giòn với nhiều hương vị'
  UNION ALL SELECT 'Tart & Mousse', 'Tart trái cây và mousse mát lạnh'
) AS sample
WHERE NOT EXISTS (SELECT 1 FROM categories existing WHERE existing.name = sample.name);

INSERT INTO products (name, description, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT sample.name, sample.description, sample.price, sample.stock, sample.days,
       'SELLING', category.id, 0, NOW(), NOW()
FROM (
  -- Bánh kem: 10
  SELECT 'Bánh kem dâu tây' AS name, 'Cốt bánh vani mềm, kem tươi và dâu tây.' AS description, 250000 AS price, 10 AS stock, 1 AS days, 'Bánh kem' AS category_name
  UNION ALL SELECT 'Bánh kem chocolate', 'Cốt bánh cacao phủ kem chocolate đậm vị.', 280000, 8, 1, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem matcha', 'Cốt matcha Nhật Bản kết hợp kem sữa thanh nhẹ.', 295000, 9, 1, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem red velvet', 'Cốt red velvet mềm ẩm cùng kem phô mai.', 320000, 7, 2, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem bắp', 'Kem bắp ngọt dịu phủ trên cốt chiffon mềm.', 240000, 12, 1, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem tiramisu', 'Hương cà phê và cacao trên lớp kem mascarpone.', 330000, 6, 2, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem xoài', 'Kem sữa tươi kết hợp xoài chín và cốt vani.', 270000, 11, 1, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem việt quất', 'Sốt việt quất chua nhẹ cân bằng vị kem béo.', 310000, 8, 1, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem caramel muối', 'Caramel muối thơm đậm trên cốt bánh vani.', 300000, 5, 2, 'Bánh kem'
  UNION ALL SELECT 'Bánh kem hoa quả', 'Kem tươi và trái cây theo mùa nhiều màu sắc.', 350000, 7, 2, 'Bánh kem'

  -- Bánh ngọt: 10
  UNION ALL SELECT 'Tiramisu ly', 'Bánh vị cà phê với lớp kem mascarpone.', 65000, 15, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Cheesecake chanh dây', 'Bánh phô mai với sốt chanh dây chua nhẹ.', 55000, 12, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Bánh su kem', 'Vỏ choux mềm với nhân kem trứng.', 25000, 20, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Bánh cuộn trà xanh', 'Bánh cuộn mềm với kem matcha thanh nhẹ.', 48000, 14, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Brownie socola', 'Brownie ẩm mềm với socola đậm vị.', 45000, 18, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Muffin việt quất', 'Muffin bơ mềm với việt quất mọng.', 42000, 16, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Cupcake vani', 'Cupcake vani phủ kem bơ nhẹ.', 38000, 22, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Bông lan trứng muối', 'Bông lan mềm, sốt bơ trứng và trứng muối.', 75000, 13, 1, 'Bánh ngọt'
  UNION ALL SELECT 'Madeleine chanh', 'Bánh con sò bơ thơm vỏ chanh tươi.', 48000, 20, 0, 'Bánh ngọt'
  UNION ALL SELECT 'Financier hạnh nhân', 'Bánh hạnh nhân nhỏ với bơ nâu thơm dịu.', 52000, 17, 0, 'Bánh ngọt'

  -- Bánh mì: 10
  UNION ALL SELECT 'Croissant bơ', 'Croissant nhiều lớp, giòn ngoài và thơm bơ.', 42000, 18, 0, 'Bánh mì'
  UNION ALL SELECT 'Bánh mì bơ tỏi', 'Bánh mì vàng giòn phủ bơ tỏi và thảo mộc.', 35000, 30, 0, 'Bánh mì'
  UNION ALL SELECT 'Bánh mì sữa', 'Bánh mì mềm xốp với vị sữa ngọt vừa.', 28000, 25, 0, 'Bánh mì'
  UNION ALL SELECT 'Brioche mật ong', 'Brioche mềm dai, thơm bơ và mật ong.', 45000, 16, 1, 'Bánh mì'
  UNION ALL SELECT 'Baguette truyền thống', 'Baguette vỏ giòn, ruột mềm và thơm lúa mì.', 32000, 24, 0, 'Bánh mì'
  UNION ALL SELECT 'Bánh mì phô mai', 'Bánh mì mềm với nhân phô mai kéo sợi.', 39000, 20, 0, 'Bánh mì'
  UNION ALL SELECT 'Bánh mì xúc xích', 'Bánh mì bơ mềm cuộn xúc xích hun khói.', 42000, 19, 0, 'Bánh mì'
  UNION ALL SELECT 'Pain au chocolat', 'Bánh ngàn lớp bơ với thanh socola bên trong.', 48000, 14, 0, 'Bánh mì'
  UNION ALL SELECT 'Danish đào', 'Danish giòn xốp với đào vàng và kem trứng.', 52000, 12, 1, 'Bánh mì'
  UNION ALL SELECT 'Bánh mì nguyên cám', 'Ổ bánh giàu chất xơ từ bột mì nguyên cám.', 55000, 11, 1, 'Bánh mì'

  -- Bánh quy: 10
  UNION ALL SELECT 'Bánh quy bơ', 'Bánh quy bơ tan nhẹ, nướng theo mẻ nhỏ.', 65000, 20, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy hạnh nhân', 'Hạnh nhân rang giòn trong lớp bánh quy bơ.', 75000, 16, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy mè đen', 'Bánh quy giòn thơm với mè đen rang.', 68000, 18, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy dừa', 'Dừa sấy thơm béo trong từng miếng bánh giòn.', 62000, 21, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy cà phê', 'Bánh quy bơ pha hương espresso dịu.', 70000, 15, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy quế', 'Hương quế ấm áp và lớp đường mỏng.', 66000, 17, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy trà xanh', 'Bánh quy matcha thanh nhẹ, ít ngọt.', 72000, 14, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy phô mai', 'Vị phô mai mặn nhẹ trong bánh quy giòn.', 78000, 13, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy yến mạch', 'Yến mạch, nho khô và mật ong tự nhiên.', 74000, 19, 0, 'Bánh quy'
  UNION ALL SELECT 'Bánh quy cacao', 'Bánh quy cacao giòn với vị socola đậm.', 70000, 16, 0, 'Bánh quy'

  -- Cookies: 10
  UNION ALL SELECT 'Cookies chocolate chip', 'Cookies giòn ngoài, mềm trong với chocolate chip.', 55000, 22, 0, 'Cookies'
  UNION ALL SELECT 'Cookies double chocolate', 'Cốt cacao và hai loại chocolate đậm vị.', 62000, 18, 0, 'Cookies'
  UNION ALL SELECT 'Cookies red velvet', 'Cookies red velvet mềm với chocolate trắng.', 65000, 15, 0, 'Cookies'
  UNION ALL SELECT 'Cookies matcha macadamia', 'Matcha thanh nhẹ kết hợp hạt macadamia.', 68000, 14, 0, 'Cookies'
  UNION ALL SELECT 'Cookies bơ đậu phộng', 'Cookies mềm thơm bơ đậu phộng rang.', 58000, 20, 0, 'Cookies'
  UNION ALL SELECT 'Cookies caramel muối', 'Nhân caramel muối mềm trong cốt bánh vani.', 65000, 13, 0, 'Cookies'
  UNION ALL SELECT 'Cookies hạt dẻ', 'Cookies bơ nâu với hạt dẻ rang giòn.', 70000, 12, 0, 'Cookies'
  UNION ALL SELECT 'Cookies marshmallow', 'Socola và marshmallow mềm dẻo bên trong.', 64000, 17, 0, 'Cookies'
  UNION ALL SELECT 'Cookies dâu trắng', 'Dâu sấy kết hợp chocolate trắng dịu ngọt.', 66000, 14, 0, 'Cookies'
  UNION ALL SELECT 'Cookies yến mạch nho', 'Yến mạch cán, nho khô và bột quế.', 56000, 19, 0, 'Cookies'

  -- Tart & Mousse: 10
  UNION ALL SELECT 'Tart trứng', 'Đế tart giòn với nhân trứng sữa béo nhẹ.', 38000, 14, 0, 'Tart & Mousse'
  UNION ALL SELECT 'Tart trái cây', 'Đế tart bơ, kem trứng và trái cây theo mùa.', 68000, 12, 0, 'Tart & Mousse'
  UNION ALL SELECT 'Tart chanh vàng', 'Kem chanh vàng chua dịu trên đế tart giòn.', 62000, 13, 0, 'Tart & Mousse'
  UNION ALL SELECT 'Tart socola', 'Ganache socola đậm vị trên đế cacao.', 70000, 10, 0, 'Tart & Mousse'
  UNION ALL SELECT 'Tart phô mai việt quất', 'Kem phô mai và sốt việt quất tươi.', 75000, 9, 0, 'Tart & Mousse'
  UNION ALL SELECT 'Mousse xoài', 'Mousse xoài mát nhẹ với lớp gương vàng óng.', 65000, 11, 1, 'Tart & Mousse'
  UNION ALL SELECT 'Mousse socola', 'Mousse socola mềm mịn trên cốt bánh cacao.', 72000, 10, 1, 'Tart & Mousse'
  UNION ALL SELECT 'Mousse dâu', 'Mousse dâu chua ngọt với cốt vanilla.', 68000, 12, 1, 'Tart & Mousse'
  UNION ALL SELECT 'Mousse matcha', 'Mousse matcha Nhật cùng kem sữa thanh nhẹ.', 74000, 8, 1, 'Tart & Mousse'
  UNION ALL SELECT 'Panna cotta chanh dây', 'Kem sữa mát lạnh với sốt chanh dây.', 52000, 15, 0, 'Tart & Mousse'
) AS sample
JOIN categories category ON category.name = sample.category_name
WHERE NOT EXISTS (SELECT 1 FROM products existing WHERE existing.name = sample.name);

COMMIT;

-- Check how many products are currently available after importing the sample catalog.
SELECT COUNT(*) AS selling_product_count FROM products WHERE status = 'SELLING';
