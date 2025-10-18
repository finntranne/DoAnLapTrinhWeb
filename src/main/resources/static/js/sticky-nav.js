// Sticky Navigation on Scroll
document.addEventListener('DOMContentLoaded', function() {
    const stickyNav = document.getElementById('stickyNav');
    
    if (stickyNav) {
        let lastScrollTop = 0;
        
        window.addEventListener('scroll', function() {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            
            // Hiển thị sticky nav khi cuộn xuống hơn 150px
            if (scrollTop > 200) {
                stickyNav.classList.add('show');
            } else {
                stickyNav.classList.remove('show');
            }
            
            lastScrollTop = scrollTop;
        });
    }
});