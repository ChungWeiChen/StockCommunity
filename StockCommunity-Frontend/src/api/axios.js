import axios from "axios";

import router from "@/router/index.js";  // 直接 import router 實例，不是 useRouter() composable

import { serverError } from "@/serverStatus.js";


// 建立 axios 實例，baseURL 之後依 Spring Boot 後端實際位置調整
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
  timeout: 15000, // 原本5000太短，後端冷啟動重算容易誤判逾時，拉長到15秒
});

// 請求攔截器：自動附加 Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 回應攔截器：統一處理錯誤（401 自動登出導回登入頁）
api.interceptors.response.use(
  (response) => {
    // 【新增】只要有任何一次請求成功，代表後端已經恢復連線了，
    // 把serverError重設回false，畫面才有機會自動從「無法連線」復原，
    // 原本這裡只有 (response) => response，沒有做任何事
    serverError.value = false;
    return response;
  },
  (error) => {
    if (!error.response) {
      // ECONNABORTED = 這一次請求逾時，不代表伺服器真的斷線，
      // 不該把整站蓋成錯誤畫面，讓呼叫端自己catch處理（例如產業卡片顯示「取得失敗」）就好
      if (error.code === "ECONNABORTED") {
        console.error("請求逾時：", error.config?.url);
        return Promise.reject(error);
      }

      console.error("無法連線到後端伺服器");
      serverError.value = true;

      return Promise.reject(error);
    }

      switch (error.response.status) {
        case 401:
          localStorage.removeItem("token");
          localStorage.removeItem("user");  // 順手也清掉 user，跟 authStore.logout() 邏輯一致
          // 原本：router.push("/login")，登入完不會回到原本操作到一半的頁面
          router.push({ path: "/login", query: { redirect: router.currentRoute.value.fullPath } });  // SPA 內部導航，不整頁重新載入
          break;
        case 404:
          console.error("找不到資源");
          break;
        case 500:
          console.error("伺服器出錯了");
          break;
        default:
          console.error("發生未知錯誤", error.response.status);
      }
    return Promise.reject(error);
  },
);

export default api;
