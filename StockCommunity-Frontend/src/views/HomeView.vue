<script setup>
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
</script>

<template>
  <v-container>
    <h2 class="text-h5 font-weight-bold mb-2">
      歡迎回來{{ user?.username ? '，' + user.username : '' }}
    </h2>
    <p class="text-body-2">
      從左側選單前往股票行情、計畫表、下單或持股頁面。
    </p>
  </v-container>
</template>
