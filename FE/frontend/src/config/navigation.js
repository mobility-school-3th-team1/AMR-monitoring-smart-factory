export const workspaceNavigation = [
  {
    label: '관제 화면',
    description: '실시간 상태와 운영 현황을 확인합니다.',
    links: [
      { label: '메인 대시보드', to: '/dashboard' },
      { label: 'AMR 전체 관리', to: '/amr-list' },
      { label: 'AMR 개별 관제', to: '/amr-detail' }
    ]
  },
  {
    label: '운영 분석',
    description: '배터리와 작업 이력을 점검합니다.',
    links: [
      { label: '배터리/충전 스테이션', to: '/battery' },
      { label: '작업 이력 및 분석', to: '/work-history' }
    ]
  }
]