package com.splitpay;

import static org.springframework.http.HttpStatus.*;

import com.splitpay.Dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepo users;
    private final WalletRepo wallets;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/register") @ResponseStatus(CREATED) @Transactional
    public Token register(@Valid @RequestBody Register r) {
        String email = r.email().toLowerCase();
        if (users.existsByEmail(email)) throw new ApiException(CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        users.save(u);
        wallets.save(new Wallet(u.getId()));               // every user gets a wallet (balance 0)
        return new Token(jwt.issue(u.getId()), u.getId());
    }

    @PostMapping("/login")
    public Token login(@Valid @RequestBody Login r) {
        User u = users.findByEmail(r.email().toLowerCase()).filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ApiException(UNAUTHORIZED, "Invalid credentials"));
        return new Token(jwt.issue(u.getId()), u.getId());
    }
}
