<template>
  <div class="popular-stocks">

    <h3 class="popular-title">
      熱門股票標籤
    </h3>

    <div
      v-for="(stock, index) in popularStocks"
      :key="stock.stockCode"
      class="stock-item"
      @click="goToStockPosts(stock.stockCode, stock.simpleName)"
    >

      <span class="rank">
        {{ index + 1 }}
      </span>

      <div class="stock-info">

        <span class="stock-code">
            <span class="mdi mdi-tag"></span>
          {{ stock.stockCode }}
        </span>

        <span class="stock-name">
          {{ stock.simpleName }}
        </span>

      </div>

      <span class="post-count">
        {{ stock.postCount }} 篇
      </span>

    </div>

  </div>
</template>


<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import api from "@/api/axios";

const router = useRouter();

const popularStocks = ref([]);


async function getPopularStocks() {

  try {

    const response = await api.get("/api/posts/popular-stocks");

    popularStocks.value = response.data;

  } catch (error) {

    console.error("取得熱門股票失敗", error);

  }

}

function goToStockPosts(stockCode, stockName) {

  router.push({
    path: "/findstockpost",
    query: {
      stockCode: stockCode,
      stockName: stockName
    }
  });

}


onMounted(() => {
  getPopularStocks();
});


</script>


<style scoped>

.popular-stocks {
  background: #FFFFFF;
  border: 1px solid #F0D9AD;
  border-radius: 12px;
  padding: 18px;
}

.popular-title {
  position: relative;
  margin: 0 0 14px;
  padding-left: 12px;
  color: #A56F27;
  font-size: 20px;
  font-weight: 700;
}

.popular-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 22px;
  background: #D99A3D;
  border-radius: 4px;
  transform: translateY(-50%);
}

.stock-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid #F1E5D2;
  cursor: pointer;
  transition: 0.15s;
}

.stock-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}


.stock-item:hover {
  background: #FFF9EE;
}

.rank {
  margin-left: 10px;
  width: 24px;
  color: #A98255;
  font-size: 20px;
  font-weight: 600;
}

.stock-info {
  display: flex;
  flex: 1;
  gap: 8px;
  align-items: center;
}

.stock-code {
  color: #A56F27;
  font-size: 18px;
  font-weight: 700;
}

.stock-code .mdi {
  color: #72543A;
}

.stock-name {
  color: #706455;
  font-weight: 500;
}

.post-count {
  margin-right: 10px;
  color: #8B7258;
  font-size: 17px;
}

.stock-item:hover .stock-code {
  color: #8F5F20;
}

.stock-item:hover .stock-code .mdi {
  color: #60452F;
}

</style>