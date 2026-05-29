<template>
  <div class="app-layout">
    <Sidebar />
    <div class="main-area">
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from '../components/Sidebar.vue'
</script>

<style>
.app-layout { height: 100vh; display: flex; background: #FAFAF9; }
.main-area { flex: 1; display: flex; background: #FAFAF9; overflow: hidden; }
</style>
