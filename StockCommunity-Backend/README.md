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




