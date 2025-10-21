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
        
        -- Get approval details
        SELECT 
            @ActionType = ActionType,
            @ProductID = ProductID,
            @ChangeDetails = ChangeDetails
        FROM dbo.ProductApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;
        
        -- Apply changes based on action type
        IF @ActionType = 'CREATE'
        BEGIN
            -- Activate the product
            UPDATE dbo.Products
            SET Status = 1
            WHERE ProductID = @ProductID;
        END
        ELSE IF @ActionType = 'UPDATE'
        BEGIN
            -- Apply the changes from ChangeDetails JSON
            -- This would require parsing JSON and updating product
            -- Implementation depends on the structure of ChangeDetails
            UPDATE dbo.Products
            SET Status = 1,
                UpdatedAt = SYSUTCDATETIME()
            WHERE ProductID = @ProductID;
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            -- Soft delete the product
            UPDATE dbo.Products
            SET Status = 0
            WHERE ProductID = @ProductID;
        END;
        
        -- Update approval status
        UPDATE dbo.ProductApprovals
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
        
        SELECT 
            @ActionType = ActionType,
            @PromotionID = PromotionID
        FROM dbo.PromotionApprovals
        WHERE ApprovalID = @ApprovalID AND Status = 'Pending';
        
        IF @ActionType IS NULL
        BEGIN
            RAISERROR('Approval request not found or already processed', 16, 1);
            RETURN;
        END;
        
        -- Apply changes
        IF @ActionType = 'CREATE' OR @ActionType = 'UPDATE'
        BEGIN
            UPDATE dbo.Promotions
            SET Status = 1
            WHERE PromotionID = @PromotionID;
        END
        ELSE IF @ActionType = 'DELETE'
        BEGIN
            UPDATE dbo.Promotions
            SET Status = 0
            WHERE PromotionID = @PromotionID;
        END;
        
        -- Update approval status
        UPDATE dbo.PromotionApprovals
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