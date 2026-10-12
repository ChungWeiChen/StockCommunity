package com.stockcommunity.users.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 對應 PATCH /api/users/me
// 開放改大頭照、電話、帳號名稱；密碼、email 這些敏感欄位不放進來，避免使用者透過這支 API 亂改
// （密碼改動走專門的 PATCH /api/users/me/password，需要驗證舊密碼）
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    private String username;

    private String userPhoto;

    private String phone;
}