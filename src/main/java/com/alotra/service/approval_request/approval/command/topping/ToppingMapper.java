package com.alotra.service.approval_request.approval.command.topping;

import org.springframework.stereotype.Component;

import com.alotra.entity.draft.ToppingDraft;
import com.alotra.entity.product.Topping;

@Component
public class ToppingMapper {

	public Topping toEntity(ToppingDraft draft) {
        Topping topping = new Topping();
        mapDraftToTopping(draft, topping);
        return topping;
    }

    public void updateEntity(Topping topping, ToppingDraft draft) {
        mapDraftToTopping(draft, topping);
    }
    
    private void mapDraftToTopping(ToppingDraft draft, Topping topping) {
        topping.setShop(draft.getShop());
        topping.setToppingName(draft.getToppingName());
        topping.setPrice(draft.getPrice());
        topping.setImageURL(draft.getImageURL());
        topping.setStatus((byte) 1); // 1: Đang bán


        
    }
}
