USE MilkTeaShopDB
GO

-- ============================================
-- 0. DỌN DẸP TRƯỚC KHI TẠO LẠI (DROP EXISTING OBJECTS)
-- ============================================

IF OBJECT_ID ('trg_UpdateSoldCountOnOrderComplete', 'TR') IS NOT NULL DROP TRIGGER trg_UpdateSoldCountOnOrderComplete;
IF OBJECT_ID ('TR_Favorites_AfterInsert', 'TR') IS NOT NULL DROP TRIGGER TR_Favorites_AfterInsert;
IF OBJECT_ID ('TR_Favorites_AfterDelete', 'TR') IS NOT NULL DROP TRIGGER TR_Favorites_AfterDelete;
IF OBJECT_ID ('TR_ProductVariants_AfterInsert', 'TR') IS NOT NULL DROP TRIGGER TR_ProductVariants_AfterInsert;
IF OBJECT_ID ('TR_ProductVariants_AfterUpdate', 'TR') IS NOT NULL DROP TRIGGER TR_ProductVariants_AfterUpdate;
IF OBJECT_ID ('TR_ProductVariants_AfterDelete', 'TR') IS NOT NULL DROP TRIGGER TR_ProductVariants_AfterDelete;
IF OBJECT_ID ('TR_Reviews_AfterInsert', 'TR') IS NOT NULL DROP TRIGGER TR_Reviews_AfterInsert;
IF OBJECT_ID ('TR_Reviews_AfterUpdate', 'TR') IS NOT NULL DROP TRIGGER TR_Reviews_AfterUpdate;
IF OBJECT_ID ('TR_Reviews_AfterDelete', 'TR') IS NOT NULL DROP TRIGGER TR_Reviews_AfterDelete;
IF OBJECT_ID ('dbo.GetMinPriceForProduct', 'FN') IS NOT NULL DROP FUNCTION dbo.GetMinPriceForProduct;
IF OBJECT_ID ('dbo.CalculateProductReviewStats', 'IF') IS NOT NULL DROP FUNCTION dbo.CalculateProductReviewStats;
GO

-- ============================================
-- 1. HÀM TRỢ GIÚP (HELPER FUNCTIONS)
-- ============================================

-- Hàm: Lấy giá thấp nhất cho một ProductID (Dùng cho BasePrice)
CREATE FUNCTION dbo.GetMinPriceForProduct(@ProductID INT)
RETURNS DECIMAL(18, 2)
AS
BEGIN
    DECLARE @MinPrice DECIMAL(18, 2);
    SELECT @MinPrice = MIN(Price)
    FROM dbo.ProductVariants
    WHERE ProductID = @ProductID;
    RETURN ISNULL(@MinPrice, 0.0);
END;
GO

-- Hàm: Lấy điểm trung bình và tổng số đánh giá cho một ProductID (Dùng cho Reviews)
CREATE FUNCTION dbo.CalculateProductReviewStats(@ProductID INT)
RETURNS TABLE
AS
RETURN
(
    SELECT
        AVG(CAST(Rating AS DECIMAL(3, 2))) AS AvgRating,
        COUNT(ReviewID) AS TotalReviews
    FROM dbo.Reviews
    WHERE ProductID = @ProductID
);
GO

-- ============================================
-- 2. TRIGGERS CHO ĐƠN HÀNG (SoldCount)
-- ============================================

CREATE TRIGGER trg_UpdateSoldCountOnOrderComplete
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    -- Kiểm tra xem OrderStatus có bị thay đổi không
    IF UPDATE(OrderStatus)
    BEGIN
        
        -- Lấy các OrderID vừa được chuyển sang trạng thái 'Completed'
        -- (Đảm bảo nó không phải là 'Completed' trước đó)
        SELECT 
            i.OrderID,
            i.OrderStatus AS NewStatus,
            d.OrderStatus AS OldStatus
        INTO #CompletedOrders
        FROM inserted i
        JOIN deleted d ON i.OrderID = d.OrderID
        WHERE i.OrderStatus = 'Completed' AND d.OrderStatus <> 'Completed'; -- Đảm bảo chuyển trạng thái

        -- Nếu không có đơn hàng nào vừa hoàn thành, kết thúc
        IF NOT EXISTS (SELECT 1 FROM #CompletedOrders)
        BEGIN
            RETURN;
        END

        -- Cập nhật SoldCount
        UPDATE p
        SET 
            p.SoldCount = ISNULL(p.SoldCount, 0) + t.TotalQuantitySold
        FROM 
            dbo.Products p
        JOIN 
            ( -- Tính tổng số lượng bán cho các sản phẩm trong các Order vừa hoàn thành
                SELECT 
                    pv.ProductID, 
                    SUM(od.Quantity) AS TotalQuantitySold
                FROM dbo.OrderDetails od
                JOIN dbo.ProductVariants pv ON od.VariantID = pv.VariantID
                JOIN #CompletedOrders co ON od.OrderID = co.OrderID -- JOIN với đơn hàng vừa hoàn thành
                GROUP BY pv.ProductID
            ) AS t ON p.ProductID = t.ProductID;
    END
END
GO

-- ============================================
-- 3. TRIGGERS CHO YÊU THÍCH (TotalLikes)
-- ============================================

-- 1. TRIGGER KHI THÊM (THÍCH)
CREATE TRIGGER TR_Favorites_AfterInsert
ON dbo.Favorites
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET p.totalLikes = ISNULL(p.totalLikes, 0) + i.LikeCount
    FROM dbo.Products p
    INNER JOIN (
        SELECT ProductID, COUNT(*) AS LikeCount
        FROM inserted
        GROUP BY ProductID
    ) AS i ON p.ProductID = i.ProductID;
END;
GO

-- 2. TRIGGER KHI XÓA (BỎ THÍCH)
CREATE TRIGGER TR_Favorites_AfterDelete
ON dbo.Favorites
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET p.totalLikes = 
        CASE 
            WHEN (ISNULL(p.totalLikes, 0) - d.LikeCount) < 0 THEN 0 
            ELSE (ISNULL(p.totalLikes, 0) - d.LikeCount)
        END
    FROM dbo.Products p
    INNER JOIN (
        SELECT ProductID, COUNT(*) AS LikeCount
        FROM deleted
        GROUP BY ProductID
    ) AS d ON p.ProductID = d.ProductID;
END;
GO

-- ============================================
-- 4. TRIGGERS CHO PRODUCT VARIANTS (BasePrice)
-- ============================================

-- 1. Trigger khi THÊM (INSERT) ProductVariant
CREATE TRIGGER TR_ProductVariants_AfterInsert
ON dbo.ProductVariants
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET p.basePrice = dbo.GetMinPriceForProduct(i.ProductID)
    FROM dbo.Products p
    JOIN (SELECT DISTINCT ProductID FROM inserted) AS i ON p.ProductID = i.ProductID;
END;
GO

-- 2. Trigger khi SỬA (UPDATE) ProductVariant
CREATE TRIGGER TR_ProductVariants_AfterUpdate
ON dbo.ProductVariants
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Price) OR UPDATE(ProductID)
    BEGIN
        UPDATE p
        SET p.basePrice = dbo.GetMinPriceForProduct(t.ProductID)
        FROM dbo.Products p
        JOIN (SELECT DISTINCT ProductID FROM inserted UNION SELECT DISTINCT ProductID FROM deleted) AS t ON p.ProductID = t.ProductID;
    END;
END;
GO

-- 3. Trigger khi XÓA (DELETE) ProductVariant
CREATE TRIGGER TR_ProductVariants_AfterDelete
ON dbo.ProductVariants
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET p.basePrice = dbo.GetMinPriceForProduct(d.ProductID)
    FROM dbo.Products p
    JOIN (SELECT DISTINCT ProductID FROM deleted) AS d ON p.ProductID = d.ProductID;
END;
GO

-- ============================================
-- 5. TRIGGERS CHO ĐÁNH GIÁ (AverageRating, TotalReviews)
-- ============================================

-- 1. Trigger khi THÊM (INSERT) Review
CREATE TRIGGER TR_Reviews_AfterInsert
ON dbo.Reviews
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE p
    SET 
        p.AverageRating = stats.AvgRating,
        p.TotalReviews = stats.TotalReviews
    FROM dbo.Products p
    INNER JOIN (SELECT DISTINCT ProductID FROM inserted) AS i ON p.ProductID = i.ProductID
    CROSS APPLY dbo.CalculateProductReviewStats(p.ProductID) AS stats;
END;
GO

-- 2. Trigger khi SỬA (UPDATE) Review
CREATE TRIGGER TR_Reviews_AfterUpdate
ON dbo.Reviews
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(Rating) OR UPDATE(ProductID)
    BEGIN
        UPDATE p
        SET
            p.AverageRating = stats.AvgRating,
            p.TotalReviews = stats.TotalReviews
        FROM dbo.Products p
        INNER JOIN (
            SELECT DISTINCT ProductID FROM inserted
            UNION
            SELECT DISTINCT ProductID FROM deleted
        ) AS t ON p.ProductID = t.ProductID
        CROSS APPLY dbo.CalculateProductReviewStats(p.ProductID) AS stats;
    END
END;
GO

-- 3. Trigger khi XÓA (DELETE) Review
CREATE TRIGGER TR_Reviews_AfterDelete
ON dbo.Reviews
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET 
        p.AverageRating = ISNULL(stats.AvgRating, 0.00),
        p.TotalReviews = ISNULL(stats.TotalReviews, 0)
    FROM dbo.Products p
    INNER JOIN (SELECT DISTINCT ProductID FROM deleted) AS d ON p.ProductID = d.ProductID
    CROSS APPLY dbo.CalculateProductReviewStats(p.ProductID) AS stats;
END;
GO

-- ============================================
-- 6. CẬP NHẬT DỮ LIỆU HIỆN CÓ (CHỈ CHẠY MỘT LẦN)
-- ============================================

-- Cập nhật BasePrice cho các sản phẩm hiện có
UPDATE Products
SET basePrice = dbo.GetMinPriceForProduct(ProductID)

-- Cập nhật Rating Stats cho các sản phẩm hiện có
UPDATE p
SET 
    p.AverageRating = stats.AvgRating,
    p.TotalReviews = stats.TotalReviews
FROM dbo.Products p
CROSS APPLY dbo.CalculateProductReviewStats(p.ProductID) AS stats;

GO