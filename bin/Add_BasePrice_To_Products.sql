-- ===============================================
-- Migration Script: Thêm cột BasePrice vào bảng Products
-- Phiên bản: SQL Server
-- ===============================================

-- 1️⃣ Thêm cột BasePrice (nếu chưa có)
IF COL_LENGTH('Products', 'BasePrice') IS NULL
BEGIN
    ALTER TABLE Products 
    ADD BasePrice DECIMAL(10, 2) DEFAULT 0.00;
END
GO

-- 2️⃣ Cập nhật BasePrice cho các sản phẩm hiện có
-- Lấy giá thấp nhất từ bảng ProductVariants
UPDATE p
SET p.BasePrice = pv.MinPrice
FROM Products p
INNER JOIN (
    SELECT ProductID, MIN(Price) AS MinPrice
    FROM ProductVariants
    GROUP BY ProductID
) pv ON p.ProductID = pv.ProductID;
GO

-- 3️⃣ Xử lý các sản phẩm không có variant
UPDATE Products
SET BasePrice = 0.00
WHERE BasePrice IS NULL;
GO

-- 4️⃣ Thêm index cho BasePrice (tối ưu truy vấn sắp xếp theo giá)
IF NOT EXISTS (
    SELECT * FROM sys.indexes WHERE name = 'IX_Products_BasePrice'
)
BEGIN
    CREATE INDEX IX_Products_BasePrice ON Products(BasePrice);
END
GO

-- 5️⃣ Tạo trigger tự động cập nhật BasePrice khi bảng ProductVariants thay đổi

-- Sau khi INSERT variant
CREATE OR ALTER TRIGGER trg_UpdateProductBasePrice_AfterInsert
ON ProductVariants
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Products
    SET BasePrice = (
        SELECT MIN(Price)
        FROM ProductVariants
        WHERE ProductID = i.ProductID
    )
    FROM inserted i
    WHERE Products.ProductID = i.ProductID;
END
GO

-- Sau khi UPDATE variant
CREATE OR ALTER TRIGGER trg_UpdateProductBasePrice_AfterUpdate
ON ProductVariants
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Products
    SET BasePrice = (
        SELECT MIN(Price)
        FROM ProductVariants
        WHERE ProductID = i.ProductID
    )
    FROM inserted i
    WHERE Products.ProductID = i.ProductID;
END
GO

-- Sau khi DELETE variant
CREATE OR ALTER TRIGGER trg_UpdateProductBasePrice_AfterDelete
ON ProductVariants
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Products
    SET BasePrice = (
        SELECT ISNULL(MIN(Price), 0)
        FROM ProductVariants
        WHERE ProductID = d.ProductID
    )
    FROM deleted d
    WHERE Products.ProductID = d.ProductID;
END
GO

-- 6️⃣ Kiểm tra kết quả
SELECT 
    p.ProductID,
    p.ProductName,
    p.BasePrice,
    MIN(pv.Price) AS MinVariantPrice,
    COUNT(pv.VariantID) AS VariantCount
FROM Products p
LEFT JOIN ProductVariants pv ON p.ProductID = pv.ProductID
GROUP BY p.ProductID, p.ProductName, p.BasePrice;
GO
