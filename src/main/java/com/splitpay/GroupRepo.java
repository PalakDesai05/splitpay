package com.splitpay;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GroupRepo extends JpaRepository<ExpenseGroup, Long> {
    @Query("select g from ExpenseGroup g where :uid member of g.members")
    List<ExpenseGroup> findByMember(@Param("uid") Long uid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ExpenseGroup g where g.id = :id")
    Optional<ExpenseGroup> lockById(@Param("id") Long id);
}
