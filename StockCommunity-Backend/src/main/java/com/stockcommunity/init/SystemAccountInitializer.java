// package com.stockcommunity.init;

// import java.util.Optional;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.context.event.EventListener;
// import org.springframework.stereotype.Component;
// import com.stockcommunity.event.SystemAccountCreateEvent;

// import com.stockcommunity.users.UserRepository;
// import com.stockcommunity.users.UserService;
// import com.stockcommunity.users.Users;

// import jakarta.transaction.Transactional;
// import lombok.RequiredArgsConstructor;


// // 造市者所用的系統帳號創建
// // 目前先照一般使用者創建，不過怎麼想怎麼不對，可能要改
// @Component
// @RequiredArgsConstructor
// public class SystemAccountInitializer {

//     private static final Logger log = LoggerFactory.getLogger(SystemAccountInitializer.class);
    
//     private final UserRepository userRepository; 
    
//     private final UserService userService;
    
//     // 發出監聽事件以便造市者創建Holding的方法追蹤是否已創建造市者帳號
//     private final ApplicationEventPublisher eventPublisher;
    
//     @EventListener(ApplicationReadyEvent.class) // 當 Spring Boot 完全啟動成功後觸發
//     @Transactional // 確保寫入資料庫的事務安全
//     public void initSystemAccount() {
//         log.info("正在檢查系統造市商帳號是否存在...");
        
//         // 1. 檢查是否已經存在於資料庫中
//         Optional<Users> systemAccount = userRepository.findByUsername("SYSTEM_MARKET_MAKER");
        
//         if (systemAccount.isEmpty()) {
//             log.info("偵測到系統造市商帳號不存在，正在自動建立系統帳號：{}", "SYSTEM_MARKET_MAKER");
             
//             // 存入資料庫
//             userService.register("SYSTEM_MARKET_MAKER", "000000", "0000@xxxx@smail.com");
//             log.info("系統造市商帳號 {} 建立成功！", "SYSTEM_MARKET_MAKER");
            
//         } else {
//             log.info("系統造市商帳號已存在，跳過建立流程。");
//         }
        
//         eventPublisher.publishEvent(
//                 new SystemAccountCreateEvent("確認造市者系統帳號")
//         );
//     }
// }
