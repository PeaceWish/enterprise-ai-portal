<template>
  <div class="market-view">
    <div class="market-header">
      <h1>智能体广场</h1>
      <p class="subtitle">选择一个智能体开始对话</p>
    </div>
    <div class="card-grid">
      <div v-for="agent in agents" :key="agent.id" class="card-item" @click="selectAgent(agent)">
        <div class="card-header">
          <div class="avatar-wrap">
            <el-avatar :size="40" :src="agent.iconUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + agent.agentName" />
          </div>
          <span class="verified-tag">官方</span>
        </div>
        <div class="card-body">
          <h3 class="agent-name">{{ agent.agentName }}</h3>
          <p class="agent-desc">{{ agent.description || '赋能业务增长的 AI 助手' }}</p>
        </div>
        <div class="card-footer">
          <span class="model-info">DeepSeek-V3</span>
          <span class="go-text">开始对话 →</span>
        </div>
      </div>
      <div v-if="agents.length === 0" class="empty-state">
        <p>暂无可用智能体</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
const router = useRouter()
const agents = ref<any[]>([])
onMounted(async () => {
  try { const res = await axios.get('/api/agents/active'); agents.value = res.data.data || [] } catch {}
})

const selectAgent = (agent: any) => {
  router.push({ path: '/chat', query: { agentId: agent.id } })
}
</script>

<style scoped>
.market-view { width: 100%; padding: 48px 40px; overflow-y: auto; }
.market-header { max-width: 800px; margin: 0 auto 32px; }
.market-header h1 { font-size: 1.5rem; font-weight: 700; color: #1C1917; margin: 0 0 6px; }
.subtitle { font-size: 0.875rem; color: #78716C; margin: 0; }

.card-grid {
  max-width: 800px; margin: 0 auto;
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px;
}

.card-item {
  background: #fff; border: 1px solid #E7E5E4; border-radius: 12px;
  padding: 20px; cursor: pointer; transition: border-color 150ms, box-shadow 150ms;
  display: flex; flex-direction: column;
}
.card-item:hover { border-color: #2563EB; box-shadow: 0 2px 8px rgba(37,99,235,0.06); }

.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.avatar-wrap { background: #EFF6FF; border-radius: 10px; padding: 8px; }
.verified-tag { font-size: 0.6875rem; color: #2563EB; background: #EFF6FF; padding: 2px 8px; border-radius: 12px; font-weight: 500; }

.card-body { flex: 1; margin-bottom: 14px; }
.agent-name { font-size: 0.9375rem; font-weight: 600; color: #1C1917; margin: 0 0 6px; }
.agent-desc { font-size: 0.8125rem; color: #78716C; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12px; border-top: 1px solid #F5F5F4; margin-top: auto;
}
.model-info { font-size: 0.6875rem; color: #A8A29E; }
.go-text { font-size: 0.8125rem; color: #2563EB; font-weight: 500; }

.empty-state { text-align: center; padding: 60px 20px; grid-column: 1 / -1; }
.empty-state p { color: #A8A29E; }
</style>