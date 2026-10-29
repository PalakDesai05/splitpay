package com.splitpay;

import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Entity @Table(name = "expense_groups") @Getter @Setter
public class ExpenseGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    String name;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "user_id")
    Set<Long> members = new HashSet<>();
}
