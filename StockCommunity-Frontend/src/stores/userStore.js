import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";
import { useAuthStore } from "@/stores/authStore";

// 對應後端 UserController (@RequestMapping "/api/users")
export const useUserStore = defineStore("user", () => {
    // State
    const profile = ref(null);
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");

    // Actions

    // 拿自己的個人資料，對應 GET /api/users/me
    const fetchProfile = async () => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/users/me");
            profile.value = res.data;
        } catch (error) {
            console.error("取得個人資料失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得個人資料失敗";
        }
    };

    // 改自己的個人資料（大頭照、電話、帳號名稱），對應 PATCH /api/users/me
    // userPhoto/phone/username 傳 undefined 就不改那個欄位
    // 後端現在回傳格式變成 { user, token }，token 只有改了帳號名稱時才會有
    const updateProfile = async (userPhoto, phone, username) => {
        errorMessage.value = "";
        try {
            const res = await api.patch("/api/users/me", { userPhoto, phone, username });
            profile.value = res.data.user;

            if (res.data.token) {
                const authStore = useAuthStore();
                authStore.updateToken(res.data.token);
                authStore.updateUserInfo({ username: res.data.user.username });
            }

            return true;
        } catch (error) {
            console.error("更新個人資料失敗", error);
            errorMessage.value = error.response?.data || error.message || "更新個人資料失敗";
            return false;
        }
    };

    return { profile, errorMessage, fetchProfile, updateProfile };
});