<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__brand">
      <p class="app-sidebar__brand-kicker">AMR Control Center</p>
      <h1 class="app-sidebar__brand-title">Smart Factory</h1>
    </div>

    <nav class="app-sidebar__nav" aria-label="주요 메뉴">
      <section
        v-for="group in workspaceNavigation"
        :key="group.label"
        class="app-sidebar__group"
      >
        <p class="app-sidebar__group-label">{{ group.label }}</p>
        <p class="app-sidebar__group-description">{{ group.description }}</p>
        <RouterLink
          v-for="link in group.links"
          :key="link.to"
          :to="link.to"
          class="app-sidebar__link"
          active-class="is-active"
        >
          {{ link.label }}
        </RouterLink>
      </section>
    </nav>

  </aside>
</template>

<script setup>
import { workspaceNavigation } from '../../config/navigation'
</script>

<style scoped>
.app-sidebar {
  width: var(--layout-sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 28px 22px;
  color: #dbe7f7;
  background:
    linear-gradient(180deg, rgba(7, 16, 37, 0.98) 0%, rgba(14, 27, 52, 0.98) 100%),
    #0f172a;
  border-right: 1px solid rgba(148, 163, 184, 0.16);
}

.app-sidebar__brand-kicker {
  margin: 0 0 8px;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #60a5fa;
}

.app-sidebar__brand-title {
  margin: 0;
  font-size: 1.35rem;
  line-height: 1.2;
  letter-spacing: -0.04em;
  color: #ffffff;
}

.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.app-sidebar__group-label {
  margin: 0;
  font-size: 0.82rem;
  font-weight: 800;
  color: #f8fafc;
}

.app-sidebar__group-description {
  margin: 6px 0 10px;
  font-size: 0.75rem;
  line-height: 1.5;
  color: #94a3b8;
}

.app-sidebar__link {
  display: block;
  padding: 11px 13px;
  margin-bottom: 8px;
  border-radius: 12px;
  color: #dbe7f7;
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid transparent;
  transition: 0.18s ease;
}

.app-sidebar__link:hover,
.app-sidebar__link.is-active {
  color: #ffffff;
  border-color: rgba(96, 165, 250, 0.35);
  background: rgba(37, 99, 235, 0.22);
  transform: translateX(2px);
}

</style>