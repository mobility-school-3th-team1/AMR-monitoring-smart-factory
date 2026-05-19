import { createApp } from 'vue'
import { watch } from 'vue'
import App from './App.vue'
import router from './router'
import { createStore, useAuthStore } from './store'
import { setupAxios } from './plugins/axios'
import { initMqtt } from './plugins/ws'

import './styles/base.css'
import './styles/tokens.css'

const app = createApp(App)
const pinia = createStore(app)
const authStore = useAuthStore(pinia)

// Pinia 설정 후에 axios interceptor 설정
setupAxios()

const connectMqttIfAuthenticated = () => {
	if (!authStore.isAuthenticated) return

	initMqtt({
		brokerUrl: import.meta.env.VITE_MQTT_URL || 'ws://localhost:9001'
	})
}

connectMqttIfAuthenticated()

watch(
	() => authStore.isAuthenticated,
	(isAuthenticated) => {
		if (isAuthenticated) {
			connectMqttIfAuthenticated()
		}
	}
)

app.use(router).mount('#app')
