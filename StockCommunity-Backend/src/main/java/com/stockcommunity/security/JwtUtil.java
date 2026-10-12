package com.stockcommunity.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // 密鑰：用來簽名跟驗證 token，改成從 application-local.properties 讀取 jwt.secret，
    // 不再寫死在程式碼裡；這個檔案被 .gitignore 排除，每台電腦要自己建立一份，不會被commit上去
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;

    // Spring 把 secret 注入完畢後會自動呼叫這個方法，把字串轉成簽名/驗證用的 SecretKey
    @PostConstruct
    private void init() {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // token 有效期限：這裡設定 24 小時（毫秒） expirationMs 意思是：過期時間長度，單位是毫秒
    private final long expirationMs = 24 * 60 * 60 * 1000;

    // 產生 token：登入成功後呼叫這個方法
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 從 token 裡取出 username
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // 驗證 token 是否合法（簽名正確 + 沒過期）
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
