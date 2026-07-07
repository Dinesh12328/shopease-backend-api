package com.shopease.repository;
import com.shopease.entity.Cart;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Cart> findByUserId(Long userId);
}
