-- AloTra Milk Tea Chain Database Schema
-- Optimized for SQL Server

-- Create database
IF DB_ID('alotra_store_db') IS NOT NULL
    DROP DATABASE alotra_store_db;
GO
CREATE DATABASE alotra_store_db;
GO
USE alotra_store_db;
GO

-- 1. User Management Tables
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME()
);
GO

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    email NVARCHAR(100) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    first_name NVARCHAR(50) NOT NULL,
    last_name NVARCHAR(50) NOT NULL,
    phone NVARCHAR(20),
    avatar_url NVARCHAR(500),
    date_of_birth DATE,
    gender NVARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    is_active BIT DEFAULT 1,
    is_email_verified BIT DEFAULT 0,
    last_login DATETIME2,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    INDEX idx_username NONCLUSTERED (username),
    INDEX idx_email NONCLUSTERED (email),
    INDEX idx_phone NONCLUSTERED (phone)
);
GO

CREATE TABLE user_roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_user_roles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_role UNIQUE (user_id, role_id)
);
GO

-- 2. Address Management
CREATE TABLE provinces (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    code NVARCHAR(10) NOT NULL UNIQUE
);
GO

CREATE TABLE districts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    code NVARCHAR(10) NOT NULL,
    province_id BIGINT NOT NULL,
    
    CONSTRAINT FK_districts_provinces FOREIGN KEY (province_id) REFERENCES provinces(id)
);
GO

CREATE TABLE wards (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    code NVARCHAR(10) NOT NULL,
    district_id BIGINT NOT NULL,
    
    CONSTRAINT FK_wards_districts FOREIGN KEY (district_id) REFERENCES districts(id)
);
GO

CREATE TABLE user_addresses (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) NOT NULL,
    address_line NVARCHAR(255) NOT NULL,
    ward_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    is_default BIT DEFAULT 0,
    address_type NVARCHAR(10) DEFAULT 'HOME' CHECK (address_type IN ('HOME', 'WORK', 'OTHER')),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_user_addresses_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_addresses_wards FOREIGN KEY (ward_id) REFERENCES wards(id),
    CONSTRAINT FK_user_addresses_districts FOREIGN KEY (district_id) REFERENCES districts(id),
    CONSTRAINT FK_user_addresses_provinces FOREIGN KEY (province_id) REFERENCES provinces(id)
);
GO

-- 3. Store Management
CREATE TABLE stores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20),
    email NVARCHAR(100),
    address_line NVARCHAR(255) NOT NULL,
    ward_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    opening_hours NVARCHAR(MAX), -- Store opening hours in JSON format
    is_active BIT DEFAULT 1,
    manager_id BIGINT,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_stores_wards FOREIGN KEY (ward_id) REFERENCES wards(id),
    CONSTRAINT FK_stores_districts FOREIGN KEY (district_id) REFERENCES districts(id),
    CONSTRAINT FK_stores_provinces FOREIGN KEY (province_id) REFERENCES provinces(id),
    CONSTRAINT FK_stores_manager FOREIGN KEY (manager_id) REFERENCES users(id),
    
    INDEX idx_location NONCLUSTERED (latitude, longitude),
    INDEX idx_active NONCLUSTERED (is_active)
);
GO

-- 4. Product Management
CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    image_url NVARCHAR(500),
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
    INDEX idx_parent NONCLUSTERED (parent_id),
    INDEX idx_active NONCLUSTERED (is_active)
);
GO

CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    slug NVARCHAR(120) NOT NULL UNIQUE,
    description NVARCHAR(MAX),
    short_description NVARCHAR(255),
    base_price DECIMAL(10, 2) NOT NULL,
    image_url NVARCHAR(500),
    category_id BIGINT NOT NULL,
    is_active BIT DEFAULT 1,
    is_featured BIT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_slug NONCLUSTERED (slug),
    INDEX idx_category NONCLUSTERED (category_id),
    INDEX idx_active NONCLUSTERED (is_active),
    INDEX idx_featured NONCLUSTERED (is_featured)
);
GO

CREATE TABLE product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url NVARCHAR(500) NOT NULL,
    alt_text NVARCHAR(255),
    is_primary BIT DEFAULT 0,
    sort_order INT DEFAULT 0,
    
    CONSTRAINT FK_product_images_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
GO

-- 5. Product Variations (Size, Toppings, etc.)
CREATE TABLE variation_types (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL,
    description NVARCHAR(MAX),
    is_required BIT DEFAULT 0,
    is_multiple_choice BIT DEFAULT 0,
    sort_order INT DEFAULT 0
);
GO

CREATE TABLE variation_options (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    variation_type_id BIGINT NOT NULL,
    name NVARCHAR(50) NOT NULL,
    price_adjustment DECIMAL(10, 2) DEFAULT 0.00,
    is_default BIT DEFAULT 0,
    is_active BIT DEFAULT 1,
    sort_order INT DEFAULT 0,
    
    CONSTRAINT FK_variation_options_types FOREIGN KEY (variation_type_id) REFERENCES variation_types(id)
);
GO

CREATE TABLE product_variations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variation_type_id BIGINT NOT NULL,
    is_required BIT DEFAULT 0,
    
    CONSTRAINT FK_product_variations_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT FK_product_variations_types FOREIGN KEY (variation_type_id) REFERENCES variation_types(id),
    CONSTRAINT unique_product_variation UNIQUE (product_id, variation_type_id)
);
GO

-- 6. Shopping Cart
CREATE TABLE carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    session_id NVARCHAR(100),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_carts_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user NONCLUSTERED (user_id),
    INDEX idx_session NONCLUSTERED (session_id)
);
GO

CREATE TABLE cart_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    variations NVARCHAR(MAX), -- Store selected variations as JSON
    special_instructions NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_cart_items_carts FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_products FOREIGN KEY (product_id) REFERENCES products(id)
);
GO

-- 7. Orders Management
CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_number NVARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT,
    customer_name NVARCHAR(100) NOT NULL,
    customer_phone NVARCHAR(20) NOT NULL,
    customer_email NVARCHAR(100),
    
    -- Delivery Information
    delivery_type NVARCHAR(20) NOT NULL CHECK (delivery_type IN ('DELIVERY', 'PICKUP')),
    store_id BIGINT,
    delivery_address NVARCHAR(MAX),
    delivery_ward_id BIGINT,
    delivery_district_id BIGINT,
    delivery_province_id BIGINT,
    
    -- Order Details
    subtotal DECIMAL(10, 2) NOT NULL,
    delivery_fee DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    
    -- Payment Information
    payment_method NVARCHAR(20) NOT NULL CHECK (payment_method IN ('COD', 'BANK_TRANSFER', 'E_WALLET', 'CREDIT_CARD')),
    payment_status NVARCHAR(20) DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    
    -- Order Status
    status NVARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'DELIVERED', 'CANCELLED')),
    
    -- Additional Information
    notes NVARCHAR(MAX),
    delivery_time DATETIME2,
    estimated_delivery_time DATETIME2,
    
    -- Timestamps
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_orders_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_orders_stores FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT FK_orders_wards FOREIGN KEY (delivery_ward_id) REFERENCES wards(id),
    CONSTRAINT FK_orders_districts FOREIGN KEY (delivery_district_id) REFERENCES districts(id),
    CONSTRAINT FK_orders_provinces FOREIGN KEY (delivery_province_id) REFERENCES provinces(id),
    
    INDEX idx_order_number NONCLUSTERED (order_number),
    INDEX idx_user NONCLUSTERED (user_id),
    INDEX idx_status NONCLUSTERED (status),
    INDEX idx_created_at NONCLUSTERED (created_at)
);
GO

CREATE TABLE order_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    variations NVARCHAR(MAX),
    special_instructions NVARCHAR(MAX),
    
    CONSTRAINT FK_order_items_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_products FOREIGN KEY (product_id) REFERENCES products(id)
);
GO

-- 8. Promotions & Coupons
CREATE TABLE promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    promotion_type NVARCHAR(20) NOT NULL CHECK (promotion_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING', 'BUY_X_GET_Y')),
    discount_value DECIMAL(10, 2),
    minimum_order_amount DECIMAL(10, 2),
    maximum_discount_amount DECIMAL(10, 2),
    start_date DATETIME2 NOT NULL,
    end_date DATETIME2 NOT NULL,
    usage_limit INT,
    usage_count INT DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    INDEX idx_dates NONCLUSTERED (start_date, end_date),
    INDEX idx_active NONCLUSTERED (is_active)
);
GO

CREATE TABLE coupons (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,
    promotion_id BIGINT NOT NULL,
    user_id BIGINT,
    usage_limit INT DEFAULT 1,
    usage_count INT DEFAULT 0,
    is_active BIT DEFAULT 1,
    expires_at DATETIME2,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_coupons_promotions FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT FK_coupons_users FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_code NONCLUSTERED (code)
);
GO

CREATE TABLE order_promotions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    promotion_id BIGINT NOT NULL,
    coupon_code NVARCHAR(50),
    discount_amount DECIMAL(10, 2) NOT NULL,
    
    CONSTRAINT FK_order_promotions_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_promotions_promotions FOREIGN KEY (promotion_id) REFERENCES promotions(id)
);
GO

-- 9. Reviews & Ratings
CREATE TABLE reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title NVARCHAR(200),
    comment NVARCHAR(MAX),
    is_verified_purchase BIT DEFAULT 0,
    is_approved BIT DEFAULT 1,
    helpful_count INT DEFAULT 0,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_reviews_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_reviews_products FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT FK_reviews_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_product NONCLUSTERED (product_id),
    INDEX idx_rating NONCLUSTERED (rating),
    INDEX idx_created_at NONCLUSTERED (created_at)
);
GO

CREATE TABLE review_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_url NVARCHAR(500) NOT NULL,
    
    CONSTRAINT FK_review_images_reviews FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);
GO

-- 10. Notifications & Messages
CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    notification_type NVARCHAR(20) NOT NULL CHECK (notification_type IN ('ORDER_UPDATE', 'PROMOTION', 'SYSTEM', 'REVIEW_REMINDER')),
    reference_id BIGINT,
    is_read BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_read NONCLUSTERED (user_id, is_read),
    INDEX idx_created_at NONCLUSTERED (created_at)
);
GO

-- 11. System Settings
CREATE TABLE settings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    setting_key NVARCHAR(100) NOT NULL UNIQUE,
    setting_value NVARCHAR(MAX),
    description NVARCHAR(MAX),
    setting_type NVARCHAR(20) DEFAULT 'STRING' CHECK (setting_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON')),
    updated_at DATETIME2 DEFAULT SYSDATETIME()
);
GO

-- 12. Chat/Support Messages (for WebSocket)
CREATE TABLE chat_rooms (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    support_agent_id BIGINT,
    status NVARCHAR(20) DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'WAITING')),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    updated_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_chat_rooms_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_chat_rooms_support_agent FOREIGN KEY (support_agent_id) REFERENCES users(id)
);
GO

CREATE TABLE chat_messages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    message_type NVARCHAR(10) DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    file_url NVARCHAR(500),
    is_read BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_chat_messages_rooms FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT FK_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    INDEX idx_room_created NONCLUSTERED (chat_room_id, created_at)
);
GO

-- 13. Analytics & Tracking
CREATE TABLE user_activities (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    session_id NVARCHAR(100),
    activity_type NVARCHAR(50) NOT NULL,
    reference_id BIGINT,
    metadata NVARCHAR(MAX),
    ip_address NVARCHAR(45),
    user_agent NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT SYSDATETIME(),
    
    CONSTRAINT FK_user_activities_users FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_created NONCLUSTERED (user_id, created_at),
    INDEX idx_activity_type NONCLUSTERED (activity_type),
    INDEX idx_created_at NONCLUSTERED (created_at)
);
GO

-- Insert initial data
INSERT INTO roles (name, description) VALUES 
(N'ADMIN', N'System Administrator'),
(N'STORE_MANAGER', N'Store Manager'),
(N'STAFF', N'Store Staff'),
(N'CUSTOMER', N'Customer'),
(N'SUPPORT_AGENT', N'Customer Support Agent');
GO

INSERT INTO variation_types (name, description, is_required, is_multiple_choice, sort_order) VALUES
(N'SIZE', N'Drink Size', 1, 0, 1),
(N'ICE_LEVEL', N'Ice Level', 1, 0, 2),
(N'SUGAR_LEVEL', N'Sugar Level', 1, 0, 3),
(N'TOPPINGS', N'Additional Toppings', 0, 1, 4);
GO

INSERT INTO variation_options (variation_type_id, name, price_adjustment, is_default, sort_order) VALUES
-- Size options
(1, N'Small', 0.00, 0, 1),
(1, N'Medium', 5000.00, 1, 2),
(1, N'Large', 10000.00, 0, 3),
-- Ice Level options
(2, N'No Ice', 0.00, 0, 1),
(2, N'Less Ice', 0.00, 0, 2),
(2, N'Normal Ice', 0.00, 1, 3),
(2, N'Extra Ice', 0.00, 0, 4),
-- Sugar Level options
(3, N'0%', 0.00, 0, 1),
(3, N'30%', 0.00, 0, 2),
(3, N'50%', 0.00, 0, 3),
(3, N'70%', 0.00, 1, 4),
(3, N'100%', 0.00, 0, 5),
-- Topping options
(4, N'Pearl', 5000.00, 0, 1),
(4, N'Jelly', 5000.00, 0, 2),
(4, N'Pudding', 7000.00, 0, 3),
(4, N'Red Bean', 6000.00, 0, 4);
GO

INSERT INTO settings (setting_key, setting_value, description, setting_type) VALUES
(N'DELIVERY_FEE', N'15000', N'Default delivery fee in VND', N'NUMBER'),
(N'FREE_DELIVERY_THRESHOLD', N'200000', N'Minimum order amount for free delivery', N'NUMBER'),
(N'MAX_DELIVERY_DISTANCE', N'10', N'Maximum delivery distance in km', N'NUMBER'),
(N'ORDER_TIMEOUT', N'30', N'Order timeout in minutes', N'NUMBER'),
(N'SITE_NAME', N'AloTra', N'Website name', N'STRING'),
(N'SITE_DESCRIPTION', N'Premium Milk Tea Chain', N'Website description', N'STRING');
GO

-- Triggers
CREATE TRIGGER tr_users_update
ON users
AFTER UPDATE
AS
BEGIN
    UPDATE users
    SET updated_at = SYSDATETIME()
    FROM users u
    INNER JOIN inserted i ON u.id = i.id;
END;
GO

CREATE TRIGGER tr_stores_update
ON stores
AFTER UPDATE
AS
BEGIN
    UPDATE stores
    SET updated_at = SYSDATETIME()
    FROM stores s
    INNER JOIN inserted i ON s.id = i.id;
END;
GO

CREATE TRIGGER tr_categories_update
ON categories
AFTER UPDATE
AS
BEGIN
    UPDATE categories
    SET updated_at = SYSDATETIME()
    FROM categories c
    INNER JOIN inserted i ON c.id = i.id;
END;
GO

CREATE TRIGGER tr_products_update
ON products
AFTER UPDATE
AS
BEGIN
    UPDATE products
    SET updated_at = SYSDATETIME()
    FROM products p
    INNER JOIN inserted i ON p.id = i.id;
END;
GO

CREATE TRIGGER tr_carts_update
ON carts
AFTER UPDATE
AS
BEGIN
    UPDATE carts
    SET updated_at = SYSDATETIME()
    FROM carts c
    INNER JOIN inserted i ON c.id = i.id;
END;
GO

CREATE TRIGGER tr_cart_items_update
ON cart_items
AFTER UPDATE
AS
BEGIN
    UPDATE cart_items
    SET updated_at = SYSDATETIME()
    FROM cart_items ci
    INNER JOIN inserted i ON ci.id = i.id;
END;
GO

CREATE TRIGGER tr_orders_update
ON orders
AFTER UPDATE
AS
BEGIN
    UPDATE orders
    SET updated_at = SYSDATETIME()
    FROM orders o
    INNER JOIN inserted i ON o.id = i.id;
END;
GO

CREATE TRIGGER tr_promotions_update
ON promotions
AFTER UPDATE
AS
BEGIN
    UPDATE promotions
    SET updated_at = SYSDATETIME()
    FROM promotions p
    INNER JOIN inserted i ON p.id = i.id;
END;
GO

CREATE TRIGGER tr_reviews_update
ON reviews
AFTER UPDATE
AS
BEGIN
    UPDATE reviews
    SET updated_at = SYSDATETIME()
    FROM reviews r
    INNER JOIN inserted i ON r.id = i.id;
END;
GO

CREATE TRIGGER tr_chat_rooms_update
ON chat_rooms
AFTER UPDATE
AS
BEGIN
    UPDATE chat_rooms
    SET updated_at = SYSDATETIME()
    FROM chat_rooms cr
    INNER JOIN inserted i ON cr.id = i.id;
END;
GO

CREATE TRIGGER tr_settings_update
ON settings
AFTER UPDATE
AS
BEGIN
    UPDATE settings
    SET updated_at = SYSDATETIME()
    FROM settings s
    INNER JOIN inserted i ON s.id = i.id;
END;
GO