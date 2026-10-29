package com.splitpay;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity @Table(name = "expenses", indexes = @Index(columnList = "group_id")) @Getter @Setter
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "group_id", nullable = false) Long groupId;
    @Column(name = "paid_by", nullable = false) Long paidBy;
    String description;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount;
    Instant createdAt = Instant.now();
    /** userId -> share of the expense (sums to amount). */
    @ElementCollection(fetch = FetchType.EAGER) @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "expense_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "user_id") @Column(name = "amount", precision = 12, scale = 2)
    Map<Long, BigDecimal> shares = new HashMap<>();
}
