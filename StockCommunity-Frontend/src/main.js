// 建立 app
import { createApp } from 'vue'
import App from './App.vue'

// Pinia
import { createPinia } from 'pinia'

// Router
import router from './router'

// Vuetify
import vuetify from './plugins/vuetify.js'


const app = createApp(App)

// 建立 Pinia
app.use(createPinia())

// 建立 Router
app.use(router)

// Vuetify
app.use(vuetify)

// 掛載
app.mount('#app')
