<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store'
import api from '@/plugins/axios'

const router = useRouter()
const authStore = useAuthStore()

const formData = ref({
  username: '',
  password: ''
})

const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = ''

  // 입력값 검증
  if (!formData.value.username.trim()) {
    errorMessage.value = '사용자명을 입력해주세요.'
    return
  }
  if (!formData.value.password) {
    errorMessage.value = '비밀번호를 입력해주세요.'
    return
  }

  // 개발 편의용 인증 우회 — VITE_AUTH_BYPASS=true 환경변수가 명시적으로 설정된 경우에만 동작.
  // import.meta.env.DEV 만으로 체크하면 vite preview 빌드나 실수로 배포된 DEV 빌드에서도
  // 백엔드 없이 로그인이 성공할 수 있으므로, 별도 플래그를 요구한다.
  if (import.meta.env.VITE_AUTH_BYPASS === 'true') {
    console.warn('[AUTH BYPASS] 인증 우회 모드가 활성화되어 있습니다. 실제 /auth/login 흐름이 실행되지 않습니다.')
    isLoading.value = true
    const fakeUser = { id: 0, name: formData.value.username || 'dev-admin', role: 'admin' }
    authStore.setTokens({ accessToken: 'dev-access-token', refreshToken: 'dev-refresh-token' })
    authStore.setUser(fakeUser)
    router.push({ path: '/dashboard' })
    isLoading.value = false
    return
  }

  isLoading.value = true
  try {
    const response = await api.post('/auth/login', {
      username: formData.value.username,
      password: formData.value.password
    })

    const { accessToken, refreshToken, user } = response.data
    
    // 토큰 및 사용자 정보 저장 (setUser 액션 사용 — localStorage 동기화 포함)
    authStore.setTokens({ accessToken, refreshToken })
    authStore.setUser(user)
    
    // 대시보드로 이동
    router.push({ path: '/dashboard' })
  } catch (error) {
    if (error.response?.status === 401) {
      errorMessage.value = '아이디 또는 비밀번호가 일치하지 않습니다.'
    } else if (error.response?.status === 400) {
      errorMessage.value = error.response.data?.message || '입력값이 올바르지 않습니다.'
    } else if (error.message === 'Network Error') {
      errorMessage.value = '네트워크 연결을 확인해주세요.'
    } else {
      errorMessage.value = '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
    }
  } finally {
    isLoading.value = false
  }
}

const handleKeydown = (event) => {
  if (event.key === 'Enter') {
    handleLogin()
  }
}
</script>

<template>
  <form class="login-form" @submit.prevent="handleLogin">
    <div class="login-header">
      <div class="logo">A</div>
      <h1>AMR 모니터링</h1>
      <p>스마트 팩토리 통합 관제 시스템</p>
    </div>

    <div v-if="errorMessage" class="error-message">
      <span class="error-icon">⚠</span>
      {{ errorMessage }}
    </div>

    <div class="form-group">
      <label for="username" class="form-label">사용자명</label>
      <input
        id="username"
        v-model="formData.username"
        type="text"
        class="form-input"
        placeholder="admin"
        autocomplete="username"
        :disabled="isLoading"
        @keydown="handleKeydown"
      />
    </div>

    <div class="form-group">
      <label for="password" class="form-label">비밀번호</label>
      <input
        id="password"
        v-model="formData.password"
        type="password"
        class="form-input"
        placeholder="••••••••"
        autocomplete="current-password"
        :disabled="isLoading"
        @keydown="handleKeydown"
      />
    </div>

    <button
      type="submit"
      class="btn-login"
      :disabled="isLoading"
    >
      {{ isLoading ? '로그인 중...' : '로그인' }}
    </button>

    <p class="form-footer">
      테스트 계정: admin / demo123
    </p>
  </form>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.login-header {
  text-align: center;
  margin-bottom: 12px;
}

.logo {
  width: 60px;
  height: 60px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 900;
  font-size: 24px;
}

.login-header h1 {
  font-size: 1.4rem;
  font-weight: 800;
  color: #1f2937;
  letter-spacing: -0.02em;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.85rem;
}

.error-icon {
  flex-shrink: 0;
  font-size: 1.1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #1f2937;
  display: block;
}

.form-input {
  padding: 11px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.85rem;
  font-family: 'Malgun Gothic', 'Pretendard', sans-serif;
  color: #1f2937;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input:disabled {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
}

.form-input::placeholder {
  color: #cbd5e1;
}

.btn-login {
  padding: 11px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.btn-login:hover:not(:disabled) {
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-login:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
}

.btn-login:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.form-footer {
  font-size: 0.75rem;
  color: #64748b;
  text-align: center;
  margin: 0;
}
</style>