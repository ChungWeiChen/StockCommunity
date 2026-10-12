<template>
  <div class="error-page">
    <img src="/pictures/404.png" class="error-image" />
    <!-- 【新增】原本卡住這畫面沒有任何出口，只能手動整頁重新整理才能回到網站，
         加一顆按鈕直接幫使用者reload，體驗上比較清楚知道要做什麼 -->
    <v-btn class="retry-btn" color="primary" @click="reconnect"> 重新連線 </v-btn>
  </div>
</template>

<script setup>
// 按下「重新連線」直接整頁重新整理，跟使用者原本手動按F5的效果一樣，
// 只是不用讓他自己想到要按F5
function reconnect() {
  window.location.reload()
}
</script>

<style scoped>
.error-page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  gap: 24px;
}

.error-image {
  width: 75%;
}
</style>
