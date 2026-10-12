<template>
  <div class="find-stock">

    <!-- 搜尋區 -->
    <div class="search-area">

      <div class="input-area">

        <input
          ref="stockInput"
          v-model="stockKeyword"
          type="text"
          placeholder="搜尋股票代號或名稱"
          @input="showResults = true"
          @keyup.enter="search"
        >

        <button
          v-if="stockKeyword"
          type="button"
          class="cancel-button"
          @click="cancelSearch"
        >
          <span class="mdi mdi-close"></span>
        </button>

      </div>

      <button
        type="button"
        class="publish-button"
        @click="search"
      >
        <span class="mdi mdi-magnify"></span>
        搜尋
      </button>

    </div>


    <!-- 搜尋結果 -->
    <div
      v-if="showResults && stockResults.length > 0"
      class="stock-results"
    >
      <div
        v-for="stock in stockResults"
        :key="stock.stockCode"
        class="stock-result"
        @click="selectStock(stock)"
      >
        <span class="mdi mdi-tag"></span>

        {{ stock.stockCode }} {{ stock.simpleName }}
      </div>
    </div>
  </div>

</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { storeToRefs } from "pinia";
import { useStockStore } from "@/stores/stockStore";

const stockStore = useStockStore();

const { stocks } = storeToRefs(stockStore);

const props = defineProps({
  stockCode: {
    type: String,
    default: ""
  },
  stockName: {
    type: String,
    default: ""
  }
});

const stockKeyword = ref(props.stockCode);

const stockInput = ref(null);

const showResults = ref(true);

const emit = defineEmits(["select-stock", "clear-stock"]);


onMounted(() => {
  stockStore.fetchAllStocks();

  stockInput.value?.focus();
});

watch(
  () => [props.stockCode, props.stockName],
  ([stockCode, stockName]) => {

    if (stockCode) {

      stockKeyword.value = stockName
        ? `${stockCode} ${stockName}`
        : stockCode;

      showResults.value = false;

    } else {

      stockKeyword.value = "";
      showResults.value = false;

      stockInput.value?.focus();
    }

  },
  { immediate: true }
);


const stockResults = computed(() => {

  const keyword = stockKeyword.value.trim();

  if (!keyword) {
    return [];
  }

  return stocks.value.filter((stock) => {

    // 股票代號：必須從 keyword 開頭
    const codeMatch =
      stock.stockCode.startsWith(keyword);

    // 股票名稱：只要包含 keyword
    const nameMatch =
      stock.simpleName.includes(keyword);

    return codeMatch || nameMatch;
  });

});

function search() {
  if (stockResults.value.length === 0) {
    return;
  }
  selectStock(stockResults.value[0]);
}

const selectedStock = ref(null);

function selectStock(stock) {
  stockKeyword.value = `${stock.stockCode} ${stock.simpleName}`;

  selectedStock.value = stock;

  showResults.value = false;

  emit("select-stock", stock);
}

function cancelSearch() {
  stockKeyword.value = "";
  selectedStock.value = null;
  showResults.value = false;

  emit("clear-stock");

  stockInput.value?.focus();
}


</script>

<style scoped>

.input-area {
  position: relative;
  flex: 1;
  min-width: 0;
}

.find-stock {
  position: relative;
  width: 100%;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.find-stock input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 40px 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 25px;
  background: #e0e1e1;
  outline: none;
  resize: vertical;
  color: #374151;
  font-size: 20px;
  line-height: 1.5;
  transition: 0.2s;
}

.find-stock input:focus {
  border: solid 2px #ffc70e;
  background: #f2f3f3;
}

.find-stock input::placeholder {
  color: #9ca3af;
  opacity: 0.6;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.cancel-button {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 30px;
}

.publish-button {
  margin-top: 5px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 16px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 20px;
  font-weight: 600;
  background-color: #f4c665;
  color: #5B4520;
}

.stock-results {
  width: 100%;
  max-height: 225px;
  overflow-y: auto;
  margin-top: 5px;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.stock-result {
  height: 45px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  padding: 6px 12px;
  color: #374151;
  font-size: 20px;
  cursor: pointer;
}


.stock-result:hover {
  background: #d6e8ff;
}


</style>