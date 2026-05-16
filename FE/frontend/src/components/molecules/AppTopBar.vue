<template>
  <header class="app-topbar">
    <div>
      <p class="app-topbar__eyebrow">Project Base and Common Infrastructure</p>
      <h2 class="app-topbar__title">{{ pageTitle }}</h2>
      <p class="app-topbar__description">{{ pageDescription }}</p>
    </div>

    <div class="app-topbar__meta">
      <BaseBadge tone="success" label="Live Preview" />
      <span class="app-topbar__clock">{{ currentTime }}</span>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import BaseBadge from '../atoms/BaseBadge.vue'

const route = useRoute()

const pageTitle = computed(() => route.meta.title ?? 'AMR Monitoring')
const pageDescription = computed(
  () => route.meta.description ?? '공통 레이아웃과 화면 골격을 정리한 초기 프론트엔드 구조입니다.'
)
const currentTime = new Intl.DateTimeFormat('ko-KR', {
  hour: '2-digit',
  minute: '2-digit'
}).format(new Date())
</script>

<style scoped>
.app-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 28px;
  border-bottom: 1px solid rgba(217, 228, 240, 0.92);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px);
}

.app-topbar__eyebrow {
  margin: 0 0 7px;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-primary);
}

.app-topbar__title {
  margin: 0;
  font-size: 1.28rem;
  letter-spacing: -0.04em;
}

.app-topbar__description {
  margin: 8px 0 0;
  font-size: 0.86rem;
  color: var(--color-text-muted);
}

.app-topbar__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.app-topbar__clock {
  padding: 9px 12px;
  border-radius: 999px;
  background: var(--color-surface-soft);
  border: 1px solid rgba(217, 228, 240, 0.92);
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-muted);
}
</style>