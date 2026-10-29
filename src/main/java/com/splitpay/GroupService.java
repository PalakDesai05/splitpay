package com.splitpay;

import static org.springframework.http.HttpStatus.*;

import com.splitpay.Dto.*;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class GroupService {
    private final GroupRepo groups;
    private final UserRepo users;
    private final ExpenseRepo expenses;
    private final SettlementRepo settlements;

    /** Loads the group and verifies membership; lock=true takes a row lock (serialises settlements per group). */
    public ExpenseGroup member(Long gid, Long uid, boolean lock) {
        ExpenseGroup g = (lock ? groups.lockById(gid) : groups.findById(gid))
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Group not found"));
        if (!g.getMembers().contains(uid)) throw new ApiException(FORBIDDEN, "Not a member of this group");
        return g;
    }

    @Transactional
    public ExpenseGroup create(Long uid, NewGroup r) {
        ExpenseGroup g = new ExpenseGroup();
        g.setName(r.name());
        g.getMembers().add(uid);
        if (r.memberEmails() != null) r.memberEmails().forEach(e -> g.getMembers().add(userByEmail(e).getId()));
        return groups.save(g);
    }

    @Transactional
    public ExpenseGroup addMember(Long gid, Long uid, String email) {
        ExpenseGroup g = member(gid, uid, false);
        g.getMembers().add(userByEmail(email).getId());
        return g;
    }

    @Transactional
    public Expense addExpense(Long gid, Long uid, NewExpense r) {
        ExpenseGroup g = member(gid, uid, false);
        Long payer = r.paidBy() == null ? uid : r.paidBy();
        if (!g.getMembers().contains(payer)) throw new ApiException(BAD_REQUEST, "Payer is not a group member");
        Expense e = new Expense();
        e.setGroupId(gid);
        e.setPaidBy(payer);
        e.setDescription(r.description());
        e.setAmount(r.amount().setScale(2, RoundingMode.HALF_UP));
        e.setShares(split(r, g.getMembers(), e.getAmount()));
        return expenses.save(e);
    }

    /** Net balance per member: positive = is owed, negative = owes. Sums to zero. */
    public Map<Long, BigDecimal> balances(ExpenseGroup g) {
        Map<Long, BigDecimal> m = new TreeMap<>();
        g.getMembers().forEach(id -> m.put(id, BigDecimal.ZERO));
        for (Expense e : expenses.findByGroupId(g.getId())) {
            m.merge(e.getPaidBy(), e.getAmount(), BigDecimal::add);
            e.getShares().forEach((u, a) -> m.merge(u, a.negate(), BigDecimal::add));
        }
        for (Settlement s : settlements.findByGroupIdOrderByIdDesc(g.getId())) {
            m.merge(s.getFromUser(), s.getAmount(), BigDecimal::add);         // payer owes less
            m.merge(s.getToUser(), s.getAmount().negate(), BigDecimal::add);  // payee is owed less
        }
        return m;
    }

    private Map<Long, BigDecimal> split(NewExpense r, Set<Long> members, BigDecimal amt) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        if (r.exactShares() != null && !r.exactShares().isEmpty()) {
            r.exactShares().forEach((u, v) -> out.put(u, v.setScale(2, RoundingMode.HALF_UP)));
            if (out.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(amt) != 0)
                throw new ApiException(BAD_REQUEST, "exactShares must add up to the amount");
            if (out.values().stream().anyMatch(v -> v.signum() < 0)) throw new ApiException(BAD_REQUEST, "Negative share");
        } else {
            List<Long> p = r.participants() == null || r.participants().isEmpty()
                    ? members.stream().sorted().toList() : r.participants().stream().distinct().toList();
            long cents = amt.movePointRight(2).longValueExact(), base = cents / p.size(), rem = cents % p.size();
            for (int i = 0; i < p.size(); i++) out.put(p.get(i), BigDecimal.valueOf(base + (i < rem ? 1 : 0), 2)); // leftover cents spread
        }
        if (!members.containsAll(out.keySet())) throw new ApiException(BAD_REQUEST, "All participants must be group members");
        return out;
    }

    private User userByEmail(String email) {
        return users.findByEmail(email.toLowerCase()).orElseThrow(() -> new ApiException(NOT_FOUND, "No user with email " + email));
    }
}
