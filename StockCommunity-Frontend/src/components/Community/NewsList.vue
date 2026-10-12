<template>
  <div class="news-list">

    <h3 class="news-heading">
      <template v-if="stockName">
        <span class="stock-title">
          <span class="mdi mdi-tag"></span>
          {{ stockName }}
        </span>
        相關新聞
      </template>

      <template v-else>
        股市焦點
      </template>
    </h3>

    <div
      v-if="newsList.length === 0"
      class="no-news"
    >
      目前沒有與「
      <span class="mdi mdi-tag"></span>
      <span class="stock-name">
        {{ props.stockCode }} {{ props.stockName }}
      </span>
      」相關的新聞
    </div>

    <a
      v-for="news in newsList"
      :key="news.id"
      :href="news.url"
      target="_blank"
      rel="noopener noreferrer"
      class="news-item"
    >

      <img
        v-if="news.image"
        :src="news.image"
        class="news-image"
      />

      <div class="news-content">

        <div class="news-title">
          {{ news.title }}
        </div>

        <div class="news-source">
          {{ news.sourceName }}
        </div>

      </div>

    </a>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import api from "@/api/axios";

const newsList = ref([]);

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

async function fetchNews() {
  try {

    let response;

    if (props.stockCode) {
      // 搜尋股票 → 取得該股票新聞
      response = await api.get(
        `/api/news/stock/${props.stockCode}`
      );

      // 最多顯示 10 則
      newsList.value = response.data.slice(0, 10);

    } else {
      // 沒有搜尋股票 → 首頁熱門股票新聞
      response = await api.get("/api/news");

      newsList.value = response.data;
    }

    console.log("取得新聞：", newsList.value);

  } catch (error) {
    console.error("取得新聞失敗：", error);
  }
}

watch(
  () => props.stockCode,
  () => {
    fetchNews();
  }
);

onMounted(() => {
  fetchNews();
});

</script>

<style scoped>

.news-list {
  background: #FFFFFF;
  border: 1px solid #aca0ca;
  border-radius: 12px;
  padding: 18px;
}

.news-heading {
  position: relative;
  margin: 0 0 14px;
  padding-left: 12px;
  color: #7561A8;
  font-size: 22px;
  font-weight: 700;
}


.news-heading::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 22px;
  background: #8B73C7;
  border-radius: 4px;
  transform: translateY(-50%);
}

.stock-title {
  color: #7561A8;
}

.stock-title .mdi {
  margin-right: 3px;
  color: #8B73C7;
}


.news-item {
  display: flex;
  gap: 16px;
  margin: 0 -8px;
  padding: 15px 8px;
  border-bottom: 1px solid #E3EDF5;
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease;
}

.news-item:last-child {
  border-bottom: none;
}


.news-image {
  width: 140px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.news-content {
  flex: 1;
  min-width: 0;
}

.news-title {
  color: #374151;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.55;
  transition: color 0.2s ease;
}

.news-source {
  display: inline-block;
  margin-top: 8px;
  padding: 3px 9px;
  background: #F1EDF7;
  border-radius: 20px;
  color: #7561A8;
  font-size: 13px;
  line-height: 1.4;
}


.news-item:hover {
  background: #F8F6FB;
  transform: translateY(-1px);
}

.news-item:hover .news-title {
  color: #7561A8;
}

.news-item:hover .news-source {
  background: #E9E3F2;
  color: #6F5A9D;
}

.no-news {
  padding: 16px 12px;
  background: #F8F6FB;
  border-radius: 8px;
  color: #756F80;
  font-size: 14px;
}

.no-news .mdi,
.no-news .stock-name {
  color: #7561A8;
}

.no-news .stock-name {
  font-weight: 600;
}
</style>