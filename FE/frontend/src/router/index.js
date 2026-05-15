import { createRouter, createWebHistory } from 'vue-router'

import AuthLayout from '../layouts/AuthLayout.vue'
import WorkspaceLayout from '../layouts/WorkspaceLayout.vue'
import AmrDetailView from '../views/AmrDetailView.vue'
import AmrListView from '../views/AmrListView.vue'
import BatteryView from '../views/BatteryView.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import WorkHistoryView from '../views/WorkHistoryView.vue'

const routes = [
  {
    path: '/',
    component: WorkspaceLayout,
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
          description: '공장 전체 평면도 기반의 통합 관제 화면입니다.'
        }
      },
      {
        path: 'amr-list',
        component: AmrListView,
        meta: {
          title: 'AMR 전체 관리',
          description: '전체 장비 상태와 오류 통계를 확인합니다.'
        }
      },
      {
        path: 'amr-detail',
        component: AmrDetailView,
        meta: {
          title: 'AMR 개별 관제',
          description: '선택한 장비의 상세 상태와 비상 제어를 다룹니다.'
        }
      },
      {
        path: 'battery',
        component: BatteryView,
        meta: {
          title: '배터리/충전 스테이션 현황',
          description: '충전 대기열과 스테이션 점유 상태를 확인합니다.'
        }
      },
      {
        path: 'work-history',
        component: WorkHistoryView,
        meta: {
          title: '작업 이력 및 분석',
          description: '작업 내역과 운영 지표를 점검합니다.'
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
  const pageTitle = to.meta.title ?? 'AMR Monitoring'
  document.title = `AMR Monitoring | ${pageTitle}`
})

export default router