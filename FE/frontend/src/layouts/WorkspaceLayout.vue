<template>
  <div class="workspace-layout">
    <AppSidebar />

    <div class="workspace-layout__main">
      <AppTopBar />

      <main class="workspace-layout__content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import AppSidebar from '../components/molecules/AppSidebar.vue'
import AppTopBar from '../components/molecules/AppTopBar.vue'
</script>

<style scoped>
.workspace-layout {
  min-height: 100vh;
  display: flex;
}

.workspace-layout__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.workspace-layout__content {
  flex: 1;
  min-width: 0;
  padding: 24px;
}
</style>