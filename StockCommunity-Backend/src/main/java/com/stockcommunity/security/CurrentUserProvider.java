package com.stockcommunity.security;

import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentUserId() {
        // SecurityContextHolder：目前這個請求（Thread）的身份容器
        // JwtAuthenticationFilter 驗證 token 成功後，會把身份塞進這裡
        // 每個請求各自獨立，不會跟別的使用者的資料混在一起
        // Authentication：代表「這次請求是誰打的」
        // 目前 filter 只有把 username 放進 principal，沒有放 userId
        // 所以還是得靠 username 去資料庫查出真正的 userId
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("目前沒有已驗證的使用者，此方法只能在需要登入的端點被呼叫");
        }

        String username = String.valueOf(authentication.getPrincipal());

        // 用 username 查出對應的使用者，回傳他的 id
        // 刻意不接受前端自己傳 userId 進來，避免有人偽造別人的身份操作資料
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        return user.getId();
    }

}
