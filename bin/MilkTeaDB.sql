/* ================================================================
   SCRIPT: Multi-Vendor Milk Tea E-commerce Database - Unified User Structure
   Database: MilkTeaShopDB
   Purpose: Spring Boot + JPA + SQLServer + JWT + Spring Security
   Features: OTP verification, Multi-role users, Payment integration
   ================================================================ */

-- Check and create database
IF DB_ID('MilkTeaShopDB') IS NULL
BEGIN
  CREATE DATABASE MilkTeaShopDB;
END;
GO

USE MilkTeaShopDB;
GO

-- ================================================================
-- PART 1: DROP EXISTING TABLES IN CORRECT ORDER
-- ================================================================

PRINT N'==== Dropping existing tables...';
GO

IF OBJECT_ID('dbo.Reviews','U') IS NOT NULL DROP TABLE dbo.Reviews;
IF OBJECT_ID('dbo.OrderDetail_Toppings','U') IS NOT NULL DROP TABLE dbo.OrderDetail_Toppings;
IF OBJECT_ID('dbo.OrderDetails','U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.OrderHistory','U') IS NOT NULL DROP TABLE dbo.OrderHistory;
IF OBJECT_ID('dbo.Orders','U') IS NOT NULL DROP TABLE dbo.Orders;

IF OBJECT_ID('dbo.CartItem_Toppings','U') IS NOT NULL DROP TABLE dbo.CartItem_Toppings;
IF OBJECT_ID('dbo.CartItems','U') IS NOT NULL DROP TABLE dbo.CartItems;
IF OBJECT_ID('dbo.Carts','U') IS NOT NULL DROP TABLE dbo.Carts;

IF OBJECT_ID('dbo.Favorites','U') IS NOT NULL DROP TABLE dbo.Favorites;
IF OBJECT_ID('dbo.ViewedProducts','U') IS NOT NULL DROP TABLE dbo.ViewedProducts;
IF OBJECT_ID('dbo.PromotionProducts','U') IS NOT NULL DROP TABLE dbo.PromotionProducts;
IF OBJECT_ID('dbo.Promotions','U') IS NOT NULL DROP TABLE dbo.Promotions;

IF OBJECT_ID('dbo.CloudinaryAssets','U') IS NOT NULL DROP TABLE dbo.CloudinaryAssets;
IF OBJECT_ID('dbo.ProductImages','U') IS NOT NULL DROP TABLE dbo.ProductImages;
IF OBJECT_ID('dbo.Toppings','U') IS NOT NULL DROP TABLE dbo.Toppings;
IF OBJECT_ID('dbo.ProductVariants','U') IS NOT NULL DROP TABLE dbo.ProductVariants;
IF OBJECT_ID('dbo.Sizes','U') IS NOT NULL DROP TABLE dbo.Sizes;

IF OBJECT_ID('dbo.Products','U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories','U') IS NOT NULL DROP TABLE dbo.Categories;

IF OBJECT_ID('dbo.Shops','U') IS NOT NULL DROP TABLE dbo.Shops;
IF OBJECT_ID('dbo.ShopRevenue','U') IS NOT NULL DROP TABLE dbo.ShopRevenue;

IF OBJECT_ID('dbo.ChatMessages','U') IS NOT NULL DROP TABLE dbo.ChatMessages;
IF OBJECT_ID('dbo.DeviceTokens','U') IS NOT NULL DROP TABLE dbo.DeviceTokens;
IF OBJECT_ID('dbo.JWTTokens','U') IS NOT NULL DROP TABLE dbo.JWTTokens;
IF OBJECT_ID('dbo.Notifications','U') IS NOT NULL DROP TABLE dbo.Notifications;

IF OBJECT_ID('dbo.Addresses','U') IS NOT NULL DROP TABLE dbo.Addresses;
IF OBJECT_ID('dbo.SystemConfiguration','U') IS NOT NULL DROP TABLE dbo.SystemConfiguration;
IF OBJECT_ID('dbo.ShippingProviders','U') IS NOT NULL DROP TABLE dbo.ShippingProviders;
IF OBJECT_ID('dbo.UserRoles','U') IS NOT NULL DROP TABLE dbo.UserRoles;
IF OBJECT_ID('dbo.Roles','U') IS NOT NULL DROP TABLE dbo.Roles;
IF OBJECT_ID('dbo.Users','U') IS NOT NULL DROP TABLE dbo.Users;

GO

PRINT N'✓ All tables dropped successfully.';
GO

-- ================================================================
-- PART 2: CREATE CORE TABLES
-- ================================================================

PRINT N'==== Creating core tables...';
GO

-- 1. User Account System (Unified structure)
CREATE TABLE dbo.Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    PhoneNumber NVARCHAR(20) NULL UNIQUE,
    FullName NVARCHAR(255) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 0 CHECK (Status IN (0,1,2)), 
    -- 0: Pending (chưa xác thực OTP), 1: Active, 2: Suspended
    AvatarURL NVARCHAR(500) NULL,
    
    -- OTP fields for registration and password reset
    OtpCode NVARCHAR(10) NULL,
    OtpExpiryTime DATETIME2 NULL,
    OtpPurpose NVARCHAR(20) NULL CHECK (OtpPurpose IN ('REGISTER', 'RESET_PASSWORD', NULL)),
    
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    LastLoginAt DATETIME2 NULL
);

CREATE TABLE dbo.Roles (
    RoleID INT PRIMARY KEY,
    RoleName NVARCHAR(50) NOT NULL UNIQUE,
    Description NVARCHAR(255) NULL
);

CREATE TABLE dbo.UserRoles (
    UserID INT NOT NULL,
    RoleID INT NOT NULL,
    AssignedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    PRIMARY KEY (UserID, RoleID),
    CONSTRAINT FK_UserRoles_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_UserRoles_Role FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);

-- 2. Addresses (for shipping)
CREATE TABLE dbo.Addresses (
    AddressID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    AddressName NVARCHAR(100) NOT NULL, -- e.g., "Nhà riêng", "Văn phòng"
    FullAddress NVARCHAR(500) NOT NULL,
    PhoneNumber NVARCHAR(20) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    IsDefault BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Address_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 3. Shops (For Multi-Vendor)
CREATE TABLE dbo.Shops (
    ShopID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL UNIQUE, -- The user who owns this shop (Vendor)
    ShopName NVARCHAR(255) NOT NULL UNIQUE,
    Description NVARCHAR(MAX) NULL,
    LogoURL NVARCHAR(500) NULL,
    CoverImageURL NVARCHAR(500) NULL,
    Address NVARCHAR(500) NOT NULL,
    PhoneNumber NVARCHAR(20) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 0 CHECK (Status IN (0,1,2)), 
    -- 0: Pending Approval, 1: Active, 2: Suspended
    CommissionRate DECIMAL(5,2) DEFAULT 5.00 CHECK (CommissionRate >= 0 AND CommissionRate <= 100), 
    -- App commission rate for this shop (%)
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Shop_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
);

-- Shop Revenue Tracking
CREATE TABLE dbo.ShopRevenue (
    RevenueID INT IDENTITY(1,1) PRIMARY KEY,
    ShopID INT NOT NULL,
    OrderID INT NOT NULL,
    OrderAmount DECIMAL(12,2) NOT NULL, -- Total order value
    CommissionAmount DECIMAL(12,2) NOT NULL, -- Amount deducted by platform
    NetRevenue DECIMAL(12,2) NOT NULL, -- Amount shop receives
    RecordedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ShopRevenue_Shop FOREIGN KEY (ShopID) REFERENCES dbo.Shops(ShopID)
);

-- 4. Categories and Products
CREATE TABLE dbo.Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(255) NOT NULL UNIQUE,
    Description NVARCHAR(MAX) NULL,
    ImageURL NVARCHAR(500) NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)) -- 0: Inactive, 1: Active
);

CREATE TABLE dbo.Products (
    ProductID INT IDENTITY(1,1) PRIMARY KEY,
    ShopID INT NOT NULL, -- Each product belongs to a shop
    CategoryID INT NOT NULL,
    ProductName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)), -- 0: Inactive, 1: Active
    AverageRating DECIMAL(3,2) DEFAULT 0 CHECK (AverageRating >= 0 AND AverageRating <= 5),
    TotalReviews INT DEFAULT 0 CHECK (TotalReviews >= 0),
    ViewCount INT DEFAULT 0 CHECK (ViewCount >= 0),
    SoldCount INT DEFAULT 0 CHECK (SoldCount >= 0), -- Track best-selling products
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Product_Shop FOREIGN KEY (ShopID) REFERENCES dbo.Shops(ShopID),
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);

-- 5. Product Images, Sizes, Variants, Toppings
CREATE TABLE dbo.ProductImages (
    ImageID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    ImageURL NVARCHAR(500) NOT NULL,
    IsPrimary BIT NOT NULL DEFAULT 0,
    DisplayOrder INT DEFAULT 0,
    CONSTRAINT FK_ProductImage_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

CREATE TABLE dbo.Sizes (
    SizeID INT IDENTITY(1,1) PRIMARY KEY,
    SizeName NVARCHAR(20) NOT NULL UNIQUE,
    Description NVARCHAR(100) NULL
);

CREATE TABLE dbo.ProductVariants (
    VariantID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    SizeID INT NOT NULL,
    Price DECIMAL(10,2) NOT NULL CHECK (Price >= 0),
    Stock INT NOT NULL DEFAULT 0 CHECK (Stock >= 0),
    SKU NVARCHAR(50) NULL UNIQUE, -- Stock Keeping Unit
    CONSTRAINT UQ_ProductVariant_Unique UNIQUE (ProductID, SizeID),
    CONSTRAINT FK_Variant_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT FK_Variant_Size FOREIGN KEY (SizeID) REFERENCES dbo.Sizes(SizeID)
);

CREATE TABLE dbo.Toppings (
    ToppingID INT IDENTITY(1,1) PRIMARY KEY,
    ToppingName NVARCHAR(255) NOT NULL UNIQUE,
    AdditionalPrice DECIMAL(10,2) NOT NULL CHECK (AdditionalPrice >= 0),
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    ImageURL NVARCHAR(500) NULL
);

-- 6. Promotions
CREATE TABLE dbo.Promotions (
    PromotionID INT IDENTITY(1,1) PRIMARY KEY,
    CreatedByUserID INT NOT NULL, -- Can be Admin or Vendor
    CreatedByShopID INT NULL, -- NULL if created by Admin, ShopID if by Vendor
    PromotionName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    PromoCode NVARCHAR(50) UNIQUE NULL,
    DiscountType NVARCHAR(20) NOT NULL CHECK (DiscountType IN ('Percentage', 'FixedAmount', 'FreeShip')),
    DiscountValue DECIMAL(10,2) NOT NULL CHECK (DiscountValue >= 0),
    MaxDiscountAmount DECIMAL(10,2) NULL, -- For percentage discounts
    StartDate DATETIME2 NOT NULL,
    EndDate DATETIME2 NOT NULL,
    MinOrderValue DECIMAL(10,2) DEFAULT 0 CHECK (MinOrderValue >= 0),
    UsageLimit INT NULL, -- Total usage limit
    UsedCount INT DEFAULT 0 CHECK (UsedCount >= 0),
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Promotion_User FOREIGN KEY (CreatedByUserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Promotion_Shop FOREIGN KEY (CreatedByShopID) REFERENCES dbo.Shops(ShopID),
    CONSTRAINT CK_Promotion_Dates CHECK (EndDate >= StartDate)
);

CREATE TABLE dbo.PromotionProducts (
    PromotionID INT NOT NULL,
    ProductID INT NOT NULL,
    PRIMARY KEY (PromotionID, ProductID),
    CONSTRAINT FK_PromoProduct_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotions(PromotionID) ON DELETE CASCADE,
    CONSTRAINT FK_PromoProduct_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 7. Favorites & Viewed History
CREATE TABLE dbo.Favorites (
    FavoriteID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    ProductID INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Favorite_Unique UNIQUE (UserID, ProductID),
    CONSTRAINT FK_Favorite_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_Favorite_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

CREATE TABLE dbo.ViewedProducts (
    ViewedID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    ProductID INT NOT NULL,
    ViewCount INT DEFAULT 1,
    LastViewedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Viewed_Unique UNIQUE (UserID, ProductID),
    CONSTRAINT FK_Viewed_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_Viewed_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 8. Shopping Carts
CREATE TABLE dbo.Carts (
    CartID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL UNIQUE,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Cart_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.CartItems (
    CartItemID INT IDENTITY(1,1) PRIMARY KEY,
    CartID INT NOT NULL,
    VariantID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    AddedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_CartItem_Cart FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID) ON DELETE CASCADE,
    CONSTRAINT FK_CartItem_Variant FOREIGN KEY (VariantID) REFERENCES dbo.ProductVariants(VariantID)
);

CREATE TABLE dbo.CartItem_Toppings (
    CartItemID INT NOT NULL,
    ToppingID INT NOT NULL,
    PRIMARY KEY (CartItemID, ToppingID),
    CONSTRAINT FK_CartItemTopping_CartItem FOREIGN KEY (CartItemID) REFERENCES dbo.CartItems(CartItemID) ON DELETE CASCADE,
    CONSTRAINT FK_CartItemTopping_Topping FOREIGN KEY (ToppingID) REFERENCES dbo.Toppings(ToppingID)
);

-- 9. Shipping Providers
CREATE TABLE dbo.ShippingProviders (
    ProviderID INT IDENTITY(1,1) PRIMARY KEY,
    ProviderName NVARCHAR(255) NOT NULL UNIQUE,
    BaseFee DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (BaseFee >= 0),
    Description NVARCHAR(500) NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1))
);

-- 10. Orders
CREATE TABLE dbo.Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL, -- Customer who placed the order
    ShopID INT NOT NULL, -- Shop that receives the order
    PromotionID INT NULL,
    OrderDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    OrderStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending'
        CHECK (OrderStatus IN ('Pending', 'Confirmed', 'Delivering', 'Completed', 'Cancelled', 'Returned', 'Refunded')),
    
    -- Payment Information
    PaymentMethod NVARCHAR(50) NOT NULL 
        CHECK (PaymentMethod IN ('COD', 'Momo', 'VNPay', 'ZaloPay', 'BankTransfer')),
    PaymentStatus NVARCHAR(30) NOT NULL DEFAULT 'Unpaid' 
        CHECK (PaymentStatus IN ('Unpaid', 'Paid', 'Refunded')),
    PaidAt DATETIME2 NULL,
    TransactionID NVARCHAR(255) NULL, -- Payment gateway transaction ID
    
    -- Shipping Information
    ShippingProviderID INT NULL,
    ShippingAddress NVARCHAR(500) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    RecipientPhone NVARCHAR(20) NOT NULL,
    ShipperID INT NULL, -- User with SHIPPER role
    
    -- Order Amounts
    Subtotal DECIMAL(12,2) NOT NULL CHECK (Subtotal >= 0),
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
    DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (DiscountAmount >= 0),
    GrandTotal DECIMAL(12,2) NOT NULL CHECK (GrandTotal >= 0),
    
    Notes NVARCHAR(500) NULL,
    CancellationReason NVARCHAR(500) NULL,
    CompletedAt DATETIME2 NULL,
    
    CONSTRAINT FK_Order_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Order_Shop FOREIGN KEY (ShopID) REFERENCES dbo.Shops(ShopID),
    CONSTRAINT FK_Order_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotions(PromotionID),
    CONSTRAINT FK_Order_ShippingProvider FOREIGN KEY (ShippingProviderID) REFERENCES dbo.ShippingProviders(ProviderID),
    CONSTRAINT FK_Order_Shipper FOREIGN KEY (ShipperID) REFERENCES dbo.Users(UserID)
);

CREATE TABLE dbo.OrderHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    OldStatus NVARCHAR(30) NULL,
    NewStatus NVARCHAR(30) NOT NULL,
    ChangedByUserID INT NULL, -- User who changed the status
    Timestamp DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    Notes NVARCHAR(500) NULL,
    CONSTRAINT FK_OrderHistory_Order FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderHistory_User FOREIGN KEY (ChangedByUserID) REFERENCES dbo.Users(UserID)
);

CREATE TABLE dbo.OrderDetails (
    OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    VariantID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0), -- Price at the time of purchase
    Subtotal DECIMAL(12,2) NOT NULL CHECK (Subtotal >= 0),
    CONSTRAINT FK_OrderDetail_Order FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetail_Variant FOREIGN KEY (VariantID) REFERENCES dbo.ProductVariants(VariantID)
);

CREATE TABLE dbo.OrderDetail_Toppings (
    OrderDetailID INT NOT NULL,
    ToppingID INT NOT NULL,
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0), -- Price at the time of purchase
    PRIMARY KEY (OrderDetailID, ToppingID),
    CONSTRAINT FK_OrderDetailTopping_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetailTopping_Topping FOREIGN KEY (ToppingID) REFERENCES dbo.Toppings(ToppingID)
);

-- 11. Reviews & Comments
CREATE TABLE dbo.Reviews (
    ReviewID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    ProductID INT NOT NULL,
    OrderDetailID INT NOT NULL UNIQUE, -- A review must be linked to a specific purchased item
    Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(MAX) NULL,
    MediaURLs NVARCHAR(MAX) NULL, -- Store JSON array of image/video URLs
    ReviewDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    IsVerifiedPurchase BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Review_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_Review_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_Review_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID),
    CONSTRAINT CK_Review_Comment CHECK (Comment IS NULL OR LEN(Comment) >= 50) -- Minimum 50 characters
);

-- 12. System & Integration Tables
CREATE TABLE dbo.CloudinaryAssets (
    AssetID INT IDENTITY(1,1) PRIMARY KEY,
    PublicID NVARCHAR(255) NOT NULL UNIQUE,
    CloudinaryURL NVARCHAR(500) NOT NULL,
    ResourceType NVARCHAR(20) NOT NULL CHECK (ResourceType IN ('image', 'video', 'raw')),
    UploadedByUserID INT NULL,
    UploadedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_CloudinaryAssets_User FOREIGN KEY (UploadedByUserID) REFERENCES dbo.Users(UserID)
);

CREATE TABLE dbo.Notifications (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    Type NVARCHAR(50) NOT NULL, -- 'OrderStatus', 'NewPromotion', 'System', etc.
    RelatedEntityID INT NULL, -- e.g., OrderID, ProductID
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Notification_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.ChatMessages (
    MessageID INT IDENTITY(1,1) PRIMARY KEY,
    SenderID INT NOT NULL,
    ReceiverID INT NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ChatMessage_Sender FOREIGN KEY (SenderID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ChatMessage_Receiver FOREIGN KEY (ReceiverID) REFERENCES dbo.Users(UserID)
);

CREATE TABLE dbo.JWTTokens (
    TokenID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Token NVARCHAR(MAX) NOT NULL,
    TokenType NVARCHAR(20) NOT NULL CHECK (TokenType IN ('ACCESS', 'REFRESH')),
    ExpiresAt DATETIME2 NOT NULL,
    IsRevoked BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_JWTToken_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.DeviceTokens (
    DeviceTokenID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    DeviceToken NVARCHAR(500) NOT NULL,
    DeviceType NVARCHAR(20) CHECK (DeviceType IN ('Android', 'iOS', 'Web')),
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_DeviceToken_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.SystemConfiguration (
    ConfigKey NVARCHAR(255) PRIMARY KEY,
    ConfigValue NVARCHAR(MAX) NOT NULL,
    Description NVARCHAR(500) NULL,
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

GO
PRINT N'✓ All tables created successfully.';
GO

-- ================================================================
-- PART 3: CREATE INDEXES FOR PERFORMANCE
-- ================================================================

PRINT N'==== Creating indexes...';
GO

-- User indexes
CREATE INDEX IX_Users_Email ON dbo.Users(Email);
CREATE INDEX IX_Users_Username ON dbo.Users(Username);
CREATE INDEX IX_Users_Status ON dbo.Users(Status);

-- Product indexes
CREATE INDEX IX_Products_ShopID ON dbo.Products(ShopID);
CREATE INDEX IX_Products_CategoryID ON dbo.Products(CategoryID);
CREATE INDEX IX_Products_Status ON dbo.Products(Status);
CREATE INDEX IX_Products_SoldCount ON dbo.Products(SoldCount DESC);
CREATE INDEX IX_Products_AverageRating ON dbo.Products(AverageRating DESC);
CREATE INDEX IX_Products_CreatedAt ON dbo.Products(CreatedAt DESC);

-- Order indexes
CREATE INDEX IX_Orders_UserID ON dbo.Orders(UserID);
CREATE INDEX IX_Orders_ShopID ON dbo.Orders(ShopID);
CREATE INDEX IX_Orders_OrderStatus ON dbo.Orders(OrderStatus);
CREATE INDEX IX_Orders_OrderDate ON dbo.Orders(OrderDate DESC);
CREATE INDEX IX_Orders_ShipperID ON dbo.Orders(ShipperID);

-- Review indexes
CREATE INDEX IX_Reviews_ProductID ON dbo.Reviews(ProductID);
CREATE INDEX IX_Reviews_UserID ON dbo.Reviews(UserID);

-- Cart indexes
CREATE INDEX IX_Carts_UserID ON dbo.Carts(UserID);

-- Favorite indexes
CREATE INDEX IX_Favorites_UserID ON dbo.Favorites(UserID);
CREATE INDEX IX_Favorites_ProductID ON dbo.Favorites(ProductID);

-- ViewedProducts indexes
CREATE INDEX IX_ViewedProducts_UserID ON dbo.ViewedProducts(UserID);
CREATE INDEX IX_ViewedProducts_LastViewedAt ON dbo.ViewedProducts(LastViewedAt DESC);

GO
PRINT N'✓ All indexes created successfully.';
GO

-- ================================================================
-- PART 4: CREATE TRIGGERS
-- ================================================================

PRINT N'==== Creating triggers...';
GO

-- Trigger to update Users.UpdatedAt
CREATE TRIGGER trg_Users_UpdatedAt
ON dbo.Users
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.Users
    SET UpdatedAt = SYSUTCDATETIME()
    FROM dbo.Users u
    INNER JOIN inserted i ON u.UserID = i.UserID;
END;
GO

-- Trigger to update Shops.UpdatedAt
CREATE TRIGGER trg_Shops_UpdatedAt
ON dbo.Shops
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.Shops
    SET UpdatedAt = SYSUTCDATETIME()
    FROM dbo.Shops s
    INNER JOIN inserted i ON s.ShopID = i.ShopID;
END;
GO

-- Trigger to update Products.UpdatedAt
CREATE TRIGGER trg_Products_UpdatedAt
ON dbo.Products
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.Products
    SET UpdatedAt = SYSUTCDATETIME()
    FROM dbo.Products p
    INNER JOIN inserted i ON p.ProductID = i.ProductID;
END;
GO

-- Trigger to update Carts.UpdatedAt
CREATE TRIGGER trg_Carts_UpdatedAt
ON dbo.Carts
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.Carts
    SET UpdatedAt = SYSUTCDATETIME()
    FROM dbo.Carts c
    INNER JOIN inserted i ON c.CartID = i.CartID;
END;
GO

-- Trigger to update Cart.UpdatedAt when CartItems change
CREATE TRIGGER trg_CartItems_UpdateCart
ON dbo.CartItems
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE dbo.Carts
    SET UpdatedAt = SYSUTCDATETIME()
    WHERE CartID IN (
        SELECT DISTINCT CartID FROM inserted
        UNION
        SELECT DISTINCT CartID FROM deleted
    );
END;
GO

-- Trigger to update product's sold count when an order is completed
CREATE TRIGGER trg_UpdateSoldCountOnOrderCompletion
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF UPDATE(OrderStatus)
    BEGIN
        -- Increase sold count when order is completed
        UPDATE p
        SET p.SoldCount = p.SoldCount + od.Quantity
        FROM dbo.Products p
        JOIN dbo.ProductVariants pv ON p.ProductID = pv.ProductID
        JOIN dbo.OrderDetails od ON pv.VariantID = od.VariantID
        JOIN inserted i ON od.OrderID = i.OrderID
        JOIN deleted d ON i.OrderID = d.OrderID
        WHERE i.OrderStatus = 'Completed' AND d.OrderStatus <> 'Completed';
        
        -- Decrease sold count when order is cancelled/returned after being completed
        UPDATE p
        SET p.SoldCount = p.SoldCount - od.Quantity
        FROM dbo.Products p
        JOIN dbo.ProductVariants pv ON p.ProductID = pv.ProductID
        JOIN dbo.OrderDetails od ON pv.VariantID = od.VariantID
        JOIN inserted i ON od.OrderID = i.OrderID
        JOIN deleted d ON i.OrderID = d.OrderID
        WHERE d.OrderStatus = 'Completed' 
        AND i.OrderStatus IN ('Cancelled', 'Returned', 'Refunded')
        AND p.SoldCount > 0;
    END;
END;
GO

-- Trigger to update average rating and total reviews on a product
CREATE TRIGGER trg_UpdateProductRating
ON dbo.Reviews
AFTER INSERT, DELETE, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Get ProductIDs from inserted and deleted tables
    DECLARE @ProductIDs TABLE (ID INT);
    INSERT INTO @ProductIDs SELECT ProductID FROM inserted;
    INSERT INTO @ProductIDs SELECT ProductID FROM deleted;

    -- Update statistics for affected products
    UPDATE p
    SET
        p.AverageRating = ISNULL((SELECT AVG(CAST(r.Rating AS DECIMAL(3,2))) FROM dbo.Reviews r WHERE r.ProductID = p.ProductID), 0),
        p.TotalReviews = ISNULL((SELECT COUNT(*) FROM dbo.Reviews r WHERE r.ProductID = p.ProductID), 0)
    FROM dbo.Products p
    WHERE p.ProductID IN (SELECT DISTINCT ID FROM @ProductIDs);
END;
GO

-- Trigger to record order status changes in history
CREATE TRIGGER trg_RecordOrderStatusChange
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF UPDATE(OrderStatus)
    BEGIN
        INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Notes)
        SELECT 
            i.OrderID,
            d.OrderStatus,
            i.OrderStatus,
            NULL, -- Will be set by application
            CASE 
                WHEN i.OrderStatus = 'Cancelled' THEN i.CancellationReason
                ELSE NULL
            END
        FROM inserted i
        JOIN deleted d ON i.OrderID = d.OrderID
        WHERE i.OrderStatus <> d.OrderStatus;
    END;
END;
GO

-- Trigger to update product view count
CREATE TRIGGER trg_UpdateProductViewCount
ON dbo.ViewedProducts
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE p
    SET p.ViewCount = (
        SELECT COUNT(DISTINCT vp.UserID)
        FROM dbo.ViewedProducts vp
        WHERE vp.ProductID = p.ProductID
    )
    FROM dbo.Products p
    WHERE p.ProductID IN (SELECT DISTINCT ProductID FROM inserted);
END;
GO

-- Trigger to record shop revenue when order is completed
CREATE TRIGGER trg_RecordShopRevenue
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF UPDATE(OrderStatus)
    BEGIN
        INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue)
        SELECT 
            i.ShopID,
            i.OrderID,
            i.GrandTotal,
            i.GrandTotal * s.CommissionRate / 100,
            i.GrandTotal * (100 - s.CommissionRate) / 100
        FROM inserted i
        JOIN deleted d ON i.OrderID = d.OrderID
        JOIN dbo.Shops s ON i.ShopID = s.ShopID
        WHERE i.OrderStatus = 'Completed' 
        AND d.OrderStatus <> 'Completed';
    END;
END;
GO

-- Trigger to update promotion used count
CREATE TRIGGER trg_UpdatePromotionUsedCount
ON dbo.Orders
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE p
    SET p.UsedCount = p.UsedCount + 1
    FROM dbo.Promotions p
    INNER JOIN inserted i ON p.PromotionID = i.PromotionID
    WHERE i.PromotionID IS NOT NULL;
END;
GO

-- Trigger to ensure only one default address per user
CREATE TRIGGER trg_EnsureOneDefaultAddress
ON dbo.Addresses
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF UPDATE(IsDefault) OR EXISTS(SELECT 1 FROM inserted WHERE IsDefault = 1)
    BEGIN
        -- When an address is set as default, unset all other defaults for that user
        UPDATE a
        SET a.IsDefault = 0
        FROM dbo.Addresses a
        INNER JOIN inserted i ON a.UserID = i.UserID
        WHERE a.AddressID <> i.AddressID 
        AND i.IsDefault = 1
        AND a.IsDefault = 1;
    END;
END;
GO

PRINT N'✓ All triggers created successfully.';
GO

-- ================================================================
-- PART 5: CREATE VIEWS
-- ================================================================

PRINT N'==== Creating views...';
GO

-- View for guest homepage (products from shops with > 10 total sales)
CREATE VIEW dbo.v_GuestHomepageProducts
AS
SELECT 
    p.ProductID,
    p.ProductName,
    p.Description,
    p.AverageRating,
    p.TotalReviews,
    p.SoldCount,
    v.Price AS DefaultPrice,
    img.ImageURL AS PrimaryImageURL,
    s.ShopID,
    s.ShopName,
    s.LogoURL AS ShopLogoURL,
    c.CategoryName
FROM dbo.Products p
JOIN dbo.Shops s ON p.ShopID = s.ShopID
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
-- Find the price of the default variant (size M or smallest)
OUTER APPLY (
    SELECT TOP 1 pv.Price
    FROM dbo.ProductVariants pv
    JOIN dbo.Sizes sz ON pv.SizeID = sz.SizeID
    WHERE pv.ProductID = p.ProductID
    ORDER BY CASE WHEN sz.SizeName = 'M' THEN 1 ELSE 2 END, pv.Price
) v
-- Find the primary image
OUTER APPLY (
    SELECT TOP 1 pi.ImageURL
    FROM dbo.ProductImages pi
    WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1
) img
WHERE p.Status = 1 
AND s.Status = 1
AND s.ShopID IN (
    SELECT ShopID 
    FROM dbo.Products
    GROUP BY ShopID
    HAVING SUM(SoldCount) > 10
);
GO

-- View for detailed product information
CREATE VIEW dbo.v_ProductDetails
AS
SELECT
    p.ProductID,
    p.ProductName,
    p.Description,
    p.AverageRating,
    p.TotalReviews,
    p.ViewCount,
    p.SoldCount,
    p.Status,
    p.CreatedAt,
    c.CategoryID,
    c.CategoryName,
    s.ShopID,
    s.ShopName,
    s.LogoURL AS ShopLogoURL,
    s.Address AS ShopAddress,
    s.PhoneNumber AS ShopPhone
FROM dbo.Products p
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
JOIN dbo.Shops s ON p.ShopID = s.ShopID;
GO

-- View for best-selling products (top 20)
CREATE VIEW dbo.v_BestSellingProducts
AS
SELECT TOP 20
    p.ProductID,
    p.ProductName,
    p.SoldCount,
    p.AverageRating,
    v.Price AS DefaultPrice,
    img.ImageURL AS PrimaryImageURL,
    s.ShopName,
    c.CategoryName
FROM dbo.Products p
JOIN dbo.Shops s ON p.ShopID = s.ShopID
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
OUTER APPLY (
    SELECT TOP 1 pv.Price
    FROM dbo.ProductVariants pv
    WHERE pv.ProductID = p.ProductID
    ORDER BY pv.Price
) v
OUTER APPLY (
    SELECT TOP 1 pi.ImageURL
    FROM dbo.ProductImages pi
    WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1
) img
WHERE p.Status = 1 AND s.Status = 1
ORDER BY p.SoldCount DESC;
GO

-- View for newest products (top 20)
CREATE VIEW dbo.v_NewestProducts
AS
SELECT TOP 20
    p.ProductID,
    p.ProductName,
    p.CreatedAt,
    p.AverageRating,
    v.Price AS DefaultPrice,
    img.ImageURL AS PrimaryImageURL,
    s.ShopName,
    c.CategoryName
FROM dbo.Products p
JOIN dbo.Shops s ON p.ShopID = s.ShopID
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
OUTER APPLY (
    SELECT TOP 1 pv.Price
    FROM dbo.ProductVariants pv
    WHERE pv.ProductID = p.ProductID
    ORDER BY pv.Price
) v
OUTER APPLY (
    SELECT TOP 1 pi.ImageURL
    FROM dbo.ProductImages pi
    WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1
) img
WHERE p.Status = 1 AND s.Status = 1
ORDER BY p.CreatedAt DESC;
GO

-- View for top-rated products (top 20)
CREATE VIEW dbo.v_TopRatedProducts
AS
SELECT TOP 20
    p.ProductID,
    p.ProductName,
    p.AverageRating,
    p.TotalReviews,
    v.Price AS DefaultPrice,
    img.ImageURL AS PrimaryImageURL,
    s.ShopName,
    c.CategoryName
FROM dbo.Products p
JOIN dbo.Shops s ON p.ShopID = s.ShopID
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
OUTER APPLY (
    SELECT TOP 1 pv.Price
    FROM dbo.ProductVariants pv
    WHERE pv.ProductID = p.ProductID
    ORDER BY pv.Price
) v
OUTER APPLY (
    SELECT TOP 1 pi.ImageURL
    FROM dbo.ProductImages pi
    WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1
) img
WHERE p.Status = 1 AND s.Status = 1 AND p.TotalReviews >= 5
ORDER BY p.AverageRating DESC, p.TotalReviews DESC;
GO

-- View for order summary with details
CREATE VIEW dbo.v_OrderSummary
AS
SELECT
    o.OrderID,
    o.OrderDate,
    o.OrderStatus,
    o.PaymentMethod,
    o.PaymentStatus,
    o.GrandTotal,
    u.UserID,
    u.FullName AS CustomerName,
    u.Email AS CustomerEmail,
    u.PhoneNumber AS CustomerPhone,
    s.ShopID,
    s.ShopName,
    shipper.FullName AS ShipperName,
    o.RecipientName,
    o.RecipientPhone,
    o.ShippingAddress
FROM dbo.Orders o
JOIN dbo.Users u ON o.UserID = u.UserID
JOIN dbo.Shops s ON o.ShopID = s.ShopID
LEFT JOIN dbo.Users shipper ON o.ShipperID = shipper.UserID;
GO

-- View for shop revenue summary
CREATE VIEW dbo.v_ShopRevenueSummary
AS
SELECT
    s.ShopID,
    s.ShopName,
    COUNT(DISTINCT sr.OrderID) AS TotalOrders,
    SUM(sr.OrderAmount) AS TotalOrderAmount,
    SUM(sr.CommissionAmount) AS TotalCommission,
    SUM(sr.NetRevenue) AS TotalNetRevenue,
    s.CommissionRate
FROM dbo.Shops s
LEFT JOIN dbo.ShopRevenue sr ON s.ShopID = sr.ShopID
GROUP BY s.ShopID, s.ShopName, s.CommissionRate;
GO

-- View for shipper assigned orders
CREATE VIEW dbo.v_ShipperOrders
AS
SELECT
    o.OrderID,
    o.OrderDate,
    o.OrderStatus,
    o.GrandTotal,
    o.ShipperID,
    shipper.FullName AS ShipperName,
    o.RecipientName,
    o.RecipientPhone,
    o.ShippingAddress,
    s.ShopName,
    CASE 
        WHEN o.OrderStatus = 'Delivering' THEN 1
        WHEN o.OrderStatus = 'Completed' THEN 2
        ELSE 3
    END AS DeliveryPriority
FROM dbo.Orders o
JOIN dbo.Shops s ON o.ShopID = s.ShopID
LEFT JOIN dbo.Users shipper ON o.ShipperID = shipper.UserID
WHERE o.ShipperID IS NOT NULL;
GO

PRINT N'✓ All views created successfully.';
GO

-- ================================================================
-- PART 6: INSERT SAMPLE DATA
-- ================================================================

PRINT N'==== Inserting sample data...';
GO

-- 1. Roles
INSERT INTO dbo.Roles (RoleID, RoleName, Description) VALUES 
(1, 'ADMIN', N'Quản trị viên hệ thống'),
(2, 'CUSTOMER', N'Khách hàng'),
(3, 'VENDOR', N'Người bán hàng'),
(4, 'SHIPPER', N'Nhân viên giao hàng của shop');
(5, 'STAFF', N'Nhân viên của shop');


-- 2. Users (default password: 123456789)
-- Password hash generated with BCrypt
INSERT INTO dbo.Users (Username, PasswordHash, Email, PhoneNumber, FullName, Status) VALUES
('admin', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'admin@alotra.com', '0900000001', N'Quản Trị Viên', 1),
('vendor1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor1@shop.com', '0900000002', N'Nguyễn Văn A', 1),
('vendor2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor2@shop.com', '0900000003', N'Trần Thị B', 1),
('customer1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'minhanh@email.com', '0912345678', N'Lê Minh Anh', 1),
('customer2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'baotran@email.com', '0987654321', N'Phạm Bảo Trân', 1),
('shipper1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'shipper1@email.com', '0911111111', N'Giao Hàng Nhanh', 1),
('customer3', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'ducminh@email.com', '0923456789', N'Hoàng Đức Minh', 1);

-- 3. UserRoles
INSERT INTO dbo.UserRoles (UserID, RoleID) VALUES
(1, 1), -- admin
(2, 2), (2, 3), -- vendor1 (customer + vendor)
(3, 2), (3, 3), -- vendor2 (customer + vendor)
(4, 2), -- customer1
(5, 2), -- customer2
(6, 4), -- shipper1
(7, 2); -- customer3

-- 4. Addresses
INSERT INTO dbo.Addresses (UserID, AddressName, FullAddress, PhoneNumber, RecipientName, IsDefault) VALUES
(4, N'Nhà riêng', N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', '0912345678', N'Lê Minh Anh', 1),
(4, N'Văn phòng', N'456 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM', '0912345678', N'Lê Minh Anh', 0),
(5, N'Nhà riêng', N'789 Pasteur, Phường 6, Quận 3, TP.HCM', '0987654321', N'Phạm Bảo Trân', 1),
(7, N'Nhà riêng', N'321 Hai Bà Trưng, Phường Tân Định, Quận 1, TP.HCM', '0923456789', N'Hoàng Đức Minh', 1);

-- 5. Shops
INSERT INTO dbo.Shops (UserID, ShopName, Description, Address, PhoneNumber, Status, CommissionRate) VALUES
(2, N'Trà Sữa Phúc Long Clone', N'Chuyên cung cấp trà sữa cao cấp, pha chế từ nguyên liệu tự nhiên', N'12 Ngô Đức Kế, Phường Bến Nghé, Quận 1, TP.HCM', '0902222222', 1, 5.00),
(3, N'The Coffee House Mini', N'Cà phê & trà sữa phong cách hiện đại', N'24 Pasteur, Phường Nguyễn Thái Bình, Quận 1, TP.HCM', '0903333333', 1, 5.00);

-- 6. Categories
INSERT INTO dbo.Categories (CategoryName, Description, Status) VALUES 
(N'Trà Sữa', N'Các loại trà sữa truyền thống và hiện đại', 1),
(N'Trà Trái Cây', N'Trà pha chế với trái cây tươi', 1),
(N'Cà Phê', N'Cà phê rang xay và pha phin', 1),
(N'Đá Xay', N'Các loại đồ uống đá xay mát lạnh', 1);

-- 7. Sizes
INSERT INTO dbo.Sizes (SizeName, Description) VALUES 
('S', N'Nhỏ - 300ml'),
('M', N'Vừa - 500ml'),
('L', N'Lớn - 700ml');

-- 8. Toppings
INSERT INTO dbo.Toppings (ToppingName, AdditionalPrice, Status) VALUES
(N'Trân châu đen', 8000, 1),
(N'Trân châu trắng', 8000, 1),
(N'Thạch dừa', 8000, 1),
(N'Pudding', 10000, 1),
(N'Thạch rau câu', 8000, 1),
(N'Kem cheese', 15000, 1);

-- 9. Products
INSERT INTO dbo.Products (ShopID, CategoryID, ProductName, Description, SoldCount, AverageRating, TotalReviews, Status) VALUES
(1, 1, N'Trà Sữa Phúc Long Đặc Biệt', N'Trà sữa pha chế theo công thức độc quyền, hương vị đậm đà', 35, 4.5, 12, 1),
(1, 2, N'Trà Đào Cam Sả', N'Trà đen kết hợp với đào, cam và sả tươi', 28, 4.7, 10, 1),
(1, 1, N'Trà Sữa Ô Long', N'Trà Ô Long thơm ngon pha cùng sữa tươi', 22, 4.3, 8, 1),
(2, 3, N'Cà Phê Đen Đá', N'Cà phê rang xay pha phin truyền thống', 15, 4.6, 6, 1),
(2, 3, N'Cà Phê Sữa Đá', N'Cà phê phin pha cùng sữa đặc', 20, 4.8, 9, 1),
(2, 1, N'Trà Sữa Olong Macchiato', N'Trà Ô Long đặc biệt phủ lớp kem cheese', 18, 4.4, 7, 1),
(2, 4, N'Đá Xay Matcha', N'Matcha xay cùng đá và kem tươi', 12, 4.2, 5, 1);

-- 10. Product Images
INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder) VALUES
(1, 'https://example.com/images/product1-main.jpg', 1, 1),
(1, 'https://example.com/images/product1-detail.jpg', 0, 2),
(2, 'https://example.com/images/product2-main.jpg', 1, 1),
(3, 'https://example.com/images/product3-main.jpg', 1, 1),
(4, 'https://example.com/images/product4-main.jpg', 1, 1),
(5, 'https://example.com/images/product5-main.jpg', 1, 1),
(6, 'https://example.com/images/product6-main.jpg', 1, 1),
(7, 'https://example.com/images/product7-main.jpg', 1, 1);

-- 11. Product Variants
INSERT INTO dbo.ProductVariants (ProductID, SizeID, Price, Stock, SKU) VALUES
-- Trà Sữa Phúc Long Đặc Biệt
(1, 1, 35000, 100, 'TSPL-S-001'),
(1, 2, 45000, 150, 'TSPL-M-001'),
(1, 3, 55000, 100, 'TSPL-L-001'),
-- Trà Đào Cam Sả
(2, 2, 50000, 120, 'TDCS-M-002'),
(2, 3, 60000, 80, 'TDCS-L-002'),
-- Trà Sữa Ô Long
(3, 1, 38000, 90, 'TSOL-S-003'),
(3, 2, 48000, 130, 'TSOL-M-003'),
(3, 3, 58000, 70, 'TSOL-L-003'),
-- Cà Phê Đen Đá
(4, 1, 25000, 100, 'CPDD-S-004'),
(4, 2, 30000, 150, 'CPDD-M-004'),
(4, 3, 35000, 100, 'CPDD-L-004'),
-- Cà Phê Sữa Đá
(5, 1, 28000, 120, 'CPSD-S-005'),
(5, 2, 35000, 180, 'CPSD-M-005'),
(5, 3, 42000, 90, 'CPSD-L-005'),
-- Trà Sữa Olong Macchiato
(6, 2, 52000, 100, 'TSOM-M-006'),
(6, 3, 62000, 80, 'TSOM-L-006'),
-- Đá Xay Matcha
(7, 2, 55000, 70, 'DXMT-M-007'),
(7, 3, 65000, 50, 'DXMT-L-007');

-- 12. Shipping Providers
INSERT INTO dbo.ShippingProviders (ProviderName, BaseFee, Description, Status) VALUES
(N'Giao Hàng Nhanh', 20000, N'Giao hàng trong 24h', 1),
(N'Giao Hàng Tiết Kiệm', 15000, N'Giao hàng trong 2-3 ngày', 1),
(N'Viettel Post', 18000, N'Dịch vụ bưu chính Viettel', 1);

-- 13. Promotions
INSERT INTO dbo.Promotions (CreatedByUserID, CreatedByShopID, PromotionName, Description, PromoCode, DiscountType, DiscountValue, MaxDiscountAmount, StartDate, EndDate, MinOrderValue, UsageLimit, Status) VALUES
(1, NULL, N'Khuyến mãi tháng 10', N'Giảm 10% cho đơn hàng từ 100k', 'OCT2025', 'Percentage', 10, 50000, '2025-10-01', '2025-10-31', 100000, 100, 1),
(2, 1, N'Miễn phí ship', N'Miễn phí vận chuyển cho đơn từ 200k', 'FREESHIP', 'FreeShip', 20000, NULL, '2025-10-15', '2025-11-15', 200000, 50, 1),
(3, 2, N'Giảm 20k', N'Giảm 20k cho đơn đầu tiên', 'NEW20K', 'FixedAmount', 20000, NULL, '2025-10-01', '2025-12-31', 50000, 200, 1);

-- 14. Sample Orders
DECLARE @ShopID1 INT = 1, @ShopID2 INT = 2;

-- Order 1 (Completed)
INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (4, @ShopID1, DATEADD(DAY, -10, GETDATE()), 'Completed', 'COD', 'Paid', 1, N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', N'Lê Minh Anh', '0912345678', 45000, 20000, 0, 65000, 6, DATEADD(DAY, -8, GETDATE()));

DECLARE @OrderID1 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID1, 2, 1, 45000, 45000); -- Trà Sữa Phúc Long size M

-- Order 2 (Completed with toppings)
INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (5, @ShopID1, DATEADD(DAY, -7, GETDATE()), 'Completed', 'VNPay', 'Paid', 1, N'789 Pasteur, Phường 6, Quận 3, TP.HCM', N'Phạm Bảo Trân', '0987654321', 116000, 20000, 0, 136000, 6, DATEADD(DAY, -5, GETDATE()));

DECLARE @OrderID2 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID2, 2, 2, 45000, 90000), -- 2x Trà Sữa Phúc Long size M
(@OrderID2, 7, 1, 48000, 48000); -- 1x Trà Sữa Ô Long size M

DECLARE @OrderDetailID1 INT = SCOPE_IDENTITY() - 1;
DECLARE @OrderDetailID2 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID1, 1, 8000), -- Trân châu đen
(@OrderDetailID1, 4, 10000); -- Pudding

-- Order 3 (Delivering)
INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID)
VALUES (7, @ShopID2, DATEADD(DAY, -2, GETDATE()), 'Delivering', 'COD', 'Unpaid', 1, N'321 Hai Bà Trưng, Phường Tân Định, Quận 1, TP.HCM', N'Hoàng Đức Minh', '0923456789', 70000, 20000, 10000, 80000, 6);

DECLARE @OrderID3 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID3, 11, 2, 30000, 60000), -- 2x Cà Phê Đen size M
(@OrderID3, 14, 1, 35000, 35000); -- 1x Cà Phê Sữa size M

-- Order 4 (Pending)
INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (4, @ShopID2, DATEADD(HOUR, -3, GETDATE()), 'Pending', 'Momo', 'Unpaid', 2, N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', N'Lê Minh Anh', '0912345678', 52000, 15000, 0, 67000);

DECLARE @OrderID4 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID4, 16, 1, 52000, 52000); -- Trà Sữa Olong Macchiato size M

-- 15. Sample Reviews (only for completed orders)
INSERT INTO dbo.Reviews (UserID, ProductID, OrderDetailID, Rating, Comment, ReviewDate, IsVerifiedPurchase) VALUES
(4, 1, 1, 5, N'Trà sữa rất ngon, pha chế chuẩn vị. Sẽ ủng hộ shop tiếp. Đặc biệt là trân châu rất dai và ngọt vừa phải. Nhân viên phục vụ nhiệt tình.', DATEADD(DAY, -7, GETDATE()), 1),
(5, 1, 2, 4, N'Trà sữa khá ngon nhưng hơi ngọt so với khẩu vị của mình. Giao hàng nhanh, đóng gói cẩn thận. Lần sau sẽ đặt ít đường hơn để phù hợp hơn.', DATEADD(DAY, -4, GETDATE()), 1);

-- 16. Sample Favorites
INSERT INTO dbo.Favorites (UserID, ProductID) VALUES
(4, 1), (4, 2), (4, 5),
(5, 1), (5, 3), (5, 6),
(7, 2), (7, 4), (7, 7);

-- 17. Sample Viewed Products
INSERT INTO dbo.ViewedProducts (UserID, ProductID, ViewCount, LastViewedAt) VALUES
(4, 1, 5, DATEADD(HOUR, -2, GETDATE())),
(4, 2, 3, DATEADD(HOUR, -5, GETDATE())),
(4, 3, 2, DATEADD(DAY, -1, GETDATE())),
(5, 1, 4, DATEADD(HOUR, -1, GETDATE())),
(5, 4, 2, DATEADD(HOUR, -3, GETDATE())),
(7, 2, 1, DATEADD(HOUR, -4, GETDATE())),
(7, 5, 3, DATEADD(HOUR, -6, GETDATE()));

-- 18. Sample Carts
INSERT INTO dbo.Carts (UserID) VALUES (4), (5), (7);

DECLARE @CartID1 INT = 1, @CartID2 INT = 2, @CartID3 INT = 3;

INSERT INTO dbo.CartItems (CartID, VariantID, Quantity) VALUES
(@CartID1, 2, 2), -- 2x Trà Sữa Phúc Long size M
(@CartID1, 5, 1), -- 1x Trà Đào Cam Sả size L
(@CartID2, 7, 1), -- 1x Trà Sữa Ô Long size M
(@CartID3, 11, 1); -- 1x Cà Phê Đen size M

DECLARE @CartItemID1 INT = 1;

INSERT INTO dbo.CartItem_Toppings (CartItemID, ToppingID) VALUES
(@CartItemID1, 1), -- Trân châu đen
(@CartItemID1, 6); -- Kem cheese

-- 19. System Configuration
INSERT INTO dbo.SystemConfiguration (ConfigKey, ConfigValue, Description) VALUES
('SITE_NAME', N'Alo Trà - Hệ Thống Trà Sữa', N'Tên website'),
('SITE_EMAIL', 'contact@alotra.com', N'Email liên hệ'),
('SITE_PHONE', '1900-1234', N'Số điện thoại hotline'),
('OTP_EXPIRY_MINUTES', '5', N'Thời gian hết hạn OTP (phút)'),
('MIN_ORDER_VALUE', '30000', N'Giá trị đơn hàng tối thiểu'),
('MAX_CART_ITEMS', '20', N'Số lượng sản phẩm tối đa trong giỏ'),
('REVIEW_MIN_LENGTH', '50', N'Độ dài tối thiểu của đánh giá'),
('DEFAULT_COMMISSION_RATE', '5.00', N'Tỷ lệ hoa hồng mặc định (%)'),
('CLOUDINARY_CLOUD_NAME', 'your_cloud_name', N'Tên cloud của Cloudinary'),
('CLOUDINARY_API_KEY', 'your_api_key', N'API Key của Cloudinary'),
('VNPAY_TMN_CODE', 'your_tmn_code', N'Mã TmnCode VNPay'),
('VNPAY_HASH_SECRET', 'your_hash_secret', N'Hash Secret VNPay'),
('MOMO_PARTNER_CODE', 'your_partner_code', N'Partner Code MoMo'),
('MOMO_ACCESS_KEY', 'your_access_key', N'Access Key MoMo'),
('JWT_SECRET', 'your_jwt_secret_key_here', N'Secret key cho JWT'),
('JWT_EXPIRATION_MS', '86400000', N'Thời gian hết hạn JWT (milliseconds) - 24h'),
('REFRESH_TOKEN_EXPIRATION_MS', '604800000', N'Thời gian hết hạn Refresh Token (milliseconds) - 7 days');

-- 20. Sample Notifications
INSERT INTO dbo.Notifications (UserID, Title, Message, Type, RelatedEntityID, IsRead) VALUES
(4, N'Đơn hàng đã giao thành công', N'Đơn hàng #' + CAST(@OrderID1 AS NVARCHAR) + N' đã được giao thành công. Cảm ơn bạn đã mua hàng!', 'OrderStatus', @OrderID1, 1),
(5, N'Đơn hàng đã giao thành công', N'Đơn hàng #' + CAST(@OrderID2 AS NVARCHAR) + N' đã được giao thành công. Đánh giá sản phẩm để nhận điểm thưởng!', 'OrderStatus', @OrderID2, 1),
(7, N'Đơn hàng đang được giao', N'Đơn hàng #' + CAST(@OrderID3 AS NVARCHAR) + N' đang trên đường giao đến bạn. Vui lòng chú ý điện thoại!', 'OrderStatus', @OrderID3, 0),
(4, N'Khuyến mãi mới', N'Giảm 10% cho đơn hàng từ 100k với mã OCT2025. Áp dụng đến 31/10!', 'NewPromotion', 1, 0);

GO

PRINT N'✓ All sample data inserted successfully.';
GO

-- ================================================================
-- PART 7: CREATE STORED PROCEDURES
-- ================================================================

PRINT N'==== Creating stored procedures...';
GO

-- Procedure to get products by category with pagination
CREATE PROCEDURE sp_GetProductsByCategory
    @CategoryID INT = NULL,
    @PageNumber INT = 1,
    @PageSize INT = 20,
    @SortBy NVARCHAR(20) = 'newest' -- 'newest', 'bestselling', 'rating', 'price_asc', 'price_desc'
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;
    
    WITH ProductList AS (
        SELECT 
            p.ProductID,
            p.ProductName,
            p.Description,
            p.AverageRating,
            p.TotalReviews,
            p.SoldCount,
            p.CreatedAt,
            c.CategoryName,
            s.ShopName,
            s.LogoURL AS ShopLogoURL,
            (SELECT TOP 1 pv.Price FROM dbo.ProductVariants pv WHERE pv.ProductID = p.ProductID ORDER BY pv.Price) AS MinPrice,
            (SELECT TOP 1 pv.Price FROM dbo.ProductVariants pv WHERE pv.ProductID = p.ProductID ORDER BY pv.Price DESC) AS MaxPrice,
            (SELECT TOP 1 pi.ImageURL FROM dbo.ProductImages pi WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1) AS PrimaryImageURL
        FROM dbo.Products p
        JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
        JOIN dbo.Shops s ON p.ShopID = s.ShopID
        WHERE p.Status = 1 
        AND s.Status = 1
        AND (@CategoryID IS NULL OR p.CategoryID = @CategoryID)
    )
    SELECT *
    FROM ProductList
    ORDER BY 
        CASE WHEN @SortBy = 'newest' THEN CreatedAt END DESC,
        CASE WHEN @SortBy = 'bestselling' THEN SoldCount END DESC,
        CASE WHEN @SortBy = 'rating' THEN AverageRating END DESC,
        CASE WHEN @SortBy = 'price_asc' THEN MinPrice END ASC,
        CASE WHEN @SortBy = 'price_desc' THEN MaxPrice END DESC
    OFFSET @Offset ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- Procedure to search products
CREATE PROCEDURE sp_SearchProducts
    @SearchKeyword NVARCHAR(255),
    @CategoryID INT = NULL,
    @MinPrice DECIMAL(10,2) = NULL,
    @MaxPrice DECIMAL(10,2) = NULL,
    @MinRating DECIMAL(3,2) = NULL,
    @PageNumber INT = 1,
    @PageSize INT = 20
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;
    
    SELECT 
        p.ProductID,
        p.ProductName,
        p.Description,
        p.AverageRating,
        p.TotalReviews,
        p.SoldCount,
        c.CategoryName,
        s.ShopName,
        (SELECT TOP 1 pv.Price FROM dbo.ProductVariants pv WHERE pv.ProductID = p.ProductID ORDER BY pv.Price) AS MinPrice,
        (SELECT TOP 1 pi.ImageURL FROM dbo.ProductImages pi WHERE pi.ProductID = p.ProductID AND pi.IsPrimary = 1) AS PrimaryImageURL
    FROM dbo.Products p
    JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
    JOIN dbo.Shops s ON p.ShopID = s.ShopID
    WHERE p.Status = 1 
    AND s.Status = 1
    AND (
        p.ProductName LIKE N'%' + @SearchKeyword + '%' 
        OR p.Description LIKE N'%' + @SearchKeyword + '%'
    )
    AND (@CategoryID IS NULL OR p.CategoryID = @CategoryID)
    AND (@MinPrice IS NULL OR EXISTS (SELECT 1 FROM dbo.ProductVariants pv WHERE pv.ProductID = p.ProductID AND pv.Price >= @MinPrice))
    AND (@MaxPrice IS NULL OR EXISTS (SELECT 1 FROM dbo.ProductVariants pv WHERE pv.ProductID = p.ProductID AND pv.Price <= @MaxPrice))
    AND (@MinRating IS NULL OR p.AverageRating >= @MinRating)
    ORDER BY p.SoldCount DESC, p.AverageRating DESC
    OFFSET @Offset ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- Procedure to get user's order history
CREATE PROCEDURE sp_GetUserOrderHistory
    @UserID INT,
    @OrderStatus NVARCHAR(30) = NULL,
    @PageNumber INT = 1,
    @PageSize INT = 10
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;
    
    SELECT 
        o.OrderID,
        o.OrderDate,
        o.OrderStatus,
        o.PaymentMethod,
        o.PaymentStatus,
        o.GrandTotal,
        s.ShopName,
        s.LogoURL AS ShopLogoURL,
        o.RecipientName,
        o.RecipientPhone,
        (SELECT COUNT(*) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS TotalItems
    FROM dbo.Orders o
    JOIN dbo.Shops s ON o.ShopID = s.ShopID
    WHERE o.UserID = @UserID
    AND (@OrderStatus IS NULL OR o.OrderStatus = @OrderStatus)
    ORDER BY o.OrderDate DESC
    OFFSET @Offset ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- Procedure to get shop's order management
CREATE PROCEDURE sp_GetShopOrders
    @ShopID INT,
    @OrderStatus NVARCHAR(30) = NULL,
    @PageNumber INT = 1,
    @PageSize INT = 10
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;
    
    SELECT 
        o.OrderID,
        o.OrderDate,
        o.OrderStatus,
        o.PaymentMethod,
        o.PaymentStatus,
        o.GrandTotal,
        u.FullName AS CustomerName,
        u.PhoneNumber AS CustomerPhone,
        o.RecipientName,
        o.RecipientPhone,
        o.ShippingAddress,
        shipper.FullName AS ShipperName,
        (SELECT COUNT(*) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS TotalItems
    FROM dbo.Orders o
    JOIN dbo.Users u ON o.UserID = u.UserID
    LEFT JOIN dbo.Users shipper ON o.ShipperID = shipper.UserID
    WHERE o.ShopID = @ShopID
    AND (@OrderStatus IS NULL OR o.OrderStatus = @OrderStatus)
    ORDER BY o.OrderDate DESC
    OFFSET @Offset ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- Procedure to get shipper's assigned orders
CREATE PROCEDURE sp_GetShipperOrders
    @ShipperID INT,
    @OrderStatus NVARCHAR(30) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        o.OrderID,
        o.OrderDate,
        o.OrderStatus,
        o.GrandTotal,
        o.RecipientName,
        o.RecipientPhone,
        o.ShippingAddress,
        s.ShopName,
        s.PhoneNumber AS ShopPhone,
        CASE 
            WHEN o.OrderStatus = 'Delivering' THEN 1
            WHEN o.OrderStatus = 'Completed' THEN 2
            ELSE 3
        END AS Priority
    FROM dbo.Orders o
    JOIN dbo.Shops s ON o.ShopID = s.ShopID
    WHERE o.ShipperID = @ShipperID
    AND (@OrderStatus IS NULL OR o.OrderStatus = @OrderStatus)
    ORDER BY Priority ASC, o.OrderDate DESC;
END;
GO

-- Procedure to calculate shop revenue
CREATE PROCEDURE sp_GetShopRevenue
    @ShopID INT,
    @StartDate DATETIME2 = NULL,
    @EndDate DATETIME2 = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @StartDate IS NULL SET @StartDate = DATEADD(MONTH, -1, GETDATE());
    IF @EndDate IS NULL SET @EndDate = GETDATE();
    
    SELECT 
        COUNT(DISTINCT sr.OrderID) AS TotalOrders,
        SUM(sr.OrderAmount) AS TotalOrderAmount,
        SUM(sr.CommissionAmount) AS TotalCommission,
        SUM(sr.NetRevenue) AS TotalNetRevenue,
        s.CommissionRate
    FROM dbo.ShopRevenue sr
    JOIN dbo.Shops s ON sr.ShopID = s.ShopID
    WHERE sr.ShopID = @ShopID
    AND sr.RecordedAt BETWEEN @StartDate AND @EndDate
    GROUP BY s.CommissionRate;
END;
GO

-- Procedure to get product reviews with pagination
CREATE PROCEDURE sp_GetProductReviews
    @ProductID INT,
    @Rating INT = NULL,
    @PageNumber INT = 1,
    @PageSize INT = 10
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;
    
    SELECT 
        r.ReviewID,
        r.Rating,
        r.Comment,
        r.MediaURLs,
        r.ReviewDate,
        r.IsVerifiedPurchase,
        u.FullName AS ReviewerName,
        u.AvatarURL AS ReviewerAvatar
    FROM dbo.Reviews r
    JOIN dbo.Users u ON r.UserID = u.UserID
    WHERE r.ProductID = @ProductID
    AND (@Rating IS NULL OR r.Rating = @Rating)
    ORDER BY r.ReviewDate DESC
    OFFSET @Offset ROWS
    FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- Procedure to validate and apply promotion
CREATE PROCEDURE sp_ValidatePromotion
    @PromoCode NVARCHAR(50),
    @UserID INT,
    @ShopID INT,
    @OrderSubtotal DECIMAL(12,2),
    @IsValid BIT OUTPUT,
    @DiscountAmount DECIMAL(12,2) OUTPUT,
    @Message NVARCHAR(500) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @PromotionID INT;
    DECLARE @DiscountType NVARCHAR(20);
    DECLARE @DiscountValue DECIMAL(10,2);
    DECLARE @MaxDiscountAmount DECIMAL(10,2);
    DECLARE @MinOrderValue DECIMAL(10,2);
    DECLARE @UsageLimit INT;
    DECLARE @UsedCount INT;
    DECLARE @StartDate DATETIME2;
    DECLARE @EndDate DATETIME2;
    DECLARE @CreatedByShopID INT;
    
    SET @IsValid = 0;
    SET @DiscountAmount = 0;
    
    -- Get promotion details
    SELECT 
        @PromotionID = PromotionID,
        @DiscountType = DiscountType,
        @DiscountValue = DiscountValue,
        @MaxDiscountAmount = MaxDiscountAmount,
        @MinOrderValue = MinOrderValue,
        @UsageLimit = UsageLimit,
        @UsedCount = UsedCount,
        @StartDate = StartDate,
        @EndDate = EndDate,
        @CreatedByShopID = CreatedByShopID
    FROM dbo.Promotions
    WHERE PromoCode = @PromoCode AND Status = 1;
    
    -- Check if promotion exists
    IF @PromotionID IS NULL
    BEGIN
        SET @Message = N'Mã khuyến mãi không tồn tại hoặc đã bị vô hiệu hóa';
        RETURN;
    END;
    
    -- Check if promotion is valid for this shop
    IF @CreatedByShopID IS NOT NULL AND @CreatedByShopID <> @ShopID
    BEGIN
        SET @Message = N'Mã khuyến mãi không áp dụng cho shop này';
        RETURN;
    END;
    
    -- Check date range
    IF GETDATE() < @StartDate OR GETDATE() > @EndDate
    BEGIN
        SET @Message = N'Mã khuyến mãi đã hết hạn hoặc chưa có hiệu lực';
        RETURN;
    END;
    
    -- Check minimum order value
    IF @OrderSubtotal < @MinOrderValue
    BEGIN
        SET @Message = N'Giá trị đơn hàng chưa đạt mức tối thiểu ' + CAST(@MinOrderValue AS NVARCHAR) + N'đ';
        RETURN;
    END;
    
    -- Check usage limit
    IF @UsageLimit IS NOT NULL AND @UsedCount >= @UsageLimit
    BEGIN
        SET @Message = N'Mã khuyến mãi đã hết lượt sử dụng';
        RETURN;
    END;
    
    -- Calculate discount
    IF @DiscountType = 'Percentage'
    BEGIN
        SET @DiscountAmount = @OrderSubtotal * @DiscountValue / 100;
        IF @MaxDiscountAmount IS NOT NULL AND @DiscountAmount > @MaxDiscountAmount
            SET @DiscountAmount = @MaxDiscountAmount;
    END
    ELSE IF @DiscountType = 'FixedAmount'
    BEGIN
        SET @DiscountAmount = @DiscountValue;
    END
    ELSE IF @DiscountType = 'FreeShip'
    BEGIN
        SET @DiscountAmount = @DiscountValue; -- This should be applied to shipping fee
    END;
    
    SET @IsValid = 1;
    SET @Message = N'Áp dụng mã khuyến mãi thành công';
END;
GO

PRINT N'✓ All stored procedures created successfully.';
GO

-- ================================================================
-- PART 8: CREATE FUNCTIONS
-- ================================================================

PRINT N'==== Creating functions...';
GO

-- Function to calculate order total
CREATE FUNCTION fn_CalculateOrderTotal
(
    @Subtotal DECIMAL(12,2),
    @ShippingFee DECIMAL(12,2),
    @DiscountAmount DECIMAL(12,2)
)
RETURNS DECIMAL(12,2)
AS
BEGIN
    DECLARE @Total DECIMAL(12,2);
    SET @Total = @Subtotal + @ShippingFee - @DiscountAmount;
    IF @Total < 0 SET @Total = 0;
    RETURN @Total;
END;
GO

-- Function to check if user can review a product
CREATE FUNCTION fn_CanUserReviewProduct
(
    @UserID INT,
    @ProductID INT
)
RETURNS BIT
AS
BEGIN
    DECLARE @CanReview BIT = 0;
    
    -- Check if user has purchased and received this product
    IF EXISTS (
        SELECT 1
        FROM dbo.Orders o
        JOIN dbo.OrderDetails od ON o.OrderID = od.OrderID
        JOIN dbo.ProductVariants pv ON od.VariantID = pv.VariantID
        WHERE o.UserID = @UserID
        AND pv.ProductID = @ProductID
        AND o.OrderStatus = 'Completed'
        AND NOT EXISTS (
            SELECT 1 FROM dbo.Reviews r 
            WHERE r.OrderDetailID = od.OrderDetailID
        )
    )
    BEGIN
        SET @CanReview = 1;
    END;
    
    RETURN @CanReview;
END;
GO

-- Function to get user's cart total
CREATE FUNCTION fn_GetCartTotal
(
    @UserID INT
)
RETURNS DECIMAL(12,2)
AS
BEGIN
    DECLARE @Total DECIMAL(12,2) = 0;
    
    SELECT @Total = SUM(pv.Price * ci.Quantity + ISNULL(ToppingTotal, 0))
    FROM dbo.Carts c
    JOIN dbo.CartItems ci ON c.CartID = ci.CartID
    JOIN dbo.ProductVariants pv ON ci.VariantID = pv.VariantID
    OUTER APPLY (
        SELECT SUM(t.AdditionalPrice) AS ToppingTotal
        FROM dbo.CartItem_Toppings cit
        JOIN dbo.Toppings t ON cit.ToppingID = t.ToppingID
        WHERE cit.CartItemID = ci.CartItemID
    ) AS Toppings
    WHERE c.UserID = @UserID;
    
    RETURN ISNULL(@Total, 0);
END;
GO

PRINT N'✓ All functions created successfully.';
GO