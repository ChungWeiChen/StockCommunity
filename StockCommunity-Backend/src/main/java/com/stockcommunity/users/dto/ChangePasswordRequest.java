package com.stockcommunity.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 對應 PATCH /api/users/me/password
// userId 一律從 JWT 解出來（CurrentUserProvider），這裡只收舊密碼、新密碼，
// 不開放前端自己傳 userId，避免有人偽造別人的身份改別人的密碼
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "舊密碼不能為空")
    private String oldPassword;

    @NotBlank(message = "新密碼不能為空")
    @Size(min = 6, message = "密碼長度至少需要 6 個字元")
    private String newPassword;

}
