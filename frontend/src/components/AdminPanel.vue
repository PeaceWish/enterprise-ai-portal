<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>智能体管理</h1>
        <p>管理智能体接入参数、状态及可见性权限</p>
      </div>
      <button class="add-btn" @click="openDialog(null)" v-if="authStore.hasPermission('agent:create')">
        <el-icon><Plus /></el-icon>
      </button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-num">{{ agents.length }}</span>
        <span class="stat-label">智能体总数</span>
      </div>
      <div class="stat-card green">
        <span class="stat-num">{{ activeCount }}</span>
        <span class="stat-label">已激活</span>
      </div>
      <div class="stat-card red">
        <span class="stat-num">{{ inactiveCount }}</span>
        <span class="stat-label">已禁用</span>
      </div>
    </div>

    <div class="table-section">
      <div class="table-head">
        <h3>智能体列表</h3>
        <div class="search-wrap">
          <el-icon><Search /></el-icon>
          <input v-model="searchKeyword" type="text" placeholder="搜索..." />
        </div>
      </div>

      <table class="tbl">
        <thead>
          <tr>
            <th style="width:40px">#</th>
            <th>智能体</th>
            <th>App ID</th>
            <th>可见性</th>
            <th style="width:90px">状态</th>
            <th style="width:140px">创建时间</th>
            <th style="width:80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(a, i) in filtered" :key="a.id">
            <td class="idx">{{ i + 1 }}</td>
            <td>
              <div class="agent-cell">
                <el-avatar :size="28" :src="a.iconUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + a.agentName" />
                <div>
                  <span class="name">{{ a.agentName }}</span>
                  <span class="desc">{{ a.description || '-' }}</span>
                </div>
              </div>
            </td>
            <td><code>{{ a.difyAppId?.slice(0, 8) }}...</code></td>
            <td>
              <span :class="['vis-tag', a.visibility]">{{ visibilityLabel(a.visibility) }}</span>
            </td>
            <td>
              <span :class="['badge', a.status === 1 ? 'on' : 'off']">
                {{ a.status === 1 ? '在线' : '离线' }}
              </span>
            </td>
            <td class="time">{{ fmtDate(a.createdAt) }}</td>
            <td>
              <div class="actions">
                <button class="act-btn edit" @click="openDialog(a)" title="编辑" v-if="authStore.hasPermission('agent:edit')">
                  <el-icon><Edit /></el-icon>
                </button>
                <button class="act-btn del" @click="handleDelete(a)" title="删除" v-if="authStore.hasPermission('agent:delete')">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td colspan="7" class="empty-row">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 弹窗 -->
    <div v-if="dialogVisible" class="overlay" @click.self="dialogVisible = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ form.id ? '编辑智能体' : '新增智能体' }}</h3>
          <button class="close-btn" @click="dialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>名称 <em>*</em></label>
            <input v-model="form.agentName" placeholder="例如：DeepSeek 调试助手" />
          </div>
          <div class="field">
            <label>描述</label>
            <textarea v-model="form.description" rows="2" placeholder="简要描述功能..."></textarea>
          </div>
          <div class="field">
            <label>系统提示词 (Prompt)</label>
            <textarea v-model="form.prompt" rows="4" placeholder="输入该智能体的系统提示词，例如：你是一个差旅报销助手，专门回答差旅报销相关问题..."></textarea>
            <p class="field-hint">首次对话时，该提示词会通过 inputs.system_prompt 注入 Dify 应用</p>
          </div>
          <div class="field">
            <label>App ID <em>*</em></label>
            <input v-model="form.difyAppId" placeholder="从 Dify 控制台获取" />
          </div>
          <div class="field">
            <label>API Key <em>*</em></label>
            <div class="pwd-wrap">
              <input :type="showPwd ? 'text' : 'password'" v-model="form.difyApiKey" placeholder="从 Dify 控制台获取" />
              <button class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</button>
            </div>
          </div>

          <!-- 可见性权限配置 -->
          <div class="field">
            <label>可见性</label>
            <select v-model="form.visibility" class="form-select">
              <option value="public">全员可见</option>
              <option value="role">按角色限制</option>
              <option value="dept">按部门限制</option>
              <option value="private">仅创建者</option>
            </select>
          </div>

          <div class="field" v-if="form.visibility === 'role'">
            <label>可见角色</label>
            <div class="checkbox-group">
              <label v-for="r in roleList" :key="r.id" class="checkbox-item">
                <input type="checkbox" :value="r.id" v-model="form.selectedRoles" />
                <span>{{ r.roleName }}</span>
              </label>
            </div>
          </div>

          <div class="field" v-if="form.visibility === 'dept'">
            <label>可见部门</label>
            <div class="checkbox-group">
              <label v-for="d in deptList" :key="d.id" class="checkbox-item">
                <input type="checkbox" :value="d.id" v-model="form.selectedDepts" />
                <span>{{ d.deptName }}</span>
              </label>
            </div>
          </div>

          <div class="field switch-field">
            <label>状态</label>
            <label class="switch">
              <input type="checkbox" v-model="form.status" :true-value="1" :false-value="0" />
              <span class="slider"></span>
              <span class="switch-text">{{ form.status === 1 ? '在线' : '离线' }}</span>
            </label>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn-ok" @click="handleSave" :disabled="saving">
            {{ saving ? '保存中...' : (form.id ? '保存' : '创建') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const agents = ref<any[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const showPwd = ref(false)
const roleList = ref<any[]>([])
const deptList = ref<any[]>([])

const form = ref<any>({
  agentName: '', difyAppId: '', difyApiKey: '', description: '', prompt: '', status: 1,
  visibility: 'public', selectedRoles: [], selectedDepts: []
})

const activeCount = computed(() => agents.value.filter(a => a.status === 1).length)
const inactiveCount = computed(() => agents.value.filter(a => a.status === 0).length)
const filtered = computed(() => {
  if (!searchKeyword.value) return agents.value
  const k = searchKeyword.value.toLowerCase()
  return agents.value.filter(a => a.agentName?.toLowerCase().includes(k) || a.difyAppId?.includes(k))
})

const visibilityLabel = (v: string) => {
  const map: Record<string, string> = { public: '全员', role: '角色', dept: '部门', private: '私有' }
  return map[v] || v
}

const fetchAgents = async () => {
  try { const res = await axios.get('/api/agents/list'); agents.value = res.data.data } catch {}
}

const fetchRoles = async () => {
  try { const res = await axios.get('/api/admin/roles'); roleList.value = res.data.data || [] } catch {}
}

const fetchDepts = async () => {
  try { const res = await axios.get('/api/admin/depts', { params: { onlyEnabled: true } }); deptList.value = res.data.data || [] } catch {}
}

const openDialog = (row: any) => {
  if (row) {
    let selectedRoles: number[] = []
    let selectedDepts: number[] = []
    try {
      if (row.allowedRoles) selectedRoles = JSON.parse(row.allowedRoles)
    } catch {}
    try {
      if (row.allowedDepts) selectedDepts = JSON.parse(row.allowedDepts)
    } catch {}
    form.value = {
      ...row,
      selectedRoles,
      selectedDepts
    }
  } else {
    form.value = { agentName: '', difyAppId: '', difyApiKey: '', description: '', prompt: '', status: 1, visibility: 'public', selectedRoles: [], selectedDepts: [] }
  }
  showPwd.value = false
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.agentName?.trim() || !form.value.difyAppId?.trim() || !form.value.difyApiKey?.trim()) {
    ElMessage.warning('请填写必填项'); return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      allowedRoles: JSON.stringify(form.value.selectedRoles || []),
      allowedDepts: JSON.stringify(form.value.selectedDepts || [])
    }
    delete payload.selectedRoles
    delete payload.selectedDepts
    await axios.post('/api/agents/save', payload)
    ElMessage.success('已保存'); dialogVisible.value = false; fetchAgents()
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除「${row.agentName}」吗？`, '确认', { type: 'warning' }).then(async () => {
    try { await axios.delete(`/api/agents/${row.id}`); ElMessage.success('已删除'); fetchAgents() } catch { ElMessage.error('删除失败') }
  })
}

const fmtDate = (s: string) => s ? new Date(s).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'

onMounted(() => {
  fetchAgents()
  fetchRoles()
  fetchDepts()
})
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }

.add-btn {
  width: 36px; height: 36px; border: 1px solid #E7E5E4; background: #fff;
  border-radius: 8px; cursor: pointer; color: #44403C;
  display: flex; align-items: center; justify-content: center;
  transition: all 150ms;
}
.add-btn:hover { border-color: #2563EB; color: #2563EB; background: #EFF6FF; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card {
  background: #fff; border: 1px solid #E7E5E4; border-radius: 10px;
  padding: 16px 20px; display: flex; flex-direction: column; gap: 4px;
}
.stat-num { font-size: 1.5rem; font-weight: 700; color: #1C1917; }
.stat-label { font-size: 0.75rem; color: #A8A29E; }
.stat-card.green .stat-num { color: #059669; }
.stat-card.red .stat-num { color: #DC2626; }

.table-section { background: #fff; border: 1px solid #E7E5E4; border-radius: 10px; overflow: hidden; }
.table-head { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; border-bottom: 1px solid #F5F5F4; }
.table-head h3 { font-size: 0.875rem; font-weight: 600; color: #1C1917; margin: 0; }
.search-wrap {
  display: flex; align-items: center; gap: 6px; background: #FAFAF9;
  border: 1px solid #E7E5E4; border-radius: 8px; padding: 0 10px; height: 32px;
}
.search-wrap .el-icon { font-size: 14px; color: #A8A29E; }
.search-wrap input { border: none; background: transparent; outline: none; font-size: 0.8125rem; color: #44403C; width: 140px; }
.search-wrap input::placeholder { color: #D6D3D1; }

.tbl { width: 100%; border-collapse: collapse; }
.tbl th { background: #FAFAF9; font-size: 0.75rem; font-weight: 600; color: #78716C; padding: 10px 16px; text-align: left; border-bottom: 1px solid #F5F5F4; }
.tbl td { padding: 12px 16px; font-size: 0.8125rem; color: #44403C; border-bottom: 1px solid #F5F5F4; vertical-align: middle; }
.tbl tbody tr:hover { background: #FAFAF9; }
.tbl code { font-family: 'JetBrains Mono', monospace; font-size: 0.6875rem; background: #F5F5F4; padding: 2px 6px; border-radius: 4px; color: #78716C; }
.idx { color: #A8A29E; text-align: center !important; }
.time { color: #A8A29E; font-size: 0.75rem; }

.agent-cell { display: flex; align-items: center; gap: 10px; }
.agent-cell .name { font-weight: 600; color: #1C1917; display: block; }
.agent-cell .desc { font-size: 0.75rem; color: #A8A29E; }

.vis-tag { font-size: 0.75rem; padding: 2px 8px; border-radius: 12px; font-weight: 500; }
.vis-tag.public { background: #ECFDF5; color: #059669; }
.vis-tag.role { background: #EFF6FF; color: #2563EB; }
.vis-tag.dept { background: #FEF3C7; color: #D97706; }
.vis-tag.private { background: #F3F4F6; color: #6B7280; }

.badge { font-size: 0.75rem; padding: 3px 10px; border-radius: 20px; font-weight: 500; }
.badge.on { background: #ECFDF5; color: #059669; }
.badge.off { background: #FEF2F2; color: #DC2626; }

.actions { display: flex; gap: 4px; justify-content: center; }
.act-btn {
  width: 28px; height: 28px; border: 1px solid #E7E5E4; background: #fff;
  border-radius: 6px; cursor: pointer; color: #A8A29E;
  display: flex; align-items: center; justify-content: center; transition: all 150ms;
}
.act-btn .el-icon { font-size: 14px; }
.act-btn.edit:hover { border-color: #2563EB; color: #2563EB; background: #EFF6FF; }
.act-btn.del:hover { border-color: #DC2626; color: #DC2626; background: #FEF2F2; }

.empty-row { text-align: center; color: #A8A29E; padding: 40px !important; }

/* 弹窗 */
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
  backdrop-filter: blur(2px);
}
.modal {
  background: #fff; border-radius: 14px; width: 480px;
  box-shadow: 0 24px 48px rgba(0,0,0,0.12); overflow: hidden;
  animation: modalIn 200ms ease-out;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(0.97); } to { opacity: 1; transform: none; } }

.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #F5F5F4; }
.modal-head h3 { font-size: 0.9375rem; font-weight: 600; color: #1C1917; margin: 0; }
.close-btn { width: 28px; height: 28px; border: none; background: transparent; font-size: 1.25rem; color: #A8A29E; cursor: pointer; border-radius: 6px; }
.close-btn:hover { background: #FAFAF9; color: #44403C; }

.modal-body { padding: 20px; max-height: 70vh; overflow-y: auto; }
.field { margin-bottom: 16px; }
.field:last-child { margin-bottom: 0; }
.field label { display: block; font-size: 0.8125rem; font-weight: 500; color: #44403C; margin-bottom: 6px; }
.field em { color: #DC2626; font-style: normal; }

.field input[type="text"], .field input[type="password"], .field textarea, .form-select {
  width: 100%; padding: 8px 12px; border: 1px solid #E7E5E4; border-radius: 8px;
  font-size: 0.875rem; color: #1C1917; outline: none; transition: border-color 150ms, box-shadow 150ms;
  font-family: inherit; background: #fff;
}
.field input:focus, .field textarea:focus, .form-select:focus { border-color: #2563EB; box-shadow: 0 0 0 3px rgba(37,99,235,0.06); }
.field input::placeholder, .field textarea::placeholder { color: #D6D3D1; }
.field textarea { resize: vertical; min-height: 56px; }

.pwd-wrap { position: relative; display: flex; align-items: center; }
.pwd-wrap input { padding-right: 56px !important; }
.pwd-toggle {
  position: absolute; right: 8px; border: none; background: transparent;
  font-size: 0.75rem; color: #A8A29E; cursor: pointer; padding: 4px 8px; border-radius: 4px;
}
.pwd-toggle:hover { background: #F5F5F4; color: #44403C; }

.checkbox-group { display: flex; flex-wrap: wrap; gap: 10px; }
.checkbox-item { display: flex; align-items: center; gap: 4px; font-size: 0.8125rem; color: #44403C; cursor: pointer; }
.checkbox-item input { cursor: pointer; }

.switch-field { display: flex; align-items: center; justify-content: space-between; background: #FAFAF9; padding: 10px 14px; border-radius: 8px; margin-bottom: 0 !important; }
.switch { display: inline-flex; align-items: center; gap: 10px; cursor: pointer; font-size: 0.875rem; color: #44403C; font-weight: 500; }
.switch input { display: none; }
.switch .slider { width: 36px; height: 20px; background: #D6D3D1; border-radius: 20px; position: relative; transition: background 200ms; }
.switch .slider::before { content: ''; position: absolute; width: 14px; height: 14px; left: 3px; top: 3px; background: #fff; border-radius: 50%; transition: transform 200ms; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }
.switch input:checked + .slider { background: #2563EB; }
.switch input:checked + .slider::before { transform: translateX(16px); }

.modal-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid #F5F5F4; }
.btn-cancel { padding: 8px 18px; border: 1px solid #E7E5E4; background: #fff; border-radius: 8px; font-size: 0.8125rem; color: #44403C; cursor: pointer; }
.btn-cancel:hover { background: #FAFAF9; }
.field-hint { font-size: 0.75rem; color: #A8A29E; margin-top: 4px; line-height: 1.4; }
.btn-ok { padding: 8px 20px; border: none; background: #2563EB; border-radius: 8px; font-size: 0.8125rem; font-weight: 500; color: #fff; cursor: pointer; }
.btn-ok:hover { background: #1D4ED8; }
.btn-ok:disabled { background: #93C5FD; cursor: not-allowed; }
</style>
