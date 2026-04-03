-- =====================================================
-- RESET ORDERS DATA ONLY AND INSERT FRESH DATA
-- =====================================================
-- This script will:
-- 1. Delete all existing orders data
-- 2. Reset IDENTITY columns for order tables
-- 3. Insert fresh test orders
-- =====================================================

USE MilkTeaShopDB;
GO

PRINT '=====================================================';
PRINT 'STARTING ORDERS RESET PROCESS...';
PRINT '=====================================================';

-- =====================================================
-- STEP 1: DISABLE FOREIGN KEY CONSTRAINTS ON ORDER TABLES
-- =====================================================
PRINT 'Disabling foreign key constraints on order tables...';

ALTER TABLE dbo.Reviews NOCHECK CONSTRAINT ALL; -- *** THÊM DÒNG NÀY ***
ALTER TABLE dbo.OrderDetail_Toppings NOCHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderDetails NOCHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderShippingHistory NOCHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderHistory NOCHECK CONSTRAINT ALL;
ALTER TABLE dbo.ShopRevenue NOCHECK CONSTRAINT ALL;
ALTER TABLE dbo.Orders NOCHECK CONSTRAINT ALL;

-- =====================================================
-- STEP 2: DELETE ALL ORDERS DATA
-- =====================================================
PRINT 'Deleting existing orders data...';

DELETE FROM dbo.Reviews; -- *** THÊM DÒNG NÀY (Xóa Reviews trước) ***
DELETE FROM dbo.OrderDetail_Toppings;
DELETE FROM dbo.OrderDetails;
DELETE FROM dbo.OrderShippingHistory;
DELETE FROM dbo.OrderHistory;
DELETE FROM dbo.ShopRevenue;
DELETE FROM dbo.Orders;

PRINT 'All orders data deleted successfully.';

-- =====================================================
-- STEP 3: RESET IDENTITY COLUMNS
-- =====================================================
PRINT 'Resetting identity columns...';

DBCC CHECKIDENT ('dbo.Reviews', RESEED, 0); -- *** THÊM DÒNG NÀY ***
DBCC CHECKIDENT ('dbo.Orders', RESEED, 0);
DBCC CHECKIDENT ('dbo.OrderDetails', RESEED, 0);
DBCC CHECKIDENT ('dbo.OrderHistory', RESEED, 0);
DBCC CHECKIDENT ('dbo.OrderShippingHistory', RESEED, 0);
DBCC CHECKIDENT ('dbo.ShopRevenue', RESEED, 0);

PRINT 'Identity columns reset successfully.';

-- =====================================================
-- STEP 4: RE-ENABLE FOREIGN KEY CONSTRAINTS
-- =====================================================
PRINT 'Re-enabling foreign key constraints...';

ALTER TABLE dbo.Reviews WITH CHECK CHECK CONSTRAINT ALL; -- *** THÊM DÒNG NÀY ***
ALTER TABLE dbo.OrderDetail_Toppings WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderDetails WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderShippingHistory WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE dbo.OrderHistory WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE dbo.ShopRevenue WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE dbo.Orders WITH CHECK CHECK CONSTRAINT ALL;

PRINT 'Foreign key constraints re-enabled.';

-- =====================================================
-- STEP 5: INSERT FRESH ORDERS DATA
-- =====================================================
PRINT '=====================================================';
PRINT 'INSERTING FRESH ORDERS DATA... (Bắt đầu từ đây)';
PRINT '=====================================================';


DECLARE @ShopID1 INT = 1, @ShopID2 INT = 2;
DECLARE @CustomerID1 INT = 4, @CustomerID2 INT = 5, @CustomerID3 INT = 7;
DECLARE @ShipperID INT = 6;

-- =====================================================
-- ORDER 1: Completed - Đã giao thành công (Shop 1)
-- =====================================================
PRINT 'Creating Order 1 (Completed)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (@CustomerID1, @ShopID1, DATEADD(DAY, -10, GETDATE()), 'Completed', 'COD', 'Paid', 1, 
        N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 45000, 20000, 0, 65000, @ShipperID, DATEADD(DAY, -8, GETDATE()));
DECLARE @OrderID1 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID1, 2, 1, 45000, 45000); -- Trà Sữa Phúc Long size M

-- OrderShippingHistory cho Order 1
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID1, @ShipperID, 'Assigned', DATEADD(DAY, -10, GETDATE()), N'Đơn hàng được gán cho shipper'),
(@OrderID1, @ShipperID, 'Picking_Up', DATEADD(DAY, -9, DATEADD(HOUR, 2, GETDATE())), N'Đang lấy hàng'),
(@OrderID1, @ShipperID, 'Delivering', DATEADD(DAY, -9, DATEADD(HOUR, 4, GETDATE())), N'Đang giao hàng'),
(@OrderID1, @ShipperID, 'Delivered', DATEADD(DAY, -8, GETDATE()), N'Đã giao thành công');

-- OrderHistory cho Order 1
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID1, 'Pending', 'Confirmed', 2, DATEADD(DAY, -10, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID1, 'Confirmed', 'Delivering', 2, DATEADD(DAY, -10, DATEADD(HOUR, 1, GETDATE())), N'Gán shipper: Nguyễn Văn E'),
(@OrderID1, 'Delivering', 'Completed', @ShipperID, DATEADD(DAY, -8, GETDATE()), N'Đơn hàng đã được giao thành công');

-- ShopRevenue cho Order 1
INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
VALUES (@ShopID1, @OrderID1, 65000, 65000 * 5.00 / 100, 65000 * (100 - 5.00) / 100, DATEADD(DAY, -8, GETDATE()));

-- =====================================================
-- ORDER 2: Completed with toppings (Shop 1)
-- =====================================================
PRINT 'Creating Order 2 (Completed with toppings)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (@CustomerID2, @ShopID1, DATEADD(DAY, -7, GETDATE()), 'Completed', 'VNPay', 'Paid', 1, 
        N'789 Pasteur, Phường 6, Quận 3, TP.HCM', 
        N'Phạm Bảo Trân', '0987654321', 116000, 20000, 0, 136000, @ShipperID, DATEADD(DAY, -5, GETDATE()));
DECLARE @OrderID2 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID2, 2, 2, 45000, 90000), -- 2x Trà Sữa Phúc Long size M
(@OrderID2, 7, 1, 48000, 48000); -- 1x Trà Sữa Ô Long size M

DECLARE @OrderDetailID2_1 INT = SCOPE_IDENTITY() - 1;
INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID2_1, 1, 8000), -- Trân châu đen
(@OrderDetailID2_1, 4, 10000); -- Pudding

-- OrderShippingHistory cho Order 2
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID2, @ShipperID, 'Assigned', DATEADD(DAY, -7, GETDATE()), N'Đơn hàng được gán'),
(@OrderID2, @ShipperID, 'Picking_Up', DATEADD(DAY, -6, DATEADD(HOUR, 2, GETDATE())), N'Đang lấy hàng'),
(@OrderID2, @ShipperID, 'Delivering', DATEADD(DAY, -6, DATEADD(HOUR, 4, GETDATE())), N'Đang giao hàng'),
(@OrderID2, @ShipperID, 'Delivered', DATEADD(DAY, -5, GETDATE()), N'Giao hàng thành công');

-- ShopRevenue cho Order 2
INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
VALUES (@ShopID1, @OrderID2, 136000, 136000 * 5.00 / 100, 136000 * (100 - 5.00) / 100, DATEADD(DAY, -5, GETDATE()));

-- =====================================================
-- ORDER 3: Delivering - Đang giao (Shop 2)
-- =====================================================
PRINT 'Creating Order 3 (Delivering)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID)
VALUES (@CustomerID3, @ShopID2, DATEADD(DAY, -2, GETDATE()), 'Delivering', 'COD', 'Unpaid', 1, 
        N'321 Hai Bà Trưng, Phường Tân Định, Quận 1, TP.HCM', 
        N'Hoàng Đức Minh', '0923456789', 70000, 20000, 10000, 80000, @ShipperID);
DECLARE @OrderID3 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID3, 11, 2, 30000, 60000), -- 2x Cà Phê Đen size M
(@OrderID3, 14, 1, 35000, 35000); -- 1x Cà Phê Sữa size M

-- OrderShippingHistory cho Order 3
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID3, @ShipperID, 'Assigned', DATEADD(DAY, -2, GETDATE()), N'Đơn hàng được gán'),
(@OrderID3, @ShipperID, 'Picking_Up', DATEADD(DAY, -2, DATEADD(HOUR, 2, GETDATE())), N'Đang lấy hàng'),
(@OrderID3, @ShipperID, 'Delivering', DATEADD(DAY, -1, GETDATE()), N'Đang giao hàng');

-- OrderHistory cho Order 3
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID3, 'Pending', 'Confirmed', 3, DATEADD(DAY, -2, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID3, 'Confirmed', 'Delivering', 3, DATEADD(DAY, -2, DATEADD(HOUR, 1, GETDATE())), N'Gán shipper: Nguyễn Văn E');

-- =====================================================
-- ORDER 4: Pending - Mới tạo (Shop 2)
-- =====================================================
PRINT 'Creating Order 4 (Pending)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID1, @ShopID2, DATEADD(HOUR, -3, GETDATE()), 'Pending', 'COD', 'Unpaid', 2, 
        N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 52000, 15000, 0, 67000);
DECLARE @OrderID4 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID4, 16, 1, 52000, 52000); -- Trà Sữa Olong Macchiato size M

-- =====================================================
-- ORDER 5: Confirmed - Chờ gán shipper (Shop 1)
-- =====================================================
PRINT 'Creating Order 5 (Confirmed - Waiting for shipper)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID1, @ShopID1, DATEADD(HOUR, -5, GETDATE()), 'Confirmed', 'COD', 'Unpaid', 1, 
        N'456 Nguyễn Thị Minh Khai, Phường 5, Quận 3, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 95000, 20000, 0, 115000);
DECLARE @OrderID5 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID5, 2, 1, 45000, 45000),  -- Trà Sữa Phúc Long size M
(@OrderID5, 5, 1, 50000, 50000);  -- Trà Sữa Trân Châu size L

DECLARE @OrderDetailID5_1 INT = SCOPE_IDENTITY() - 1;
INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID5_1, 1, 8000),  -- Trân châu đen
(@OrderDetailID5_1, 2, 8000);  -- Trân châu trắng

-- OrderHistory cho Order 5
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID5, 'Pending', 'Confirmed', 2, DATEADD(HOUR, -5, GETDATE()), N'Vendor xác nhận đơn hàng');

-- =====================================================
-- ORDER 6: Confirmed - Chờ gán shipper (Shop 2)
-- =====================================================
PRINT 'Creating Order 6 (Confirmed - Waiting for shipper)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID2, @ShopID2, DATEADD(HOUR, -2, GETDATE()), 'Confirmed', 'VNPay', 'Paid', 1, 
        N'789 Pasteur, Phường 6, Quận 3, TP.HCM', 
        N'Phạm Bảo Trân', '0987654321', 120000, 20000, 10000, 130000);
DECLARE @OrderID6 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID6, 11, 2, 30000, 60000),  -- 2x Cà Phê Đen size M
(@OrderID6, 14, 2, 35000, 70000);  -- 2x Cà Phê Sữa size M

-- OrderHistory cho Order 6
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID6, 'Pending', 'Confirmed', 3, DATEADD(HOUR, -2, GETDATE()), N'Vendor xác nhận đơn hàng');

-- =====================================================
-- ORDER 7: Delivering - Đang giao với nhiều topping (Shop 1)
-- =====================================================
PRINT 'Creating Order 7 (Delivering with toppings)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID)
VALUES (@CustomerID3, @ShopID1, DATEADD(HOUR, -8, GETDATE()), 'Delivering', 'COD', 'Unpaid', 1, 
        N'321 Hai Bà Trưng, Phường Tân Định, Quận 1, TP.HCM', 
        N'Hoàng Đức Minh', '0923456789', 145000, 20000, 15000, 150000, @ShipperID);
DECLARE @OrderID7 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID7, 2, 2, 45000, 90000),  -- 2x Trà Sữa Phúc Long size M
(@OrderID7, 7, 1, 48000, 48000);  -- 1x Trà Sữa Ô Long size M

DECLARE @OrderDetailID7_1 INT = SCOPE_IDENTITY() - 1;
INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID7_1, 1, 8000),  -- Trân châu đen
(@OrderDetailID7_1, 3, 8000),  -- Thạch dừa
(@OrderDetailID7_1, 4, 10000); -- Pudding

-- OrderShippingHistory cho Order 7
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID7, @ShipperID, 'Assigned', DATEADD(HOUR, -7, GETDATE()), N'Đơn hàng đã được gán cho shipper'),
(@OrderID7, @ShipperID, 'Picking_Up', DATEADD(HOUR, -6, GETDATE()), N'Đang trên đường đến lấy hàng'),
(@OrderID7, @ShipperID, 'Delivering', DATEADD(HOUR, -5, GETDATE()), N'Đã lấy hàng và đang giao');

-- OrderHistory cho Order 7
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID7, 'Pending', 'Confirmed', 2, DATEADD(HOUR, -8, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID7, 'Confirmed', 'Delivering', 2, DATEADD(HOUR, -7, GETDATE()), N'Gán shipper: Nguyễn Văn E');

-- =====================================================
-- ORDER 8: Pending - Mới tạo (Shop 1)
-- =====================================================
PRINT 'Creating Order 8 (Pending - New)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID1, @ShopID1, DATEADD(MINUTE, -30, GETDATE()), 'Pending', 'COD', 'Unpaid', 2, 
        N'555 Võ Văn Tần, Phường 5, Quận 3, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 80000, 15000, 0, 95000);
DECLARE @OrderID8 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID8, 5, 1, 50000, 50000),  -- Trà Sữa Trân Châu size L
(@OrderID8, 2, 1, 45000, 45000);  -- Trà Sữa Phúc Long size M

-- =====================================================
-- ORDER 9: Delivering - Đang giao (Shop 2)
-- =====================================================
PRINT 'Creating Order 9 (Delivering)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID)
VALUES (@CustomerID2, @ShopID2, DATEADD(HOUR, -4, GETDATE()), 'Delivering', 'COD', 'Unpaid', 1, 
        N'999 Cách Mạng Tháng 8, Phường 7, Quận 3, TP.HCM', 
        N'Phạm Bảo Trân', '0987654321', 100000, 20000, 0, 120000, @ShipperID);
DECLARE @OrderID9 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID9, 16, 2, 52000, 104000);  -- 2x Trà Sữa Olong Macchiato size M

-- OrderShippingHistory cho Order 9
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID9, @ShipperID, 'Assigned', DATEADD(HOUR, -3, GETDATE()), N'Đơn hàng đã được gán cho shipper'),
(@OrderID9, @ShipperID, 'Picking_Up', DATEADD(HOUR, -2, GETDATE()), N'Đang đi lấy hàng'),
(@OrderID9, @ShipperID, 'Delivering', DATEADD(HOUR, -1, GETDATE()), N'Đang giao hàng cho khách');

-- OrderHistory cho Order 9
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID9, 'Pending', 'Confirmed', 3, DATEADD(HOUR, -4, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID9, 'Confirmed', 'Delivering', 3, DATEADD(HOUR, -3, GETDATE()), N'Gán shipper: Nguyễn Văn E');

-- =====================================================
-- ORDER 10: Completed - Đã giao với nhiều sản phẩm (Shop 1)
-- =====================================================
PRINT 'Creating Order 10 (Completed with multiple items)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (@CustomerID3, @ShopID1, DATEADD(DAY, -3, GETDATE()), 'Completed', 'COD', 'Paid', 1, 
        N'777 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM', 
        N'Hoàng Đức Minh', '0923456789', 150000, 20000, 20000, 150000, @ShipperID, DATEADD(DAY, -2, GETDATE()));
DECLARE @OrderID10 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID10, 2, 3, 45000, 135000),  -- 3x Trà Sữa Phúc Long size M
(@OrderID10, 7, 1, 48000, 48000);   -- 1x Trà Sữa Ô Long size M

DECLARE @OrderDetailID10_1 INT = SCOPE_IDENTITY() - 1;
INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID10_1, 1, 8000),  -- Trân châu đen
(@OrderDetailID10_1, 4, 10000); -- Pudding

-- OrderShippingHistory cho Order 10
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID10, @ShipperID, 'Assigned', DATEADD(DAY, -3, GETDATE()), N'Đơn hàng được gán'),
(@OrderID10, @ShipperID, 'Picking_Up', DATEADD(DAY, -3, DATEADD(HOUR, 1, GETDATE())), N'Đang lấy hàng'),
(@OrderID10, @ShipperID, 'Delivering', DATEADD(DAY, -3, DATEADD(HOUR, 2, GETDATE())), N'Đang giao hàng'),
(@OrderID10, @ShipperID, 'Delivered', DATEADD(DAY, -2, GETDATE()), N'Đã giao thành công, khách hàng đã nhận hàng và thanh toán');

-- OrderHistory cho Order 10
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID10, 'Pending', 'Confirmed', 2, DATEADD(DAY, -3, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID10, 'Confirmed', 'Delivering', 2, DATEADD(DAY, -3, DATEADD(HOUR, 1, GETDATE())), N'Gán shipper: Nguyễn Văn E'),
(@OrderID10, 'Delivering', 'Completed', @ShipperID, DATEADD(DAY, -2, GETDATE()), N'Đơn hàng đã được giao thành công');

-- ShopRevenue cho Order 10
INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
VALUES (@ShopID1, @OrderID10, 150000, 150000 * 5.00 / 100, 150000 * (100 - 5.00) / 100, DATEADD(DAY, -2, GETDATE()));

-- =====================================================
-- ORDER 11: Confirmed - Có promotion (Shop 1)
-- =====================================================
PRINT 'Creating Order 11 (Confirmed with promotion)...';

INSERT INTO dbo.Orders (UserID, ShopID, PromotionID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID1, @ShopID1, 1, DATEADD(HOUR, -6, GETDATE()), 'Confirmed', 'VNPay', 'Paid', 1, 
        N'888 Điện Biên Phủ, Phường 22, Quận Bình Thạnh, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 200000, 20000, 30000, 190000);
DECLARE @OrderID11 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID11, 5, 2, 50000, 100000),  -- 2x Trà Sữa Trân Châu size L
(@OrderID11, 2, 2, 45000, 90000);   -- 2x Trà Sữa Phúc Long size M

DECLARE @OrderDetailID11_1 INT = SCOPE_IDENTITY() - 1;
DECLARE @OrderDetailID11_2 INT = SCOPE_IDENTITY();
INSERT INTO dbo.OrderDetail_Toppings (OrderDetailID, ToppingID, UnitPrice) VALUES
(@OrderDetailID11_1, 1, 8000),  -- Trân châu đen
(@OrderDetailID11_1, 2, 8000),  -- Trân châu trắng
(@OrderDetailID11_2, 3, 8000);  -- Thạch dừa

-- OrderHistory cho Order 11
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID11, 'Pending', 'Confirmed', 2, DATEADD(HOUR, -6, GETDATE()), N'Vendor xác nhận đơn hàng');

-- =====================================================
-- ORDER 12: Canceled - Đã hủy (Shop 2)
-- =====================================================
PRINT 'Creating Order 12 (Canceled)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, Notes)
VALUES (@CustomerID2, @ShopID2, DATEADD(DAY, -1, GETDATE()), 'Canceled', 'COD', 'Unpaid', 1, 
        N'789 Pasteur, Phường 6, Quận 3, TP.HCM', 
        N'Phạm Bảo Trân', '0987654321', 35000, 20000, 0, 55000, N'Khách hàng báo bận, hủy đơn');
DECLARE @OrderID12 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID12, 14, 1, 35000, 35000); -- 1x Cà Phê Sữa size M

-- OrderHistory cho Order 12
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID12, 'Pending', 'Confirmed', 3, DATEADD(DAY, -1, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID12, 'Confirmed', 'Canceled', 3, DATEADD(DAY, -1, DATEADD(HOUR, 2, GETDATE())), N'Vendor hủy đơn theo yêu cầu của khách');

-- =====================================================
-- ORDER 13: Completed - (Shop 1)
-- =====================================================
PRINT 'Creating Order 13 (Completed)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (@CustomerID2, @ShopID1, DATEADD(DAY, -4, GETDATE()), 'Completed', 'COD', 'Paid', 1, 
        N'789 Pasteur, Phường 6, Quận 3, TP.HCM', 
        N'Phạm Bảo Trân', '0987654321', 50000, 20000, 0, 70000, @ShipperID, DATEADD(DAY, -3, GETDATE()));
DECLARE @OrderID13 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID13, 5, 1, 50000, 50000); -- 1x Trà Sữa Trân Châu size L

-- OrderShippingHistory cho Order 13
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID13, @ShipperID, 'Assigned', DATEADD(DAY, -4, GETDATE()), N'Đơn hàng được gán'),
(@OrderID13, @ShipperID, 'Picking_Up', DATEADD(DAY, -4, DATEADD(HOUR, 1, GETDATE())), N'Đang lấy hàng'),
(@OrderID13, @ShipperID, 'Delivering', DATEADD(DAY, -4, DATEADD(HOUR, 2, GETDATE())), N'Đang giao hàng'),
(@OrderID13, @ShipperID, 'Delivered', DATEADD(DAY, -3, GETDATE()), N'Đã giao thành công');

-- OrderHistory cho Order 13
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID13, 'Pending', 'Confirmed', 2, DATEADD(DAY, -4, GETDATE()), N'Vendor xác nhận'),
(@OrderID13, 'Confirmed', 'Delivering', 2, DATEADD(DAY, -4, GETDATE()), N'Gán shipper'),
(@OrderID13, 'Delivering', 'Completed', @ShipperID, DATEADD(DAY, -3, GETDATE()), N'Giao hàng thành công');

-- ShopRevenue cho Order 13
INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
VALUES (@ShopID1, @OrderID13, 70000, 70000 * 5.00 / 100, 70000 * (100 - 5.00) / 100, DATEADD(DAY, -3, GETDATE()));

-- =====================================================
-- ORDER 14: Pending - Mới tạo (Shop 2)
-- =====================================================
PRINT 'Creating Order 14 (Pending)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal)
VALUES (@CustomerID3, @ShopID2, DATEADD(MINUTE, -15, GETDATE()), 'Pending', 'COD', 'Unpaid', 1, 
        N'321 Hai Bà Trưng, Phường Tân Định, Quận 1, TP.HCM', 
        N'Hoàng Đức Minh', '0923456789', 30000, 20000, 0, 50000);
DECLARE @OrderID14 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID14, 11, 1, 30000, 30000); -- 1x Cà Phê Đen size M

-- =====================================================
-- ORDER 15: Completed - 100% discount item (Shop 2)
-- =====================================================
PRINT 'Creating Order 15 (Completed with 100% product discount)...';

INSERT INTO dbo.Orders (UserID, ShopID, OrderDate, OrderStatus, PaymentMethod, PaymentStatus, ShippingProviderID, ShippingAddress, RecipientName, RecipientPhone, Subtotal, ShippingFee, DiscountAmount, GrandTotal, ShipperID, CompletedAt)
VALUES (@CustomerID1, @ShopID2, DATEADD(DAY, -1, GETDATE()), 'Completed', 'COD', 'Paid', 1, 
        N'123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM', 
        N'Lê Minh Anh', '0912345678', 52000, 20000, 52000, 20000, @ShipperID, DATEADD(DAY, -1, DATEADD(HOUR, 2, GETDATE())));
DECLARE @OrderID15 INT = SCOPE_IDENTITY();

INSERT INTO dbo.OrderDetails (OrderID, VariantID, Quantity, UnitPrice, Subtotal) VALUES
(@OrderID15, 16, 1, 52000, 52000); -- Trà Sữa Olong Macchiato size M (được giảm 100% ở Order level)

-- OrderShippingHistory cho Order 15
INSERT INTO dbo.OrderShippingHistory (OrderID, ShipperID, Status, Timestamp, Notes)
VALUES 
(@OrderID15, @ShipperID, 'Assigned', DATEADD(DAY, -1, GETDATE()), N'Đơn hàng được gán'),
(@OrderID15, @ShipperID, 'Delivering', DATEADD(DAY, -1, DATEADD(HOUR, 1, GETDATE())), N'Đang giao hàng'),
(@OrderID15, @ShipperID, 'Delivered', DATEADD(DAY, -1, DATEADD(HOUR, 2, GETDATE())), N'Đã giao thành công');

-- OrderHistory cho Order 15
INSERT INTO dbo.OrderHistory (OrderID, OldStatus, NewStatus, ChangedByUserID, Timestamp, Notes)
VALUES
(@OrderID15, 'Pending', 'Confirmed', 3, DATEADD(DAY, -1, GETDATE()), N'Vendor xác nhận đơn hàng'),
(@OrderID15, 'Confirmed', 'Delivering', 3, DATEADD(DAY, -1, GETDATE()), N'Gán shipper'),
(@OrderID15, 'Delivering', 'Completed', @ShipperID, DATEADD(DAY, -1, DATEADD(HOUR, 2, GETDATE())), N'Đơn hàng đã được giao thành công');

-- ShopRevenue cho Order 15 (Doanh thu thực tính là 20k (chỉ tiền ship))
INSERT INTO dbo.ShopRevenue (ShopID, OrderID, OrderAmount, CommissionAmount, NetRevenue, RecordedAt)
VALUES (@ShopID2, @OrderID15, 20000, 20000 * 5.00 / 100, 20000 * (100 - 5.00) / 100, DATEADD(DAY, -1, DATEADD(HOUR, 2, GETDATE())));


PRINT '=====================================================';
PRINT 'FINISHED INSERTING ALL ORDERS.';
PRINT '=====================================================';
PRINT 'ORDERS RESET PROCESS COMPLETED SUCCESSFULLY.';
PRINT '=====================================================';
GO