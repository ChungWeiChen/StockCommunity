package com.stockcommunity.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    // 原本是拿 username 登入，統一改成信箱驗證後保留在這裡當註解，
    // 如果之後要改回帳號登入可以參考
    // @NotBlank(message = "帳號不能為空")
    // private String username;

    @NotBlank(message = "信箱不能為空")
    private String email;

    @NotBlank(message = "密碼不能為空")
    private String password;
}
