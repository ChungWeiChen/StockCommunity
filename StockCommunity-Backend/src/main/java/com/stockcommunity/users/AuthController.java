package com.stockcommunity.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.exception.UsernameSuggestionRequiredException;
import com.stockcommunity.security.JwtUtil;
import com.stockcommunity.users.dto.ForgotPasswordRequest;
import com.stockcommunity.users.dto.LoginRequest;
import com.stockcommunity.users.dto.RegisterRequest;
import com.stockcommunity.users.dto.ResetPasswordRequest;
import com.stockcommunity.users.dto.UserResponseDTO;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Users user = userService.register(request.getUsername(), request.getPassword(), request.getEmail());
            return ResponseEntity.ok(UserResponseDTO.fromEntity(user));
        } catch (UsernameSuggestionRequiredException e) {
            // 留空帳號、系統自動產生的預設帳號撞名：不算失敗，回 409 附上建議帳號，
            // 讓前端顯示給使用者確認/修改，再重打一次註冊
            Map<String, Object> body = new HashMap<>();
            body.put("message", e.getMessage());
            body.put("suggestedUsername", e.getSuggestedUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 原本是用 username 登入：
            // Users user = userService.login(request.getUsername(), request.getPassword());
            Users user = userService.login(request.getEmail(), request.getPassword());
            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("如果該 Email 存在，重設密碼信件已寄出");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("密碼重設成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}