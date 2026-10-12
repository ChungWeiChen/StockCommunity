<template>

  <div class="stock-info-card">

    <!-- 左側：標題 / 股票資訊 -->
    <div class="stock-info-left">

      <div class="stock-title-area">

        <div class="stock-icon">
          <span class="mdi mdi-magnify"></span>
        </div>

        <div>
          <h3>股票搜尋</h3>
          <p>輸入股票代號查看五檔報價</p>
        </div>

      </div>

      <!-- 已選股票 -->
      <div v-if="stockStore.currentStockAndPrice" class="selected-stock">
        <div class="selected-stock-name">
          {{ stockStore.currentStockAndPrice.stockCode }}
          {{ stockStore.currentStockAndPrice.simpleName }}
        </div>

        <div class="latest-price">
          <span class="price-label">最新價</span>

          <span class="price-value" :class="{
            'price-up': priceChange === 'up',
            'price-down': priceChange === 'down',
            'price-flat': priceChange === 'flat'
          }">
            <span class="price-symbol">
              {{ priceSymbol }}
            </span>

            {{ formatNumber(stockStore.currentStockAndPrice?.price) }}
          </span>
        </div>
      </div>

    </div>


    <!-- 右側：搜尋 -->
    <!-- <div class="stock-search-area">

      <v-text-field
        v-model="localInputCode"
        placeholder="輸入股票代號"
        density="compact"
        hide-details
        variant="outlined"
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #prepend-inner>
          <span class="mdi mdi-magnify search-icon"></span>
        </template>
</v-text-field>

<v-btn class="search-btn" size="small" @click="handleSearch">
  搜尋
</v-btn>

</div> -->



    <div class="stock-search-area">

      <div class="search-wrapper">

        <v-text-field v-model="localInputCode" placeholder="輸入股票代號" density="compact" hide-details variant="outlined"
          class="search-input" @keyup.enter="handleSearch">
          <template #prepend-inner>
            <span class="mdi mdi-magnify search-icon"></span>
          </template>
        </v-text-field>

        <div v-if="showSearchResults && stocks.length > 0" class="search-results">
          <div v-for="stock in stocks" :key="stock.stockCode" class="search-result-item" @click="selectStock(stock)">
            <span class="result-code">
              {{ stock.stockCode }}
            </span>

            <span class="result-name">
              {{ stock.simpleName }}
            </span>
          </div>
        </div>

      </div>

      <v-btn class="search-btn" size="small" @click="handleSearch">
        搜尋
      </v-btn>

    </div>
  </div>





</template>

<script setup>
import { watch, ref, computed, nextTick } from "vue";
import { onMounted, onUnmounted } from "vue"; // 💡 引入生命週期鉤子
import { useRoute } from "vue-router";
import { storeToRefs } from "pinia";
import { useStockStore } from "@/stores/stockStore"
import { useMarketDepthStore } from "@/stores/marketDepthStore";
import { useHoldingStore } from "@/stores/holdingStore";
import { useAuthStore } from "@/stores/authStore";
import { formatNumber } from '@/utils/format' // 💡 引入工具


const stockStore = useStockStore()
const marketDepthStore = useMarketDepthStore()
const holdingStore = useHoldingStore();
const authStore = useAuthStore();

const route = useRoute();
const localInputCode = ref(''); // 💡 用一個內部的變數來綁定 input 的 v-model
const showSearchResults = ref(false);

// 💡 storeToRefs 拿出來的分身，在下面 script 裡一律用 searchKeyword.value 來控制
const { currentStockCode, stocks } = storeToRefs(stockStore)

// 最新價變化(顏色&圖案)
const previousPrice = ref(null);
const priceChange = ref('flat'); // 'up' | 'down' | 'flat'

const updatePriceStatus = (newPrice) => {
  console.log(
    '[updatePriceStatus]',
    'newPrice =', newPrice,
    'previousPrice =', previousPrice.value,
    'priceChange(before) =', priceChange.value
  );

  if (newPrice == null) {
    return;
  }

  const currentPrice = Number(newPrice);

  if (previousPrice.value == null) {
    previousPrice.value = currentPrice;
    priceChange.value = 'flat';
    return;
  }

  if (currentPrice > previousPrice.value) {
    priceChange.value = 'up';
  } else if (currentPrice < previousPrice.value) {
    priceChange.value = 'down';
  } else {
    priceChange.value = 'flat';
  }

  previousPrice.value = currentPrice;

  console.log(
    '[updatePriceStatus after]',
    'previousPrice =', previousPrice.value,
    'priceChange =', priceChange.value
  );
};

watch(
  () => stockStore.currentStockAndPrice?.price,
  (newPrice, oldPrice) => {
    console.log(
      '[Price Watch]',
      'old =', oldPrice,
      'new =', newPrice,
      'stockCode =', stockStore.currentStockCode,
      'currentStock =', stockStore.currentStockAndPrice
    );

    updatePriceStatus(newPrice);
  }
);



const priceSymbol = computed(() => {
  switch (priceChange.value) {
    case 'up':
      return '▲';
    case 'down':
      return '▼';
    case 'flat':
    default:
      return '●';
  }
});

// 搜尋股票代碼時代入資訊
// const handleSearch = () => {
//   if (!localInputCode.value) return;


//   stockStore.currentStock = stockStore.fetchStock(localInputCode.value);

//   stockStore.currentStockCode = localInputCode.value;

//   // 搜索時，同時去拿持股資訊跟五檔報價的資料
//   stockStore.fetchNewestPrice(currentStockCode.value)

//   marketDepthStore.fetchMarketDepth(currentStockCode.value);

//   // 持股資訊是個人資料，沒登入就不打，避免被401悄悄彈回登入頁
//   if (authStore.isLoggedIn) {
//     holdingStore.fetchMyStockHolding(currentStockCode.value);
//   }
// }

// 搜尋
// const handleSearch = async () => {
//   const keyword = localInputCode.value.trim();

//   if (!keyword) {
//     showSearchResults.value = false;
//     return;
//   }

//   await stockStore.searchStocks(keyword);

//   showSearchResults.value = stockStore.stocks.length > 0;
// };

const handleSearch = async () => {
  const keyword = localInputCode.value.trim();

  if (!keyword) {
    showSearchResults.value = false;
    return;
  }

  await stockStore.searchStocks(keyword);

  console.log("=== 搜尋結果 ===");
  console.log("stocks =", stockStore.stocks);
  console.log("length =", stockStore.stocks?.length);

  showSearchResults.value = stockStore.stocks?.length > 0;
};

// 選擇股票
const selectStock = (stock) => {
  showSearchResults.value = false;

  localInputCode.value = stock.stockCode;

  stockStore.currentStock = stock;
  stockStore.currentStockCode = stock.stockCode;

  marketDepthStore.fetchMarketDepth(stockStore.currentStockCode);

  if (authStore.isLoggedIn) {
    holdingStore.fetchMyStockHolding(stockStore.currentStockCode);
  }
};

// 輪巡，定期重整最新價
let pollingTimer = null;

// 開始輪詢的函式
const startPolling = () => {
  stopPolling(); // 確保不會重複重複建立定時器
  pollingTimer = setInterval(() => {
    if (stockStore.currentStockCode) {
      // 💡 每隔 2 秒，自動默默在背景呼叫你的 Store 獲取最新價
      stockStore.fetchNewestPrice(stockStore.currentStockCode);
    }
  }, 2000); // 2000 毫秒 = 2 秒，可依據需求調整為 1000 或 3000
};

// 停止輪詢的函式
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
};

watch(
  () => route.query.code,
  async (newCode) => {
    if (newCode) {
      // 1. 同步輸入框
      localInputCode.value = newCode;

      // 2. 💡 加上 nextTick 或 async，確保路由與 DOM 狀態完全就緒後才打 API
      // 這是解決「從 C 跳過來時資料死掉、但在 B 手動搜尋或 A 點選卻正常」的非同步時差良藥
      await nextTick();

      // 3. 呼叫你原本就想觸發的方法去撈最新價格
      console.log("=== 🚀 B 元件頂層 watch 成功觸發 fetchNewestPrice ===", newCode);
      stockStore.fetchNewestPrice(newCode);
    }
  },
  { immediate: true } // 確保 C 元件第一次 router.push 跳進來時，B 元件一掛載就能立刻執行
);


// 💡 修正關鍵：改用生命週期確保元件完全掛載後才執行監聽與計時器
onMounted(() => {

  // 監聽當前股票代碼，代碼一變就立刻抓取並重新啟動定時器、並重置價格
  watch(
    () => stockStore.currentStockCode,
    (newCode, oldCode) => {
      if (!newCode) {
        stopPolling();
        return;
      }

      // 切換股票時，不沿用上一支股票的價格
      if (newCode !== oldCode) {
        previousPrice.value = null;
        priceChange.value = 'flat';
      }

      stockStore.fetchNewestPrice(newCode);
      startPolling();
    },
    { immediate: true }
  );
});



// 💡 元件卸載（例如使用者切換到其他分頁）時，一定要把定時器關掉，否則會造成記憶體殘留與效能怪獸
onUnmounted(() => {
  stopPolling();
});

// // 🔍 監聽網址上的 query.code 變化
// watch(
//   () => route.query.code,
//   (newCode) => {
//     if (newCode) {

//       localInputCode.value = newCode;

//       // 直接呼叫 Pinia 去撈最新股票資料
//       stockStore.fetchNewestPrice(newCode);
//     }
//   },
//   { immediate: true }
// );
</script>

<style scoped>
/* 股票搜尋卡片 */

.stock-info-card {
  width: 100%;
  height: 100%;
  min-height: 126px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 28px;

  background: #ffffff;

  border-radius: 12px;

  box-sizing: border-box;
}


/* 左側 */

.stock-info-left {
  flex: 1;

  display: flex;
  flex-direction: column;

  gap: 8px;
}


/* 標題 */

.stock-title-area {
  display: flex;
  align-items: center;

  gap: 12px;
}

.stock-icon {
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

.stock-title-area h3 {
  margin: 0;

  color: #10233f;

  font-size: 20px;
  font-weight: 700;
}

.stock-title-area p {
  margin: 3px 0 0;

  color: #718096;

  font-size: 14px;
}


/* 已選股票 */

.selected-stock {
  display: flex;
  align-items: center;

  gap: 18px;

  padding-left: 56px;
}

.selected-stock-name {
  color: #10233f;

  font-size: 18px;
  font-weight: 700;
}


/* 最新價 */

.latest-price {
  display: flex;
  align-items: center;

  gap: 7px;
}

.price-label {
  color: #9ca3af;

  font-size: 13px;
}

.price-value {
  font-size: 16px;
  font-weight: 700;
}


/* 台股漲跌 */

.price-up {
  color: #d32f2f;
}

.price-down {
  color: #388e3c;
}

.price-flat {
  color: #757575;
}

.price-symbol {
  margin-right: 2px;

  font-size: 0.75em;
}


/* 搜尋區 */

.stock-search-area {
  width: 360px;

  display: flex;
  align-items: center;

  gap: 10px;

  flex-shrink: 0;
}

.search-input {
  flex: 1;
}

.search-input :deep(.v-field) {
  background: #fbfaff;

  border-radius: 8px;
}

.search-input :deep(.v-field:hover) {
  background: #f6f3ff;
}

.search-input :deep(.v-field--focused) {
  color: #f59e0b;
}

.search-icon {
  color: #a5adba;

  font-size: 19px;
}

.search-wrapper {
  position: relative;
  flex: 1;
}

.search-results {
  position: absolute;

  top: calc(100% + 4px);
  left: 0;
  right: 0;

  max-height: 280px;

  overflow-y: auto;

  background: #ffffff;

  border: 1px solid #e5e7eb;
  border-radius: 8px;

  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);

  z-index: 1000;
}

.search-result-item {
  display: flex;
  align-items: center;

  padding: 10px 14px;

  cursor: pointer;

  gap: 10px;
}

.search-result-item:hover {
  background: #f6f3ff;
}

.result-code {
  min-width: 55px;

  color: #10233f;

  font-weight: 700;
}

.result-name {
  color: #718096;

  font-size: 14px;
}


/* 搜尋按鈕 */

.search-btn {
  min-width: 72px;
  height: 40px !important;

  background: #fff1dc !important;

  border: 1px solid #f4c76c;

  border-radius: 8px;

  color: #d98200 !important;

  font-weight: 600;

  letter-spacing: normal;
  text-transform: none;

  box-shadow: none !important;

  transition: 0.2s;
}

.search-btn:hover {
  background: #ffe8bd !important;

  color: #d98200 !important;
}
</style>
