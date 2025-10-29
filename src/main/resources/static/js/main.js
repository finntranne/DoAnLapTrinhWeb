const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

async function selectShop(shopId, shopName) {
    try {
        const response = await fetch(`/select-shop/${shopId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(csrfHeader && csrfToken && { [csrfHeader]: csrfToken })
            }
        });

        if (response.ok) {
            localStorage.setItem("selectedShopId", shopId);
            localStorage.setItem("selectedShopName", shopName);
            window.location.href = '/';
        }
    } catch (error) {
        console.error(error);
    }
}

// Khi load lại trang:
window.addEventListener("DOMContentLoaded", () => {
    const shopId = localStorage.getItem("selectedShopId");
    if (shopId && shopId !== "0") {
        fetch(`/select-shop/${shopId}`, { method: 'POST' });
    }
});
