import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' }
})

export function setupAxios(store) {
  api.interceptors.request.use((config) => {
    const token = store.auth?.accessToken || localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  })

  api.interceptors.response.use(
    (res) => res,
    async (err) => {
      const status = err?.response?.status
      if (status === 401) {
        try {
          const refreshToken = store.auth?.refreshToken || localStorage.getItem('refreshToken')
          if (refreshToken) {
            const r = await axios.post('/api/v1/auth/refresh', { refreshToken })
            const accessToken = r.data.accessToken
            store.auth.setTokens({ accessToken, refreshToken })
            localStorage.setItem('accessToken', accessToken)
            err.config.headers.Authorization = `Bearer ${accessToken}`
            return axios(err.config)
          }
        } catch (e) {
          // fallback to login
          store.auth.logout()
          router.push({ name: 'Login' })
        }
      }
      return Promise.reject(err)
    }
  )
}

export default api
