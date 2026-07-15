package com.shopease.service;

import com.shopease.dto.CartDtos.*;
import com.shopease.entity.*;
import com.shopease.exception.*;
import com.shopease.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final CurrentUserService currentUser;

    @Transactional
    public CartResponse add(AddItemRequest request) {
        Cart cart = cart();
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        CartItem item = items.findByCartIdAndProductId(cart.getId(), product.getId()).orElseGet(() -> {
            CartItem value = new CartItem(); value.setCart(cart); value.setProduct(product); value.setQuantity(0); return value;
        });
        boolean newItem = item.getId() == null;
        int quantity = item.getQuantity() + request.quantity();
        validateStock(product, quantity);
        item.setQuantity(quantity); items.save(item);
        if (newItem) cart.getItems().add(item);
        cart.setUpdatedAt(Instant.now());
        return map(carts.save(cart));
    }
    @Transactional(readOnly = true) public CartResponse get() { return map(cart()); }
    @Transactional
    public CartResponse update(Long itemId, UpdateItemRequest request) {
        Cart cart = cart();
        CartItem item = ownedItem(cart, itemId);
        validateStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity()); items.save(item); cart.setUpdatedAt(Instant.now());
        return map(carts.save(cart));
    }
    @Transactional
    public CartResponse remove(Long itemId) {
        Cart cart = cart();
        CartItem item = ownedItem(cart, itemId);
        cart.getItems().removeIf(line -> line.getId().equals(item.getId()));
        items.delete(item);
        items.flush();
        cart.setUpdatedAt(Instant.now());
        return map(carts.save(cart));
    }
    @Transactional
    public void clear() { Cart cart = cart(); cart.getItems().clear(); carts.save(cart); }
    Cart cart() {
        User user = currentUser.get();
        return carts.findByUserId(user.getId()).orElseGet(() -> {
            Cart c = new Cart(); c.setUser(user); return carts.save(c);
        });
    }
    private CartItem ownedItem(Cart cart, Long itemId) {
        return items.findById(itemId).filter(i -> i.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }
    private void validateStock(Product p, int quantity) {
        if (quantity > p.getStock()) throw new BadRequestException("Only " + p.getStock() + " units are available for " + p.getName());
    }
    CartResponse map(Cart cart) {
        var lines = cart.getItems().stream().map(i -> new CartItemResponse(i.getId(), i.getProduct().getId(),
                i.getProduct().getName(), i.getProduct().getPrice(), i.getQuantity(),
                i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())), i.getProduct().getImageUrl())).toList();
        BigDecimal total = lines.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), lines, total);
    }
}
