<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>使用统计</h1>
        <p>系统使用数据统计（后续扩展）</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <span class="stat-value">--</span>
            <span class="stat-label">总对话次数</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <span class="stat-value">--</span>
            <span class="stat-label">活跃用户</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <span class="stat-value">--</span>
            <span class="stat-label">智能体数量</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <span class="stat-value">--</span>
            <span class="stat-label">今日请求</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>数据概览</span>
      </template>
      <el-empty description="统计功能开发中，敬请期待" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
.stat-item { display: flex; flex-direction: column; align-items: center; padding: 12px; }
.stat-value { font-size: 1.75rem; font-weight: 700; color: #2563EB; }
.stat-label { font-size: 0.8125rem; color: #78716C; margin-top: 4px; }
</style>
