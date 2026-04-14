package com.alotra.service.checkout;

import org.springframework.transaction.annotation.Transactional;

public abstract class CheckoutTemplate {

    /**
     * The Template Method outlining the steps for an order checkout.
     * @param context the checkout context
     * @return the context with the processed Order attached
     */
    @Transactional
    public CheckoutContext processCheckout(CheckoutContext context) {
        validateBusinessRules(context);
        prepareAddress(context);
        saveOrderAndItems(context);
        processPayment(context);
        createOrderHistory(context);
        notifyVendor(context);
        clearCart(context);
        return context;
    }

    protected abstract void validateBusinessRules(CheckoutContext context);

    protected abstract void prepareAddress(CheckoutContext context);

    protected abstract void saveOrderAndItems(CheckoutContext context);

    protected abstract void processPayment(CheckoutContext context);

    protected abstract void createOrderHistory(CheckoutContext context);

    protected abstract void notifyVendor(CheckoutContext context);

    protected abstract void clearCart(CheckoutContext context);
}
