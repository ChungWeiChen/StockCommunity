<template>
  <div class="post-list">

    <div
      v-if="props.type === 'stock' && !loading && posts.length === 0"
      class="no-posts"
    >
      目前沒有與「 
      <span class="mdi mdi-tag"></span>
      <span class="stock-name">
      {{ props.stockCode }} {{props.stockName}} 
      </span>
      」相關的貼文
    </div>

    <div
      v-if="props.type === 'username' && !loading && posts.length === 0"
      class="no-posts"
    >
      「
      <span class="mdi mdi-account"></span>
      <span class="user-name">
        {{ props.username }}
      </span>
      」目前沒有貼文
    </div>

    <OnePost
      v-for="post in paginatedPosts"
      :key="post.id"
      :post="post"
    />

    <div
      v-if="totalPages > 1"
      class="pagination"
    >
      <button
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        ‹
      </button>

      <button
        v-for="page in totalPages"
        :key="page"
        :class="{ active: currentPage === page }"
        @click="currentPage = page"
      >
        {{ page }}
      </button>

      <button
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        ›
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from "vue";
import api from "@/api/axios";

import OnePost from "@/components/Community/OnePost.vue";


const posts = ref([]);
const loading = ref(false);

const currentPage = ref(1);

const totalPages = ref(0);

const pageSize = 5;

const props = defineProps({
  type:{
    type: String,
    default: "all"
  },

  stockCode: {
    type: String,
    default: ""
  },
  stockName: {
    type: String,
    default: ""
  },
  username: {
    type: String,
    default: ""
  }
})

async function getAllPosts() {
  loading.value = true;

  try {
    let response;

    // Vue 第 1 頁 = 後端 page 0
    const backendPage = currentPage.value - 1;

    if (props.type === "my") {

      response = await api.get("/api/posts/my", {
        params: {
          page: backendPage
        }
      });

    } else if (props.type === "hot") {

      // 熱門一次取得 Top 25
      response = await api.get("/api/posts/hot");

    } else if (props.type === "stock") {

      response = await api.get(
        `/api/posts/stock/${props.stockCode}`,
        {
          params: {
            page: backendPage
          }
        }
      );

    } else if (props.type === "username") {

      response = await api.get("/api/posts/username", {
        params: {
          username: props.username,
          page: backendPage
        }
      });

    } else {

      response = await api.get("/api/posts", {
        params: {
          page: backendPage
        }
      });
    }

    // 熱門還是直接回傳 List
    if (props.type === "hot") {

      posts.value = response.data;

      totalPages.value =
        Math.ceil(posts.value.length / pageSize);

    } else {

      // 其他四種現在回傳 PostPageResponseDTO
      posts.value = response.data.posts;

      totalPages.value = response.data.totalPages;
    }

    console.log("API 回傳：", response.data);

  } catch (error) {
    console.log("取得貼文失敗：", error);

  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  getAllPosts();
});

watch(
  () => props.stockCode,
  () => {
    if (props.type === "stock") {

      if (currentPage.value === 1) {
        getAllPosts();
      } else {
        currentPage.value = 1;
      }
    }
  }
);

watch(
  () => props.username,
  () => {
    if (props.type === "username") {

      if (currentPage.value === 1) {
        getAllPosts();
      } else {
        currentPage.value = 1;
      }
    }
  }
);

watch(
  currentPage,
  () => {

    // 熱門 25 則已經一次全部拿回來
    // 所以 Hot 換頁不需要重新打 API
    if (props.type !== "hot") {
      getAllPosts();
    }
  }
);

const paginatedPosts = computed(() => {

  // 熱門一次拿 Top 25，所以由 Vue 自己分頁
  if (props.type === "hot") {

    const start =
      (currentPage.value - 1) * pageSize;

    const end =
      start + pageSize;

    return posts.value.slice(start, end);
  }

  // 其他貼文後端已經切成每頁 5 則
  return posts.value;
});




</script>

<style scoped>
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.pagination button {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #706455;
  cursor: pointer;
}

.pagination button:hover:not(:disabled) {
  background: #f9f1d8;
}

.pagination button.active {
  background: #f5e8bd;
  border-color: #e1b25a;
  color: #795f35;
  font-weight: 600;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.no-posts {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  font-size: 20px;
}

.no-posts .mdi,
.no-posts .stock-name,
.no-posts .user-name {
  color: #b88732;
}

</style>