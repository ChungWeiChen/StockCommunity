package com.stockcommunity.users;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    // 忘記密碼流程用：拿使用者點連結帶回來的 token 找對應的使用者
    Optional<Users> findByResetToken(String resetToken);

    // 【修改】排除系統帳號(造市商/未來假AI帳號)，避免出現在使用者搜尋結果裡
    // 舊版：
    // @Query("from Users where username like %:username%")
    // List<Users> findAllByUsername(@Param("username") String username);
    @Query("from Users where username like %:username% and username not in :reservedUsernames")
    List<Users> findAllByUsername(
            @Param("username") String username,
            @Param("reservedUsernames") Set<String> reservedUsernames);

}
