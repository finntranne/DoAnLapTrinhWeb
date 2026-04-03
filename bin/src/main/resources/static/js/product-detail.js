/**
 * Product Detail Page JavaScript
 * Handles: image gallery, size selection, quantity control, and price updates
 */

(function() {
    'use strict';

    // Configuration
    const CONFIG = {
        minQuantity: 1,
        maxQuantity: 999
    };

    // Get discount percentage from data attribute
    const discountPercentage = parseFloat(
        document.getElementById('productData')?.dataset.discount || 0
    );

    /**
     * Format price to Vietnamese currency
     * @param {number} price - Price value
     * @returns {string} Formatted price string
     */
    function formatCurrency(price) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price).replace(/\s/g, '');
    }

    /**
     * Calculate discounted price
     * @param {number} basePrice - Original price
     * @param {number} discount - Discount percentage
     * @returns {number} Discounted price
     */
    function calculateDiscountedPrice(basePrice, discount) {
        return basePrice * (100 - discount) / 100;
    }

    /**
     * Update displayed price based on selected variant
     */
    function updatePrice() {
        const selectedSizeInput = document.querySelector('input[name="size"]:checked');
        
        if (!selectedSizeInput) {
            console.warn('No size selected');
            return;
        }

        const basePrice = parseFloat(selectedSizeInput.dataset.price);
        const variantId = selectedSizeInput.dataset.variantId;
        
        // Update hidden variant ID field
        const variantIdField = document.getElementById('selectedVariantId');
        if (variantIdField) {
            variantIdField.value = variantId;
        }

        // Update price display
        const priceDisplay = document.getElementById('productPrice');
        const originalPriceDisplay = document.getElementById('originalPrice');

        if (!priceDisplay) return;

        if (discountPercentage && discountPercentage > 0) {
            // With discount
            const discountedPrice = calculateDiscountedPrice(basePrice, discountPercentage);
            priceDisplay.textContent = formatCurrency(discountedPrice);
            
            if (originalPriceDisplay) {
                originalPriceDisplay.textContent = formatCurrency(basePrice);
                originalPriceDisplay.style.display = 'inline';
            }
        } else {
            // Without discount
            priceDisplay.textContent = formatCurrency(basePrice);
            
            if (originalPriceDisplay) {
                originalPriceDisplay.style.display = 'none';
            }
        }
    }

    /**
     * Handle quantity change
     * @param {number} delta - Change amount (+1 or -1)
     */
    function changeQuantity(delta) {
        const quantityInput = document.getElementById('quantityInput');
        
        if (!quantityInput) return;

        let currentQuantity = parseInt(quantityInput.value) || CONFIG.minQuantity;
        currentQuantity += delta;

        // Validate quantity bounds
        if (currentQuantity < CONFIG.minQuantity) {
            currentQuantity = CONFIG.minQuantity;
        } else if (currentQuantity > CONFIG.maxQuantity) {
            currentQuantity = CONFIG.maxQuantity;
        }

        quantityInput.value = currentQuantity;
    }

    /**
     * Handle main product image change
     * @param {string} imageSrc - New image source URL
     */
    function changeMainImage(imageSrc) {
        const mainImage = document.getElementById('mainProductImage');
        
        if (mainImage && imageSrc) {
            mainImage.src = imageSrc;
        }
    }

    /**
     * Initialize image gallery
     */
    function initImageGallery() {
        const thumbnails = document.querySelectorAll('.product-thumbnail img');
        
        thumbnails.forEach(thumbnail => {
            thumbnail.addEventListener('click', function() {
                const imageSrc = this.dataset.image || this.src;
                changeMainImage(imageSrc);
                
                // Add active state
                thumbnails.forEach(t => t.classList.remove('active'));
                this.classList.add('active');
            });
            
            // Add cursor pointer style
            thumbnail.style.cursor = 'pointer';
        });
    }

    /**
     * Initialize size selection
     */
    function initSizeSelection() {
        const sizeOptions = document.querySelectorAll('.size-option');
        
        sizeOptions.forEach(option => {
            option.addEventListener('change', updatePrice);
        });
    }

    /**
     * Initialize quantity controls
     */
    function initQuantityControls() {
        const decreaseBtn = document.getElementById('decreaseQty');
        const increaseBtn = document.getElementById('increaseQty');
        
        if (decreaseBtn) {
            decreaseBtn.addEventListener('click', () => changeQuantity(-1));
        }
        
        if (increaseBtn) {
            increaseBtn.addEventListener('click', () => changeQuantity(1));
        }
    }

    /**
     * Initialize form validation
     */
    function initFormValidation() {
        const form = document.getElementById('addToCartForm');
        
        if (!form) return;
        
        form.addEventListener('submit', function(e) {
            const variantId = document.getElementById('selectedVariantId')?.value;
            const quantity = parseInt(document.getElementById('quantityInput')?.value);
            
            // Validate variant selection
            if (!variantId) {
                e.preventDefault();
                alert('Vui lòng chọn size sản phẩm');
                return false;
            }
            
            // Validate quantity
            if (!quantity || quantity < CONFIG.minQuantity) {
                e.preventDefault();
                alert('Số lượng không hợp lệ');
                return false;
            }
            
            return true;
        });
    }

    /**
     * Initialize all features when DOM is ready
     */
    function init() {
        // Initialize price display
        updatePrice();
        
        // Initialize event listeners
        initImageGallery();
        initSizeSelection();
        initQuantityControls();
        initFormValidation();
        
        console.log('Product detail page initialized');
    }

    // Run initialization when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();