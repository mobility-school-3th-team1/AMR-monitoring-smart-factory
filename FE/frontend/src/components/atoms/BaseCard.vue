<template>
  <section class="base-card">
    <slot />
  </section>
</template>

<style scoped>
.base-card {
  background: var(--color-surface);
  border: 1px solid rgba(217, 228, 240, 0.92);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
}
</style>