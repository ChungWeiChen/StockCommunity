<template>
  <!-- 畫面一：當有持股資訊時顯示 (數量大於 0) -->
  <div
    v-if="holdingStore.holdings && holdingStore.holdings.quantity > 0"
    class="stock-holding-bar d-flex justify-center align-center w-100"
  >
    <span>
      當前持股數:
      <span class="text-grey-darken-1">
        {{ holdingStore.holdings.quantity }}
      </span>
    </span>
  </div>

  <!-- 畫面二：當沒有持股資訊時顯示 -->
  <div v-else class="stock-holding-bar d-flex justify-center align-center w-100">
    <span>目前暫無持股資訊</span>
  </div>
</template>

<script setup>
import { watch } from "vue"; //  引入 watch
import { useRoute } from "vue-router"; //  引入用來讀取路由的 useRoute
import { storeToRefs } from "pinia";
import { useStockStore } from "@/stores/stockStore"
import { useHoldingStore } from "@/stores/holdingStore";
import { useAuthStore } from "@/stores/authStore";

const stockStore = useStockStore()
const holdingStore = useHoldingStore();
const authStore = useAuthStore();
const route = useRoute(); // 取得當前路由物件

const { currentStockCode } = storeToRefs(stockStore)


//  監聽網址上的 query.code 變化
watch(
  () => route.query.code,
  (newCode) => {
    if (newCode) {
      // 1. 同步把網址的代碼填入你的搜尋框（讓 UI 顯示一致）
      currentStockCode.value = newCode;

      // 2. 直接呼叫 Pinia 去撈該使用者該股票的持股狀況
      // 持股資訊是個人資料，沒登入就不打，避免被401悄悄彈回登入頁
      if (authStore.isLoggedIn) {
        holdingStore.fetchMyStockHolding(newCode);
      }
    }
  },
  { immediate: true } // 確保從別頁第一次跳進來時就會觸發撈資料
);



</script>


<style scoped>
.stock-holding-bar {
  border: 2px solid #000000;
  border-radius: 6px;
  overflow: hidden;
  min-height: 50px; /* 建議設定一個基礎高度，上下置中的效果會更明顯 */
}
</style>
