package com.splitpay;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepo extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupIdOrderByIdDesc(Long groupId);
    Optional<Settlement> findByFromUserAndIdempotencyKey(Long fromUser, String idempotencyKey);
}
