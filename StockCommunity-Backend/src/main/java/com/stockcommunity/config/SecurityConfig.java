package com.stockcommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.stockcommunity.security.JwtAuthenticationEntryPoint;
import com.stockcommunity.security.JwtAuthenticationFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint; // 新增

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/", "/index.html", "/*.html", "/*.js", "/*.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stocks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stock-prices/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stock-summary/**").permitAll()
                        // 五檔下單頁改成不擋瀏覽，只在真的送出委託才要求登入，
                        // 但頁面上的五檔報價(market depth)原本沒開放，未登入查詢會被401
                        // 全域攔截器悄悄導回登入頁；這裡開放GET查詢，讀取盤口深度本身不需要知道是誰在看，
                        // 不影響OOVE/Wei的搓合引擎邏輯(只改權限規則，沒動撮合演算法)
                        .requestMatchers(HttpMethod.GET, "/api/market/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/industries/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        // Spring Boot 例外處理會把請求內部轉發到 /error（dispatcher type = ERROR）。
                        // 這個 stack 是 stateless（沒有 session），每個 dispatch 都是全新的 SecurityContext，
                        // JwtAuthenticationFilter 對 ERROR dispatch 預設不會重跑（shouldNotFilterErrorDispatch=true），
                        // 如果不放行 /error，這個轉發請求會被 anyRequest().authenticated() 擋下，
                        // JwtAuthenticationEntryPoint 又被觸發一次，把原本該回的 400（例如 Bean Validation 失敗）蓋成 401
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                // 未認證的請求（沒帶 token / token 無效）統一交給這裡回 401，
                // 而不是讓 Spring Security 用預設的匿名機制導致回 403
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}