<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const email = ref('')
const password = ref('')
const errorMsg = ref('')
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const handleLogin = async () => {
  errorMsg.value = ''
  const success = await authStore.login(email.value, password.value)
  if (success) {
    // 如果是被 beforeEach 導來的(網址上帶了 redirect 參數)，登入完導回原本想去的頁面
    // 沒有帶這個參數，代表是使用者自己直接打開 /login，維持原本導去首頁的行為
    // 原本：router.push('/')
    const redirectTo = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirectTo)
  } else {
    // 優先顯示後端實際回傳的錯誤原因，抓不到才用通用文字
    errorMsg.value = authStore.errorMessage || '登入失敗，請確認信箱密碼'
  }
}
</script>

<template>
  <v-row justify="center" class="mt-16">
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-6" elevation="4">
        <h2 class="mb-4">登入</h2>
        <v-form @submit.prevent="handleLogin">
          <v-text-field v-model="email" label="信箱" type="email" variant="outlined" />
          <v-text-field v-model="password" label="密碼" type="password" variant="outlined" />
          <v-btn type="submit" color="primary" block>登入</v-btn>
        </v-form>
        <div class="mt-2 text-right">
          <router-link to="/forgot-password">忘記密碼？</router-link>
        </div>
        <v-alert v-if="errorMsg" type="error" density="compact" class="mt-4">{{
          errorMsg
        }}</v-alert>
      </v-card>
    </v-col>
  </v-row>
</template>

