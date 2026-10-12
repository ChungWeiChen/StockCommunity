package com.stockcommunity.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 統一處理「未認證」的請求應該怎麼回應。
 *
 * 什麼時候會被呼叫：
 * - 使用者完全沒帶 Authorization header
 * - 帶了 token，但 JwtAuthenticationFilter 判斷無效（過期/篡改/格式錯），
 *   因此沒有把 Authentication 放進 SecurityContext
 *
 * 只要最後 SecurityContext 裡沒有已認證的使用者，Spring Security
 * 就會呼叫這裡，而不是讓匿名 filter 把請求包裝成「已認證的訪客」
 * （那樣後面授權判斷會變成 403，語意上是錯的——未登入應該是 401）。
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 未認證/認證失敗的請求會統一走到這裡；記錄 method/path 方便之後追蹤
        // 是否有端點被異常大量嘗試存取，或前端哪裡漏帶 token
        log.info("JwtAuthenticationEntryPoint triggered: {} {} - reason: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "請先登入或重新登入"
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}