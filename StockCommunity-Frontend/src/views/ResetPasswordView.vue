<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const token = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const submitting = ref(false)

onMounted(() => {
  // 網址格式：/reset-password?token=xxx，對應後端信件裡的連結
  token.value = route.query.token || ''
  if (!token.value) {
    errorMsg.value = '連結無效，缺少 token 參數，請重新申請忘記密碼'
  }
})

const handleSubmit = async () => {
  errorMsg.value = ''
  successMsg.value = ''

  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '兩次輸入的新密碼不一致'
    return
  }

  submitting.value = true
  const { success, message } = await authStore.resetPassword(token.value, newPassword.value)
  submitting.value = false

  if (success) {
    successMsg.value = message || '密碼重設成功，請重新登入'
    setTimeout(() => router.push('/login'), 2000)
  } else {
    errorMsg.value = message
  }
}
</script>

<template>
  <v-row justify="center" class="mt-16">
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-6" elevation="4">
        <h2 class="mb-4">重設密碼</h2>
        <v-form @submit.prevent="handleSubmit">
          <v-text-field v-model="newPassword" label="新密碼" type="password" variant="outlined" />
          <v-text-field v-model="confirmPassword" label="確認新密碼" type="password" variant="outlined" />
          <v-btn type="submit" color="primary" block :loading="submitting" :disabled="!token">重設密碼</v-btn>
        </v-form>

        <v-alert v-if="errorMsg" type="error" density="compact" class="mt-4">{{ errorMsg }}</v-alert>
        <v-alert v-if="successMsg" type="success" density="compact" class="mt-4">{{ successMsg }}</v-alert>
      </v-card>
    </v-col>
  </v-row>
</template>