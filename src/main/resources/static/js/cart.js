// Thêm 2 dòng này lên đầu file nếu chưa có
const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");


// Hàm format tiền tệ
function formatCurrency(price) {
    if (isNaN(price)) return 'N/A';
    // Đảm bảo trả về chuỗi không có ký tự đặc biệt (chỉ số và 'đ')
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price).replace(/\s/g, '');
}

/**
 * === HÀM TÍNH LẠI TỔNG TIỀN (CLIENT-SIDE) ===
 */
function updateCartSummary() {
    let newSubtotal = 0;
    const itemCheckboxes = document.querySelectorAll('.cart-item-checkbox');
    let allChecked = true;

    itemCheckboxes.forEach(checkbox => {
        const cartItemRow = checkbox.closest('.cart-item');
        if (checkbox.checked) {
            // Lấy tổng tiền của hàng từ data-attribute
            const lineTotal = parseFloat(cartItemRow.dataset.lineTotal || 0);
            newSubtotal += lineTotal;
        } else {
            allChecked = false; // Nếu có 1 cái chưa check -> bỏ check "Chọn tất cả"
        }
    });

    // Cập nhật text ở phần Tóm tắt
    const subtotalSpan = document.getElementById('cart-subtotal');
    const totalSpan = document.getElementById('cart-total');

    if (subtotalSpan) {
        subtotalSpan.textContent = formatCurrency(newSubtotal);
    }
    if (totalSpan) {
        totalSpan.textContent = formatCurrency(newSubtotal); // Giả sử phí ship = 0
    }
    
    // Cập nhật checkbox "Chọn tất cả"
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = allChecked && itemCheckboxes.length > 0;
    }
}

/**
 * === HÀM CHỌN/BỎ CHỌN TẤT CẢ ===
 */
function toggleSelectAll(selectAllCheckbox) {
    const itemCheckboxes = document.querySelectorAll('.cart-item-checkbox');
    itemCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    // Tính lại tổng tiền sau khi đổi
    updateCartSummary();
}

/**
 * === HÀM XÓA ITEM (AJAX) ===
 */
async function removeItem(cartItemId) {
    if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
        return;
    }

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
            // Xóa hàng đó khỏi DOM
            const itemRow = document.getElementById('quantity-' + cartItemId).closest('.cart-item');
            if (itemRow) {
                itemRow.remove();
            }
            // Tính lại tổng tiền
            updateCartSummary();
            
            // Kiểm tra nếu giỏ hàng rỗng
            const remainingItems = document.querySelectorAll('.cart-item');
             if (remainingItems.length === 0) {
                 window.location.reload();
             }

        } else {
            alert('Lỗi khi xóa sản phẩm. Vui lòng thử lại.');
        }
    } catch (error) {
        console.error('Error removing item:', error);
        alert('Lỗi mạng khi xóa sản phẩm.');
    }
}


/**
 * === HÀM CẬP NHẬT SỐ LƯỢNG (ĐÃ SỬA ĐỔI) ===
 */
async function updateQuantity(cartItemId, change) {
    const quantityInput = document.getElementById('quantity-' + cartItemId);
    if (!quantityInput) return;
    
    const cartItemRow = quantityInput.closest('.cart-item'); // Lấy hàng item

    let currentQty = parseInt(quantityInput.value);
    let newQty = currentQty + change;

    if (newQty < 1) newQty = 1;
    if (newQty === currentQty && change !== 0) return;

    const formData = new URLSearchParams();
    formData.append('quantity', newQty);

    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    try {
        const response = await fetch(`/cart/update/${cartItemId}`, {
            method: 'POST',
            headers: headers,
            body: formData
        });

        const result = await response.json();

        if (response.ok) {
            quantityInput.value = result.newQuantity;
            
            const lineTotalSpan = cartItemRow.querySelector('.fw-bold.text-danger.fs-5');
            if (lineTotalSpan) {
                lineTotalSpan.textContent = formatCurrency(result.newLineTotal);
            }
            
            if (cartItemRow) {
                cartItemRow.dataset.lineTotal = result.newLineTotal;
            }

            updateCartSummary(); 
            
            console.log('Update success:', result);
        } else {
            console.error('Update failed:', result.error);
            alert('Lỗi cập nhật số lượng: ' + (result.error || 'Lỗi không xác định'));
            quantityInput.value = currentQty;
        }
    } catch (error) {
        console.error('Error sending update request:', error);
        alert('Đã xảy ra lỗi mạng khi cập nhật giỏ hàng.');
        quantityInput.value = currentQty; 
    }
}

/**
 * === HÀM TỰ ĐỘNG CHẠY KHI TẢI TRANG ===
 * Thêm trình nghe sự kiện "submit"
 */
document.addEventListener('DOMContentLoaded', (event) => {
    // 1. Tính tổng ban đầu khi tải trang
    updateCartSummary();

    // 2. Gán sự kiện submit cho form giỏ hàng
    const cartForm = document.getElementById('cartForm');
    if (cartForm) {
        cartForm.addEventListener('submit', function(e) {
            // Đếm số lượng checkbox đã được chọn
            const selectedItemsCount = document.querySelectorAll('.cart-item-checkbox:checked').length;
            
            if (selectedItemsCount === 0) {
                // Ngăn form gửi đi
                e.preventDefault(); 
                
                // Hiển thị thông báo lỗi
                alert('Vui lòng chọn ít nhất một sản phẩm để thanh toán.');
            }
        });
    }
});