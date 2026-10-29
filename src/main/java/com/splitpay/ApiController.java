package com.splitpay;

import static org.springframework.http.HttpStatus.CREATED;

import com.splitpay.DebtSimplifier.Transfer;
import com.splitpay.Dto.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ApiController {
    private final GroupService groups;
    private final SettlementService settlements;
    private final WalletService wallets;
    private final GroupRepo groupRepo;
    private final ExpenseRepo expenseRepo;
    private final SettlementRepo settlementRepo;

    // ---- wallet ----
    @GetMapping("/wallet")
    public Wallet wallet(@AuthenticationPrincipal Long uid) { return wallets.get(uid); }

    @PostMapping("/wallet/deposit")
    public Wallet deposit(@AuthenticationPrincipal Long uid, @Valid @RequestBody Amount r) { return wallets.deposit(uid, r.amount()); }

    // ---- groups ----
    @PostMapping("/groups") @ResponseStatus(CREATED)
    public ExpenseGroup create(@AuthenticationPrincipal Long uid, @Valid @RequestBody NewGroup r) { return groups.create(uid, r); }

    @GetMapping("/groups")
    public List<ExpenseGroup> mine(@AuthenticationPrincipal Long uid) { return groupRepo.findByMember(uid); }

    @GetMapping("/groups/{id}")
    public ExpenseGroup group(@PathVariable Long id, @AuthenticationPrincipal Long uid) { return groups.member(id, uid, false); }

    @PostMapping("/groups/{id}/members")
    public ExpenseGroup addMember(@PathVariable Long id, @AuthenticationPrincipal Long uid, @Valid @RequestBody Member r) {
        return groups.addMember(id, uid, r.email());
    }

    // ---- expenses & balances ----
    @PostMapping("/groups/{id}/expenses") @ResponseStatus(CREATED)
    public Expense addExpense(@PathVariable Long id, @AuthenticationPrincipal Long uid, @Valid @RequestBody NewExpense r) {
        return groups.addExpense(id, uid, r);
    }

    @GetMapping("/groups/{id}/expenses")
    public List<Expense> expenses(@PathVariable Long id, @AuthenticationPrincipal Long uid) {
        groups.member(id, uid, false);
        return expenseRepo.findByGroupIdOrderByIdDesc(id);
    }

    @GetMapping("/groups/{id}/balances")
    public Map<Long, BigDecimal> balances(@PathVariable Long id, @AuthenticationPrincipal Long uid) {
        return groups.balances(groups.member(id, uid, false));
    }

    /** Minimal set of payments that settles the whole group (heap-based). */
    @GetMapping("/groups/{id}/settle-up")
    public List<Transfer> plan(@PathVariable Long id, @AuthenticationPrincipal Long uid) {
        return DebtSimplifier.minimize(groups.balances(groups.member(id, uid, false)));
    }

    // ---- settlements (wallet payments) ----
    @PostMapping("/groups/{id}/settlements") @ResponseStatus(CREATED)
    public Settlement settle(@PathVariable Long id, @AuthenticationPrincipal Long uid,
                             @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody NewSettlement r) {
        return settlements.pay(id, uid, key, r);
    }

    @GetMapping("/groups/{id}/settlements")
    public List<Settlement> history(@PathVariable Long id, @AuthenticationPrincipal Long uid) {
        groups.member(id, uid, false);
        return settlementRepo.findByGroupIdOrderByIdDesc(id);
    }
}
