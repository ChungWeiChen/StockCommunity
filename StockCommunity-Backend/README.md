# StockCommunity

新手導向的股市模擬交易社群平台。使用者可以註冊登入、瀏覽股票行情、建立自選股清單、下單模擬買賣、查看持股與交易紀錄。

## 專案定位

給股市新手練習操作、學習股票知識的教學型平台，不追求還原真實交易所的完整複雜度（例如不做多層級管理員、不追求高併發撮合效能）。

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

本專案分成兩個獨立的 Repo：

- `StockCommunity`：後端（本 repo）
- `StockCommunity-frontend`：前端

## 模組總覽

| 模組 | 說明 | 負責人 |
|---|---|---|
| User | 註冊、登入、JWT 驗證、個人資料查看/修改 | WG |
| Stock | 股票基本資料（代號、名稱、市場別、產業別） | OOVE/Wei |
| StockPrice | 股票每日開高低收行情、漲跌幅計算 | WG |
| Watchlist | 自選股清單（建立清單、加入/移除股票） | WG |
| Order | 下單（買進/賣出）、查詢訂單、取消訂單 | OOVE/Wei |
| Holding | 持股（買進成交後更新持股均價、賣出扣減股數） | OOVE/Wei |
| Trade | 成交紀錄（由撮合引擎內部呼叫寫入，不開放外部 API 新增）。撮合核心邏輯（訂單匹配、產生成交紀錄）由 Wei 開發，WG 負責 Trade 資料表與 API 串接 | OOVE/Wei（撮合引擎主要負責人）、WG（Trade API 串接） |
| Post / Comment / Like | 社群功能：發文、留言、按讚 | PZ（PZHsu） |
| Message | 社群私訊功能 | PZ（PZHsu） |

## 環境設定（第一次拉下專案要做的事）

### 1. 設定環境變數

資料庫連線資訊、JWT 密鑰與郵件帳密不會寫入版本控制。啟動後端前，請在執行環境設定下列變數：

| 環境變數 | 說明 |
|---|---|
| `DB_URL` | SQL Server JDBC 連線網址 |
| `DB_USERNAME` | 資料庫帳號 |
| `DB_PASSWORD` | 資料庫密碼 |
| `JWT_SECRET` | JWT 簽名密鑰，至少 32 個 ASCII 字元 |
| `MAIL_USERNAME` | 寄送重設密碼郵件的帳號（不使用寄信功能時可留空） |
| `MAIL_PASSWORD` | 郵件服務的應用程式密碼（不使用寄信功能時可留空） |

PowerShell 範例（只對目前終端機工作階段生效）：

```powershell
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=stockcommunity;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "請填入資料庫帳號"
$env:DB_PASSWORD = "請填入資料庫密碼"
$env:JWT_SECRET = "請填入至少32個ASCII字元的隨機密鑰"
$env:MAIL_USERNAME = ""
$env:MAIL_PASSWORD = ""
```

macOS / Linux 範例：

```bash
export DB_URL='jdbc:sqlserver://localhost:1433;databaseName=stockcommunity;encrypt=true;trustServerCertificate=true'
export DB_USERNAME='請填入資料庫帳號'
export DB_PASSWORD='請填入資料庫密碼'
export JWT_SECRET='請填入至少32個ASCII字元的隨機密鑰'
export MAIL_USERNAME=''
export MAIL_PASSWORD=''
```

`JWT_SECRET` 是簽名／驗證登入 JWT 用的密鑰；沒設定時程式會啟動失敗。正式環境應使用獨立產生的隨機密鑰，且不要提交到 Git。

### 2. 確認本機已安裝 SQL Server，並建立資料庫

資料庫名稱需與 `DB_URL` 中的 `databaseName` 對應；上述本機範例使用 `stockcommunity`。

### 3. 用 Maven 建置並啟動

```bash
# Windows
.\mvnw clean install
.\mvnw spring-boot:run

# Mac / Linux
./mvnw clean install
./mvnw spring-boot:run
```

啟動成功後，後端會跑在 `http://localhost:8080`。

### 4. 前端啟動（另一個 repo：StockCommunity-frontend）

```bash
npm install
npm run dev
```


## 錯誤處理慣例

各模組的驗證錯誤統一丟出以下例外類型，由各 Controller 接住並轉換成對應的 HTTP 狀態碼：

- `IllegalArgumentException` → 400（請求資料不合法，例如欄位為空、格式錯誤）
- 查無資料 → 404（例如 `/api/stocks/{不存在的代號}`）
- `SecurityException` → 403（無權限操作，例如操作別人的資料）
- `IllegalStateException` → 409（狀態不允許此操作，例如訂單已成交卻要取消）

錯誤回應目前為純文字訊息，尚未統一成 JSON 格式。




