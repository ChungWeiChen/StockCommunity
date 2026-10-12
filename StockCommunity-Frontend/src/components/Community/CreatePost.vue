<template>
  <div class="create-post">

    <div class="create-top">

      <div class="avatar-wrapper">

        <img
          v-if="userPhoto"
          :src="userPhoto"
          class="avatar"
        >

        <div
          v-else
          class="avatar-fallback"
        >
          {{ userInitial }}
        </div>

      </div>

      <!-- 還沒展開 -->
      <div
        v-if="!isExpanded"
        class="start-input"
        @click="openEditor"
      >
        有什麼投資想法想分享嗎？
      </div>

      <!-- 展開後 -->
      <input
        v-else
        v-model="postTitle"
        type="text"
        class="title-input"
        placeholder="貼文標題"
      >

    </div>


    <div
      v-if="isExpanded"
      class="editor"
    >

      <textarea
        ref="textInput"
        v-model="postText"
        class="text-input"
        placeholder="分享你的投資想法..."
      ></textarea>


      <div class="create-actions">

        <div class="left-actions">

          <label class="action-button">

            <span class="mdi mdi-image-outline"></span>
            <span>圖片</span>

            <input
              type="file"
              accept="image/*"
              hidden
              @change="selectPhoto"
            >

          </label>

          <button
            type="button"
            class="action-button"
            @click="toggleStockSelector"
          >
            <span class="mdi mdi-tag-outline"></span>
            <span>股票標籤</span>
          </button>

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
            @click="createPost"
          >
            {{ isSubmitting ? "發布中..." : "發布" }}
          </button>

        </div>

      </div>


      <!-- 股票選擇區 -->
      <div
        v-if="showStockSelector"
        class="stock-selector"
      >

        <!-- 已選擇的股票 -->
        <div
          v-if="selectedStocks.length"
          class="selected-stocks"
        >
          <span
            v-for="stock in selectedStocks"
            :key="stock.stockCode"
            class="stock-tag"
          >
            <span class="mdi mdi-tag"></span>
            {{ stock.stockCode }} {{ stock.simpleName }}
              <button
              type="button"
              class="remove-stock"
              @click="removeStock(stock.stockCode)"
            >
              <span class="mdi mdi-close"></span>
            </button>
          </span>
        </div>


        <!-- 股票搜尋 -->
        <input
          ref="stockSearchInput"
          v-model="stockKeyword"
          type="text"
          class="stock-search-input"
          placeholder="搜尋股票代號或名稱"
          @input="searchStocks"
          @keydown.enter.prevent="selectFirstStock"
        >

        <!-- 搜尋結果 -->
      <div
        v-if="stockResults.length"
        class="stock-results"
      >
        <div
          v-for="stock in stockResults"
          :key="stock.stockCode"
          class="stock-result"
          @click="selectStock(stock)"
        >
          {{ stock.stockCode }} {{ stock.simpleName }}
        </div>
      </div>

      </div>

      <div
        v-if="previewImage"
        class="preview-box"
      >
        <div class="preview-image-wrapper">

        <button
          type="button"
          class="remove-image"
          @click="removePhoto"
        >
          <span class="mdi mdi-close-circle-outline"></span>
        </button>

        <img
          :src="previewImage"
          alt="貼文圖片預覽"
        >

        </div>
      </div>
    </div>
  </div>
</template>


<script setup>

import api from "@/api/axios";
import router from "@/router/index"

import { ref, nextTick, computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useUserStore  } from "@/stores/userStore";


const userStore = useUserStore();
const { profile } = storeToRefs(userStore);

onMounted(() => {
  const token = localStorage.getItem("token");

  if (token && !profile.value) {
    userStore.fetchProfile();
  }
});


const userPhoto = computed(() => {

  if (profile.value?.userPhoto) {
    return `data:image/jpeg;base64,${profile.value.userPhoto}`;
  }
  return null;
});

const userInitial = computed(() =>
  profile.value?.username?.charAt(0)?.toUpperCase() ?? "?"
);

const postTitle = ref("");
const postText = ref("");
const photoFile = ref(null);
const previewImage = ref(null);

const stockCodes = ref([]);
const showStockSelector = ref(false);

const isExpanded = ref(false);
const isSubmitting = ref(false);
const textInput = ref(null);

async function openEditor() {
  const token = localStorage.getItem("token");

  if(!token){
    alert("您還沒有登入會員，請先登入會員。");
    // 原本：router.push('/login')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }

  isExpanded.value = true;
  // 等 Vue 把 input 顯示出來
  await nextTick();
  // 自動 focus
  textInput.value?.focus();

}


function closeEditor() {
  postTitle.value = "";
  postText.value = "";
  photoFile.value = null;

  stockCodes.value = [];
  selectedStocks.value = [];
  stockKeyword.value = "";
  stockResults.value = [];
  showStockSelector.value = false;

  if (previewImage.value) {
    URL.revokeObjectURL(previewImage.value);
  }

  previewImage.value = null;
  isExpanded.value = false;
}


function selectPhoto(event) {
  const file =  event.target.files[0];

  if (!file) {
    return;
  }

  if (previewImage.value) {
    URL.revokeObjectURL( previewImage.value);
  }
  photoFile.value = file;
  previewImage.value = URL.createObjectURL(file);
}


function removePhoto() {
  photoFile.value = null;

  if (previewImage.value) { URL.revokeObjectURL( previewImage.value);
  }
  previewImage.value = null;
}

const stockKeyword = ref("");
const stockResults = ref([]);
const selectedStocks = ref([]);
const stockSearchInput = ref(null);

async function toggleStockSelector() {

  showStockSelector.value = !showStockSelector.value;

  if (showStockSelector.value) {

    await nextTick();

    stockSearchInput.value?.focus();
  }
}

async function searchStocks() {
  if (!stockKeyword.value.trim()) {
    stockResults.value = [];
    return;
  }

  try {
    const response = await api.get("/api/stocks/search", {
      params: {
        keyword: stockKeyword.value.trim()
      }
    });
    

    stockResults.value = response.data.filter(
      stock => !stockCodes.value.includes(stock.stockCode)
    );

  } catch (error) {
    console.error(error);
  }
}

function selectFirstStock() {
  if (!stockResults.value.length) {
    return;
  }

  selectStock(stockResults.value[0]);
}

function selectStock(stock) {

  // 已經選過就不要重複加入
  if (stockCodes.value.includes(stock.stockCode)) {
    return;
  }

  stockCodes.value.push(stock.stockCode);
  selectedStocks.value.push(stock);

  // 選完清空搜尋
  stockKeyword.value = "";
  stockResults.value = [];
}

function removeStock(stockCode) {
  selectedStocks.value = selectedStocks.value.filter(
    stock => stock.stockCode !== stockCode
  );

  stockCodes.value = stockCodes.value.filter(
    code => code !== stockCode
  );
}


async function createPost() {
  // 防止重複點擊
  if (isSubmitting.value) {
     return;
    }

  if (!postText.value.trim()) {
    alert("貼文內容不能為空");
    return;
  }

  const formData = new FormData();

  if (postTitle.value.trim()) {
    formData.append("postTitle", postTitle.value.trim());
  }

  formData.append("postText", postText.value);

  if (photoFile.value) {
    formData.append("photoFile", photoFile.value);
  }

  stockCodes.value.forEach((stockCode) => {
    formData.append("stockCodes", stockCode);
  });


  try {
    isSubmitting.value = true;

    const response =
      await api.post(
        "/api/posts/add",
        formData
      );

    console.log("發布成功", response);
    closeEditor();
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

.create-post {
  width: 100%;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}


.create-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 45px;
  height: 45px;
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
  background: #f3ead8;
  color: #7c6848;
  font-size: 20px;
  font-weight: 600;
}

.start-input {
  flex: 1;
  padding: 13px 18px;
  background: #f3f4f6;
  border: 1px solid transparent;
  border-radius: 24px;
  color: #6b7280;
  font-size: 18px;
  cursor: text;
  transition: 0.2s;
}

.start-input:hover {
  background: #eef0f3;
  border-color: #dbe2ea;
}

.title-input {
  flex: 1;
  padding: 10px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  outline: none;
  color: #111827;
  font-size: 19px;
  transition: 0.2s;
}

.text-input {
  width: 100%;
  margin-top: 12px;
  padding: 14px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  outline: none;
  resize: vertical;
  color: #374151;
  font-size: 18px;
  line-height: 1.7;
  transition: 0.2s;
}

.title-input:focus,
.text-input:focus {
  background: white;
  border-color: #2563eb;
}

.title-input::placeholder,
.text-input::placeholder {
  color: #9ca3af;
}

.preview-box {
  width: 100%;
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

.preview-image-wrapper {
  position: relative;
  display: inline-block;
}

.preview-image-wrapper img {
  display: block;
  max-width: 100%;
  max-height: 400px;
  width: auto;
  height: auto;
  object-fit: contain;
}

.remove-image {
  position: absolute;
  top: -12px;
  right: -40px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  z-index: 2;
}

.remove-image .mdi {
  font-size: 34px;
  color: #838181;
}

.remove-image .mdi:hover {
  color: #191a1b;
}

.create-actions {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.left-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-button {
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


.action-button:hover {
  background: #d6e8ff;
  color: #2c2822;
}

.action-button .mdi {
  font-size: 20px;
}

.right-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cancel-button {
  padding: 10px 18px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #6b7280;
  font-family: inherit;
  font-size: 18px;
  cursor: pointer;
}

.cancel-button:hover {
  background: #f3f4f6;
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

.stock-selector {
  position: relative;
  width: 100%;
  margin-top: 12px;
}

.selected-stocks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
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

.stock-search-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d9d4ca;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  font-family: inherit;
  font-size: 16px;
  outline: none;
  transition: 0.2s;
}

.stock-search-input:focus{
  border-color: #2563eb;
}

.stock-results {
  margin-top: 4px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  max-height: 195px;
  overflow-y: auto;
}

.stock-result {
  padding: 9px 12px;
  color: #374151;
  font-size: 15px;
  cursor: pointer;
  transition: 0.15s;
}

.stock-result:hover {
  background: #d6e8ff;
}

.remove-stock {
  padding: 0;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.remove-stock .mdi {
  font-size: 16px;
}

.remove-stock:hover {
  color: #111827;
}
</style>