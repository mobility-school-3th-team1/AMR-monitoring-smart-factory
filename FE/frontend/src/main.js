import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createStore } from './store'
import { useAuthStore } from './store'
import { setupAxios } from './plugins/axios'
import { initMqtt } from './plugins/ws'

import './styles/base.css'
import './styles/tokens.css'

const app = createApp(App)
const pinia = createStore(app)
const auth = useAuthStore(pinia)

setupAxios(auth)
if (auth.accessToken) initMqtt({ brokerUrl: 'mqtt://localhost:8080' })

app.use(router).mount('#app')
