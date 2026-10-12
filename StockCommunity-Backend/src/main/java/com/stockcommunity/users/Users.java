package com.stockcommunity.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(unique = true)
    private String email;

    private String phone;

    // @Lob 一定要加：byte[] 沒加這個 Hibernate 預設只會建一個 VARBINARY(255) 的小欄位，
    // 隨便一張照片就會存不下，等於重演今天want_buy_price那次NOT NULL的雷（欄位型別建錯，事後才發現）
    @Lob
    @Column(columnDefinition = "VARBINARY(MAX)")
    private byte[] userPhoto;

    private LocalDateTime createdAt;

    // 忘記密碼用：產生重設密碼的 token 跟過期時間，平常是 null，
    // 只有使用者按了忘記密碼、且還在有效期限內才會有值；用過一次或過期就清空
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
