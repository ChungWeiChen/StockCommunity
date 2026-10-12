<template>
  <div class="community-layout">
    <!-- 左邊 -->
    <div class="community-main">
      <CreatePost />

      <div class="post-gap"></div>

      <PostsList />
    </div>

    <!-- 右邊 -->
    <div class="community-news">
      <IndustryMomentum />

      <div class="news-gap"></div>

      <NewsList />
    </div>
  </div>
</template>

<script setup>
import CreatePost from '@/components/Community/CreatePost.vue'
import PostsList from '@/components/Community/PostsList.vue'
import NewsList from '@/components/Community/NewsList.vue'
import IndustryMomentum from '@/components/Community/IndustryMomentum.vue'
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
  height: 12px;
}

.community-news {
  width: 360px;
  flex-shrink: 0;
}

.news-gap {
  height: 16px;
}
</style>
