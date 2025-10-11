/* ================================================================
   SCRIPT: Complete E-commerce Milk Tea Database - Enhanced Version
   Database: MilkTeaShopDB
   Purpose: Support Spring Boot + Thymeleaf + Bootstrap + JPA + 
            SQLServer + JWT + WebSocket + Cloudinary
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
IF OBJECT_ID('dbo.PromotionProducts','U') IS NOT NULL DROP TABLE dbo.PromotionProducts;
IF OBJECT_ID('dbo.Promotions','U') IS NOT NULL DROP TABLE dbo.Promotions;

IF OBJECT_ID('dbo.CloudinaryAssets','U') IS NOT NULL DROP TABLE dbo.CloudinaryAssets;
IF OBJECT_ID('dbo.ProductImages','U') IS NOT NULL DROP TABLE dbo.ProductImages;
IF OBJECT_ID('dbo.Toppings','U') IS NOT NULL DROP TABLE dbo.Toppings;
IF OBJECT_ID('dbo.ProductVariants','U') IS NOT NULL DROP TABLE dbo.ProductVariants;
IF OBJECT_ID('dbo.Sizes','U') IS NOT NULL DROP TABLE dbo.Sizes;

IF OBJECT_ID('dbo.Products','U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories','U') IS NOT NULL DROP TABLE dbo.Categories;

IF OBJECT_ID('dbo.ChatMessages','U') IS NOT NULL DROP TABLE dbo.ChatMessages;
IF OBJECT_ID('dbo.DeviceTokens','U') IS NOT NULL DROP TABLE dbo.DeviceTokens;
IF OBJECT_ID('dbo.JWTTokens','U') IS NOT NULL DROP TABLE dbo.JWTTokens;
IF OBJECT_ID('dbo.Notifications','U') IS NOT NULL DROP TABLE dbo.Notifications;

IF OBJECT_ID('dbo.Addresses','U') IS NOT NULL DROP TABLE dbo.Addresses;
IF OBJECT_ID('dbo.Employees','U') IS NOT NULL DROP TABLE dbo.Employees;
IF OBJECT_ID('dbo.Customers','U') IS NOT NULL DROP TABLE dbo.Customers;

IF OBJECT_ID('dbo.SystemConfiguration','U') IS NOT NULL DROP TABLE dbo.SystemConfiguration;
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

-- 1. User Account System
CREATE TABLE dbo.Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    PhoneNumber NVARCHAR(20) NULL UNIQUE,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    AvatarURL NVARCHAR(500) NULL,
    LastLogin DATETIME2 NULL,
    LoginAttempts INT DEFAULT 0,
    IsLocked BIT DEFAULT 0,
    LockUntil DATETIME2 NULL,
    TwoFactorEnabled BIT DEFAULT 0,
    TwoFactorSecret NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.Roles (
    RoleID INT PRIMARY KEY,
    RoleName NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE dbo.UserRoles (
    UserID INT NOT NULL,
    RoleID INT NOT NULL,
    PRIMARY KEY (UserID, RoleID),
    CONSTRAINT FK_UserRoles_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_UserRoles_Role FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID)
);

-- 2. Customers and Employees
CREATE TABLE dbo.Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT UNIQUE NOT NULL,
    FullName NVARCHAR(255) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    DateOfBirth DATE NULL,
    Gender TINYINT NULL,
    TotalSpent DECIMAL(15,2) DEFAULT 0,
    LoyaltyPoints INT DEFAULT 0,
    MembershipLevel NVARCHAR(20),
    PreferredContactMethod NVARCHAR(20),
    CONSTRAINT FK_Customer_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.Employees (
    EmployeeID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT UNIQUE NOT NULL,
    FullName NVARCHAR(255) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    CONSTRAINT FK_Employee_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 3. Customer Addresses
CREATE TABLE dbo.Addresses (
    AddressID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    AddressName NVARCHAR(100) NOT NULL,
    FullAddress NVARCHAR(500) NOT NULL,
    PhoneNumber NVARCHAR(20) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    IsDefault BIT NOT NULL DEFAULT 0,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Address_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE
);

-- 4. Categories and Products
CREATE TABLE dbo.Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(255) NOT NULL UNIQUE,
    Description NVARCHAR(MAX) NULL
);

CREATE TABLE dbo.Products (
    ProductID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryID INT NOT NULL,
    ProductName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    SKU NVARCHAR(50) UNIQUE NULL,
    AverageRating DECIMAL(3,2) DEFAULT 0,
    TotalReviews INT DEFAULT 0,
    IsHotDeal BIT DEFAULT 0,
    TrendingScore INT DEFAULT 0,
    ViewCount INT DEFAULT 0,
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);

-- 5. Product Images
CREATE TABLE dbo.ProductImages (
    ImageID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    ImageURL NVARCHAR(500) NOT NULL,
    IsPrimary BIT NOT NULL DEFAULT 0,
    DisplayOrder INT NOT NULL DEFAULT 0,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ProductImage_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 6. Sizes and Product Variants
CREATE TABLE dbo.Sizes (
    SizeID INT IDENTITY(1,1) PRIMARY KEY,
    SizeName NVARCHAR(10) NOT NULL UNIQUE,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1))
);

CREATE TABLE dbo.ProductVariants (
    VariantID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    SizeID INT NOT NULL,
    Price DECIMAL(10,2) NOT NULL CHECK (Price > 0),
    Stock INT NOT NULL DEFAULT 0 CHECK (Stock >= 0),
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    DiscountPrice DECIMAL(10,2) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_ProductVariant_Unique UNIQUE (ProductID, SizeID),
    CONSTRAINT FK_Variant_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT FK_Variant_Size FOREIGN KEY (SizeID) REFERENCES dbo.Sizes(SizeID)
);

-- 7. Toppings
CREATE TABLE dbo.Toppings (
    ToppingID INT IDENTITY(1,1) PRIMARY KEY,
    ToppingName NVARCHAR(255) NOT NULL UNIQUE,
    AdditionalPrice DECIMAL(10,2) NOT NULL CHECK (AdditionalPrice >= 0),
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1))
);

-- 8. Promotions
CREATE TABLE dbo.Promotions (
    PromotionID INT IDENTITY(1,1) PRIMARY KEY,
    PromotionName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    PromoCode NVARCHAR(50) UNIQUE NULL,
    DiscountType NVARCHAR(20),
    DiscountValue DECIMAL(10,2) NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    MinOrderValue DECIMAL(10,2) DEFAULT 0,
    MaxUsageCount INT NULL,
    UsageCount INT DEFAULT 0,
    ApplicableForNewUsers BIT DEFAULT 0,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)),
    CONSTRAINT CK_Promotion_Dates CHECK (EndDate >= StartDate)
);

CREATE TABLE dbo.PromotionProducts (
    PromotionID INT NOT NULL,
    ProductID INT NOT NULL,
    DiscountPercentage INT NOT NULL CHECK (DiscountPercentage BETWEEN 1 AND 100),
    PRIMARY KEY (PromotionID, ProductID),
    CONSTRAINT FK_PromoProduct_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotions(PromotionID) ON DELETE CASCADE,
    CONSTRAINT FK_PromoProduct_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 9. Favorites
CREATE TABLE dbo.Favorites (
    FavoriteID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    ProductID INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Favorite_Unique UNIQUE (CustomerID, ProductID),
    CONSTRAINT FK_Favorite_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE,
    CONSTRAINT FK_Favorite_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 10. Shopping Carts
CREATE TABLE dbo.Carts (
    CartID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (Status IN ('ACTIVE', 'CHECKED_OUT', 'CANCELLED')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Cart_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE
);
CREATE UNIQUE INDEX UX_Carts_Active_OnePerCustomer ON dbo.Carts(CustomerID) WHERE Status = 'ACTIVE';

CREATE TABLE dbo.CartItems (
    CartItemID INT IDENTITY(1,1) PRIMARY KEY,
    CartID INT NOT NULL,
    VariantID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0),
    LineTotal DECIMAL(10,2) NOT NULL CHECK (LineTotal >= 0),
    Notes NVARCHAR(500) NULL,
    CONSTRAINT FK_CartItem_Cart FOREIGN KEY (CartID) REFERENCES dbo.Carts(CartID) ON DELETE CASCADE,
    CONSTRAINT FK_CartItem_Variant FOREIGN KEY (VariantID) REFERENCES dbo.ProductVariants(VariantID)
);

CREATE TABLE dbo.CartItem_Toppings (
    CartItemID INT NOT NULL,
    ToppingID INT NOT NULL,
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0),
    LineTotal DECIMAL(10,2) NOT NULL CHECK (LineTotal >= 0),
    PRIMARY KEY (CartItemID, ToppingID),
    CONSTRAINT FK_CartItemTopping_CartItem FOREIGN KEY (CartItemID) REFERENCES dbo.CartItems(CartItemID) ON DELETE CASCADE,
    CONSTRAINT FK_CartItemTopping_Topping FOREIGN KEY (ToppingID) REFERENCES dbo.Toppings(ToppingID)
);

-- 11. Orders
CREATE TABLE dbo.Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    EmployeeID INT NULL,
    PromotionID INT NULL,
    OrderDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    OrderStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending'
        CHECK (OrderStatus IN ('Pending', 'Processing', 'Delivering', 'Completed', 'Cancelled')),
    PaymentStatus NVARCHAR(30) NOT NULL DEFAULT 'Unpaid'
        CHECK (PaymentStatus IN ('Unpaid', 'Paid')),
    PaymentMethod NVARCHAR(50) NOT NULL DEFAULT 'Cash'
        CHECK (PaymentMethod IN ('Cash', 'Momo', 'VNPay', 'ZaloPay', 'BankTransfer')),
    PaidAt DATETIME2 NULL,
    ShippingAddress NVARCHAR(500) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    RecipientPhone NVARCHAR(20) NOT NULL,
    Subtotal DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (Subtotal >= 0),
    DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (DiscountAmount >= 0),
    ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (ShippingFee >= 0),
    GrandTotal DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (GrandTotal >= 0),
    Notes NVARCHAR(500) NULL,
    TrackingCode NVARCHAR(50) UNIQUE NULL,
    EstimatedDeliveryDate DATE NULL,
    ActualDeliveryDate DATETIME2 NULL,
    ShipperID INT NULL,
    CancellationReason NVARCHAR(500) NULL,
    CancelledAt DATETIME2 NULL,
    CONSTRAINT CK_Order_PaidAt_Logic CHECK (
        (PaymentStatus = 'Paid' AND PaidAt IS NOT NULL) OR
        (PaymentStatus = 'Unpaid' AND PaidAt IS NULL)
    ),
    CONSTRAINT FK_Order_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Order_Employee FOREIGN KEY (EmployeeID) REFERENCES dbo.Employees(EmployeeID),
    CONSTRAINT FK_Order_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotions(PromotionID),
    CONSTRAINT FK_Order_Shipper FOREIGN KEY (ShipperID) REFERENCES dbo.Employees(EmployeeID)
);

-- 12. Order History
CREATE TABLE dbo.OrderHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    PreviousStatus NVARCHAR(30) NULL,
    NewStatus NVARCHAR(30) NOT NULL,
    ChangedBy INT NULL,
    UserType NVARCHAR(10) NOT NULL CHECK (UserType IN ('Employee', 'Customer', 'System')),
    Timestamp DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    Notes NVARCHAR(500) NULL,
    CONSTRAINT FK_OrderHistory_Order FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE
);

-- 13. Order Details
CREATE TABLE dbo.OrderDetails (
    OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    VariantID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0),
    LineDiscount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (LineDiscount >= 0),
    LineTotal DECIMAL(10,2) NOT NULL CHECK (LineTotal >= 0),
    CONSTRAINT FK_OrderDetail_Order FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetail_Variant FOREIGN KEY (VariantID) REFERENCES dbo.ProductVariants(VariantID)
);

CREATE TABLE dbo.OrderDetail_Toppings (
    OrderDetailID INT NOT NULL,
    ToppingID INT NOT NULL,
    Quantity INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    UnitPrice DECIMAL(10,2) NOT NULL CHECK (UnitPrice >= 0),
    LineTotal DECIMAL(10,2) NOT NULL CHECK (LineTotal >= 0),
    PRIMARY KEY (OrderDetailID, ToppingID),
    CONSTRAINT FK_OrderDetailTopping_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetailTopping_Topping FOREIGN KEY (ToppingID) REFERENCES dbo.Toppings(ToppingID)
);

-- 14. Reviews
CREATE TABLE dbo.Reviews (
    ReviewID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    OrderDetailID INT NOT NULL,
    Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(MAX) NULL,
    ReviewDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Review_OnePerLine UNIQUE (CustomerID, OrderDetailID),
    CONSTRAINT FK_Review_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Review_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID)
);

-- 15. Cloudinary Assets
CREATE TABLE dbo.CloudinaryAssets (
    AssetID INT IDENTITY(1,1) PRIMARY KEY,
    PublicID NVARCHAR(255) NOT NULL UNIQUE,
    CloudinaryURL NVARCHAR(500) NOT NULL,
    EntityType NVARCHAR(50) NOT NULL,
    EntityID INT NOT NULL,
    UploadedBy INT NULL,
    UploadedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_CloudinaryAssets_User FOREIGN KEY (UploadedBy) REFERENCES dbo.Users(UserID)
);

-- 16. Notifications
CREATE TABLE dbo.Notifications (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    Type NVARCHAR(50) NOT NULL,
    RelatedOrderID INT NULL,
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReadAt DATETIME2 NULL,
    CONSTRAINT FK_Notification_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_Notification_Order FOREIGN KEY (RelatedOrderID) REFERENCES dbo.Orders(OrderID) ON DELETE SET NULL
);

-- 17. Chat Messages
CREATE TABLE dbo.ChatMessages (
    MessageID INT IDENTITY(1,1) PRIMARY KEY,
    SenderID INT NOT NULL,
    ReceiverID INT NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    ChatImageURL NVARCHAR(500) NULL,
    MessageType NVARCHAR(20) NOT NULL,
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ChatMessage_Sender FOREIGN KEY (SenderID) REFERENCES dbo.Users(UserID),
    CONSTRAINT FK_ChatMessage_Receiver FOREIGN KEY (ReceiverID) REFERENCES dbo.Users(UserID)
);

-- 18. Device Tokens
CREATE TABLE dbo.DeviceTokens (
    TokenID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    DeviceToken NVARCHAR(500) NOT NULL,
    DeviceType NVARCHAR(20),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    LastUsedAt DATETIME2 NULL,
    CONSTRAINT FK_DeviceToken_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 19. JWT Tokens
CREATE TABLE dbo.JWTTokens (
    TokenID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    AccessToken NVARCHAR(MAX) NOT NULL,
    RefreshToken NVARCHAR(MAX) NULL,
    TokenType NVARCHAR(50),
    IssuedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ExpiresAt DATETIME2 NOT NULL,
    IsRevoked BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_JWTToken_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 20. System Configuration
CREATE TABLE dbo.SystemConfiguration (
    ConfigID INT IDENTITY(1,1) PRIMARY KEY,
    ConfigKey NVARCHAR(255) NOT NULL UNIQUE,
    ConfigValue NVARCHAR(MAX) NOT NULL,
    Description NVARCHAR(500) NULL,
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

GO

PRINT N'✓ All 20 tables created successfully.';
GO

-- ================================================================
-- PART 3: CREATE PERFORMANCE INDEXES
-- ================================================================

PRINT N'==== Creating performance indexes...';
GO

-- User related indexes
CREATE INDEX IX_Users_Email ON dbo.Users(Email);
CREATE INDEX IX_Users_PhoneNumber ON dbo.Users(PhoneNumber) WHERE PhoneNumber IS NOT NULL;
CREATE INDEX IX_Users_Username ON dbo.Users(Username);

-- Customer & Employee indexes
CREATE INDEX IX_Customers_UserID ON dbo.Customers(UserID);
CREATE INDEX IX_Addresses_CustomerID ON dbo.Addresses(CustomerID);

-- Product indexes
CREATE INDEX IX_Products_CategoryID_Status ON dbo.Products(CategoryID, Status);
CREATE INDEX IX_Products_SKU ON dbo.Products(SKU);
CREATE INDEX IX_ProductVariants_ProductID_Status ON dbo.ProductVariants(ProductID, Status);
CREATE INDEX IX_ProductVariants_Stock ON dbo.ProductVariants(Stock);
CREATE INDEX IX_ProductImages_ProductID ON dbo.ProductImages(ProductID);

-- Order indexes
CREATE INDEX IX_Orders_CustomerID_OrderDate ON dbo.Orders(CustomerID, OrderDate DESC);
CREATE INDEX IX_Orders_OrderStatus_OrderDate ON dbo.Orders(OrderStatus, OrderDate DESC);
CREATE INDEX IX_Orders_TrackingCode ON dbo.Orders(TrackingCode);
CREATE INDEX IX_Orders_ShipperID ON dbo.Orders(ShipperID);
CREATE INDEX IX_Orders_PaymentStatus ON dbo.Orders(PaymentStatus);

-- Order details & history
CREATE INDEX IX_OrderHistory_OrderID_Timestamp ON dbo.OrderHistory(OrderID, Timestamp DESC);
CREATE INDEX IX_OrderDetails_OrderID ON dbo.OrderDetails(OrderID);
CREATE INDEX IX_OrderDetails_VariantID ON dbo.OrderDetails(VariantID);

-- Review indexes
CREATE INDEX IX_Reviews_OrderDetailID ON dbo.Reviews(OrderDetailID);
CREATE INDEX IX_Reviews_CustomerID ON dbo.Reviews(CustomerID);
CREATE INDEX IX_Reviews_Rating ON dbo.Reviews(Rating);

-- Cart indexes
CREATE INDEX IX_Carts_CustomerID_Status ON dbo.Carts(CustomerID, Status);
CREATE INDEX IX_CartItems_CartID ON dbo.CartItems(CartID);
CREATE INDEX IX_CartItems_VariantID ON dbo.CartItems(VariantID);

-- Promotion indexes
CREATE INDEX IX_PromotionProducts_ProductID ON dbo.PromotionProducts(ProductID);
CREATE INDEX IX_Promotions_Status_Dates ON dbo.Promotions(Status, StartDate, EndDate);
CREATE INDEX IX_Promotions_PromoCode ON dbo.Promotions(PromoCode);

-- Favorites indexes
CREATE INDEX IX_Favorites_CustomerID ON dbo.Favorites(CustomerID);
CREATE INDEX IX_Favorites_ProductID ON dbo.Favorites(ProductID);

-- WebSocket & Real-time indexes
CREATE INDEX IX_ChatMessages_ReceiverID_IsRead ON dbo.ChatMessages(ReceiverID, IsRead);
CREATE INDEX IX_ChatMessages_CreatedAt ON dbo.ChatMessages(CreatedAt DESC);
CREATE INDEX IX_ChatMessages_SenderID ON dbo.ChatMessages(SenderID);

CREATE INDEX IX_Notifications_UserID_IsRead ON dbo.Notifications(UserID, IsRead);
CREATE INDEX IX_Notifications_CreatedAt ON dbo.Notifications(CreatedAt DESC);

-- Cloudinary & JWT indexes
CREATE INDEX IX_CloudinaryAssets_EntityType ON dbo.CloudinaryAssets(EntityType, EntityID);
CREATE INDEX IX_JWTTokens_UserID_ExpiresAt ON dbo.JWTTokens(UserID, ExpiresAt);
CREATE INDEX IX_JWTTokens_IsRevoked ON dbo.JWTTokens(IsRevoked);

-- Device token indexes
CREATE INDEX IX_DeviceTokens_UserID ON dbo.DeviceTokens(UserID);

GO

PRINT N'✓ All performance indexes created successfully.';
GO

-- ================================================================
-- PART 4: CREATE TRIGGERS
-- ================================================================

PRINT N'==== Creating triggers...';
GO

-- Trigger 1: Log order status changes
CREATE TRIGGER dbo.trg_LogOrderStatusChange
ON dbo.Orders
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    -- On INSERT (new order creation)
    INSERT INTO dbo.OrderHistory (OrderID, PreviousStatus, NewStatus, UserType, Notes)
    SELECT 
        i.OrderID,
        NULL,
        i.OrderStatus,
        'System',
        'Order created'
    FROM inserted i
    WHERE NOT EXISTS (SELECT 1 FROM deleted WHERE OrderID = i.OrderID);
    
    -- On UPDATE (only log if status changes)
    INSERT INTO dbo.OrderHistory (OrderID, PreviousStatus, NewStatus, ChangedBy, UserType, Notes)
    SELECT 
        i.OrderID,
        d.OrderStatus,
        i.OrderStatus,
        i.EmployeeID,
        CASE WHEN i.EmployeeID IS NOT NULL THEN 'Employee' ELSE 'System' END,
        'Order status updated'
    FROM inserted i
    INNER JOIN deleted d ON i.OrderID = d.OrderID
    WHERE i.OrderStatus <> d.OrderStatus;
END;
GO

-- Trigger 2: Update the 'UpdatedAt' timestamp
CREATE TRIGGER dbo.trg_SetUpdatedAt_Addresses
ON dbo.Addresses
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(UpdatedAt) RETURN;
    UPDATE dbo.Addresses
    SET UpdatedAt = SYSUTCDATETIME()
    FROM inserted i
    WHERE dbo.Addresses.AddressID = i.AddressID;
END;
GO

-- Trigger 3: Update stock after order is placed
CREATE TRIGGER trg_UpdateStockAfterOrder
ON dbo.OrderDetails
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE pv
    SET pv.Stock = pv.Stock - i.Quantity
    FROM dbo.ProductVariants pv
    INNER JOIN inserted i ON pv.VariantID = i.VariantID;
END;
GO

-- Trigger 4: Update product trending score
CREATE TRIGGER trg_UpdateProductTrending
ON dbo.OrderDetails
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.Products
    SET TrendingScore = TrendingScore + 1
    FROM inserted i
    INNER JOIN dbo.ProductVariants pv ON i.VariantID = pv.VariantID
    WHERE dbo.Products.ProductID = pv.ProductID;
END;
GO

-- Trigger 5: Create notification on order status update
CREATE TRIGGER trg_CreateNotificationOnOrderUpdate
ON dbo.OrderHistory
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.Notifications (UserID, Title, Message, Type, RelatedOrderID, CreatedAt)
    SELECT 
        c.UserID,
        N'Cập nhật đơn hàng',
        N'Đơn hàng #' + CAST(i.OrderID AS NVARCHAR(20)) + N' có trạng thái: ' + i.NewStatus,
        'OrderStatus',
        i.OrderID,
        SYSUTCDATETIME()
    FROM inserted i
    INNER JOIN dbo.Orders o ON i.OrderID = o.OrderID
    INNER JOIN dbo.Customers c ON o.CustomerID = c.CustomerID
    WHERE i.NewStatus <> i.PreviousStatus;
END;
GO

-- Trigger 6: Update product variant timestamps
CREATE TRIGGER trg_UpdateProductVariantsTimestamp
ON dbo.ProductVariants
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(UpdatedAt) RETURN;
    UPDATE dbo.ProductVariants
    SET UpdatedAt = SYSUTCDATETIME()
    FROM inserted i
    WHERE dbo.ProductVariants.VariantID = i.VariantID;
END;
GO

-- Trigger 7: Update user timestamps
CREATE TRIGGER trg_UpdateUserTimestamp
ON dbo.Users
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(UpdatedAt) RETURN;
    UPDATE dbo.Users
    SET UpdatedAt = SYSUTCDATETIME()
    FROM inserted i
    WHERE dbo.Users.UserID = i.UserID;
END;
GO

-- Trigger 8: Update cart timestamp
CREATE TRIGGER trg_UpdateCartTimestamp
ON dbo.Carts
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(UpdatedAt) RETURN;
    UPDATE dbo.Carts
    SET UpdatedAt = SYSUTCDATETIME()
    FROM inserted i
    WHERE dbo.Carts.CartID = i.CartID;
END;
GO

-- Trigger 9: Update customer loyalty points on order completion
CREATE TRIGGER trg_UpdateLoyaltyPoints
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE c
    SET c.LoyaltyPoints = c.LoyaltyPoints + CAST(i.GrandTotal / 1000 AS INT),
        c.TotalSpent = c.TotalSpent + i.GrandTotal
    FROM dbo.Customers c
    INNER JOIN inserted i ON c.CustomerID = i.CustomerID
    INNER JOIN deleted d ON i.OrderID = d.OrderID
    WHERE i.OrderStatus = 'Completed' AND d.OrderStatus <> 'Completed';
END;
GO

-- Trigger 10: Update promotion usage count
CREATE TRIGGER trg_UpdatePromotionUsageCount
ON dbo.Orders
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET p.UsageCount = p.UsageCount + 1
    FROM dbo.Promotions p
    INNER JOIN inserted i ON p.PromotionID = i.PromotionID
    WHERE i.PromotionID IS NOT NULL;
END;
GO

PRINT N'✓ All triggers created successfully.';
GO

-- ================================================================
-- PART 5: CREATE STORED PROCEDURES
-- ================================================================

PRINT N'==== Creating stored procedures...';
GO

-- SP 1: Register a new customer account
CREATE PROCEDURE dbo.sp_RegisterUser
    @Username NVARCHAR(50),
    @PasswordHash NVARCHAR(255),
    @Email NVARCHAR(255),
    @FullName NVARCHAR(255),
    @PhoneNumber NVARCHAR(20) = NULL,
    @UserID INT OUTPUT,
    @CustomerID INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        IF EXISTS (SELECT 1 FROM dbo.Users WHERE Username = @Username)
            THROW 50001, 'Username already exists.', 1;
        IF EXISTS (SELECT 1 FROM dbo.Users WHERE Email = @Email)
            THROW 50002, 'Email is already in use.', 1;
            
        INSERT INTO dbo.Users (Username, PasswordHash, Email, PhoneNumber)
        VALUES (@Username, @PasswordHash, @Email, @PhoneNumber);
        SET @UserID = SCOPE_IDENTITY();
        
        INSERT INTO dbo.UserRoles (UserID, RoleID) VALUES (@UserID, 2);
        
        INSERT INTO dbo.Customers (UserID, FullName)
        VALUES (@UserID, @FullName);
        SET @CustomerID = SCOPE_IDENTITY();
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- SP 2: Set default address for customer
CREATE PROCEDURE dbo.sp_SetDefaultAddress
    @AddressID INT,
    @CustomerID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.Addresses WHERE AddressID = @AddressID AND CustomerID = @CustomerID AND Status = 1)
            THROW 50003, 'Address not found or does not belong to this customer.', 1;
        
        UPDATE dbo.Addresses SET IsDefault = 0 WHERE CustomerID = @CustomerID;
        UPDATE dbo.Addresses SET IsDefault = 1 WHERE AddressID = @AddressID;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- SP 3: Add item to active cart
CREATE PROCEDURE dbo.sp_AddToCart
    @CustomerID INT,
    @VariantID INT,
    @Quantity INT,
    @Notes NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CartID INT, @UnitPrice DECIMAL(10,2), @Stock INT;

    BEGIN TRANSACTION;
    BEGIN TRY
        -- Get active cart or create new one
        SELECT @CartID = CartID FROM dbo.Carts WHERE CustomerID = @CustomerID AND Status = 'ACTIVE';
        IF @CartID IS NULL
        BEGIN
            INSERT INTO dbo.Carts (CustomerID) VALUES (@CustomerID);
            SET @CartID = SCOPE_IDENTITY();
        END

        -- Check stock and get price
        SELECT @UnitPrice = Price, @Stock = Stock FROM dbo.ProductVariants WHERE VariantID = @VariantID AND Status = 1;
        IF @UnitPrice IS NULL
            THROW 50004, 'Product variant is not available.', 1;
        IF @Stock < @Quantity
            THROW 50005, 'Insufficient stock available.', 1;

        -- Add or update cart item
        IF EXISTS (SELECT 1 FROM dbo.CartItems WHERE CartID = @CartID AND VariantID = @VariantID)
        BEGIN
            UPDATE dbo.CartItems
            SET Quantity = Quantity + @Quantity,
                LineTotal = (Quantity + @Quantity) * UnitPrice
            WHERE CartID = @CartID AND VariantID = @VariantID;
        END
        ELSE
        BEGIN
            INSERT INTO dbo.CartItems (CartID, VariantID, Quantity, UnitPrice, LineTotal, Notes)
            VALUES (@CartID, @VariantID, @Quantity, @UnitPrice, @Quantity * @UnitPrice, @Notes);
        END
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

-- SP 4: Create order from active cart
CREATE PROCEDURE dbo.sp_CreateOrderFromCart
    @CustomerID INT,
    @AddressID INT,
    @PaymentMethod NVARCHAR(50),
    @ShippingFee DECIMAL(12,2),
    @Notes NVARCHAR(500) = NULL,
    @OrderID INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @CartID INT, @Subtotal DECIMAL(12,2), @GrandTotal DECIMAL(12,2);
    DECLARE @ShippingAddress NVARCHAR(500), @RecipientName NVARCHAR(255), @RecipientPhone NVARCHAR(20);

    BEGIN TRANSACTION;
    BEGIN TRY
        -- Validate Cart and Address
        SELECT @CartID = CartID FROM dbo.Carts WHERE CustomerID = @CustomerID AND Status = 'ACTIVE';
        IF @CartID IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.CartItems WHERE CartID = @CartID)
            THROW 50006, 'Active cart is empty.', 1;

        SELECT @ShippingAddress = FullAddress, @RecipientName = RecipientName, @RecipientPhone = PhoneNumber
        FROM dbo.Addresses WHERE AddressID = @AddressID AND CustomerID = @CustomerID;
        IF @ShippingAddress IS NULL
            THROW 50007, 'Invalid shipping address.', 1;

        -- Calculate totals
        SELECT @Subtotal = SUM(LineTotal) FROM dbo.CartItems WHERE CartID = @CartID;
        SET @GrandTotal = @Subtotal + @ShippingFee;

        -- Create Order
        INSERT INTO dbo.Orders (CustomerID, PaymentMethod, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, GrandTotal, Notes)
        VALUES (@CustomerID, @PaymentMethod, @ShippingAddress, @RecipientName, @RecipientPhone, @Subtotal, @ShippingFee, @GrandTotal, @Notes);
        SET @OrderID = SCOPE_IDENTITY();

        -- Copy CartItems to OrderDetails
        INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, LineTotal)
        SELECT @OrderID, VariantID, Quantity, UnitPrice, LineTotal
        FROM dbo.CartItems WHERE CartID = @CartID;

        -- Update Cart status
        UPDATE dbo.Carts SET Status = 'CHECKED_OUT' WHERE CartID = @CartID;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

-- SP 5: Get order summary by CustomerID
CREATE PROCEDURE dbo.sp_GetOrderSummary
    @CustomerID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT
        o.OrderID,
        o.OrderDate,
        o.OrderStatus,
        o.PaymentStatus,
        o.GrandTotal,
        (SELECT COUNT(*) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS ItemCount,
        o.ShippingAddress,
        o.RecipientName
    FROM dbo.Orders o
    WHERE o.CustomerID = @CustomerID
    ORDER BY o.OrderDate DESC;
END;
GO

-- SP 6: Update order status
CREATE PROCEDURE dbo.sp_UpdateOrderStatus
    @OrderID INT,
    @NewStatus NVARCHAR(30),
    @EmployeeID INT = NULL,
    @Notes NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.Orders WHERE OrderID = @OrderID)
            THROW 50008, 'Order not found.', 1;
            
        UPDATE dbo.Orders 
        SET OrderStatus = @NewStatus,
            EmployeeID = ISNULL(@EmployeeID, EmployeeID)
        WHERE OrderID = @OrderID;
        
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- SP 7: Add review
CREATE PROCEDURE dbo.sp_AddReview
    @CustomerID INT,
    @OrderDetailID INT,
    @Rating INT,
    @Comment NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        IF @Rating < 1 OR @Rating > 5
            THROW 50009, 'Rating must be between 1 and 5.', 1;
            
        INSERT INTO dbo.Reviews (CustomerID, OrderDetailID, Rating, Comment)
        VALUES (@CustomerID, @OrderDetailID, @Rating, @Comment);
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END;
GO

-- SP 8: Get product statistics
CREATE PROCEDURE dbo.sp_GetProductStatistics
    @ProductID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT
        p.ProductID,
        p.ProductName,
        c.CategoryName,
        COUNT(DISTINCT v.VariantID) AS VariantCount,
        ISNULL(MIN(v.Price), 0) AS LowestPrice,
        ISNULL(MAX(v.Price), 0) AS HighestPrice,
        SUM(ISNULL(v.Stock, 0)) AS TotalStock,
        COUNT(DISTINCT r.ReviewID) AS ReviewCount,
        ISNULL(AVG(CAST(r.Rating AS FLOAT)), 0) AS AverageRating
    FROM dbo.Products p
    INNER JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
    LEFT JOIN dbo.ProductVariants v ON p.ProductID = v.ProductID AND v.Status = 1
    LEFT JOIN dbo.OrderDetails od ON v.VariantID = od.VariantID
    LEFT JOIN dbo.Reviews r ON od.OrderDetailID = r.OrderDetailID
    WHERE p.ProductID = @ProductID
    GROUP BY p.ProductID, p.ProductName, c.CategoryName;
END;
GO

PRINT N'✓ All stored procedures created successfully.';
GO

-- ================================================================
-- PART 6: CREATE VIEWS
-- ================================================================

PRINT N'==== Creating views...';
GO

-- View 1: Detailed order information
CREATE VIEW dbo.v_OrderSummary
AS
SELECT
    o.OrderID,
    o.OrderDate,
    o.OrderStatus,
    o.PaymentStatus,
    c.CustomerID,
    c.FullName AS CustomerName,
    u.Email AS CustomerEmail,
    e.FullName AS EmployeeName,
    o.ShippingAddress,
    o.GrandTotal,
    o.TrackingCode,
    (SELECT COUNT(*) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS ItemCount
FROM dbo.Orders o
INNER JOIN dbo.Customers c ON o.CustomerID = c.CustomerID
INNER JOIN dbo.Users u ON c.UserID = u.UserID
LEFT JOIN dbo.Employees e ON o.EmployeeID = e.EmployeeID;
GO

-- View 2: Product statistics
CREATE VIEW dbo.v_ProductStatistics
AS
SELECT
    p.ProductID,
    p.ProductName,
    c.CategoryName,
    COUNT(DISTINCT v.VariantID) AS VariantCount,
    ISNULL(MIN(v.Price), 0) AS LowestPrice,
    ISNULL(MAX(v.Price), 0) AS HighestPrice,
    SUM(ISNULL(v.Stock, 0)) AS TotalStock,
    COUNT(DISTINCT r.ReviewID) AS ReviewCount,
    ISNULL(AVG(CAST(r.Rating AS FLOAT)), 0) AS AverageRating,
    p.TrendingScore,
    p.ViewCount,
    p.IsHotDeal,
    (SELECT TOP 1 ImageURL FROM dbo.ProductImages WHERE ProductID = p.ProductID AND IsPrimary = 1) AS PrimaryImage
FROM dbo.Products p
INNER JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
LEFT JOIN dbo.ProductVariants v ON p.ProductID = v.ProductID AND v.Status = 1
LEFT JOIN dbo.OrderDetails od ON v.VariantID = od.VariantID
LEFT JOIN dbo.Reviews r ON od.OrderDetailID = r.OrderDetailID
WHERE p.Status = 1
GROUP BY p.ProductID, p.ProductName, c.CategoryName, p.TrendingScore, p.ViewCount, p.IsHotDeal;
GO

-- View 3: Active promotions
CREATE VIEW dbo.v_ActivePromotions
AS
SELECT
    p.PromotionID,
    p.PromotionName,
    p.Description,
    p.PromoCode,
    p.DiscountType,
    p.DiscountValue,
    p.StartDate,
    p.EndDate,
    p.MinOrderValue,
    p.MaxUsageCount,
    p.UsageCount,
    CAST(CASE WHEN GETDATE() BETWEEN p.StartDate AND p.EndDate THEN 1 ELSE 0 END AS BIT) AS IsActive
FROM dbo.Promotions p
WHERE p.Status = 1 AND GETDATE() BETWEEN p.StartDate AND p.EndDate;
GO

-- View 4: Customer purchase history
CREATE VIEW dbo.v_CustomerPurchaseHistory
AS
SELECT
    c.CustomerID,
    c.FullName,
    u.Email,
    COUNT(DISTINCT o.OrderID) AS TotalOrders,
    SUM(o.GrandTotal) AS TotalSpent,
    AVG(o.GrandTotal) AS AverageOrderValue,
    MAX(o.OrderDate) AS LastOrderDate,
    c.LoyaltyPoints,
    c.MembershipLevel
FROM dbo.Customers c
INNER JOIN dbo.Users u ON c.UserID = u.UserID
LEFT JOIN dbo.Orders o ON c.CustomerID = o.CustomerID
GROUP BY c.CustomerID, c.FullName, u.Email, c.LoyaltyPoints, c.MembershipLevel;
GO

-- View 5: Pending orders
CREATE VIEW dbo.v_PendingOrders
AS
SELECT
    o.OrderID,
    o.OrderDate,
    c.FullName AS CustomerName,
    u.PhoneNumber,
    o.ShippingAddress,
    o.GrandTotal,
    o.OrderStatus,
    DATEDIFF(HOUR, o.OrderDate, GETDATE()) AS HoursSinceOrder
FROM dbo.Orders o
INNER JOIN dbo.Customers c ON o.CustomerID = c.CustomerID
INNER JOIN dbo.Users u ON c.UserID = u.UserID
WHERE o.OrderStatus IN ('Pending', 'Processing');
GO

-- View 6: Top selling products
CREATE VIEW dbo.v_TopSellingProducts
AS
SELECT TOP 10
    p.ProductID,
    p.ProductName,
    c.CategoryName,
    COUNT(od.OrderDetailID) AS TotalSales,
    SUM(od.Quantity) AS QuantitySold,
    SUM(od.LineTotal) AS TotalRevenue,
    AVG(CAST(r.Rating AS FLOAT)) AS AverageRating
FROM dbo.Products p
INNER JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
INNER JOIN dbo.ProductVariants pv ON p.ProductID = pv.ProductID
INNER JOIN dbo.OrderDetails od ON pv.VariantID = od.VariantID
LEFT JOIN dbo.Reviews r ON od.OrderDetailID = r.OrderDetailID
WHERE p.Status = 1
GROUP BY p.ProductID, p.ProductName, c.CategoryName
ORDER BY TotalSales DESC;
GO

-- View 7: Unread notifications
CREATE VIEW dbo.v_UnreadNotifications
AS
SELECT
    n.NotificationID,
    n.UserID,
    n.Title,
    n.Message,
    n.Type,
    n.CreatedAt,
    n.RelatedOrderID
FROM dbo.Notifications n
WHERE n.IsRead = 0;
-- SỬA LỖI: Đã xóa "ORDER BY n.CreatedAt DESC;"
GO

-- View 8: Unread chat messages
CREATE VIEW dbo.v_UnreadChatMessages
AS
SELECT
    cm.MessageID,
    cm.SenderID,
    cm.ReceiverID,
    u.Username AS SenderName,
    cm.Message,
    cm.MessageType,
    cm.CreatedAt
FROM dbo.ChatMessages cm
INNER JOIN dbo.Users u ON cm.SenderID = u.UserID
WHERE cm.IsRead = 0;
-- SỬA LỖI: Đã xóa "ORDER BY cm.CreatedAt DESC;"
GO

PRINT N'✓ All views created successfully.';
GO

-- ================================================================
-- PART 7: CREATE FUNCTIONS
-- ================================================================

PRINT N'==== Creating functions...';
GO

-- Function 1: Calculate discounted price
CREATE FUNCTION dbo.fn_CalculateDiscountedPrice(@ProductID INT, @BasePrice DECIMAL(10,2))
RETURNS DECIMAL(10,2)
AS
BEGIN
    DECLARE @DiscountPercentage INT;
    DECLARE @FinalPrice DECIMAL(10,2);
    
    SELECT TOP 1 @DiscountPercentage = pp.DiscountPercentage
    FROM dbo.PromotionProducts pp
    INNER JOIN dbo.Promotions p ON pp.PromotionID = p.PromotionID
    WHERE pp.ProductID = @ProductID
      AND GETDATE() BETWEEN p.StartDate AND p.EndDate
      AND p.Status = 1
    ORDER BY pp.DiscountPercentage DESC;
    
    IF @DiscountPercentage IS NOT NULL
        SET @FinalPrice = @BasePrice * (100 - @DiscountPercentage) / 100.0;
    ELSE
        SET @FinalPrice = @BasePrice;
    
    RETURN @FinalPrice;
END;
GO

-- Function 2: Check if promotion is active
CREATE FUNCTION dbo.fn_IsPromotionActive(@PromotionID INT)
RETURNS BIT
AS
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM dbo.Promotions 
        WHERE PromotionID = @PromotionID 
          AND GETDATE() BETWEEN StartDate AND EndDate
          AND Status = 1
    )
        RETURN 1;
    
    RETURN 0;
END;
GO

-- Function 3: Calculate total cart amount
CREATE FUNCTION dbo.fn_CalculateCartTotal(@CartID INT)
RETURNS DECIMAL(12,2)
AS
BEGIN
    DECLARE @Total DECIMAL(12,2) = 0;
    
    SELECT @Total = ISNULL(SUM(ci.LineTotal), 0) +
                    ISNULL((SELECT SUM(cit.LineTotal) FROM dbo.CartItem_Toppings cit WHERE cit.CartItemID IN (SELECT CartItemID FROM dbo.CartItems WHERE CartID = @CartID)), 0)
    FROM dbo.CartItems ci
    WHERE ci.CartID = @CartID;
    
    RETURN @Total;
END;
GO

-- Function 4: Get customer membership level
CREATE FUNCTION dbo.fn_GetMembershipLevel(@TotalSpent DECIMAL(15,2))
RETURNS NVARCHAR(20)
AS
BEGIN
    DECLARE @Level NVARCHAR(20);
    
    IF @TotalSpent >= 10000000
        SET @Level = 'Platinum';
    ELSE IF @TotalSpent >= 5000000
        SET @Level = 'Gold';
    ELSE IF @TotalSpent >= 1000000
        SET @Level = 'Silver';
    ELSE
        SET @Level = 'Bronze';
    
    RETURN @Level;
END;
GO

PRINT N'✓ All functions created successfully.';
GO

-- ================================================================
-- PART 8: INSERT SAMPLE DATA
-- ================================================================

PRINT N'==== Inserting sample data...';
GO

-- 1. Insert Roles
INSERT INTO dbo.Roles (RoleID, RoleName) VALUES 
(1, 'Admin'), (2, 'Customer'), (3, 'Employee'), (4, 'Shipper');

-- 2. Insert Users
INSERT INTO dbo.Users (Username, PasswordHash, Email, PhoneNumber, Status) VALUES
('admin', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'admin@alotea.com', '0900000001', 1),
('nhanvien1', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'nhanvien1@alotea.com', '0900000002', 1),
('nhanvien2', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'nhanvien2@alotea.com', '0900000003', 1),
('minhanh', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'minhanh@email.com', '0912345678', 1),
('baotran', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'baotran@email.com', '0987654321', 1),
('trungkien', '$2a$12$G.p/iP.f5h1x/A7Q5w8f1uL.IPoTjSF7xT4O0XkI4w2kC7gq3g/O2', 'trungkien@email.com', '0911223344', 1);

-- 3. Assign roles
INSERT INTO dbo.UserRoles (UserID, RoleID) VALUES
(1, 1), (2, 3), (3, 3), (4, 2), (5, 2), (6, 2);

-- 4. Create employees and customers
INSERT INTO dbo.Employees (UserID, FullName, Status) VALUES
(2, N'Nguyễn Văn An', 1),
(3, N'Trần Thị Bích', 1);

INSERT INTO dbo.Customers (UserID, FullName, Status, MembershipLevel) VALUES
(4, N'Lê Minh Anh', 1, 'Gold'),
(5, N'Phạm Bảo Trân', 1, 'Silver'),
(6, N'Đặng Trung Kiên', 1, 'Bronze');

-- 5. Add addresses
INSERT INTO dbo.Addresses (CustomerID, AddressName, FullAddress, PhoneNumber, RecipientName, IsDefault) VALUES
(1, N'Nhà', N'123 Đường Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', '0912345678', N'Lê Minh Anh', 1),
(1, N'Công ty', N'456 Đường Nguyễn Huệ, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', '0912345678', N'Lê Minh Anh', 0),
(2, N'Nhà', N'789 Đường Pasteur, Phường 6, Quận 3, TP. Hồ Chí Minh', '0987654321', N'Phạm Bảo Trân', 1);

-- 6. Insert sizes
INSERT INTO dbo.Sizes (SizeName, Status) VALUES ('S', 1), ('M', 1), ('L', 1);

-- 7. Insert categories
INSERT INTO dbo.Categories (CategoryName, Description) VALUES 
(N'Trà Sữa', N'Các loại trà sữa truyền thống'),
(N'Trà Trái Cây', N'Trà kết hợp với trái cây tươi'),
(N'Cà Phê', N'Các loại cà phê specialty'),
(N'Sinh Tố', N'Sinh tố trái cây tươi ngon');

-- 8. Insert products
INSERT INTO dbo.Products (CategoryID, ProductName, Description, Status, SKU, IsHotDeal) VALUES
(1, N'Trà Sữa Trân Châu Đường Đen', N'Hương vị trà sữa truyền thống kết hợp với trân châu đường đen.', 1, 'TS001', 1),
(1, N'Trà Sữa Oolong Kem Phô Mai', N'Trà Oolong thơm dịu phủ lớp kem phô mai mặn béo.', 1, 'TS002', 0),
(2, N'Trà Chanh Dây Macchiato', N'Vị chua thanh của chanh dây tươi hòa quyện cùng Macchiato.', 1, 'TT001', 1),
(2, N'Trà Vải Hoa Hồng', N'Sự kết hợp tinh tế giữa trà đen, vải và hương hoa hồng.', 1, 'TT002', 0),
(3, N'Cà Phê Sữa Đá', N'Cà phê Robusta đậm chất Việt, pha phin truyền thống.', 1, 'CF001', 0),
(4, N'Sinh Tố Xoài Sữa Chua', N'Xoài cát chín xay mịn cùng sữa chua Hy Lạp.', 1, 'ST001', 1);

-- 9. Insert product variants
INSERT INTO dbo.ProductVariants (ProductID, SizeID, Price, Stock, Status) VALUES
(1, 1, 35000, 100, 1), (1, 2, 42000, 150, 1), (1, 3, 48000, 80, 1),
(2, 2, 49000, 120, 1), (2, 3, 55000, 70, 1),
(3, 2, 45000, 90, 1),
(4, 2, 48000, 95, 1),
(5, 1, 25000, 200, 1), (5, 2, 30000, 200, 1),
(6, 2, 55000, 60, 1);

-- 10. Insert product images
INSERT INTO dbo.ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder) VALUES
(1, '/images/products/ts-duong-den.jpg', 1, 1),
(2, '/images/products/ts-oolong-kem-pho-mai.jpg', 1, 1),
(3, '/images/products/tra-chanh-day.jpg', 1, 1),
(4, '/images/products/tra-vai.jpg', 1, 1),
(4, '/images/products/tra-vai-2.jpg', 0, 2),
(5, '/images/products/cafe-sua-da.jpg', 1, 1),
(6, '/images/products/sinh-to-xoai.jpg', 1, 1);

-- 11. Insert toppings
INSERT INTO dbo.Toppings (ToppingName, AdditionalPrice, Status) VALUES 
(N'Trân Châu Đường Đen', 5000, 1),
(N'Trân Châu Trắng', 5000, 1),
(N'Thạch Dừa', 5000, 1),
(N'Pudding', 7000, 1),
(N'Kem Phô Mai', 10000, 1);

-- 12. Insert promotions
INSERT INTO dbo.Promotions (PromotionName, Description, PromoCode, DiscountType, DiscountValue, StartDate, EndDate, MinOrderValue, Status) VALUES
(N'Giảm giá mừng khai trương', N'Giảm 20% cho các sản phẩm trà sữa', 'KHAITRA20', 'Percentage', 20, CAST(GETDATE() AS DATE), CAST(DATEADD(MONTH, 1, GETDATE()) AS DATE), 0, 1),
(N'Mua 2 tặng 1', N'Mua 2 sản phẩm được tặng 1 sản phẩm khác', 'MUA2TANG1', 'Fixed', 50000, CAST(GETDATE() AS DATE), CAST(DATEADD(MONTH, 1, GETDATE()) AS DATE), 100000, 1),
(N'Giảm giá cho khách hàng mới', N'Giảm 15% cho đơn hàng đầu tiên', 'NEW15', 'Percentage', 15, CAST(GETDATE() AS DATE), CAST(DATEADD(MONTH, 2, GETDATE()) AS DATE), 50000, 1);

-- 13. Insert promotion products
INSERT INTO dbo.PromotionProducts (PromotionID, ProductID, DiscountPercentage) VALUES
(1, 1, 20), (1, 2, 20),
(2, 3, 10), (2, 4, 10),
(3, 1, 15), (3, 5, 15);

-- 14. Insert system configuration
INSERT INTO dbo.SystemConfiguration (ConfigKey, ConfigValue, Description) VALUES
('CLOUDINARY_CLOUD_NAME', 'your_cloud_name', 'Cloudinary Cloud Name'),
('CLOUDINARY_API_KEY', 'your_api_key', 'Cloudinary API Key'),
('CLOUDINARY_API_SECRET', 'your_api_secret', 'Cloudinary API Secret (Keep it secret!)'),
('MAX_IMAGE_SIZE_MB', '5', 'Maximum image upload size in MB'),
('ALLOWED_IMAGE_FORMATS', 'jpg,png,jpeg,gif,webp', 'Allowed image formats'),
('JWT_SECRET_KEY', 'your-secret-key-change-in-production', 'JWT Secret Key'),
('JWT_EXPIRATION_HOURS', '24', 'JWT Token expiration in hours'),
('REFRESH_TOKEN_EXPIRATION_DAYS', '30', 'Refresh Token expiration in days'),
('SHIPPING_FEE_DEFAULT', '30000', 'Default shipping fee in VND'),
('LOYALTY_POINTS_MULTIPLIER', '1000', 'Loyalty points earned per 1000 VND spent');

-- 15. Sample cart for customer 1
INSERT INTO dbo.Carts (CustomerID, Status) VALUES (1, 'ACTIVE');
DECLARE @CartID_1 INT = SCOPE_IDENTITY();

INSERT INTO dbo.CartItems (CartID, VariantID, Quantity, UnitPrice, LineTotal, Notes) VALUES
(@CartID_1, 4, 2, 49000, 98000, N'Ít đá, 70% đường');

DECLARE @CartItemID_1 INT = SCOPE_IDENTITY();
INSERT INTO dbo.CartItem_Toppings (CartItemID, ToppingID, Quantity, UnitPrice, LineTotal) VALUES
(@CartItemID_1, 4, 1, 7000, 7000);

INSERT INTO dbo.CartItems (CartID, VariantID, Quantity, UnitPrice, LineTotal, Notes) VALUES
(@CartID_1, 8, 1, 25000, 25000, N'Ít ngọt');

-- 16. Sample completed order for customer 2
INSERT INTO dbo.Orders (CustomerID, EmployeeID, OrderStatus, PaymentStatus, PaymentMethod, PaidAt, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, GrandTotal, Notes, OrderDate) VALUES
(2, 1, 'Completed', 'Paid', 'Momo', GETDATE(), N'789 Đường Pasteur, Phường 6, Quận 3, TP. Hồ Chí Minh', N'Phạm Bảo Trân', '0987654321', 80000, 15000, 95000, N'Giao nhanh!', DATEADD(DAY, -5, GETDATE()));

DECLARE @OrderID_1 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, LineTotal) VALUES
(@OrderID_1, 1, 1, 35000, 35000);

DECLARE @OrderDetailID_1 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, Quantity, UnitPrice, LineTotal) VALUES
(@OrderDetailID_1, 1, 1, 5000, 5000);

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, LineTotal) VALUES
(@OrderID_1, 6, 1, 45000, 45000);

DECLARE @OrderDetailID_2 INT = SCOPE_IDENTITY();

-- 17. Add reviews
INSERT INTO dbo.Reviews (CustomerID, OrderDetailID, Rating, Comment) VALUES
(2, @OrderDetailID_2, 5, N'Trà rất ngon, vị chua ngọt vừa phải. Sẽ ủng hộ quán!');

INSERT INTO dbo.Reviews (CustomerID, OrderDetailID, Rating, Comment) VALUES
(2, @OrderDetailID_1, 5, N'Trân châu chứ không quá ngon!');

-- 18. Add some favorites
INSERT INTO dbo.Favorites (CustomerID, ProductID) VALUES
(1, 1), (1, 3), (1, 6),
(2, 2), (2, 4),
(3, 1), (3, 5);

-- 19. Add notifications
INSERT INTO dbo.Notifications (UserID, Title, Message, Type, IsRead) VALUES
(4, N'Chào mừng!', N'Chào mừng bạn đến với AloTea', 'System', 0),
(5, N'Đơn hàng đã giao', N'Đơn hàng của bạn đã giao thành công', 'OrderStatus', 1),
(6, N'Khuyến mãi mới', N'Có khuyến mãi mới giảm 20% cho trà sữa', 'Promotion', 0);

GO

PRINT N'✓ All sample data inserted successfully.';
GO

-- ================================================================
-- PART 9: VERIFY AND DISPLAY RESULTS
-- ================================================================

PRINT N'==== Database verification...';
GO

PRINT N'Total tables created:';
SELECT COUNT(*) AS TableCount FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo';

PRINT N'Users in system:';
SELECT UserID, Username, Email, Status FROM dbo.Users;

PRINT N'Customers:';
SELECT CustomerID, FullName, MembershipLevel, LoyaltyPoints FROM dbo.Customers;

PRINT N'Products:';
SELECT ProductID, ProductName, SKU, Status FROM dbo.Products;

PRINT N'Orders:';
SELECT OrderID, OrderDate, OrderStatus, GrandTotal FROM dbo.Orders;

PRINT N'Active Promotions:';
SELECT PromotionID, PromotionName, PromoCode, StartDate, EndDate FROM dbo.Promotions;

PRINT N'System Configuration:';
SELECT ConfigKey, ConfigValue FROM dbo.SystemConfiguration WHERE ConfigKey NOT LIKE '%SECRET%' AND ConfigKey NOT LIKE '%PASSWORD%';

GO

PRINT N'';
PRINT N'========================================';
PRINT N'✓ DATABASE SETUP COMPLETED SUCCESSFULLY!';
PRINT N'========================================';
PRINT N'Database: MilkTeaShopDB';
PRINT N'Tables: 20 tables created';
PRINT N'Views: 8 views created';
PRINT N'Stored Procedures: 8 procedures created';
PRINT N'Functions: 4 functions created';
PRINT N'Triggers: 10 triggers created';
PRINT N'Indexes: 30+ performance indexes created';
PRINT N'Sample Data: Complete with products, users, orders, and promotions';
PRINT N'';
PRINT N'Ready for Spring Boot Integration with:';
PRINT N'- JWT Token Authentication';
PRINT N'- WebSocket Support (Chat & Notifications)';
PRINT N'- Cloudinary Integration';
PRINT N'- JPA/Hibernate ORM';
PRINT N'- Real-time Features';
PRINT N'========================================';
GO