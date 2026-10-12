<template>

  
  <div
    v-if="isLoggedIn"
    class="community-layout"
  >

    <div class="community-main">

      <CreatePost />

      <div class="post-gap"></div>

      <PostsList type="my" />

    </div>


    <!-- 右邊：我的摘要 -->
    <div class="community-right">

      <MySummary />

    </div>

  </div>


  <div
    v-else
    class="login-required"
  >
    <div class="login-title">
      <span class="mdi mdi-account-question"></span>
      尚未登入會員
    </div>

    <div class="login-text">
      登入後即可查看自己的貼文與社群摘要
    </div>

    <v-btn
      class="login-btn"
      to="/login"
      flat
    >
      登入會員
    </v-btn>
  </div>

</template>

<script setup>

import CreatePost from '@/components/Community/CreatePost.vue'
import PostsList from '@/components/Community/PostsList.vue'
import MySummary from '@/components/Community/MySummary.vue'

import { ref } from 'vue'

const isLoggedIn = ref(!!localStorage.getItem('token'))

</script>

<style scoped>

.community-layout {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.community-main {
  flex: 1;
  min-width: 0;
}

.post-gap {
  margin-bottom: 12px;
}

.community-right {
  width: 350px;
  flex-shrink: 0;
}


/* 未登入 */

.login-required {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.login-title {
  color: #706455;
  font-size: 24px;
  font-weight: 600;
}

.login-title .mdi{
  font-size: 30px;
}

.login-text {
  margin-top: 10px;
  color: #9A9185;
  font-size: 18px;
}

.login-btn {
  margin-top: 22px;
  background: #F5E8BD;
  color: #795F35;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
}

</style>