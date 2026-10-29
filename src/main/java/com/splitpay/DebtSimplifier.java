package com.splitpay;

import java.math.BigDecimal;
import java.util.*;

/** Minimises the number of payments needed to settle a group using two max-heaps (greedy). */
public final class DebtSimplifier {
    private DebtSimplifier() {}

    public record Transfer(Long from, Long to, BigDecimal amount) {}

    /**
     * @param net userId -> net balance (positive = is owed money, negative = owes money; must sum to 0)
     * @return at most (nonZeroBalances - 1) transfers; O(n log n)
     */
    public static List<Transfer> minimize(Map<Long, BigDecimal> net) {
        Comparator<long[]> largestFirst = (a, b) -> Long.compare(b[1], a[1]);
        PriorityQueue<long[]> creditors = new PriorityQueue<>(largestFirst), debtors = new PriorityQueue<>(largestFirst);
        net.forEach((id, v) -> {
            long cents = v.movePointRight(2).longValue();   // work in integer cents: no rounding drift
            if (cents > 0) creditors.add(new long[]{id, cents});
            else if (cents < 0) debtors.add(new long[]{id, -cents});
        });
        List<Transfer> out = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] c = creditors.poll(), d = debtors.poll();
            long x = Math.min(c[1], d[1]);
            out.add(new Transfer(d[0], c[0], BigDecimal.valueOf(x, 2)));
            if ((c[1] -= x) > 0) creditors.add(c);
            if ((d[1] -= x) > 0) debtors.add(d);
        }
        return out;
    }
}
