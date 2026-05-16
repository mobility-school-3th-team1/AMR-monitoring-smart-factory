<template>
  <span class="base-badge" :class="toneClass">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
/* global defineProps */
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'info'
  }
})

const toneClass = computed(() => `tone-${props.tone}`)
</script>

<style scoped>
.base-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1;
  border: 1px solid transparent;
}

.tone-info {
  color: #0f766e;
  background: rgba(6, 182, 212, 0.12);
  border-color: rgba(6, 182, 212, 0.2);
}

.tone-success {
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.2);
}

.tone-warning {
  color: #92400e;
  background: rgba(245, 158, 11, 0.16);
  border-color: rgba(245, 158, 11, 0.26);
}

.tone-danger {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.22);
}
</style>