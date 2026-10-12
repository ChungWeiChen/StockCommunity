<template>
  <div class="community-layout">

    <!-- 左邊 -->
    <div class="community-main">

      <FindStock
        :stock-code="selectedStockCode"
        :stock-name="selectedStockName"
        @select-stock="handleSelectStock"
        @clear-stock="handleClearStock"
      />

      <div class="post-gap"></div>

      <PostsList
        v-if="selectedStockCode"
        type="stock"
        :stock-code="selectedStockCode"
        :stock-name="selectedStockName"
      />

    </div>

    <!-- 右邊 -->
    <div class="community-right">

      <PopularStocks />

      <NewsList
        :stock-code="selectedStockCode"
        :stock-name="selectedStockName"
      />

    </div>

  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";

import FindStock from "@/components/Community/StockSearch.vue";
import PostsList from "@/components/Community/PostsList.vue";
import PopularStocks from "@/components/Community/PopularStocks.vue";
import NewsList from "@/components/Community/NewsList.vue";

const route = useRoute();

const selectedStockCode = ref("");
const selectedStockName = ref("");


// 監聽網址上的股票代號 + 股票名稱
watch(
  () => [route.query.stockCode, route.query.stockName],
  ([stockCode, stockName]) => {

    selectedStockCode.value = stockCode ?? "";
    selectedStockName.value = stockName ?? "";

  },
  { immediate: true }
);


function handleSelectStock(stock) {

  selectedStockCode.value = stock.stockCode;
  selectedStockName.value = stock.simpleName;

  console.log("選到的股票：", stock);
}


function handleClearStock() {

  selectedStockCode.value = "";
  selectedStockName.value = "";

}
</script>

<style scoped>

.community-layout {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.community-main {
  flex: 1;
  min-width: 0;
}

.post-gap {
  height: 8px;
}

.community-right {
  width: 350px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}



</style>