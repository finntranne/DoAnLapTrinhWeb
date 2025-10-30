/**
 * Checkout Page JavaScript
 * Xử lý form validation, address selection, và UI interactions
 */

function getSelectedItemIdsFromDOM() {
    const ids = [];
    // Lấy tất cả input hidden có name="selectedItemIds"
    document.querySelectorAll('#checkoutForm input[name="selectedItemIds"]').forEach(input => {
        // Đảm bảo giá trị được thêm vào mảng (dù giá trị đó là string)
        if (input.value) {
             ids.push(input.value);
        }
    });
    return ids;
}

document.addEventListener('DOMContentLoaded', function() {
    initCheckoutForm();
    initAddressCards();
    initPaymentMethodCards();
    animateOrderItems(); // Chạy animation sau khi DOM sẵn sàng
	initCouponLogic();
});

/**
 * Khởi tạo form checkout và validation
 */
/**
 * Khởi tạo form checkout và validation
 */
function initCheckoutForm() {
    const checkoutForm = document.getElementById('checkoutForm');
    const submitBtn = checkoutForm ? checkoutForm.querySelector('button[type="submit"]') : null;
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmPlaceOrderModal')); // ✅ KHỞI TẠO MODAL

    if (!checkoutForm || !submitBtn) return;

    checkoutForm.addEventListener('submit', function(e) {
        e.preventDefault(); // ❌ NGĂN CHẶN SUBMIT TRÌNH DUYỆT MẶC ĐỊNH

        // --- 1. Validation ---
        const addressSelected = document.querySelector('input[name="addressId"]:checked');
        if (!addressSelected) {
            showAlert('Vui lòng chọn địa chỉ giao hàng.', 'warning');
            scrollToElement(document.getElementById('addressListContainer'));
            enableSubmitButton(submitBtn);
            return false;
        }

        const paymentSelected = document.querySelector('input[name="paymentMethod"]:checked');
        if (!paymentSelected) {
            showAlert('Vui lòng chọn phương thức thanh toán.', 'warning');
            scrollToElement(document.querySelector('.card-body:has(input[name="paymentMethod"])'));
            enableSubmitButton(submitBtn);
            return false;
        }

        // --- 2. Validation thành công: Hiển thị Modal ---
        
        // Lấy tổng tiền để hiển thị trong modal
        const grandTotalText = document.querySelector('.price-highlight.fs-5.fw-bold')?.textContent || 'N/A';
        
        // Cập nhật nội dung Modal
        document.getElementById('modalGrandTotal').textContent = grandTotalText;
        
        // Hiển thị Modal
        confirmModal.show();
        
        // --- 3. Xử lý khi người dùng xác nhận trong Modal ---
        const confirmBtn = document.getElementById('confirmPlaceOrderBtn');
        
        // Gán sự kiện click cho nút xác nhận
        confirmBtn.onclick = function() {
            confirmModal.hide();
            
            // Chỉ disable nút submit khi xác nhận thành công
            disableSubmitButton(submitBtn); 

            // Loại bỏ tất cả các sự kiện submit trước đó để tránh lặp
            checkoutForm.removeEventListener('submit', arguments.callee);
            
            // ✅ GỬI FORM LÊN SERVER
            checkoutForm.submit();
        };

        // Quan trọng: Trả về false để đảm bảo ngăn chặn submit lần đầu
        return false;
    });
    
    // Đảm bảo nút submit được kích hoạt lại nếu người dùng hủy Modal
    const modalElement = document.getElementById('confirmPlaceOrderModal');
    if (modalElement) {
        modalElement.addEventListener('hide.bs.modal', function () {
            enableSubmitButton(submitBtn);
        });
    }
}
// ... (Phần còn lại của checkout.js giữ nguyên) ...
/**
 * Khởi tạo address cards với hover và selection effects
 */
function initAddressCards() {
    const addressCards = document.querySelectorAll('.address-card');
    
    addressCards.forEach(card => {
        // Hover effects
        card.addEventListener('mouseenter', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (!radio.checked) {
                this.style.backgroundColor = 'var(--primary-lighter)';
                this.style.borderColor = 'var(--primary-light)';
            }
        });
        
        card.addEventListener('mouseleave', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (!radio.checked) {
                this.style.backgroundColor = '';
                this.style.borderColor = '';
            }
        });
        
        // Click card to select
        card.addEventListener('click', function(e) {
            // Không trigger nếu click vào label hoặc input
            if (e.target.tagName === 'LABEL' || e.target.tagName === 'INPUT') {
                return;
            }
            const radio = this.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
                radio.dispatchEvent(new Event('change'));
            }
        });
    });
    
    // Radio change events
    const addressRadios = document.querySelectorAll('input[name="addressId"]');
    addressRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            updateAddressSelection(this);
        });
    });
    
    // Set initial selected address style
    const selectedAddress = document.querySelector('input[name="addressId"]:checked');
    if (selectedAddress) {
        updateAddressSelection(selectedAddress);
    }
}

/**
 * Cập nhật style cho address card được chọn
 */
function updateAddressSelection(selectedRadio) {
    // Reset all cards
    document.querySelectorAll('.address-card').forEach(card => {
        card.style.backgroundColor = '';
        card.style.borderColor = '';
        card.style.boxShadow = '';
    });
    
    // Highlight selected card
    if (selectedRadio && selectedRadio.checked) {
        const card = selectedRadio.closest('.address-card');
        if (card) {
            card.style.backgroundColor = 'var(--primary-lighter)';
            card.style.borderColor = 'var(--primary-color)';
            card.style.boxShadow = '0 0 0 3px rgba(115, 165, 128, 0.1)';
        }
    }
}

/**
 * Khởi tạo payment method cards
 */
function initPaymentMethodCards() {
    const paymentCards = document.querySelectorAll('.form-check:has(input[name="paymentMethod"])');
    
    paymentCards.forEach(card => {
        // Hover effects
        card.addEventListener('mouseenter', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (!radio.checked) {
                this.style.backgroundColor = '#f8f9fa';
                this.style.borderColor = 'var(--primary-light)';
            }
        });
        
        card.addEventListener('mouseleave', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (!radio.checked) {
                this.style.backgroundColor = '';
                this.style.borderColor = '';
            }
        });
    });
    
    // Radio change events
    const paymentRadios = document.querySelectorAll('input[name="paymentMethod"]');
    paymentRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            updatePaymentSelection(this);
        });
    });
    
    // Set initial selected payment style
    const selectedPayment = document.querySelector('input[name="paymentMethod"]:checked');
    if (selectedPayment) {
        updatePaymentSelection(selectedPayment);
    }
}

/**
 * Cập nhật style cho payment method được chọn
 */
function updatePaymentSelection(selectedRadio) {
    // Reset all cards
    document.querySelectorAll('.form-check:has(input[name="paymentMethod"])').forEach(card => {
        card.style.backgroundColor = '';
        card.style.borderColor = '';
        card.style.boxShadow = '';
    });
    
    // Highlight selected card
    if (selectedRadio && selectedRadio.checked) {
        const card = selectedRadio.closest('.form-check');
        if (card) {
            card.style.backgroundColor = 'var(--primary-lighter)';
            card.style.borderColor = 'var(--primary-color)';
            card.style.boxShadow = '0 0 0 3px rgba(115, 165, 128, 0.1)';
        }
    }
}

/**
 * Chỉ disable nút submit (ĐÃ SỬA)
 */
function disableSubmitButton(submitBtn) {
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i>Đang xử lý...';
        submitBtn.style.opacity = '0.7';
    }
}

/**
 * Kích hoạt lại nút submit (Hàm mới)
 */
function enableSubmitButton(submitBtn) {
     if (submitBtn) {
        submitBtn.disabled = false;
        // Đặt lại text gốc của nút submit
        submitBtn.innerHTML = '<i class="fa-solid fa-lock me-2"></i> Hoàn tất Đặt Hàng';
        submitBtn.style.opacity = '1';
    }
}

/**
 * Hiển thị alert message
 */
function showAlert(message, type = 'danger') {
    // Tạo alert element
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.setAttribute('role', 'alert');
    
    const icon = type === 'danger' || type === 'warning' 
        ? 'fa-exclamation-triangle' 
        : 'fa-check-circle';
    
    alertDiv.innerHTML = `
        <i class="fa-solid ${icon} me-2"></i>
        <span>${message}</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    // Thêm vào đầu container
    const container = document.querySelector('.container.my-5');
    if (container) {
        container.insertBefore(alertDiv, container.firstChild);
        
        // Scroll to alert
        alertDiv.scrollIntoView({ behavior: 'smooth', block: 'start' });
        
        // Auto dismiss sau 5 giây
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alertDiv);
            bsAlert.close();
        }, 5000);
    }
}

/**
 * Scroll mượt đến element
 */
function scrollToElement(element) {
    if (element) {
        element.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center' 
        });
        
        // Highlight effect
        element.style.transition = 'all 0.3s ease';
        element.style.backgroundColor = 'rgba(255, 193, 7, 0.1)';
        setTimeout(() => {
            element.style.backgroundColor = '';
        }, 2000);
    }
}

/**
 * Format currency
 */
function formatCurrency(amount) {
    if (isNaN(amount) || amount === null || amount === undefined) return '0đ';
    return new Intl.NumberFormat('vi-VN').format(Math.round(amount)) + 'đ';
}

/**
 * Animation cho order items khi load
 */
function animateOrderItems() {
    const orderItems = document.querySelectorAll('.order-items-summary > div');
    orderItems.forEach((item, index) => {
        item.style.opacity = '0';
        item.style.transform = 'translateY(10px)';
        setTimeout(() => {
            item.style.transition = 'all 0.4s ease';
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
        }, index * 50);
    });
}

/**
 * Logic MÃ GIẢM GIÁ
 */

// Hàm gửi request áp dụng mã giảm giá
async function applyCoupon() {
    const couponInput = document.getElementById('couponInput');
    const code = couponInput.value.trim().toUpperCase();
    const applyBtn = document.getElementById('applyCouponBtn');
    
    if (code === '') {
        showAlert('Vui lòng nhập mã giảm giá.', 'warning');
        return;
    }
	
	const itemIds = getSelectedItemIdsFromDOM();
	    if (itemIds.length === 0) {
	        showAlert('Không tìm thấy sản phẩm nào trong form.', 'danger');
	        return;
	    }

    const formData = new URLSearchParams();
    formData.append('couponCode', code);
	
	itemIds.forEach(id => {
	        formData.append('selectedItemIds', id);
	    });

    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    
    applyBtn.disabled = true;

    try {
        const response = await fetch('/apply-coupon', {
            method: 'POST',
            headers: headers,
            body: formData
        });

        const result = await response.json();

        if (response.ok) {
            // Cập nhật UI thành công
            updateSummaryUI(result.discountAmount, result.grandTotal, result.couponCode);
            showAlert(result.success, 'success');
			setTimeout(() => {
			                window.location.reload(); 
			            }, 500); // Chờ 0.5 giây để người dùng thấy thông báo
        } else {
            // Xử lý lỗi từ Controller
            showAlert(result.error || 'Mã giảm giá không hợp lệ.', 'danger');
            removeCouponUiOnly();
        }
    } catch (error) {
        console.error('AJAX Error:', error);
        showAlert('Lỗi kết nối khi áp dụng mã giảm giá.', 'danger');
    } finally {
        applyBtn.disabled = false;
    }
}

// Hàm gửi request xóa mã giảm giá
async function removeCoupon() {
    const removeBtn = document.getElementById('removeCouponBtn');
    
    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    
    removeBtn.disabled = true;

    try {
        // Gọi endpoint xóa mã giảm giá
        const response = await fetch('/remove-coupon', {
            method: 'POST',
            headers: headers
        });

        if (response.ok) {
            // Xóa UI và Tải lại trang để tổng tiền reset chính xác
            removeCouponUiOnly();
            showAlert('Đã xóa mã giảm giá thành công!', 'success');
            // Tải lại trang để đồng bộ tổng tiền
            window.location.reload(); 
        } else {
             const result = await response.json();
             showAlert(result.error || 'Lỗi khi xóa mã giảm giá.', 'danger');
        }
    } catch (error) {
        console.error('AJAX Error:', error);
        showAlert('Lỗi mạng khi xóa mã giảm giá.', 'danger');
    } finally {
        removeBtn.disabled = false;
    }
}

// Hàm cập nhật giao diện tổng tiền
function updateSummaryUI(discountAmount, grandTotal, couponCode) {
    // 1. Cập nhật Tổng cộng
    const totalElement = document.querySelector('.price-highlight.fs-5.fw-bold');
    if (totalElement) {
        totalElement.textContent = formatCurrency(parseFloat(grandTotal));
        totalElement.style.transition = 'all 0.3s ease';
        totalElement.style.color = 'var(--bs-red)'; // Hiệu ứng nhấp nháy
        setTimeout(() => totalElement.style.color = '', 500);
    }
    
    // 2. Cập nhật dòng Giảm giá
    const discountRow = document.querySelector('li[th\\:if*="discount"]');
    const discountElement = discountRow ? discountRow.querySelector('span.fw-semibold.text-success') : null;

    if (discountRow && discountElement) {
        discountElement.innerHTML = `-${formatCurrency(parseFloat(discountAmount))}`;
        discountRow.style.display = (parseFloat(discountAmount) > 0) ? 'flex' : 'none';
    }

    // 3. Cập nhật hộp thông báo coupon
    const couponMessage = document.getElementById('coupon-message');
    const couponInput = document.getElementById('couponInput');
    
    if (couponMessage) {
         couponMessage.className = 'alert alert-success py-2 mb-2';
         couponMessage.innerHTML = `<i class="fa-solid fa-check-circle me-1"></i> Đã áp dụng: <span>${couponCode}</span>`;
         couponMessage.style.display = 'block';
    }
    
    // 4. Cập nhật input và nút xóa
    if (couponInput) couponInput.value = couponCode;
    document.getElementById('applyCouponBtn').style.display = 'none';
    document.getElementById('removeCouponBtn').style.display = 'block';
}

// Hàm reset UI nếu có lỗi hoặc xóa thành công
function removeCouponUiOnly() {
    const couponInput = document.getElementById('couponInput');
    const couponMessage = document.getElementById('coupon-message');
    const discountRow = document.querySelector('li[th\\:if*="discount"]');
    
    if (couponMessage) couponMessage.style.display = 'none';
    if (couponInput) couponInput.value = '';
    if (discountRow) discountRow.style.display = 'none';
    
    document.getElementById('applyCouponBtn').style.display = 'block';
    document.getElementById('removeCouponBtn').style.display = 'none';
}

// Hàm khởi tạo logic Coupon
function initCouponLogic() {
    const applyBtn = document.getElementById('applyCouponBtn');
    const removeBtn = document.getElementById('removeCouponBtn');
    const couponMessage = document.getElementById('coupon-message');
    const couponInput = document.getElementById('couponInput');
    
    if (applyBtn) {
        applyBtn.addEventListener('click', applyCoupon);
    }
    if (removeBtn) {
        removeBtn.addEventListener('click', removeCoupon);
    }
    
    // Khởi tạo trạng thái nút và message khi tải trang
    if (couponMessage && couponInput && couponInput.value.trim() !== '') {
        // Mã đã tồn tại trong session khi trang tải
        document.getElementById('applyCouponBtn').style.display = 'none';
        document.getElementById('removeCouponBtn').style.display = 'block';
        couponMessage.style.display = 'block';
    } else {
        // Không có mã
        document.getElementById('applyCouponBtn').style.display = 'block';
        document.getElementById('removeCouponBtn').style.display = 'none';
        if (couponMessage) couponMessage.style.display = 'none';
    }
}
