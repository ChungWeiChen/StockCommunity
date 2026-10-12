<template>
  <div class="comment-list">

    <img
      v-if="userPhoto"
      :src="userPhoto"
      class="comment-avatar"
    />

    <div
      v-else
      class="comment-avatar-fallback"
      :class="{ 'my-avatar': isOwner }"
    >
      {{ userInitial }}
    </div>


   
    <div class="comment-main">

      <div class="comment-header">

        <span class="comment-username">
          {{ comment.username }}
        </span>

        <span class="comment-time">
          {{ formatTime(comment.updatedAt ? comment.updatedAt : comment.createdAt) }}
        </span>

        <span
          v-if="comment.updatedAt != null"
          class="edited">
            已編輯
        </span>

        <button
          v-if="isOwner && !isEditing"
          type="button"
          class="edit-button"
          @click="openEditComment"
        >
        <span class="mdi mdi-pencil"></span>
          編輯
      </button>

      </div>


      <template v-if="!isEditing">

        <div class="comment-text">
          {{ comment.commentText }}
        </div>

        <div
          v-if="comment.commentPhoto"
          class="comment-photo-box"
        >
          <img
            :src="'data:image/jpeg;base64,' + comment.commentPhoto"
            class="comment-photo"
          />
        </div>

      </template>


      <template v-else>

        <textarea
          v-model="editText"
          class="edit-text-input"
          placeholder="留言內容"
        ></textarea>


        <div class="edit-photo-area">

          <div
            v-if="editPreviewImage"
            class="edit-photo-preview"
          >
            <button
              type="button"
              class="remove-edit-photo"
              @click="removeSelectedEditPhoto"
            >
              <span class="mdi mdi-close-circle-outline"></span>
            </button>

            <img :src="editPreviewImage">
          </div>

          <div
            v-else-if="comment.commentPhoto && !removeEditPhoto"
            class="edit-photo-preview"
          >
            <button
              type="button"
              class="remove-edit-photo"
              @click="removeOriginalPhoto"
            >
              <span class="mdi mdi-close-circle-outline"></span>
            </button>

            <img :src="'data:image/jpeg;base64,' + comment.commentPhoto">
          </div>

          <label class="edit-photo-button">
            <span class="mdi mdi-image-outline"></span>

            <span>
              {{ comment.commentPhoto && !removeEditPhoto ? "更換圖片" : "新增圖片" }}
            </span>

            <input
              type="file"
              accept="image/*"
              hidden
              @change="selectEditPhoto"
            >
          </label>

        </div>

        <div class="edit-actions">

          <button
            type="button"
            class="edit-cancel-button"
            @click="isEditing = false"
          >
            取消
          </button>

          <button
            type="button"
            class="edit-save-button"
            @click="updateComment"
          >
            儲存
          </button>

        </div>

      </template>
      
      <div class="comment-actions">

        <button
        type="button"
        class="comment-action-button"
        @click="addLike"
        :style="{color: comment.liked ? '#ef4444' : '#6b7280'}"
      >

        <span
          class="mdi"
          :class=" comment.liked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
        ></span>

        <span>
          {{ comment.commentLikeNum }}
        </span>

      </button>

      <button
        type="button"
        class="comment-action-button"
        @click="addDislike"
        :style="{ color: comment.disliked ? '#22c55e' : '#6b7280' }"
      >
        <span class="mdi mdi-leek"></span>

        <span>
          {{ comment.commentDislikeNum }}
        </span>
      </button>

      <button
        v-if="isOwner"
        type="button"
        class="delete-action-button"
        @click="deleteComment"
        >
        <span class="mdi mdi-trash-can-outline"></span>
        <span>刪除</span>
      </button>
      </div>

    </div>

  </div>
  
</template>


<script setup>

import api from "@/api/axios";
import router from "@/router/index";

import { ref, computed } from "vue";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/userStore";

const userStore = useUserStore();
const { profile } = storeToRefs(userStore);

const props = defineProps({
  comment: {
    type: Object,
    required: true
  }
});

const isEditing = ref(false);

const editText = ref("");

const editPhoto = ref(null);
const editPreviewImage = ref(null);
const removeEditPhoto = ref(false);

const isOwner = computed(() => {
  return profile.value?.id === props.comment.userId;
});


const userPhoto = computed(() => {

  if (props.comment?.userPhoto) {

    return `data:image/jpeg;base64,${props.comment.userPhoto}`;

  }

  return null;

});


const userInitial = computed(() =>

  props.comment?.username ?.charAt(0) ?.toUpperCase() ?? "?"

);


function formatTime(time) {

  if (!time) {
    return "";
  }

  const commentTime = new Date(time);
  const now = new Date();

  const diff = now - commentTime;

  const minutes =
    Math.floor(diff / 1000 / 60);

  const hours =
    Math.floor(diff / 1000 / 60 / 60);


  if (minutes < 1) {
    return "剛剛";
  }

  if (minutes < 60) {
    return `${minutes} 分鐘前`;
  }

  if (hours < 24) {
    return `${hours} 小時前`;
  }

  return commentTime.toLocaleString("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  });
}

function openEditComment() {

  isEditing.value = true;

  editText.value = props.comment.commentText ?? "";

  editPhoto.value = null;
  editPreviewImage.value = null;
  removeEditPhoto.value = false;
}

function selectEditPhoto(event) {
  const file = event.target.files[0];

  if (!file) {
    return;
  }

  if (editPreviewImage.value) {
    URL.revokeObjectURL(editPreviewImage.value);
  }

  editPhoto.value = file;

  editPreviewImage.value =
    URL.createObjectURL(file);

  // 選擇新圖片後，不是刪除圖片
  removeEditPhoto.value = false;
}

function removeSelectedEditPhoto() {
  if (editPreviewImage.value) {
    URL.revokeObjectURL(editPreviewImage.value);
  }

  editPhoto.value = null;
  editPreviewImage.value = null;
}

function removeOriginalPhoto() {
  editPhoto.value = null;
  editPreviewImage.value = null;

  removeEditPhoto.value = true;
}

async function updateComment() {
  if (!editText.value.trim()) {
    alert("留言內容不能為空");
    return;
  }

  const formData = new FormData();

  formData.append("commentText",editText.value.trim());

  if (editPhoto.value) {
    formData.append("commentPhoto",editPhoto.value);
  }

  formData.append("removePhoto",removeEditPhoto.value);

  try {
    const response = await api.patch(
      `/api/comments/edit/${props.comment.id}`,
      formData
    );

    console.log("留言編輯成功", response.data);
    window.location.reload();

  } catch (error) {
    console.error(error);
    alert(error.response?.data || "留言編輯失敗");
  }
}


async function deleteComment(){
   try{
    await api.delete(`/api/comments/delete/${props.comment.id}`);
    window.location.reload();
   alert("刪除成功");
   }catch(error){
    console.log(error);
   }
}

async function addLike() {
  const token = localStorage.getItem("token");

  if (!token) {
    alert("請先登入會員");

    router.push({
      path: "/login",
      query: {
        redirect: router.currentRoute.value.fullPath
      }
    });

    return;
  }

  try {
    const response =
      await api.post(`/api/comments/${props.comment.id}/likes`);

    props.comment.commentLikeNum = response.data.commentLikeNum;

    props.comment.commentDislikeNum = response.data.commentDislikeNum;

    props.comment.liked = response.data.liked;

    props.comment.disliked = response.data.disliked;

  } catch (error) {
    console.log(error);
  }
}

async function addDislike() {
  const token = localStorage.getItem("token");

  if (!token) {
    alert("請先登入會員");

    router.push({
      path: "/login",
      query: {
        redirect: router.currentRoute.value.fullPath
      }
    });

    return;
  }

  try {
    const response =
      await api.post(`/api/comments/${props.comment.id}/dislikes`);

    props.comment.commentLikeNum = response.data.commentLikeNum;

    props.comment.commentDislikeNum = response.data.commentDislikeNum;

    props.comment.liked = response.data.liked;

    props.comment.disliked = response.data.disliked;

  } catch (error) {
    console.log(error);
  }
}

</script>


<style scoped>

.comment-list {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-bottom: 12px;
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
  background: #E6DCEF;
  color: #765A8C;
  font-size: 20px;
  font-weight: 600;
}

.my-avatar {
  background: #f3ead8;
  color: #7c6848;
}

.comment-main {
  flex: 1;
  min-width: 0;
  padding: 8px 14px;
  background-color: #F7F2FB;
  border-radius: 18px;
}


.comment-header {
  display: flex;
  align-items: center;
  gap: 5px;
}


.comment-username {
  color: #111827;
  font-size: 16px;
  font-weight: 600;
}


.comment-time {
  margin-left: 10px;
  color: #9ca3af;
  font-size: 16px;
}


.comment-text {
  margin-top: 1px;
  margin-left: 3px;
  color: #374151;
  font-size: 18px;
  line-height: 1.5;
  white-space: pre-wrap;
}


.comment-photo-box {
  margin-top: 8px;
  max-width: 260px;
  overflow: hidden;
  border-radius: 10px;
}


.comment-photo {
  width: 100%;
  max-height: 180px;
  object-fit: contain;
  display: block;
}


.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 5px;
}


.comment-action-button {
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

.edit-button{
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
  margin-left: auto;
  margin-right: 30px;
}

.edit-button:hover {
  background: #CFE2F3;
  color: #0089d8;
  transform: translateY(-1px);
}

.edit-cancel-button{
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
}

.edit-save-button{
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
  margin-right: 30px;
}

.edit-save-button:hover,
.edit-cancel-button:hover {
  border: solid 2px #6a624b;
  color: #e8c66a;
  font-weight: 500;
}

.edit-text-input {
  width: 100%;
  min-height: 60px;
  box-sizing: border-box;
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  resize: vertical;
  color: #374151;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.5;
}

.edit-text-input:focus {
  background: white;
  border-color: #2563eb;
}

.edit-photo-area {
  margin-top: 10px;
}

.edit-photo-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  color: #6b7280;
  cursor: pointer;
}

.edit-photo-button:hover {
  background: #d6e8ff;
}

.edit-photo-preview {
  position: relative;
  width: fit-content;
  max-width: 100%;
  margin-top: 10px;
}

.edit-photo-preview img {
  display: block;
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
}

.remove-edit-photo {
  position: absolute;
  top: -10px;
  right: -30px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.remove-edit-photo .mdi {
  font-size: 30px;
}

.edit-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.edited{
  font-size: 14px;
  color: #6b7280;
  margin-left: 10px;
}

.delete-action-button {
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
  margin-left: auto;
  margin-right: 30px;
}

.delete-action-button:hover {
  background: #fee2e2;
  color: #dc2626;
  transform: translateY(-1px);
}

</style>