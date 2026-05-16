<template>
  <BaseCard class="section-panel">
    <div class="section-panel__header">
      <div>
        <p class="section-panel__eyebrow" v-if="eyebrow">{{ eyebrow }}</p>
        <h2 class="section-panel__title">{{ title }}</h2>
        <p class="section-panel__subtitle" v-if="subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.action" class="section-panel__action">
        <slot name="action" />
      </div>
    </div>
    <div class="section-panel__body">
      <slot />
    </div>
  </BaseCard>
</template>

<script setup>
/* global defineProps */
import BaseCard from '../atoms/BaseCard.vue'

defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.section-panel {
  padding: 20px;
}

.section-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-primary);
}

.section-panel__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.section-panel__subtitle {
  margin: 6px 0 0;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.section-panel__body {
  min-width: 0;
}
</style>