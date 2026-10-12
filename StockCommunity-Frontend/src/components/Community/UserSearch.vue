<template>
  <div class="find-user">

    <div class="search-area">

      <div class="input-area">

        <input
          ref="userSearchInput"
          v-model="keyword"
          class="search-input"
          placeholder="請輸入想搜尋的使用者名稱:"
          @keyup.enter="search"
        />

        <button
          v-if="keyword"
          type="button"
          class="cancel-button"
          @click="cancelSearch"
        >
          <span class="mdi mdi-close"></span>
        </button>

      </div>

      <button
        type="button"
        class="publish-button"
        @click="search"
      >
        <span class="mdi mdi-magnify"></span>
        搜尋
      </button>

    </div>

    <!-- 搜尋結果 -->
    <div
      v-if="users.length > 0"
      class="user-list"
    >
      <span
        v-for="user in users"
        :key="user.id"
        class="user-item"
        @click="selectUser(user)"
      >
        <span class="mdi mdi-account"></span>
        {{ user.username }}
      </span>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import api from "@/api/axios";

const userSearchInput = ref(null);

const keyword = ref("");
const users = ref([]);

const emit = defineEmits([
  "select-user",
  "clear-user"
]);

onMounted(() => {
  userSearchInput.value?.focus();
});

async function search() {

  if (!keyword.value.trim()) {
    return;
  }

  try {

    const response = await api.get(
      "/api/posts/search",
      {
        params: {
          username: keyword.value.trim()
        }
      }
    );

    users.value = response.data;

  } catch (error) {
    console.error("搜尋使用者失敗：", error);
  }
}

function selectUser(user) {

  // 使用者名稱放入搜尋框
  keyword.value = user.username;

  // 清除搜尋結果
  users.value = [];

  // 告訴 View 選到哪個使用者
  emit("select-user", user);
}

function cancelSearch() {

  keyword.value = "";
  users.value = [];

  emit("clear-user");

  userSearchInput.value?.focus();
}

</script>

<style scoped>

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-area {
  position: relative;
  flex: 1;
  min-width: 0;
}

.find-user {
  position: relative;
  width: 100%;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 40px 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 25px;
  background: #e0e1e1;
  outline: none;
  color: #374151;
  font-size: 20px;
  line-height: 1.5;
  transition: 0.2s;
}

.search-input:focus {
  border: solid 2px #ffc70e;
  background: #f2f3f3;
}

.search-input::placeholder {
  color: #9ca3af;
  opacity: 0.6;
}

.cancel-button {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 30px;
}

.publish-button {
  margin-top: 5px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 16px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 20px;
  font-weight: 600;
  background-color: #f4c665;
  color: #5B4520;
}

.user-list {
  display: flex;
  flex-direction: column;
  max-height: 250px;
  overflow-y: auto;
  margin-top: 5px;
}

.user-item {
  height: 50px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 22px;
}

.user-item:hover {
  background: #d6e8ff;
}

.user-item .mdi {
  color: #b3b2b2;
}

.user-item:hover .mdi {
  color: #000000;
}

</style>