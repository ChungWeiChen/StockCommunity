<template>

  <div class="holding-page">

    <!-- 頁面標題 -->
    <div class="page-header">
      <h2>我的持股</h2>
      <p>查看您的持股部位與投資損益</p>
    </div>


    <!-- 錯誤訊息 -->
    <v-alert v-if="errorMessage" type="error" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>


    <!-- 未登入 -->
    <v-alert v-if="!authStore.isLoggedIn" type="info" variant="tonal" class="mt-4">
      請先登入會員查看您的持股

      <div class="mt-2">
        <v-btn size="small" color="primary" @click="goToLogin">
          前往登入
        </v-btn>
      </div>
    </v-alert>


    <!-- 已登入 -->
    <div v-else class="holding-card">

      <!-- 卡片標題 -->
      <div class="card-header">

        <div class="card-title">

          <div class="title-icon">
            <span class="mdi mdi-wallet-outline"></span>
          </div>

          <div>
            <h3>持股明細</h3>
            <p>您的目前持股與損益資訊</p>
          </div>

        </div>


        <div class="holding-count">
          共 {{ holdings.length }} 檔
        </div>

      </div>


      <!-- 表格 -->
      <div class="table-wrapper">

        <v-table class="holding-table">

          <thead>
            <tr>
              <th>股票代號</th>
              <th>持有股數</th>
              <th>損益</th>
              <th>損益率</th>
              <th>現價</th>
              <th>成交均價</th>
              <th>成交成本</th>
            </tr>
          </thead>


          <tbody>

            <tr v-for="holding in holdings" :key="holding.id" class="holding-row">

              <td class="stock-code">
                {{ holding.stockCode }}
              </td>

              <td>
                {{ holding.quantity }}
              </td>

              <td class="profit-text" :class="{
                'text-red': calculateProfit(holding) > 0,
                'text-green': calculateProfit(holding) < 0
              }">
                {{ formatProfit(calculateProfit(holding)) }}
              </td>

              <td class="profit-text" :class="{
                'text-red': calculateProfitRate(holding) > 0,
                'text-green': calculateProfitRate(holding) < 0
              }">
                {{ formatProfitRate(calculateProfitRate(holding)) }}
              </td>

              <td>
                {{ formatNumber(holding.latestClosePrice) }}
              </td>

              <td>
                {{ holding.averagePrice }}
              </td>

              <td>
                {{ formatNumber(calculateInvestmentCost(holding)) }}
              </td>


            </tr>


            <!-- 沒有持股 -->
            <tr v-if="holdings.length === 0">

              <td colspan="8" class="empty-cell">

                <div class="empty-holding">

                  <span class="mdi mdi-wallet-outline empty-icon"></span>

                  <div class="empty-title">
                    目前沒有持股
                  </div>

                  <div class="empty-text">
                    成交後的持股會顯示在這裡
                  </div>

                </div>

              </td>

            </tr>

          </tbody>

        </v-table>

      </div>

    </div>

  </div>

</template>

<script setup>
import { onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useHoldingStore } from '@/stores/holdingStore';
import { useAuthStore } from '@/stores/authStore';
import { useTradingAccountStore } from "@/stores/tradingAccountStore";
import DateTimeText from '@/components/common/DateTimeText.vue';

const holdingStore = useHoldingStore();
const tradingAccountStore = useTradingAccountStore();
const authStore = useAuthStore();
const router = useRouter();
// State 用 storeToRefs 解構，保持響應性
const { holdings, errorMessage } = storeToRefs(holdingStore);
const { availableCash,
  frozenCash, errorMessage: accountErrorMessage } = storeToRefs(tradingAccountStore);
// 進頁面就拿一次「我的所有持股」、「我的交易帳戶」
// 沒有表單、沒有送出動作——持股是系統算出來的結果，這頁純唯讀
// 持股是個人資料，改成先判斷有沒有登入才打API，沒登入就不打，讓畫面顯示「請先登入」提示
onMounted(() => {
  if (authStore.isLoggedIn) {
    holdingStore.fetchMyHoldings();
    tradingAccountStore.fetchTradingAccount();
  }
});

// 跳去登入頁，並記住目前這頁，登入完導回來
const goToLogin = () => {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
};
// 投資成本計算
const calculateInvestmentCost = (holding) => {
  return (
    Number(holding.averagePrice) *
    Number(holding.quantity)
  );
};
// 損益計算
const calculateProfit = (holding) => {
  if (holding.latestClosePrice == null) {
    return null;
  }

  return (
    Number(holding.latestClosePrice) -
    Number(holding.averagePrice)
  ) * Number(holding.quantity);
};

// 損益率計算
const calculateProfitRate = (holding) => {
  const latestPrice = Number(holding.latestClosePrice);
  const averagePrice = Number(holding.averagePrice);

  if (
    holding.latestClosePrice == null ||
    !Number.isFinite(averagePrice) ||
    averagePrice <= 0
  ) {
    return null;
  }

  return ((latestPrice - averagePrice) / averagePrice) * 100;
};
// 損益格式限制
const formatProfit = (profit) => {
  if (profit == null) {
    return "—";
  }

  const formatted = new Intl.NumberFormat("zh-TW", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(profit);

  return profit > 0 ? `+${formatted}` : formatted;
};
// 損益率格式限制
const formatProfitRate = (rate) => {
  if (rate == null) {
    return "—";
  }

  return `${rate > 0 ? "+" : ""}${rate.toFixed(2)}%`;
};
// 一般數字格式限制
const formatNumber = (value) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return "—";
  }
  return new Intl.NumberFormat("zh-TW", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value));
}
// 帳戶金額格式限制
const formatAccount = (value) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return "—";
  }
  return new Intl.NumberFormat("zh-TW", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value));
}

</script>

<!-- <<<<<<< HEAD
<template>
  <div class="holding-header mb-4">
    <h2 class="text-h5 font-weight-bold">我的持股</h2>

    <v-sheet v-if="authStore.isLoggedIn" class="account-summary d-flex align-center" border rounded="lg">
      <div class="account-item">
        <span class="text-caption text-medium-emphasis">
          帳戶餘額
        </span>

        <span class="text-body-2 font-weight-bold">
          $ {{ formatAccount(
            Number(availableCash) + Number(frozenCash)
          ) }}
        </span>
      </div>

      <v-divider vertical class="mx-3 my-2" />

      <div class="account-item">
        <span class="text-caption text-medium-emphasis">
          可用餘額
        </span>

        <span class="text-body-2 font-weight-bold text-success">
          $ {{ formatAccount(availableCash) }}
        </span>
      </div>
    </v-sheet>

  </div>
  <v-alert v-if="errorMessage" type="error" density="compact" class="mb-4">{{ errorMessage }}</v-alert> -->


<!-- <style scoped>


  <v-table v-else>
    <thead>
      <tr>
        <th>股票代號</th>
        <th>持有股數</th>
        <th>可下單數</th>
        <th>損益</th>
        <th>損益率</th>
        <th>現價</th>
        <th>成交均價</th>
        <th>成交成本</th>
        <th>更新時間</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="holding in holdings" :key="holding.id">
        <td>{{ holding.stockCode }}</td>
        <td>{{ holding.quantity }}</td>
        <td>{{ holding.availableQuantity ?? 0 }}</td>
        <td :class="{
          'text-red': calculateProfit(holding) > 0,
          'text-green': calculateProfit(holding) < 0
        }">
          {{ formatProfit(calculateProfit(holding)) }}
        </td>
        <td :class="{
          'text-red': calculateProfitRate(holding) > 0,
          'text-green': calculateProfitRate(holding) < 0
        }">
          {{ formatProfitRate(calculateProfitRate(holding)) }}
        </td>
        <td>
          {{ formatNumber(holding.latestClosePrice) }}
        </td>
        <td>{{ holding.averagePrice }}</td>
        <td>
          {{ formatNumber(calculateInvestmentCost(holding)) }}
        </td>
        <td><date-time-text :value="holding.updatedAt"></date-time-text></td>
      </tr>
    </tbody>
  </v-table>
  <p v-if="authStore.isLoggedIn && holdings.length === 0" class="mt-4">目前沒有持股</p>
</template> -->

<style scoped>
.holding-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.account-summary {
  flex-shrink: 0;
  padding: 4px 12px;
  white-space: nowrap;
  background: rgba(var(--v-theme-surface-variant), 0.3);
}

.account-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 整體*/

.holding-page {
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


/* 持股卡片 */

.holding-card {
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


/* 持股檔數 */

.holding-count {
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

.holding-table {
  min-width: 1000px;
}


/* 表頭 */

.holding-table :deep(th) {
  height: 46px;

  background: #faf7f1;

  color: #24364d;

  font-size: 14px;
  font-weight: 600;

  white-space: nowrap;

  border-bottom: 1px solid #ebe6dc !important;
}


/* 資料列 */

.holding-table :deep(td) {
  height: 58px;

  background: #fbfaff;

  color: #263548;

  font-size: 14px;

  white-space: nowrap;

  border-bottom: 1px solid #eeeaf5 !important;
}


/* Hover 淺紫 */

.holding-row :deep(td) {
  transition: background-color 0.2s;
}

.holding-row:hover :deep(td) {
  background: #f1edff;
}


/* 股票代號 */

.stock-code {
  color: #24364d !important;

  font-weight: 700;
}


/* 損益 */

.profit-text {
  font-weight: 600;
}


/* 台股：上漲紅、下跌綠 */

.text-red {
  color: #ef4444 !important;
}

.text-green {
  color: #20a35a !important;
}


/* 更新時間 */

.update-time {
  color: #6b7280 !important;
}


/* 沒有持股 */

.empty-cell {
  padding: 0 !important;
}

.empty-holding {
  min-height: 230px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  padding: 30px;

  text-align: center;
}

.empty-icon {
  margin-bottom: 12px;

  color: #f0b64b;

  font-size: 52px;
}

.empty-title {
  margin-bottom: 6px;

  color: #4b5563;

  font-size: 17px;
  font-weight: 600;
}

.empty-text {
  color: #9ca3af;

  font-size: 14px;
}
</style>
