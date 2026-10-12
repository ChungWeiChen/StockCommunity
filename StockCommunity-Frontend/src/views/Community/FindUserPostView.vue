<template>
  <div class="community-layout">

    <!-- 左邊 -->
    <div class="community-main">

      <UserSearch
        :username="selectedUsername"
        @select-user="handleSelectUser"
        @clear-user="handleClearUser"
      />

      <div class="post-gap"></div>

      <PostsList
        v-if="selectedUsername"
        type="username"
        :username="selectedUsername"
      />

    </div>

    <!-- 右邊 -->
    <div class="community-right">

      <LeaderboardList />

    </div>

  </div>
</template>

<script setup>
import { ref } from "vue";

import UserSearch from "@/components/Community/UserSearch.vue";
import PostsList from "@/components/Community/PostsList.vue";
import LeaderboardList from "@/components/Community/LeaderboardList.vue";

const selectedUsername = ref("");

function handleSelectUser(user) {
  selectedUsername.value = user.username;

  console.log("選到的使用者：", user);
}

function handleClearUser() {
  selectedUsername.value = "";
}
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
  height: 8px;
}

.community-right {
  width: 350px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
</style>