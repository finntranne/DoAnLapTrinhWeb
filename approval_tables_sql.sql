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

-- Procedure to approve product changes
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

        SELECT
            @ActionType = ActionType,
            @ProductID = ProductID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ProductApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';

        IF @ActionType IS NULL BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1); RETURN;
        END;

        IF @ActionType = 'CREATE' BEGIN
            UPDATE dbo.Products SET Status = 1 WHERE ProductID = @ProductID;
            PRINT N'Approved CREATE for ProductID: ' + CAST(@ProductID AS NVARCHAR);
        END
        ELSE IF @ActionType = 'UPDATE' BEGIN
            -- 1. Update basic product info
            UPDATE p SET
                p.ProductName = JSON_VALUE(@ChangeDetails, '$.productName'),
                p.Description = JSON_VALUE(@ChangeDetails, '$.description'),
                p.CategoryID = JSON_VALUE(@ChangeDetails, '$.categoryId'),
                p.UpdatedAt = SYSUTCDATETIME()
            FROM dbo.Products p WHERE p.ProductID = @ProductID;
            PRINT N'Updated basic info for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- 2. Synchronize Variants (Keep existing logic)
            DECLARE @NewVariants TABLE ( VariantID INT NULL, SizeID INT NOT NULL, Price DECIMAL(10,2) NOT NULL, Stock INT NOT NULL, SKU NVARCHAR(50) NULL );
            INSERT INTO @NewVariants (VariantID, SizeID, Price, Stock, SKU)
            SELECT JSON_VALUE(v.value, '$.variantId'), JSON_VALUE(v.value, '$.sizeId'), JSON_VALUE(v.value, '$.price'), JSON_VALUE(v.value, '$.stock'), JSON_VALUE(v.value, '$.sku')
            FROM OPENJSON(@ChangeDetails, '$.variants') AS v;
            MERGE INTO dbo.ProductVariants AS Target USING @NewVariants AS Source ON (Target.ProductID = @ProductID AND Target.VariantID = Source.VariantID)
            WHEN MATCHED THEN UPDATE SET Target.SizeID = Source.SizeID, Target.Price = Source.Price, Target.Stock = Source.Stock, Target.SKU = Source.SKU
            WHEN NOT MATCHED BY TARGET AND Source.VariantID IS NULL THEN INSERT (ProductID, SizeID, Price, Stock, SKU) VALUES (@ProductID, Source.SizeID, Source.Price, Source.Stock, Source.SKU)
            WHEN NOT MATCHED BY SOURCE AND Target.ProductID = @ProductID THEN DELETE;
            PRINT N'Synchronized variants for ProductID: ' + CAST(@ProductID AS NVARCHAR);

            -- 3. Image Update Logic
            DECLARE @NewImageUrlsJson NVARCHAR(MAX) = JSON_QUERY(@ChangeDetails, '$.newImageUrls');
            IF @NewImageUrlsJson IS NOT NULL AND ISJSON(@NewImageUrlsJson) > 0 AND LEFT(LTRIM(@NewImageUrlsJson), 1) = N'['
            BEGIN
                PRINT N'Processing new images for ProductID: ' + CAST(@ProductID AS NVARCHAR);
                DECLARE @PrimaryIndex INT = ISNULL(CAST(JSON_VALUE(@ChangeDetails, '$.primaryImageIndex') AS INT), 0);
                DECLARE @NewImages TABLE ( ImageURL NVARCHAR(500), JsonIndex INT );

                -- *** THIS IS THE CORRECTED INSERT ***
                INSERT INTO @NewImages (ImageURL, JsonIndex)
                SELECT
                    img.value, -- Select the string value directly
                    CAST(img.[key] AS INT)
                FROM OPENJSON(@NewImageUrlsJson, '$') AS img; -- Use the extracted array
                -- *** END CORRECTION ***

                DELETE FROM dbo.ProductImages WHERE ProductID = @ProductID;
                PRINT N'Deleted old images for ProductID: ' + CAST(@ProductID AS NVARCHAR);

                INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder)
                SELECT @ProductID, ni.ImageURL, CASE WHEN ni.JsonIndex = @PrimaryIndex THEN 1 ELSE 0 END, ni.JsonIndex
                FROM @NewImages ni;
                PRINT N'Inserted new images for ProductID: ' + CAST(@ProductID AS NVARCHAR) + N'. Primary index: ' + CAST(@PrimaryIndex AS NVARCHAR);
            END
            ELSE BEGIN
                PRINT N'No valid new images array found in ChangeDetails for ProductID: ' + CAST(@ProductID AS NVARCHAR);
            END

            UPDATE dbo.Products SET Status = 1 WHERE ProductID = @ProductID;
        END
        ELSE IF @ActionType = 'DELETE' BEGIN
            UPDATE dbo.Products SET Status = 0 WHERE ProductID = @ProductID;
            PRINT N'Approved DELETE for ProductID: ' + CAST(@ProductID AS NVARCHAR);
        END;

        UPDATE dbo.ProductApprovals SET Status = 'Approved', ReviewedByUserID = @ReviewedByUserID, ReviewedAt = SYSUTCDATETIME()
        WHERE ApprovalID = @ApprovalID;

        COMMIT TRANSACTION;
        PRINT N'ApprovalID ' + CAST(@ApprovalID AS NVARCHAR) + N' processed successfully.';

    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        PRINT N'Error processing ApprovalID ' + CAST(@ApprovalID AS NVARCHAR);
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