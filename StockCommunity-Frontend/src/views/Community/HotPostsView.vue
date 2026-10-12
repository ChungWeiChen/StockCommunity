<template>
  <div class="community-layout">

    <!-- 左邊 -->
    <div class="community-main">

      <CreatePost />

      <div class="post-gap"></div>

      <PostsList type="hot" />

    </div>


    <!-- 右邊 -->
    <div class="community-right">

      <PopularStocks />

    </div>

  </div>
</template>


<script setup>
import CreatePost from "@/components/Community/CreatePost.vue";
import PostsList from "@/components/Community/PostsList.vue";
import PopularStocks from "@/components/Community/PopularStocks.vue";
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

</style>