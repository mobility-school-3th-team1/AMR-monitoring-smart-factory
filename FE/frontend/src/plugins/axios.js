import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/store'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' }
})

export function setupAxios() {
  api.interceptors.request.use((config) => {
    const authStore = useAuthStore()
    const token = authStore.accessToken || localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  api.interceptors.response.use(
    (res) => res,
    async (err) => {
      const authStore = useAuthStore()
      const status = err?.response?.status
      
      if (status === 401) {
        try {
          const refreshToken = authStore.refreshToken || localStorage.getItem('refreshToken')
          if (refreshToken) {
            const response = await axios.post('/api/v1/auth/refresh', { refreshToken })
            const newAccessToken = response.data.accessToken
            
            authStore.setTokens({ 
              accessToken: newAccessToken, 
              refreshToken: refreshToken 
            })
            
            err.config.headers.Authorization = `Bearer ${newAccessToken}`
            return axios(err.config)
          }
        } catch (refreshError) {
          // Refresh 토큰도 만료됨 → 로그아웃
          authStore.logout()
          router.push('/login')
          return Promise.reject(refreshError)
        }
      }
      
      return Promise.reject(err)
    }
  )
}

export default api
