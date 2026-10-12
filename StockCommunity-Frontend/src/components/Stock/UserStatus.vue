<template>
  <div class="user-status-card">

    <!-- 帳戶餘額 -->
    <div class="status-item">

      <!-- 第一排：icon + 標題 -->
      <div class="status-header">
        <div class="status-icon">
          <span class="mdi mdi-piggy-bank-outline"></span>
        </div>

        <span class="status-label">
          帳戶餘額
        </span>
      </div>

      <!-- 第二排：餘額 -->
      <span class="status-value cash-value">
        {{ formatNumber(availableCash) }}
      </span>

    </div>


    <!-- 分隔線 -->
    <div class="status-divider"></div>


    <!-- 目前持股 -->
    <div class="status-item">

      <!-- 第一排：icon + 標題 -->
      <div class="status-header">
        <div class="status-icon">
          <span class="mdi mdi-chart-box-outline"></span>
        </div>

        <span class="status-label">
          目前持股
        </span>
      </div>

      <!-- 第二排：持股資訊 -->
      <span
        v-if="holdingStore.holding && holdingStore.holding.quantity > 0"
        class="status-value"
      >
        {{ holdingStore.holding.quantity }}
        <span class="status-unit">股</span>
      </span>

      <span
        v-else
        class="status-value empty-value"
      >
        目前暫無持股資訊
      </span>

    </div>

  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import { useHoldingStore } from '@/stores/holdingStore'
import { useTradingAccountStore } from '@/stores/tradingAccountStore'
import { useStockStore } from "@/stores/stockStore"
import { formatNumber } from '@/utils/format' // 💡 引入工具


const route = useRoute()
const stockStore = useStockStore()
const authStore = useAuthStore()
const holdingStore = useHoldingStore()
const tradingAccountStore = useTradingAccountStore()

const { currentStockCode } = storeToRefs(stockStore)
const { availableCash } = storeToRefs(tradingAccountStore)


onMounted(() => {
  if (!authStore.isLoggedIn) return

  tradingAccountStore.fetchTradingAccount()

  if (currentStockCode.value) {
    holdingStore.fetchMyStockHolding(currentStockCode.value)
  }
})

//  監聽網址上的 query.code 變化
watch(
  () => route.query.code,
  (newCode) => {
    if (newCode) {
      // 1. 同步把網址的代碼填入你的搜尋框（讓 UI 顯示一致）
      currentStockCode.value = newCode

      // 2. 直接呼叫 Pinia 去撈該使用者該股票的持股狀況
      // 持股資訊是個人資料，沒登入就不打，避免被401悄悄彈回登入頁
      if (authStore.isLoggedIn) {
        holdingStore.fetchMyStockHolding(newCode)
      }
    }
  },
  { immediate: true }, // 確保從別頁第一次跳進來時就會觸發撈資料
)
</script>


<style scoped>

/* 帳戶資訊 */

.user-status-card {
  width: 100%;
  height: 100%;
  min-height: 126px;

  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;

  background: #ffffff;
  border-radius: 12px;

  box-sizing: border-box;
}


/* 單一資訊 */

.status-item {
  width: 100%;
  min-width: 0;

  display: flex;
  flex-direction: column;
  justify-content: center;

  box-sizing: border-box;
}


/* Icon + 標題 */

.status-header {
  display: flex;
  align-items: center;

  gap: 12px;
  margin-bottom: 8px;
}


/* Icon */

.status-icon {
  width: 42px;
  height: 42px;

  display: flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;

  background: #fff1dc;
  border-radius: 50%;

  color: #f59e0b;

  font-size: 22px;
}


/* 標題 */

.status-label {
  color: #8b95a5;

  font-size: 13px;
  font-weight: 500;

  white-space: nowrap;
}


/* 金額 / 持股 */

.status-value {
  width: 100%;

  padding-left: 10px;

  color: #10233f;

  font-size: 18px;
  font-weight: 700;

  white-space: nowrap;

  box-sizing: border-box;
}


/* 股 */

.status-unit {
  margin-left: 2px;

  color: #718096;

  font-size: 13px;
  font-weight: 500;
}


/* 沒有持股 */

.empty-value {
  font-size: 15px;
}


/* 分隔線 */

.status-divider {
  width: 1px;

  align-self: stretch;
  margin: 20px 0;

  flex-shrink: 0;

  margin: 0 12px;

  background: #eee9df;
}

</style>
