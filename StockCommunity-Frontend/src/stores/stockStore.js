import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";

// 對應後端 StockController (@RequestMapping "/api/stocks")
// 和 StockPriceController (@RequestMapping "/api/stock-prices")
// 新增 StockSummaryController (@RequestMapping "/api/stock-summary")
// 全部都是公開 GET，不需要登入
export const useStockStore = defineStore("stock", () => {
    // State
    const currentStockCode = ref(""); // 目前要查詢的股票代號
    const stocks = ref([]);          // 全部股票清單，給下拉選單/列表用
    const currentStock = ref(null);  // 目前選中查看的那一檔股票基本資料
    const currentStockAndPrice = ref(null);  // 給 fetchNewestPrice 使用的資料
    const priceHistory = ref([]);    // 目前選中股票的歷史行情
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");
    const industries = ref([]);  // 產業別清單，給篩選下拉選單用

    // Actions

    // 拿全部股票清單，對應 GET /api/stocks
    const fetchAllStocks = async () => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/stocks");
            stocks.value = res.data;
        } catch (error) {
            console.error("取得股票清單失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得股票清單失敗";
        }
    };

    const searchStocks = async (keyword) => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/stocks/search", { params: { keyword } });
            stocks.value = res.data;
        } catch (error) {
            console.error("搜尋股票失敗", error);
            errorMessage.value = error.response?.data || error.message || "搜尋股票失敗";
        }
    };

    // 拿單一股票基本資料，對應 GET /api/stocks/{stockCode}
    const fetchStock = async (stockCode) => {
        errorMessage.value = "";
        try {
            const res = await api.get(`/api/stocks/${stockCode}`);
            currentStock.value = res.data;
        } catch (error) {
            console.error("取得股票資料失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得股票資料失敗";
            currentStock.value = null;
        }
    };

    // 拿某檔股票的「全部」歷史行情，對應 GET /api/stock-prices/{stockCode}
    // 目前 selectStock 已經改用下面的 fetchPriceHistoryByRange，不會再呼叫這支，
    // 保留著是避免之後有其他地方要用「不限範圍、抓全部」的情境
    const fetchPriceHistory = async (stockCode) => {
        errorMessage.value = "";
        try {
            const res = await api.get(`/api/stock-prices/${stockCode}`);
            priceHistory.value = [...res.data].sort(
                (a, b) => new Date(a.tradeDate) - new Date(b.tradeDate)
            );
        } catch (error) {
            console.error("取得歷史行情失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得歷史行情失敗";
            priceHistory.value = [];
        }
    };

    // 拿某檔股票在指定日期區間的歷史行情，對應 GET /api/stock-prices/{stockCode}/range
    const fetchPriceHistoryByRange = async (stockCode, startDate, endDate) => {
        errorMessage.value = "";
        try {
            const res = await api.get(`/api/stock-prices/${stockCode}/range`, {
                params: { startDate, endDate }
            });
            priceHistory.value = [...res.data].sort(
                (a, b) => new Date(a.tradeDate) - new Date(b.tradeDate)
            );
        } catch (error) {
            console.error("取得日期範圍內歷史行情失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得日期範圍內歷史行情失敗";
            priceHistory.value = [];
        }
    };

    // 選定一檔股票：同時拿基本資料，跟「近一個月」的歷史行情
    // 改用 fetchPriceHistoryByRange 而不是抓全部歷史，
    // 避免使用者一選股票，圖表就直接塞進去好幾年份的資料，看起來會很雜亂
    const selectStock = async (stockCode) => {

        currentStockCode.value = stockCode;

        const endDate = new Date();
        const startDate = new Date();

        startDate.setDate(startDate.getDate() - 30);

        const toDateString = (date) => date.toISOString().split("T")[0];

        await Promise.all([
            fetchStock(stockCode),
            fetchPriceHistoryByRange(stockCode, toDateString(startDate), toDateString(endDate)),
        ]);
    };

    // 拿全部產業別，對應 GET /api/industries
    const fetchIndustries = async () => {
        try {
            const res = await api.get("/api/industries");
            industries.value = res.data;
        } catch (error) {
            console.error("取得產業別清單失敗", error);
        }
    };

    // 依產業代號篩選股票，對應 GET /api/stocks?industry=xxx
    const filterStocksByIndustry = async (industryId) => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/stocks", { params: { industry: industryId } });
            stocks.value = res.data;
        } catch (error) {
            console.error("依產業別篩選股票失敗", error);
            errorMessage.value = error.response?.data || error.message || "依產業別篩選股票失敗";
        }
    };

    // 選定一檔股票：獲取最新價格(無成交價為昨日收盤價、有新成交則為成交價)
    const fetchNewestPrice = async (stockCode) => {
        try {
            const res = await api.get(`/api/stock-summary/${stockCode}`);
            currentStockAndPrice.value = res.data;
        } catch (error) {
            console.error("取得股票最新價資料失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得股票最新價資料失敗";
            currentStock.value = null;
            currentStockAndPrice.value = null;
        }
    };



    return {
        stocks,
        currentStock,
        priceHistory,
        errorMessage,
        industries,
        currentStockCode,
        currentStockAndPrice,
        fetchAllStocks,
        searchStocks,
        fetchStock,
        fetchPriceHistory,
        fetchPriceHistoryByRange,
        selectStock,
        fetchIndustries,
        filterStocksByIndustry,
        fetchNewestPrice
    };
});
