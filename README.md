# StockCommunity

新手導向的股市模擬交易社群平台。使用者可以註冊登入、瀏覽股票行情、建立自選股清單、下單模擬買賣、查看持股與交易紀錄。

## 專案定位

給股市新手練習操作、學習股票知識的教學型平台，不追求還原真實交易所的完整複雜度。

## 技術棧

**後端**
- Java 17
- Spring Boot 4.1.0
- Spring Security + JWT（無狀態驗證）
- Spring Data JPA + Hibernate
- SQL Server
- Maven

**前端**
- Vue 3（Composition API）
- Vite
- Pinia（狀態管理）
- Vue Router
- Axios

## 專案結構

本專案採用 Mono-repo（單一儲存庫）架構管理，將前後端整合於同一個儲存庫中：

-  **[backend/](./StockCommunity-Backend)**：後端核心 API 服務。
    *   *詳細資料庫設定與環境變數請參閱：[後端技術文件](./StockCommunity-Backend/README.md)*
-  **[frontend/](./StockCommunity-Frontend)**：前端使用者網頁介面。
    *   *詳細開發設定請參閱：[前端技術文件](./StockCommunity-Frontend/README.md)*

## 模組總覽

| 模組 | 說明 | 負責人 |
|---|---|---|
| User | 註冊、登入、JWT 驗證、個人資料查看/修改 | WG |
| Stock | 股票基本資料（代號、名稱、市場別、產業別） | OOVE/Wei |
| StockPrice | 股票每日開高低收行情、漲跌幅計算 | WG |
| Watchlist | 自選股清單（建立清單、加入/移除股票） | WG |
| Order | 下單（買進/賣出）、查詢訂單、取消訂單 | OOVE/Wei |
| Holding | 持股（買進成交後更新持股均價、賣出扣減股數） | OOVE/Wei |
| MarketMaker | 自動產生訂單供使用者下訂的造市商 | OOVE |
| MatchingEngine | 負責管理搓合時的整個流程。 引擎的架構由 OOVE 負責設計，而 Wei 則負責核心撮合邏輯，涵蓋訂單匹配與訂單條件處理。 | OOVE/Wei |
| Trade | 成交紀錄（由撮合引擎內部呼叫寫入，不開放外部 API 新增）。 Trade 資料表設計與前端 API 串接由 WG 負責，另外 OOVE 開發訂單寫入器，並由 Wei 撰寫實際的寫入邏輯 | OOVE/Wei（撮合引擎主要負責人）、WG（Trade API 串接） |
| Post / Comment / Like | 社群功能：發文、留言、按讚 | PZ（PZHsu） |
| Message | 社群私訊功能 | PZ（PZHsu） |
| news | 提供相關的股票新聞 | PZ（PZHsu） |
