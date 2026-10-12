<template>

  <div class="five-level-price-container">

    <!-- 標題 -->
    <div class="panel-header">

      <div class="panel-title-area">
        <div class="panel-icon">
          <span class="mdi mdi-format-list-numbered"></span>
        </div>

        <div>
          <h3>五檔報價</h3>
          <p>點擊價格可快速帶入下單</p>
        </div>
      </div>

      <div class="live-badge">
        即時報價
      </div>

    </div>


    <div class="depth-column">

      <!-- 欄位名稱 -->
      <div class="side-header">
        <span class="price">價格</span>
        <span class="vol">數量</span>
      </div>


      <!-- 賣盤 -->

      <div class="side-category ask-title">
        <span>賣盤</span>
      </div>

      <div
        v-for="(item, index) in priceList.asks"
        :key="'ask-' + index"
        class="price-row ask"
        :class="{ 'is-empty': item.price === '--' }"
        @click="onPriceClick(item.price, 'BUY')"
      >
        <span class="price">
          {{ formatNumber(item.price, null, '--') }}
        </span>

        <span class="vol">
          {{ item.volume }}
        </span>
      </div>


      <!-- 買盤 -->

      <div class="side-category bid-title">
        <span>買盤</span>
      </div>

      <div
        v-for="(item, index) in priceList.bids"
        :key="'bid-' + index"
        class="price-row bid"
        :class="{ 'is-empty': item.price === '--' }"
        @click="onPriceClick(item.price, 'SELL')"
      >
        <span class="price">
          {{ formatNumber(item.price, null, '--') }}
        </span>

        <span class="vol">
          {{ item.volume }}
        </span>
      </div>

    </div>

  </div>

</template>

<script setup>
import { computed, watch } from "vue";
import { onMounted, onUnmounted } from "vue"; // 💡 引入生命週期鉤子
import { storeToRefs } from "pinia";
import { useMarketDepthStore } from "@/stores/marketDepthStore";
import { useStockStore } from '@/stores/stockStore'
import { formatNumber } from '@/utils/format' // 💡 引入工具

const stockStore = useStockStore();
const markerDepthStore = useMarketDepthStore();

const { formattedPriceList } = storeToRefs(markerDepthStore);

// 💡 改良後的資料處理：不再合併成 10 筆，而是分別輸出 5 筆
const priceList = computed(() => {
  const rawArray = Array.isArray(formattedPriceList.value) ? formattedPriceList.value : [];

  const sells = rawArray.filter(item => item.type === 'sell');
  const buys = rawArray.filter(item => item.type === 'buy');

  // 賣檔 5 筆
  const asks = Array.from({ length: 5 }, (_, i) => {
    const item = sells[i];
    return {
      price: item ? item.price : '--',
      volume: item ? item.quantity : '--'
    };
  });

  // 買檔 5 筆
  const bids = Array.from({ length: 5 }, (_, i) => {
    const item = buys[i];
    return {
      price: item ? item.price : '--',
      volume: item ? item.quantity : '--'
    };
  });

  return { asks, bids };
});

// // 監聽路由
// watch(
//   () => route?.query?.code,
//   (newCode) => {
//     if (!newCode) return;
//     stockStore.currentStockCode = newCode;
//     markerDepthStore.fetchMarketDepth(newCode);
//   },
//   { immediate: true }
// );

// // 監聽按鈕/全域
// watch(
//   () => stockStore.currentStockCode,
//   (newCode) => {
//     if (!newCode) return;
//     markerDepthStore.fetchMarketDepth(newCode);
//   }
// );
// 在 PriceLevel 點擊某一列時觸發
const onPriceClick = (clickedPrice, suggestedOrderType) => {
  // 防呆：如果是空資料 '--' 則不理會
  if (clickedPrice === '--') return;

  // 💡 直接調用剛才在 window 註冊的方法，把價格帶過去下單匣
  if (typeof window.__updateOrderPadPrice === 'function') {
    window.__updateOrderPadPrice(clickedPrice, suggestedOrderType);
  }
};


// 輪巡，定期重整五檔報價
let pollingTimer = null;

// 開始輪詢的函式
const startPolling = () => {
  stopPolling(); // 確保不會重複重複建立定時器
  pollingTimer = setInterval(() => {
    if (stockStore.currentStockCode) {
      // 💡 每隔 2 秒，自動默默在背景呼叫你的 Store Action 去重新抓取最新五檔
      markerDepthStore.fetchMarketDepth(stockStore.currentStockCode);
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


// 💡 修正關鍵：改用生命週期確保元件完全掛載後才執行監聽與計時器
onMounted(() => {
  // 1. 監聽 URL 路由參數變更
  // watch(
  //   () => route?.query?.code,
  //   (newCode) => {
  //     if (!newCode) return;
  //     stockStore.currentStockCode = newCode;
  //   },
  //   { immediate: true }
  // );


  // 2. 監聽當前股票代碼，代碼一變就立刻抓取並重新啟動定時器
  watch(
    () => stockStore.currentStockCode,
    (newCode) => {
      if (!newCode) {
        stopPolling();
        return;
      }
      // 立刻手動刷新第一次
      markerDepthStore.fetchMarketDepth(newCode);
      // 啟動每兩秒的循環
      startPolling();
    },
    { immediate: true }
  );
});

// 💡 元件卸載（例如使用者切換到其他分頁）時，一定要把定時器關掉，否則會造成記憶體殘留與效能怪獸
onUnmounted(() => {
  stopPolling();
});

</script>


<style scoped>

/* 五檔報價卡片 */

.five-level-price-container {
  width: 100%;
  height: 100%;
  min-height: 480px;

  display: flex;
  flex-direction: column;

  background: #ffffff;

  box-sizing: border-box;
}


/* 標題 */

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.panel-title-area {
  display: flex;
  align-items: center;

  gap: 11px;
}

.panel-icon {
  width: 42px;
  height: 42px;

  display: flex;
  align-items: center;
  justify-content: center;

  background: #fff1dc;

  border-radius: 50%;

  color: #f59e0b;

  font-size: 21px;
}

.panel-title-area h3 {
  margin: 0;

  color: #10233f;

  font-size: 19px;
  font-weight: 700;
}

.panel-title-area p {
  margin: 2px 0 0;

  color: #8b95a5;

  font-size: 13px;
}


/* 即時報價 */

.live-badge {
  padding: 5px 10px;

  background: #fff3dc;

  border: 1px solid #f3d39a;
  border-radius: 20px;

  color: #d98200;

  font-size: 12px;
  font-weight: 600;
}


/* 五檔內容 */

.depth-column {
  flex: 1;

  display: flex;
  flex-direction: column;
}


/* 欄位標題 */

.side-header {
  display: flex;
  align-items: center;

  padding: 10px 14px;

  background: #fbfaff;

  border-bottom: 1px solid #ebe7f5;
  border-radius: 7px 7px 0 0;

  color: #718096;

  font-size: 13px;
  font-weight: 600;
}


/* 買賣盤標題 */

.side-category {
  padding: 7px 14px;

  font-size: 13px;
  font-weight: 700;
}

.ask-title {
  color: #2e8b57;

  background: #f5fbf7;

  border-left: 3px solid #22c55e;
}

.bid-title {
  margin-top: 4px;

  color: #dc4545;

  background: #fff7f7;

  border-left: 3px solid #ef4444;
}


/* 五檔列*/

.price-row {
  flex: 1;

  min-height: 34px;

  display: flex;
  align-items: center;

  padding: 0 14px;

  border-bottom: 1px solid #f0edf5;

  cursor: pointer;

  font-size: 14px;

  transition: background-color 0.15s ease;
}


.price-row:hover:not(.is-empty) {
  background: #f1edff;
}


/* 欄位寬度 */

.price {
  width: 38%;

  text-align: right;
}

.vol {
  width: 38%;

  text-align: right;
}


/* 賣盤 */

.ask .price,
.ask .vol {
  color: #22a35a;

  font-weight: 600;
}

.ask {
  background: #f5fbf7;
}

/* 買盤 */

.bid .price,
.bid .vol {
  color: #ef4444;

  font-weight: 600;
}

.bid {
  background: #fff7f7;
}

/* 空資料 */

.price-row.is-empty {
  cursor: default;
}

.price-row.is-empty .price,
.price-row.is-empty .vol {
  color: #a5adba !important;

  font-weight: 400;
}

</style>
