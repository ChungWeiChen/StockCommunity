<template>
  <section class="order-record-page">

    <!-- 頁面標題 -->
    <div class="page-header">
      <h2>委託紀錄</h2>
      <p>查看您的歷史委託與成交狀況</p>
    </div>


    <!-- 未登入 -->
    <v-alert v-if="!authStore.isLoggedIn" type="info" variant="tonal" class="mb-4">
      請先登入會員查看您的委託紀錄

      <div class="mt-2">
        <v-btn size="small" color="primary" @click="goToLogin">
          前往登入
        </v-btn>
      </div>
    </v-alert>


    <template v-if="authStore.isLoggedIn">

      <!-- 查詢區 -->
      <div class="record-card search-card">

        <div class="section-title">
          <div class="section-icon">
            <span class="mdi mdi-magnify"></span>
          </div>

          <div>
            <h3>委託紀錄查詢</h3>
            <p>選擇日期區間查看歷史委託</p>
          </div>
        </div>


        <!-- 日期 -->
        <div class="date-row">

          <v-text-field v-model="startDate" type="date" label="開始日期" density="compact" variant="outlined" hide-details
            class="date-input" />

          <span class="date-separator">至</span>

          <v-text-field v-model="endDate" type="date" label="結束日期" density="compact" variant="outlined" hide-details
            class="date-input" />

        </div>


        <!-- 快速選擇 -->
        <div class="search-bottom">

          <div class="quick-range">

            <span class="quick-label">
              快速選擇
            </span>

            <div class="quick-buttons">

              <v-btn size="small" variant="outlined" @click="handleQuickRange(1)">
                今天
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(7)">
                近一週
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(30)">
                近一月
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(90)">
                3 個月
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(365)">
                1 年
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(1095)">
                3 年
              </v-btn>

              <v-btn size="small" variant="outlined" @click="handleQuickRange(1825)">
                5 年
              </v-btn>

            </div>

          </div>


          <!-- 按鈕 -->
          <div class="query-actions">

            <v-btn size="small" variant="outlined" class="all-btn" @click="showAllOrders">
              全部紀錄
            </v-btn>

            <v-btn size="small" class="query-btn" elevation="0" @click="handleQueryRange">
              <span class="mdi mdi-magnify"></span>
              查詢
            </v-btn>

          </div>

        </div>

      </div>


      <!-- 日期錯誤 -->
      <v-alert v-if="dateError" type="warning" density="compact" class="mb-4">
        {{ dateError }}
      </v-alert>


      <!-- Loading -->
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" aria-label="載入委託與成交紀錄" />


      <!-- 錯誤 -->
      <v-alert v-if="errorMessage" type="error" density="compact" class="mb-4">
        {{ errorMessage }}
      </v-alert>


      <!-- 委託紀錄 -->
      <div class="record-card order-card">

        <div class="record-header">

          <div class="section-title record-title">

            <div class="section-icon">
              <span class="mdi mdi-clipboard-text-outline"></span>
            </div>

            <div>
              <h3>委託紀錄</h3>
              <p>您的歷史下單與成交資訊</p>
            </div>

          </div>


          <div v-if="!loading && !orderError" class="record-count">
            共 {{ orders.length }} 筆
          </div>

        </div>


        <!-- 有紀錄 -->
        <div v-if="!loading && !orderError && orders.length > 0" class="table-wrapper">

          <v-table class="order-table">

            <thead>
              <tr>
                <th>股票代號</th>
                <th>買賣別</th>
                <th>委託股數</th>
                <th>委託價格</th>
                <th>價格類型</th>
                <th>委託條件</th>
                <th>日期</th>
                <th>成交股數</th>
                <th>狀態</th>
                <th></th>
              </tr>
            </thead>


            <tbody>

              <template v-for="order in orders" :key="order.id">

                <tr class="order-row">


                  <td class="stock-code">
                    {{ order.stockCode }}
                  </td>

                  <td>
                    <span class="side-text" :class="{
                      'buy-text': order.orderType === 'BUY',
                      'sell-text': order.orderType === 'SELL'
                    }">
                      {{ formatSide(order.orderType) }}
                    </span>
                  </td>

                  <td>
                    {{ formatNumber(order.originalQuantity) }}
                  </td>

                  <td>
                    {{
                      order.priceType === 'MARKET_ORDER'
                        ? '市價'
                        : formatNumber(order.wantBuyPrice)
                    }}
                  </td>

                  <td>
                    {{ formatPriceType(order.priceType) }}
                  </td>

                  <td>
                    {{ formatTradingType(order.tradingType) }}
                  </td>

                  <td>
                    <date-time-text :value="order.createdAt" />
                  </td>

                  <td>
                    {{
                      tradeError
                        ? '無法取得'
                        : formatNumber(getFilledQuantity(order))
                    }}
                  </td>

                  <td>




                    <span class="status-badge" :class="{
                      'status-pending': order.status === 'PENDING',
                      'status-filled': order.status === 'FILLED',
                      'status-partial': order.status === 'PARTIALLY_FILLED',
                      'status-partial-cancelled': order.status === 'PARTIALLY_FILLED_CANCELLED',
                      'status-cancelled': order.status === 'CANCELLED'
                    }">
                      {{ getNoTradeText(order.status) }}
                    </span>

                  </td>
                  <td>
                    <span v-if="tradeError" class="text-medium-emphasis">
                      成交紀錄載入失敗
                    </span>


                    <v-btn v-else-if="getTrades(order).length > 0" size="small" variant="text" class="trade-action"
                      :aria-expanded="expandedOrderId === order.id" :aria-controls="`order-trades-${order.id}`"
                      @click="toggleTrades(order)">
                      {{
                        expandedOrderId === order.id
                          ? '收合'
                          : '查看成交'
                      }}
                    </v-btn>

                  </td>
                </tr>


                <!-- 成交資訊 -->
                <tr v-if="expandedOrderId === order.id && !tradeError" class="trade-detail-row">

                  <td colspan="10" class="trade-detail-cell">

                    <div :id="`order-trades-${order.id}`">

                      <h3 class="trade-detail-title">
                        成交資訊
                      </h3>


                      <v-table density="compact" class="trade-table">

                        <thead>
                          <tr>
                            <th>成交價</th>
                            <th>成交股數</th>
                            <th>成交時間</th>
                          </tr>
                        </thead>


            <tbody>

              <tr v-for="trade in getTrades(order)" :key="trade.id">

                <td>
                  {{ formatNumber(trade.price) }}
                </td>

                <td>
                  {{ formatNumber(trade.quantity) }}
                </td>

                <td>
                  <date-time-text :value="trade.tradedAt" />
                </td>

              </tr>

            </tbody>

          </v-table>

        </div>

        </td>

        </tr>

    </template>

    </tbody>

    </v-table>

    </div>


    <!-- 沒有紀錄 -->
    <div v-if="!loading && !orderError && orders.length === 0" class="empty-record">

      <span class="mdi mdi-clipboard-text-outline empty-icon"></span>

      <div class="empty-title">
        目前沒有委託紀錄
      </div>

      <div class="empty-text">
        完成下單後，您的委託紀錄會顯示在這裡
      </div>

    </div>

    </div>

</template>

</section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/orderStore'
import { useTradeStore } from '@/stores/tradeStore'
import { useAuthStore } from '@/stores/authStore'
import DateTimeText from '@/components/common/DateTimeText.vue'

const orderStore = useOrderStore()
const tradeStore = useTradeStore()
const authStore = useAuthStore()
const router = useRouter()

// 跳去登入頁，並記住目前這頁，登入完導回來
const goToLogin = () => {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

const {
  orders,
  errorMessage: orderError,
} = storeToRefs(orderStore)

const {
  trades,
  errorMessage: tradeError,
} = storeToRefs(tradeStore)


const loading = ref(true) // loading狀態
const expandedOrderId = ref(null)

const errorMessage = computed(() =>
  [
    orderError.value ? `委託紀錄：${orderError.value}` : '',
    tradeError.value ? `成交紀錄：${tradeError.value}` : '',
  ]
    .filter(Boolean)
    .join('；'),
)

const getTrades = (order) => {
  const field =
    order.orderType === 'BUY'
      ? 'buyOrderId'
      : order.orderType === 'SELL'
        ? 'sellOrderId'
        : null

  if (!field || order.id == null) return []

  return trades.value.filter((trade) =>
    trade[field] != null &&
    String(trade[field]) === String(order.id),
  )
}

const getFilledQuantity = (order) =>
  getTrades(order).reduce(
    (total, trade) => total + Number(trade.quantity),
    0,
  )

const formatNumber = (value) => {
  if (value == null || value === '') return '—'

  const number = Number(value)

  return Number.isFinite(number)
    ? number.toLocaleString('zh-TW', {
      maximumFractionDigits: 4,
    })
    : '—'
}

const formatSide = (orderType) => {
  if (orderType === 'BUY') return '買進'
  if (orderType === 'SELL') return '賣出'
  return orderType ?? '—'
}


const toggleTrades = (order) => {
  if (loading.value || tradeError.value) return
  if (getTrades(order).length === 0) return

  expandedOrderId.value =
    expandedOrderId.value === order.id ? null : order.id
}
const fetchRecords = async () => {
  loading.value = true
  expandedOrderId.value = null

  // 避免新請求失敗時顯示先前帳號或查詢的資料
  orders.value = []
  trades.value = []

  try {
    await Promise.all([
      orderStore.fetchMyOrders(),
      tradeStore.fetchMyTrades(),
    ])
  } finally {
    loading.value = false
  }
}

const getNoTradeText = (status) => {
  switch (status) {
    case 'PENDING':
      return '等待搓合'

    case 'CANCELLED':
      return '已取消'

    case 'PARTIALLY_FILLED':
      return '部分成交'

    case 'FILLED':
      return '已成交'

    case 'PARTIALLY_FILLED_CANCELLED':
      return '部分成交'

    default:
      return '尚無成交紀錄'
  }
}

// 時間區間
const startDate = ref('')
const endDate = ref('')
const dateError = ref('')

const taipeiDateFormatter = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Asia/Taipei',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const toTaipeiDateString = (date) => {
  const parts = Object.fromEntries(
    taipeiDateFormatter.formatToParts(date).map(({ type, value }) => [
      type,
      value,
    ]),
  )

  return `${parts.year}-${parts.month}-${parts.day}`
}

const handleQueryRange = async () => {
  if (!startDate.value || !endDate.value) {
    dateError.value = '請選擇開始與結束日期'
    return
  }

  if (startDate.value > endDate.value) {
    dateError.value = '開始日期不能晚於結束日期'
    return
  }

  dateError.value = ''
  loading.value = true
  expandedOrderId.value = null
  orders.value = []

  try {
    await orderStore.fetchMyOrdersByDateRange(
      startDate.value,
      endDate.value,
    )
  } finally {
    loading.value = false
  }
}

const handleQuickRange = async (days) => {
  const today = toTaipeiDateString(new Date())
  const start = new Date(`${today}T00:00:00Z`)
  start.setUTCDate(start.getUTCDate() - days)

  startDate.value = start.toISOString().slice(0, 10)
  endDate.value = today

  await handleQueryRange()
}


// 交易條件格式
const formatTradingType = (value) => {
  switch (value) {
    case 'ROD': return 'ROD'
    case 'IOC': return 'IOC'
    case 'FOK': return 'FOK'
    default: return value ?? '—'
  }
}
// 委託類型格式
const formatPriceType = (value) => {
  switch (value) {
    case 'MARKET_ORDER': return '市價單'
    case 'LIMIT_ORDER': return '限價單'
    default: return value ?? '—'
  }
}

const showAllOrders = async () => {
  startDate.value = ''
  endDate.value = ''
  dateError.value = ''
  expandedOrderId.value = null
  loading.value = true
  orders.value = []

  try {
    // 回到原本查全部的方法
    await orderStore.fetchMyOrders()
  } finally {
    loading.value = false
  }

}
// 委託/成交紀錄是個人資料，改成先判斷有沒有登入才打API，
// 沒登入就不打，讓畫面顯示「請先登入」提示，不要一進頁面就被401彈回登入頁
onMounted(() => {
  if (authStore.isLoggedIn) {
    fetchRecords()
  } else {
    loading.value = false
  }
})
</script>



<style scoped>
/* 整體 */

.order-record-page {
  width: 100%;
}


/* 頁面標題 */

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;

  color: #374151;

  font-size: 28px;
  font-weight: 700;
}

.page-header p {
  margin: 5px 0 0;

  color: #9ca3af;

  font-size: 15px;
}


/* 共用卡片 */

.record-card {
  padding: 22px;

  background: #ffffff;

  border: 1px solid #eee9df;
  border-radius: 12px;

  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-card {
  margin-bottom: 18px;
}


/* 區塊標題 */

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;

  margin-bottom: 22px;
}

.section-title h3 {
  margin: 0;

  color: #374151;

  font-size: 20px;
  font-weight: 700;
}

.section-title p {
  margin: 3px 0 0;

  color: #9ca3af;

  font-size: 14px;
}

.section-icon {
  width: 44px;
  height: 44px;

  display: flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;

  background: #fff1dc;

  border-radius: 50%;

  color: #f59e0b;

  font-size: 24px;
}


/* 日期 */

.date-row {
  display: flex;
  align-items: center;
  gap: 14px;

  max-width: 670px;
}

.date-input {
  max-width: 290px;
}

.date-separator {
  color: #6b7280;

  font-size: 15px;
  font-weight: 600;
}


/* 日期欄 focus */

.date-input :deep(.v-field--focused) {
  color: #7c5ce5;
}


/* 查詢下方 */

.search-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;

  margin-top: 20px;
}


/* 快速選擇 */

.quick-range {
  display: flex;
  align-items: center;
  gap: 14px;
}

.quick-label {
  flex-shrink: 0;

  color: #6b7280;

  font-size: 14px;
  font-weight: 600;
}

.quick-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-buttons :deep(.v-btn) {
  min-width: 72px;

  background: #ffffff;

  border-color: #cfc4f6;

  border-radius: 8px;

  color: #6d50d7;

  text-transform: none;

  transition: 0.2s;
}

.quick-buttons :deep(.v-btn:hover) {
  background: #f5f1ff;

  border-color: #9277e8;

  color: #6241d2;
}


/* 查詢按鈕 */

.query-actions {
  display: flex;
  align-items: center;
  gap: 10px;

  flex-shrink: 0;
}


/* 全部紀錄 */

.all-btn {
  min-width: 95px;

  background: #f5f1ff !important;

  border-color: #cfc4f6 !important;
  border-radius: 8px;

  color: #6d50d7 !important;

  font-weight: 600;

  text-transform: none;
}

.all-btn:hover {
  background: #ebe5ff !important;

  border-color: #9277e8 !important;
}


/* 查詢*/

.query-btn {
  min-width: 100px;

  background: #f59e0b !important;
  color: #ffffff !important;

  border-radius: 8px;

  font-weight: 600;

  text-transform: none;

  transition: 0.2s;
}

.query-btn:hover {
  background: #e58d05 !important;
}


/* 委託紀錄標題*/

.record-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.record-title {
  margin-bottom: 18px;
}


/* 筆數 */

.record-count {
  padding: 6px 14px;

  background: #fff1dc;

  border-radius: 20px;

  color: #d98200;

  font-size: 14px;
  font-weight: 600;
}


/* 表格外框 */

.table-wrapper {
  width: 100%;

  overflow-x: auto;

  border: 1px solid #eee9df;
  border-radius: 9px;
}


/* 委託表格 */

.order-table {
  min-width: 1050px;
  background: #fbfaff;
}

.order-table :deep(th) {
  height: 46px;

  background: #fff9f0;

  color: #4b5563;

  font-size: 14px;
  font-weight: 600;

  white-space: nowrap;
}

.order-table :deep(td) {
  height: 54px;

  color: #374151;

  font-size: 14px;

  white-space: nowrap;
}

/* hover */
.order-row :deep(td) {
  transition: background-color 0.2s;
}

.order-row:hover :deep(td) {
  background: #f1edff;
}

.order-id {
  color: #6b7280;
}

.stock-code {
  color: #374151;

  font-weight: 700;
}


/* 買進 / 賣出 */

.side-text {
  font-weight: 600;
}

.buy-text {
  color: #ef4444;
}

.sell-text {
  color: #22a35a;
}


/* 狀態膠囊 */

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  min-width: 82px;

  padding: 4px 12px;

  border-radius: 20px;

  font-size: 13px;
  font-weight: 600;
}

.status-pending {
  background: #edf3f9;

  color: #476581;
}

.status-partial {
  background: #fff2d8;

  color: #d18400;
}

.status-filled {
  background: #e9f8ef;

  color: #238b52;
}

.status-cancelled {
  background: #fdeaea;

  color: #e04b4b;
}

.status-partial-cancelled {
  background: #fdf1ea;

  color: #c95123;
}


/* 查看成交 */

.trade-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  min-width: 82px;
  min-height: 32px;
  padding: 0 12px;

  background: #fff3dc !important;

  border: 1px solid #f3c66f;
  border-radius: 7px;

  color: #d98200 !important;

  font-size: 14px;
  font-weight: 600;

  text-transform: none;

  transition: 0.2s;
}

.trade-action:hover {
  background: #ffe9c2 !important;
  border-color: #f3c66f;
}

.trade-status {
  display: inline-flex;
  align-items: center;

  min-height: 32px;

  color: #6d727a;

  font-size: 14px;
  font-weight: 500;
}

/* 成交明細 */

.trade-detail-row {
  background: #fffaf3;
}

.trade-detail-cell {
  padding: 18px 24px !important;

  background: #fffaf3;

  border-left: 3px solid #f3c66f;
}

.trade-detail-title {
  margin: 0 0 12px;

  color: #d98200;

  font-size: 16px;
  font-weight: 700;
}

.trade-table {
  overflow: hidden;

  background: #ffffff;

  border: 1px solid #f1dfbd;
  border-radius: 8px;
}

.trade-table :deep(th) {
  background: #fff3dc;

  color: #5f5546;
}

.trade-table :deep(td) {
  background: #fffdf9;
}


/* 沒有紀錄 */

.empty-record {
  min-height: 250px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text-align: center;
}

.empty-icon {
  margin-bottom: 12px;

  color: #f0b64b;

  font-size: 58px;
}

.empty-title {
  margin-bottom: 6px;

  color: #4b5563;

  font-size: 18px;
  font-weight: 600;
}

.empty-text {
  color: #9ca3af;

  font-size: 14px;
}
</style>
