function logout() {
    if (!confirm("Bạn có chắc chắn muốn đăng xuất?")) return;

    fetch("/api/auth/logout", {
        method: "POST",
        credentials: "include"
    })
    .then(res => res.ok ? res.json() : Promise.reject("Logout failed"))
    .then(result => {
        alert(result.message || "Đăng xuất thành công!");
        window.location.href = "/login";
    })
    .catch(err => {
        console.error(err);
        alert("Đã xảy ra lỗi khi đăng xuất.");
        window.location.href = "/login";
    });
}

document.addEventListener("DOMContentLoaded", function() {
    const logoutBtn = document.getElementById("vendorLogoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", function(e) {
            e.preventDefault();
            logout();
        });
    }
});
