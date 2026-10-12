import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";

// 對應後端 WatchlistController (@RequestMapping "/api/watchlists")
export const useWatchlistStore = defineStore("watchlist", () => {
    // State
    const watchlists = ref([]);  // 目前登入者擁有的所有清單（例如：我的定存 ETF、觀察名單...）
    const currentItems = ref([]);  // 目前正在查看的「某一個」清單裡面的股票項目
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");

    // Actions

    // 拿「我自己」建立過的所有清單，對應 GET /api/watchlists/mine
    // 後端會從 JWT 解出目前登入者身份，不用也不能自己傳 userId
    const fetchMyWatchlists = async () => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/watchlists/mine");
            watchlists.value = res.data;
        } catch (error) {
            console.error("取得清單失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得清單失敗";
        }
    };

    // 建立一張新清單，對應 POST /api/watchlists，body: { name }
    // 成功後直接把新清單塞進 watchlists 陣列，不用重新整頁再打一次 fetchMyWatchlists
    const createWatchlist = async (name) => {
        errorMessage.value = "";
        try {
            const res = await api.post("/api/watchlists", { name });
            watchlists.value.push(res.data);
            return true;
        } catch (error) {
            console.error("建立清單失敗", error);
            errorMessage.value = error.response?.data || error.message || "建立清單失敗";
            return false;
        }
    };

    // 查某一張清單裡面有哪些股票，對應 GET /api/watchlists/{watchlistId}/items
    // 後端會檢查這張清單是不是自己的，不是的話會回 403
    const fetchWatchlistItems = async (watchlistId) => {
        errorMessage.value = "";
        try {
            const res = await api.get(`/api/watchlists/${watchlistId}/items`);
            currentItems.value = res.data;
        } catch (error) {
            console.error("取得清單項目失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得清單項目失敗";
        }
    };

    // 把一檔股票加進指定清單，對應 POST /api/watchlists/{watchlistId}/items，body: { stockCode }
    const addItem = async (watchlistId, stockCode) => {
        errorMessage.value = "";
        try {
            const res = await api.post(`/api/watchlists/${watchlistId}/items`, {
                stockCode,
            });
            currentItems.value.push(res.data);
            return true;
        } catch (error) {
            console.error("加入股票失敗", error);
            errorMessage.value = error.response?.data || error.message || "加入股票失敗";
            return false;
        }
    };

    // 從清單移除一個項目，對應 DELETE /api/watchlists/items/{itemId}
    // 成功後直接在前端陣列裡過濾掉該筆，畫面立刻反應，不用重新打一次 API
    const removeItem = async (itemId) => {
        errorMessage.value = "";
        try {
            await api.delete(`/api/watchlists/items/${itemId}`);
            currentItems.value = currentItems.value.filter((item) => item.id !== itemId);
            return true;
        } catch (error) {
            console.error("移除股票失敗", error);
            errorMessage.value = error.response?.data || error.message || "移除股票失敗";
            return false;
        }
    };

    return {
        watchlists,
        currentItems,
        errorMessage,
        fetchMyWatchlists,
        createWatchlist,
        fetchWatchlistItems,
        addItem,
        removeItem,
    };
});
