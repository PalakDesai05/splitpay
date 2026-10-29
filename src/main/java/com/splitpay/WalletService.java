package com.splitpay;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class WalletService {
    private final WalletRepo wallets;

    public Wallet get(Long uid) {
        return wallets.findById(uid).orElseThrow(() -> new ApiException(NOT_FOUND, "Wallet not found"));
    }

    @Transactional
    public Wallet deposit(Long uid, BigDecimal amt) {
        Wallet w = wallets.lockAll(List.of(uid)).stream().findFirst()
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Wallet not found"));
        w.setBalance(w.getBalance().add(amt));
        return w;
    }

    /** Atomic debit + credit. Joins the caller's transaction, so a later failure rolls both back. */
    @Transactional
    public void transfer(Long from, Long to, BigDecimal amt) {
        if (from.equals(to)) throw new ApiException(BAD_REQUEST, "Cannot pay yourself");
        Map<Long, Wallet> w = wallets.lockAll(List.of(from, to)).stream()
                .collect(Collectors.toMap(Wallet::getUserId, x -> x));
        Wallet a = w.get(from), b = w.get(to);
        if (a == null || b == null) throw new ApiException(NOT_FOUND, "Wallet not found");
        if (a.getBalance().compareTo(amt) < 0) throw new ApiException(UNPROCESSABLE_ENTITY, "Insufficient wallet balance");
        a.setBalance(a.getBalance().subtract(amt));
        b.setBalance(b.getBalance().add(amt));
    }
}
