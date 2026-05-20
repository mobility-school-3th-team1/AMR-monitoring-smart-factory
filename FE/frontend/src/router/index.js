import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store'

import AuthLayout from '../layouts/AuthLayout.vue'
import WorkspaceLayout from '../layouts/WorkspaceLayout.vue'
import AmrListView from '../views/AmrListView.vue'
import BatteryView from '../views/BatteryView.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import WorkHistoryView from '../views/WorkHistoryView.vue'
import ChargingStationsView from '../views/ChargingStationsView.vue'

const routes = [
  {
    path: '/',
    component: WorkspaceLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        component: DashboardView,
        meta: {
          title: '메인 대시보드',
          description: '공장 전체 평면도 기반의 통합 관제 화면입니다.',
          requiresAuth: true
        }
      },
      {
        path: 'amr-list',
        component: AmrListView,
        meta: {
          title: 'AMR 전체 관리',
          description: '전체 장비 상태와 오류 통계를 확인합니다.',
          requiresAuth: true
        }
      },
      {
        path: 'amr-detail',
        redirect: '/amr-list'
      },
      {
        path: 'battery',
        component: BatteryView,
        meta: {
          title: '배터리/충전 스테이션 현황',
          description: '충전 대기열과 스테이션 점유 상태를 확인합니다.',
          requiresAuth: true
        }
      },
      {
        path: 'charging',
        component: ChargingStationsView,
        meta: {
          title: '충전 스테이션',
          description: '충전 스테이션별 점유 현황과 AMR 충전 큐를 확인합니다.',
          requiresAuth: true
        }
      },
      {
        path: 'work-history',
        component: WorkHistoryView,
        meta: {
          title: '작업 이력 및 분석',
          description: '작업 내역과 운영 지표를 점검합니다.',
          requiresAuth: true
        }
      }
    ]
  },
  {
    path: '/login',
    component: AuthLayout,
    children: [
      {
        path: '',
        component: LoginView,
        meta: {
          title: '로그인',
          description: 'Fleet Management System 로그인 화면입니다.'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const pageTitle = to.meta.title ?? 'AMR Monitoring'
  document.title = `AMR Monitoring | ${pageTitle}`

  // 인증이 필요한 페이지인지 확인 (default: false)
  const requiresAuth = to.meta.requiresAuth ?? false
  const isAuthenticated = authStore.isAuthenticated

  // 로그인 페이지에서 인증된 사용자가 접근하려면 대시보드로 리다이렉트
  if (to.path === '/login' && isAuthenticated) {
    return { path: '/dashboard' }
  }

  // 보호된 페이지에 미인증 사용자가 접근하려면 로그인 페이지로 리다이렉트
  if (requiresAuth && !isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router