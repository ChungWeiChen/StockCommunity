<template>

  <div class="stock-market-page">

    <!-- 頁面標題 -->
    <div class="page-header">
      <h2>股票行情</h2>
      <p>查看個股歷史行情與價格走勢</p>
    </div>


    <!-- 錯誤訊息 -->
    <v-alert
      v-if="errorMessage"
      type="error"
      density="compact"
      class="mb-4"
    >
      {{ errorMessage }}
    </v-alert>


    <!-- 產業分類 -->
    <div class="market-card industry-card">

      <div class="section-title">

        <div class="section-icon">
          <span class="mdi mdi-view-grid-outline"></span>
        </div>

        <div>
          <h3>產業分類</h3>
          <p>選擇產業快速篩選股票</p>
        </div>

      </div>


      <div class="industry-buttons">

        <v-btn
          v-for="ind in visibleIndustries"
          :key="ind.id"
          size="small"
          variant="outlined"
          class="industry-btn"
          :class="{
            'industry-active': selectedIndustry === ind.id
          }"
          @click="handleIndustryChange(ind.id)"
        >
          {{ ind.name }}
        </v-btn>

      </div>

    </div>


    <!-- 下方行情區 -->
    <div
      ref="marketSection"
      class="market-layout"
    >


      <!-- 左側股票清單 -->
      <div class="market-card stock-list-card">

        <div class="section-title stock-title">

          <div class="section-icon">
            <span class="mdi mdi-format-list-bulleted"></span>
          </div>

          <div>
            <h3>股票清單</h3>
            <p>搜尋或選擇股票</p>
          </div>

        </div>


        <!-- 搜尋 -->
        <div class="stock-search">

          <v-text-field
            v-model="keyword"
            placeholder="搜尋股票代號或簡稱"
            density="compact"
            variant="outlined"
            hide-details
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prepend-inner>
              <span class="mdi mdi-magnify search-icon"></span>
            </template>
          </v-text-field>

          <v-btn
            class="search-btn"
            @click="handleSearch"
          >
            搜尋
          </v-btn>

        </div>


        <!-- 股票列表 -->
        <div class="stock-list-scroll">

          <div
            v-for="stock in stocks"
            :key="stock.stockCode"
            class="stock-item"
            :class="{
              'stock-active':
                currentStock?.stockCode === stock.stockCode
            }"
            @click="handleSelectStock(stock.stockCode)"
          >

            <span class="stock-code">
              {{ stock.stockCode }}
            </span>

            <span class="stock-name">
              {{ stock.simpleName }}
            </span>

            <span class="mdi mdi-chevron-right stock-arrow"></span>

          </div>


          <!-- 股票清單沒有資料 -->
          <div
            v-if="stocks.length === 0"
            class="stock-list-empty"
          >
            目前沒有符合條件的股票
          </div>

        </div>

      </div>


      <!-- 右側行情 -->
      <div class="market-card stock-detail-card">


        <!-- 已選擇股票 -->
        <template v-if="currentStock">

          <!-- 股票標題 -->
          <div class="stock-detail-header">

            <div>

              <div class="stock-main-title">

                <span class="detail-stock-code">
                  {{ currentStock.stockCode }}
                </span>

                <span class="detail-stock-name">
                  {{ currentStock.simpleName }}
                </span>

              </div>


              <div class="stock-meta">
                {{ currentStock.companyName }}
                ·
                {{ currentStock.market }}
                /
                {{ currentStockIndustryName }}
              </div>

            </div>


            <div class="industry-tag">
              {{ currentStockIndustryName }}
            </div>

          </div>


          <!-- 日期查詢 -->
          <div class="range-card">

            <div class="range-title">
              <span class="mdi mdi-calendar-range"></span>
              歷史行情區間
            </div>


            <div class="range-controls">

              <v-text-field
                v-model="startDate"
                type="date"
                label="開始日期"
                density="compact"
                variant="outlined"
                hide-details
                class="date-input"
              />

              <span class="date-separator">
                至
              </span>

              <v-text-field
                v-model="endDate"
                type="date"
                label="結束日期"
                density="compact"
                variant="outlined"
                hide-details
                class="date-input"
              />


              <div class="quick-buttons">

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(7)"
                >
                  週
                </v-btn>

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(30)"
                >
                  月
                </v-btn>

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(90)"
                >
                  3 月
                </v-btn>

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(365)"
                >
                  1 年
                </v-btn>

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(1095)"
                >
                  3 年
                </v-btn>

                <v-btn
                  size="small"
                  variant="outlined"
                  @click="handleQuickRange(1825)"
                >
                  5 年
                </v-btn>

              </div>


              <v-btn
                class="query-btn"
                @click="handleQueryRange"
              >
                查詢區間
              </v-btn>

            </div>

          </div>


          <!-- 圖表 -->
          <div class="chart-area">

            <PriceChart
              :price-history="priceHistory"
            />


            <div
              v-if="priceHistory.length === 0"
              class="chart-empty"
            >

              <span class="mdi mdi-chart-line empty-chart-icon"></span>

              <div class="empty-title">
                目前沒有歷史行情資料
              </div>

              <div class="empty-text">
                請調整日期區間後重新查詢
              </div>

            </div>

          </div>

        </template>


        <!-- 尚未選股票 -->
        <div
          v-else
          class="select-stock-empty"
        >

          <div class="empty-stock-icon">
            <span class="mdi mdi-chart-line"></span>
          </div>

          <h3>選擇一檔股票</h3>

          <p>
            從左側股票清單選擇股票，即可查看歷史行情
          </p>

        </div>

      </div>

    </div>

  </div>

</template>


<script setup>
import { onActivated, onMounted, computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useStockStore } from '@/stores/stockStore'
import PriceChart from '@/components/Stock/PriceChart.vue'

const route = useRoute()
const stockStore = useStockStore()
const { stocks, currentStock, priceHistory, errorMessage, industries } = storeToRefs(stockStore)
const selectedIndustry = ref(null) // 目前選擇的產業別代號，null 代表「全部」
const keyword = ref('')

//PZ加:
const marketSection = ref(null)
//


// 9/22確認：「綜合」這個產業別分類目前篩出來完全沒有股票，畫面只會顯示「這個篩選條件下目前沒有股票」，
// 這種按了也篩不出東西的按鈕沒必要留著，先手動排除；之後如果發現其他產業別也是同樣情況，直接加進這個陣列即可
const EMPTY_INDUSTRIES = ['綜合', '電子工業', '文化創意業', '農業科技業']
const visibleIndustries = computed(() =>
  industries.value.filter((ind) => !EMPTY_INDUSTRIES.includes(ind.name))
)
const startDate = ref('')
const endDate = ref('')

const syncCurrentStock = () => {
  const code = route.query.code;

  if (code) {
    stockStore.selectStock(code);
  }
};


onMounted(() => {
  stockStore.fetchAllStocks();
  stockStore.fetchIndustries();

  syncCurrentStock();
});


watch(
  [currentStock, industries],
  () => {
    console.log("currentStock.industry =", currentStock.value?.industry);
    console.log("industries =", industries.value);
  },
  { deep: true }
);


onActivated(() => {
  console.log("=== 🔵 A 元件 onActivated 觸發 ===");
  console.log("此時網址的 query.code:", route.query.code, route.query.code.type);
  syncCurrentStock();
});


let searchTimer = null

// 旗標，標記「keyword 被清空是 handleIndustryChange 自己清的」
// 避免點產業別按鈕時，清空搜尋框連帶觸發下面的 watch 又多打一次 fetchAllStocks，
// 跟 handleIndustryChange 自己要打的篩選 API 互相搶結果，造成畫面顯示不穩定
let isClearingKeywordFromIndustryChange = false

watch(keyword, (newValue) => {
  if (isClearingKeywordFromIndustryChange) {
    isClearingKeywordFromIndustryChange = false
    return
  }

  // 新增：使用者自己打字搜尋時，順便清掉產業別按鈕的選中狀態，
  // 避免搜尋結果跟畫面上還亮著的產業別按鈕對不起來（跟 handleSearch 那邊同理）
  selectedIndustry.value = null

  clearTimeout(searchTimer)

  if (newValue === '') {
    stockStore.fetchAllStocks()
    return
  }

  searchTimer = setTimeout(() => {
    stockStore.searchStocks(newValue)
  }, 300)
})


//PZ加:
const scrollToMarketSection = () => {
  setTimeout(() => {
    marketSection.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }, 100)
}
//


// const handleSelectStock = (stockCode) => {
//   stockStore.selectStock(stockCode)
//   window.scrollTo({ top: 0, behavior: 'smooth' })
// }

//PZ從上面的方法改:
const handleSelectStock = (stockCode) => {
  stockStore.selectStock(stockCode)
  scrollToMarketSection()
}
//


const handleSearch = () => {
  // 新增：按搜尋鈕時，順便清掉產業別按鈕的選中狀態
  selectedIndustry.value = null
  stockStore.searchStocks(keyword.value)
}

const handleIndustryChange = (industryId) => {
  currentStock.value = null

  // 只有搜尋框真的有內容時，才需要清空、也才需要設旗標
  // 如果本來就是空字串，設成空字串不會觸發下面的 watch，
  // 旗標會卡在 true 出不來，導致下一次真正打字時被錯誤跳過（這正是你剛才測到的狀況）
  if (keyword.value !== '') {
    isClearingKeywordFromIndustryChange = true
    keyword.value = ''
  }

  if (selectedIndustry.value === industryId) {
    selectedIndustry.value = null
    stockStore.fetchAllStocks()
  } else {
    selectedIndustry.value = industryId
    stockStore.filterStocksByIndustry(industryId)
  }

  //PZ加:
  scrollToMarketSection()
  //
}

const currentStockIndustryName = computed(() => {
  const found = industries.value.find((i) => i.id === currentStock.value?.industry)
  return found ? found.name : currentStock.value?.industry
})

const handleQueryRange = () => {
  if (!currentStock.value) return

  if (!startDate.value || !endDate.value) {
    errorMessage.value = '請選擇開始與結束日期'
    return
  }

  stockStore.fetchPriceHistoryByRange(currentStock.value.stockCode, startDate.value, endDate.value)
}

const handleQuickRange = (days) => {
  if (!currentStock.value) return

  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - days)

  const toDateString = (date) => date.toISOString().split('T')[0]

  startDate.value = toDateString(start)
  endDate.value = toDateString(end)

  handleQueryRange()
}
</script>



<style scoped>

/* 整體 */

.stock-market-page {
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


/* 共用卡片 */

.market-card {
  background: #ffffff;

  border: 1px solid #eee9df;
  border-radius: 12px;

  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.035);
}


/* 區塊標題 */

.section-title {
  display: flex;
  align-items: center;

  gap: 12px;
}

.section-title h3 {
  margin: 0;

  color: #10233f;

  font-size: 19px;
  font-weight: 700;
}

.section-title p {
  margin: 3px 0 0;

  color: #718096;

  font-size: 14px;
}

.section-icon {
  width: 44px;
  height: 44px;

  display: flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;

  background: #fff3dc;

  border-radius: 50%;

  color: #e99a00;

  font-size: 23px;
}


/* 產業分類 */

.industry-card {
  padding: 22px;

  margin-bottom: 18px;
}

.industry-card .section-title {
  margin-bottom: 18px;
}

.industry-buttons {
  display: flex;
  flex-wrap: wrap;

  gap: 9px;
}


/* 一般產業 */

.industry-btn {
  min-height: 34px;

  padding: 0 14px !important;

  background: #ffffff !important;

  border: 1px solid #e4d9c7 !important;
  border-radius: 8px !important;

  color: #4b5563 !important;

  font-size: 14px;
  font-weight: 500;

  letter-spacing: normal;
  text-transform: none;

  transition: 0.2s;
}


/* hover 淺紫 */

.industry-btn:hover {
  background: #f1edff !important;

  border-color: #d8cef7 !important;
}


/* 選中的產業 */

.industry-btn.industry-active {
  background: #fff3dc !important;

  border-color: #f3c66f !important;

  color: #d98200 !important;

  font-weight: 600;
}


/* 下方左右布局 */

.market-layout {
  display: grid;

  grid-template-columns: 280px minmax(0, 1fr);

  gap: 18px;

  align-items: stretch;
  scroll-margin-top: 90px;
}


/* 左側股票清單 */

.stock-list-card {
  padding: 20px;

  min-width: 0;
}

.stock-title {
  margin-bottom: 18px;
}


/* 搜尋 */

.stock-search {
  display: flex;
  align-items: center;

  gap: 8px;

  margin-bottom: 16px;
}

.search-input {
  flex: 1;

  min-width: 0;
}

.search-input :deep(.v-field) {
  border-radius: 8px;
}

.search-input :deep(.v-field--focused) {
  color: #e99a00;
}

.search-icon {
  color: #9ca3af;

  font-size: 19px;
}


/* 搜尋按鈕 */

.search-btn {
  min-width: 62px;
  height: 40px !important;

  background: #fff3dc !important;

  border: 1px solid #f3c66f;
  border-radius: 8px;

  color: #d98200 !important;

  font-size: 14px;
  font-weight: 600;

  letter-spacing: normal;
  text-transform: none;

  box-shadow: none !important;

  transition: 0.2s;
}

.search-btn:hover {
  background: #ffe9c2 !important;
}


/* 股票清單 */

.stock-list-scroll {
  max-height: 430px;

  overflow-y: auto;

  border: 1px solid #eeeaf5;
  border-radius: 9px;
}


/* 股票 */

.stock-item {
  min-height: 52px;

  display: flex;
  align-items: center;

  padding: 0 13px;

  background: #fbfaff;

  border-bottom: 1px solid #eeeaf5;

  cursor: pointer;

  transition: background-color 0.2s;
}

.stock-item:last-child {
  border-bottom: none;
}


/* hover 淺紫 */

.stock-item:hover {
  background: #f1edff;
}


/* 選中的股票 */

.stock-item.stock-active {
  background: #fff3dc;
}

.stock-code {
  width: 64px;

  flex-shrink: 0;

  color: #24364d;

  font-size: 14px;
  font-weight: 700;
}

.stock-name {
  flex: 1;

  color: #4b5563;

  font-size: 14px;
}

.stock-arrow {
  color: #c1b8d9;

  font-size: 19px;
}

.stock-active .stock-code,
.stock-active .stock-name {
  color: #c98000;
}

.stock-active .stock-arrow {
  color: #e99a00;
}


/* scrollbar捲軸 */

.stock-list-scroll::-webkit-scrollbar {
  width: 6px;
}

.stock-list-scroll::-webkit-scrollbar-thumb {
  background: #ddd6ee;

  border-radius: 10px;
}

.stock-list-scroll::-webkit-scrollbar-track {
  background: transparent;
}


/* 沒有搜尋結果 */

.stock-list-empty {
  padding: 30px 15px;

  color: #9ca3af;

  font-size: 14px;

  text-align: center;
}


/* 右側行情 */

.stock-detail-card {
  min-width: 0;
  max-height: 600px;
  padding: 22px;
}


/* 股票資訊 */

.stock-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 20px;

  margin-bottom: 20px;
}

.stock-main-title {
  display: flex;
  align-items: baseline;

  gap: 10px;
}

.detail-stock-code {
  color: #10233f;

  font-size: 24px;
  font-weight: 700;
}

.detail-stock-name {
  color: #10233f;

  font-size: 21px;
  font-weight: 700;
}

.stock-meta {
  margin-top: 4px;

  color: #8b95a5;

  font-size: 14px;
}


/* 右上產業標籤 */

.industry-tag {
  padding: 6px 14px;

  flex-shrink: 0;

  background: #fff3dc;

  border-radius: 20px;

  color: #c98000;

  font-size: 13px;
  font-weight: 600;
}


/* 歷史區間 */

.range-card {
  padding: 16px;

  margin-bottom: 20px;

  background: #fbfaff;

  border: 1px solid #eeeaf5;
  border-radius: 9px;
}

.range-title {
  display: flex;
  align-items: center;

  gap: 7px;

  margin-bottom: 14px;

  color: #4b5563;

  font-size: 14px;
  font-weight: 600;
}

.range-title .mdi {
  color: #e99a00;

  font-size: 19px;
}

.range-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  gap: 9px;
}

.date-input {
  max-width: 165px;
}

.date-input :deep(.v-field) {
  background: #ffffff;

  border-radius: 8px;
}

.date-input :deep(.v-field--focused) {
  color: #e99a00;
}

.date-separator {
  color: #718096;

  font-size: 14px;
  font-weight: 600;
}


/* 快速日期 */

.quick-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  gap: 6px;
}

.quick-buttons :deep(.v-btn) {
  min-width: 48px;

  background: #ffffff;

  border-color: #d8cef7;

  border-radius: 7px;

  color: #6d50d7;

  font-size: 13px;

  letter-spacing: normal;
  text-transform: none;

  transition: 0.2s;
}

.quick-buttons :deep(.v-btn:hover) {
  background: #f1edff;

  border-color: #c4b6ef;
}


/* 查詢按鈕 */

.query-btn {
  min-width: 88px;

  background: #fff3dc !important;

  border: 1px solid #f3c66f;
  border-radius: 8px;

  color: #d98200 !important;

  font-size: 14px;
  font-weight: 600;

  letter-spacing: normal;
  text-transform: none;

  box-shadow: none !important;

  transition: 0.2s;
}

.query-btn:hover {
  background: #ffe9c2 !important;
}


/* 圖表 */

.chart-area {
  width: 100%;
  max-height: 360px;
  display: flex;
  justify-content: center;
}


/* 尚未選股票 */

.select-stock-empty {
  min-height: 520px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text-align: center;
}

.empty-stock-icon {
  width: 72px;
  height: 72px;

  display: flex;
  align-items: center;
  justify-content: center;

  margin-bottom: 16px;

  background: #fff3dc;

  border-radius: 50%;

  color: #e99a00;

  font-size: 36px;
}

.select-stock-empty h3 {
  margin: 0 0 7px;

  color: #4b5563;

  font-size: 19px;
  font-weight: 600;
}

.select-stock-empty p {
  margin: 0;

  color: #9ca3af;

  font-size: 14px;
}


/* 沒有行情 */

.chart-empty {
  min-height: 260px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text-align: center;
}

.empty-chart-icon {
  margin-bottom: 10px;

  color: #e5c98e;

  font-size: 48px;
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
