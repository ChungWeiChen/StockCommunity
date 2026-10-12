package com.stockcommunity.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    // 帳號改成選填：留空的話後端會拿 email 前半段自動產生一個預設帳號
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, message = "密碼長度至少需要 6 個字元")
    private String password;

    @NotBlank(message = "Email 不能為空")
    // 原本只用 @Email 驗證，但Hibernate Validator預設的@Email規則其實允許中文等Unicode字元
    // （例如「中文@gmail.com」會被誤判成合法），改用明確限制只能ASCII英數字元跟常見符號的正則，
    // 才能真正擋掉不是真實信箱格式的輸入
    // @Email(message = "Email 格式不正確")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email 格式不正確")
    private String email;
}