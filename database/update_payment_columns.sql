-- Migration: Increase payment_url and qr_code column length
-- Purpose: Support long VNPay URLs and VietQR image URLs
-- Date: 2026-04-05

USE MilkTeaShopDB12;

-- Check current column size
-- SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'payments' AND COLUMN_NAME IN ('payment_url', 'qr_code');

-- Update payment_url column from VARCHAR(255) to VARCHAR(2000)
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'payments' AND COLUMN_NAME = 'payment_url')
BEGIN
    ALTER TABLE payments
    ALTER COLUMN payment_url VARCHAR(2000);
    PRINT 'Updated payment_url column to VARCHAR(2000)';
END

-- Update qr_code column from VARCHAR(255) to VARCHAR(2000)
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'payments' AND COLUMN_NAME = 'qr_code')
BEGIN
    ALTER TABLE payments
    ALTER COLUMN qr_code VARCHAR(2000);
    PRINT 'Updated qr_code column to VARCHAR(2000)';
END

PRINT 'Migration completed successfully!';
