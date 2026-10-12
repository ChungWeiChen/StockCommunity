import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/api/axios";

// 對應後端 OrderController (@RequestMapping "/api/orders")
export const useOrderStore = defineStore("order", () => {
    // State
    const orders = ref([]);
    // 存後端實際回傳的錯誤訊息，讓畫面能顯示真正的失敗原因
    const errorMessage = ref("");

    // Actions

    // 拿我自己的所有訂單，對應 GET /api/orders/mine
    const fetchMyOrders = async () => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/orders/mine");
            orders.value = res.data;
        } catch (error) {
            console.error("取得訂單失敗", error);
            errorMessage.value = error.response?.data || error.message || "取得訂單失敗";
        }
    };
    // 新增：依日期區間取得委託
    const fetchMyOrdersByDateRange = async (startDate, endDate) => {
        errorMessage.value = "";
        try {
            const res = await api.get("/api/orders/mine/range", {
                params: { startDate, endDate },
            });
            orders.value = res.data;
        } catch (error) {
            console.error("依日期取得訂單失敗", error);
            errorMessage.value =
                error.response?.data || error.message || "依日期取得訂單失敗";
        }
    };
    // 建立新訂單，對應 POST /api/orders
    // orderType 只能是 "BUY" 或 "SELL"（對應後端 OrderType enum）
    // priceType/tradingType/tradingTime 後端都是 @Column(nullable = false)，
    // 少傳任何一個 OrderService.createOrder 會直接丟 400，所以這裡全部設為必填參數
    // wantBuyPrice 只有 priceType === "LIMIT_ORDER" 時才需要，市價單傳 null 即可（後端也會自己把市價單的 wantBuyPrice 存成 null）
    // 欄位名稱對應後端 CreateOrderRequest.wantBuyPrice（原本叫 price，已改名對齊 merge_notes 草稿）
    const createOrder = async (stockCode, orderType, quantity, wantBuyPrice, protectionPrice, priceType, tradingType, tradingTime) => {
        errorMessage.value = "";
        try {
            const res = await api.post("/api/orders", {
                stockCode,
                orderType,
                quantity,
                wantBuyPrice,
                protectionPrice,
                priceType,
                tradingType,
                tradingTime,
            });
            orders.value.unshift(res.data);  // 新訂單放最前面，最新的先看到
            return true;
        } catch (error) {
            console.error("建立訂單失敗", error);
            errorMessage.value = error.response?.data || error.message || "建立訂單失敗";
            return false;
        }
    };

    // 取消訂單，對應 PATCH /api/orders/{orderId}/cancel
    // 後端只允許 PENDING / PARTIALLY_FILLED 狀態的訂單被取消，其餘情況會回 400/403/409
    const cancelOrder = async (orderId) => {
        errorMessage.value = "";
        try {
            const res = await api.patch(`/api/orders/${orderId}/cancel`);
            // 直接更新本地陣列裡對應那筆訂單的狀態，不用整個重新打 API
            const index = orders.value.findIndex((o) => o.id === orderId);
            if (index !== -1) {
                orders.value[index] = res.data;
            }
            return true;
        } catch (error) {
            console.error("取消訂單失敗", error);
            errorMessage.value = error.response?.data || error.message || "取消訂單失敗";
            return false;
        }
    };

    return { orders, errorMessage, fetchMyOrders, createOrder, cancelOrder, fetchMyOrdersByDateRange };
});
