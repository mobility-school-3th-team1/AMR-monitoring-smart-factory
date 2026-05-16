<template>
  <article class="stat-card" :class="toneClass">
    <p class="stat-label">{{ label }}</p>
    <p class="stat-value">{{ value }}</p>
    <p class="stat-description">{{ description }}</p>
  </article>
</template>

<script setup>
/* global defineProps */
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'blue'
  }
})

const toneClass = computed(() => `tone-${props.tone}`)
</script>

<style scoped>
.stat-card {
  padding: 20px;
  border-radius: var(--radius-md);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  border: 1px solid rgba(217, 228, 240, 0.95);
  box-shadow: var(--shadow-soft);
}

.stat-label {
  margin: 0 0 10px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.stat-value {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--color-text);
}

.stat-description {
  margin: 8px 0 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.tone-blue .stat-value {
  color: #1d4ed8;
}

.tone-green .stat-value {
  color: #047857;
}

.tone-orange .stat-value {
  color: #b45309;
}

.tone-red .stat-value {
  color: #b91c1c;
}

.tone-purple .stat-value {
  color: #7c3aed;
}
</style>