<template>

  <div class="order-page">

    <!-- 頁面標題 -->
    <div class="page-header">
      <h2>訂單紀錄</h2>
      <p>查看您的訂單狀態與成交進度</p>
    </div>


    <!-- 已登入 -->
    <div
      v-if="authStore.isLoggedIn"
      class="order-card"
    >

      <!-- 卡片標題 -->
      <div class="card-header">

        <div class="card-title">

          <div class="title-icon">
            <span class="mdi mdi-receipt-text-clock-outline"></span>
          </div>

          <div>
            <h3>訂單紀錄</h3>
            <p>您的歷史下單與成交資訊</p>
          </div>

        </div>


        <div class="order-count">
          共 {{ orders.length }} 筆
        </div>

      </div>


      <!-- 表格 -->
      <div class="table-wrapper">

        <v-table class="order-table">

          <thead>
            <tr>
              <th>訂單編號</th>
              <th>股票代號</th>
              <th>買賣別</th>
              <th>訂單股數</th>
              <th>成交股數</th>
              <th>價格</th>
              <th>價格類型</th>
              <th>下單條件</th>
              <th>交易時段</th>
              <th>狀態</th>
              <th>操作</th>
            </tr>
          </thead>


          <tbody>

            <tr
              v-for="order in orders"
              :key="order.id"
              class="order-row"
            >

              <td class="order-id">
                # {{ order.id }}
              </td>

              <td class="stock-code">
                {{ order.stockCode }}
              </td>

              <td>
                <span
                  class="side-text"
                  :class="
                    order.orderType === 'BUY'
                      ? 'buy-text'
                      : 'sell-text'
                  "
                >
                  {{ order.orderType === 'BUY' ? '買進' : '賣出' }}
                </span>
              </td>

              <td>
                {{ order.originalQuantity }}
              </td>

              <td>
                {{ calculateFilledQuantity(order) }}
              </td>

              <td>
                {{ order.wantBuyPrice ?? '市價' }}
              </td>

              <td>
                {{
                  order.priceType === 'LIMIT_ORDER'
                    ? '限價單'
                    : '市價單'
                }}
              </td>

              <td>
                {{ order.tradingType }}
              </td>

              <td>
                {{
                  order.tradingTime === 'DURING_HOURS'
                    ? '盤中'
                    : '盤後'
                }}
              </td>

              <td>

                <span
                  class="status-badge"
                  :class="{
                    'status-pending': order.status === 'PENDING',
                    'status-filled': order.status === 'FILLED',
                    'status-partial': order.status === 'PARTIALLY_FILLED',
                    'status-partial-cancelled': order.status === 'PARTIALLY_FILLED_CANCELLED',
                    'status-cancelled': order.status === 'CANCELLED'
                  }"
                >

                  {{
                    order.status === 'PENDING'
                      ? '等待撮合'
                      : order.status === 'FILLED'
                      ? '已成交'
                      : order.status === 'PARTIALLY_FILLED'
                      ? '部分成交'
                      : order.status === 'CANCELLED'
                      ? '已取消'
                      : order.status === 'PARTIALLY_FILLED_CANCELLED'
                      ? '部分成交部分取消'
                      : order.status
                  }}

                </span>

              </td>


              <td>

                <v-btn
                  v-if="order.status === 'PENDING'"
                  size="small"
                  variant="text"
                  class="cancel-btn"
                  @click="handleCancelOrder(order.id)"
                >
                  取消
                </v-btn>

                <span
                  v-else
                  class="no-action"
                >
                  —
                </span>

              </td>

            </tr>


            <!-- 沒有資料 -->
            <tr v-if="orders.length === 0">

              <td
                colspan="11"
                class="empty-cell"
              >

                <div class="empty-record">

                  <span
                    class="mdi mdi-receipt-text-outline empty-icon"
                  ></span>

                  <div class="empty-title">
                    目前沒有訂單紀錄
                  </div>

                  <div class="empty-text">
                    完成下單後，訂單會顯示在這裡
                  </div>

                </div>

              </td>

            </tr>

          </tbody>

        </v-table>

      </div>

    </div>


    <!-- 未登入 -->
    <v-alert
      v-else
      type="info"
      variant="tonal"
      class="mt-4"
    >
      請先登入會員查看您的訂單紀錄

      <div class="mt-2">
        <v-btn
          size="small"
          color="primary"
          @click="goToLogin"
        >
          前往登入
        </v-btn>
      </div>

    </v-alert>

  </div>

</template>

<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/orderStore'
import { useAuthStore } from '@/stores/authStore'
const orderStore = useOrderStore()
const authStore = useAuthStore()
const router = useRouter()
// State 用 storeToRefs 解構，保持響應性
const { orders } = storeToRefs(orderStore)

// 進頁面就先拿一次「我的所有訂單」
// 訂單紀錄是個人資料，改成先判斷有沒有登入才打API，沒登入就不打，讓畫面顯示「請先登入」提示
onMounted(() => {
    if (authStore.isLoggedIn) {
      orderStore.fetchMyOrders()
    }
})

// 跳去登入頁，並記住目前這頁，登入完導回來
const goToLogin = () => {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

function calculateFilledQuantity(order) {
    const original = Number(order.originalQuantity) || 0;
    const remaining = Number(order.remainingQuantity) || 0;

    return Math.max(0, original - remaining);
}


const handleCancelOrder = async (orderId) => {
  await orderStore.cancelOrder(orderId)
}
</script>


<style scoped>

.order-page {
  width: 100%;
}


/* 頁面標題 */

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;

  color: #10233f;

  font-size: 28px;
  font-weight: 700;
}

.page-header p {
  margin: 5px 0 0;

  color: #718096;

  font-size: 15px;
}


/* 卡片*/

.order-card {
  padding: 24px;

  background: #ffffff;

  border: 1px solid #eee9df;
  border-radius: 12px;

  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.035);
}


/* 卡片標題 */

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;

  gap: 14px;
}

.title-icon {
  width: 46px;
  height: 46px;

  display: flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;

  background: #fff3dc;

  border-radius: 50%;

  color: #e99a00;

  font-size: 24px;
}

.card-title h3 {
  margin: 0;

  color: #10233f;

  font-size: 20px;
  font-weight: 700;
}

.card-title p {
  margin: 3px 0 0;

  color: #718096;

  font-size: 14px;
}


/* 筆數 */

.order-count {
  padding: 7px 16px;

  background: #fff3dc;

  border-radius: 20px;

  color: #c98000;

  font-size: 14px;
  font-weight: 600;
}


/* 表格 */

.table-wrapper {
  width: 100%;

  overflow-x: auto;

  border-radius: 8px;
}

.order-table {
  min-width: 1100px;
}


/* 表頭 */

.order-table :deep(th) {
  height: 46px;

  background: #faf7f1;

  color: #24364d;

  font-size: 14px;
  font-weight: 600;

  white-space: nowrap;

  border-bottom: 1px solid #ebe6dc !important;
}


/* 內容 */

.order-table :deep(td) {
  height: 54px;

  background: #fbfaff;

  color: #263548;

  font-size: 14px;

  white-space: nowrap;

  border-bottom: 1px solid #eeeaf5 !important;
}


/* hover */

.order-row :deep(td) {
  transition: background-color 0.2s;
}

.order-row:hover :deep(td) {
  background: #f1edff;
}


/* 一般欄位 */

.order-id {
  color: #374151;

  font-weight: 500;
}

.stock-code {
  color: #24364d;

  font-weight: 600;
}


/* 買賣 */

.side-text {
  font-weight: 600;
}

.buy-text {
  color: #ef4444;
}

.sell-text {
  color: #20a35a;
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


/* 等待撮合 */

.status-pending {
  background: #edf3f9;

  color: #476581;
}


/* 部分成交 */

.status-partial {
  background: #fff2d8;

  color: #d18400;
}


/* 已成交 */

.status-filled {
  background: #e9f8ef;

  color: #238b52;
}


/* 已取消 */

.status-cancelled {
  background: #fdeaea;

  color: #e04b4b;
}

/* 部分成交部分取消 */

.status-partial-cancelled {
  /* 調整為更溫和的粉橘色背景，與純紅色的 status-cancelled 做出區隔 */
  background: #fdf1ea;

  /* 調整為深紅棕色，既有部分成交的暖色感，又有取消的嚴肅感 */
  color: #c95123;
}


/* 取消按鈕 */

.cancel-btn {
  min-width: 52px;

  color: #e05252 !important;

  font-size: 14px;
  font-weight: 600;

  letter-spacing: normal;
  text-transform: none;
}

.cancel-btn:hover {
  background: #fff0f0 !important;
}

.no-action {
  color: #a8adb4;
}


/* 空資料 */

.empty-cell {
  padding: 0 !important;
}

.empty-record {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  min-height: 220px;

  padding: 30px;
}

.empty-icon {
  margin-bottom: 10px;

  color: #e5c98e;

  font-size: 46px;
}

.empty-title {
  margin-bottom: 5px;

  color: #4b5563;

  font-size: 16px;
  font-weight: 600;
}

.empty-text {
  color: #9ca3af;

  font-size: 14px;
}

</style>
