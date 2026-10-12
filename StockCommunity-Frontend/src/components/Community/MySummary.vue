<template>

  <div class="summary-card">

    <div class="summary-title">
      My 股 Life
    </div>


    <div class="user-info">

        <img
        v-if="userPhoto"
        :src="userPhoto"
        class="avatar"
        >

        <div
        v-else
        class="avatar"
        >
        {{ userInitial }}
        </div>

      <div class="username">
        {{ summary.username }}
      </div>

    </div>


    <div class="summary-stats">

      <div class="stat-item">

        <div class="stat-icon">
          <span class="mdi mdi-post-outline"></span>
        </div>

        <div class="stat-content">
          <div class="stat-number">
            {{ summary.postCount }}
          </div>

          <div class="stat-label">
            貼文
          </div>
        </div>

      </div>


      <div class="stat-item">

        <div class="stat-icon">
          <span class="mdi mdi-comment-outline"></span>
        </div>

        <div class="stat-content">
          <div class="stat-number">
            {{ summary.commentCount }}
          </div>

          <div class="stat-label">
            留言
          </div>
        </div>

      </div>


      <div class="stat-item">

        <div class="stat-icon">
          <span class="mdi mdi-thumb-up-outline"></span>
        </div>

        <div class="stat-content">
          <div class="stat-number">
            {{ summary.receivedLikeCount }}
          </div>

          <div class="stat-label">
            獲讚
          </div>
        </div>

      </div>

    </div>


    <div class="divider">
      <span class="divider-line"></span>

      <span class="stock-trend">
        <span class="mdi mdi-chart-line"></span>
        <span class="mdi mdi-chart-line"></span>
        <span class="mdi mdi-chart-line"></span>
        <span class="mdi mdi-chart-line"></span>
        <span class="mdi mdi-chart-line"></span>
      </span>

      <span class="divider-line"></span>
    </div>


    <div class="stock-section">

      <div class="stock-title">
        My 股 Talk
      </div>


      <div
        v-if="summary.topStocks.length > 0"
        class="stock-tags"
      >

        <div
          v-for="stock in summary.topStocks"
          :key="stock.stockCode"
          class="stock-tag"
          @click="goToStock(stock)"
        >
          <span class="mdi mdi-tag"></span>        
          {{ stock.simpleName }}
        </div>

      </div>


      <div
        v-else
        class="no-stock"
      >
        目前還沒有討論過股票
      </div>

    </div>

  </div>

</template>


<script setup>

import { ref, computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from 'vue-router'
import { useUserStore } from "@/stores/userStore";
import api from "@/api/axios";

const router = useRouter()

const userStore = useUserStore();
const { profile } = storeToRefs(userStore);

const summary = ref({
  userId: null,
  username: '',
  postCount: 0,
  commentCount: 0,
  receivedLikeCount: 0,
  topStocks: []
})

const userPhoto = computed(() => {
  if (profile.value?.userPhoto) {
    return `data:image/jpeg;base64,${profile.value.userPhoto}`;
  }

  return null;
});


const userInitial = computed(() => {
  return summary.value.username?.charAt(0) || ''
})


const fetchSummary = async () => {

  try {
    const response =
      await api.get('/api/posts/summary')

    summary.value = response.data

  } catch (error) {
    console.error('取得我的摘要失敗', error)
  }

}

function goToStock(stock) {
  router.push({
    path: '/findstockpost',
    query: {
      stockCode: stock.stockCode,
      stockName: stock.simpleName
    }
  })
}

onMounted(() => {
  const token = localStorage.getItem("token");

  if (token && !profile.value) {
    userStore.fetchProfile();
  }

  fetchSummary();
});

</script>


<style scoped>

.summary-card {
  width: 100%;
  padding: 24px;
  background: #FFFDF8;
  border: 1px solid #E7E1D5;
  border-radius: 12px;
  box-sizing: border-box;
}


.summary-title {
  position: relative;
  padding-left: 12px;
  color: #374151;
  font-size: 22px;
  font-weight: 600;
}

.summary-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 20px;
  background: #E9AD50;
  border-radius: 10px;
  transform: translateY(-50%);
}


.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 24px;
}

.avatar {
  width: 64px;
  height: 64px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F9F1D8;
  border: 3px solid #F3C45E;
  border-radius: 50%;
  box-shadow:
    0 0 0 5px #FFF4D8,
    0 5px 14px rgba(121, 95, 53, 0.12);

  color: #795F35;
  font-size: 24px;
  font-weight: 600;
}

.username {
  margin-top: 14px;
  color: #374151;
  font-size: 20px;
  font-weight: 600;
}


.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 26px;
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  padding: 12px 6px;

  border-radius: 12px;
}

/* 貼文 */
.stat-item:nth-child(1) {
  background: #FFFBF0;
  border: 1px solid #F5E5B8;
}

/* 留言 */
.stat-item:nth-child(2) {
  background: #FAF8FE;
  border: 1px solid #E9E1F5;
}

/* 獲讚 */
.stat-item:nth-child(3) {
  background: #FFF8F5;
  border: 1px solid #F3DDD5;
}


.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  font-size: 16px;
}

/* 貼文：奶油黃 */
.stat-item:nth-child(1) .stat-icon {
  background: #FFF0C9;
  color: #B98228;
}

/* 留言：淡紫色 */
.stat-item:nth-child(2) .stat-icon {
  background: #EEE7F8;
  color: #8066B3;
}

/* 獲讚：淡杏橘 */
.stat-item:nth-child(3) .stat-icon {
  background: #FBE9E2;
  color: #C9785C;
}


.stat-content {
  text-align: left;
}

.stat-number {
  color: #374151;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  margin-top: 4px;
  color: #8B8172;
  font-size: 13px;
}


.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 22px 0;
}

.divider-line {
  height: 1px;
  flex: 1;
  background: #EEE9DF;
}

.stock-trend {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #D79B3B;
  font-size: 18px;
}


.stock-title {
  position: relative;
  margin-bottom: 14px;
  padding-left: 12px;
  color: #374151;
  font-size: 17px;
  font-weight: 600;
}

.stock-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 16px;
  background: #E9AD50;
  border-radius: 10px;
  transform: translateY(-50%);
}


.stock-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stock-tag {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  background: #FDF3D7;
  border: 1px solid transparent;
  border-radius: 20px;
  color: #8F6827;
  font-size: 15px;
  cursor: pointer;
  transition: 0.2s ease;
}

.stock-tag .mdi {
  font-size: 15px;
}

.stock-tag:hover {
  background: #F8E8B8;
  border-color: #EDD395;
  color: #795F35;
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(121, 95, 53, 0.1);
}

.no-stock {
  color: #A39A8D;
  font-size: 15px;
}

</style>