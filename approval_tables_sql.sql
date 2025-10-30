/*
-- ================================================================
-- SCRIPT: Approval Tables for Product and Promotion Management
-- Purpose: Add approval workflow for Vendor actions
-- ================================================================

USE MilkTeaShopDB;
GO

-- ================================================================
-- CREATE APPROVAL TABLES
-- ================================================================

-- Product Approval Table
IF OBJECT_ID('dbo.ProductApprovals','U') IS NOT NULL 
    DROP TABLE dbo.ProductApprovals;
GO

CREATE TABLE dbo.ProductApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NULL, -- NULL for new product creation until approved
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL, -- JSON string with change details
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_ProductApproval_Product FOREIGN KEY (ProductID) 
        REFERENCES dbo.Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT FK_ProductApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ProductApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

-- Promotion Approval Table
IF OBJECT_ID('dbo.PromotionApprovals','U') IS NOT NULL 
    DROP TABLE dbo.PromotionApprovals;
GO

CREATE TABLE dbo.PromotionApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    PromotionID INT NULL, -- NULL for new promotion creation until approved
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL,
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_PromotionApproval_Promotion FOREIGN KEY (PromotionID) 
        REFERENCES dbo.Promotions(PromotionID) ON DELETE CASCADE,
    CONSTRAINT FK_PromotionApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_PromotionApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

-- (Thêm vào cuối file)

-- ================================================================
-- TOPPING APPROVAL TABLE
-- ================================================================

IF OBJECT_ID('dbo.ToppingApprovals','U') IS NOT NULL 
    DROP TABLE dbo.ToppingApprovals;
GO

CREATE TABLE dbo.ToppingApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    ToppingID INT NULL,
    ShopID INT NOT NULL, -- Shop nào đang yêu cầu
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL, -- JSON
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_ToppingApproval_Topping FOREIGN KEY (ToppingID) 
        REFERENCES dbo.Toppings(ToppingID) ON DELETE CASCADE,
    CONSTRAINT FK_ToppingApproval_Shop FOREIGN KEY (ShopID) 
        REFERENCES dbo.Shops(ShopID), -- Không cascade
    CONSTRAINT FK_ToppingApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ToppingApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_ToppingApprovals_ToppingID ON dbo.ToppingApprovals(ToppingID);
CREATE INDEX IX_ToppingApprovals_ShopID ON dbo.ToppingApprovals(ShopID);
CREATE INDEX IX_ToppingApprovals_Status ON dbo.ToppingApprovals(Status);
GO

-- ================================================================
-- TOPPING APPROVAL PROCEDURES
-- ================================================================

-- Procedure to approve topping changes
CREATE OR ALTER PROCEDURE sp_ApproveToppingChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @ToppingID INT;
        DECLARE @ShopID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX);
        
        SELECT 
            @ActionType = ActionType,
            @ToppingID = ToppingID,
            @ShopID = ShopID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ToppingApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;

        IF @ActionType = 'CREATE'
        BEGIN
            -- Logic: Topping đã được tạo với Status = 0
            UPDATE dbo.Toppings
            SET Status = 1 -- Kích hoạt nó
            WHERE ToppingID = @ToppingID;
        END
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            -- Đọc JSON và cập nhật
            UPDATE dbo.Toppings
            SET 
                ToppingName = JSON_VALUE(@ChangeDetails, '$.toppingName'),
                AdditionalPrice = JSON_VALUE(@ChangeDetails, '$.additionalPrice'),
                ImageURL = JSON_VALUE(@ChangeDetails, '$.imageURL'),
                Status = 1 -- Kích hoạt lại (nếu đang bị ẩn)
            WHERE ToppingID = @ToppingID;
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            -- Ẩn topping
            UPDATE dbo.Toppings
            SET Status = 0
            WHERE ToppingID = @ToppingID;
        END;
        
        UPDATE dbo.ToppingApprovals
        SET Status = 'Approved',
            ReviewedByUserID = @ReviewedByUserID,
            ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

-- Procedure to reject topping changes
CREATE OR ALTER PROCEDURE sp_RejectToppingChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.ToppingApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

-- Cập nhật View
CREATE OR ALTER VIEW dbo.v_PendingApprovals
AS
-- ... (Giữ nguyên 2 phần PRODUCT và PROMOTION) ...
UNION ALL

SELECT 
    'TOPPING' AS EntityType,
    pa.ApprovalID,
    pa.ToppingID AS EntityID,
    t.ToppingName AS EntityName,
    pa.ActionType,
    pa.Status,
    pa.RequestedAt,
    u.FullName AS RequestedByName,
    s.ShopName
FROM dbo.ToppingApprovals pa
JOIN dbo.Users u ON pa.RequestedByUserID = u.UserID
JOIN dbo.Shops s ON pa.ShopID = s.ShopID
LEFT JOIN dbo.Toppings t ON pa.ToppingID = t.ToppingID
WHERE pa.Status = 'Pending';
GO

-- ================================================================
-- CREATE INDEXES
-- ================================================================

CREATE INDEX IX_ProductApprovals_ProductID ON dbo.ProductApprovals(ProductID);
CREATE INDEX IX_ProductApprovals_Status ON dbo.ProductApprovals(Status);
CREATE INDEX IX_ProductApprovals_RequestedBy ON dbo.ProductApprovals(RequestedByUserID);
CREATE INDEX IX_ProductApprovals_RequestedAt ON dbo.ProductApprovals(RequestedAt DESC);

CREATE INDEX IX_PromotionApprovals_PromotionID ON dbo.PromotionApprovals(PromotionID);
CREATE INDEX IX_PromotionApprovals_Status ON dbo.PromotionApprovals(Status);
CREATE INDEX IX_PromotionApprovals_RequestedBy ON dbo.PromotionApprovals(RequestedByUserID);
CREATE INDEX IX_PromotionApprovals_RequestedAt ON dbo.PromotionApprovals(RequestedAt DESC);

GO

-- ================================================================
-- CREATE STORED PROCEDURES FOR ADMIN APPROVAL
-- ================================================================

CREATE OR ALTER PROCEDURE sp_ApproveProductChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @ProductID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX);

        -- Lấy thông tin approval đang ở trạng thái Pending
        SELECT
            @ActionType = ActionType,
            @ProductID = ProductID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ProductApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';

        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        -- ============================
        -- HANDLE CREATE
        -- ============================
        IF @ActionType = 'CREATE'
        BEGIN
            -- Kích hoạt product (giả sử khi vendor tạo product thì product đã được insert với Status = 0)
            UPDATE dbo.Products
            SET Status = 1, UpdatedAt = SYSUTCDATETIME()
            WHERE ProductID = @ProductID;

            PRINT N'Approved CREATE for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- Đồng bộ Toppings (nếu có)
            DECLARE @NewToppingIDs_C TABLE (ToppingID INT NOT NULL);
            INSERT INTO @NewToppingIDs_C (ToppingID)
            SELECT CAST(value AS INT)
            FROM OPENJSON(@ChangeDetails, '$.availableToppingIds');

            MERGE INTO dbo.ProductAvailableToppings AS Target
            USING @NewToppingIDs_C AS Source
            ON (Target.ProductID = @ProductID AND Target.ToppingID = Source.ToppingID)
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, ToppingID) VALUES (@ProductID, Source.ToppingID);

            -- Đồng bộ Promotions (hỗ trợ cả int array hoặc object array có discount)
            DECLARE @NewPromotions_C TABLE (PromotionID INT NOT NULL, DiscountPercentage DECIMAL(10,2) NOT NULL);
            INSERT INTO @NewPromotions_C (PromotionID, DiscountPercentage)
            SELECT
                COALESCE(TRY_CAST(JSON_VALUE([value], '$.promotionId') AS INT), TRY_CAST([value] AS INT)) AS PromotionID,
                COALESCE(TRY_CAST(JSON_VALUE([value], '$.discountPercentage') AS DECIMAL(10,2)), 0.00) AS DiscountPercentage
            FROM OPENJSON(@ChangeDetails, '$.promotionIds');

            MERGE INTO dbo.PromotionProducts AS Target
            USING @NewPromotions_C AS Source
            ON (Target.ProductID = @ProductID AND Target.PromotionID = Source.PromotionID)
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, PromotionID, DiscountPercentage)
                VALUES (@ProductID, Source.PromotionID, Source.DiscountPercentage);

            -- Nếu cần: cập nhật các bảng khác do CREATE (nếu DTO chứa variants/images ngay từ đầu)
            -- Đồng bộ images nếu DTO cung cấp newImageUrls vào ChangeDetails
            DECLARE @NewImageUrlsJson_C NVARCHAR(MAX) = JSON_QUERY(@ChangeDetails, '$.newImageUrls');
            IF @NewImageUrlsJson_C IS NOT NULL AND ISJSON(@NewImageUrlsJson_C) = 1 AND LEFT(LTRIM(@NewImageUrlsJson_C), 1) = N'['
            BEGIN
                DECLARE @NewImages_C TABLE (ImageURL NVARCHAR(500), JsonIndex INT);
                INSERT INTO @NewImages_C (ImageURL, JsonIndex)
                SELECT [value], TRY_CAST([key] AS INT)
                FROM OPENJSON(@NewImageUrlsJson_C);

                DELETE FROM dbo.ProductImages WHERE ProductID = @ProductID;

                DECLARE @PrimaryIndex_C INT = ISNULL(TRY_CAST(JSON_VALUE(@ChangeDetails, '$.primaryImageIndex') AS INT), 0);

                INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder)
                SELECT @ProductID, ImageURL, CASE WHEN JsonIndex = @PrimaryIndex_C THEN 1 ELSE 0 END, JsonIndex
                FROM @NewImages_C;
            END

        END
        -- ============================
        -- HANDLE UPDATE
        -- ============================
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            -- 1. Update basic product info (safely; cast JSON values)
            UPDATE p
            SET
                p.ProductName = JSON_VALUE(@ChangeDetails, '$.productName'),
                p.Description = JSON_VALUE(@ChangeDetails, '$.description'),
                p.CategoryID = TRY_CAST(JSON_VALUE(@ChangeDetails, '$.categoryId') AS INT),
                p.UpdatedAt = SYSUTCDATETIME()
            FROM dbo.Products p
            WHERE p.ProductID = @ProductID;

            PRINT N'Updated basic info for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- 2. Synchronize Variants
            DECLARE @NewVariants TABLE (
                VariantID INT NULL,
                SizeID INT NOT NULL,
                Price DECIMAL(10,2) NOT NULL,
                Stock INT NOT NULL,
                SKU NVARCHAR(50) NULL
            );

            INSERT INTO @NewVariants (VariantID, SizeID, Price, Stock, SKU)
            SELECT
                TRY_CAST(JSON_VALUE(v.value, '$.variantId') AS INT),
                TRY_CAST(JSON_VALUE(v.value, '$.sizeId') AS INT),
                TRY_CAST(JSON_VALUE(v.value, '$.price') AS DECIMAL(10,2)),
                TRY_CAST(JSON_VALUE(v.value, '$.stock') AS INT),
                JSON_VALUE(v.value, '$.sku')
            FROM OPENJSON(@ChangeDetails, '$.variants') AS v;

            MERGE INTO dbo.ProductVariants AS Target
            USING @NewVariants AS Source
            ON (Target.ProductID = @ProductID AND Target.VariantID = Source.VariantID)
            WHEN MATCHED THEN
                UPDATE SET Target.SizeID = Source.SizeID, Target.Price = Source.Price, Target.Stock = Source.Stock, Target.SKU = Source.SKU
            WHEN NOT MATCHED BY TARGET AND Source.VariantID IS NULL THEN
                INSERT (ProductID, SizeID, Price, Stock, SKU) VALUES (@ProductID, Source.SizeID, Source.Price, Source.Stock, Source.SKU)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN
                DELETE;

            PRINT N'Synchronized variants for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- 3. Image Update Logic
            DECLARE @NewImageUrlsJson NVARCHAR(MAX) = JSON_QUERY(@ChangeDetails, '$.newImageUrls');

            IF @NewImageUrlsJson IS NOT NULL AND ISJSON(@NewImageUrlsJson) = 1 AND LEFT(LTRIM(@NewImageUrlsJson),1) = N'['
            BEGIN
                PRINT N'Processing new images for ProductID: ' + CAST(@ProductID AS NVARCHAR);

                DECLARE @NewImages TABLE ( ImageURL NVARCHAR(500), JsonIndex INT );
                INSERT INTO @NewImages (ImageURL, JsonIndex)
                SELECT [value], TRY_CAST([key] AS INT)
                FROM OPENJSON(@NewImageUrlsJson);

                -- Xóa ảnh cũ
                DELETE FROM dbo.ProductImages WHERE ProductID = @ProductID;
                PRINT N'Deleted old images for ProductID: ' + CAST(@ProductID AS NVARCHAR);

                DECLARE @PrimaryIndex INT = ISNULL(TRY_CAST(JSON_VALUE(@ChangeDetails, '$.primaryImageIndex') AS INT), 0);

                INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder)
                SELECT @ProductID, ni.ImageURL, CASE WHEN ni.JsonIndex = @PrimaryIndex THEN 1 ELSE 0 END, ni.JsonIndex
                FROM @NewImages ni;

                PRINT N'Inserted new images for ProductID: ' + CAST(@ProductID AS NVARCHAR) + N'. Primary index: ' + CAST(@PrimaryIndex AS NVARCHAR);
            END
            ELSE
            BEGIN
                PRINT N'No valid new images array found in ChangeDetails for ProductID: ' + CAST(@ProductID AS NVARCHAR);
            END

            -- 4. Đồng bộ Toppings
            DECLARE @NewToppingIDs TABLE ( ToppingID INT NOT NULL );
            INSERT INTO @NewToppingIDs (ToppingID)
            SELECT TRY_CAST([value] AS INT)
            FROM OPENJSON(@ChangeDetails, '$.availableToppingIds');

            MERGE INTO dbo.ProductAvailableToppings AS Target
            USING @NewToppingIDs AS Source
            ON (Target.ProductID = @ProductID AND Target.ToppingID = Source.ToppingID)
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, ToppingID) VALUES (@ProductID, Source.ToppingID)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN
                DELETE;

            PRINT N'Synchronized toppings for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- 5. Đồng bộ Promotions (hỗ trợ cả dạng số và object có discount)
            DECLARE @NewPromotions TABLE ( PromotionID INT NOT NULL, DiscountPercentage DECIMAL(10,2) NOT NULL );

            INSERT INTO @NewPromotions (PromotionID, DiscountPercentage)
            SELECT
                COALESCE(TRY_CAST(JSON_VALUE([value], '$.promotionId') AS INT), TRY_CAST([value] AS INT)) AS PromotionID,
                COALESCE(TRY_CAST(JSON_VALUE([value], '$.discountPercentage') AS DECIMAL(10,2)), 0.00) AS DiscountPercentage
            FROM OPENJSON(@ChangeDetails, '$.promotionIds');

            MERGE INTO dbo.PromotionProducts AS Target
            USING @NewPromotions AS Source
            ON (Target.ProductID = @ProductID AND Target.PromotionID = Source.PromotionID)
            WHEN MATCHED THEN
                -- Nếu cần cập nhật discount nếu đã khác
                UPDATE SET Target.DiscountPercentage = Source.DiscountPercentage
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, PromotionID, DiscountPercentage)
                VALUES (@ProductID, Source.PromotionID, Source.DiscountPercentage)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN
                DELETE;

            PRINT N'Synchronized promotions for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- Sau khi đồng bộ xong bật trạng thái Product (nếu cần)
            UPDATE dbo.Products SET Status = 1, UpdatedAt = SYSUTCDATETIME() WHERE ProductID = @ProductID;
        END
        -- ============================
        -- HANDLE DELETE
        -- ============================
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            UPDATE dbo.Products SET Status = 0, UpdatedAt = SYSUTCDATETIME() WHERE ProductID = @ProductID;
            PRINT N'Approved DELETE for ProductID: ' + CAST(@ProductID AS NVARCHAR);
        END

        -- Cập nhật approval record
        UPDATE dbo.ProductApprovals
        SET Status = 'Approved', ReviewedByUserID = @ReviewedByUserID, ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;

        COMMIT TRANSACTION;
        PRINT N'ApprovalID ' + CAST(@ApprovalID AS NVARCHAR) + N' processed successfully.';

    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrNum INT = ERROR_NUMBER();

        RAISERROR('Error processing ApprovalID %d: %s', 16, 1, @ApprovalID, @ErrMsg);
        THROW;
    END CATCH;
END;
GO

-- Procedure to reject product changes
CREATE OR ALTER PROCEDURE sp_RejectProductChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.ProductApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

-- Procedure to approve promotion changes
USE MilkTeaShopDB;
GO

CREATE OR ALTER PROCEDURE sp_ApprovePromotionChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @PromotionID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX); -- Variable to store JSON
        
        -- Get approval details including ChangeDetails
        SELECT 
            @ActionType = ActionType,
            @PromotionID = PromotionID,
            @ChangeDetails = ChangeDetails -- Fetch the JSON data
        FROM dbo.PromotionApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;
        
        -- Apply changes
        IF @ActionType = 'CREATE'
        BEGIN
            -- Activate the promotion (VendorService created it with Status=0)
            UPDATE dbo.Promotions
            SET Status = 1
            WHERE PromotionID = @PromotionID;
            
            PRINT N'Approved CREATE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR);
        END
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            -- Parse JSON and update the promotion details
            UPDATE dbo.Promotions
            SET 
                PromotionName = JSON_VALUE(@ChangeDetails, '$.promotionName'),
                Description = JSON_VALUE(@ChangeDetails, '$.description'),
                PromoCode = JSON_VALUE(@ChangeDetails, '$.promoCode'),
                DiscountType = JSON_VALUE(@ChangeDetails, '$.discountType'),
                DiscountValue = JSON_VALUE(@ChangeDetails, '$.discountValue'),
                MaxDiscountAmount = JSON_VALUE(@ChangeDetails, '$.maxDiscountAmount'),
                StartDate = CONVERT(DATETIME2, JSON_VALUE(@ChangeDetails, '$.startDate')), -- Convert string to datetime2
                EndDate = CONVERT(DATETIME2, JSON_VALUE(@ChangeDetails, '$.endDate')),     -- Convert string to datetime2
                MinOrderValue = JSON_VALUE(@ChangeDetails, '$.minOrderValue'),
                UsageLimit = JSON_VALUE(@ChangeDetails, '$.usageLimit'),
                Status = 1 -- Ensure it's active after update approval
            WHERE PromotionID = @PromotionID;

            PRINT N'Approved UPDATE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR) + N' using ChangeDetails.';
            
            -- Note: This SP doesn't handle PromotionProducts linking. 
            -- If your PromotionRequestDTO contains product IDs, 
            -- additional logic (MERGE statement) would be needed here 
            -- to update the dbo.PromotionProducts table.
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            -- Soft delete the promotion
            UPDATE dbo.Promotions
            SET Status = 0
            WHERE PromotionID = @PromotionID;
            
            PRINT N'Approved DELETE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR);
        END;
        
        -- Update approval status regardless of ActionType
        UPDATE dbo.PromotionApprovals
        SET Status = 'Approved',
            ReviewedByUserID = @ReviewedByUserID,
            ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;
        
        COMMIT TRANSACTION;
        PRINT N'ApprovalID ' + CAST(@ApprovalID AS NVARCHAR) + N' processed successfully.';
        
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        PRINT N'Error processing ApprovalID ' + CAST(@ApprovalID AS NVARCHAR);
        THROW; -- Re-throw the error
    END CATCH;
END;
GO

PRINT N'✓ Procedure sp_ApprovePromotionChange updated successfully.';
GO

-- Procedure to reject promotion changes
CREATE OR ALTER PROCEDURE sp_RejectPromotionChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.PromotionApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

-- ================================================================
-- CREATE VIEWS FOR ADMIN
-- ================================================================

-- View for all pending approvals
CREATE OR ALTER VIEW dbo.v_PendingApprovals
AS
SELECT 
    'PRODUCT' AS EntityType,
    pa.ApprovalID,
    pa.ProductID AS EntityID,
    p.ProductName AS EntityName,
    pa.ActionType,
    pa.Status,
    pa.RequestedAt,
    u.FullName AS RequestedByName,
    s.ShopName
FROM dbo.ProductApprovals pa
JOIN dbo.Users u ON pa.RequestedByUserID = u.UserID
LEFT JOIN dbo.Products p ON pa.ProductID = p.ProductID
LEFT JOIN dbo.Shops s ON p.ShopID = s.ShopID
WHERE pa.Status = 'Pending'

UNION ALL

SELECT 
    'PROMOTION' AS EntityType,
    pa.ApprovalID,
    pa.PromotionID AS EntityID,
    pr.PromotionName AS EntityName,
    pa.ActionType,
    pa.Status,
    pa.RequestedAt,
    u.FullName AS RequestedByName,
    s.ShopName
FROM dbo.PromotionApprovals pa
JOIN dbo.Users u ON pa.RequestedByUserID = u.UserID
LEFT JOIN dbo.Promotions pr ON pa.PromotionID = pr.PromotionID
LEFT JOIN dbo.Shops s ON pr.CreatedByShopID = s.ShopID
WHERE pa.Status = 'Pending';
GO

PRINT N'✓ Approval tables and procedures created successfully.';
GO
*/

-- ================================================================
-- SCRIPT: Updated Approval Tables for Product and Promotion Management
-- Purpose: Product discount now managed at product level, not promotion level
-- ================================================================

USE MilkTeaShopDB_1;
GO

-- ================================================================
-- CREATE/UPDATE APPROVAL TABLES (Giữ nguyên cấu trúc)
-- ================================================================

-- Product Approval Table
IF OBJECT_ID('dbo.ProductApprovals','U') IS NOT NULL 
    DROP TABLE dbo.ProductApprovals;
GO

CREATE TABLE dbo.ProductApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NULL,
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL,
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_ProductApproval_Product FOREIGN KEY (ProductID) 
        REFERENCES dbo.Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT FK_ProductApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ProductApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_ProductApprovals_ProductID ON dbo.ProductApprovals(ProductID);
CREATE INDEX IX_ProductApprovals_Status ON dbo.ProductApprovals(Status);
CREATE INDEX IX_ProductApprovals_RequestedBy ON dbo.ProductApprovals(RequestedByUserID);
CREATE INDEX IX_ProductApprovals_RequestedAt ON dbo.ProductApprovals(RequestedAt DESC);
GO

-- Promotion Approval Table
IF OBJECT_ID('dbo.PromotionApprovals','U') IS NOT NULL 
    DROP TABLE dbo.PromotionApprovals;
GO

CREATE TABLE dbo.PromotionApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    PromotionID INT NULL,
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL,
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_PromotionApproval_Promotion FOREIGN KEY (PromotionID) 
        REFERENCES dbo.Promotions(PromotionID) ON DELETE CASCADE,
    CONSTRAINT FK_PromotionApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_PromotionApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_PromotionApprovals_PromotionID ON dbo.PromotionApprovals(PromotionID);
CREATE INDEX IX_PromotionApprovals_Status ON dbo.PromotionApprovals(Status);
CREATE INDEX IX_PromotionApprovals_RequestedBy ON dbo.PromotionApprovals(RequestedByUserID);
CREATE INDEX IX_PromotionApprovals_RequestedAt ON dbo.PromotionApprovals(RequestedAt DESC);
GO

-- Topping Approval Table
IF OBJECT_ID('dbo.ToppingApprovals','U') IS NOT NULL 
    DROP TABLE dbo.ToppingApprovals;
GO

CREATE TABLE dbo.ToppingApprovals (
    ApprovalID INT IDENTITY(1,1) PRIMARY KEY,
    ToppingID INT NULL,
    ShopID INT NOT NULL,
    ActionType NVARCHAR(20) NOT NULL CHECK (ActionType IN ('CREATE', 'UPDATE', 'DELETE')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    ChangeDetails NVARCHAR(MAX) NULL,
    RequestedByUserID INT NOT NULL,
    ReviewedByUserID INT NULL,
    RejectionReason NVARCHAR(500) NULL,
    RequestedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReviewedAt DATETIME2 NULL,
    
    CONSTRAINT FK_ToppingApproval_Topping FOREIGN KEY (ToppingID) 
        REFERENCES dbo.Toppings(ToppingID) ON DELETE CASCADE,
    CONSTRAINT FK_ToppingApproval_Shop FOREIGN KEY (ShopID) 
        REFERENCES dbo.Shops(ShopID),
    CONSTRAINT FK_ToppingApproval_RequestedBy FOREIGN KEY (RequestedByUserID) 
        REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ToppingApproval_ReviewedBy FOREIGN KEY (ReviewedByUserID) 
        REFERENCES dbo.Users(UserID)
);
GO

CREATE INDEX IX_ToppingApprovals_ToppingID ON dbo.ToppingApprovals(ToppingID);
CREATE INDEX IX_ToppingApprovals_ShopID ON dbo.ToppingApprovals(ShopID);
CREATE INDEX IX_ToppingApprovals_Status ON dbo.ToppingApprovals(Status);
GO

-- ================================================================
-- STORED PROCEDURE: APPROVE PRODUCT CHANGE (UPDATED)
-- ================================================================

CREATE OR ALTER PROCEDURE sp_ApproveProductChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @ProductID INT;
        DECLARE @ShopID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX);
        DECLARE @DiscountPercentage INT;

        -- Lấy thông tin approval
        SELECT
            @ActionType = ActionType,
            @ProductID = ProductID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ProductApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';

        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        -- *** ĐỌC % GIẢM GIÁ TỪ JSON ***
        SET @DiscountPercentage = TRY_CAST(JSON_VALUE(@ChangeDetails, '$.discountPercentage') AS INT);

        -- ============================
        -- HANDLE CREATE
        -- ============================
        IF @ActionType = 'CREATE'
        BEGIN
            -- Kích hoạt product
            UPDATE dbo.Products
            SET Status = 1, UpdatedAt = SYSUTCDATETIME()
            WHERE ProductID = @ProductID;

            -- Lấy ShopID từ Product
            SELECT @ShopID = ShopID FROM dbo.Products WHERE ProductID = @ProductID;

            PRINT N'Approved CREATE for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- *** MỚI: TẠO PROMOTION NỘI BỘ CHO PRODUCT NÀY ***
            IF @DiscountPercentage IS NOT NULL AND @DiscountPercentage > 0
            BEGIN
                DECLARE @NewPromotionID INT;
                DECLARE @RequestedByUserID INT;
                
                SELECT @RequestedByUserID = RequestedByUserID 
                FROM dbo.ProductApprovals 
                WHERE ApprovalID = @ApprovalID;

                -- Tạo Promotion với PromotionType = 'PRODUCT'
                INSERT INTO dbo.Promotions (
                    CreatedByUserID, 
                    CreatedByShopID,
                    PromotionName,
                    Description,
                    PromoCode,
                    PromotionType,
                    DiscountType,
                    DiscountValue,
                    StartDate,
                    EndDate,
                    MinOrderValue,
                    UsageLimit,
                    UsedCount,
                    Status,
                    CreatedAt
                )
                VALUES (
                    @RequestedByUserID,
                    @ShopID,
                    N'Giảm giá sản phẩm #' + CAST(@ProductID AS NVARCHAR),
                    N'Khuyến mãi nội bộ cho sản phẩm',
                    'PRODUCT_' + CAST(@ProductID AS NVARCHAR) + '_' + CAST(CAST(NEWID() AS BINARY(4)) AS NVARCHAR(8)),
                    'PRODUCT',
                    NULL,
                    NULL,
                    SYSUTCDATETIME(),
                    DATEADD(YEAR, 10, SYSUTCDATETIME()), -- Vô thời hạn
                    0,
                    0,
                    0,
                    1, -- Active ngay
                    SYSUTCDATETIME()
                );

                SET @NewPromotionID = SCOPE_IDENTITY();

                -- Tạo liên kết PromotionProduct
                INSERT INTO dbo.PromotionProducts (PromotionID, ProductID, DiscountPercentage)
                VALUES (@NewPromotionID, @ProductID, @DiscountPercentage);

                PRINT N'Created internal promotion ID: ' + CAST(@NewPromotionID AS NVARCHAR) + 
                      N' with ' + CAST(@DiscountPercentage AS NVARCHAR) + N'% discount';
            END

            -- Đồng bộ Toppings
            DECLARE @NewToppingIDs_C TABLE (ToppingID INT NOT NULL);
            INSERT INTO @NewToppingIDs_C (ToppingID)
            SELECT CAST(value AS INT)
            FROM OPENJSON(@ChangeDetails, '$.availableToppingIds');

            MERGE INTO dbo.ProductAvailableToppings AS Target
            USING @NewToppingIDs_C AS Source
            ON (Target.ProductID = @ProductID AND Target.ToppingID = Source.ToppingID)
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, ToppingID) VALUES (@ProductID, Source.ToppingID);

            -- *** BỎ LOGIC ĐỒNG BỘ promotionIds (không còn từ DTO) ***

            -- Đồng bộ images
            DECLARE @NewImageUrlsJson_C NVARCHAR(MAX) = JSON_QUERY(@ChangeDetails, '$.newImageUrls');
            IF @NewImageUrlsJson_C IS NOT NULL AND ISJSON(@NewImageUrlsJson_C) = 1
            BEGIN
                DECLARE @NewImages_C TABLE (ImageURL NVARCHAR(500), JsonIndex INT);
                INSERT INTO @NewImages_C (ImageURL, JsonIndex)
                SELECT [value], TRY_CAST([key] AS INT)
                FROM OPENJSON(@NewImageUrlsJson_C);

                DELETE FROM dbo.ProductImages WHERE ProductID = @ProductID;

                DECLARE @PrimaryIndex_C INT = ISNULL(TRY_CAST(JSON_VALUE(@ChangeDetails, '$.primaryImageIndex') AS INT), 0);

                INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder)
                SELECT @ProductID, ImageURL, 
                       CASE WHEN JsonIndex = @PrimaryIndex_C THEN 1 ELSE 0 END, 
                       JsonIndex
                FROM @NewImages_C;
            END
        END
        -- ============================
        -- HANDLE UPDATE
        -- ============================
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            -- 1. Update basic info
            UPDATE p
            SET
                p.ProductName = JSON_VALUE(@ChangeDetails, '$.productName'),
                p.Description = JSON_VALUE(@ChangeDetails, '$.description'),
                p.CategoryID = TRY_CAST(JSON_VALUE(@ChangeDetails, '$.categoryId') AS INT),
                p.UpdatedAt = SYSUTCDATETIME()
            FROM dbo.Products p
            WHERE p.ProductID = @ProductID;

            -- Lấy ShopID
            SELECT @ShopID = ShopID FROM dbo.Products WHERE ProductID = @ProductID;

            PRINT N'Updated basic info for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- *** MỚI: CẬP NHẬT HOẶC TẠO PROMOTION NỘI BỘ ***
            IF @DiscountPercentage IS NOT NULL AND @DiscountPercentage > 0
            BEGIN
                -- Tìm promotion nội bộ hiện tại
                DECLARE @ExistingPromotionID INT;
                
                SELECT TOP 1 @ExistingPromotionID = pp.PromotionID
                FROM dbo.PromotionProducts pp
                JOIN dbo.Promotions pr ON pp.PromotionID = pr.PromotionID
                WHERE pp.ProductID = @ProductID 
                  AND pr.PromotionType = 'PRODUCT'
                  AND pr.CreatedByShopID = @ShopID
                ORDER BY pr.CreatedAt DESC;

                IF @ExistingPromotionID IS NOT NULL
                BEGIN
                    -- Cập nhật % giảm giá
                    UPDATE dbo.PromotionProducts
                    SET DiscountPercentage = @DiscountPercentage
                    WHERE PromotionID = @ExistingPromotionID 
                      AND ProductID = @ProductID;

                    PRINT N'Updated discount to ' + CAST(@DiscountPercentage AS NVARCHAR) + N'% for promotion ID: ' + CAST(@ExistingPromotionID AS NVARCHAR);
                END
                ELSE
                BEGIN
                    -- Tạo mới promotion
                    DECLARE @RequestedByUserID_U INT;
                    SELECT @RequestedByUserID_U = RequestedByUserID 
                    FROM dbo.ProductApprovals 
                    WHERE ApprovalID = @ApprovalID;

                    INSERT INTO dbo.Promotions (
                        CreatedByUserID, CreatedByShopID, PromotionName, Description, PromoCode,
                        PromotionType, StartDate, EndDate, MinOrderValue, UsageLimit, UsedCount, Status, CreatedAt
                    )
                    VALUES (
                        @RequestedByUserID_U, @ShopID,
                        N'Giảm giá sản phẩm #' + CAST(@ProductID AS NVARCHAR),
                        N'Khuyến mãi nội bộ cho sản phẩm',
                        'PRODUCT_' + CAST(@ProductID AS NVARCHAR) + '_' + CAST(CAST(NEWID() AS BINARY(4)) AS NVARCHAR(8)),
                        'PRODUCT',
                        SYSUTCDATETIME(), DATEADD(YEAR, 10, SYSUTCDATETIME()),
                        0, 0, 0, 1, SYSUTCDATETIME()
                    );

                    SET @ExistingPromotionID = SCOPE_IDENTITY();

                    INSERT INTO dbo.PromotionProducts (PromotionID, ProductID, DiscountPercentage)
                    VALUES (@ExistingPromotionID, @ProductID, @DiscountPercentage);

                    PRINT N'Created new internal promotion ID: ' + CAST(@ExistingPromotionID AS NVARCHAR);
                END
            END
            ELSE IF @DiscountPercentage = 0 OR @DiscountPercentage IS NULL
            BEGIN
                -- Xóa promotion nội bộ nếu discount = 0
                DELETE pp
                FROM dbo.PromotionProducts pp
                JOIN dbo.Promotions pr ON pp.PromotionID = pr.PromotionID
                WHERE pp.ProductID = @ProductID 
                  AND pr.PromotionType = 'PRODUCT'
                  AND pr.CreatedByShopID = @ShopID;

                PRINT N'Removed internal promotion for ProductID: ' + CAST(@ProductID AS NVARCHAR);
            END

            -- 2. Synchronize Variants
            DECLARE @NewVariants TABLE (
                VariantID INT NULL,
                SizeID INT NOT NULL,
                Price DECIMAL(10,2) NOT NULL,
                Stock INT NOT NULL,
                SKU NVARCHAR(50) NULL
            );

            INSERT INTO @NewVariants (VariantID, SizeID, Price, Stock, SKU)
            SELECT
                TRY_CAST(JSON_VALUE(v.value, '$.variantId') AS INT),
                TRY_CAST(JSON_VALUE(v.value, '$.sizeId') AS INT),
                TRY_CAST(JSON_VALUE(v.value, '$.price') AS DECIMAL(10,2)),
                TRY_CAST(JSON_VALUE(v.value, '$.stock') AS INT),
                JSON_VALUE(v.value, '$.sku')
            FROM OPENJSON(@ChangeDetails, '$.variants') AS v;

            MERGE INTO dbo.ProductVariants AS Target
            USING @NewVariants AS Source
            ON (Target.ProductID = @ProductID AND Target.VariantID = Source.VariantID)
            WHEN MATCHED THEN
                UPDATE SET Target.SizeID = Source.SizeID, Target.Price = Source.Price, 
                           Target.Stock = Source.Stock, Target.SKU = Source.SKU
            WHEN NOT MATCHED BY TARGET AND Source.VariantID IS NULL THEN
                INSERT (ProductID, SizeID, Price, Stock, SKU) 
                VALUES (@ProductID, Source.SizeID, Source.Price, Source.Stock, Source.SKU)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN
                DELETE;

            -- 3. Image Update
            DECLARE @NewImageUrlsJson NVARCHAR(MAX) = JSON_QUERY(@ChangeDetails, '$.newImageUrls');
            IF @NewImageUrlsJson IS NOT NULL AND ISJSON(@NewImageUrlsJson) = 1
            BEGIN
                DECLARE @NewImages TABLE (ImageURL NVARCHAR(500), JsonIndex INT);
                INSERT INTO @NewImages (ImageURL, JsonIndex)
                SELECT [value], TRY_CAST([key] AS INT)
                FROM OPENJSON(@NewImageUrlsJson);

                DELETE FROM dbo.ProductImages WHERE ProductID = @ProductID;

                DECLARE @PrimaryIndex INT = ISNULL(TRY_CAST(JSON_VALUE(@ChangeDetails, '$.primaryImageIndex') AS INT), 0);

                INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder)
                SELECT @ProductID, ni.ImageURL, 
                       CASE WHEN ni.JsonIndex = @PrimaryIndex THEN 1 ELSE 0 END, 
                       ni.JsonIndex
                FROM @NewImages ni;
            END

            -- 4. Đồng bộ Toppings
            DECLARE @NewToppingIDs TABLE (ToppingID INT NOT NULL);
            INSERT INTO @NewToppingIDs (ToppingID)
            SELECT TRY_CAST([value] AS INT)
            FROM OPENJSON(@ChangeDetails, '$.availableToppingIds');

            MERGE INTO dbo.ProductAvailableToppings AS Target
            USING @NewToppingIDs AS Source
            ON (Target.ProductID = @ProductID AND Target.ToppingID = Source.ToppingID)
            WHEN NOT MATCHED BY TARGET THEN
                INSERT (ProductID, ToppingID) VALUES (@ProductID, Source.ToppingID)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN
                DELETE;

            -- *** BỎ LOGIC ĐỒNG BỘ promotionIds ***

            -- Activate product
            UPDATE dbo.Products SET Status = 1, UpdatedAt = SYSUTCDATETIME() WHERE ProductID = @ProductID;
        END
        -- ============================
        -- HANDLE DELETE
        -- ============================
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            UPDATE dbo.Products SET Status = 0, UpdatedAt = SYSUTCDATETIME() WHERE ProductID = @ProductID;
            
            -- *** XÓA PROMOTION NỘI BỘ (nếu có) ***
            SELECT @ShopID = ShopID FROM dbo.Products WHERE ProductID = @ProductID;
            
            DELETE pp
            FROM dbo.PromotionProducts pp
            JOIN dbo.Promotions pr ON pp.PromotionID = pr.PromotionID
            WHERE pp.ProductID = @ProductID 
              AND pr.PromotionType = 'PRODUCT'
              AND pr.CreatedByShopID = @ShopID;

            PRINT N'Approved DELETE for ProductID: ' + CAST(@ProductID AS NVARCHAR);
        END

        -- Cập nhật approval record
        UPDATE dbo.ProductApprovals
        SET Status = 'Approved', ReviewedByUserID = @ReviewedByUserID, ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;

        COMMIT TRANSACTION;
        PRINT N'ApprovalID ' + CAST(@ApprovalID AS NVARCHAR) + N' processed successfully.';

    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        RAISERROR('Error processing ApprovalID %d: %s', 16, 1, @ApprovalID, @ErrMsg);
        THROW;
    END CATCH;
END;
GO

-- ================================================================
-- STORED PROCEDURE: APPROVE PROMOTION CHANGE (SIMPLIFIED)
-- ================================================================

CREATE OR ALTER PROCEDURE sp_ApprovePromotionChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @PromotionID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX);
        DECLARE @PromotionType NVARCHAR(20);
        
        SELECT 
            @ActionType = ActionType,
            @PromotionID = PromotionID,
            @ChangeDetails = ChangeDetails
        FROM dbo.PromotionApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;
        
        -- Kiểm tra PromotionType (phải là ORDER)
        SELECT @PromotionType = PromotionType FROM dbo.Promotions WHERE PromotionID = @PromotionID;
        
        IF @PromotionType = 'PRODUCT'
        BEGIN
            RAISERROR('Cannot manually approve PRODUCT type promotions. They are managed automatically via product approval.', 16, 1);
            RETURN;
        END;
        
        IF @ActionType = 'CREATE'
        BEGIN
            UPDATE dbo.Promotions SET Status = 1 WHERE PromotionID = @PromotionID;
            PRINT N'Approved CREATE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR);
        END
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            UPDATE dbo.Promotions
            SET 
                PromotionName = JSON_VALUE(@ChangeDetails, '$.promotionName'),
                Description = JSON_VALUE(@ChangeDetails, '$.description'),
                PromoCode = JSON_VALUE(@ChangeDetails, '$.promoCode'),
                DiscountType = JSON_VALUE(@ChangeDetails, '$.discountType'),
                DiscountValue = JSON_VALUE(@ChangeDetails, '$.discountValue'),
                MaxDiscountAmount = JSON_VALUE(@ChangeDetails, '$.maxDiscountAmount'),
                StartDate = CONVERT(DATETIME2, JSON_VALUE(@ChangeDetails, '$.startDate')),
                EndDate = CONVERT(DATETIME2, JSON_VALUE(@ChangeDetails, '$.endDate')),
                MinOrderValue = JSON_VALUE(@ChangeDetails, '$.minOrderValue'),
                UsageLimit = JSON_VALUE(@ChangeDetails, '$.usageLimit'),
                Status = 1
            WHERE PromotionID = @PromotionID;

            PRINT N'Approved UPDATE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR);
            
            -- *** BỎ LOGIC ĐỒNG BỘ PromotionProducts ***
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            UPDATE dbo.Promotions SET Status = 0 WHERE PromotionID = @PromotionID;
            PRINT N'Approved DELETE for PromotionID: ' + CAST(@PromotionID AS NVARCHAR);
        END;
        
        UPDATE dbo.PromotionApprovals
        SET Status = 'Approved',
            ReviewedByUserID = @ReviewedByUserID,
            ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;
        
        COMMIT TRANSACTION;
        PRINT N'ApprovalID ' + CAST(@ApprovalID AS NVARCHAR) + N' processed successfully.';
        
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

-- ================================================================
-- REJECT PROCEDURES (Giữ nguyên)
-- ================================================================

CREATE OR ALTER PROCEDURE sp_RejectProductChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.ProductApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

CREATE OR ALTER PROCEDURE sp_RejectPromotionChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.PromotionApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

CREATE OR ALTER PROCEDURE sp_ApproveToppingChange
    @ApprovalID INT,
    @ReviewedByUserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    BEGIN TRY
        DECLARE @ActionType NVARCHAR(20);
        DECLARE @ToppingID INT;
        DECLARE @ShopID INT;
        DECLARE @ChangeDetails NVARCHAR(MAX);
        
        SELECT 
            @ActionType = ActionType,
            @ToppingID = ToppingID,
            @ShopID = ShopID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ToppingApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;

        IF @ActionType = 'CREATE'
        BEGIN
            UPDATE dbo.Toppings SET Status = 1 WHERE ToppingID = @ToppingID;
        END
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            UPDATE dbo.Toppings
            SET 
                ToppingName = JSON_VALUE(@ChangeDetails, '$.toppingName'),
                AdditionalPrice = JSON_VALUE(@ChangeDetails, '$.additionalPrice'),
                ImageURL = JSON_VALUE(@ChangeDetails, '$.imageURL'),
                Status = 1
            WHERE ToppingID = @ToppingID;
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            UPDATE dbo.Toppings SET Status = 0 WHERE ToppingID = @ToppingID;
        END;
        
        UPDATE dbo.ToppingApprovals
        SET Status = 'Approved',
            ReviewedByUserID = @ReviewedByUserID,
            ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE sp_RejectToppingChange
    @ApprovalID INT,
    @ReviewedByUserID INT,
    @RejectionReason NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.ToppingApprovals
    SET Status = 'Rejected',
        ReviewedByUserID = @ReviewedByUserID,
        ReviewedAt = SYSUTCDATETIME(),
        RejectionReason = @RejectionReason
    WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
    
    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Approval request not found or already processed', 16, 1);
        RETURN;
    END;
END;
GO

-- Trigger to record shop revenue when order is completed
CREATE OR ALTER TRIGGER trg_RecordShopRevenue
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF UPDATE(OrderStatus)
    BEGIN
        INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
        SELECT 
            i.ShopID,
            i.OrderID,
            i.GrandTotal,
            i.GrandTotal * s.CommissionRate / 100,
            i.GrandTotal * (100 - s.CommissionRate) / 100,
			GETDATE()
        FROM inserted i
        JOIN deleted d ON i.OrderID = d.OrderID
        JOIN dbo.Shops s ON i.ShopID = s.ShopID
        WHERE i.OrderStatus = 'Completed' 
        AND d.OrderStatus <> 'Completed';
    END;
END;
GO