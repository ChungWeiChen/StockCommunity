<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const username = ref('')
const password = ref('')
const email = ref('')
const errorMsg = ref('')
const formError = ref('') // 前端自己的驗證錯誤，跟後端回傳的錯誤分開顯示
const router = useRouter()
const authStore = useAuthStore()

// Email 格式簡單檢查用的正則表達式
// 改用明確限制只能ASCII英數字元跟常見符號的正則，避免中文或其他特殊符號造成的問題
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const handleRegister = async () => {
  errorMsg.value = ''
  formError.value = ''

  // 前端先擋明顯錯誤的輸入，減少不必要的後端來回
  // 帳號現在可以留空（後端會拿 email 自動產生），所以這裡不再檢查帳號

  if (!email.value.trim()) {
    formError.value = '請輸入信箱'
    return
  }

  if (!emailPattern.test(email.value)) {
    formError.value = '信箱格式不正確'
    return
  }

  if (!password.value) {
    formError.value = '請輸入密碼'
    return
  }

  if (password.value.length < 6) {
    formError.value = '密碼長度至少需要 6 個字元'
    return
  }

  const result = await authStore.register(username.value, password.value, email.value)
  if (result.success) {
    router.push('/login')
  } else {
    // 優先顯示後端實際回傳的錯誤原因，抓不到才用通用文字
    errorMsg.value = authStore.errorMessage || '註冊失敗，請確認輸入內容'

    // 留空暱稱撞到自動產生的預設名稱時，後端會給建議名稱，直接帶進輸入框讓使用者確認或修改後再送一次
    if (result.suggestedUsername) {
      username.value = result.suggestedUsername
    }
  }
}
</script>

<template>
  <v-row justify="center" class="mt-16">
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-6" elevation="4">
        <h2 class="mb-4">註冊</h2>
        <v-form @submit.prevent="handleRegister">
          <v-text-field v-model="email" label="信箱" type="email" variant="outlined" />
          <v-text-field v-model="password" label="密碼" type="password" variant="outlined" />
          <v-text-field
            v-model="username"
            label="暱稱（留空會用信箱自動產生）"
            variant="outlined"
          />
          <v-btn type="submit" color="primary" block>註冊</v-btn>
        </v-form>
        <v-alert v-if="formError" type="error" density="compact" class="mt-4">{{
          formError
        }}</v-alert>
        <v-alert v-if="errorMsg" type="error" density="compact" class="mt-4">{{
          errorMsg
        }}</v-alert>
      </v-card>
    </v-col>
  </v-row>
</template>

<style scoped></style>

