<template>
  <div class="order-pad-desktop">

    <div class="order-header">

      <div class="order-title-area">

        <div class="order-icon">
          <span class="mdi mdi-swap-horizontal"></span>
        </div>

        <div>
          <h3>下單配置</h3>
          <p>設定交易條件並送出委託</p>
        </div>

      </div>

    </div>

    <div class="order-grid">
      <!-- 👈 左側欄：條件配置區 -->
      <div class="grid-section">
        <!-- 買賣別 -->
        <div class="form-row">
          <label class="row-label">買賣別</label>
          <div class="btn-group-large">
            <button :class="['btn-buy', { active: orderType === 'BUY' }]" @click="orderType = 'BUY'">
              買進 (BUY)
            </button>
            <button :class="['btn-sell', { active: orderType === 'SELL' }]" @click="orderType = 'SELL'">
              賣出 (SELL)
            </button>
          </div>
        </div>

        <!-- 交易時段 -->
        <div class="form-row">
          <label class="row-label">交易時段</label>
          <div class="btn-group-normal">
            <button :class="{ active: tradingTime === 'DURING_HOURS' }" @click="tradingTime = 'DURING_HOURS'">
              盤中交易
            </button>
            <button :class="{ active: tradingTime === 'AFTER_HOURS' }" @click="tradingTime = 'AFTER_HOURS'">
              盤後固定
            </button>
          </div>
        </div>

        <!-- 下單條件 -->
        <div class="form-row">
          <label class="row-label">下單條件</label>
          <div class="btn-group-normal three-cols">
            <button :class="{ active: tradingType === 'ROD' }" @click="tradingType = 'ROD'">
              ROD (當日有效)
            </button>
            <button :class="{ active: tradingType === 'IOC' }" @click="tradingType = 'IOC'">
              IOC (立即成交否則取消)
            </button>
            <button :class="{ active: tradingType === 'FOK' }" @click="tradingType = 'FOK'">
              FOK (全部成交否則取消)
            </button>
          </div>
        </div>
      </div>

      <!-- 👉 右側欄：數值輸入與送出區 -->
      <div class="grid-section style-border-left">
        <!-- 價格類型 -->
        <div class="form-row">
          <label class="row-label">價格類型</label>
          <div class="btn-group-normal">
            <button :class="{ active: priceType === 'MARKET_ORDER' }" @click="handlePriceTypeChange('MARKET_ORDER')">
              市價委託
            </button>
            <button :class="{ active: priceType === 'LIMIT_ORDER' }" @click="handlePriceTypeChange('LIMIT_ORDER')">
              限價委託
            </button>
          </div>
        </div>

        <!-- 價格輸入框 -->
        <div class="form-row">
          <label class="row-label">委託價格</label>
          <div class="input-container-large">
            <input type="number" step="0.1" v-model="price" :placeholder="priceType === 'MARKET_ORDER' ? '市價委託 (無需填寫價格)' : '請輸入限價金額'
              " :disabled="priceType === 'MARKET_ORDER'" class="input-control" />
            <span class="input-unit" v-if="priceType === 'LIMIT_ORDER'">TWD</span>
          </div>
        </div>

        <!-- 股數輸入框 -->
        <div class="form-row">
          <label class="row-label">委託數量</label>
          <div class="input-container-large">
            <input type="number" v-model.number="quantity" placeholder="請輸入欲交易股數" class="input-control" min="1" />
            <span class="input-unit">股</span>
          </div>
        </div>

        <div>
          <!-- ================= 買入（BUY）區塊 ================= -->
          <div v-if="orderType === 'BUY'" class="order-preview-box">
            <!-- 買入 + 限價 -->
            <span v-if="priceType === 'LIMIT_ORDER' && price" :class="{ 'text-red': totalPriceState.isError }">
              <template v-if="totalPriceState.isError">{{ totalPriceState.text }}</template>
              <template v-else>
                以限價 {{ formatNumber(price) }}買入 {{ quantity }} 股
                <br />
                (預估自帳戶扣除：{{ totalPriceState.text }})
              </template>
            </span>

            <!-- 買入 + 市價 -->
            <span v-if="priceType === 'MARKET_ORDER'" :class="{ 'text-red': totalPriceState.isError }">
              <template v-if="totalPriceState.isError">{{ totalPriceState.text }}</template>
              <template v-else>
                以市價（預估 {{ totalPriceState.text }}）買入 {{ quantity }} 股
              </template>
            </span>
          </div>

          <!-- ================= 賣出（SELL）區塊 ================= -->
          <div v-else-if="orderType === 'SELL'" class="order-preview-box">
            <!-- 賣出 + 限價 -->
            <span v-if="priceType === 'LIMIT_ORDER' && price" :class="{ 'text-red': totalPriceState.isError }">
              <template v-if="totalPriceState.isError">{{ totalPriceState.text }}</template>
              <template v-else>
                以限價 {{ formatNumber(price) }} 元賣出 {{ quantity }} 股
                <br />
                (預估款項匯入：{{ totalPriceState.text }})
              </template>
            </span>

            <!-- 賣出 + 市價 -->
            <span v-if="priceType === 'MARKET_ORDER'" :class="{ 'text-red': totalPriceState.isError }">
              <template v-if="totalPriceState.isError">{{ totalPriceState.text }}</template>
              <template v-else>
                以市價（預估 {{ totalPriceState.text }}）賣出 {{ quantity }} 股
              </template>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 🚀 底部大按鈕：一目了然的確定送出區 -->
    <div class="submit-section">
      <button :class="['btn-submit-desktop', orderType === 'BUY' ? 'submit-buy' : 'submit-sell']" @click="handleSubmit">
        <span class="btn-orderType-text">{{
          orderType === 'BUY' ? '確認送出買進委託' : '確認送出賣出委託'
          }}</span>

      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useOrderStore } from '@/stores/orderStore'
import { useStockStore } from '@/stores/stockStore'
import { useHoldingStore } from '@/stores/holdingStore'
import { useMarketDepthStore } from '@/stores/marketDepthStore'
import { useAuthStore } from '@/stores/authStore'
import { useTradingAccountStore } from '@/stores/tradingAccountStore'
import { formatNumber } from '@/utils/format' // 💡 引入工具

const router = useRouter()
const markerDepthStore = useMarketDepthStore()
const orderStore = useOrderStore()
const stockStore = useStockStore()
const holdingStore = useHoldingStore()
const tradingAccountStore = useTradingAccountStore()
const authStore = useAuthStore()

const { currentStockCode } = storeToRefs(stockStore)
const { availableCash } = storeToRefs(tradingAccountStore)
const { holding } = storeToRefs(holdingStore)


const orderType = ref('BUY') // 預設買進，對應後端 OrderType enum 的值
const quantity = ref(0)
const price = ref('')
const priceType = ref('LIMIT_ORDER')
const tradingType = ref('ROD') // ROD 當日有效 / IOC 立即成交否則取消 / FOK 全部成交否則取消
const tradingTime = ref('DURING_HOURS') // DURING_HOURS 盤中 / AFTER_HOURS 盤後


const totalPriceState = computed(() => {
  const q = Number(quantity.value) || 0

  // 1. 根據單別決定使用的單價
  let currentPrice = 0
  if (priceType.value === 'LIMIT_ORDER') {
    currentPrice = Number(price.value) || 0
  } else if (priceType.value === 'MARKET_ORDER') {
    currentPrice = Number(protectionPrice.value) || 0
  }

  const total = currentPrice * q

  // 追加避免股數或價格自己填負數
  const isQuantityNegative = q < 0
  const isPriceNegative = currentPrice < 0

  if (isQuantityNegative && isPriceNegative) {
    return {
      isError: true,
      text: '股數與價格不可為負數',
      value: total
    }
  }

  if (isQuantityNegative) {
    return {
      isError: true,
      text: '股數不可為負數',
      value: total
    }
  }

  if (isPriceNegative) {
    return {
      isError: true,
      text: '價格不可為負數',
      value: total
    }
  }


  // 2. 根據 買/賣 進行不同的資產檢驗
  if (q > 0) {
    if (orderType.value === 'BUY') {
      // 【買進】檢查：總價是否大於可用餘額
      if (total > Number(availableCash.value)) {
        return { isError: true, text: '餘額不足', value: total }
      }
    } else if (orderType.value === 'SELL') {
      // 【賣出】檢查：輸入股數是否大於目前持有的股票數量
      const currentHoldingQty = Number(holding.value?.quantity) || 0
      if (q > currentHoldingQty) {
        return { isError: true, text: '庫存股數不足', value: total }
      }
    }
  }

  // 3. 正常回傳格式化後的千分位字串
  return { isError: false, text: `${total.toLocaleString()} 元`, value: total }
})

const protectionPrice = computed(() => {
  // 買進市價單：吃對面的賣盤第一檔最優價 sellDepth[0]
  if (orderType.value === 'BUY') {
    const bestSell = markerDepthStore.sellDepth[0];
    // 根據你陣列裡放的是物件還是單純數字調整，這裡假設是物件型態 { price: xxx }
    // 如果你的陣列直接存數字，請改成: return bestSell || 0;
    return bestSell ? bestSell.price : 0;
  }

  // 賣出市價單：吃對面的買盤第一檔最優價 buyDepth[0]
  if (orderType.value === 'SELL') {
    const bestBuy = markerDepthStore.buyDepth[0];
    return bestBuy ? bestBuy.price : 0;
  }

  return 0;
});

const handlePriceTypeChange = (type) => {
  priceType.value = type
  if (type === 'MARKET_ORDER') {
    price.value = ''
  }
}

const handleSubmit = async () => {
  // 五檔下單頁本身不擋瀏覽，但真正送出委託這個動作要登入才能做，
  // 沒登入就跳跟社群那邊(CreatePost/OnePost/OneComment)一致的alert，帶redirect導去登入頁
  if (!authStore.isLoggedIn) {
    alert('您還沒有登入會員，請先登入會員。')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  const isLimitOrder = priceType.value === 'LIMIT_ORDER'

  const quantityNum = Number(quantity.value)
  // 市價單直接傳 null，對應後端 OrderService 會把市價單的 price 存成 null 的邏輯
  const priceNum = isLimitOrder ? Number(price.value) : null

  if (priceType.value === 'LIMIT_ORDER' && !price.value) {
    alert('請輸入委託限價！')
    return
  }
  if (!quantity.value || quantity.value <= 0) {
    alert('請輸入正確的委託股數！')
    return
  }

  const success = await orderStore.createOrder(
    currentStockCode.value,
    orderType.value,
    quantityNum,
    priceNum,
    protectionPrice.value,
    priceType.value,
    tradingType.value,
    tradingTime.value,
  )

  if (success) {
    // 送出成功才清空表單，失敗的話讓使用者留著原本打的內容修改重送
    quantity.value = ''
    price.value = ''

    console.log('=== [電腦版送出單] ===')

    alert(`訂單送出成功：${orderType.value === 'BUY' ? '買進' : '賣出'} ${quantityNum} 股`)

    holdingStore.fetchMyStockHolding(currentStockCode.value)

    // 下單成功後，主動去通知 store 重新拉取最新的五檔與訂單
    markerDepthStore.fetchMarketDepth(currentStockCode.value)

    tradingAccountStore.fetchTradingAccount()
  } else {
    // 原本沒有失敗分支，送出失敗時畫面完全沒反應，看起來像當機
    // 把後端回傳的真正原因(例如餘額不足)顯示出來
    alert(`訂單送出失敗：${orderStore.errorMessage}`)
  }
}

// 💡 5. 提供一個全域/跨元件能監聽的方法，讓五檔點擊可以直接改這裡的價格
// 為了讓五檔元件（或其他元件）可以改到這裡的 price，我們將此方法掛在 window 物件或透過全域事件。
// （註：這是在不想動 Store 架構下的最速做法）
window.__updateOrderPadPrice = (clickedPrice, suggestedOrderType) => {
  priceType.value = 'LIMIT_ORDER' // 點擊五檔一律切換為限價
  price.value = clickedPrice

  if (suggestedOrderType) {
    orderType.value = suggestedOrderType // 💡 自動切換買進或賣出！
  }
}
</script>

<style scoped>
/* 下單面板 */

.order-pad-desktop {
  width: 100%;
  height: 100%;

  display: flex;
  flex-direction: column;

  background: #ffffff;

  box-sizing: border-box;
}


/* 標題 */

.order-header {
  display: flex;
  align-items: center;

  margin-bottom: 22px;
}

.order-title-area {
  display: flex;
  align-items: center;

  gap: 11px;
}

.order-icon {
  width: 42px;
  height: 42px;

  display: flex;
  align-items: center;
  justify-content: center;

  background: #fff1dc;

  border-radius: 50%;

  color: #f59e0b;

  font-size: 22px;
}

.order-title-area h3 {
  margin: 0;

  color: #10233f;

  font-size: 19px;
  font-weight: 700;
}

.order-title-area p {
  margin: 2px 0 0;

  color: #8b95a5;

  font-size: 13px;
}


/* 兩欄配置 */

.order-grid {
  flex: 1;

  display: grid;
  grid-template-columns: 1fr 1fr;

  gap: 28px;
}

.grid-section {
  display: flex;
  flex-direction: column;

  gap: 20px;
}

.style-border-left {
  padding-left: 28px;

  border-left: 1px solid #eee9df;
}


/* 表單 */

.form-row {
  display: flex;
  flex-direction: column;

  gap: 8px;
}

.row-label {
  color: #475569;

  font-size: 14px;
  font-weight: 600;
}


/* 買進 / 賣出 */

.btn-group-large {
  display: flex;

  gap: 5px;

  padding: 4px;

  background: #fbfaff;

  border: 1px solid #eeeaf5;
  border-radius: 8px;
}

.btn-group-large button {
  flex: 1;

  padding: 12px 16px;

  background: transparent;

  border: none;
  border-radius: 6px;

  color: #718096;

  font-size: 14px;
  font-weight: 600;

  cursor: pointer;

  transition: 0.2s;
}


/* 未選中的按鈕：滑鼠移入時顯示淺紫色 */

.btn-group-large button:hover:not(.active) {
  background: #f1edff;
}


/* 買進 */

.btn-group-large button.btn-buy.active {
  background: #ef4444;

  color: #ffffff;

  box-shadow: none;
}


/* 賣出 */

.btn-group-large button.btn-sell.active {
  background: #22c55e;

  color: #ffffff;

  box-shadow: none;
}


/* 一般選項 */

.btn-group-normal {
  display: flex;

  gap: 4px;

  padding: 4px;

  background: #fbfaff;

  border: 1px solid #eeeaf5;
  border-radius: 8px;
}

.btn-group-normal button {
  flex: 1;

  padding: 10px 12px;

  background: transparent;

  border: none;
  border-radius: 6px;

  color: #718096;

  font-size: 13px;
  font-weight: 500;

  cursor: pointer;

  transition: 0.2s;
}


/* 未選中的按鈕 Hover → 淺紫 */

.btn-group-normal button:hover:not(.active) {
  background: #f1edff;
}


/* 選中 → 淡橘 */

.btn-group-normal button.active {
  background: #fff3dc;

  color: #d98200;

  font-weight: 600;

  box-shadow: none;
}


/* ROD / IOC / FOK */

.three-cols button {
  padding-left: 8px;
  padding-right: 8px;

  font-size: 12px;
}


/* Input*/

.input-container-large {
  position: relative;

  display: flex;
  align-items: center;
}

.input-control {
  width: 100%;

  padding: 12px 58px 12px 14px;

  background: #fbfaff;

  border: 1px solid #ddd8e8;
  border-radius: 8px;

  outline: none;

  color: #10233f;

  font-size: 15px;
  font-weight: 600;

  box-sizing: border-box;

  transition: 0.2s;
}

.input-control:hover:not(:disabled) {
  background: #f8f6ff;

  border-color: #cfc7df;
}

.input-control:focus:not(:disabled) {
  background: #ffffff;

  border-color: #f0b84d;

  box-shadow: 0 0 0 3px rgba(240, 184, 77, 0.12);
}

.input-control:disabled {
  background: #f7f7f8;

  border-color: #e5e7eb;

  color: #a5adba;

  cursor: not-allowed;
}

.input-unit {
  position: absolute;

  right: 14px;

  color: #8b95a5;

  font-size: 13px;
  font-weight: 600;
}


/* 送出區 */

.submit-section {
  margin-top: 28px;
  padding-top: 18px;

  border-top: 1px solid #eee9df;
}

.btn-submit-desktop {
  width: 100%;
  min-height: 58px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  gap: 3px;

  padding: 12px 16px;

  border: none;
  border-radius: 8px;

  cursor: pointer;

  transition: 0.2s;
}

.btn-orderType-text {
  color: #ffffff;

  font-size: 16px;
  font-weight: 700;
}

.btn-sub-text {
  color: rgba(255, 255, 255, 0.85);

  font-size: 12px;
}


/* 買進送出 */

.submit-buy {
  background: #ef4444;
}

.submit-buy:hover {
  background: #dc3d3d;
}


/* 賣出送出 */

.submit-sell {
  background: #22c55e;
}

.submit-sell:hover {
  background: #1eaf53;
}
</style>
