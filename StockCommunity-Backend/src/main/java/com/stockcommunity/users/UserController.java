package com.stockcommunity.users;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.security.JwtUtil;
import com.stockcommunity.users.dto.ChangePasswordRequest;
import com.stockcommunity.users.dto.UpdateProfileRequest;
import com.stockcommunity.users.dto.UserResponseDTO;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider, JwtUtil jwtUtil) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
        this.jwtUtil = jwtUtil;
    }

    // 跟 OrderController、TradeController、HoldingController 同一套邏輯：從 SecurityContext 解出 username 再查 userId
    // 查詢自己的個人資料
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            Users user = userService.getProfile(userId);
            return ResponseEntity.ok(UserResponseDTO.fromEntity(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 修改自己的個人資料（大頭照、電話、帳號名稱）
    // 如果這次有真的改動 username，JWT 裡記的身份會跟資料庫對不上，
    // 所以額外簽一組新 token 一起回傳，前端要記得覆蓋掉 localStorage 裡的舊 token
    @PatchMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            String oldUsername = userService.getProfile(userId).getUsername();

            Users user = userService.updateProfile(
                    userId, request.getUserPhoto(), request.getPhone(), request.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("user", UserResponseDTO.fromEntity(user));

            if (!user.getUsername().equals(oldUsername)) {
                response.put("token", jwtUtil.generateToken(user.getUsername()));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 修改自己的密碼：userId 一樣從 CurrentUserProvider 解出來，不接受前端自己傳 userId，
    // 避免有人拿別人的身份（或偽造的參數）改掉別人的密碼
    @PatchMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "密碼修改成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}