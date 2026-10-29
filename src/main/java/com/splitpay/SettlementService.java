package com.splitpay;

import static org.springframework.http.HttpStatus.*;

import com.splitpay.Dto.NewSettlement;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service @RequiredArgsConstructor
public class SettlementService {
    private final GroupService groups;
    private final WalletService wallets;
    private final SettlementRepo repo;
    private final TransactionTemplate tx;

    /** Pays `uid -> r.toUser()` from wallet balance. Safe to retry with the same Idempotency-Key. */
    public Settlement pay(Long gid, Long uid, String key, NewSettlement r) {
        if (key == null || key.isBlank() || key.length() > 100) throw new ApiException(BAD_REQUEST, "Idempotency-Key must be 1-100 chars");
        Optional<Settlement> prior = repo.findByFromUserAndIdempotencyKey(uid, key);   // fast path for retries
        if (prior.isEmpty()) {
            try { return tx.execute(s -> process(gid, uid, key, r)); }
            catch (DataIntegrityViolationException dup) {                              // concurrent twin won the race
                prior = repo.findByFromUserAndIdempotencyKey(uid, key);
                if (prior.isEmpty()) throw dup;
            }
        }
        return replay(prior.get(), gid, r);
    }

    private Settlement process(Long gid, Long uid, String key, NewSettlement r) {
        ExpenseGroup g = groups.member(gid, uid, true);       // group lock: validation + payment are one critical section
        Optional<Settlement> dup = repo.findByFromUserAndIdempotencyKey(uid, key);   // re-check after acquiring the lock
        if (dup.isPresent()) return replay(dup.get(), gid, r);

        Long to = r.toUser();
        BigDecimal amt = r.amount().setScale(2, RoundingMode.HALF_UP);
        if (to.equals(uid) || !g.getMembers().contains(to)) throw new ApiException(BAD_REQUEST, "Invalid recipient");
        Map<Long, BigDecimal> net = groups.balances(g);
        if (net.get(uid).negate().compareTo(amt) < 0 || net.get(to).compareTo(amt) < 0)
            throw new ApiException(UNPROCESSABLE_ENTITY, "Amount exceeds what you owe or what the recipient is owed");

        Settlement s = new Settlement();
        s.setGroupId(gid); s.setFromUser(uid); s.setToUser(to); s.setAmount(amt); s.setIdempotencyKey(key);
        repo.saveAndFlush(s);                                  // claims the key (unique index) BEFORE money moves
        wallets.transfer(uid, to, amt);                        // same transaction: all-or-nothing
        return s;
    }

    private Settlement replay(Settlement s, Long gid, NewSettlement r) {
        if (!s.getGroupId().equals(gid) || !s.getToUser().equals(r.toUser())
                || s.getAmount().compareTo(r.amount().setScale(2, RoundingMode.HALF_UP)) != 0)
            throw new ApiException(UNPROCESSABLE_ENTITY, "Idempotency-Key was already used for a different request");
        return s;
    }
}
