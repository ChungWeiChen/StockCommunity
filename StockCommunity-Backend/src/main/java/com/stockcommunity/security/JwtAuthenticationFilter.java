package com.stockcommunity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 每個請求進來時，檢查 Authorization header 有沒有帶有效的 JWT。
 *
 * 這支 filter 只負責一件事：token 驗證通過，就把身份寫進 SecurityContext。
 * 驗證失敗（不管是沒帶 token、格式錯、還是 token 本身無效）一律「不設定身份」，
 * 讓請求繼續往下走。
 *
 * 之後如果該請求打到需要登入的端點，Spring Security 會發現 SecurityContext
 * 裡沒有已認證的使用者，進而呼叫 JwtAuthenticationEntryPoint 統一回 401。
 * 這裡不用自己手動寫 response，避免邏輯分散在兩個地方。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 情況一：沒帶 Authorization header，或格式不是 "Bearer xxx"
        // → 不設定身份，直接放行，交給後面 Security 判斷是否需要登入
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // 去掉開頭的 "Bearer "

        // 情況二：有帶 token，且驗證通過 → 設定身份到 SecurityContext
        if (jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 情況三：有帶 token，但驗證失敗（過期/篡改/格式錯）
        // → 不設定身份，同樣放行。SecurityContext 是空的，
        //   之後會被 JwtAuthenticationEntryPoint 統一攔下回 401。
        // （這裡故意不 return、不寫 response，避免跟 EntryPoint 邏輯重複）

        filterChain.doFilter(request, response);
    }
}