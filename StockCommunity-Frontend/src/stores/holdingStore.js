import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";

// 對應後端 HoldingController (@RequestMapping "/api/holdings")
// 注意：沒有 create/cancel action，因為後端故意不開放 POST
// 持股是成交後系統自動算出來的結果，前端只能查詢，不能自己新增/修改
export const useHoldingStore = defineStore("holding", () => {
    // State
    const holdings = ref([]);
    const holding = ref("");
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");

    // Actions

    // 拿我自己的所有持股（投資組合），對應 GET /api/holdings/mine
const fetchMyHoldings = async () => {
    errorMessage.value = "";

    try {
        const res = await api.get("/api/holdings/mine");

        holdings.value = await Promise.all(
            res.data.map(async (holding) => {
                try {
                    // 找出指定stock_code 的 價格資料
                    const priceRes = await api.get(
                        `/api/stock-prices/${holding.stockCode}`
                    );

                    // API 已依 tradeDate ASC 排序，最後一筆即最新行情
                    const latestStockPrice = priceRes.data.at(-1);

                    return {
                        ...holding,
                        latestClosePrice: latestStockPrice?.closePrice ?? null
                    };
                } catch (error) {
                    console.error(
                        `取得 ${holding.stockCode} 最新收盤價失敗`,
                        error
                    );

                    return {
                        ...holding,
                        latestClosePrice: null
                    };
                }
            })
        );
    } catch (error) {
        console.error("取得持股失敗", error);
        errorMessage.value =
            error.response?.data ||
            error.message ||
            "取得持股失敗";
    }
};

    // 拿我特定股票的持股，對應 GET /api/holdings/mine/{stockCode}
    const fetchMyStockHolding = async (stockCode) => {
        errorMessage.value = "";
        try {
            const res = await api.get(`/api/holdings/mine/${stockCode}`);
            holding.value = res.data;
        } catch (error) {
            console.error("取得持股失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得持股失敗";
        }
    };


    return { holdings, holding, errorMessage, fetchMyHoldings, fetchMyStockHolding };
});
