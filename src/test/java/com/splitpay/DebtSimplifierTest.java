package com.splitpay;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class DebtSimplifierTest {
    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    void settlesEveryoneToZeroWithFewTransfers() {
        Map<Long, BigDecimal> net = Map.of(1L, bd("60.00"), 2L, bd("-40.00"), 3L, bd("-30.00"), 4L, bd("10.00"));
        var plan = DebtSimplifier.minimize(net);
        assertTrue(plan.size() <= 3);                       // at most n-1 payments
        Map<Long, BigDecimal> after = new HashMap<>(net);
        plan.forEach(t -> {
            after.merge(t.from(), t.amount(), BigDecimal::add);
            after.merge(t.to(), t.amount().negate(), BigDecimal::add);
        });
        assertTrue(after.values().stream().allMatch(v -> v.signum() == 0));
    }

    @Test
    void emptyWhenAlreadySettled() {
        assertTrue(DebtSimplifier.minimize(Map.of(1L, BigDecimal.ZERO, 2L, BigDecimal.ZERO)).isEmpty());
    }
}
