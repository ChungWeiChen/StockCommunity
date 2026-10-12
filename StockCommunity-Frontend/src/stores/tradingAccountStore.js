import { defineStore } from "pinia";

// import { ref } from "vue";
// import api from "@/api/axios";

// export const useTradingAccountStore = defineStore(
//     "tradingAccount",
//     () => {
//         const account = ref(null);
//         const errorMessage = ref("");

//         const fetchMyAccount = async () => {
//             errorMessage.value = "";

//             try {
//                 const response = await api.get(
//                     "/api/trading-accounts/me"
//                 );

//                 account.value = response.data;
//             } catch (error) {
//                 console.error("取得模擬交易帳戶失敗", error);

//                 errorMessage.value =
//                     error.response?.data ||
//                     error.message ||
//                     "取得模擬交易帳戶失敗";
//             }
//         };

//         return {
//             account,
//             errorMessage,
//             fetchMyAccount
//         };
//     }
// );

import { ref, computed } from "vue";

import api from "@/api/axios";
import { useAuthStore } from "@/stores/authStore";

// 對應後端 TradingAccountController (@RequestMapping "/api/trading-accounts")
export const useTradingAccountStore = defineStore("account", () => {
    // State
    const availableCash = ref(0)
    const initialCash = ref(0)
    const frozenCash = ref(0)
    const updatedAt = ref('')

    const errorMessage = ref("");

    const updateAccount = (res) => {
        // 透過這行，就能直接將 res 裡面的對應欄位單獨拔出來，並賦值給 ref
        ({
            initialCash: initialCash.value,
            availableCash: availableCash.value,
            frozenCash: frozenCash.value,
            updatedAt: updatedAt.value
        } = res)
    }

    const fetchTradingAccount = async () => {

        errorMessage.value = "";
        try {
            // 💡 直接帶入內部狀態 currentStockCode.value
            const res = await api.get("/api/trading-accounts/me")
            updateAccount(res.data)
        } catch (error) {
            console.error("帳號資產獲取失敗", error);
            errorMessage.value = error.response?.data || error.message || "帳號資產獲取失敗";
        }
    };

    // 檢查最新一筆流水帳是不是「賽季重置」，是的話跳一次提示
    // 不加後端欄位，直接重用 /api/trading-accounts/me/ledger 的第一筆(最新)紀錄來判斷：
    // - 重置後使用者還沒有其他操作 → 最新一筆一定是 SEASON_RESET → 該跳提示
    // - 重置後使用者已經有交易 → 最新一筆變成別的類型 → 代表他早就知道重置的事了，不用再跳
    // 用 localStorage 記住「這個使用者已經看過的那筆重置紀錄id」，同一筆重置不會重複跳提示
    // 跳提示方式跟專案裡其他地方一致，直接用原生 alert()，跳出「確定」框，按了才消失
    const checkSeasonReset = async () => {
        try {
            const res = await api.get("/api/trading-accounts/me/ledger", {
                params: { page: 0, size: 1 },
            });
            const latest = res.data?.content?.[0];
            if (!latest || latest.type !== "SEASON_RESET") return;

            const authStore = useAuthStore();
            const userId = authStore.user?.userId;
            if (!userId) return;

            // key要帶userId，避免同一台電腦換帳號登入時互相干擾
            const storageKey = `seasonResetSeenId_${userId}`;
            const seenId = localStorage.getItem(storageKey);

            // 用字串比較，localStorage存的一定是字串，latest.id是number
            if (String(latest.id) !== seenId) {
                // alert() 會凍結整個畫面的渲染，如果太早跳出來，
                // 頁面上其他元件（導覽列、側邊欄）可能還沒畫完，看起來像「按鈕不見了」
                // 延遲一點點再跳，讓頁面先正常畫完，背景才會是完整畫面
                setTimeout(() => {
                    alert("你的模擬帳戶已進入新賽季，資金已重新回到100萬");
                }, 500);
                localStorage.setItem(storageKey, String(latest.id));
            }
        } catch (err) {
            console.error("檢查賽季重置狀態失敗", err);
        }
    };

    return {
        availableCash,
        initialCash,
        frozenCash,
        updatedAt,
        fetchTradingAccount,
        checkSeasonReset,
    };

});

