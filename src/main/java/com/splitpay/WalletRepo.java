package com.splitpay;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WalletRepo extends JpaRepository<Wallet, Long> {
    /** Row-locks wallets in id order, so concurrent transfers can never deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId in :ids order by w.userId")
    List<Wallet> lockAll(@Param("ids") Collection<Long> ids);
}
