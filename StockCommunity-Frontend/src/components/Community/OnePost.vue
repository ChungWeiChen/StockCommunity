<template>
  <div class="post-card">

    <div class="post-header">

      <div class="user-area">

        <img
          v-if="userPhoto"
          :src="userPhoto"
          class="avatar"
        />
     
        <div
          v-else
          class="avatar-fallback"
          :class="{ 'my-avatar': isOwner }"
        >
          {{ userInitial }}
        </div>

        <div class="user-info">

          <span class="username">
            {{ post.username }}
          </span>

          <span class="post-time">
            {{ formatTime( post.updatedAt ? post.updatedAt : post.createdAt) }}
          </span>

          <span
          v-if="post.updatedAt != null"
          class="edited">
            已編輯
          </span>

        </div>

        <button
          v-if="isOwner && !isEditing"
          type="button"
          class="edit-button"
          @click="openEditPost"
        >
        <span class="mdi mdi-pencil"></span>
          編輯
        </button>

      </div>

    </div>


    <div class="post-content">

      <div
        v-if="post.stocks?.length && !isEditing"
        class="stock-area"
      >
        <span
          v-for="stock in post.stocks"
          :key="stock.stockCode"
          class="stock-tag"
          @click="goToStockPosts(stock)"
        >
          <span class="mdi mdi-tag"></span>
          {{ stock.stockCode }} {{ stock.simpleName }}
        </span>
      </div>


      <template v-if="!isEditing">

        <div
          v-if="post.postTitle"
          class="post-title"
        >
          {{ post.postTitle }}
        </div>

        <div class="post-text">
          {{ post.postText }}
        </div>

      </template>


      <template v-else>

        <input
          v-model="editTitle"
          type="text"
          class="edit-title-input"
          placeholder="貼文標題"
        >

        <textarea
          v-model="editText"
          class="edit-text-input"
          placeholder="貼文內容"
        ></textarea>


        <div class="edit-stock-area">

          <div
            v-if="editSelectedStocks.length"
            class="stock-area"
          >
            <span
              v-for="stock in editSelectedStocks"
              :key="stock.stockCode"
              class="stock-tag"
            >
              <span class="mdi mdi-tag"></span>
              {{ stock.stockCode }} {{ stock.simpleName }}

              <button
                type="button"
                class="remove-stock"
                @click="removeEditStock(stock.stockCode)"
              >
                <span class="mdi mdi-close"></span>
              </button>
            </span>
          </div>

          <input
            v-model="editStockKeyword"
            type="text"
            class="edit-stock-search"
            placeholder="搜尋股票代號或名稱"
            @input="searchEditStocks"
            @keydown.enter.prevent="selectFirstEditStock"
          >

          <div
            v-if="editStockResults.length"
            class="stock-results"
          >
            <div
              v-for="stock in editStockResults"
              :key="stock.stockCode"
              class="stock-result"
              @click="selectEditStock(stock)"
            >
              <span class="mdi mdi-tag"></span>
              {{ stock.stockCode }} {{ stock.simpleName }}
            </div>
          </div>

        </div>


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
            v-else-if="post.photoFile && !removeEditPhoto"
            class="edit-photo-preview"
          >
            <button
              type="button"
              class="remove-edit-photo"
              @click="removeOriginalPhoto"
            >
              <span class="mdi mdi-close-circle-outline"></span>
            </button>

            <img :src="'data:image/jpeg;base64,' + post.photoFile">
          </div>

          <label class="edit-photo-button">
            <span class="mdi mdi-image-outline"></span>

            <span>
              {{ post.photoFile && !removeEditPhoto ? "更換圖片" : "新增圖片" }}
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
            @click="updatePost"
          >
            儲存
          </button>

        </div>

      </template>


      <div
        v-if="post.photoFile && !isEditing"
        class="post-photo-box"
      >
        <img
          :src="'data:image/jpeg;base64,' + post.photoFile"
          class="post-photo"
        />
      </div>

    </div>


    <div class="post-actions">

      <button
        type="button"
        class="action-button"
        @click="addLike"
        :style="{color: post.liked ? '#ef4444' : '#6b7280'}"
      >

        <span
          class="mdi"
          :class=" post.liked ? 'mdi-thumb-up' : 'mdi-thumb-up-outline'"
        ></span>

        <span>
          {{ post.postLikeTotal }}
        </span>

      </button>

      <button
        type="button"
        class="action-button"
        @click="addDislike"
        :style="{ color: post.disliked ? '#22c55e' : '#6b7280' }"
      >
        <span class="mdi mdi-leek"></span>

        <span>
          {{ post.postDislikeTotal }}
        </span>
      </button>


      <button
        type="button"
        class="action-button"
        @click="showAllComments = !showAllComments"
        :style="{ color: post.commentTotal > 0 ? '#073763' : '#6b7280'}"
      >

        <span class="mdi mdi-comment"></span>

        <span>
          {{ post.commentTotal }}
        </span>

      </button>

      <button
      @click="openCreateComment"
      type="button"
      class="reply-action-button"
      >
        回覆
      </button>

      <button
      v-if="isOwner"
      type="button"
      class="delete-action-button"
      @click="deletePost"
      >
      <span class="mdi mdi-trash-can-outline"></span>
      <span>刪除</span>
      </button>
        
    </div>
    
    <CreateComment
      v-if="showComment"
      :post-id="post.id"
    />

   <CommentsList
      :comments="post.comments"
      :showAllComments="showAllComments"
      @toggleComments="showAllComments = !showAllComments"
    />

  </div>

    
</template>


<script setup>

import api from "@/api/axios"
import router from "@/router/index";

import { ref, computed } from "vue";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/userStore"

const userStore = useUserStore();
const { profile }= storeToRefs(userStore);

const isOwner = computed(() => {
  return profile.value?.id === props.post.userId;
});

import CommentsList from "@/components/Community/CommentsList.vue";
import CreateComment from "./CreateComment.vue";


const showAllComments = ref(false);
const showComment = ref(false);


const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

const isEditing = ref(false);

const editTitle = ref("");
const editText = ref("");

const editPhoto = ref(null);
const editPreviewImage = ref(null);
const removeEditPhoto = ref(false);

const editStockCodes = ref([]);
const editSelectedStocks = ref([]);

const editStockKeyword = ref("");
const editStockResults = ref([]);

const userPhoto = computed(() => {

  if (props.post?.userPhoto) {
    return `data:image/jpeg;base64,${props.post.userPhoto}`;
  }

  return null;

});


const userInitial = computed(() =>
  props.post?.username
    ?.charAt(0)
    ?.toUpperCase() ?? "?"
);


function formatTime(time) {

  if (!time) {
    return "";
  }

  const postTime = new Date(time);
  const now = new Date();

  const diff = now - postTime;

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

  return postTime.toLocaleString("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  });
}

function goToStockPosts(stock) {
  router.push({
    path: "/findstockpost",
    query: {
      stockCode: stock.stockCode,
      stockName: stock.simpleName 
    }
  });
}

async function addLike() {
  const token = localStorage.getItem("token");

  if (!token) {
    alert("請先登入會員");

    router.push({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath }
    });

    return;
  }

  try {
    const response =
      await api.post(`/api/posts/${props.post.id}/likes`);

    props.post.postLikeTotal = response.data.postLikeTotal;
    props.post.postDislikeTotal = response.data.postDislikeTotal;

    props.post.liked = response.data.liked;
    props.post.disliked = response.data.disliked;

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
      query: { redirect: router.currentRoute.value.fullPath }
    });

    return;
  }

  try {
    const response =
      await api.post(`/api/posts/${props.post.id}/dislikes`);

    props.post.postLikeTotal = response.data.postLikeTotal;
    props.post.postDislikeTotal = response.data.postDislikeTotal;

    props.post.liked = response.data.liked;
    props.post.disliked = response.data.disliked;

  } catch (error) {
    console.log(error);
  }
}

function openEditPost() {
  isEditing.value = true;

  editTitle.value = props.post.postTitle ?? "";
  editText.value = props.post.postText ?? "";

  editSelectedStocks.value = [...(props.post.stocks ?? [])];

  editStockCodes.value = editSelectedStocks.value.map(
    stock => stock.stockCode
  );

  editPhoto.value = null;
  editPreviewImage.value = null;
  removeEditPhoto.value = false;
}

async function updatePost() {
  if (!editText.value.trim()) {
    alert("貼文內容不能為空");
    return;
  }

  const formData = new FormData();

  if (editTitle.value.trim()) {
    formData.append("postTitle",editTitle.value.trim());
  }

  formData.append("postText", editText.value.trim());

  if (editPhoto.value) {
    formData.append("photoFile",editPhoto.value);
  }

  editStockCodes.value.forEach((stockCode) => {
    formData.append("stockCodes", stockCode);
  });
  formData.append("updateStocks", true);

  formData.append("removePhoto",removeEditPhoto.value);

  try {
    const response = await api.patch(
      `/api/posts/edit/${props.post.id}`,
      formData
    );

    console.log("編輯成功", response.data);
    window.location.reload();
  } catch (error) {
    console.error("編輯失敗", error);
    alert(error.response?.data || "編輯失敗");
  }
}

async function searchEditStocks() {
  if (!editStockKeyword.value.trim()) {
    editStockResults.value = [];
    return;
  }

  try {
    const response = await api.get("/api/stocks/search", {
      params: {
        keyword: editStockKeyword.value.trim()
      }
    });

    editStockResults.value = response.data.filter(
    stock => !editStockCodes.value.includes(stock.stockCode)
    );

  } catch (error) {
    console.error("搜尋股票失敗", error);
  }
}


function selectFirstEditStock() {

  if (!editStockResults.value.length) {
    return;
  }

  selectEditStock(editStockResults.value[0]);
}

function selectEditStock(stock) {

  // 已經選過就不重複加入
  if (editStockCodes.value.includes(stock.stockCode)) {
    return;
  }

  editStockCodes.value.push(stock.stockCode);
  editSelectedStocks.value.push(stock);

  // 選完後清空搜尋框與搜尋結果
  editStockKeyword.value = "";
  editStockResults.value = [];
}


function removeEditStock(stockCode) {
  editSelectedStocks.value =
    editSelectedStocks.value.filter(
      stock => stock.stockCode !== stockCode
    );

  editStockCodes.value =
    editStockCodes.value.filter(
      code => code !== stockCode
    );
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
  editPreviewImage.value = URL.createObjectURL(file);

  // 選了新圖片，就不是單純刪除圖片
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


async function deletePost(){
   try{
    await api.delete(`/api/posts/delete/${props.post.id}`);
    window.location.reload();
   alert("刪除成功");
   }catch(error){
    console.log(error);
   }

}

async function openCreateComment() {
  const token = localStorage.getItem("token");

  if(!token){
    alert("您還沒有登入會員，請先登入會員。");
    // 原本：router.push('/login')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }

  showComment.value = !showComment.value;
}


</script>


<style scoped>

.post-card {
  width: 100%;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}


.post-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}


.user-area {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}


.avatar {
  width: 55px;
  height: 55px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  width: 45px;
  height: 45px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #E6DCEF;
  color: #765A8C;
  font-size: 20px;
  font-weight: 600;
}

.my-avatar {
  background: #f3ead8;
  color: #7c6848;
}


.user-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
}


.username {
  color: #111827;
  font-size: 22px;
  font-weight: 700;
}


.post-time {
  margin-left: 10px;
  font-size: 16px;
  font-weight: 400;
  color: #9ca3af;
}


.post-content {
  margin-left: 54px;
  margin-top: 4px;
}


.post-title {
  margin-left: 5px;
  color: #374151;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
}


.post-text {
  margin-top: 5px;
  margin-left: 25px;
  color: #374151;
  font-size: 18px;
  font-weight: 400;
  line-height: 1.5;
  white-space: pre-wrap;
}

.stock-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  background: #FFF3D6;
  border-radius: 20px;
  color: #A56F27;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.2;
  cursor: pointer;
}

.stock-tag .mdi {
  font-size: 16px;
}

.stock-result {
  padding: 10px 12px;
  color: #374151;
  font-size: 16px;
  cursor: pointer;
}

.stock-result:hover {
  background: #d6e8ff;
}

.stock-results {
  max-height: 132px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.post-photo-box {
  width: 100%;
  margin-top: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}


.post-photo {
  max-width: 80%;
  max-height: 250px;
  object-fit: contain;
  display: block;
}


.post-actions {
  margin-left: 54px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 42px;
}


.action-button {
  padding: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 16px;
  cursor: pointer;
}

.action-button .mdi {
  font-size: 21px;
}

.action-button:hover {
  transform: translateY(-1px);
}


.reply-action-button {
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

.reply-action-button:hover {
  background: #ebe4f2;
  color: #543E68;
  transform: translateY(-1px);
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
  font-size: 18px;
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
  font-size: 20px;
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
  font-size: 20px;
  cursor: pointer;
  margin-right: 30px;
}

.edit-save-button:hover,
.edit-cancel-button:hover {
  border: solid 2px #6a624b;
  color: #e8c66a;
  font-weight: 500;
}

.edit-title-input {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  color: #374151;
  font-family: inherit;
  font-size: 20px;
}

.edit-text-input {
  width: 100%;
  min-height: 90px;
  box-sizing: border-box;
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  resize: vertical;
  color: #374151;
  font-family: inherit;
  font-size: 18px;
  line-height: 1.5;
}

.edit-title-input:focus,
.edit-text-input:focus {
  background: white;
  border-color: #2563eb;
}

.edit-stock-area {
  position: relative;
  margin-top: 10px;
}

.edit-stock-search {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  font-family: inherit;
  font-size: 16px;
}

.edit-stock-search:focus{
  border-color: #2563eb;
}

.remove-stock {
  padding: 0;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
}

.edit-photo-area {
  margin-top: 12px;
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
  max-height: 350px;
  object-fit: contain;
}

.remove-edit-photo {
  position: absolute;
  top: -10px;
  right: -40px;
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
  font-size: 15px;
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
  font-size: 18px;
  cursor: pointer;
  margin-left: auto;
  margin-right: 30px;
}

.delete-action-button:hover {
  background: #fee2e2;
  color: #dc2626;
  transform: translateY(-1px);
}

.stock-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: 20px;
  margin-bottom: 10px;
}


</style>