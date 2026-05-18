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

// TODO(§6.1–6.2): WebSocket/MQTT 실시간 채널은 백엔드 브로커 연결 환경이 갖춰진 뒤 활성화 예정.
// initMqtt()를 여기서 호출하면 됨. 연결 전까지는 REST 폴링(10 s)으로 대체한다.

app.use(router).mount('#app')
