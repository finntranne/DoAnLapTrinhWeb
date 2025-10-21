
const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");


// Hàm format tiền tệ (giữ nguyên hoặc copy từ detail.js)
function formatCurrency(price) {
    if (isNaN(price)) return 'N/A';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price).replace(/\s/g, '');
}

// Hàm cập nhật số lượng (AJAX)
async function updateQuantity(cartItemId, change) {
    const quantityInput = document.getElementById('quantity-' + cartItemId);
    if (!quantityInput) return;

    let currentQty = parseInt(quantityInput.value);
    let newQty = currentQty + change;

    // Giới hạn số lượng tối thiểu là 1
    if (newQty < 1) {
        newQty = 1;
    }
    // Không gửi yêu cầu nếu số lượng không đổi
    if (newQty === currentQty && change !== 0) return;

    // Chuẩn bị dữ liệu gửi đi
    const formData = new URLSearchParams();
    formData.append('quantity', newQty);

    // Chuẩn bị header
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    } else {
        console.warn("CSRF token not found. Request might fail if CSRF protection is enabled.");
    }

    try {
        // Gửi yêu cầu POST
        const response = await fetch(`/cart/update/${cartItemId}`, {
            method: 'POST',
            headers: headers,
            body: formData
        });

        const result = await response.json();

        if (response.ok) {
            // Cập nhật giao diện
            quantityInput.value = result.newQuantity;
            const lineTotalSpan = quantityInput.closest('.cart-item').querySelector('.row > .col-2.text-end > span');
            if (lineTotalSpan) {
                lineTotalSpan.textContent = formatCurrency(result.newLineTotal);
            }
            const subtotalSpan = document.querySelector('.list-group-item span:first-child + span');
            const grandTotalSpan = document.querySelector('.list-group-item.fs-5 span:first-child + span');
            if (subtotalSpan) {
                subtotalSpan.textContent = formatCurrency(result.newSubtotal);
            }
            if (grandTotalSpan) {
                grandTotalSpan.textContent = formatCurrency(result.newSubtotal);
            }
            console.log('Update success:', result);
        } else {
            console.error('Update failed:', result.error);
            alert('Lỗi cập nhật số lượng: ' + (result.error || 'Lỗi không xác định'));
            quantityInput.value = currentQty; // Hoàn tác nếu lỗi
        }
    } catch (error) {
        console.error('Error sending update request:', error);
        alert('Đã xảy ra lỗi mạng khi cập nhật giỏ hàng.');
        quantityInput.value = currentQty; // Hoàn tác nếu lỗi mạng
    }
}
