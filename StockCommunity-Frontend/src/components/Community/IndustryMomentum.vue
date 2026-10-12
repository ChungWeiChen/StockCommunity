<template>
  <div class="industry-momentum">
    <h3 class="momentum-heading">今日領漲/領跌產業</h3>

    <div v-if="loading" class="loading-text">載入中...</div>

    <div v-else class="momentum-columns">
      <div class="momentum-col">
        <div class="col-title gainer-title">領漲</div>
        <div v-if="gainers.length === 0" class="empty-text">目前沒有資料</div>
        <div v-for="item in gainers" :key="item.industryCode" class="momentum-row">
          <span class="industry-name">{{ item.industryName }}</span>
          <span class="change-percent gainer">+{{ item.avgChangePercent }}%</span>
        </div>
      </div>

      <div class="momentum-divider"></div>

      <div class="momentum-col">
        <div class="col-title loser-title">領跌</div>
        <div v-if="losers.length === 0" class="empty-text">目前沒有資料</div>
        <div v-for="item in losers" :key="item.industryCode" class="momentum-row">
          <span class="industry-name">{{ item.industryName }}</span>
          <span class="change-percent loser">{{ item.avgChangePercent }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const gainers = ref([])
const losers = ref([])
const loading = ref(true)

async function fetchIndustryMomentum() {
  loading.value = true
  try {
    const response = await api.get('/api/industries/momentum?topN=3')
    gainers.value = response.data.gainers
    losers.value = response.data.losers
  } catch (error) {
    console.error('取得產業漲跌排行失敗：', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchIndustryMomentum()
})
</script>

<style scoped>
.industry-momentum {
  background: #ffffff;
  border: 1px solid #aca0ca;
  border-radius: 12px;
  padding: 18px;
}

.momentum-heading {
  position: relative;
  margin: 0 0 14px;
  padding-left: 12px;
  color: #7561a8;
  font-size: 22px;
  font-weight: 700;
}

.momentum-heading::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 4px;
  height: 22px;
  background: #8b73c7;
  border-radius: 4px;
  transform: translateY(-50%);
}

.momentum-columns {
  display: flex;
  gap: 12px;
}

.momentum-col {
  flex: 1;
  min-width: 0;
}

.momentum-divider {
  width: 1px;
  background: #e3edf5;
}

.col-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
}

.gainer-title {
  color: #d32f2f;
}

.loser-title {
  color: #2e7d32;
}

.momentum-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 14px;
}

.industry-name {
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.change-percent {
  font-weight: 600;
  flex-shrink: 0;
}

.change-percent.gainer {
  color: #d32f2f;
}

.change-percent.loser {
  color: #2e7d32;
}

.empty-text {
  color: #756f80;
  font-size: 13px;
}

.loading-text {
  color: #756f80;
  font-size: 16px;
  padding: 8px 0;
}
</style>
