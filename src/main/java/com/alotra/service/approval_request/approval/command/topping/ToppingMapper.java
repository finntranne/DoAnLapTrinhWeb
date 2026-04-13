package com.alotra.service.approval_request.approval.command.topping;

import org.springframework.stereotype.Component;

import com.alotra.entity.draft.ToppingDraft;
import com.alotra.entity.product.Topping;

@Component
public class ToppingMapper {

	public Topping toEntity(ToppingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("ToppingDraft không được phép NULL");
        }
        if (draft.getShop() == null) {
            throw new IllegalArgumentException("Draft topping không có Shop. Không thể tạo Topping mới.");
        }
        Topping topping = new Topping();
        mapDraftToTopping(draft, topping);
        return topping;
    }

    public void updateEntity(Topping topping, ToppingDraft draft) {
        if (topping == null) {
            throw new IllegalArgumentException("Topping entity không được phép NULL");
        }
        mapDraftToTopping(draft, topping);
    }
    
    private void mapDraftToTopping(ToppingDraft draft, Topping topping) {
        // Giữ ShopID hiện tại nếu draft.getShop() là NULL
        if (draft.getShop() != null) {
            topping.setShop(draft.getShop());
        }
        topping.setToppingName(draft.getToppingName());
        topping.setPrice(draft.getPrice());
        topping.setImageURL(draft.getImageURL());
        topping.setStatus((byte) 1); // 1: Đang bán
    }
}
