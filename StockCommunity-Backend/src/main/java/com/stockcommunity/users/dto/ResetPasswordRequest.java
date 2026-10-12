package com.stockcommunity.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 對應 POST /api/auth/reset-password
// token 來自忘記密碼信件裡的連結，不需要 JWT 就能呼叫（使用者本來就是因為忘記密碼、登不進去才會用到這支）
@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Token 不能為空")
    private String token;

    @NotBlank(message = "新密碼不能為空")
    @Size(min = 6, message = "密碼長度至少需要 6 個字元")
    private String newPassword;

}
