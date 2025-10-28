const SHOP_STORAGE_KEY = 'selectedShopId';
const SHOP_NAME_KEY = 'selectedShopName';

const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

// ✅ FIX: Sửa lỗi cú pháp fetch
async function selectShop(shopId, shopName) {
    localStorage.setItem(SHOP_STORAGE_KEY, shopId);
    localStorage.setItem(SHOP_NAME_KEY, shopName);

    try {
        const response = await fetch(`/select-shop/${shopId}`, { // ✅ Sửa từ fetch` thành fetch(`
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(csrfHeader && csrfToken && {[csrfHeader]: csrfToken})
            }
        });

        if (response.ok) {
            window.location.href = '/';
        } else {
            console.error('Failed to save shop ID to session. Server status:', response.status);
            alert('Lỗi: Không thể lưu lựa chọn cửa hàng. Vui lòng thử lại.');
        }
    } catch (error) {
        console.error('Network error during shop selection:', error);
        alert('Lỗi kết nối khi chọn cửa hàng.');
    }
}