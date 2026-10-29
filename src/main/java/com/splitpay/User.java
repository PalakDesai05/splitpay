package com.splitpay;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users") @Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    String name;
    @Column(unique = true, nullable = false) String email;
    @JsonIgnore @Column(nullable = false) String password;
}
