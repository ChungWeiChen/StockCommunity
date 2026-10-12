import { createRouter, createWebHistory } from 'vue-router'
import TestView from '@/views/TestView.vue'
import TestView2 from '@/views/TestView2.vue'
import { useAuthStore } from '@/stores/authStore'

// 範例：Wei 原本留的展示頁 src/views/TestView.vue / TestView2.vue 保留著沒刪，
// 沒有掛在下面的路由表裡（避免跟真正的頁面混在一起），但可以直接打開來看基本寫法。
// 新增一個路由大致長這樣：
// { path: "/example", name: "example", component: () => import("@/views/TestView.vue") }
const routes = [
  // createWebHistory：用瀏覽器原生網址列（沒有 # 符號），需要伺服器配合處理重新整理時的路徑
  {
    path: '/',
    name: 'home',
    // 原本：component: HomeView, meta: { requiresAuth: true }（會顯示一個空白的「歡迎回來」頁面）
    // 改成直接導去社群首頁，不管有沒有登入，一進站就有實際內容可以看，登入完沒帶redirect參數時也是導來這裡
    redirect: '/communityview',
  },
  // 主要功能路徑
  {
    path: '/us',
    name: 'user',
    // 功能分支
    children: [
      {
        path: '/about',
        name: 'about',
        meta: { title: '關於', noComputedScan: 'true' },
        component: () => import('@/views/AboutView.vue'),
      },
      {
        path: '/login',
        name: 'login',
        meta: { title: '登入', noComputedScan: 'true' },
        component: () => import('@/views/LoginView.vue'),
        // 沒設 requiresAuth，代表沒登入也能進來
      },
      {
        path: '/register',
        name: 'register',
        meta: { title: '註冊', noComputedScan: 'true' },
        component: () => import('@/views/RegisterView.vue'),
      },
      {
        path: '/forgot-password',
        name: 'forgot-password',
        meta: { title: '忘記密碼', noComputedScan: 'true' },
        component: () => import('@/views/ForgotPasswordView.vue'),
        // 沒設 requiresAuth，代表沒登入也能進來
      },
      {
        path: '/reset-password',
        name: 'reset-password',
        meta: { title: '重設密碼', noComputedScan: 'true' },
        component: () => import('@/views/ResetPasswordView.vue'),
        // 沒設 requiresAuth，代表沒登入也能進來
      },
    ],
  },
  {
    path: '/community',
    name: 'community',
    redirect: '/communityview',
    children: [
      {
        path: '/communityview',
        component: () => import('@/views/Community/CommunityView.vue'),
        // sidebar icon 設定
        meta: { title: '首頁', icon: 'mdi-account-group-outline' },
      },
      {
         path: "/hotpost",
         component: () => import('@/views/Community/HotPostsView.vue'),
         //sidebar icon 設定
         meta: { title:'熱門貼文', icon:'mdi-fire'},
      },
       {
          path: "/findpost",
          component: () => import('@/views/Community/FindUserPostView.vue'),
          meta: { title:'投資人動態', icon:'mdi-account-search-outline'},
       },
       {
          path: "/findstockpost",
          component: () => import('@/views/Community/FindStockPostsView.vue'),
          meta: {title: '個股動態', icon:'mdi-tag-search-outline'},
        },
        {
          path: "/mypost",
          component: () => import('@/views/Community/MyPostsView.vue'),
          meta: { title:'我的貼文', icon:'mdi-comment-account-outline'},
       },
      ],
  },
  {
    path: '/st',
    name: 'st',
    redirect: '/stock',
    children: [
      {
        path: '/watchlist',
        name: 'watchlist',
        component: () => import('@/views/WatchlistView.vue'),
        meta: { title: '關注清單', icon: 'mdi mdi-heart-outline' }, // 原本 requiresAuth: true，改成頁面可先瀏覽，登入判斷交給元件內部（點實際動作才擋）
      },
      // {
      //   path: '/old-order',
      //   name: 'old-order',
      //   component: () => import('@/views/OrderView.vue'),
      //   meta: { requiresAuth: true, title: '下單', icon: 'mdi mdi-swap-horizontal' },
      // },
      {
        path: '/order',
        name: 'order',
        component: () => import('@/views/OrderPad.vue'),
        meta: { title: '五檔下單', icon: 'mdi mdi-swap-horizontal' }, // 原本 requiresAuth: true，改成頁面可先瀏覽，送出委託按鈕才要求登入
      },
      {
        path: '/historyorder',
        name: 'historyorder',
        component: () => import('@/views/HistoryOrderView.vue'),
        meta: { title: '訂單紀錄', icon: 'mdi mdi-comment-account-outline' }, // 原本 requiresAuth: true，改成頁面可先瀏覽，未登入時顯示提示卡片
      },
      {
        path: '/holding',
        name: 'holding',
        component: () => import('@/views/HoldingView.vue'),
        meta: { title: '我的持股', icon: 'mdi mdi-wallet-outline' }, // 原本 requiresAuth: true，改成頁面可先瀏覽，未登入時顯示提示卡片
      },
      {
        path: '/stock',
        name: 'stock',
        component: () => import('@/views/StockView.vue'),
        meta: { title: '股票行情', icon: 'mdi mdi-chart-line' },
        // 沒設 requiresAuth：股票清單、歷史行情屬於公開資料
      },
      {
        path: '/profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { requiresAuth: true, title: '個人資料', icon: 'mdi mdi-comment-account-outline', noComputedScan: 'true' },
        // 加了 noComputedScan：只讓右上角頭像選單連過來，不進左側 sidebar 自動掃描的列表
      },
      {
        path: '/trade',
        name: 'trade',
        component: () => import('@/views/TradeView2.vue'),
        meta: { title: '委託紀錄', icon: 'mdi-history' }, // 原本 requiresAuth: true，改成頁面可先瀏覽，未登入時顯示提示卡片
      },
    ],
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
})

router.beforeEach((to) => {
  // 必須在函式「內部」呼叫 useAuthStore()，不能寫在檔案最外層
  // 因為這個檔案在 Pinia 初始化之前就會被載入，外層呼叫會抓不到 store
  const authStore = useAuthStore()

  // 這個路由要求登入，但目前沒有登入狀態 → 導去 /login
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    // 跟社群那邊(CreatePost/OnePost/OneComment)手動擋的alert訊息統一，
    // 讓「未登入點需要登入的功能」全站體驗一致
    alert("您還沒有登入會員，請先登入會員。")
    // 把使用者原本想去的完整路徑存成 query 參數，登入成功後才能導回這裡
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 沒有 return 任何東西 = 放行，正常進入目標路由
})

export default router
