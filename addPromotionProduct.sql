USE MilkTeaShopDB;
GO

PRINT N'🚀 Đang kiểm tra và cập nhật Promotions & PromotionProducts...';
GO

/*===========================================================
  1️⃣ Thêm cột PromotionType vào bảng Promotions (nếu chưa có)
===========================================================*/
IF NOT EXISTS (
    SELECT * 
    FROM sys.columns 
    WHERE Name = N'PromotionType' 
      AND Object_ID = Object_ID(N'dbo.Promotions')
)
BEGIN
    ALTER TABLE dbo.Promotions
    ADD PromotionType NVARCHAR(20) NOT NULL 
        CONSTRAINT DF_Promotions_PromotionType DEFAULT 'ORDER';

    PRINT N'✅ Đã thêm cột PromotionType vào bảng Promotions.';
END
ELSE
    PRINT N'ℹ️ Cột PromotionType đã tồn tại, bỏ qua.';
GO

/*===========================================================
  2️⃣ Thêm CHECK constraint cho PromotionType (ORDER/PRODUCT)
===========================================================*/
IF NOT EXISTS (
    SELECT * 
    FROM sys.check_constraints 
    WHERE Name = N'CK_Promotions_PromotionType'
)
BEGIN
    ALTER TABLE dbo.Promotions
    ADD CONSTRAINT CK_Promotions_PromotionType 
    CHECK (PromotionType IN ('ORDER', 'PRODUCT'));

    PRINT N'✅ Đã thêm CHECK constraint CK_Promotions_PromotionType.';
END
ELSE
    PRINT N'ℹ️ Constraint CK_Promotions_PromotionType đã tồn tại, bỏ qua.';
GO

/*===========================================================
  3️⃣ Cho phép DiscountType và DiscountValue NULL
===========================================================*/
BEGIN TRY
    ALTER TABLE dbo.Promotions
    ALTER COLUMN DiscountType NVARCHAR(20) NULL;
    ALTER TABLE dbo.Promotions
    ALTER COLUMN DiscountValue DECIMAL(10,2) NULL;
    PRINT N'✅ Đã cập nhật DiscountType và DiscountValue cho phép NULL.';
END TRY
BEGIN CATCH
    PRINT N'⚠️ Không thể thay đổi NULL constraint (có thể đã được chỉnh trước).';
END CATCH
GO

/*===========================================================
  4️⃣ Thêm cột DiscountPercentage vào PromotionProducts (nếu chưa có)
===========================================================*/
IF NOT EXISTS (
    SELECT * 
    FROM sys.columns 
    WHERE Name = N'DiscountPercentage' 
      AND Object_ID = Object_ID(N'dbo.PromotionProducts')
)
BEGIN
    ALTER TABLE dbo.PromotionProducts
    ADD DiscountPercentage INT NOT NULL DEFAULT 0;

    ALTER TABLE dbo.PromotionProducts
    ADD CONSTRAINT CK_PromotionProducts_DiscountPercentage 
    CHECK (DiscountPercentage >= 0 AND DiscountPercentage <= 100);

    PRINT N'✅ Đã thêm cột DiscountPercentage vào bảng PromotionProducts.';
END
ELSE
    PRINT N'ℹ️ Cột DiscountPercentage đã tồn tại, bỏ qua.';
GO

PRINT N'🎯 Hoàn tất cập nhật Promotions & PromotionProducts.';
GO
