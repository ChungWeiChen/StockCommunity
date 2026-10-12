<template>
  <Line :data="chartData" />
</template>

<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const props = defineProps({
  priceHistory: {
    type: Array,
    default: () => [],
  },
})

const chartData = computed(() => {
  const labels = props.priceHistory.map((row) => row.tradeDate)
  const data = props.priceHistory.map((row) => row.closePrice)
  return {
    labels,
    datasets: [
      {
        label: '收盤價',
        data,
        borderColor: '#1976D2',
        backgroundColor: 'rgba(25, 118, 210, 0.15)',
        tension: 0.2,
      },
    ],
  }
})
</script>
