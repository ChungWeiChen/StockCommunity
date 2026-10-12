package com.stockcommunity.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockcommunity.accounts.TradingAccountService;
import com.stockcommunity.common.SystemAccounts;
import com.stockcommunity.exception.UsernameSuggestionRequiredException;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TradingAccountService tradingAccountService;
    // 可能是 null：沒設定 spring.mail.host 的話，Spring 不會建立這個 Bean，
    // 用 ObjectProvider 包一層，注入本身不會失敗，實際要寄信時再檢查是不是 null
    private final JavaMailSender mailSender;

    // 系統做市帳號（SystemAccountInitializer 建立的那個）的 username，
    // 統一在這裡定義一次，login()/forgotPassword() 都會用到，
    // 避免同一個字串到處複製貼上、之後改了忘記同步
    private static final String SYSTEM_USERNAME = "SYSTEM_MARKET_MAKER";

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TradingAccountService tradingAccountService,
                       ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tradingAccountService = tradingAccountService;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Transactional
    public Users register(String username,
                          String password,
                          String email) {

        // 防呆：密碼不能是空字串（且建議至少要有基本長度限制）
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密碼不能為空");
        }

        // 防呆：Email 不能是空字串
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email 不能為空");
        }

        // 防呆：Email 不能重複
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        String finalUsername;

        if (username == null || username.trim().isEmpty()) {
            // 使用者沒填帳號：拿 email @ 前面那段當預設帳號
            String base = email.substring(0, email.indexOf('@'));

            if (userRepository.findByUsername(base).isPresent()) {
                // 預設帳號撞名：不默默改掉、也不擋住使用者，
                // 生一個建議名稱丟出去，讓前端顯示給使用者確認或自己修改
                String suggested = generateAvailableUsername(base);
                throw new UsernameSuggestionRequiredException(suggested);
            }
            finalUsername = base;
        } else {
            // 使用者自己手動輸入帳號：撞名就直接報錯，請他自己重新輸入
            if (userRepository.findByUsername(username.trim()).isPresent()) {
                throw new IllegalArgumentException("用戶名已被使用");
            }
            finalUsername = username.trim();
        }

        Users user = new Users();
        user.setUsername(finalUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);

        Users savedUser = userRepository.save(user);
        boolean systemAccount = SystemAccounts.RESERVED_USERNAMES.contains(savedUser.getUsername());
        // 綁定交易帳戶給新建立的user
        tradingAccountService.createAccountIfAbsent(savedUser.getId(), systemAccount);

        return savedUser;
    }

    // 幫沒有手動輸入帳號的使用者，在預設帳號撞名時生一個可用的建議名稱
    // 規則：base + 兩位數亂數（例如 Peter01 撞名 → 建議 Peter0102）
    private String generateAvailableUsername(String base) {
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            String candidate = base + String.format("%02d", random.nextInt(100));
            if (userRepository.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
        }
        // 極端情況：20 次都撞名，機率非常低，用更長的隨機字串保底，一定給得出可用名稱
        return base + UUID.randomUUID().toString().substring(0, 6);
    }

    public Users login(String email,
                       String password) {
        // 原本是用 username 找使用者：
        // Users user = userRepository.findByUsername(username)
        //         .orElseThrow(() -> new IllegalArgumentException("用戶名或密碼錯誤"));
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("信箱或密碼錯誤"));

        // 防呆：系統做市帳號的帳密是寫死的公開值，不能讓一般人從登入頁面直接登進去操作，
        // 錯誤訊息刻意跟下面密碼錯誤時用同一句，避免讓人猜出「密碼其實是對的、只是帳號被擋」
        if (user.getUsername().equals(SYSTEM_USERNAME)) {
            throw new IllegalArgumentException("信箱或密碼錯誤");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("信箱或密碼錯誤");
        }
        return user;
    }

    // 查自己的個人資料，對應 GET /api/users/me
    public Users getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
    }

    // 修改自己的個人資料，對應 PATCH /api/users/me
    // 大頭照、電話、username：null / 空字串代表這次沒有要改這個欄位，維持原值
    public Users updateProfile(Long userId, String userPhoto, String phone, String username) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        // userPhoto entity 欄位改成 byte[] 之後，JSON request body 沒辦法直接傳二進位資料，
        // 所以前端(或PZ)改傳 Base64 字串進來，這裡解碼成 byte[] 再存；
        // GET /api/users/me 回傳時 Jackson 會自動把 byte[] 編回 Base64 字串，讀取那邊不用額外處理
        if (userPhoto != null && !userPhoto.trim().isEmpty()) {
            user.setUserPhoto(Base64.getDecoder().decode(userPhoto));
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
        }

        // username 是使用者自己手動輸入要改的名字，撞名就直接報錯，
        // 讓使用者自己決定要改成什麼，不像註冊留空那種情況會自動生成建議名稱
        if (username != null && !username.trim().isEmpty()) {
            String trimmedUsername = username.trim();
            if (!trimmedUsername.equals(user.getUsername())) {
                if (userRepository.findByUsername(trimmedUsername).isPresent()) {
                    throw new IllegalArgumentException("這個帳號名稱已存在，請重新輸入");
                }
                user.setUsername(trimmedUsername);
            }
        }

        return userRepository.save(user);
    }

    // 修改自己的密碼，對應 PATCH /api/users/me/password
    // 一定要先驗證舊密碼正確才能改成新密碼，避免有人拿到別人的JWT（例如共用電腦忘記登出）
    // 就能直接把密碼改掉、把原本的使用者鎖在外面
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("舊密碼不能為空");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("新密碼不能為空");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("舊密碼錯誤");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密碼不能與舊密碼相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 忘記密碼，對應 POST /api/auth/forgot-password
    public void forgotPassword(String email) {
        Optional<Users> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return;
        }

        Users user = userOptional.get();

        if (user.getUsername().equals(SYSTEM_USERNAME)) {
            return;
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);

        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);
        user.setResetTokenExpiry(expiry);

        userRepository.save(user);

        if (mailSender == null) {
            log.warn("尚未設定郵件伺服器，忘記密碼信件未寄出");
            return;
        }

        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("重置密碼");
        message.setText("點擊以下連結重置您的密碼：" + resetLink);

        mailSender.send(message);
    }

    // 用重設密碼信件裡的 token 設定新密碼，對應 POST /api/auth/reset-password
    public void resetPassword(String token, String newPassword) {
        Users user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("重設連結無效或已過期"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("重設連結無效或已過期");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }
}
