USE MilkTeaShopDB;
GO

PRINT N'Updating Toppings table for multi-vendor support...';
GO

-- 1. Thêm cột ShopID vào bảng Toppings
ALTER TABLE dbo.Toppings
ADD ShopID INT NULL; -- Cho phép NULL nếu Admin cũng có thể tạo topping chung

-- 2. Tạo Foreign Key liên kết Toppings với Shops
ALTER TABLE dbo.Toppings
ADD CONSTRAINT FK_Topping_Shop FOREIGN KEY (ShopID)
    REFERENCES dbo.Shops(ShopID)
    ON DELETE CASCADE; -- Tự động xóa topping nếu shop bị xóa

-- 3. Cập nhật lại Unique Constraint (quan trọng)
-- Lưu ý: Tên constraint 'UQ__Toppings__...' của bạn có thể khác.
-- Hãy kiểm tra tên đúng trong SSMS (trong thư mục Keys của bảng Toppings) và thay thế vào lệnh DROP.
BEGIN TRY
    ALTER TABLE dbo.Toppings
    DROP CONSTRAINT UQ__Toppings__4D4E3C02B94A9A18; -- <-- THAY TÊN NÀY NẾU CẦN
END TRY
BEGIN CATCH
    PRINT N'Could not drop old unique constraint (maybe name is different or does not exist).';
END CATCH
GO

-- Thêm constraint mới: Tên topping phải là duy nhất TRONG CÙNG MỘT SHOP (hoặc là NULL nếu admin tạo)
ALTER TABLE dbo.Toppings
ADD CONSTRAINT UQ_Topping_Shop_Name UNIQUE (ShopID, ToppingName);
GO

PRINT N'✓ Toppings table updated.';
GO