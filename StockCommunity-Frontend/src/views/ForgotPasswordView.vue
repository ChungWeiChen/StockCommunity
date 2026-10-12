<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/authStore'

const email = ref('')
const submitting = ref(false)
const resultMsg = ref('')
const resultType = ref('success')

const authStore = useAuthStore()

const handleSubmit = async () => {
  resultMsg.value = ''
  submitting.value = true
  const { success, message } = await authStore.forgotPassword(email.value.trim())
  submitting.value = false
  resultType.value = success ? 'success' : 'error'
  resultMsg.value = success
    ? message || '如果該信箱存在，重設密碼信件已寄出，請去信箱收信'
    : message
}
</script>

<template>
  <v-row justify="center" class="mt-16">
    <v-col cols="12" sm="6" md="4">
      <v-card class="pa-6" elevation="4">
        <h2 class="mb-4">忘記密碼</h2>
        <v-form @submit.prevent="handleSubmit">
          <v-text-field v-model="email" label="信箱" type="email" variant="outlined" />
          <v-btn type="submit" color="primary" block :loading="submitting">寄送重設密碼信件</v-btn>
        </v-form>

        <v-alert v-if="resultMsg" :type="resultType" density="compact" class="mt-4">{{
          resultMsg
        }}</v-alert>

        <div class="mt-4 text-center">
          <router-link to="/login">回登入頁</router-link>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>

