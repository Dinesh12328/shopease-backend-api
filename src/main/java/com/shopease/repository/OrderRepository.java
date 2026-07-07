package com.shopease.repository;
import com.shopease.entity.Order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findDetailedById(Long id);
}
