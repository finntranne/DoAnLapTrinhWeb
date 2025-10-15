/* ================================================================
   SCRIPT: Complete E-commerce Milk Tea Database - Multi-Vendor Version
   Database: MilkTeaShopDB
   Purpose: Support Spring Boot + Thymeleaf + Bootstrap + JPA + 
            SQLServer + JWT + WebSocket + Cloudinary for a multi-vendor platform.
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

IF OBJECT_ID('dbo.ChatMessages','U') IS NOT NULL DROP TABLE dbo.ChatMessages;
IF OBJECT_ID('dbo.DeviceTokens','U') IS NOT NULL DROP TABLE dbo.DeviceTokens;
IF OBJECT_ID('dbo.JWTTokens','U') IS NOT NULL DROP TABLE dbo.JWTTokens;
IF OBJECT_ID('dbo.Notifications','U') IS NOT NULL DROP TABLE dbo.Notifications;

IF OBJECT_ID('dbo.Addresses','U') IS NOT NULL DROP TABLE dbo.Addresses;
IF OBJECT_ID('dbo.Employees','U') IS NOT NULL DROP TABLE dbo.Employees;
IF OBJECT_ID('dbo.Customers','U') IS NOT NULL DROP TABLE dbo.Customers;

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

-- 1. User Account System
CREATE TABLE dbo.Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    PhoneNumber NVARCHAR(20) NULL UNIQUE,
    Status TINYINT NOT NULL DEFAULT 1 CHECK (Status IN (0,1)), -- 0: Inactive, 1: Active
    AvatarURL NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
	OtpCode NVARCHAR(10) NULL,
    OtpExpiryTime DATETIME2 NULL,
    OtpPurpose NVARCHAR(20) NULL -- e.g., 'REGISTER', 'RESET_PASSWORD'
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

-- 2. Customer, Employee Profiles
CREATE TABLE dbo.Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT UNIQUE NOT NULL,
    FullName NVARCHAR(255) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Customer_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.Employees (
    EmployeeID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT UNIQUE NOT NULL,
    FullName NVARCHAR(255) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Employee_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 3. Addresses
CREATE TABLE dbo.Addresses (
    AddressID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    AddressName NVARCHAR(100) NOT NULL,
    FullAddress NVARCHAR(500) NOT NULL,
    PhoneNumber NVARCHAR(20) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    IsDefault BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_Address_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE
);

-- 4. Shops (For Multi-Vendor)
CREATE TABLE dbo.Shops (
    ShopID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL UNIQUE, -- The user who owns this shop (Vendor)
    ShopName NVARCHAR(255) NOT NULL UNIQUE,
    Description NVARCHAR(MAX) NULL,
    LogoURL NVARCHAR(500) NULL,
    CoverImageURL NVARCHAR(500) NULL,
    Address NVARCHAR(500) NOT NULL,
    PhoneNumber NVARCHAR(20) NOT NULL,
    Status TINYINT NOT NULL DEFAULT 0, -- 0: Pending Approval, 1: Active, 2: Suspended
    CommissionRate DECIMAL(5,2) DEFAULT 5.00, -- App commission rate for this shop
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Shop_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

-- 5. Categories and Products
CREATE TABLE dbo.Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(255) NOT NULL UNIQUE,
    Description NVARCHAR(MAX) NULL
);

CREATE TABLE dbo.Products (
    ProductID INT IDENTITY(1,1) PRIMARY KEY,
    ShopID INT NOT NULL, -- Each product belongs to a shop
    CategoryID INT NOT NULL,
    ProductName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    Status TINYINT NOT NULL DEFAULT 1, -- 0: Inactive, 1: Active
    AverageRating DECIMAL(3,2) DEFAULT 0,
    TotalReviews INT DEFAULT 0,
    ViewCount INT DEFAULT 0,
    SoldCount INT DEFAULT 0, -- To track best-selling products
    CONSTRAINT FK_Product_Shop FOREIGN KEY (ShopID) REFERENCES dbo.Shops(ShopID),
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID)
);

-- 6. Product Images, Sizes, Variants, Toppings
CREATE TABLE dbo.ProductImages (
    ImageID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    ImageURL NVARCHAR(500) NOT NULL,
    IsPrimary BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_ProductImage_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

CREATE TABLE dbo.Sizes (
    SizeID INT IDENTITY(1,1) PRIMARY KEY,
    SizeName NVARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE dbo.ProductVariants (
    VariantID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID INT NOT NULL,
    SizeID INT NOT NULL,
    Price DECIMAL(10,2) NOT NULL CHECK (Price > 0),
    Stock INT NOT NULL DEFAULT 0 CHECK (Stock >= 0),
    CONSTRAINT UQ_ProductVariant_Unique UNIQUE (ProductID, SizeID),
    CONSTRAINT FK_Variant_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT FK_Variant_Size FOREIGN KEY (SizeID) REFERENCES dbo.Sizes(SizeID)
);

CREATE TABLE dbo.Toppings (
    ToppingID INT IDENTITY(1,1) PRIMARY KEY,
    ToppingName NVARCHAR(255) NOT NULL UNIQUE,
    AdditionalPrice DECIMAL(10,2) NOT NULL CHECK (AdditionalPrice >= 0)
);

-- 7. Promotions
CREATE TABLE dbo.Promotions (
    PromotionID INT IDENTITY(1,1) PRIMARY KEY,
    CreatedByShopID INT NULL, -- NULL if created by Admin, ShopID if by Vendor
    PromotionName NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    PromoCode NVARCHAR(50) UNIQUE NULL,
    DiscountType NVARCHAR(20) NOT NULL, -- 'Percentage', 'FixedAmount', 'FreeShip'
    DiscountValue DECIMAL(10,2) NOT NULL,
    StartDate DATETIME2 NOT NULL,
    EndDate DATETIME2 NOT NULL,
    MinOrderValue DECIMAL(10,2) DEFAULT 0,
    Status TINYINT NOT NULL DEFAULT 1,
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

-- 8. Favorites & Viewed History
CREATE TABLE dbo.Favorites (
    FavoriteID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    ProductID INT NOT NULL,
    CONSTRAINT UQ_Favorite_Unique UNIQUE (CustomerID, ProductID),
    CONSTRAINT FK_Favorite_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE,
    CONSTRAINT FK_Favorite_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

CREATE TABLE dbo.ViewedProducts (
    ViewedID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    ProductID INT NOT NULL,
    LastViewedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Viewed_Unique UNIQUE (CustomerID, ProductID),
    CONSTRAINT FK_Viewed_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE,
    CONSTRAINT FK_Viewed_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID) ON DELETE CASCADE
);

-- 9. Shopping Carts
CREATE TABLE dbo.Carts (
    CartID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL UNIQUE,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Cart_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID) ON DELETE CASCADE
);

CREATE TABLE dbo.CartItems (
    CartItemID INT IDENTITY(1,1) PRIMARY KEY,
    CartID INT NOT NULL,
    VariantID INT NOT NULL,
    Quantity INT NOT NULL CHECK (Quantity > 0),
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

-- 10. Orders & Shipping
CREATE TABLE dbo.ShippingProviders (
    ProviderID INT IDENTITY(1,1) PRIMARY KEY,
    ProviderName NVARCHAR(255) NOT NULL,
    BaseFee DECIMAL(10,2) NOT NULL DEFAULT 0,
    Status TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE dbo.Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    PromotionID INT NULL,
    OrderDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    OrderStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending'
        CHECK (OrderStatus IN ('Pending', 'Confirmed', 'Delivering', 'Completed', 'Cancelled', 'Returned', 'Refunded')),
    PaymentMethod NVARCHAR(50) NOT NULL CHECK (PaymentMethod IN ('COD', 'Momo', 'VNPay', 'ZaloPay', 'BankTransfer')),
    PaymentStatus NVARCHAR(30) NOT NULL DEFAULT 'Unpaid' CHECK (PaymentStatus IN ('Unpaid', 'Paid')),
    PaidAt DATETIME2 NULL,
    ShippingAddress NVARCHAR(500) NOT NULL,
    RecipientName NVARCHAR(255) NOT NULL,
    RecipientPhone NVARCHAR(20) NOT NULL,
    Subtotal DECIMAL(12,2) NOT NULL,
    ShippingFee DECIMAL(12,2) NOT NULL,
    DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0,
    GrandTotal DECIMAL(12,2) NOT NULL,
    Notes NVARCHAR(500) NULL,
    ShipperID INT NULL, -- Refers to an Employee with Shipper role
    CONSTRAINT FK_Order_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Order_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotions(PromotionID),
    CONSTRAINT FK_Order_Shipper FOREIGN KEY (ShipperID) REFERENCES dbo.Employees(EmployeeID)
);

CREATE TABLE dbo.OrderHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
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
    Quantity INT NOT NULL,
    UnitPrice DECIMAL(10,2) NOT NULL, -- Price at the time of purchase
    CONSTRAINT FK_OrderDetail_Order FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetail_Variant FOREIGN KEY (VariantID) REFERENCES dbo.ProductVariants(VariantID)
);

CREATE TABLE dbo.OrderDetail_Toppings (
    OrderDetailID INT NOT NULL,
    ToppingID INT NOT NULL,
    UnitPrice DECIMAL(10,2) NOT NULL, -- Price at the time of purchase
    PRIMARY KEY (OrderDetailID, ToppingID),
    CONSTRAINT FK_OrderDetailTopping_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetailTopping_Topping FOREIGN KEY (ToppingID) REFERENCES dbo.Toppings(ToppingID)
);

-- 11. Reviews & Comments
CREATE TABLE dbo.Reviews (
    ReviewID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    ProductID INT NOT NULL,
    OrderDetailID INT NOT NULL UNIQUE, -- A review must be linked to a specific purchased item
    Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(MAX) NULL,
    MediaURLs NVARCHAR(MAX) NULL, -- Store comma-separated URLs for images/videos
    ReviewDate DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Review_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID),
    CONSTRAINT FK_Review_Product FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID),
    CONSTRAINT FK_Review_OrderDetail FOREIGN KEY (OrderDetailID) REFERENCES dbo.OrderDetails(OrderDetailID)
);

-- 12. System & Integration Tables (JWT, Cloudinary, WebSocket)
CREATE TABLE dbo.CloudinaryAssets (
    AssetID INT IDENTITY(1,1) PRIMARY KEY,
    PublicID NVARCHAR(255) NOT NULL UNIQUE,
    CloudinaryURL NVARCHAR(500) NOT NULL,
    UploadedByUserID INT NULL,
    CONSTRAINT FK_CloudinaryAssets_User FOREIGN KEY (UploadedByUserID) REFERENCES dbo.Users(UserID)
);

CREATE TABLE dbo.Notifications (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    Type NVARCHAR(50) NOT NULL, -- e.g., 'OrderStatus', 'NewPromotion'
    RelatedEntityID INT NULL, -- e.g., OrderID
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
    ExpiresAt DATETIME2 NOT NULL,
    IsRevoked BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_JWTToken_User FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID) ON DELETE CASCADE
);

CREATE TABLE dbo.SystemConfiguration (
    ConfigKey NVARCHAR(255) PRIMARY KEY,
    ConfigValue NVARCHAR(MAX) NOT NULL,
    Description NVARCHAR(500) NULL
);

GO
PRINT N'✓ All tables created successfully.';
GO

-- ================================================================
-- PART 3: CREATE TRIGGERS
-- ================================================================

PRINT N'==== Creating triggers...';
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
        UPDATE p
        SET p.SoldCount = p.SoldCount + od.Quantity
        FROM dbo.Products p
        JOIN dbo.ProductVariants pv ON p.ProductID = pv.ProductID
        JOIN dbo.OrderDetails od ON pv.VariantID = od.VariantID
        JOIN inserted i ON od.OrderID = i.OrderID
        JOIN deleted d ON i.OrderID = d.OrderID
        WHERE i.OrderStatus = 'Completed' AND d.OrderStatus <> 'Completed';
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

PRINT N'✓ All triggers created successfully.';
GO

-- ================================================================
-- PART 4: CREATE VIEWS
-- ================================================================

PRINT N'==== Creating views...';
GO

-- View for the guest homepage (products from shops with > 10 total sales)
CREATE VIEW dbo.v_GuestHomepageProducts
AS
SELECT 
    p.ProductID,
    p.ProductName,
    p.Description,
    p.AverageRating,
    v.Price,
    img.ImageURL AS PrimaryImageURL,
    s.ShopName,
    p.SoldCount
FROM dbo.Products p
JOIN dbo.Shops s ON p.ShopID = s.ShopID
-- Find the price of the default variant (e.g., size M)
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
WHERE p.Status = 1 AND s.Status = 1
AND s.ShopID IN (
    SELECT ShopID FROM dbo.Products
    GROUP BY ShopID
    HAVING SUM(SoldCount) > 10
)
GO

-- View for detailed product information including shop details
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
    c.CategoryID,
    c.CategoryName,
    s.ShopID,
    s.ShopName,
    s.LogoURL AS ShopLogoURL
FROM dbo.Products p
JOIN dbo.Categories c ON p.CategoryID = c.CategoryID
JOIN dbo.Shops s ON p.ShopID = s.ShopID
WHERE p.Status = 1 AND s.Status = 1
GO

PRINT N'✓ All views created successfully.';
GO
-- ================================================================
-- PART 5: INSERT SAMPLE DATA
-- ================================================================

PRINT N'==== Inserting sample data...';
GO

-- 1. Roles
INSERT INTO dbo.Roles (RoleID, RoleName) VALUES 
(1, 'ADMIN'), (2, 'CUSTOMER'), (3, 'VENDOR'), (4, 'SHIPPER'), (5, 'EMPLOYEE');

-- 2. Users defalut-password: 123456789
INSERT INTO dbo.Users (Username, PasswordHash, Email, PhoneNumber) VALUES
('admin', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'admin@alotra.com', '0900000001'),
('vendor1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor1@shop.com', '0900000002'),
('vendor2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor2@shop.com', '0900000003'),
('customer1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'minhanh@email.com', '0912345678'),
('customer2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'baotran@email.com', '0987654321'),
('shipper1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'shipper1@email.com', '0911111111');


-- 3. UserRoles
INSERT INTO dbo.UserRoles (UserID, RoleID) VALUES
(1, 1), (2, 3), (3, 3), (4, 2), (5, 2), (6, 4);

-- 4. Customers and Employees (for Shipper)
INSERT INTO dbo.Customers (UserID, FullName) VALUES
(4, N'Lê Minh Anh'), (5, N'Phạm Bảo Trân');

INSERT INTO dbo.Employees (UserID, FullName) VALUES
(6, N'Giao Hàng Nhanh'); -- Shipper is also an employee

-- 5. Shops
INSERT INTO dbo.Shops (UserID, ShopName, Address, PhoneNumber, Status) VALUES
(2, N'Trà Sữa Phúc Long Clone', N'12 Ngô Đức Kế, Quận 1, TP.HCM', '0902222222', 1),
(3, N'The Coffee House Mini', N'24 Pasteur, Quận 1, TP.HCM', '0903333333', 1);

-- 6. Categories
INSERT INTO dbo.Categories (CategoryName) VALUES (N'Trà Sữa'), (N'Trà Trái Cây'), (N'Cà Phê');

-- 7. Products (linked to shops)
INSERT INTO dbo.Products (ShopID, CategoryID, ProductName, SoldCount) VALUES
(1, 1, N'Trà Sữa Phúc Long', 25),
(1, 2, N'Trà Đào Phúc Long', 15),
(2, 3, N'Cà Phê Đen Đá', 5),
(2, 1, N'Trà Sữa Olong The Coffee House', 8);

-- 8. Sizes & ProductVariants
INSERT INTO dbo.Sizes (SizeName) VALUES ('S'), ('M'), ('L');
INSERT INTO dbo.ProductVariants (ProductID, SizeID, Price, Stock) VALUES
(1, 2, 45000, 100), (1, 3, 55000, 100), -- Trà Sữa PL
(2, 2, 50000, 100),                      -- Trà Đào PL
(3, 1, 30000, 100), (3, 2, 35000, 100), -- Cà phê TCH
(4, 2, 48000, 100);                      -- Trà sữa TCH

-- 9. Sample Order
DECLARE @CustomerID INT = (SELECT CustomerID FROM dbo.Customers WHERE FullName = N'Lê Minh Anh');
INSERT INTO dbo.Orders (CustomerID, OrderStatus, PaymentMethod, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, GrandTotal)
VALUES (@CustomerID, 'Completed', 'COD', N'123 Lê Lợi, Q1', N'Lê Minh Anh', '0912345678', 45000, 15000, 60000);
DECLARE @OrderID INT = SCOPE_IDENTITY();

DECLARE @VariantID INT = (SELECT VariantID FROM dbo.ProductVariants WHERE ProductID = 1 AND SizeID = 2);
INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice) VALUES (@OrderID, @VariantID, 1, 45000);

GO

PRINT N'✓ All sample data inserted successfully.';
GO

PRINT N'========================================';
PRINT N'✓ DATABASE SETUP COMPLETED SUCCESSFULLY!';
PRINT N'========================================';
