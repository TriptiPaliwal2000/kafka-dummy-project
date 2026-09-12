-- Seed catalog so the system is demoable immediately after `docker-compose up`.
-- version is explicit because the @Version column Hibernate generates is NOT NULL with no
-- default - this raw insert bypasses JPA (and its automatic version initialization) entirely.
MERGE INTO inventory_items (product_id, product_name, available_stock, unit_price, version) VALUES
  ('PRODUCT-1', 'Mechanical Keyboard', 50, 89.99, 0),
  ('PRODUCT-2', 'Wireless Mouse', 120, 29.99, 0),
  ('PRODUCT-3', '27" 4K Monitor', 15, 349.00, 0),
  ('PRODUCT-4', 'USB-C Dock', 40, 59.50, 0),
  ('PRODUCT-5', 'Standing Desk', 5, 299.00, 0);
