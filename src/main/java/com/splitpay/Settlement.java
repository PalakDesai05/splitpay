package com.splitpay;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "settlements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_user", "idempotency_key"}),
       indexes = @Index(columnList = "group_id"))
@Getter @Setter
public class Settlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "group_id", nullable = false) Long groupId;
    @Column(name = "from_user", nullable = false) Long fromUser;
    @Column(name = "to_user", nullable = false) Long toUser;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount;
    @Column(name = "idempotency_key", nullable = false, length = 100) String idempotencyKey;
    Instant createdAt = Instant.now();
}
