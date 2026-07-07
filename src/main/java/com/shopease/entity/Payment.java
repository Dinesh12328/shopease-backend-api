package com.shopease.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "order_id", unique = true) private Order order;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status = PaymentStatus.PENDING;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    private String transactionId;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
