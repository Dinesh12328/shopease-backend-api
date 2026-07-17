package com.shopease.repository;

import com.shopease.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(ci.quantity), 0) from CartItem ci where ci.product.id = :productId")
    long quantityReservedInCarts(@Param("productId") Long productId);

    @Query("select count(oi) from OrderItem oi where oi.product.id = :productId")
    long quantitySoldInOrders(@Param("productId") Long productId);

    @Query("select count(p) from Product p where p.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
