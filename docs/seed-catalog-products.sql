-- Catalog data: 6 danh mục chuẩn + sản phẩm LUNE kèm ảnh.
-- Chạy lặp lại an toàn: danh mục/sản phẩm/ảnh chỉ được thêm khi chưa tồn tại.
-- File ảnh phải nằm trong thư mục uploads/ với đúng tên file được ghi bên dưới (/uploads/<tên file>).

USE bakery_db;

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO categories (name, description, active, version, created_at, updated_at)
SELECT s.name, s.description, 1, 0, NOW(), NOW()
FROM (
  SELECT 'Bánh Hộp Thiếc & Quà Tặng' AS name, 'Bánh quy và bánh nướng đóng hộp thiếc dùng làm quà tặng' AS description
  UNION ALL SELECT 'Bánh Kem & Bánh Tiệc', 'Bánh sinh nhật, gâteau, entremet và bánh kem cho các dịp đặc biệt'
  UNION ALL SELECT 'Bánh Mì & Viennoiserie', 'Croissant, brioche, baguette, sourdough và các dòng bánh ngàn lớp'
  UNION ALL SELECT 'Cookies & Bánh Nướng', 'Cookies, bánh quy, brownie, muffin, madeleine và financier'
  UNION ALL SELECT 'Mousse & Dessert Lạnh', 'Mousse, panna cotta, tiramisu ly, cheesecake lạnh và flan'
  UNION ALL SELECT 'Tart & Choux', 'Tart, éclair, su kem và các sản phẩm từ bột choux'
) AS s
WHERE NOT EXISTS (SELECT 1 FROM categories e WHERE e.name = s.name);

-- Bánh Kem Dâu Tây LUNE
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Kem Dâu Tây LUNE', 'Bánh kem cốt vanilla xốp mềm, xen kẽ các lớp mứt dâu tây thủ công và kem tươi whipping cream thanh nhẹ. Phủ bên trên là dâu tây tươi mọng nước.', 'BEST_SELLER', 'Bột mì, trứng gà, đường, bơ lạt, kem tươi (whipping cream), dâu tây tươi, mứt dâu tây.', 'Bảo quản trong ngăn mát tủ lạnh từ 2-4 độ C. Sử dụng tốt nhất trong vòng 48 giờ.', 'Sản phẩm có chứa gluten (lúa mì), bơ sữa (dairy) và trứng.', 450000.00, 5, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Kem & Bánh Tiệc'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Kem Dâu Tây LUNE');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/39e6d002-8f33-43f3-a8e4-1a3efbfd3962-banh-kem-dau-tay-01.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Kem Dâu Tây LUNE'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Croissant Bơ Pháp Truyền Thống
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Croissant Bơ Pháp Truyền Thống', 'Bánh sừng bò ngàn lớp chuẩn vị Pháp với vỏ ngoài giòn rụm, ruột bánh tổ ong dai mềm và ngập tràn hương bơ lạt thượng hạng.', 'BEST_SELLER', 'Bột mì cao cấp, bơ lạt Pháp, men, đường, muối, nước, trứng phết mặt bánh.', 'Bảo quản ở nhiệt độ phòng, nơi thoáng mát trong 2 ngày. Có thể làm nóng lại bằng lò nướng hoặc nồi chiên không dầu ở 160 độ C trong 3 phút trước khi ăn.', 'Sản phẩm có chứa gluten (lúa mì), bơ sữa (dairy) và trứng.', 45000.00, 30, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Mì & Viennoiserie'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Croissant Bơ Pháp Truyền Thống');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/1946ddba-07b6-4386-b003-f3993186167e-croissant.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Croissant Bơ Pháp Truyền Thống'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Tart Trái Cây Nhiệt Đới
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Tart Trái Cây Nhiệt Đới', 'Đế tart hạnh nhân giòn tan, nhân kem custard béo ngậy điểm xuyết cùng các loại trái cây nhiệt đới tươi theo mùa như xoài, kiwi, dâu tây và việt quất.', 'SEASONAL', 'Bột mì, hạnh nhân xay, bơ lạt, trứng, sữa tươi, đường, tinh chất vani, xoài, kiwi, dâu, việt quất.', 'Bảo quản trong ngăn mát tủ lạnh (2-4 độ C). Ăn ngon nhất trong ngày.', 'Chứa gluten, các loại hạt (hạnh nhân), trứng, và bơ sữa.', 75000.00, 15, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Tart & Choux'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Tart Trái Cây Nhiệt Đới');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/79cf825a-5498-418e-810b-f52130c44167-tart1.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Tart Trái Cây Nhiệt Đới'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/446fe50b-b5e7-4c89-992a-d49366fcf279-tart2.png', 1, NOW(), NOW() FROM products p WHERE p.name = 'Tart Trái Cây Nhiệt Đới'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 1);

-- Choux Kem Vani Madagascar
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Choux Kem Vani Madagascar', 'Bánh su kem mềm dai với lớp vỏ nướng mỏng nhẹ, nhân ngập tràn kem mouselinne nấu từ hạt vani Madagascar nguyên chất thơm lừng.', NULL, 'Bột mì, bơ lạt, nước, trứng gà, sữa tươi nguyên kem, đường, tinh bột bắp, hạt vani Madagascar.', 'Bắt buộc bảo quản lạnh (2-6 độ C). Sử dụng trong vòng 2 ngày kể từ ngày sản xuất.', 'Sản phẩm chứa gluten, trứng, và bơ sữa.', 35000.00, 40, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Tart & Choux'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Choux Kem Vani Madagascar');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/fe47fa36-23da-49bb-8084-f69fbf492b10-choux.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Choux Kem Vani Madagascar'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/fa4b48e0-e5f3-4c25-aa65-00a4faf806e9-images (4).jpg', 1, NOW(), NOW() FROM products p WHERE p.name = 'Choux Kem Vani Madagascar'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 1);

-- Tiramisu Lạnh Ý Truyền Thống
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Tiramisu Lạnh Ý Truyền Thống', 'Lớp kem mascarpone mượt mà xen kẽ bánh sâm panh thấm đẫm cà phê espresso đậm đặc và rượu Kahlua, phủ bột cacao nguyên chất đắng nhẹ trên cùng.', 'NEW', 'Phô mai Mascarpone, trứng pasteurized, đường, kem tươi, bánh ladyfinger, cà phê espresso, rượu Kahlua, bột cacao.', 'Giữ lạnh liên tục ở 2-4 độ C. Không để ngoài nhiệt độ phòng quá 30 phút.', 'Chứa gluten, trứng, bơ sữa (phô mai), và có một lượng rất nhỏ cồn từ rượu.', 85000.00, 20, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Mousse & Dessert Lạnh'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Tiramisu Lạnh Ý Truyền Thống');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/0cd79e49-437d-474a-b58a-19a7fd38ffb1-tiramisu.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Tiramisu Lạnh Ý Truyền Thống'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Bánh Mì Hoa Cúc Brioche
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Mì Hoa Cúc Brioche', 'Bánh mì Brioche kiểu Pháp thơm lừng bơ và hoa cam, cốt bánh xé sợi dai mềm bồng bềnh như mây, tan ngay trong miệng.', NULL, 'Bột mì dai, bơ lạt Pháp, trứng gà, đường, men, sữa tươi, tinh chất hoa cam tự nhiên, muối.', 'Buộc kín miệng túi, bảo quản ở nhiệt độ phòng nơi thoáng mát từ 3-4 ngày.', 'Sản phẩm chứa gluten, bơ sữa và trứng.', 95000.00, 15, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Mì & Viennoiserie'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Mì Hoa Cúc Brioche');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/08faa6c0-06d3-49ba-ad92-3b96f7c5bf9e-brioche.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Mì Hoa Cúc Brioche'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Mousse Chanh Dây Phô Mai
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Mousse Chanh Dây Phô Mai', 'Sự kết hợp hoàn hảo giữa độ chua thanh của chanh dây tươi và vị béo ngậy của creamcheese trên lớp đế bánh quy bơ giòn.', 'SEASONAL', 'Creamcheese, whipping cream, đường, cốt chanh dây tươi, gelatin, bánh quy bơ, bơ lạt đun chảy.', 'Bảo quản lạnh 2-4 độ C. Sử dụng tốt nhất trong 2 ngày.', 'Chứa bơ sữa và gluten (từ đế bánh quy).', 80000.00, 12, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Mousse & Dessert Lạnh'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Mousse Chanh Dây Phô Mai');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/210c53eb-8bb0-4c41-8d98-9795b32edf96-mousse1.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Mousse Chanh Dây Phô Mai'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/d17a6abe-9eff-4e49-8a10-d68c5f48e351-mousse2.png', 1, NOW(), NOW() FROM products p WHERE p.name = 'Mousse Chanh Dây Phô Mai'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 1);

-- Cookies Chocolate Chip Cổ Điển
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Cookies Chocolate Chip Cổ Điển', 'Bánh quy bơ mềm dai kiểu Mỹ (Chewy Cookies) với đầy ắp các hạt chocolate đen 65% đắng nhẹ, cân bằng hoàn hảo hương vị.', NULL, 'Bột mì đa dụng, bơ lạt, đường nâu, đường trắng, trứng gà, chocolate chip 65%, baking soda, tinh chất vani, muối biển.', 'Đựng trong hộp kín, để nơi khô ráo, thoáng mát. Hạn sử dụng 14 ngày.', 'Chứa gluten, trứng, bơ sữa và đậu nành (từ lecithin trong chocolate).', 40000.00, 50, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Cookies & Bánh Nướng'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Cookies Chocolate Chip Cổ Điển');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/98e0a166-8ae7-48b1-b7a4-de398d271ca2-cookies.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Cookies Chocolate Chip Cổ Điển'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Hộp Quà Bánh Macaron 6 Vị
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Hộp Quà Bánh Macaron 6 Vị', 'Hộp thiếc sang trọng gồm 6 chiếc bánh Macaron kiểu Pháp với 6 hương vị signature: Vani, Chocolate, Chanh dây, Phúc bồn tử, Matcha, và Caramel muối.', 'LIMITED', 'Bột hạnh nhân, đường bột, lòng trắng trứng, bơ lạt, chocolate trắng, chocolate đen, trà xanh matcha, puree trái cây tươi.', 'Bảo quản ngăn mát tủ lạnh. Để ra nhiệt độ phòng khoảng 10 phút trước khi ăn để nhân bánh mềm lại.', 'Chứa các loại hạt (hạnh nhân), trứng và bơ sữa.', 240000.00, 10, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Hộp Thiếc & Quà Tặng'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Hộp Quà Bánh Macaron 6 Vị');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/96f03ded-6feb-4ba1-8746-43e7df60cc8f-macaron.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Hộp Quà Bánh Macaron 6 Vị'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Bánh Kem Chocolate Hạnh Nhân
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Kem Chocolate Hạnh Nhân', 'Tuyệt phẩm dành cho tín đồ chocolate. Cốt bánh chocolate đậm vị, kem chocolate ganache 70% cacao phủ ngoài lớp vụn hạnh nhân rang giòn thơm lừng.', 'BEST_SELLER', 'Bột mì, trứng, đường, bơ, bột cacao nguyên chất, chocolate 70%, kem tươi whipping, hạnh nhân lát mỏng.', 'Bảo quản ở nhiệt độ 2-5 độ C. Sử dụng tốt nhất trong vòng 48 giờ.', 'Chứa gluten, trứng, bơ sữa, các loại hạt (hạnh nhân).', 520000.00, 4, 1, 'STOPPED', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Kem & Bánh Tiệc'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Kem Chocolate Hạnh Nhân');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/66542751-7467-4a8c-b114-4043432138aa-chocolate.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Kem Chocolate Hạnh Nhân'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Bánh Kem Matcha Đậu Đỏ
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Kem Matcha Đậu Đỏ', 'Sự kết hợp tinh tế giữa cốt bánh matcha Nhật Bản xốp mềm, lớp kem tươi trà xanh thanh mát và nhân đậu đỏ nghiền ngọt dịu chuẩn vị Á Đông.', 'NEW', 'Bột mì, bơ lạt, trứng gà, đường, bột matcha Uji nguyên chất, kem tươi whipping, đậu đỏ Azuki, sữa tươi.', 'Bảo quản ngăn mát tủ lạnh (2-5 độ C). Sử dụng tốt nhất trong 2 ngày.', 'Chứa gluten, trứng và bơ sữa.', 420000.00, 4, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Kem & Bánh Tiệc'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Kem Matcha Đậu Đỏ');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/096522bc-bfef-489d-a526-717d7d9ce635-matcha.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Kem Matcha Đậu Đỏ'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Pain au Chocolat
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Pain au Chocolat', 'Bánh mì ngàn lớp nhân sô cô la đậm đà. Vỏ bánh giòn rụm quyện cùng hai thanh sô cô la đen đắng nhẹ tan chảy bên trong.', 'BEST_SELLER', 'Bột mì cao cấp, bơ lạt Pháp, men, đường, muối, sữa, sô cô la đen 55%.', 'Bảo quản nhiệt độ phòng trong 2 ngày. Ngon hơn khi làm nóng lại ở 160 độ C trong 2-3 phút.', 'Chứa gluten, bơ sữa, trứng và đậu nành (từ sô cô la).', 48000.00, 25, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Mì & Viennoiserie'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Pain au Chocolat');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/e9e89e9f-61e6-42b2-991a-13dc4c680b67-pain.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Pain au Chocolat'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Tart Chanh Vàng Meringue
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Tart Chanh Vàng Meringue', 'Đế tart bơ giòn rụm ôm trọn lớp kem chanh vàng chua thanh, phủ bên trên là chóp kem Meringue đánh bông bồng bềnh và được khò lửa xém vàng đẹp mắt.', 'SEASONAL', 'Bột mì, bơ lạt, đường, trứng, cốt chanh vàng (lemon), vỏ chanh bào, gelatin.', 'Giữ lạnh ở 2-4 độ C. Thưởng thức trong ngày để vỏ bánh giữ được độ giòn.', 'Chứa gluten, trứng và bơ sữa.', 65000.00, 15, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Tart & Choux'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Tart Chanh Vàng Meringue');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/d2159a2d-ff52-43df-a49b-fa3a9e7f3b86-tartchanh.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Tart Chanh Vàng Meringue'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Mousse Chocolate Mâm Xôi
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Mousse Chocolate Mâm Xôi', 'Bánh mousse sô cô la nguyên chất 70% đắng nhẹ, bao bọc lớp lõi thạch quả mâm xôi (raspberry) chua dịu, tạo nên sự bùng nổ vị giác hoàn hảo.', 'BEST_SELLER', 'Sô cô la đen 70%, whipping cream, quả mâm xôi tươi, đường, gelatin, sữa tươi, lòng đỏ trứng.', 'Bảo quản ngăn mát tủ lạnh. Không để ngoài nhiệt độ phòng quá lâu tránh chảy mousse.', 'Chứa bơ sữa, trứng và đậu nành.', 85000.00, 10, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Mousse & Dessert Lạnh'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Mousse Chocolate Mâm Xôi');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/579d8fb0-c07d-4639-95e2-b5b9a59208c4-mousse.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Mousse Chocolate Mâm Xôi'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Cheesecake Phô Mai Cháy Basque
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Cheesecake Phô Mai Cháy Basque', 'Bánh phô mai nướng kiểu Tây Ban Nha với bề mặt xém nâu caramen đặc trưng, ruột bánh mềm mịn, béo ngậy và tan chảy ngay khi chạm lưỡi.', 'LIMITED', 'Creamcheese, kem tươi (whipping cream), trứng gà, đường, bột bắp, tinh chất vani.', 'Bảo quản trong ngăn mát tủ lạnh từ 3-5 ngày. Ăn lạnh sẽ ngon hơn.', 'Chứa lượng lớn bơ sữa (dairy) và trứng.', 380000.00, 6, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Kem & Bánh Tiệc'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Cheesecake Phô Mai Cháy Basque');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/63f218aa-7b8d-4318-8037-06e100aff8f7-basque.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Cheesecake Phô Mai Cháy Basque'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Bánh Mì Phô Mai Bơ Tỏi
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Mì Phô Mai Bơ Tỏi', 'Ổ bánh mì tròn khía múi cau, thấm đẫm sốt bơ tỏi lá thơm và nhồi đầy nhân phô mai kem chua ngọt béo ngậy. Nướng giòn rụm bên ngoài, mềm ẩm bên trong.', 'NEW', 'Bột mì, bơ lạt, tỏi tươi xay, ngò tây (parsley), creamcheese, đường, sữa đặc, trứng.', 'Sử dụng ngon nhất trong ngày. Có thể bảo quản tủ lạnh 2 ngày và nướng lại bằng nồi chiên không dầu trước khi ăn.', 'Chứa gluten, tỏi, bơ sữa và trứng.', 60000.00, 30, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Mì & Viennoiserie'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Mì Phô Mai Bơ Tỏi');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/4e3324a7-8e0c-485a-916e-3118f005a962-garlic.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Mì Phô Mai Bơ Tỏi'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Biscotti Hạt Dinh Dưỡng Nam Việt Quất
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Biscotti Hạt Dinh Dưỡng Nam Việt Quất', 'Bánh quy nướng hai lần kiểu Ý giòn rụm, ít ngọt, dồi dào dinh dưỡng nhờ sự kết hợp của hạt hạnh nhân, hạt bí xanh và mứt nam việt quất chua ngọt.', NULL, 'Bột mì nguyên cám, trứng, mật ong, hạt hạnh nhân, hạt bí, nam việt quất khô, tinh chất vani, bột nở.', 'Đựng trong hũ hoặc túi zip kín. Hạn sử dụng 1 tháng ở nhiệt độ phòng, nơi khô ráo.', 'Chứa gluten, trứng và các loại hạt (hạnh nhân, hạt bí).', 95000.00, 20, 0, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Cookies & Bánh Nướng'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Biscotti Hạt Dinh Dưỡng Nam Việt Quất');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/8b0e5786-73b3-4758-b017-3202bbd768e0-biscotti.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Biscotti Hạt Dinh Dưỡng Nam Việt Quất'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Hộp Thiếc Cookies Bơ Pháp
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Hộp Thiếc Cookies Bơ Pháp', 'Hộp thiếc vintage chứa đầy những chiếc bánh quy bơ Sablé truyền thống của Pháp. Bánh thơm lừng mùi bơ lạt thượng hạng, giòn xốp và tan trong miệng.', 'BEST_SELLER', 'Bột mì đa dụng, bơ lạt Pháp, đường bột, lòng đỏ trứng, muối biển.', 'Bảo quản ở nhiệt độ phòng trong hộp thiếc đóng kín. Hạn sử dụng 3 tuần.', 'Chứa gluten, bơ sữa và trứng.', 280000.00, 15, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Hộp Thiếc & Quà Tặng'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Hộp Thiếc Cookies Bơ Pháp');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/1d4dd969-2d33-4780-94c2-207923327c73-tin.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Hộp Thiếc Cookies Bơ Pháp'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);

-- Bánh Kem Red Velvet
INSERT INTO products (name, description, badge, ingredients, storage_instructions, allergen_info, price, stock_quantity, preparation_days, status, category_id, version, created_at, updated_at)
SELECT 'Bánh Kem Red Velvet', 'Cốt bánh nhung đỏ rực rỡ, ẩm mịn thoảng vị cacao, xen kẽ cùng các lớp kem phô mai (creamcheese frosting) chua chua ngọt ngọt đặc trưng đầy quyến rũ.', 'SEASONAL', 'Bột mì, đường, bơ, sữa tươi, giấm, bột cacao nguyên chất, màu thực phẩm đỏ an toàn, creamcheese, whipping cream.', 'Giữ lạnh 2-5 độ C. Bánh ngon nhất khi để nguội ở nhiệt độ phòng 10 phút trước khi cắt.', 'Chứa gluten, bơ sữa, trứng và màu thực phẩm đỏ.', 460000.00, 3, 1, 'SELLING', c.id, 0, NOW(), NOW()
FROM categories c WHERE c.name = 'Bánh Kem & Bánh Tiệc'
  AND NOT EXISTS (SELECT 1 FROM products e WHERE e.name = 'Bánh Kem Red Velvet');
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/2d84bffe-c7b2-4d5f-90b0-a69956730267-rv1.png', 0, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Kem Red Velvet'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 0);
INSERT INTO product_images (product_id, image_path, display_order, created_at, updated_at)
SELECT p.id, '/uploads/b0932cdb-b420-4ef8-a340-67d8e4b956b7-rv2.png', 1, NOW(), NOW() FROM products p WHERE p.name = 'Bánh Kem Red Velvet'
  AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.display_order = 1);

COMMIT;
