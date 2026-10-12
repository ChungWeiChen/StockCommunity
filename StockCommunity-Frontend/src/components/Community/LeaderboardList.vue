<template>
  <v-card class="leaderboard-card" elevation="0">
    <v-card-title class="leaderboard-title">股賽排行榜</v-card-title>

    <v-table class="leaderboard-table">
      <thead>
        <tr>
          <th class="text-center">名次</th>
          <th>暱稱</th>
          <th class="text-right">總資產</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(entry, index) in entries" :key="entry.userId">
          <td class="text-center">
            <span v-if="getMedal(index)" class="medal">{{ getMedal(index) }}</span>
            <span v-else>{{ index + 1 }}</span>
          </td>
          <td>{{ entry.username }}</td>
          <td class="text-right">
            {{ formatAssets(entry.totalAssets) }}
          </td>
        </tr>
      </tbody>
    </v-table>

    <p v-if="!loading && entries.length === 0" class="empty-text">目前排行榜還沒有資料</p>
  </v-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const entries = ref([])
const loading = ref(true)

async function getLeaderboard() {
  try {
    const response = await api.get('/api/leaderboard')
    entries.value = response.data

    console.log('排行榜 API 回傳：', response.data)
  } catch (error) {
    console.log('取得排行榜失敗：', error)
  } finally {
    loading.value = false
  }
}

function formatAssets(totalAssets) {
  return totalAssets.toLocaleString()
}

// 前三名顯示獎牌 emoji，其餘名次照舊顯示數字
function getMedal(index) {
  const medals = ['🥇', '🥈', '🥉']
  return medals[index] || null
}

onMounted(() => {
  getLeaderboard()
})
</script>

<style scoped>
.leaderboard-card {
  width: 100%;
  box-sizing: border-box;

  background: #fbfaf6;
  border: 1px solid #e7e1d5;
  border-radius: 12px;
  padding: 8px;
}

.leaderboard-title {
  color: #6f5833;
  font-weight: 600;
}

.medal {
  font-size: 18px;
}

.empty-text {
  text-align: center;
  color: #9c9385;
  padding: 24px 0;
}
</style>
