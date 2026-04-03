package com.alotra.view.address;

import com.alotra.entity.location.Address;
import com.alotra.entity.user.User;
import com.alotra.util.OrderPricingUtils;

public class AddressDisplayView {

    private final Integer addressID;
    private final Boolean isDefault;
    private final String recipientName;
    private final String phoneNumber;
    private final String fullAddress;
    private final Address address;

    public AddressDisplayView(Address address, User user) {
        this.address = address;
        this.addressID = address != null ? address.getAddressID() : null;
        this.isDefault = address != null ? address.getIsDefault() : Boolean.FALSE;
        this.recipientName = user != null ? user.getFullName() : "";
        this.phoneNumber = user != null ? user.getPhoneNumber() : "";
        this.fullAddress = address != null ? OrderPricingUtils.formatAddress(address) : "";
    }

    public Integer getAddressID() {
        return addressID;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public Address getAddress() {
        return address;
    }
}
