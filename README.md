# 🧋 ALOTRA – HỆ THỐNG WEBSITE BÁN TRÀ SỮA

### Đồ án môn học: **Lập Trình Web – WEPR330479**

**Trường Đại học Sư Phạm Kỹ Thuật TP. Hồ Chí Minh**  
**Khoa Công Nghệ Thông Tin**  
**GVHD:** ThS. Nguyễn Hữu Trung  
**Nhóm 10 – Sinh viên thực hiện:**  
- Trần Hữu Thoại – 23110334  
- Nguyễn Trí Lâm – 23110250  
- Đoàn Quân Tuấn – 23110354  

---

## 📖 Giới thiệu dự án

**AloTra** là một **nền tảng thương mại điện tử** được xây dựng nhằm **quản lý và kinh doanh trà sữa trực tuyến**.  
Dự án mô phỏng một **hệ sinh thái đa vai trò** bao gồm: **Khách hàng, Người bán, Shipper và Quản trị viên**, với quy trình khép kín từ đặt hàng – thanh toán – giao hàng – phản hồi.

Mục tiêu của hệ thống là **đưa các hoạt động kinh doanh trà sữa lên môi trường số**, giúp người bán tối ưu quy trình vận hành và người dùng có trải nghiệm mua hàng hiện đại, thuận tiện.

---

## 🎯 Mục tiêu & Phạm vi

### 🎯 **Mục tiêu chính**
- Xây dựng website bán hàng đa vai trò: *Khách hàng – Người bán – Shipper – Quản trị viên*  
- Quản lý toàn bộ quy trình: đăng ký, đăng nhập, đặt hàng, thanh toán, giao hàng, quản trị hệ thống.  
- Cung cấp giao diện thân thiện, phản hồi nhanh, bảo mật cao.  
- Tích hợp thanh toán VNPay, VietQR và Chat thời gian thực bằng WebSocket.  

### 🧩 **Phạm vi kỹ thuật**
| Thành phần | Công nghệ sử dụng |
|-------------|-------------------|
| **Backend** | Java 17, Spring Boot 3.x, Spring Security, JPA/Hibernate |
| **Frontend** | HTML5, CSS3, JavaScript, Bootstrap 5, Thymeleaf |
| **CSDL** | Microsoft SQL Server |
| **Tích hợp** | VNPay, VietQR, Cloudinary, WebSocket (Chat Realtime) |
| **Triển khai** | Máy chủ nội bộ hoặc Cloud (tùy mở rộng) |

---

## 🧠 Kiến trúc hệ thống

Hệ thống được xây dựng theo **mô hình 3 tầng (Controller – Service – Repository)**, tuân thủ kiến trúc RESTful API.  
Các vai trò hoạt động tách biệt nhưng liên kết qua cơ sở dữ liệu trung tâm.

### 🏗️ **Cấu trúc chính**
```
/src
 ├── main/java/com/alotra
 │    ├── controller/      # Các API và Controller web
 │    ├── service/         # Business logic
 │    ├── repository/      # DAO, JPA interfaces
 │    ├── model/           # Entity và DTO
 │    ├── security/        # JWT, Spring Security config
 │    └── config/          # Cấu hình hệ thống
 ├── main/resources/
 │    ├── templates/       # Thymeleaf view
 │    ├── static/          # CSS, JS, Image
 │    └── application.yml  # Cấu hình môi trường
```

---

## 👥 Vai trò người dùng

| Vai trò | Chức năng chính |
|----------|------------------|
| **Khách hàng (Customer)** | Đăng ký/Đăng nhập, đặt hàng, thanh toán (VNPay, VietQR, COD), theo dõi đơn, chat với shop, đánh giá sản phẩm |
| **Người bán (Vendor)** | Quản lý sản phẩm, khuyến mãi, đơn hàng, doanh thu, phê duyệt topping và khuyến mãi |
| **Quản trị viên (Admin)** | Quản lý user, shop, sản phẩm, thống kê doanh thu, khuyến mãi, vận chuyển |
| **Shipper** | Nhận đơn giao hàng, cập nhật trạng thái giao, thống kê hiệu suất cá nhân |

---

## 💬 Tính năng nổi bật

✅ **Đa vai trò – Đa giao diện**: mỗi người dùng có dashboard riêng  
✅ **Quản lý sản phẩm, topping, khuyến mãi, đơn hàng**  
✅ **Thanh toán điện tử (VNPay, VietQR)**  
✅ **Chat hỗ trợ realtime qua WebSocket**  
✅ **Thống kê doanh thu, hiệu suất**  
✅ **Xác thực & phân quyền bằng JWT + Spring Security**  
✅ **Giao diện responsive, dễ sử dụng trên mọi thiết bị**

---

## 🧾 Cấu trúc dữ liệu chính

CSDL bao gồm hơn **30 bảng**, được thiết kế đảm bảo **chuẩn hóa, toàn vẹn, ràng buộc khóa ngoại đầy đủ**.  
Một số bảng quan trọng:

| Bảng | Mục đích |
|------|-----------|
| `Users`, `Roles`, `UserRoles` | Quản lý người dùng & phân quyền |
| `Products`, `Categories`, `ProductVariants`, `Toppings` | Quản lý sản phẩm và tùy chọn |
| `Orders`, `OrderDetails`, `OrderHistory` | Xử lý và lưu lịch sử đơn hàng |
| `Promotions`, `PromotionProducts` | Quản lý khuyến mãi |
| `ChatMessages`, `Notifications` | Chat realtime & thông báo hệ thống |

---

## 🧪 Cài đặt & chạy thử

### ⚙️ **1. Yêu cầu môi trường**
- Java JDK 17 hoặc 22  
- SQL Server 2019+  
- Maven 3.8+  
- IDE: IntelliJ IDEA / Eclipse  
- Cổng mặc định: `8080`

### 🚀 **2. Hướng dẫn chạy**
```bash
# Clone repository
git clone https://github.com/yourusername/alotra.git
cd alotra

# Cấu hình database trong application.yml
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=AloTra
spring.datasource.username=sa
spring.datasource.password=your_password

# Build và chạy server
mvn clean install
mvn spring-boot:run
```

Truy cập: [http://localhost:8080](http://localhost:8080)

---

## 🧭 Kiểm thử & kết quả

Các chức năng đã được **kiểm thử đầy đủ**:
- ✅ Đăng ký/Đăng nhập (OTP Email)  
- ✅ Mua hàng & thanh toán VNPay  
- ✅ Quản lý sản phẩm & đơn hàng  
- ✅ Giao hàng – Shipper cập nhật trạng thái realtime  
- ✅ Chat realtime giữa khách hàng & shop  
- ✅ Báo cáo doanh thu tổng quát  

Tốc độ phản hồi trung bình **< 3 giây / yêu cầu**.

---

## 📊 Kết quả đạt được

- Xây dựng hoàn chỉnh hệ thống thương mại điện tử đa vai trò.  
- Thiết kế CSDL logic, tối ưu và mở rộng dễ dàng.  
- Tích hợp thành công thanh toán trực tuyến và chat realtime.  
- Hệ thống vận hành ổn định, đáp ứng tiêu chuẩn UI/UX hiện đại.

---

## ⚡ Hướng phát triển

- Tích hợp **AI gợi ý sản phẩm** và **đề xuất khuyến mãi cá nhân hóa**.  
- Phát triển **ứng dụng di động** đồng bộ với hệ thống web.  
- Mở rộng tích hợp bản đồ GPS cho shipper.  
- Hỗ trợ **đa ngôn ngữ & đa phương thức thanh toán quốc tế**.

---

## 🧑‍💻 Thông tin nhóm

| Họ và tên | MSSV | Vai trò |
|------------|-------|----------|
| **Trần Hữu Thoại** | 23110334 | Guest, Customer |
| **Nguyễn Trí Lâm** | 23110250 | Vendor, Shipper |
| **Đoàn Quân Tuấn** | 23110354 | Admin |

---

## 📚 Tài liệu tham khảo
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)  
- [VNPay Developer API](https://sandbox.vnpayment.vn/apis/docs)  
- [Bootstrap 5 Docs](https://getbootstrap.com/)  
- [Cloudinary API](https://cloudinary.com/documentation)

---

## 🏁 Giấy phép
Dự án này được phát triển cho mục đích học tập tại **ĐH Sư Phạm Kỹ Thuật TP.HCM** – Không sử dụng cho mục đích thương mại.
