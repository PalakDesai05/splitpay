package com.splitpay;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Wallet {
    @Id Long userId;
    @Column(nullable = false, precision = 14, scale = 2) BigDecimal balance = BigDecimal.ZERO;
    public Wallet(Long userId) { this.userId = userId; }
}
