<template>
  <ErrorView v-if="serverError" />

  <v-layout v-else>
    <v-app-bar class="px-md-4 top-navbar" height="80" flat>
      <template #prepend v-if="$vuetify.display.smAndDown">
        <v-app-bar-nav-icon v-if="$vuetify.display.smAndDown" @click="drawer = !drawer" />
        <v-btn v-for="(item, i) in navbarItems" :key="i" :to="item.to" class="me-2 text-none" slim>
          <!-- 要新增上方BarList到程式最後的參數欄 -->
          {{ item.text }}
        </v-btn>
      </template>

      <v-toolbar-title>
        <router-link to="/community">
          <img src="/pictures/GoodSign icon.png" class="navbar-logo" />
        </router-link>
      </v-toolbar-title>

      <!-- 桌面版布局 上方的 navbar -->
      <template v-if="$vuetify.display.mdAndUp">
        <!-- 上方BarList -->
        <v-btn
          v-for="(item, i) in navbarItems"
          :key="i"
          :to="item.to"
          class="me-2 navbar-item"
          slim
        >
          <!-- 要新增上方BarList到程式最後的參數欄 -->
          {{ item.text }}
        </v-btn>
      </template>

      <v-spacer />

      <template #append>
        <!-- 已登入：顯示帳號縮寫頭像 + 選單（更新資料/登出） -->
        <!-- 帳戶餘額：只有登入時顯示，放在原本的空白處(v-spacer右邊、頭像左邊) -->
        <span v-if="isLoggedIn" class="navbar-balance">NT$ {{ formatNumber(availableCash) }}</span>
        <template v-if="isLoggedIn">
          <v-btn class="ms-1" icon>
            <v-avatar color="#F3EAD8">
              <v-img v-if="avatarPhotoUrl" :src="avatarPhotoUrl" alt="大頭照" cover />
              <span v-else class="text-body-2 avatar-initial">{{ userInitial }}</span>
            </v-avatar>

            <v-menu activator="parent" origin="top">
              <v-list>
                <v-list-item link title="個人資料" to="/profile" />
                <v-list-item link title="登出" @click="handleLogout" />
              </v-list>
            </v-menu>
          </v-btn>
        </template>

        <!-- 未登入：顯示登入/註冊 -->
        <template v-else>
          <v-btn to="/login" class="me-2 text-none" variant="text">登入</v-btn>
          <v-btn to="/register" class="text-none" variant="tonal">註冊</v-btn>
        </template>
      </template>
    </v-app-bar>

    <!-- 桌面版布局 左方的 Sidebar -->
    <v-navigation-drawer v-if="$vuetify.display.mdAndUp" permanent width="220" class="sidebar">
      <v-list nav>
        <v-list-item
          v-for="(item, i) in sidebarItemsVisible"
          :key="item.path"
          :to="item.path"
          :title="item.meta?.title"
          :prepend-icon="item.meta?.icon"
          class="sidebar-item"
        />
      </v-list>

      <img src="/pictures/GoodSign logo.png" class="sidebar-bottom-image" />

      <img src="/pictures/GoodSign logo.png" class="sidebar-bottom-image" />
    </v-navigation-drawer>

    <!-- 手機版布局 左方的 Sidebar -->
    <v-navigation-drawer v-if="$vuetify.display.smAndDown" v-model="drawer" temporary width="280">
      <v-list nav>
        <!-- 原本手機版用舊的 navItems，已無對應宣告會報錯，改用跟桌面版一致的 sidebarItemsVisible -->
        <!-- <v-list-item
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :title="item.title"
          :prepend-icon="item.icon"
          @click="drawer = false"
        /> -->
        <v-list-item
          v-for="item in sidebarItemsVisible"
          :key="item.path"
          :to="item.path"
          :title="item.meta?.title"
          :prepend-icon="item.meta?.icon"
          @click="drawer = false"
        />

        <!-- 社群平台：之後接功能的佔位按鈕 -->
        <!-- <v-list-item
          prepend-icon="mdi-comment-account-outline"
          title="社群平台"
        /> -->
      </v-list>
    </v-navigation-drawer>

    <!-- 之後放view的地方 -->
    <v-main>
      <!-- <v-toolbar color="background" elevation="1" height="84">
        <template #title>
          <h2 class="text-headline-large font-weight-bold">
            {{ currentPageTitle }}
          </h2>
        </template>
      </v-toolbar> -->

      <!-- router 跳轉的頁面 -->
      <div class="pa-4">
        <router-view />
      </div>
    </v-main>
  </v-layout>
</template>

<script setup>
import { shallowRef, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import { useUserStore } from '@/stores/userStore'
import { serverError } from '@/serverStatus.js'
import ErrorView from '@/views/404View.vue'
import { useTradingAccountStore } from '@/stores/tradingAccountStore'

const drawer = shallowRef(false)
const route = useRoute()
const router = useRouter()

const authStore = useAuthStore()
const { isLoggedIn, user } = storeToRefs(authStore)

// 跟 ProfileView 用同一個 userStore 實例、同一份 profile 資料，
// ProfileView 存大頭照成功後 profile.value 一變，這裡會自動跟著重新渲染
const userStore = useUserStore()
const { profile } = storeToRefs(userStore)
// 帳戶餘額放在共用版面而不是ProfileView.vue裡，這樣不管在哪一頁都看得到；
// 下單後會自動更新，是因為OrderOption.vue下單成功時也會呼叫同一個store的fetchTradingAccount()，
// Pinia store全站共用同一份狀態，兩邊資料自動同步
const tradingAccountStore = useTradingAccountStore()
const { availableCash } = storeToRefs(tradingAccountStore)

// 千分位金額格式化，跟ProfileView.vue同一套寫法，維持全站數字風格一致
const formatNumber = (value) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return '—'
  }
  return new Intl.NumberFormat('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value))
}

// DefaultLayout 整個 App 只掛載一次（在登入之前就掛載了），
// 不能只靠 onMounted 抓一次資料；用 watch 盯著 isLoggedIn，
// 不管是「一開始就已登入」還是「這次剛登入」都會觸發去抓 profile
watch(
  isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      userStore.fetchProfile()
      tradingAccountStore.fetchTradingAccount()
      tradingAccountStore.checkSeasonReset()
    }
  },
  { immediate: true },
)

// 沒大頭照時是 null，v-avatar 那邊用 v-if 判斷要顯示照片還是縮寫文字
const avatarPhotoUrl = computed(() =>
  profile.value?.userPhoto ? `data:image/jpeg;base64,${profile.value.userPhoto}` : null,
)

const userInitial = computed(() => user.value?.username?.charAt(0)?.toUpperCase() ?? '?')

// 登出後把 token/user 清掉，並導回登入頁，避免卡在「看起來已登入但 API 全部失敗」的狀態
const handleLogout = () => {
  authStore.logout()
  // 原本：router.push('/login')，但這次的改動整體方向是「不強迫一進站就登入」，
  // 登出後還丟登入表單等於自打嘴巴，改成跟未登入訪客一樣落地在社群首頁，可以繼續逛
  router.push('/communityview')
}

// 側邊欄選單：requiresAuth 為 true 的項目，只有登入後才會出現
// 之後有新頁面要加進導覽列，就在這裡加一筆
// const allNavItems = [
//   { title: '總覽', path: '/', icon: 'mdi-view-dashboard-outline', requiresAuth: true },
//   { title: '股票行情', path: '/stock', icon: 'mdi-chart-line' },
//   { title: '計畫表', path: '/watchlist', icon: 'mdi-star-outline', requiresAuth: true },
//   { title: '下單', path: '/order', icon: 'mdi-swap-horizontal', requiresAuth: true },
//   { title: '我的持股', path: '/holding', icon: 'mdi-wallet-outline', requiresAuth: true },
// ]

// const navItems = computed(() =>
//   allNavItems.filter((item) => !item.requiresAuth || isLoggedIn.value)
// )

// const currentPageTitle = computed(() => {
//   const match = allNavItems.find((item) => item.path === route.path)
//   return match?.title ?? 'Dashboard'
// })

// 上方主要功能narbar , to:配合router 的路徑
const navbarItems = [
  { text: '股市', to: '/st' },
  { text: '社群', to: '/community' },
]

//計算 父router下有哪些children path 當左方 sidebar 導覽列
const sidebarItems = computed(() => {
  const parentRoute = route.matched.find((item) => item.children?.length > 0)
  //若在 router 的 meta 內 加入 noComputedScan:"true" 屬性 則不進入過濾當中(登入/登出的常駐功能有加,不然會被show在左方sidebar)
  return (parentRoute?.children ?? []).filter((item) => !item.meta?.noComputedScan)
})
// 左方 sidebar 不管有沒有登入都全部顯示，需要登入才能進的項目交給 router.beforeEach 去擋、導回登入頁
const sidebarItemsVisible = computed(() => sidebarItems.value)
</script>

<style scoped>
.top-navbar {
  background: linear-gradient(105deg, #f8e3a1 0%, #f4cf72 42%, #edb15a 78%, #e79b45 100%);
}

.navbar-logo {
  height: 80px;
  width: auto;
  margin-top: 15px;
  object-fit: contain;
}

.navbar-button {
  margin-top: 20px;
}

.navbar-button:hover {
  background: rgba(255, 255, 255, 0.12);
}

.navbar-item {
  padding: 20px 25px;
  font-size: 22px;
  border-radius: 8px;
  text-transform: none;
}

.sidebar {
  background: #fbfaf6;
  border-right: 1px solid #e7e1d5;
  position: relative;
}

.sidebar-item :deep(.v-list-item-title) {
  font-size: 16px;
}

.sidebar :deep(.v-list-item--active) {
  background: #f9f1d8;
  color: #6f5833;
}

.sidebar :deep(.v-list-item--active .v-icon) {
  color: #e1b25a;
}

.sidebar-bottom-image {
  position: absolute;
  left: 30px;
  bottom: 10px;
  width: 150px;
  height: auto;
  object-fit: contain;
}

.avatar-initial {
  color: #7c6848;
  font-weight: 600;
}

.navbar-balance {
  font-size: 18px;
  font-weight: 700;
  color: #3f3b35;
  margin-right: 12px;
}
</style>
