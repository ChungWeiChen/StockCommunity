<template>

  <div
    v-if="comments && comments.length > 0"
    class="comment-section"
  >

    <div class="comment-divider"></div>

    <OneComment
      v-for="comment in visibleComments"
      :key="comment.id"
      :comment="comment"
    />

    <button
      v-if="comments.length > 3"
      type="button"
      class="more-comments-button"
      @click="emit('toggleComments')"
    >
      {{ showAllComments ? "收起留言" : `查看更多留言 (${comments.length - 3})` }}
    </button>
    

  </div>

</template>


<script setup>
import { ref, computed } from "vue";

import OneComment from "@/components/Community/OneComment.vue";


const props =  defineProps({
  comments: {
    type: Array,
    default: () => []
  },

  showAllComments: {
    type: Boolean,
    default: false
  }

});

const emit = defineEmits([
  "toggleComments"
]);

const visibleComments = computed(() => {
  if (props.showAllComments) {
    return props.comments;
  }
  return props.comments.slice(0, 3);
});


console.log("CommentsList 收到：", props.comments);
</script>


<style scoped>

.comment-section {
  margin-left: 55px;
  margin-top: 12px;
}

.comment-divider {
  height: 1px;
  margin-bottom: 16px;
  background: #e5e7eb;
}

.more-comments-button{
  padding: 0;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
}

.more-comments-button:hover{
  color: #374151;
  font-weight: 600;
  transform: translateY(-1px);
}

</style>