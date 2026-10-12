<template>
  <div class="watchlist-page">

    <v-row class="watchlist-layout">

      <!-- 左側：關注清單 -->
      <v-col cols="12" md="4">

        <div class="watchlist-card">

          <div class="section-title">
            <span class="mdi mdi-heart-outline section-icon"></span>
            <h2>關注清單</h2>
          </div>

          <!-- 建立新清單 -->
          <v-form
            @submit.prevent="handleCreateWatchlist"
            class="create-watchlist"
          >
            <v-text-field
              v-model="newWatchlistName"
              placeholder="輸入清單名稱..."
              variant="outlined"
              density="comfortable"
              hide-details
              class="watchlist-input"
            />

            <v-btn
              type="submit"
              class="create-btn"
              elevation="0"
            >
              建立
            </v-btn>
          </v-form>

          <!-- 清單列表 -->
          <div class="watchlist-list">

            <div
              v-for="watchlist in watchlists"
              :key="watchlist.id"
              class="watchlist-item"
              :class="{
                active: selectedWatchlistId === watchlist.id
              }"
              @click="selectWatchlist(watchlist.id)"
            >

              <div class="watchlist-item-left">

                <span
                  class="mdi mdi-heart-outline watchlist-item-icon"
                ></span>

                <span class="watchlist-name">
                  {{ watchlist.name }}
                </span>

              </div>

            </div>

          </div>

        </div>

      </v-col>


      <!-- 右側：清單內容  -->
      <v-col cols="12" md="8">

        <div class="watchlist-card content-card">

          <div class="section-title">
            <span class="mdi mdi-format-list-bulleted section-icon"></span>
            <h2>清單內容</h2>
          </div>


          <!-- 尚未選擇清單 -->
          <div
            v-if="!selectedWatchlistId"
            class="empty-watchlist"
          >

            <div class="empty-icon">
              <span class="mdi mdi-clipboard-list-outline"></span>
              <span class="mdi mdi-heart empty-heart"></span>
            </div>

            <div class="empty-title">
              請先選擇左側的關注清單
            </div>

            <div class="empty-text">
              選擇清單後即可搜尋並加入股票
            </div>

          </div>


          <!-- 已選擇清單 -->
          <template v-else>

            <!-- 清單資訊 -->
            <div class="selected-list-header">

              <div>
                <div class="selected-list-title">

                  <span class="mdi mdi-trending-up"></span>

                  {{ selectedWatchlist?.name }}

                  <span class="stock-count">
                    {{ currentItems.length }} 檔股票
                  </span>

                </div>

                <div class="selected-list-description">
                  管理你感興趣的股票，隨時追蹤觀察。
                </div>
              </div>

            </div>


            <!-- 股票搜尋 -->
            <div class="stock-search">

              <v-autocomplete
                v-model="newStockCode"
                :items="stockOptions"
                item-title="title"
                item-value="value"
                placeholder="搜尋股票代號或名稱，例如：2330、台積電"
                variant="outlined"
                density="comfortable"
                hide-details
                prepend-inner-icon="mdi-magnify"
                class="stock-autocomplete"
              />

              <v-btn
                class="add-stock-btn"
                elevation="0"
                @click="handleAddItem"
              >
                加入
              </v-btn>

            </div>


            <!-- 股票列表 -->
            <div class="stock-list">

              <div
                v-for="item in currentItems"
                :key="item.id"
                class="stock-item"
              >

                <div class="stock-info">

                  <span class="stock-code">
                    {{ item.stockCode }}
                  </span>

                  <span class="stock-name">
                    {{ stockNameMap[item.stockCode] || "" }}
                  </span>

                </div>


                <div class="stock-actions">

                  <v-btn
                    size="small"
                    variant="text"
                    class="market-btn"
                    @click="goToStockView(item.stockCode)"
                  >
                    查看行情
                  </v-btn>

                  <v-btn
                    size="small"
                    variant="text"
                    class="order-btn"
                    @click="goToOrder(item.stockCode)"
                  >
                    下單
                  </v-btn>

                  <v-btn
                    icon="mdi-close"
                    size="small"
                    variant="text"
                    class="remove-btn"
                    @click="handleRemoveItem(item.id)"
                  />

                </div>

              </div>


              <!-- 清單沒有股票 -->
              <div
                v-if="currentItems.length === 0"
                class="no-stock"
              >
                這個清單目前還沒有股票
              </div>

            </div>

          </template>

        </div>

      </v-col>

    </v-row>


    <v-alert
      v-if="errorMessage"
      type="error"
      density="compact"
      class="mt-4"
    >
      {{ errorMessage }}
    </v-alert>

  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { useWatchlistStore } from "@/stores/watchlistStore";
import { useStockStore } from "@/stores/stockStore";
import { useAuthStore } from "@/stores/authStore";

const router = useRouter();
const authStore = useAuthStore();

const watchlistStore = useWatchlistStore();
// State 用 storeToRefs 解構，保持響應性
const { watchlists, currentItems, errorMessage } = storeToRefs(watchlistStore);

// 借用 stockStore 的股票清單，讓加股票這裡可以用下拉選單，不用使用者自己記代號
const stockStore = useStockStore();
const { stocks } = storeToRefs(stockStore);

// 把股票清單轉成「代號 簡稱」的顯示格式，這樣 v-autocomplete 打代號或打簡稱都篩得到，
// 不然預設只會拿使用者打的字去比對顯示文字(原本只有simpleName)，打代號完全比對不到
const stockOptions = computed(() =>
  stocks.value.map((stock) => ({
    title: `${stock.stockCode} ${stock.simpleName}`,
    value: stock.stockCode,
  }))
);

// 建立「代號→簡稱」對照表，清單內容那邊只有stockCode，靠這個表查出對應簡稱顯示
const stockNameMap = computed(() => {
  const map = {};
  stocks.value.forEach((stock) => {
    map[stock.stockCode] = stock.simpleName;
  });
  return map;
});

// 查代號對應的顯示文字；萬一stocks還沒載完查不到名稱，先顯示代號本身，不要整個空白
const getStockLabel = (stockCode) => {
  const name = stockNameMap.value[stockCode];
  return name ? `${stockCode} ${name}` : stockCode;
};

const newWatchlistName = ref("");
const newStockCode = ref("");
const selectedWatchlistId = ref(null); // 記錄目前點開的是哪張清單

// PZ : 目前選中的關注清單
const selectedWatchlist = computed(() => {
  return watchlists.value.find(
    (watchlist) => watchlist.id === selectedWatchlistId.value
  );
});
//



// 進頁面就先拿一次「我的所有清單」，同時拿全部股票清單給下拉選單用
// 股票清單本身是公開資料，沒登入也能看；但「我的清單」是個人資料，改成先判斷有沒有登入才打API，
// 沒登入就不打，讓畫面改顯示「請先登入」提示，而不是被401悄悄彈回登入頁
onMounted(() => {
  stockStore.fetchAllStocks();
  if (authStore.isLoggedIn) {
    watchlistStore.fetchMyWatchlists();
  }
});

// 跳去登入頁，並記住目前這頁，登入完導回來
const goToLogin = () => {
  router.push({ path: "/login", query: { redirect: router.currentRoute.value.fullPath } });
};

const handleCreateWatchlist = async () => {
  // 建立清單本身不需要看到別人的資料，只是「新增一筆屬於我的紀錄」這個動作才要求登入，
  // 跟五檔下單頁「先能看頁面，送出委託才要登入」是同一個道理
  if (!authStore.isLoggedIn) {
    alert("您還沒有登入會員，請先登入會員。");
    goToLogin();
    return;
  }
  if (!newWatchlistName.value.trim()) return;
  await watchlistStore.createWatchlist(newWatchlistName.value.trim());
  newWatchlistName.value = "";
};

// 點清單名稱時：記住選了哪張、去拿這張清單裡的股票
const selectWatchlist = (watchlistId) => {
  selectedWatchlistId.value = watchlistId;
  watchlistStore.fetchWatchlistItems(watchlistId);
};

const handleAddItem = async () => {
  if (!newStockCode.value || !selectedWatchlistId.value) return;
  await watchlistStore.addItem(selectedWatchlistId.value, newStockCode.value);
  newStockCode.value = "";
};

const handleRemoveItem = async (itemId) => {
  await watchlistStore.removeItem(itemId);
};

// 跳到股票行情頁看這檔股票的走勢，用 query 帶股票代號過去，StockView 那邊會自動選定
const goToStockView = (stockCode) => {
  router.push({ path: "/stock", query: { code: stockCode } });
};

// 跳到下單頁，用 query 帶股票代號過去，OrderView 那邊會自動把代號填進下單表單
const goToOrder = (stockCode) => {
  // 💡 關鍵修正：強制轉成字串，消除 Number 與 String 的比對誤差
  const stringCode = String(stockCode);

  // 直接改值（維持你最喜歡的乾淨改值方式）
  stockStore.currentStockCode = stringCode;

  // 導頁時也攜帶純字串的 query
  router.push({ path: "/order", query: { code: stringCode } });
};
</script>



<style scoped>

/* 整體 */

.watchlist-page {
  width: 100%;
}

.watchlist-layout {
  align-items: stretch;
}


/* 卡片 */

.watchlist-card {
  height: 100%;
  min-height: 430px;
  padding: 22px;

  background: #ffffff;

  border: 1px solid #eee9df;
  border-radius: 12px;

  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}


/* 標題 */

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;

  margin-bottom: 20px;
}

.section-title h2 {
  margin: 0;

  font-size: 24px;
  font-weight: 700;

  color: #374151;
}

.section-icon {
  font-size: 27px;
  color: #e5a72c;
}


/* 建立清單 */

.create-watchlist {
  display: flex;
  gap: 10px;

  margin-bottom: 18px;
}

.watchlist-input {
  flex: 1;
}

.create-btn {
  height: 48px;

  padding: 0 24px;

  background: #e9aa32;
  color: #ffffff;

  font-weight: 600;
  font-size: 16px;

  text-transform: none;
}

.create-btn:hover {
  background: #dc9c25;
}


/* 左側清單 */

.watchlist-list {
  display: flex;
  flex-direction: column;
  gap: 7px;

  max-height: 392px;
  overflow-y: auto;
}

.watchlist-item {
  display: flex;
  align-items: center;

  min-height: 50px;
  padding: 0 15px;

  border-radius: 8px;

  cursor: pointer;

  transition: background-color 0.2s ease;
}

.watchlist-item:hover {
  background: #fff9ea;
}

.watchlist-item.active {
  background: #fff1cc;
}

.watchlist-item-left {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
}

.watchlist-item-icon {
  font-size: 22px;
  color: #dba12f;
}

.watchlist-item-cancel {
  font-size: 22px;
  color: #f44336;
  margin-left: auto;
}

.watchlist-item-cancel:hover {
  transform: translateY(-2px);
}

.watchlist-name {
  color: #374151;

  font-size: 16px;
  font-weight: 500;
}


/* 未選清單 */

.empty-watchlist {
  min-height: 310px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text-align: center;
}

.empty-icon {
  position: relative;

  margin-bottom: 18px;

  font-size: 68px;
  line-height: 1;

  color: #edc46f;
}

.empty-heart {
  position: absolute;

  right: -8px;
  bottom: 0;

  font-size: 27px;

  color: #e6a62c;
}

.empty-title {
  margin-bottom: 8px;

  font-size: 20px;
  font-weight: 600;

  color: #374151;
}

.empty-text {
  font-size: 15px;

  color: #9ca3af;
}


/* 已選擇的清單 */

.selected-list-header {
  margin-bottom: 18px;
}

.selected-list-title {
  display: flex;
  align-items: center;
  gap: 9px;

  font-size: 21px;
  font-weight: 700;

  color: #374151;
}

.selected-list-title > .mdi {
  color: #dfa126;
}

.stock-count {
  padding: 4px 10px;

  background: #fff3d5;

  border-radius: 20px;

  color: #c98c1d;

  font-size: 13px;
  font-weight: 600;
}

.selected-list-description {
  margin-top: 5px;
  margin-left: 31px;

  color: #9a9a9a;

  font-size: 14px;
}


/* 股票搜尋 */

.stock-search {
  display: flex;
  align-items: center;

  gap: 10px;

  margin-bottom: 16px;
}

.stock-autocomplete {
  flex: 1;
}

.add-stock-btn {
  height: 48px;

  padding: 0 25px;

  background: #e9aa32;
  color: #ffffff;

  font-size: 16px;
  font-weight: 600;

  text-transform: none;
}

.add-stock-btn:hover {
  background: #dc9c25;
}


/* 股票列表 */

.stock-list {
  overflow: hidden;
  max-height: 320px;
  overflow-y: auto;

  border: 1px solid #f0ede7;
  border-radius: 9px;
}

.stock-item {
  min-height: 64px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 0 16px;

  border-bottom: 1px solid #f0ede7;
}

.stock-item:last-child {
  border-bottom: none;
}

.stock-item:hover {
  background: #fdfcf9;
}

.stock-info {
  display: flex;
  align-items: center;

  gap: 14px;
}

.stock-code {
  min-width: 50px;

  color: #374151;

  font-size: 17px;
  font-weight: 700;
}

.stock-name {
  color: #374151;

  font-size: 16px;
  font-weight: 500;
}


/* 股票操作 */

.stock-actions {
  display: flex;
  align-items: center;

  gap: 4px;
}

.market-btn {
  color: #374151;
}

.order-btn {
  color: #2563c7;
}

.remove-btn {
  color: #6b7280;
}


/* 空股票清單 */

.no-stock {
  padding: 45px 20px;

  text-align: center;

  color: #9ca3af;

  font-size: 15px;
}


</style>
