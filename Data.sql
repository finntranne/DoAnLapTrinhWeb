/*
=================================================================
PHẦN 0: DỌN DẸP CSDL
Xóa dữ liệu theo thứ tự ngược lại của quan hệ phụ thuộc.
=================================================================
*/
USE MilkTeaShopDB;
GO

-- Xóa các bảng phụ thuộc cấp cao nhất
DELETE FROM Reviews;
DELETE FROM OrderDetail_Toppings;
DELETE FROM OrderDetails;
DELETE FROM OrderHistory;
DELETE FROM Orders;

-- Xóa các bảng join và bảng liên quan đến Product/Promotion
DELETE FROM ProductAvailableToppings;
DELETE FROM PromotionProducts;
DELETE FROM ProductImages;
DELETE FROM ProductVariants;
DELETE FROM Promotions;
DELETE FROM Products;

-- Xóa các bảng dữ liệu nền
DELETE FROM Categories;
DELETE FROM Toppings;
DELETE FROM Sizes;
DELETE FROM Shops;
DELETE FROM UserRoles;
DELETE FROM Users;
DELETE FROM Roles;
GO

/*
=================================================================
PHẦN 0.5: RESET IDENTITY
Tách riêng ra một batch để đảm bảo thực thi
=================================================================
*/
PRINT '======== DATABASE CLEANED. RESETTING IDENTITIES ========';
GO

DBCC CHECKIDENT ('[Reviews]', RESEED, 0);
DBCC CHECKIDENT ('[OrderDetails]', RESEED, 0);
DBCC CHECKIDENT ('[OrderHistory]', RESEED, 0);
DBCC CHECKIDENT ('[Orders]', RESEED, 0);
DBCC CHECKIDENT ('[ProductImages]', RESEED, 0);
DBCC CHECKIDENT ('[ProductVariants]', RESEED, 0);
DBCC CHECKIDENT ('[Promotions]', RESEED, 0);
DBCC CHECKIDENT ('[Products]', RESEED, 0);
DBCC CHECKIDENT ('[Categories]', RESEED, 0);
DBCC CHECKIDENT ('[Toppings]', RESEED, 0);
DBCC CHECKIDENT ('[Sizes]', RESEED, 0);
DBCC CHECKIDENT ('[Shops]', RESEED, 0);
DBCC CHECKIDENT ('[Users]', RESEED, 0);
GO

PRINT '======== IDENTITIES RESET. STARTING SEEDING ========';
GO

INSERT INTO dbo.Roles (RoleID, RoleName) VALUES (1, 'ADMIN');
INSERT INTO dbo.Roles (RoleID, RoleName) VALUES (2, 'VENDOR');
INSERT INTO dbo.Roles (RoleID, RoleName) VALUES (3, 'CUSTOMER');
INSERT INTO dbo.Roles (RoleID, RoleName) VALUES (4, 'SHIPPER');

/*
=================================================================
PHẦN 1: BẢNG DỮ LIỆU NỀN
=================================================================
*/

-- 1. Bảng Users (Tạo ID 1-7)
INSERT INTO dbo.Users (Username, PasswordHash, Email, PhoneNumber, FullName, Status, CreatedAt, UpdatedAt) VALUES
('admin', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'admin@alotra.com', '0900000001', N'Quản Trị Viên', 1, GETDATE(), GETDATE()),
('vendor1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor1@shop.com', '0900000002', N'Nguyễn Văn A', 1, GETDATE(), GETDATE()),
('vendor2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'vendor2@shop.com', '0900000003', N'Trần Thị B', 1, GETDATE(), GETDATE()),
('customer1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'minhanh@email.com', '0912345678', N'Lê Minh Anh', 1, GETDATE(), GETDATE()),
('customer2', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'baotran@email.com', '0987654321', N'Phạm Bảo Trân', 1, GETDATE(), GETDATE()),
('shipper1', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'shipper1@email.com', '0911111111', N'Giao Hàng Nhanh', 1, GETDATE(), GETDATE()),
('customer3', '$2a$10$ChCyZNXDPasox8exeAvQiOe9/wW6lcl4Gq9zUl5HvChmaAtVAzMK.', 'ducminh@email.com', '0923456789', N'Hoàng Đức Minh', 1, GETDATE(), GETDATE());
GO

-- 2. Bảng UserRoles (Giả định RoleID 1=ADMIN, 2=VENDOR, 3=CUSTOMER, 4=SHIPPER)
INSERT INTO dbo.UserRoles (UserID, RoleID) VALUES
(1, 1), (2, 2), (3, 2), (4, 3), (5, 3), (6, 4), (7, 3);
GO

-- 3. Bảng Shops (Tạo ID 1, 2)
INSERT INTO dbo.Shops (UserID, ShopName, Description, LogoURL, CoverImageURL, Address, PhoneNumber, Status, CommissionRate, CreatedAt, UpdatedAt) VALUES 
(2, N'Cửa hàng Trà sữa AloTra 1', N'Chi nhánh chính...', '...', '...', N'123 Đường...', '0900000002', 1, 5.00, GETDATE(), GETDATE()),
(3, N'Cửa hàng Trà sữa AloTra 2', N'Chi nhánh chuyên về...', '...', '...', N'456 Đường...', '0900000003', 1, 5.00, GETDATE(), GETDATE());
GO

-- 4. Bảng Sizes (Tạo ID 1, 2, 3)
INSERT INTO Sizes (SizeName) VALUES (N'S'), (N'M'), (N'L');
GO

-- 5. Bảng Categories (Tạo ID 1, 2, 3)
INSERT INTO Categories (CategoryName, Description, ImageURL, Status) VALUES
(N'Trà Sữa', N'...', '...', 1), (N'Cà Phê', N'...', '...', 1), (N'Trà Trái Cây', N'...', '...', 1);
GO

-- 6. Bảng Toppings (Tạo ID 1, 2, 3, 4)
INSERT INTO Toppings (ToppingName, AdditionalPrice, ImageUrl, Status) VALUES
(N'Trân châu trắng', 10000, '...', 1), (N'Pudding trứng', 10000, '...', 1),
(N'Thạch trái cây',10000, '...', 1), (N'Kem phô mai (Macchiato)', 10000.00, '...', 1);
GO

/*
=================================================================
PHẦN 2: PRODUCTS VÀ PROMOTIONS
=================================================================
*/

-- 7. Bảng Products (Tạo ID 1, 2, 3, 4)
INSERT INTO Products (ShopID, CategoryID, ProductName, Description, Status, AverageRating, TotalReviews, ViewCount, SoldCount, CreatedAt, UpdatedAt, BasePrice, TotalLikes) VALUES
(1, 1, N'Trà Sữa Trân Châu Đường Đen', N'...', 1, 4.5, 150, 2500, 800, GETDATE(), GETDATE(), 35000.00, 50),
(1, 2, N'Cà Phê Sữa Đá', N'...', 1, 4.8, 220, 3100, 1200, GETDATE(), GETDATE(), 25000.00, 75),
(2, 3, N'Trà Đào Cam Sả', N'...', 1, 4.7, 180, 2800, 950, GETDATE(), GETDATE(), 40000.00, 60),
(2, 1, N'Trà Sữa Matcha Đậu Đỏ', N'...', 1, 4.6, 110, 1800, 600, GETDATE(), GETDATE(), 45000.00, 40);
GO

-- 8. Bảng Promotions (Tạo ID 1, 2)
INSERT INTO dbo.Promotions (CreatedByUserID, CreatedByShopID, PromotionName, Description, PromoCode, PromotionType, DiscountType, DiscountValue, MaxDiscountAmount, StartDate, EndDate, MinOrderValue, UsageLimit, UsedCount, Status, CreatedAt) VALUES
(1, NULL, N'Khuyến mãi 20/10', N'...', NULL, 'PRODUCT', NULL, NULL, NULL, '2025-10-15T00:00:00', '2025-10-25T23:59:59', 0.00, NULL, 0, 1, GETDATE()),
(1, NULL, N'Chào Hè Sôi Động', N'...', NULL, 'PRODUCT', NULL, NULL, NULL, '2025-06-01T00:00:00', '2025-06-15T23:59:59', 0.00, NULL, 0, 0, GETDATE());
GO

/*
=================================================================
PHẦN 3: BẢNG PHỤ THUỘC (VARIANTS, IMAGES, JOINS)
=================================================================
*/

-- 9. Bảng ProductVariants (Tạo ID 1-12)
INSERT INTO ProductVariants (ProductID, SizeID, Price, Stock, SKU) VALUES
(1, 1, 35000.00, 100, 'P1_S'), (1, 2, 42000.00, 100, 'P1_M'), (1, 3, 49000.00, 100, 'P1_L'),
(2, 1, 25000.00, 100, 'P2_S'), (2, 2, 30000.00, 100, 'P2_M'), (2, 3, 35000.00, 100, 'P2_L'),
(3, 1, 40000.00, 100, 'P3_S'), (3, 2, 48000.00, 100, 'P3_M'), (3, 3, 55000.00, 100, 'P3_L'),
(4, 1, 45000.00, 100, 'P4_S'), (4, 2, 52000.00, 100, 'P4_M'), (4, 3, 59000.00, 100, 'P4_L');
GO

-- 10. Bảng ProductImages
INSERT INTO ProductImages (ProductID, ImageURL, IsPrimary, DisplayOrder) VALUES
(1, 'https://gongcha.com.vn/wp-content/uploads/2024/12/STAWBERRY-8.png', 1, 0),
(2, 'https://gongcha.com.vn/wp-content/uploads/2024/12/OKINAWA-8.png', 1, 0),
(3, 'https://gongcha.com.vn/wp-content/uploads/2024/07/Tra-sua-Earl-Grey-Carvia_web.png', 1, 0),
(4, 'https://gongcha.com.vn/wp-content/uploads/2018/02/Tr%C3%A0-s%E1%BB%AFa-Oolong-3J-2.png', 1, 0);
GO

-- 11. Bảng PromotionProducts
INSERT INTO PromotionProducts (PromotionID, ProductID, DiscountPercentage) VALUES
(1, 1, 30), (1, 4, 30), (2, 2, 20);
GO

-- 12. Bảng ProductAvailableToppings
INSERT INTO ProductAvailableToppings (ProductID, ToppingID) VALUES
(1, 1), (1, 2), (1, 4), (4, 1), (4, 2), (4, 4);
GO

/*
=================================================================
PHẦN 4: ORDERS, ORDERDETAILS, REVIEWS
=================================================================
*/

-- 13. Bảng Orders (Tạo ID 1, 2)
INSERT INTO Orders (
    UserID, ShopID, ShipperID, PromotionID, OrderDate, OrderStatus, PaymentStatus, PaymentMethod, 
    PaidAt, ShippingAddress, RecipientName, RecipientPhone, 
    Subtotal, DiscountAmount, ShippingFee, GrandTotal, Notes
) VALUES
(
    4, 1, NULL, NULL, GETDATE(), N'Completed', N'Paid', N'Momo', DATEADD(HOUR, -1, GETDATE()), 
    N'123 Đường...', N'Lê Minh Anh', '0912345678', 
    114000.00, 0.00, 10000.00, 124000.00, N'Đơn hàng Shop 1'
),
(
    4, 2, NULL, NULL, GETDATE(), N'Completed', N'Paid', N'Cash', DATEADD(HOUR, -2, GETDATE()), 
    N'45 Đường...', N'Lê Minh Anh', '0912345678', 
    100000.00, 0.00, 10000.00, 110000.00, N'Đơn hàng Shop 2'
);
GO

-- 14. Bảng OrderDetails (Tạo ID 1, 2, 3, 4)
INSERT INTO OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal)
VALUES
(1, 2, 2, 42000.00, 84000.00), -- ID=1
(1, 5, 1, 30000.00, 30000.00), -- ID=2
(2, 8, 1, 48000.00, 48000.00), -- ID=3
(2, 11, 1, 52000.00, 52000.00); -- ID=4
GO

-- 15. Bảng Reviews (Tạo ID 1, 2)
INSERT INTO Reviews (UserID, ProductID, OrderDetailID, Rating, Comment, MediaURLs, ReviewDate, IsVerifiedPurchase)
VALUES
(4, 1, 1, 5, N'Trà sữa rất ngon...', '...', GETDATE(), 1), -- Tham chiếu OrderDetailID=1
(4, 4, 4, 4, N'Matcha thơm...', '...', GETDATE(), 1); -- Tham chiếu OrderDetailID=4
GO

PRINT '======== SEEDING COMPLETED SUCCESSFULLY! ========';
GO

UPDATE Promotions
SET EndDate = '2025-11-30T23:59:59' -- Gia hạn đến cuối tháng 11
WHERE PromotionID = 1; -- Giả sử là ID của KM 20/10