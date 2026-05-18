import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createStore } from './store'
import { setupAxios } from './plugins/axios'

import './styles/base.css'
import './styles/tokens.css'

const app = createApp(App)
const pinia = createStore(app)

// Pinia 설정 후에 axios interceptor 설정
setupAxios()

app.use(router).mount('#app')
