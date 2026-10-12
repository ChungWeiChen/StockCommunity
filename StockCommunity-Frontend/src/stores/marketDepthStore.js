import { defineStore } from "pinia";
import { ref, computed } from "vue";

import api from "@/api/axios";

// 對應後端 MarketDepthController (@RequestMapping "/api/market")
export const useMarketDepthStore = defineStore("depth", () => {
  // State
  const buyDepth = ref([]);        // 買盤五檔/十檔資料
  const sellDepth = ref([]);       // 賣盤五檔/十檔資料
  const depthTimestamp = ref(null);// 盤口資料更新時間點
  const errorMessage = ref("");

  // 新增：計算最佳買價 (Bid) 與最佳賣價 (Ask) 及價差
  const bestBid = computed(() => buyDepth.value[0]?.price || 0);
  const bestAsk = computed(() => sellDepth.value[0]?.price || 0);

  const depthSpread = computed(() => {
    const spread = bestAsk.value - bestBid.value;
    return spread > 0 ? spread : 0;
  });

  /**
   * 接收後端深度資料並直接寫入 State
   * @param {Object} rawData 後端傳來的 depth 物件
   */
  const updateMarketDepth = (rawData) => {
    // 確保後端沒傳時有空陣列防呆
    buyDepth.value = rawData.buyDepthEntry || [];
    sellDepth.value = rawData.sellDepthEntry || [];
    depthTimestamp.value = rawData.timestamp;
  };


  //  修改後的 Action
  const fetchMarketDepth = async (stockCode) => {
    // 防呆：如果目前根本沒有股票代號，就不發請求
    if (!stockCode) return;

    errorMessage.value = "";
    try {
      // 💡 直接帶入內部狀態 currentStockCode.value
      const res = await api.get("/api/market/depth", {
        params: {
          stockCode: stockCode // Axios 會自動拼成 /api/market/depth?stockCode=xxx
        }
      })
      updateMarketDepth(res.data || res);
    } catch (err) {
      errorMessage.value = err.message || "無法取得盤口深度資料";
    }
  };

  const formattedPriceList = computed(() => {
    // 1. 處理賣盤 (Asks) -> 來源應為 sellDepth
    // 提示：如果希望畫面上賣盤最靠近中間的是最低賣價，可以視後端排序決定是否加上 .reverse()
    const asks = sellDepth.value.map(item => ({
      price: item.price,
      quantity: item.totalQuantity,
      type: 'sell'
    }));

    // 2. 處理買盤 (Bids) -> 來源應為 buyDepth
    const bids = buyDepth.value.map(item => ({
      price: item.price,
      quantity: item.totalQuantity,
      type: 'buy'
    }));

    // 根據你的 UI 需求，你可以選擇合併成一個陣列，或者分開。
    // 如果你的五檔是賣盤在上、買盤在下連在一起，可以這樣合併：
    return [...asks, ...bids];
  });

  return {
    // 新增的深度盤口資料
    buyDepth,
    sellDepth,
    depthTimestamp,
    bestBid,
    bestAsk,
    depthSpread,
    updateMarketDepth,
    formattedPriceList, // 導出這個給父組件 v-for 使用
    fetchMarketDepth
  };

});

