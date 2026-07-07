package com.shopease.service;

import com.shopease.dto.OrderDtos.*;
import com.shopease.entity.*;
import com.shopease.exception.*;
import com.shopease.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final CartRepository carts;
    private final ProductRepository products;
    private final CurrentUserService currentUser;

    @Transactional
    public OrderResponse place(PlaceOrderRequest request) {
        User user = currentUser.get();
        Cart cart = carts.findByUserId(user.getId()).orElseThrow(() -> new BadRequestException("Cart is empty"));
        if (cart.getItems().isEmpty()) throw new BadRequestException("Cart is empty");
        Order order = new Order(); order.setUser(user); order.setShippingAddress(request.shippingAddress().trim());
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = products.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("A product in your cart no longer exists"));
            if (cartItem.getQuantity() > product.getStock())
                throw new BadRequestException("Insufficient stock for " + product.getName());
            product.setStock(product.getStock() - cartItem.getQuantity());
            OrderItem line = new OrderItem(); line.setOrder(order); line.setProduct(product);
            line.setProductName(product.getName()); line.setPrice(product.getPrice()); line.setQuantity(cartItem.getQuantity());
            order.getItems().add(line);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total); order = orders.save(order);
        Payment payment = new Payment(); payment.setOrder(order); payment.setMethod(request.paymentMethod()); payment.setAmount(total);
        if (request.paymentMethod() == PaymentMethod.COD) payment.setStatus(PaymentStatus.PENDING);
        else { payment.setStatus(PaymentStatus.COMPLETED); payment.setTransactionId(UUID.randomUUID().toString()); }
        payments.save(payment);
        cart.getItems().clear(); carts.save(cart);
        return map(order, payment);
    }
    @Transactional(readOnly = true)
    public Page<OrderResponse> history(Pageable pageable) {
        return orders.findByUserId(currentUser.get().getId(), pageable).map(o -> map(o, payments.findByOrderId(o.getId()).orElse(null)));
    }
    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order order = orders.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        User user = currentUser.get();
        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId()))
            throw new ResourceNotFoundException("Order not found: " + id);
        return map(order, payments.findByOrderId(id).orElse(null));
    }
    @Transactional(readOnly = true)
    public Page<OrderResponse> all(Pageable pageable) {
        return orders.findAll(pageable).map(o -> map(o, payments.findByOrderId(o.getId()).orElse(null)));
    }
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = orders.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED)
            throw new BadRequestException("A completed or cancelled order cannot be changed");
        if (status == OrderStatus.CANCELLED) {
            order.getItems().forEach(line -> {
                Product p = line.getProduct(); p.setStock(p.getStock() + line.getQuantity());
            });
            payments.findByOrderId(id).filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                    .ifPresent(p -> p.setStatus(PaymentStatus.REFUNDED));
        }
        order.setStatus(status); return map(orders.save(order), payments.findByOrderId(id).orElse(null));
    }
    private OrderResponse map(Order order, Payment payment) {
        var lines = order.getItems().stream().map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProductName(),
                i.getPrice(), i.getQuantity(), i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        PaymentResponse p = payment == null ? null : new PaymentResponse(payment.getId(), payment.getMethod(),
                payment.getStatus(), payment.getAmount(), payment.getTransactionId());
        return new OrderResponse(order.getId(), lines, order.getTotalAmount(), order.getShippingAddress(),
                order.getStatus(), order.getCreatedAt(), p);
    }
}
