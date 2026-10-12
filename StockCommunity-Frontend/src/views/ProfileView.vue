<script setup>
import { ref, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/userStore'
import { useTradingAccountStore } from '@/stores/tradingAccountStore'

const userStore = useUserStore()
const { profile, errorMessage } = storeToRefs(userStore)

const currentPhotoPreview = ref('')
const newPhotoBase64 = ref('')
const newPhotoPreview = ref('')

// v-file-input 的 v-model，Vuetify 3 預設給的是「陣列」(即使沒開 multiple)，要自己取第一個
const selectedPhoto = ref(null)

const phone = ref('')
const username = ref('')
const successMsg = ref('')
const errorMsgLocal = ref('')

// 帳戶餘額改抓真實資料：Wei的資金凍結/交易帳戶功能後端已經做完了，
// dev分支上也已經有人把store(tradingAccountStore)串好對應/api/trading-accounts/me，
// 這裡直接拿來用，不用自己重寫一次API呼叫
const tradingAccountStore = useTradingAccountStore()
const { availableCash } = storeToRefs(tradingAccountStore)

onMounted(() => {
  userStore.fetchProfile()
  tradingAccountStore.fetchTradingAccount() // 進頁面時順便打帳戶餘額的API
})

watch(profile, (newProfile) => {
  if (newProfile) {
    currentPhotoPreview.value = newProfile.userPhoto
      ? `data:image/jpeg;base64,${newProfile.userPhoto}`
      : ''
    phone.value = newProfile.phone || ''
    username.value = newProfile.username || ''
  }
})

// 選檔案之後：讀成 data URI 給 <img> 立即預覽，同時拆出純Base64字串留著等下送出
watch(selectedPhoto, (fileValue) => {
  const file = Array.isArray(fileValue) ? fileValue[0] : fileValue
  if (!file) {
    newPhotoBase64.value = ''
    newPhotoPreview.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    const dataUrl = reader.result
    newPhotoPreview.value = dataUrl
    newPhotoBase64.value = dataUrl.split(',')[1] || ''
  }
  reader.readAsDataURL(file)
})

const handleUpdate = async () => {
  successMsg.value = ''
  errorMsgLocal.value = ''

  const success = await userStore.updateProfile(
    newPhotoBase64.value || undefined,
    phone.value.trim(),
    username.value.trim(),
  )
  if (success) {
    successMsg.value = '更新成功'
    if (profile.value?.userPhoto) {
      currentPhotoPreview.value = `data:image/jpeg;base64,${profile.value.userPhoto}`
    }
    newPhotoBase64.value = ''
    newPhotoPreview.value = ''
    selectedPhoto.value = null
  }
}

// 千分位金額格式化，照抄HoldingView.vue同一支函式的寫法，保持全站數字顯示風格一致
const formatNumber = (value) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return '—'
  }
  return new Intl.NumberFormat('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value))
}
</script>

<template>
  <!-- 新增：帳戶餘額卡片，獨立一張、疊在個人資料卡片正上方，跟它同寬(cols/sm/md設定一致) -->
  <!-- 特意跟下面「個人資料」卡片分開，因為餘額是唯讀狀態，不是「儲存」按鈕會送出的表單欄位，混在一起容易誤導使用者 -->
  <v-row justify="center" class="mt-8">
    <v-col cols="12" sm="8" md="5">
      <v-card class="pa-4 balance-card" elevation="0">
        <!-- 改用justify-space-between，左邊放icon+標籤、右邊放金額，把整張卡片的寬度撐開用滿，
             不然文字都擠在左邊，右邊留一大片空白看起來很單調 -->
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center ga-3">
            <!-- icon外面包一層圓底，呼應頂部GoodSign的金色主視覺 -->
            <div class="balance-icon-wrap">
              <v-icon icon="mdi-wallet-outline" size="22" color="#b8860b" />
            </div>
            <!-- 標籤文字，改用balance-label這個自訂class(深灰色、16px) -->
            <div class="balance-label">帳戶餘額</div>
          </div>
          <!-- availableCash改抓tradingAccountStore的真實資料，來源是後端/api/trading-accounts/me -->
          <!-- 金額放右邊，字級拉大到28px、加粗，填滿右側空間、也更醒目 -->
          <div class="balance-amount">NT$ {{ formatNumber(availableCash) }}</div>
        </div>
      </v-card>
    </v-col>
  </v-row>

  <!-- 原本的個人資料卡片，內容完全沒動，只是把上面的class從mt-8改mt-4，讓兩張卡片間距不要太開 -->
  <v-row justify="center" class="mt-4">
    <v-col cols="12" sm="8" md="5">
      <v-card class="pa-6" elevation="4">
        <h2>個人資料</h2>

        <div v-if="profile">
          <p class="mt-4">信箱：{{ profile.email }}</p>

          <v-avatar size="100" class="my-4" v-if="newPhotoPreview || currentPhotoPreview">
            <v-img :src="newPhotoPreview || currentPhotoPreview" alt="大頭照預覽" cover />
          </v-avatar>

          <v-file-input
            v-model="selectedPhoto"
            accept="image/*"
            label="大頭照"
            variant="outlined"
            density="compact"
            prepend-icon="mdi-camera"
          />

          <v-text-field
            v-model="phone"
            label="電話"
            variant="outlined"
            density="compact"
            class="mt-2"
          />

          <v-text-field
            v-model="username"
            label="暱稱"
            variant="outlined"
            density="compact"
            class="mt-2"
          />

          <v-btn color="primary" @click="handleUpdate">儲存</v-btn>

          <v-alert v-if="errorMsgLocal" type="error" density="compact" class="mt-4">{{
            errorMsgLocal
          }}</v-alert>
          <v-alert v-if="successMsg" type="success" density="compact" class="mt-4">{{
            successMsg
          }}</v-alert>
        </div>

        <v-alert v-if="errorMessage" type="error" density="compact" class="mt-4">{{
          errorMessage
        }}</v-alert>
      </v-card>
    </v-col>
  </v-row>
</template>

<style scoped>
/* 帳戶餘額卡片外框樣式，照抄StockView.vue的industry-filter-card/StockSearch.vue的find-stock
   同一套白底圓角陰影，跟全站卡片風格保持一致 */
.balance-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

/* icon的淡金色圓底，呼應網站上方GoodSign的金色主視覺 */
.balance-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fdf3e0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 標籤文字：比原本text-medium-emphasis深一點，不會太淡看不清楚 */
.balance-label {
  font-size: 16px;
  color: #6b7280;
}

/* 金額文字：放大到28px、加粗、深色，移到卡片右側，撐滿版面也更醒目 */
.balance-amount {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}
</style>
