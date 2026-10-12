import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "@/api/axios";

// 對應後端 AuthController (@RequestMapping "/api/auth")
export const useAuthStore = defineStore("auth", () => {
  // State
  const token = ref(localStorage.getItem("token") || "");
  const user = ref(JSON.parse(localStorage.getItem("user")) || null);
  // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因，不用只猜一個通用文字
  const errorMessage = ref("");

  // Getters
  const isLoggedIn = computed(() => !!token.value);

  // Actions
  const login = async (email, password) => {
    errorMessage.value = "";
    try {
      // 後端回傳格式: { token, username, userId }
      // 登入驗證改成用信箱比對，不再用帳號名稱，所以這裡的 request body 欄位
      // 從 { username, password } 改成 { email, password }，要跟 LoginRequest.java 對齊
      // userPhoto改成byte[]存binary之後，登入回應不再帶userPhoto了(避免每次登入都要搬一次整張照片、
      // 還存進localStorage)，要顯示大頭照的地方改成打GET /api/users/me現抓
      const res = await api.post("/api/auth/login", { email, password });

      token.value = res.data.token;
      user.value = {
        username: res.data.username,
        userId: res.data.userId,
      };

      localStorage.setItem("token", token.value);
      localStorage.setItem("user", JSON.stringify(user.value));

      return true;
    } catch (error) {
      console.error("登入失敗", error);
      // 後端目前是回傳純文字（例如「信箱或密碼錯誤」），沒有的話 fallback 用 error.message
      errorMessage.value = error.response?.data || error.message || "登入失敗";
      return false;
    }
  };

  // 對應後端 POST /api/auth/register，body: { username, password, email }
  // username 可以留空（後端會拿 email 自動產生帳號）；如果自動產生的名字剛好撞名，
  // 後端會回 409 附上 suggestedUsername，不算真的失敗，回傳出去讓畫面把建議名稱帶進輸入框
  const register = async (username, password, email) => {
    errorMessage.value = "";
    try {
      await api.post("/api/auth/register", { username, password, email });
      return { success: true };
    } catch (error) {
      console.error("註冊失敗", error);
      if (error.response?.status === 409 && error.response?.data?.suggestedUsername) {
        errorMessage.value = error.response.data.message || "預設帳號名稱已被使用，請確認或修改以下建議的帳號名稱";
        return { success: false, suggestedUsername: error.response.data.suggestedUsername };
      }
      errorMessage.value = error.response?.data || error.message || "註冊失敗";
      return { success: false };
    }
  };

  // 忘記密碼，對應 POST /api/auth/forgot-password
  // 不管這個 email 有沒有註冊過，後端都回一樣的成功訊息（安全考量，避免被拿來試哪些 email 存在）
  const forgotPassword = async (email) => {
    errorMessage.value = "";
    try {
      const res = await api.post("/api/auth/forgot-password", { email });
      return { success: true, message: res.data };
    } catch (error) {
      console.error("忘記密碼請求失敗", error);
      errorMessage.value = error.response?.data || error.message || "忘記密碼請求失敗";
      return { success: false, message: errorMessage.value };
    }
  };

  // 用信件裡的 token 設定新密碼，對應 POST /api/auth/reset-password
  const resetPassword = async (token, newPassword) => {
    errorMessage.value = "";
    try {
      const res = await api.post("/api/auth/reset-password", { token, newPassword });
      return { success: true, message: res.data };
    } catch (error) {
      console.error("重設密碼失敗", error);
      errorMessage.value = error.response?.data || error.message || "重設密碼失敗";
      return { success: false, message: errorMessage.value };
    }
  };

  // 把個人資料頁改過的欄位（例如帳號名稱）同步進這裡的 user 快照，
  // 讓導覽列這種讀 authStore.user 的地方不用等重新登入就能看到最新值
  const updateUserInfo = (partial) => {
    if (!user.value) return;
    user.value = { ...user.value, ...partial };
    localStorage.setItem("user", JSON.stringify(user.value));
  };

  // 帳號名稱改掉之後，JWT 舊 token 跟資料庫對不上了，
  // 後端 PATCH /api/users/me 會多回一組新 token，這裡負責把它換掉
  const updateToken = (newToken) => {
    if (!newToken) return;
    token.value = newToken;
    localStorage.setItem("token", newToken);
  };

  const logout = () => {
    token.value = "";
    user.value = null;
    errorMessage.value = "";
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  };

  return { token, user, isLoggedIn, errorMessage, login, register, forgotPassword, resetPassword, updateUserInfo, updateToken, logout };
});