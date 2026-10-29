package com.splitpay;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupId(Long groupId);
    List<Expense> findByGroupIdOrderByIdDesc(Long groupId);
}
