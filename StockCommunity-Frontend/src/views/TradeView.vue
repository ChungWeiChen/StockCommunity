<script setup>
import { onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useTradeStore } from '@/stores/tradeStore';

const tradeStore = useTradeStore();
// State 用 storeToRefs 解構，保持響應性
const { trades, errorMessage } = storeToRefs(tradeStore);

// 進頁面就拿一次「我的所有成交紀錄」
// 沒有表單、沒有送出動作——成交紀錄是撮合引擎產生的結果，這頁純唯讀
onMounted(() => {
    tradeStore.fetchMyTrades();
});
</script>

<template>
  <h2>委託紀錄</h2>
  <v-alert v-if="errorMessage" type="error" density="compact" class="mb-4">{{ errorMessage }}</v-alert>

  <v-table>
    <thead>
      <tr>
        <th>股票代號</th>
        <th>成交價</th>
        <th>成交量</th>
        <th>委託時間</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="trade in trades" :key="trade.id">
        <td>{{ trade.stockCode }}</td>
        <td>{{ trade.price }}</td>
        <td>{{ trade.quantity }}</td>
        <td>{{ trade.tradedAt }}</td>
      </tr>
    </tbody>
  </v-table>
  <p v-if="trades.length === 0" class="mt-4">目前沒有成交紀錄</p>
</template>

<style scoped>
</style>