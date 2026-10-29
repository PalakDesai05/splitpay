package com.splitpay;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public final class Dto {
    private Dto() {}
    public record Register(@NotBlank String name, @NotBlank @Email String email, @NotBlank @Size(min = 6) String password) {}
    public record Login(@NotBlank String email, @NotBlank String password) {}
    public record Token(String token, Long userId) {}
    public record Amount(@NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record NewGroup(@NotBlank String name, List<String> memberEmails) {}
    public record Member(@NotBlank String email) {}
    /** Equal split among `participants` (default: all members), or exact split via `exactShares`. */
    public record NewExpense(@NotBlank String description, @NotNull @DecimalMin("0.01") BigDecimal amount,
                             Long paidBy, List<Long> participants, Map<Long, BigDecimal> exactShares) {}
    public record NewSettlement(@NotNull Long toUser, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
}
