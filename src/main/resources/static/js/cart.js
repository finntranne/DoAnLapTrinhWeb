// Lấy CSRF token từ meta tags
const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

/**
 * Hàm format tiền tệ theo định dạng Việt Nam
 */
function formatCurrency(price) {
    if (isNaN(price) || price === null || price === undefined) return '0đ';
    return new Intl.NumberFormat('vi-VN').format(Math.round(price)) + 'đ';
}

/**
 * Hàm toggle chọn tất cả sản phẩm
 */
function toggleSelectAll(selectAllCheckbox) {
    const itemCheckboxes = document.querySelectorAll('.cart-item-checkbox');
    itemCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    updateCartSummary();
}

// Trong file cart.js, thay thế toàn bộ hàm updateCartSummary() bằng code này

function updateCartSummary() {
    let newSubtotal = 0;
    let selectedCount = 0; // Số dòng sản phẩm (item types)
    let totalQuantity = 0; // *** THÊM: Tổng số lượng sản phẩm ***
    const itemCheckboxes = document.querySelectorAll('.cart-item-checkbox');
    let allChecked = true;

    itemCheckboxes.forEach(checkbox => {
        const cartItemRow = checkbox.closest('.cart-item');
        
        if (!checkbox.checked) {
            allChecked = false;
        }

        if (checkbox.checked) {
            selectedCount++;
            
            // Lấy số lượng của dòng hàng này
            const quantityInput = cartItemRow.querySelector('.quantity-input');
            const itemQuantity = parseInt(quantityInput.value) || 0;
            totalQuantity += itemQuantity; // *** CỘNG DỒN TỔNG SỐ LƯỢNG ***

			            let lineTotalString = cartItemRow.dataset.lineTotal ? String(cartItemRow.dataset.lineTotal).replace(/[^0-9.]/g, '') : '0';
			            
			            // 2. Chuyển sang số thực (parseFloat) để xử lý các số thập phân như 49500.00
			            const lineTotal = parseFloat(lineTotalString);
            
            if (!isNaN(lineTotal)) {
                newSubtotal += lineTotal;
            } else {
                 console.error("Lỗi: Không thể chuyển đổi LineTotal thành số:", cartItemRow.dataset.lineTotal);
            }
        }
    });

    // Cập nhật checkbox "Chọn tất cả"
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = allChecked && itemCheckboxes.length > 0;
    }

    // Cập nhật UI
    const subtotalElement = document.getElementById('cart-subtotal');
    const totalElement = document.getElementById('cart-total');
    const countElement = document.getElementById('selected-count'); // Nơi hiển thị số lượng

    // *** CẬP NHẬT: Dùng totalQuantity thay vì selectedCount ***
    if (countElement) {
        countElement.textContent = totalQuantity; 
    }
    
    // Cập nhật giá tiền
    if (subtotalElement) {
        subtotalElement.textContent = formatCurrency(newSubtotal);
    }
    if (totalElement) {
        totalElement.textContent = formatCurrency(newSubtotal); 
    }
    
    // Vô hiệu hóa nút thanh toán
    const submitButton = document.querySelector('#cartForm button[type="submit"]');
    if (submitButton) {
        submitButton.disabled = (totalQuantity === 0);
    }
}

/**
 * Cập nhật số lượng sản phẩm hiển thị ở header
 */
function updateCartCount(newCount) {
    const cartBadges = document.querySelectorAll('.cart-count, .cart-badge');
    cartBadges.forEach(badge => {
        badge.textContent = newCount;
    });
    
    const cartItemCountSpan = document.querySelector('.card-header span');
    if (cartItemCountSpan) {
        cartItemCountSpan.textContent = newCount;
    }
}

/**
 * Hàm xóa sản phẩm khỏi giỏ hàng
 */
function removeItem(cartItemId) {
    // Hiển thị modal xác nhận
    const modal = new bootstrap.Modal(document.getElementById('confirmRemoveModal'));
    modal.show();
    
    // Xử lý khi người dùng xác nhận xóa
    const confirmBtn = document.getElementById('confirmRemoveBtn');
    confirmBtn.onclick = async function() {
        modal.hide();
        
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        try {
            const response = await fetch(`/cart/remove/${cartItemId}`, {
                method: 'POST',
                headers: headers
            });

            if (response.ok) {
                // Xóa item khỏi DOM với animation
                const itemRow = document.getElementById('cart-item-' + cartItemId);
                if (itemRow) {
                    itemRow.style.transition = 'all 0.3s ease';
                    itemRow.style.opacity = '0';
                    itemRow.style.transform = 'translateX(-20px)';
                    
                    setTimeout(() => {
                        itemRow.remove();
                        
                        // Tính lại tổng tiền
                        updateCartSummary();
                        
                        // Kiểm tra nếu giỏ hàng rỗng
                        const remainingItems = document.querySelectorAll('.cart-item');
                        if (remainingItems.length === 0) {
                            // Reload trang để hiển thị trạng thái giỏ hàng trống
                            window.location.reload();
                        }
                    }, 300);
                }
            } else {
                alert('Lỗi khi xóa sản phẩm. Vui lòng thử lại.');
            }
        } catch (error) {
            console.error('Error removing item:', error);
            alert('Lỗi mạng khi xóa sản phẩm.');
        }
    };
}

// Trong file cart.js, thay thế toàn bộ hàm updateQuantity bằng đoạn code này
async function updateQuantity(cartItemId, change) {
    const quantityInput = document.getElementById('quantity-' + cartItemId);
    if (!quantityInput) return;
    
    const cartItemRow = document.getElementById('cart-item-' + cartItemId);
    if (!cartItemRow) return;

    let currentQty = parseInt(quantityInput.value);
    let newQty = currentQty + change;

    // *** LOGIC ĐÃ SỬA ***
    // 1. Buộc newQty >= 1
    if (newQty < 1) {
        newQty = 1;
    }
    
    // 2. Thoát nếu số lượng không thay đổi (Quan trọng!)
    if (newQty === currentQty) return;
    // *******************

    const formData = new URLSearchParams();
    formData.append('quantity', newQty);

    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    // Disable buttons trong khi đang xử lý
    const buttons = cartItemRow.querySelectorAll('.quantity-selector-cart button');
    buttons.forEach(btn => btn.disabled = true);

    try {
        const response = await fetch(`/cart/update/${cartItemId}`, {
            method: 'POST',
            headers: headers,
            body: formData
        });

        // Kiểm tra content-type của response
        const contentType = response.headers.get('content-type');
        let result;
        
        if (contentType && contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            console.log('Server response:', text);
            
            if (response.ok) {
                window.location.reload();
                return;
            } else {
                throw new Error('Server không trả về JSON');
            }
        }

        // Xử lý response JSON
        if (response.ok) {
            // Kiểm tra xem item có bị xóa không (khi quantity <= 0)
            if (result.removed === true) {
                // Item đã bị xóa, remove khỏi DOM
                cartItemRow.style.transition = 'all 0.3s ease';
                cartItemRow.style.opacity = '0';
                cartItemRow.style.transform = 'translateX(-20px)';
                
                setTimeout(() => {
                    cartItemRow.remove();
                    
                    // Cập nhật cart count từ server
                    if (result.newCartCount !== undefined) {
                        updateCartCount(result.newCartCount);
                    }
                    
                    // Tính lại tổng từ các item còn lại
                    updateCartSummary();
                    
                    // Kiểm tra nếu giỏ hàng rỗng
                    const remainingItems = document.querySelectorAll('.cart-item');
                    if (remainingItems.length === 0) {
                        window.location.reload();
                    }
                }, 300);
                
                return;
            }
            
            // Item không bị xóa, cập nhật bình thường
            quantityInput.value = result.newQuantity || newQty;
            
            // Cập nhật giá tiền của dòng
            const lineTotalSpan = cartItemRow.querySelector('.line-total');
            if (lineTotalSpan && result.newLineTotal !== undefined) {
                lineTotalSpan.textContent = formatCurrency(result.newLineTotal);
                // Cập nhật data-attribute để tính tổng đúng
                cartItemRow.dataset.lineTotal = result.newLineTotal;
            }
            
            // Cập nhật cart count từ server
            if (result.newCartCount !== undefined) {
                updateCartCount(result.newCartCount);
            }
            
            // Tính lại tổng tiền dựa trên checkbox
            updateCartSummary();
            
            // Hiệu ứng nhấp nháy nhẹ
            if (lineTotalSpan) {
                lineTotalSpan.style.transition = 'all 0.3s ease';
                lineTotalSpan.style.color = '#73A580';
                lineTotalSpan.style.transform = 'scale(1.1)';
                setTimeout(() => {
                    lineTotalSpan.style.color = '';
                    lineTotalSpan.style.transform = 'scale(1)';
                }, 300);
            }
            
        } else {
            console.error('Update failed:', result);
            const errorMsg = result?.error || result?.message || 'Lỗi không xác định';
            alert('Lỗi cập nhật số lượng: ' + errorMsg);
            quantityInput.value = currentQty;
        }
    } catch (error) {
        console.error('Error sending update request:', error);
        alert('Đã xảy ra lỗi khi cập nhật giỏ hàng: ' + error.message);
        quantityInput.value = currentQty;
    } finally {
        buttons.forEach(btn => btn.disabled = false);
    }
}

/**
 * Khởi tạo khi trang đã load xong
 */
document.addEventListener('DOMContentLoaded', function() {
    // Tính tổng tiền ban đầu dựa trên checkbox đã chọn
    updateCartSummary();
    
    // Xử lý submit form checkout
    const cartForm = document.getElementById('cartForm');
    if (cartForm) {
        cartForm.addEventListener('submit', function(e) {
            const selectedCheckboxes = document.querySelectorAll('.cart-item-checkbox:checked');
            
            if (selectedCheckboxes.length === 0) {
                e.preventDefault();
                alert('Vui lòng chọn ít nhất một sản phẩm để thanh toán.');
                return false;
            }
            
            // Form sẽ tự động gửi selectedItemIds qua name="selectedItemIds"
            // Thêm loading spinner
            const submitBtn = cartForm.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i>Đang xử lý...';
            }
        });
    }
    
    // Xử lý keyboard events cho quantity input
    document.querySelectorAll('.quantity-input').forEach(input => {
        input.addEventListener('keydown', function(e) {
            e.preventDefault();
        });
    });
    
    // Animation cho cart items khi load trang
    const cartItems = document.querySelectorAll('.cart-item');
    cartItems.forEach((item, index) => {
        item.style.opacity = '0';
        item.style.transform = 'translateY(20px)';
        setTimeout(() => {
            item.style.transition = 'all 0.4s ease';
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
        }, index * 100);
    });
});