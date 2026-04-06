# Hệ Thống Thanh Toán - Payment System

## Tổng Quan

Đây là một hệ thống thanh toán hoàn chỉnh áp dụng ba design patterns chính:
- **Strategy Pattern**: Định nghĩa các chiến lược thanh toán khác nhau
- **Adapter Pattern**: Chuyển đổi interface của các SDK thanh toán (VNPay, VietQR, MOMO)
- **Factory Pattern**: Tạo các instances của payment strategies

## Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────┐
│           PaymentController (REST API)                  │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│           PaymentService (Business Logic)               │
│    createPayment(), processPayment(), refundPayment()  │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│         PaymentContext (Strategy Context)               │
│    setStrategy(), executePayment(), executeRefund()    │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│    ┌────────────────────────────────────────────────┐   │
│    │   PaymentStrategyFactory                       │   │
│    │   - createStrategy(PaymentMethod)              │   │
│    │   - isSupported(PaymentMethod)                 │   │
│    └────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────┘
                       │
    ┌──────────────────┼──────────────────┬────────────────┐
    │                  │                  │                │
    ▼                  ▼                  ▼                ▼
┌─────────┐    ┌─────────────────┐  ┌──────────┐  ┌──────────────┐
│CODStrategy  │VNPayAdapter     │  │MomoAdapter  │VietQRAdapter│
└─────────┘    └─────────────────┘  └──────────┘  └──────────────┘
                    │                    │              │
                    ▼                    ▼              ▼
              ┌──────────────┐    ┌──────────┐   ┌───────────┐
              │ VNPaySDK     │    │ MomoSDK  │   │VietQRSDK  │
              └──────────────┘    └──────────┘   └───────────┘
```

## Cấu Trúc File

```
src/main/java/com/alotra/
├── dto/
│   └── order/
│       └── PaymentResult.java              # DTO cho kết quả thanh toán
├── entity/
│   └── order/
│       ├── Order.java                      # Entity đơn hàng
│       └── Payment.java                    # Entity thanh toán (cập nhật)
├── enums/
│   ├── PaymentMethod.java                  # COD, VNPAY, MOMO, BANK_TRANSFER, ZALOPAY
│   └── PaymentStatus.java                  # UNPAID, PAID, REFUNDED
├── repository/
│   └── order/
│       └── PaymentRepository.java          # Repository cho Payment
├── service/
│   ├── order/
│   │   ├── PaymentService.java             # Service chính
│   │   ├── PaymentSystemExample.java       # Ví dụ sử dụng
│   │   └── payment/
│   │       ├── context/
│   │       │   └── PaymentContext.java     # Strategy Context
│   │       ├── factory/
│   │       │   └── PaymentStrategyFactory.java
│   │       ├── strategy/
│   │       │   ├── PaymentStrategy.java    # Interface
│   │       │   ├── CODStrategy.java
│   │       │   ├── VNPayAdapter.java
│   │       │   ├── MomoAdapter.java
│   │       │   └── VietQRAdapter.java
│   │       └── sdk/
│   │           ├── VNPaySDK.java           # SDK Simulators
│   │           ├── MomoSDK.java
│   │           └── VietQRSDK.java
└── controller/
    ├── PaymentController.java              # REST API endpoints
    └── PaymentCallbackController.java      # Callback từ payment gateway
```

## Các Design Patterns

### 1. Strategy Pattern (Chiến Lược)

**Vấn đề:** Có nhiều phương thức thanh toán khác nhau (COD, VNPay, MOMO, etc.), mỗi phương thức có logic xử lý khác nhau.

**Giải pháp:** Tạo interface `PaymentStrategy` và các implementation cụ thể.

```java
// Interface Strategy
public interface PaymentStrategy {
    PaymentResult pay(Order order);
    boolean refund(String transactionCode);
    PaymentMethod getMethod();
}

// Implementation cụ thể
public class CODStrategy implements PaymentStrategy {
    @Override
    public PaymentResult pay(Order order) {
        // Logic thanh toán COD
    }
}

public class VNPayAdapter implements PaymentStrategy {
    // Sử dụng VNPaySDK
}
```

**Ưu điểm:**
- Dễ thêm phương thức thanh toán mới
- Tuân theo Open/Closed Principle
- Thay đổi strategy tại runtime

### 2. Adapter Pattern (Chuyển Đổi)

**Vấn đề:** Các SDK thanh toán (VNPaySDK, MomoSDK, VietQRSDK) có interface khác nhau không tương thích với `PaymentStrategy`.

**Giải pháp:** Tạo adapter classes chuyển đổi interface của SDK thành `PaymentStrategy`.

```java
public class VNPayAdapter implements PaymentStrategy {
    private final VNPaySDK vnPaySDK;
    
    @Override
    public PaymentResult pay(Order order) {
        // Chuyển đổi: gọi vnPaySDK.createPaymentUrl()
        // Trả về PaymentResult
    }
}

public class VietQRAdapter implements PaymentStrategy {
    private final VietQRSDK vietQRSDK;
    
    @Override
    public PaymentResult pay(Order order) {
        // Chuyển đổi: gọi vietQRSDK.requestPayment()
        // Trả về PaymentResult
    }
}
```

**Lợi ích:**
- Tách biệt SDK bên ngoài khỏi code chính
- Dễ bảo trì và test
- Có thể thay thế SDK dễ dàng

### 3. Factory Pattern (Nhà Máy)

**Vấn đề:** Cần tạo các instance của `PaymentStrategy` dựa trên `PaymentMethod`.

**Giải pháp:** Sử dụng `PaymentStrategyFactory` tập trung logic tạo instances.

```java
public class PaymentStrategyFactory {
    public static PaymentStrategy createStrategy(PaymentMethod method) {
        return switch (method) {
            case COD -> new CODStrategy();
            case VNPAY -> new VNPayAdapter();
            case MOMO -> new MomoAdapter();
            case BANK_TRANSFER -> new VietQRAdapter();
            default -> throw new IllegalArgumentException(...);
        };
    }
}
```

**Lợi ích:**
- Tập trung logic tạo object
- Dễ mở rộng khi thêm phương thức mới
- Thay đổi logic tạo ở một nơi

### 4. Context Pattern (Kiểu Bối Cảnh)

**Vấn đề:** Cần quản lý `PaymentStrategy` và thực hiện các phép toán thanh toán.

**Giải pháp:** Sử dụng `PaymentContext` để giữ và sử dụng strategy.

```java
public class PaymentContext {
    private PaymentStrategy strategy;
    
    public PaymentContext(PaymentMethod method) {
        this.strategy = PaymentStrategyFactory.createStrategy(method);
    }
    
    public PaymentResult executePayment(Order order) {
        return strategy.pay(order);
    }
    
    // Thay đổi strategy tại runtime
    public void setStrategy(PaymentMethod method) {
        this.strategy = PaymentStrategyFactory.createStrategy(method);
    }
}
```

## Cách Sử Dụng

### 1. API Endpoints

#### a. Tạo Payment Record

```bash
POST /api/payments/create
Content-Type: application/json

{
  "orderId": 1,
  "paymentMethod": "VNPAY"
}
```

Response:
```json
{
  "success": true,
  "message": "Tạo payment thành công",
  "paymentId": 1,
  "paymentMethod": "VNPAY"
}
```

#### b. Xử Lý Thanh Toán

```bash
POST /api/payments/process/1
```

Response:
```json
{
  "success": true,
  "transactionCode": "VNPAY-1-1234567890",
  "paymentUrl": "https://sandbox.vnpayment.vn/pay?order=1&amount=...",
  "message": "Vui lòng truy cập URL để thanh toán"
}
```

#### c. Lấy Thông Tin Payment

```bash
GET /api/payments/1
```

Response:
```json
{
  "paymentId": 1,
  "orderId": 1,
  "method": "VNPAY",
  "status": "PAID",
  "transactionCode": "VNPAY-1-1234567890",
  "paidAt": "2024-01-15T10:30:00",
  "paymentUrl": "..."
}
```

#### d. Hoàn Tiền

```bash
POST /api/payments/refund/1
```

#### e. Thay Đổi Phương Thức Thanh Toán

```bash
PATCH /api/payments/1/method
Content-Type: application/json

{
  "paymentMethod": "MOMO"
}
```

#### f. Lấy Danh Sách Phương Thức Hỗ Trợ

```bash
GET /api/payments/supported-methods
```

Response:
```json
{
  "supportedMethods": ["COD", "VNPAY", "MOMO", "BANK_TRANSFER", "ZALOPAY"]
}
```

### 2. Sử Dụng PaymentService (Backend)

```java
@Service
public class OrderProcessingService {
    
    @Autowired
    private PaymentService paymentService;
    
    public void processOrder(Order order) {
        // Tạo payment
        Payment payment = paymentService.createPayment(
            order, 
            PaymentMethod.VNPAY
        );
        
        // Thực hiện thanh toán
        PaymentResult result = paymentService.processPayment(order.getOrderID());
        
        if (result.isSuccess()) {
            // Redirect tới payment URL hoặc hiển thị QR code
            String paymentUrl = result.getPaymentUrl();
            String qrCode = result.getQrCode();
        }
    }
}
```

### 3. Sử Dụng Payment Entity

```java
Payment payment = new Payment();
payment.setMethod(PaymentMethod.COD);
payment.setOrder(order);

// Thực hiện thanh toán
PaymentResult result = payment.processPayment();

// Hoàn tiền
boolean refundSuccess = payment.refundPayment();
```

## Thêm Phương Thức Thanh Toán Mới

Để thêm phương thức thanh toán mới, làm theo các bước:

### 1. Tạo SDK Wrapper (nếu cần)

```java
public class NewPaymentSDK {
    public String createPaymentUrl(String orderId, long amount) {
        // Gọi external API
    }
    
    public boolean verifyReturn(Map<String, String> params) {
        // Verify callback
    }
}
```

### 2. Tạo Adapter

```java
public class NewPaymentAdapter implements PaymentStrategy {
    private final NewPaymentSDK sdk;
    
    @Override
    public PaymentResult pay(Order order) {
        // Logic thanh toán
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setTransactionCode(...);
        return result;
    }
    
    @Override
    public boolean refund(String transactionCode) {
        // Logic hoàn tiền
    }
    
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.SOME_NEW_METHOD;
    }
}
```

### 3. Cập Nhật Factory

```java
public static PaymentStrategy createStrategy(PaymentMethod method) {
    return switch (method) {
        // ...
        case NEW_METHOD -> new NewPaymentAdapter();
        // ...
    };
}
```

### 4. Cập Nhật Enum (nếu cần)

```java
public enum PaymentMethod {
    COD,
    VNPAY,
    MOMO,
    BANK_TRANSFER,
    ZALOPAY,
    NEW_METHOD  // Thêm mới
}
```

## Class Diagram

```
┌──────────────────────────────┐
│      PaymentStrategy         │
│     (<<interface>>)          │
├──────────────────────────────┤
│ + pay(order): PaymentResult  │
│ + refund(txCode): boolean    │
│ + getMethod(): PaymentMethod │
└──────────────────────────────┘
         △         △         △
         │         │         │
    ┌────┴────┐ ┌──┴──┐ ┌───┴────────┐
    │         │ │     │ │            │
┌───┴──────┐ │ │     │ └───────────────┐
│CODStrategy│ │ │     │                 │
└────────────┘ │ │     │                 │
     ┌────────┴─┴─┐  ┌─┴──────────┐  ┌──┴────────────┐
     │ VNPayAdapter│  │ MomoAdapter│  │VietQRAdapter │
     │             │  │            │  │              │
     │+ pay()      │  │+ pay()     │  │+ pay()       │
     │+ refund()   │  │+ refund()  │  │+ refund()    │
     └─────────────┘  └────────────┘  └──────────────┘

┌──────────────────────────────┐
│   PaymentStrategyFactory     │
├──────────────────────────────┤
│ + createStrategy(method)     │
│ + isSupported(method)        │
└──────────────────────────────┘
         │ uses
         ▼
    ┌────────────────┐
    │ PaymentContext │
    ├────────────────┤
    │ - strategy     │◀──┐
    ├────────────────┤   │
    │ + setStrategy()│   │ uses
    │ + execute...() │   │
    └────────────────┘───┘
         │ uses
         ▼
┌────────────────────┐
│ PaymentService     │
├────────────────────┤
│ + createPayment()  │
│ + processPayment() │
│ + refundPayment()  │
└────────────────────┘
         │ uses
         ▼
┌────────────────────┐
│ PaymentRepository  │
└────────────────────┘
```

## Testing

```java
@Test
public void testPaymentStrategy() {
    // Test Strategy Pattern
    PaymentStrategy codStrategy = new CODStrategy();
    PaymentResult result = codStrategy.pay(mockOrder);
    
    assertTrue(result.isSuccess());
    assertEquals(PaymentMethod.COD, codStrategy.getMethod());
}

@Test
public void testPaymentContext() {
    // Test Context Pattern
    PaymentContext context = new PaymentContext(PaymentMethod.COD);
    
    // Thay đổi strategy tại runtime
    context.setStrategy(PaymentMethod.VNPAY);
    assertEquals(PaymentMethod.VNPAY, context.getCurrentPaymentMethod());
}

@Test
public void testFactoryPattern() {
    // Test Factory Pattern
    PaymentStrategy strategy = PaymentStrategyFactory.createStrategy(PaymentMethod.MOMO);
    assertInstanceOf(MomoAdapter.class, strategy);
    
    // Test unsupported method
    assertThrows(IllegalArgumentException.class, () -> {
        PaymentStrategyFactory.createStrategy(PaymentMethod.INVALID);
    });
}
```

## Lợi Ích của Thiết Kế

1. **Dễ mở rộng**: Thêm phương thức thanh toán mới mà không sửa code cũ
2. **Dễ bảo trì**: Logic được tổ chức trong các classes riêng lẻ
3. **Dễ test**: Mỗi strategy có thể test độc lập
4. **Flexible**: Thay đổi strategy tại runtime
5. **Reusable**: Các components có thể tái sử dụng
6. **SOLID Principles**: Tuân theo các nguyên tắc S, O, L, D

## Kết Luận

Hệ thống thanh toán này là một ví dụ hoàn chỉnh về cách áp dụng các design patterns vào thực tế. Nó cung cấp:

- **Linh hoạt**: Dễ thêm/xoá phương thức thanh toán
- **Bảo trì**: Code sạch, dễ hiểu
- **Mở rộng**: Không cần sửa code cũ khi thêm tính năng mới
- **Testing**: Dễ viết unit tests

---

## VNPay Configuration & Callback Handling (Cập nhật v2.0)

### Cấu hình VNPay

VNPay configuration được quản lý tập trung trong `VnPayConfig` class sử dụng Spring `@ConfigurationProperties`:

```java
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VnPayConfig {
    private String tmnCode;          // Mã merchant
    private String hashSecret;       // Secret key
    private String apiUrl;           // URL gateway
    private String returnUrl;        // URL callback
    private String locale;           // Ngôn ngữ (vn/en)
}
```

### Cấu hình application.properties

```properties
# VNPay Official Demo Credentials
vnpay.tmn-code=UE7VL5EI
vnpay.hash-secret=VZ4WXT2UEBZRYKHP2TI9CW5V4HNSNGVO
vnpay.api-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/vnpay-return
vnpay.locale=vn
```

### VNPay Flow

```
Client Browser
      │
      ├─→ POST /api/payments/process/{orderId}
      │   └─→ PaymentService.processPayment()
      │   
      ├─→ Nhận PaymentResult với paymentUrl
      │
      ├─→ Redirect tới https://sandbox.vnpayment.vn/pay?...
      │   (VNPay Gateway)
      │
      ├─→ Khách hàng nhập thông tin card
      │
      ├─→ VNPay xử lý thanh toán
      │
      ├─→ Redirect về: GET /vnpay-return?vnp_ResponseCode=00&vnp_TransactionNo=...
      │   (Callback Handler)
      │
      └─→ PaymentService.confirmPayment()
          └─→ Payment status = PAID
```

### VNPay Callback Handler

Endpoint `GET /vnpay-return` xử lý callback từ VNPay:

#### Các bước xử lý:
1. **Nhận callback parameters** từ VNPay
2. **Xác minh signature** (HMAC-SHA512) để đảm bảo yêu cầu hợp lệ
3. **Kiểm tra response code**:
   - `00` = Thanh toán thành công
   - Khác = Thanh toán thất bại
4. **Cập nhật Payment entity** với transaction info
5. **Trả về response** cho khách hàng

#### Response thành công (Response Code = 00):

```json
{
  "success": true,
  "message": "Thanh toán thành công",
  "orderId": 1,
  "transactionNo": "12345678",
  "amount": 100000
}
```

#### Response thất bại:

```json
{
  "success": false,
  "message": "Thanh toán thất bại: Lỗi không xác định (99)",
  "orderId": 1,
  "responseCode": "99"
}
```

### VNPay Response Codes

| Code | Meaning | Tin nhắn |
|------|---------|---------|
| 00 | Success | Giao dịch thành công |
| 01 | IP not allowed | Yêu cầu từ IP không được phép |
| 02 | Merchant locked | Merchant bị khóa |
| 07 | Insufficient funds | Không đủ tiền |
| 11 | User cancelled | Người dùng hủy giao dịch |

### Cách kiểm tra locally

#### Bước 1: Tạo Payment Record

```bash
curl -X POST http://localhost:8080/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethod": "VNPAY"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Tạo payment thành công",
  "paymentId": 1,
  "paymentMethod": "VNPAY"
}
```

#### Bước 2: Xử lý thanh toán

```bash
curl -X POST http://localhost:8080/api/payments/process/1
```

Response:
```json
{
  "success": true,
  "transactionCode": "VNPAY-1-1704067200000",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "message": "Vui lòng truy cập URL để thanh toán VNPay"
}
```

#### Bước 3: Simulate VNPay Callback

Trên VNPay Sandbox, hoặc simulate bằng cURL:

```bash
curl -X GET "http://localhost:8080/vnpay-return?vnp_ResponseCode=00&vnp_TxnRef=1&vnp_TransactionNo=12345&vnp_Amount=10000000&vnp_PayDate=20240101120000&vnp_SecureHash=HASH..."
```

### Integration với Frontend

#### Ví dụ React

```javascript
async function processVNPayment(orderId) {
  // Bước 1: Tạo payment
  const createResponse = await fetch('/api/payments/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      orderId: orderId,
      paymentMethod: 'VNPAY'
    })
  });
  
  // Bước 2: Xử lý thanh toán
  const processResponse = await fetch(`/api/payments/process/${orderId}`, {
    method: 'POST'
  });
  const result = await processResponse.json();
  
  // Bước 3: Redirect tới VNPay
  if (result.success && result.paymentUrl) {
    window.location.href = result.paymentUrl;
  }
}
```

VNPay sẽ tự động redirect khách hàng về `/vnpay-return` sau khi thanh toán hoàn tất.

### Hash Verification (Security)

VNPay sử dụng **HMAC-SHA512** để xác minh callback:

```java
// VnPayConfig tự động verify signature
boolean isValid = vnPayConfig.validateVnPayHash(vnpParams, vnpSecureHash);
```

**Quy trình:**
1. VNPay sắp xếp tất cả parameters theo alphabet
2. URL-encode giá trị mỗi parameter
3. Kết nối thành: `key1=value1&key2=value2&...`
4. HMAC-SHA512 hash với `hashSecret`
5. Gửi hash trong `vnp_SecureHash`

**Xác minh:**
- Tính toán hash lại với cùng quy trình
- So sánh với `vnp_SecureHash` nhận được
- Nếu khớp = callback hợp lệ từ VNPay

### Troubleshooting

#### "Invalid checksum" error

- Kiểm tra `vnpay.hash-secret` đúng
- Verify signature được tính toán đúng
- Không URL-encode params trước khi hash

#### "Merchant locked" error (Code: 02)

- Tài khoản VNPay sandbox bị khóa
- Liên hệ VNPay support hoặc dùng tài khoản khác

#### Callback không nhận được

- Kiểm tra `vnpay.return-url` chính xác
- Công khai được ip từ VNPay servers
- Kiểm tra firewall/routing rules

### Production Deployment

Khi deploy lên production:

1. **Thay đổi endpoint:**

```properties
vnpay.api-url=https://api.vnpayment.vn/paymentv2/vpcpay.html
```

2. **Cấu hình credentials thực:**

```properties
vnpay.tmn-code=YOUR_REAL_MERCHANT_CODE
vnpay.hash-secret=YOUR_REAL_HASH_SECRET
vnpay.return-url=https://yourdomain.com/vnpay-return
```

3. **Sử dụng environment variables:**

```bash
export VNPAY_TMN_CODE=YOUR_CODE
export VNPAY_HASH_SECRET=YOUR_SECRET
export VNPAY_RETURN_URL=https://yourdomain.com/vnpay-return
```

4. **Bảo vệ hash secret:**
- Không commit vào Git
- Sử dụng environment variables hoặc secrets manager
- Rotate định kỳ
