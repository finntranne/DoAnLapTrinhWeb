/**
 * Checkout Page JavaScript
 * Xử lý form validation, address selection, và UI interactions
 */

document.addEventListener('DOMContentLoaded', function() {
    initCheckoutForm();
    initAddressCards();
    initPaymentMethodCards();
    animateOrderItems(); // Chạy animation sau khi DOM sẵn sàng
});

/**
 * Khởi tạo form checkout và validation
 */
function initCheckoutForm() {
    const checkoutForm = document.getElementById('checkoutForm');
    const submitBtn = checkoutForm ? checkoutForm.querySelector('button[type="submit"]') : null; // Lấy nút submit

    if (!checkoutForm || !submitBtn) return;

    checkoutForm.addEventListener('submit', function(e) {
        // --- Validation ---
        const addressSelected = document.querySelector('input[name="addressId"]:checked');
        if (!addressSelected) {
            e.preventDefault(); // Ngăn submit KHI CÓ LỖI
            showAlert('Vui lòng chọn địa chỉ giao hàng.', 'warning');
            scrollToElement(document.getElementById('addressListContainer'));
            // Kích hoạt lại nút nếu trước đó bị disable
            enableSubmitButton(submitBtn);
            return false;
        }

        const paymentSelected = document.querySelector('input[name="paymentMethod"]:checked');
        if (!paymentSelected) {
            e.preventDefault(); // Ngăn submit KHI CÓ LỖI
            showAlert('Vui lòng chọn phương thức thanh toán.', 'warning');
            scrollToElement(document.querySelector('.card-body:has(input[name="paymentMethod"])'));
            // Kích hoạt lại nút nếu trước đó bị disable
            enableSubmitButton(submitBtn);
            return false;
        }

        // --- Validation thành công ---
        // Chỉ disable nút submit để tránh double click
        disableSubmitButton(submitBtn);

        // KHÔNG gọi e.preventDefault() ở đây.
        // Để trình duyệt tự động submit form với đầy đủ dữ liệu.
        // Các input khác KHÔNG bị disable.
    });
}

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
