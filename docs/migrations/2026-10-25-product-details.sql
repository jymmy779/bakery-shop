-- Chạy đúng một lần trên database đã có bảng products.
-- Hibernate ddl-auto=update có thể tự tạo các cột khi ứng dụng khởi động;
-- nếu các cột đã tồn tại thì không chạy lại file migration này.
ALTER TABLE products
  ADD COLUMN badge VARCHAR(20) NULL,
  ADD COLUMN ingredients TEXT NULL,
  ADD COLUMN storage_instructions TEXT NULL,
  ADD COLUMN allergen_info TEXT NULL;
