SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET NOCOUNT ON;

/*
Seed data mau cho MilkTeaShopDB.
- An toan voi du lieu hien co: chi INSERT khi chua ton tai.
- PasswordHash ben duoi chi la placeholder de seed du lieu.
  Neu can dang nhap bang tai khoan test, hay thay bang BCrypt hash hop le.
*/

IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleName = 'CUSTOMER')
    INSERT INTO Roles (RoleName, Description) VALUES ('CUSTOMER', N'Khach hang');

IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleName = 'VENDOR')
    INSERT INTO Roles (RoleName, Description) VALUES ('VENDOR', N'Chu shop');

IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleName = 'ADMIN')
    INSERT INTO Roles (RoleName, Description) VALUES ('ADMIN', N'Quan tri');

IF NOT EXISTS (SELECT 1 FROM Sizes WHERE SizeName = 'M')
    INSERT INTO Sizes (SizeName, Description) VALUES ('M', N'Ly vua');

IF NOT EXISTS (SELECT 1 FROM Sizes WHERE SizeName = 'L')
    INSERT INTO Sizes (SizeName, Description) VALUES ('L', N'Ly lon');

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Tra sua seed')
    INSERT INTO Categories (CategoryName, Description, ImageURL, Status)
    VALUES (N'Tra sua seed', N'Danh muc test cho tra sua', NULL, 1);

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Ca phe seed')
    INSERT INTO Categories (CategoryName, Description, ImageURL, Status)
    VALUES (N'Ca phe seed', N'Danh muc test cho ca phe', NULL, 1);

IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'seed_customer')
    INSERT INTO Users (
        Username, PasswordHash, Email, PhoneNumber, FullName, Status,
        AvatarURL, CreatedAt, UpdatedAt, LastLoginAt, OtpCode, OtpExpiryTime, OtpPurpose
    )
    VALUES (
        'seed_customer', 'seed_password_hash', 'seed_customer@alotra.local', NULL,
        N'Khach Hang Test', 1, NULL, GETDATE(), GETDATE(), NULL, NULL, NULL, NULL
    );

IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'seed_vendor_1')
    INSERT INTO Users (
        Username, PasswordHash, Email, PhoneNumber, FullName, Status,
        AvatarURL, CreatedAt, UpdatedAt, LastLoginAt, OtpCode, OtpExpiryTime, OtpPurpose
    )
    VALUES (
        'seed_vendor_1', 'seed_password_hash', 'seed_vendor_1@alotra.local', NULL,
        N'Chu Shop Test 1', 1, NULL, GETDATE(), GETDATE(), NULL, NULL, NULL, NULL
    );

IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'seed_vendor_2')
    INSERT INTO Users (
        Username, PasswordHash, Email, PhoneNumber, FullName, Status,
        AvatarURL, CreatedAt, UpdatedAt, LastLoginAt, OtpCode, OtpExpiryTime, OtpPurpose
    )
    VALUES (
        'seed_vendor_2', 'seed_password_hash', 'seed_vendor_2@alotra.local', NULL,
        N'Chu Shop Test 2', 1, NULL, GETDATE(), GETDATE(), NULL, NULL, NULL, NULL
    );

DECLARE @CustomerRoleId INT = (SELECT TOP 1 RoleID FROM Roles WHERE RoleName = 'CUSTOMER');
DECLARE @VendorRoleId INT = (SELECT TOP 1 RoleID FROM Roles WHERE RoleName = 'VENDOR');

DECLARE @CustomerId INT = (SELECT TOP 1 UserID FROM Users WHERE Username = 'seed_customer');
DECLARE @VendorUser1Id INT = (SELECT TOP 1 UserID FROM Users WHERE Username = 'seed_vendor_1');
DECLARE @VendorUser2Id INT = (SELECT TOP 1 UserID FROM Users WHERE Username = 'seed_vendor_2');

IF NOT EXISTS (SELECT 1 FROM UserRoles WHERE UserID = @CustomerId AND RoleID = @CustomerRoleId)
    INSERT INTO UserRoles (UserID, RoleID) VALUES (@CustomerId, @CustomerRoleId);

IF NOT EXISTS (SELECT 1 FROM UserRoles WHERE UserID = @VendorUser1Id AND RoleID = @VendorRoleId)
    INSERT INTO UserRoles (UserID, RoleID) VALUES (@VendorUser1Id, @VendorRoleId);

IF NOT EXISTS (SELECT 1 FROM UserRoles WHERE UserID = @VendorUser2Id AND RoleID = @VendorRoleId)
    INSERT INTO UserRoles (UserID, RoleID) VALUES (@VendorUser2Id, @VendorRoleId);

IF NOT EXISTS (SELECT 1 FROM Shops WHERE ShopName = N'Shop Seed Quan 1')
    INSERT INTO Shops (
        UserID, ShopName, Description, LogoURL, CoverImageURL, Address,
        PhoneNumber, Status, CommissionRate, CreatedAt, UpdatedAt
    )
    VALUES (
        @VendorUser1Id, N'Shop Seed Quan 1', N'Shop test ban tra sua',
        NULL, NULL, N'123 Vo Van Ngan, Thu Duc, TP.HCM',
        '0900111111', 1, 5.00, GETDATE(), GETDATE()
    );

IF NOT EXISTS (SELECT 1 FROM Shops WHERE ShopName = N'Shop Seed Quan 7')
    INSERT INTO Shops (
        UserID, ShopName, Description, LogoURL, CoverImageURL, Address,
        PhoneNumber, Status, CommissionRate, CreatedAt, UpdatedAt
    )
    VALUES (
        @VendorUser2Id, N'Shop Seed Quan 7', N'Shop test ban ca phe',
        NULL, NULL, N'456 Nguyen Huu Tho, Quan 7, TP.HCM',
        '0900222222', 1, 5.00, GETDATE(), GETDATE()
    );

DECLARE @Shop1Id INT = (SELECT TOP 1 ShopID FROM Shops WHERE ShopName = N'Shop Seed Quan 1');
DECLARE @Shop2Id INT = (SELECT TOP 1 ShopID FROM Shops WHERE ShopName = N'Shop Seed Quan 7');
DECLARE @TeaCategoryId INT = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Tra sua seed');
DECLARE @CoffeeCategoryId INT = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Ca phe seed');
DECLARE @SizeMId INT = (SELECT TOP 1 SizeID FROM Sizes WHERE SizeName = 'M');
DECLARE @SizeLId INT = (SELECT TOP 1 SizeID FROM Sizes WHERE SizeName = 'L');

IF NOT EXISTS (SELECT 1 FROM Addresses WHERE UserID = @CustomerId AND FullAddress = N'12 Le Van Viet, Thu Duc, TP.HCM')
    INSERT INTO Addresses (
        AddressName, CreatedAt, FullAddress, IsDefault, PhoneNumber, RecipientName, UserID,
        province, district, ward, street_address
    )
    VALUES (
        N'Nha rieng', GETDATE(), N'12 Le Van Viet, Thu Duc, TP.HCM', 1,
        '0900000001', N'Khach Hang Test', @CustomerId,
        N'TP.HCM', N'Thu Duc', N'Linh Trung', N'12 Le Van Viet'
    );

DECLARE @AddressId INT = (
    SELECT TOP 1 AddressID
    FROM Addresses
    WHERE UserID = @CustomerId AND FullAddress = N'12 Le Van Viet, Thu Duc, TP.HCM'
);

IF NOT EXISTS (SELECT 1 FROM ShippingProviders WHERE ProviderName = N'Seed Express')
    INSERT INTO ShippingProviders (ProviderName, Description, BaseFee, Status)
    VALUES (N'Seed Express', N'Don vi van chuyen test', 15000, 1);

DECLARE @ProviderId INT = (SELECT TOP 1 ProviderID FROM ShippingProviders WHERE ProviderName = N'Seed Express');

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Tra sua tran chau seed')
    INSERT INTO Products (
        AverageRating, BasePrice, CreatedAt, Description, ProductName, SoldCount, Status,
        TotalLikes, TotalReviews, UpdatedAt, ViewCount, CategoryID, ShopID
    )
    VALUES (
        4.7, 32000, GETDATE(), N'Tra sua tran chau danh cho test',
        N'Tra sua tran chau seed', 12, 1, 7, 3, GETDATE(), 40, @TeaCategoryId, @Shop1Id
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Tra dao cam sa seed')
    INSERT INTO Products (
        AverageRating, BasePrice, CreatedAt, Description, ProductName, SoldCount, Status,
        TotalLikes, TotalReviews, UpdatedAt, ViewCount, CategoryID, ShopID
    )
    VALUES (
        4.5, 28000, GETDATE(), N'Tra dao cam sa danh cho test',
        N'Tra dao cam sa seed', 8, 1, 5, 2, GETDATE(), 22, @TeaCategoryId, @Shop1Id
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Ca phe sua seed')
    INSERT INTO Products (
        AverageRating, BasePrice, CreatedAt, Description, ProductName, SoldCount, Status,
        TotalLikes, TotalReviews, UpdatedAt, ViewCount, CategoryID, ShopID
    )
    VALUES (
        4.8, 30000, GETDATE(), N'Ca phe sua da danh cho test',
        N'Ca phe sua seed', 15, 1, 9, 4, GETDATE(), 55, @CoffeeCategoryId, @Shop2Id
    );

DECLARE @MilkTeaProductId INT = (SELECT TOP 1 ProductID FROM Products WHERE ProductName = N'Tra sua tran chau seed');
DECLARE @PeachTeaProductId INT = (SELECT TOP 1 ProductID FROM Products WHERE ProductName = N'Tra dao cam sa seed');
DECLARE @CoffeeProductId INT = (SELECT TOP 1 ProductID FROM Products WHERE ProductName = N'Ca phe sua seed');

IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE ProductID = @MilkTeaProductId AND SizeID = @SizeMId)
    INSERT INTO ProductVariants (Price, SKU, Stock, ProductID, SizeID)
    VALUES (32000, 'SEED-TS-M', 100, @MilkTeaProductId, @SizeMId);

IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE ProductID = @MilkTeaProductId AND SizeID = @SizeLId)
    INSERT INTO ProductVariants (Price, SKU, Stock, ProductID, SizeID)
    VALUES (38000, 'SEED-TS-L', 80, @MilkTeaProductId, @SizeLId);

IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE ProductID = @PeachTeaProductId AND SizeID = @SizeMId)
    INSERT INTO ProductVariants (Price, SKU, Stock, ProductID, SizeID)
    VALUES (28000, 'SEED-TD-M', 60, @PeachTeaProductId, @SizeMId);

IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE ProductID = @CoffeeProductId AND SizeID = @SizeMId)
    INSERT INTO ProductVariants (Price, SKU, Stock, ProductID, SizeID)
    VALUES (30000, 'SEED-CP-M', 75, @CoffeeProductId, @SizeMId);

DECLARE @MilkTeaVariantMId INT = (
    SELECT TOP 1 VariantID FROM ProductVariants WHERE ProductID = @MilkTeaProductId AND SizeID = @SizeMId
);
DECLARE @PeachTeaVariantMId INT = (
    SELECT TOP 1 VariantID FROM ProductVariants WHERE ProductID = @PeachTeaProductId AND SizeID = @SizeMId
);

IF NOT EXISTS (SELECT 1 FROM Toppings WHERE ToppingName = N'Tran chau den' AND ShopID = @Shop1Id)
    INSERT INTO Toppings (AdditionalPrice, ImageURL, Status, ToppingName, ShopID, Price)
    VALUES (8000, NULL, 1, N'Tran chau den', @Shop1Id, 8000);

IF NOT EXISTS (SELECT 1 FROM Toppings WHERE ToppingName = N'Pudding trung' AND ShopID = @Shop1Id)
    INSERT INTO Toppings (AdditionalPrice, ImageURL, Status, ToppingName, ShopID, Price)
    VALUES (10000, NULL, 1, N'Pudding trung', @Shop1Id, 10000);

DECLARE @BlackPearlToppingId INT = (
    SELECT TOP 1 ToppingID FROM Toppings WHERE ToppingName = N'Tran chau den' AND ShopID = @Shop1Id
);
DECLARE @PuddingToppingId INT = (
    SELECT TOP 1 ToppingID FROM Toppings WHERE ToppingName = N'Pudding trung' AND ShopID = @Shop1Id
);

IF NOT EXISTS (SELECT 1 FROM ProductAvailableToppings WHERE ProductID = @MilkTeaProductId AND ToppingID = @BlackPearlToppingId)
    INSERT INTO ProductAvailableToppings (ProductID, ToppingID) VALUES (@MilkTeaProductId, @BlackPearlToppingId);

IF NOT EXISTS (SELECT 1 FROM ProductAvailableToppings WHERE ProductID = @MilkTeaProductId AND ToppingID = @PuddingToppingId)
    INSERT INTO ProductAvailableToppings (ProductID, ToppingID) VALUES (@MilkTeaProductId, @PuddingToppingId);

IF NOT EXISTS (SELECT 1 FROM Favorites WHERE UserID = @CustomerId AND ProductID = @MilkTeaProductId)
    INSERT INTO Favorites (CreatedAt, ProductID, UserID) VALUES (GETDATE(), @MilkTeaProductId, @CustomerId);

IF NOT EXISTS (SELECT 1 FROM Favorites WHERE UserID = @CustomerId AND ProductID = @CoffeeProductId)
    INSERT INTO Favorites (CreatedAt, ProductID, UserID) VALUES (GETDATE(), @CoffeeProductId, @CustomerId);

IF NOT EXISTS (SELECT 1 FROM Orders WHERE Notes = N'SEED ORDER 001')
    INSERT INTO Orders (
        CancellationReason, CompletedAt, DiscountAmount, GrandTotal, Notes, OrderDate, OrderStatus,
        PaidAt, PaymentMethod, PaymentStatus, RecipientName, RecipientPhone, ShippingAddress,
        ShippingFee, Subtotal, TransactionID, PromotionID, ShipperID, ShippingProviderID,
        ShopID, UserID, AddressID
    )
    VALUES (
        NULL, GETDATE(), 0, 72000, N'SEED ORDER 001', GETDATE(), 'Completed',
        GETDATE(), 'COD', 'PAID', N'Khach Hang Test', '0900000001', N'12 Le Van Viet, Thu Duc, TP.HCM',
        12000, 60000, NULL, NULL, NULL, @ProviderId, @Shop1Id, @CustomerId, @AddressId
    );

DECLARE @Order1Id INT = (SELECT TOP 1 OrderID FROM Orders WHERE Notes = N'SEED ORDER 001');

IF NOT EXISTS (SELECT 1 FROM OrderDetails WHERE OrderID = @Order1Id AND VariantID = @MilkTeaVariantMId)
    INSERT INTO OrderDetails (Quantity, Subtotal, UnitPrice, OrderID, VariantID)
    VALUES (1, 32000, 32000, @Order1Id, @MilkTeaVariantMId);

IF NOT EXISTS (SELECT 1 FROM OrderDetails WHERE OrderID = @Order1Id AND VariantID = @PeachTeaVariantMId)
    INSERT INTO OrderDetails (Quantity, Subtotal, UnitPrice, OrderID, VariantID)
    VALUES (1, 28000, 28000, @Order1Id, @PeachTeaVariantMId);

IF NOT EXISTS (SELECT 1 FROM OrderItems WHERE OrderID = @Order1Id AND VariantID = @MilkTeaVariantMId)
    INSERT INTO OrderItems (Price, Quantity, Toppings, OrderID, VariantID)
    VALUES (32000, 1, N'Tran chau den', @Order1Id, @MilkTeaVariantMId);

IF NOT EXISTS (SELECT 1 FROM OrderItems WHERE OrderID = @Order1Id AND VariantID = @PeachTeaVariantMId)
    INSERT INTO OrderItems (Price, Quantity, Toppings, OrderID, VariantID)
    VALUES (28000, 1, NULL, @Order1Id, @PeachTeaVariantMId);

DECLARE @OrderDetail1Id INT = (
    SELECT TOP 1 OrderDetailID FROM OrderDetails WHERE OrderID = @Order1Id AND VariantID = @MilkTeaVariantMId
);
DECLARE @OrderItem1Id INT = (
    SELECT TOP 1 OrderItemID FROM OrderItems WHERE OrderID = @Order1Id AND VariantID = @MilkTeaVariantMId
);

IF NOT EXISTS (SELECT 1 FROM Reviews WHERE OrderItemID = @OrderItem1Id)
    INSERT INTO Reviews (
        Comment, IsVerifiedPurchase, MediaURLs, Rating, ReviewDate,
        OrderDetailID, ProductID, UserID, OrderItemID
    )
    VALUES (
        N'Tra sua ngon, de test review va trang chi tiet san pham.',
        1, NULL, 5, GETDATE(),
        @OrderDetail1Id, @MilkTeaProductId, @CustomerId, @OrderItem1Id
    );

IF NOT EXISTS (SELECT 1 FROM payments WHERE OrderID = @Order1Id)
    INSERT INTO payments (method, paid_at, status, transaction_code, OrderID)
    VALUES ('COD', GETDATE(), 'PAID', NULL, @Order1Id);

PRINT 'Seed sample data completed.';
