package com.alotra.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.entity.location.Address;
import com.alotra.entity.order.Order;
import com.alotra.entity.user.User;
import com.alotra.repository.location.AddressRepository;
import com.alotra.repository.order.OrderRepository;
import com.alotra.service.cart.CartService;
import com.alotra.service.product.CategoryService;
import com.alotra.service.shop.StoreService;
import com.alotra.service.user.UserService;
import com.alotra.view.address.AddressDisplayView;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/addresses")
public class CustomerAddressController {

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private StoreService storeService;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }

        String username = auth.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + username));
    }

    private int getCurrentCartItemCount() {
        try {
            return cartService.getCartItemCount(getCurrentAuthenticatedUser());
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return 0;
        }
    }

    private Integer getSelectedShopId(HttpSession session) {
        Integer selectedShopId = (Integer) session.getAttribute("selectedShopId");
        return selectedShopId == null ? 0 : selectedShopId;
    }

    @GetMapping
    public String showAddressList(Model model, HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            User user = getCurrentAuthenticatedUser();
            List<AddressDisplayView> addresses = addressRepository.findByUserId(user.getId()).stream()
                    .map(address -> new AddressDisplayView(address, user))
                    .toList();

            model.addAttribute("addresses", addresses);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/address_list";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        }
    }

    @GetMapping("/new")
    public String showAddAddressForm(Model model,
            @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin,
            HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            getCurrentAuthenticatedUser();

            model.addAttribute("address", new Address());
            model.addAttribute("pageTitle", "Them dia chi moi");
            model.addAttribute("formAction", "/user/addresses/save");
            model.addAttribute("originUrl", "checkout".equals(origin) ? "/checkout" : "/user/addresses");
            model.addAttribute("origin", origin);
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/address_form";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/save")
    public String saveAddress(@ModelAttribute("address") Address address,
            @RequestParam(name = "origin", required = false, defaultValue = "address_list") String origin,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();

            if (Boolean.TRUE.equals(address.getIsDefault())) {
                List<Address> allAddresses = addressRepository.findByUserId(user.getId());
                for (Address oldAddress : allAddresses) {
                    oldAddress.setIsDefault(false);
                }
                addressRepository.saveAll(allAddresses);
            }

            addressRepository.save(address);
            redirectAttributes.addFlashAttribute("successMessage", "Da luu dia chi thanh cong!");
            return "redirect:" + ("checkout".equals(origin) ? "/checkout" : "/user/addresses");
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi luu dia chi: " + ex.getMessage());
            return "redirect:/user/addresses/new?origin=" + origin;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            Address addressToDelete = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay dia chi."));

            List<Address> allAddresses = addressRepository.findByUserId(user.getId());
            if (Boolean.TRUE.equals(addressToDelete.getIsDefault()) && allAddresses.size() <= 1) {
                redirectAttributes.addFlashAttribute("errorMessage", "Khong the xoa dia chi mac dinh duy nhat.");
                return "redirect:/user/addresses";
            }

            if (addressRepository.existsShopUsingAddress(addressId)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Khong the xoa dia chi nay vi dang duoc shop su dung.");
                return "redirect:/user/addresses";
            }

            if (orderRepository.existsByAddress_AddressID(addressId)) {
                Address snapshotAddress = cloneAddress(addressToDelete);
                snapshotAddress = addressRepository.save(snapshotAddress);

                List<Order> linkedOrders = orderRepository.findByAddress_AddressID(addressId);
                for (Order order : linkedOrders) {
                    order.setAddress(snapshotAddress);
                }
                orderRepository.saveAll(linkedOrders);
            }

            addressRepository.delete(addressToDelete);

            if (Boolean.TRUE.equals(addressToDelete.getIsDefault()) && allAddresses.size() > 1) {
                Address newDefault = addressRepository.findByUserId(user.getId()).stream().findFirst().orElse(null);
                if (newDefault != null) {
                    newDefault.setIsDefault(true);
                    addressRepository.save(newDefault);
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Da xoa dia chi thanh cong!");
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Khong the xoa dia chi nay vi dang duoc lien ket voi du lieu khac.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi xoa dia chi: " + ex.getMessage());
        }
        return "redirect:/user/addresses";
    }

    private Address cloneAddress(Address source) {
        Address copy = new Address();
        copy.setProvince(source.getProvince());
        copy.setDistrict(source.getDistrict());
        copy.setWard(source.getWard());
        copy.setStreetAddress(source.getStreetAddress());
        copy.setIsDefault(Boolean.FALSE);
        return copy;
    }

    @PostMapping("/set-default/{id}")
    public String setDefaultAddress(@PathVariable("id") Integer addressId, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentAuthenticatedUser();
            Address newDefaultAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay dia chi."));

            List<Address> allAddresses = addressRepository.findByUserId(user.getId());
            for (Address address : allAddresses) {
                if (!address.getAddressID().equals(addressId)) {
                    address.setIsDefault(false);
                }
            }
            newDefaultAddress.setIsDefault(true);
            addressRepository.saveAll(allAddresses);
            redirectAttributes.addFlashAttribute("successMessage", "Da dat dia chi lam mac dinh!");
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi dat dia chi mac dinh.");
        }
        return "redirect:/user/addresses";
    }

    @GetMapping("/edit/{id}")
    public String showEditAddressForm(@PathVariable("id") Integer addressId, Model model, HttpSession session) {
        try {
            Integer selectedShopId = getSelectedShopId(session);
            getCurrentAuthenticatedUser();

            Address addressToEdit = addressRepository.findById(addressId)
                    .orElseThrow(() -> new EntityNotFoundException("Khong tim thay dia chi."));

            model.addAttribute("address", addressToEdit);
            model.addAttribute("pageTitle", "Chinh sua dia chi");
            model.addAttribute("formAction", "/user/addresses/update");
            model.addAttribute("originUrl", "/user/addresses");
            model.addAttribute("origin", "address_list");
            model.addAttribute("cartItemCount", getCurrentCartItemCount());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("shops", storeService.findAllActiveShops());
            model.addAttribute("selectedShopName", storeService.getShopNameById(selectedShopId));
            return "user/address_form";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException ex) {
            return "redirect:/user/addresses";
        }
    }

    @PostMapping("/update")
    public String updateAddress(@ModelAttribute("address") Address updatedAddress,
            RedirectAttributes redirectAttributes) {
        try {
            getCurrentAuthenticatedUser();

            if (updatedAddress.getAddressID() == null) {
                throw new IllegalArgumentException("Thieu ID dia chi de cap nhat.");
            }

            Address existingAddress = addressRepository.findById(updatedAddress.getAddressID())
                    .orElseThrow(() -> new EntityNotFoundException("Dia chi khong ton tai de cap nhat."));

            if (Boolean.TRUE.equals(updatedAddress.getIsDefault())) {
                List<Address> allAddresses = addressRepository.findAllByOrderByIsDefaultDescAddressIDDesc();
                for (Address address : allAddresses) {
                    if (!address.getAddressID().equals(updatedAddress.getAddressID())) {
                        address.setIsDefault(false);
                    }
                }
                addressRepository.saveAll(allAddresses);
            }

            existingAddress.setProvince(updatedAddress.getProvince());
            existingAddress.setDistrict(updatedAddress.getDistrict());
            existingAddress.setWard(updatedAddress.getWard());
            existingAddress.setStreetAddress(updatedAddress.getStreetAddress());
            existingAddress.setIsDefault(updatedAddress.getIsDefault());
            addressRepository.save(existingAddress);

            redirectAttributes.addFlashAttribute("successMessage", "Da cap nhat dia chi thanh cong!");
            return "redirect:/user/addresses";
        } catch (ResponseStatusException | UsernameNotFoundException ex) {
            return "redirect:/login";
        } catch (EntityNotFoundException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/user/addresses";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loi khi cap nhat dia chi.");
            return "redirect:/user/addresses";
        }
    }
}
