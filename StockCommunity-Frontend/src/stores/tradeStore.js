import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";

// 對應後端 TradeController (@RequestMapping "/api/trades")
// 注意：沒有 create action，因為後端故意不開放 POST
// 成交紀錄只能由撮合引擎內部產生，前端只能查詢，不能自己新增/偽造
export const useTradeStore = defineStore("trade", () => {
    // State
    const trades = ref([]);
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");

    // Actions

    // 拿我自己的所有成交紀錄，對應 GET /api/trades/mine
    const fetchMyTrades = async () => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/trades/mine");
            trades.value = res.data;
        } catch (error) {
            console.error("取得成交紀錄失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得成交紀錄失敗";
        }
    };

    return { trades, errorMessage, fetchMyTrades };
});