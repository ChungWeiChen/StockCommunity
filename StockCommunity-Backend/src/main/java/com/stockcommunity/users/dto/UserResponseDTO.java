package com.stockcommunity.users.dto;

import java.time.LocalDateTime;

import com.stockcommunity.users.Users;

import lombok.Data;

// 給前端看的「自己的個人資料」——password 本來就有 @JsonIgnore 不會外流，
// 但 resetToken / resetTokenExpiry 沒有，這兩個是忘記密碼流程的內部憑證，
// 前端完全用不到，也不該讓它們出現在任何 API 回應裡，所以額外做一層 DTO 把它們擋掉
@Data
public class UserResponseDTO {

	private Long id;

	private String username;

	private String email;

	private String phone;

	private byte[] userPhoto;

	private LocalDateTime createdAt;

	public static UserResponseDTO fromEntity(Users user) {

		UserResponseDTO dto = new UserResponseDTO();

		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setPhone(user.getPhone());
		dto.setUserPhoto(user.getUserPhoto());
		dto.setCreatedAt(user.getCreatedAt());

		return dto;
	}

}
