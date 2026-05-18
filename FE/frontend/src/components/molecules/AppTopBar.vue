<template>
  <header class="app-topbar">
    <div>
      <h2 class="app-topbar__title">{{ pageTitle }}</h2>
    </div>

    <div class="app-topbar__meta">
      <div class="app-topbar__time">{{ currentTime }}</div>
      
      <div class="app-topbar__user-menu">
        <div class="user-info">
          <div class="user-avatar">{{ userInitial }}</div>
          <div class="user-details">
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">{{ userRole }}</div>
          </div>
        </div>
        
        <button class="btn-logout" @click="handleLogout" title="로그아웃">
          <span>로그아웃</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentTime = ref('')

const pageTitle = computed(() => route.meta.title ?? 'AMR Monitoring')

const userInitial = computed(() => {
  const name = authStore.user?.displayName || authStore.user?.username || ''
  return name.charAt(0).toUpperCase()
})

const userName = computed(() => authStore.user?.displayName || authStore.user?.username || '게스트')
const userRole = computed(() => {
  const role = authStore.user?.role || 'user'
  const roleMap = { admin: '관리자', operator: '운영자', viewer: '뷰어', user: '사용자' }
  return roleMap[role] || role
})

const updateTime = () => {
  currentTime.value = new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date())
}

const handleLogout = async () => {
  authStore.logout()
  await router.push('/login')
}

onMounted(() => {
  updateTime()
  const timer = setInterval(updateTime, 1000)
  
  onUnmounted(() => {
    clearInterval(timer)
  })
})
</script>

<style scoped>
.app-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid #e6eef8;
  background: #ffffff;
}

.app-topbar__title {
  margin: 0;
  font-size: 0.85rem;
  letter-spacing: -0.02em;
  color: #1f2937;
}

.app-topbar__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.app-topbar__time {
  padding: 4px 8px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #eef3f9;
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748b;
  font-family: 'Monaco', 'Courier New', monospace;
}

.app-topbar__user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 0.7rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0px;
  min-width: 0;
}

.user-name {
  font-size: 0.72rem;
  font-weight: 700;
  color: #1f2937;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.user-role {
  font-size: 0.64rem;
  color: #94a3b8;
}

.btn-logout {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid #eef3f9;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.14s ease;
}

.btn-logout:hover {
  border-color: #d7e3f7;
  background: #f8fafc;
  color: #475569;
}

.btn-logout:active {
  transform: scale(0.995);
}

@media (max-width: 800px) {
  .app-topbar__description { display: none; }
  .app-topbar__eyebrow { display: none; }
}
</style>