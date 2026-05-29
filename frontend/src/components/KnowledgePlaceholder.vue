<template>
  <div class="placeholder">
    <div class="placeholder-icon"><el-icon :size="32"><FolderOpened /></el-icon></div>
    <h2>知识库管理</h2>
    <p>功能开发中，敬请期待</p>
    <button @click="goBack">返回广场</button>
  </div>
</template>
<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const goBack = () => router.push('/market')
</script>
<style scoped>
.placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; }
.placeholder-icon { width: 64px; height: 64px; background: #EFF6FF; border-radius: 16px; display: flex; align-items: center; justify-content: center; color: #2563EB; margin-bottom: 20px; }
.placeholder h2 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 6px; }
.placeholder p { font-size: 0.875rem; color: #78716C; margin: 0 0 24px; }
.placeholder button { padding: 8px 20px; border: 1px solid #E7E5E4; background: #fff; border-radius: 8px; font-size: 0.8125rem; color: #44403C; cursor: pointer; }
.placeholder button:hover { background: #FAFAF9; border-color: #2563EB; color: #2563EB; }
</style>
