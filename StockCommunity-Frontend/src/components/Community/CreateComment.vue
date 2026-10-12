<template>
  <div class="comment-card">

    <img
      v-if="userPhoto"
      :src="userPhoto"
      class="comment-avatar"
    />

    <div
      v-else
      class="comment-avatar-fallback"
    >
      {{ userInitial }}
    </div>

    <div class="comment-main">

      <div class="comment-header">

        <span class="comment-username">
          {{ profile?.username }}
        </span>

      </div>

      <textarea
        ref="textInput"
        v-model="commentText"
        class="comment-input"
        placeholder="還有其他想法嗎..."
      ></textarea>

      <div
        v-if="previewImage"
        class="comment-photo-box"
      >
        <img
          :src="previewImage"
          class="comment-photo"
        />

         <button
          type="button"
          class="remove-image"
          @click="removePhoto"
        >
          <span class="mdi mdi-close-circle-outline"></span>
        </button>
      </div>


      
      <div class="comment-actions">

        <div class="left-actions">

          <label class="comment-action-button">

            <span class="mdi mdi-image-outline"></span>
            <span>圖片</span>

            <input
              type="file"
              accept="image/*"
              hidden
              @change="selectPhoto"
            >

          </label>

        </div>

      <div class="right-actions">

          <button
            type="button"
            class="cancel-button"
            @click="closeEditor"
          >
            取消
          </button>

          <button
            type="button"
            class="publish-button"
            :disabled="isSubmitting"
            @click="createComment"
          >
            {{ isSubmitting ? "發布中..." : "發布" }}
          </button>

        </div>
      </div>

    </div>

  </div>
  
</template>


<script setup>

import api from "@/api/axios";

import { ref, computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/userStore";

const userStore = useUserStore();
const { profile } = storeToRefs(userStore);
 
onMounted(() => {
    const token = localStorage.getItem("token");

    if(token && !profile.value){
        userStore.fetchProfile();
    }
    
    textInput.value?.focus();
});

const userPhoto = computed(() => {
    if(profile.value?.userPhoto) {
        return `data:image/jpeg;base64,${profile.value.userPhoto}`;
    }
    return null;
});

const userInitial = computed(() =>
    profile.value?.username?.charAt(0)?.toUpperCase() ?? "?"
);


const props = defineProps({
    postId: {
        type: Number,
        required: true
    }
})

const textInput = ref(null);
const isSubmitting = ref(false);

const commentText = ref("");
const commentPhoto = ref(null);
const previewImage = ref(null);

const emit = defineEmits(["close"]);

function closeEditor() {
  commentText.value = "";
  commentPhoto.value = null;
  if (previewImage.value) {
    URL.revokeObjectURL( previewImage.value);
  }
  previewImage.value = null;
}

function selectPhoto(event) {
    const file = event.target.files[0];

    if(!file){
        return;
    }

    if(previewImage.value){
        URL.revokeObjectURL(previewImage.value);
    }
    commentPhoto.value = file;
    previewImage.value = URL.createObjectURL(file);
}

function removePhoto() {
  commentPhoto.value = null;

  if (previewImage.value) { URL.revokeObjectURL( previewImage.value);
  }
  previewImage.value = null;
}

async function createComment(){
    if (isSubmitting.value) {
        return;
    }

    if (!commentText.value.trim()) {
    alert("留言內容不能為空");
    return;
  }

  const formData = new FormData();

  if(commentText.value.trim()){
    formData.append("commentText", commentText.value.trim());
  }

  if(commentPhoto.value){
    formData.append("commentPhoto", commentPhoto.value);
  }

  try{
    isSubmitting.value = true;

    const response = 
        await api.post(
            `/api/posts/${props.postId}/comments`,
            formData
        );

    console.log("發布成功", response);
    window.location.reload();
  } catch (error) {
    console.error(  "發布失敗", error);
    alert( error.response?.data|| "發布失敗");
  } finally {
    isSubmitting.value = false;
  }
}

</script>


<style scoped>

.comment-card {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-bottom: 12px;
  margin-left: 55px;
}


.comment-avatar,
.comment-avatar-fallback {
  width: 45px;
  height: 45px;
  flex-shrink: 0;
  border-radius: 50%;
}


.comment-avatar {
  object-fit: cover;
  display: block;
}


.comment-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3ead8;
  color: #7c6848;
  font-size: 20px;
  font-weight: 600;
}


.comment-main {
  flex: 1;
  min-width: 0;
  padding: 8px 14px;
  background-color: #f0f1f3;
  border-radius: 20px;
}


.comment-header {
  display: flex;
  align-items: center;
  gap: 5px;
}


.comment-username {
  color: #111827;
  font-size: 22px;
  font-weight: 600;
}


.comment-input {
  width: 100%;
  min-height: 70px;
  box-sizing: border-box;
  margin-top: 4px;
  margin-left: 0;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.75);
  outline: none;
  resize: vertical;
  color: #374151;
  font-family: inherit;
  font-size: 18px;
  line-height: 1.5;
}


.comment-photo-box {
  position: relative;
  display: inline-block;
  width: fit-content;
  max-width: 260px;
  margin-top: 8px;
  overflow: visible;
}


.comment-photo {
  display: block;
  width: auto;
  max-width: 260px;
  max-height: 180px;
  object-fit: contain;
}

.remove-image {
  position: absolute;
  top: -10px;
  right: -40px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.remove-image .mdi {
  font-size: 30px;
  color: #655b5bda;
}

.remove-image .mdi:hover {
  color: #191a1b;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 5px;
}


.comment-action-button {
  padding: 8px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: inherit;
  font-size: 18px;
  cursor: pointer;
  transition: 0.2s;
}

.comment-action-button:hover {
  background: #d6e8ff;
  color: #2c2822;
}

.comment-action-button .mdi {
  font-size: 20px;
}

.left-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.right-actions {
  display: flex;
  margin-left: auto;
  margin-right: 10px;
  align-items: center;
  gap: 8px;
}

.cancel-button {
  padding: 9px 16px;
  border: none;
  border-radius: 9px;
  color: #6b7280;
  font-family: inherit;
  font-size: 18px;
  cursor: pointer;
}

.cancel-button:hover {
  background: #d1d5db;
  color: #111827;
}

.publish-button {
  min-width: 76px;
  padding: 10px 22px;
  border: none;
  border-radius: 10px;
  background: #8c8376;
  color: #bddaff;
  font-family: inherit;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.publish-button:hover:not(:disabled) {
  background: #706455;
  transform: translateY(-1px);
}


.publish-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

</style>