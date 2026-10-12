<!-- 父元件 OrderPad.vue -->
<template>
  <v-container fluid class="order-page">

    <!-- 頁面標題 -->
    <div class="page-header">
      <h2>五檔下單</h2>
      <p>查看五檔報價並建立委託</p>
    </div>

    <!-- 第一排 -->
    <v-row class="top-row">

      <!-- 股票搜尋 / 股票資訊 -->
      <v-col cols="12" md="8">
        <StockInfo />
      </v-col>

      <!-- 帳戶狀態 -->
      <v-col cols="12" md="4">
        <UserStatus />
      </v-col>

    </v-row>

    <!-- 第二排 -->
    <v-row class="trade-row">

      <!-- 五檔報價 -->
      <v-col cols="12" md="4">
        <FivePriceLevel />
      </v-col>

      <!-- 下單面板 -->
      <v-col cols="12" md="8">
        <OrderOption />
      </v-col>

    </v-row>

  </v-container>
</template>

<script setup>
import StockInfo from '@/components/Stock/StockInfo.vue';
import FivePriceLevel from '@/components/Stock/FivePriceLevel.vue';
import OrderOption from '@/components/Stock/OrderOption.vue';
import UserStatus from '@/components/Stock/UserStatus.vue';
</script>

<style scoped>

/* 整體頁面 */

:global(body),
:global(.v-main) {
  background-color: #f8fafc !important;
}

.order-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;

  padding: 8px 24px 24px !important;
}


/* 頁面標題 */

.page-header {
  margin-bottom: 8px;
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


/* Row 間距 */

.v-row {
  margin-top: 0 !important;
  margin-bottom: 8px !important;
}

.top-row {
  margin-bottom: 4px !important;
}


/* 卡片共用外觀 */

.v-col > :deep(.stock-info-card),
.v-col > :deep(.user-status-card),
.v-col > :deep(.five-level-price-container),
.v-col > :deep(.order-pad-desktop) {
  background-color: #ffffff !important;
  border: 1px solid #eee9df !important;
  border-radius: 12px !important;
  padding: 24px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  box-sizing: border-box;
}


/* 第二排高度一致 */

@media (min-width: 960px) {

  .trade-row {
    display: flex !important;
    align-items: stretch !important;
  }

  .trade-row > .v-col {
    display: flex;
  }

  .trade-row > .v-col > * {
    width: 100%;
    height: 100%;
  }

}

</style>