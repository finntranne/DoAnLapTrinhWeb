SET QUOTED_IDENTIFIER ON;
GO
SET ANSI_NULLS ON;
GO

IF COL_LENGTH('dbo.Addresses', 'province') IS NULL
    ALTER TABLE dbo.Addresses ADD province NVARCHAR(500) NULL;
GO
IF COL_LENGTH('dbo.Addresses', 'district') IS NULL
    ALTER TABLE dbo.Addresses ADD district NVARCHAR(500) NULL;
GO
IF COL_LENGTH('dbo.Addresses', 'ward') IS NULL
    ALTER TABLE dbo.Addresses ADD ward NVARCHAR(500) NULL;
GO
IF COL_LENGTH('dbo.Addresses', 'street_address') IS NULL
    ALTER TABLE dbo.Addresses ADD street_address NVARCHAR(500) NULL;
GO

UPDATE dbo.Addresses
SET province = COALESCE(NULLIF(province, N''), FullAddress, AddressName, N'Unknown'),
    district = COALESCE(NULLIF(district, N''), FullAddress, AddressName, N'Unknown'),
    ward = COALESCE(NULLIF(ward, N''), FullAddress, AddressName, N'Unknown'),
    street_address = COALESCE(NULLIF(street_address, N''), FullAddress, AddressName, N'Unknown');
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Addresses')
      AND name = 'province'
      AND is_nullable = 1
)
    ALTER TABLE dbo.Addresses ALTER COLUMN province NVARCHAR(500) NOT NULL;
GO
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Addresses')
      AND name = 'district'
      AND is_nullable = 1
)
    ALTER TABLE dbo.Addresses ALTER COLUMN district NVARCHAR(500) NOT NULL;
GO
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Addresses')
      AND name = 'ward'
      AND is_nullable = 1
)
    ALTER TABLE dbo.Addresses ALTER COLUMN ward NVARCHAR(500) NOT NULL;
GO
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Addresses')
      AND name = 'street_address'
      AND is_nullable = 1
)
    ALTER TABLE dbo.Addresses ALTER COLUMN street_address NVARCHAR(500) NOT NULL;
GO

IF COL_LENGTH('dbo.Toppings', 'Price') IS NULL
    ALTER TABLE dbo.Toppings ADD Price DECIMAL(10, 2) NULL;
GO
UPDATE dbo.Toppings
SET Price = COALESCE(Price, AdditionalPrice, 0);
GO
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Toppings')
      AND name = 'Price'
      AND is_nullable = 1
)
    ALTER TABLE dbo.Toppings ALTER COLUMN Price DECIMAL(10, 2) NOT NULL;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OrderItems') AND name = 'OrderItemID')
   AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OrderDetails') AND name = 'OrderDetailID')
   AND NOT EXISTS (SELECT 1 FROM dbo.OrderItems)
   AND EXISTS (SELECT 1 FROM dbo.OrderDetails)
BEGIN
    SET IDENTITY_INSERT dbo.OrderItems ON;

    INSERT INTO dbo.OrderItems (OrderItemID, Price, Quantity, Toppings, OrderID, VariantID)
    SELECT od.OrderDetailID,
           od.UnitPrice,
           od.Quantity,
           NULL,
           od.OrderID,
           od.VariantID
    FROM dbo.OrderDetails od
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.OrderItems oi
        WHERE oi.OrderItemID = od.OrderDetailID
    );

    SET IDENTITY_INSERT dbo.OrderItems OFF;
END;
GO

IF COL_LENGTH('dbo.Orders', 'AddressID') IS NULL
    ALTER TABLE dbo.Orders ADD AddressID INT NULL;
GO

;WITH MissingOrderAddresses AS (
    SELECT DISTINCT
        o.UserID,
        COALESCE(NULLIF(o.ShippingAddress, N''), N'Unknown') AS ShippingAddress,
        COALESCE(NULLIF(o.RecipientName, N''), N'Unknown') AS RecipientName,
        COALESCE(NULLIF(o.RecipientPhone, ''), 'Unknown') AS RecipientPhone
    FROM dbo.Orders o
    WHERE o.AddressID IS NULL
)
INSERT INTO dbo.Addresses (
    AddressName,
    CreatedAt,
    FullAddress,
    IsDefault,
    PhoneNumber,
    RecipientName,
    UserID,
    province,
    district,
    ward,
    street_address
)
SELECT N'Imported order address',
       SYSUTCDATETIME(),
       moa.ShippingAddress,
       0,
       moa.RecipientPhone,
       moa.RecipientName,
       moa.UserID,
       moa.ShippingAddress,
       moa.ShippingAddress,
       moa.ShippingAddress,
       moa.ShippingAddress
FROM MissingOrderAddresses moa
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Addresses a
    WHERE a.UserID = moa.UserID
      AND a.FullAddress = moa.ShippingAddress
      AND a.RecipientName = moa.RecipientName
      AND a.PhoneNumber = moa.RecipientPhone
);
GO

UPDATE o
SET AddressID = a.AddressID
FROM dbo.Orders o
CROSS APPLY (
    SELECT TOP 1 a.AddressID
    FROM dbo.Addresses a
    WHERE a.UserID = o.UserID
      AND a.FullAddress = COALESCE(NULLIF(o.ShippingAddress, N''), N'Unknown')
      AND a.RecipientName = COALESCE(NULLIF(o.RecipientName, N''), N'Unknown')
      AND a.PhoneNumber = COALESCE(NULLIF(o.RecipientPhone, ''), 'Unknown')
    ORDER BY a.IsDefault DESC, a.AddressID
) a
WHERE o.AddressID IS NULL;
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Orders')
      AND name = 'AddressID'
      AND is_nullable = 1
)
AND NOT EXISTS (
    SELECT 1
    FROM dbo.Orders
    WHERE AddressID IS NULL
)
    ALTER TABLE dbo.Orders ALTER COLUMN AddressID INT NOT NULL;
GO

IF COL_LENGTH('dbo.Reviews', 'OrderItemID') IS NULL
    ALTER TABLE dbo.Reviews ADD OrderItemID INT NULL;
GO

UPDATE r
SET OrderItemID = oi.OrderItemID
FROM dbo.Reviews r
JOIN dbo.OrderItems oi ON oi.OrderItemID = r.OrderDetailID
WHERE r.OrderItemID IS NULL;
GO
