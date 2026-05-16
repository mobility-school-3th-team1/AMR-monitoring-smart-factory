<template>
  <div class="auth-layout">
    <div class="auth-layout__card">
      <p class="auth-layout__eyebrow">Fleet Management System</p>
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
}

.auth-layout__card {
  width: min(100%, 460px);
  padding: 34px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(217, 228, 240, 0.95);
  box-shadow: var(--shadow-elevated);
}

.auth-layout__eyebrow {
  margin: 0 0 18px;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-primary);
}
</style>