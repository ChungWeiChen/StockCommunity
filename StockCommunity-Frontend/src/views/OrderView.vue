<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useOrderStore } from '@/stores/orderStore'

const route = useRoute()
const orderStore = useOrderStore()
// State 用 storeToRefs 解構，保持響應性
const { orders, errorMessage } = storeToRefs(orderStore)

const stockCode = ref('')
const orderType = ref('BUY') // 預設買進，對應後端 OrderType enum 的值
const quantity = ref('')
const price = ref('')
// 後端新併入 priceType/tradingType/tradingTime 三個欄位，entity 都是 nullable = false，
// 沒有預設值就直接 400，所以這裡給跟後端 enum 對得上的預設值
const priceType = ref('MARKET_ORDER') // MARKET_ORDER 市價單 / LIMIT_ORDER 限價單
const tradingType = ref('ROD') // ROD 當日有效 / IOC 立即成交否則取消 / FOK 全部成交否則取消
const tradingTime = ref('DURING_HOURS') // DURING_HOURS 盤中 / AFTER_HOURS 盤後
const formError = ref('') // 新增：表單自己的驗證錯誤訊息，跟後端的 errorMessage 分開

// v-select 用的選項陣列：title 是顯示文字，value 是實際存進 v-model / 送給後端的值
const orderTypeOptions = [
  { title: '買進', value: 'BUY' },
  { title: '賣出', value: 'SELL' },
]

const priceTypeOptions = [
  { title: '市價單', value: 'MARKET_ORDER' },
  { title: '限價單', value: 'LIMIT_ORDER' },
]

const tradingTypeOptions = [
  { title: 'ROD 當日有效', value: 'ROD' },
  { title: 'IOC 立即成交否則取消', value: 'IOC' },
  { title: 'FOK 全部成交否則取消', value: 'FOK' },
]

const tradingTimeOptions = [
  { title: '盤中', value: 'DURING_HOURS' },
  { title: '盤後', value: 'AFTER_HOURS' },
]

// 進頁面就先拿一次「我的所有訂單」
// 如果是從關注清單點「下單」跳過來的，網址會帶 ?code=xxx，直接幫忙帶入股票代號欄位
onMounted(() => {
  orderStore.fetchMyOrders()

  if (route.query.code) {
    stockCode.value = route.query.code
  }
})

const handleCreateOrder = async () => {
  formError.value = ''

  // 市價單(MARKET_ORDER)不需要填價格，限價單(LIMIT_ORDER)才要；股票代號跟股數一律必填
  const isLimitOrder = priceType.value === 'LIMIT_ORDER'
  if (!stockCode.value || !quantity.value || (isLimitOrder && !price.value)) return

  const quantityNum = Number(quantity.value)
  // 市價單直接傳 null，對應後端 OrderService 會把市價單的 price 存成 null 的邏輯
  const priceNum = isLimitOrder ? Number(price.value) : null

  // 股數必須是大於 0 的整數
  if (!Number.isInteger(quantityNum) || quantityNum <= 0) {
    formError.value = '股數必須是大於 0 的整數'
    return
  }

  // 價格必須大於 0（只有限價單需要檢查，用 <= 0 一起擋負數跟 0）
  if (isLimitOrder && priceNum <= 0) {
    formError.value = '價格必須大於 0'
    return
  }

  // input 的 v-model 抓出來的值是字串，要轉成數字才符合後端 Integer/BigDecimal 的型別
  const success = await orderStore.createOrder(
    stockCode.value,
    orderType.value,
    quantityNum,
    priceNum,
    priceType.value,
    tradingType.value,
    tradingTime.value,
  )

  if (success) {
    // 送出成功才清空表單，失敗的話讓使用者留著原本打的內容修改重送
    stockCode.value = ''
    quantity.value = ''
    price.value = ''
  }
}

const handleCancelOrder = async (orderId) => {
  await orderStore.cancelOrder(orderId)
}
</script>

<template>
  <h2>下單</h2>

  <v-card class="pa-6 mb-8" elevation="4">
    <v-form @submit.prevent="handleCreateOrder">
      <v-row>
        <v-col cols="12" md="4">
          <v-text-field v-model="stockCode" label="股票代號" variant="outlined" density="compact" />
        </v-col>

        <v-col cols="12" md="4">
          <v-select
            v-model="orderType"
            :items="orderTypeOptions"
            item-title="title"
            item-value="value"
            label="買賣別"
            variant="outlined"
            density="compact"
          />
        </v-col>

        <v-col cols="12" md="4">
          <v-text-field
            v-model="quantity"
            type="number"
            min="1"
            step="1"
            label="股數"
            variant="outlined"
            density="compact"
          />
        </v-col>

        <v-col cols="12" md="4">
          <v-select
            v-model="priceType"
            :items="priceTypeOptions"
            item-title="title"
            item-value="value"
            label="價格類型"
            variant="outlined"
            density="compact"
          />
        </v-col>

        <!-- 只有限價單才需要填價格，市價單交由撮合當下的市價成交 -->
        <v-col cols="12" md="4" v-if="priceType === 'LIMIT_ORDER'">
          <v-text-field
            v-model="price"
            type="number"
            min="0.01"
            step="0.01"
            label="價格"
            variant="outlined"
            density="compact"
          />
        </v-col>

        <v-col cols="12" md="4">
          <v-select
            v-model="tradingType"
            :items="tradingTypeOptions"
            item-title="title"
            item-value="value"
            label="下單條件"
            variant="outlined"
            density="compact"
          />
        </v-col>

        <v-col cols="12" md="4">
          <v-select
            v-model="tradingTime"
            :items="tradingTimeOptions"
            item-title="title"
            item-value="value"
            label="交易時段"
            variant="outlined"
            density="compact"
          />
        </v-col>
      </v-row>

      <v-btn type="submit" color="primary">送出委託</v-btn>
    </v-form>

    <v-alert v-if="formError" type="error" density="compact" class="mt-4">{{ formError }}</v-alert>
    <v-alert v-if="errorMessage" type="error" density="compact" class="mt-4">{{
      errorMessage
    }}</v-alert>
  </v-card>


</template>

<style scoped></style>
