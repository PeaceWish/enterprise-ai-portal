<template>
  <div class="prompt-view">
    <div class="view-header">
      <div>
        <h1>提示词配置</h1>
        <p>配置新对话快捷提示词和对话框底部提示语</p>
      </div>
    </div>

    <div class="tabs">
      <button :class="['tab-btn', { active: activeTab === 'prompts' }]" @click="activeTab = 'prompts'">快捷提示词</button>
      <button :class="['tab-btn', { active: activeTab === 'footer' }]" @click="activeTab = 'footer'">底部提示语</button>
    </div>

    <section v-if="activeTab === 'prompts'" class="tab-panel">
      <div class="section-head">
        <h3>快捷提示词</h3>
        <button class="add-btn" @click="openDialog(null)"><el-icon><Plus /></el-icon></button>
      </div>
      <div class="prompt-list">
        <div v-for="(item, i) in prompts" :key="i" class="prompt-card">
          <div class="prompt-info"><span class="prompt-text">{{ item.text }}</span></div>
          <div class="prompt-actions">
            <button class="act-btn edit" @click="openDialog({ index: i, ...item })"><el-icon><Edit /></el-icon></button>
            <button class="act-btn del" @click="handleDelete(i)"><el-icon><Delete /></el-icon></button>
          </div>
        </div>
        <div v-if="prompts.length === 0" class="empty">暂无提示词</div>
      </div>
    </section>

    <section v-if="activeTab === 'footer'" class="tab-panel">
      <div class="section-head"><h3>对话框底部提示语</h3></div>
      <div class="footer-form">
        <textarea v-model="chatFooterHint" rows="4" placeholder="例如：AI 可能犯错，请核实重要信息。"></textarea>
        <div class="preview">
          <span class="preview-label">预览：</span>
          <span>{{ chatFooterHint || '不显示提示语' }}</span>
        </div>
      </div>
    </section>

    <div class="page-actions">
      <button class="btn-ok" @click="saveAll" :disabled="saving">{{ saving ? '保存中...' : '保存配置' }}</button>
    </div>

    <div v-if="dialogVisible" class="overlay" @click.self="dialogVisible = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ editIndex >= 0 ? '编辑提示词' : '新增提示词' }}</h3>
          <button class="close-btn" @click="dialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>提示词内容 <em>*</em></label>
            <input v-model="formText" placeholder="例如：帮我写一份周报" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn-ok" @click="handleSavePrompt">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const defaultPrompts = [
  { text: '帮我写一份周报' },
  { text: '解释这段代码' },
  { text: '翻译这段文字' },
  { text: '帮我制定计划' },
]

const activeTab = ref<'prompts' | 'footer'>('prompts')
const prompts = ref<{text: string}[]>([...defaultPrompts])
const chatFooterHint = ref('AI 可能犯错，请核实重要信息。')
const saving = ref(false)

const dialogVisible = ref(false)
const editIndex = ref(-1)
const formText = ref('')

const load = async () => {
  try {
    const res = await axios.get('/api/config/ui')
    const data = res.data.data || {}
    prompts.value = Array.isArray(data.quickPrompts) ? data.quickPrompts : [...defaultPrompts]
    chatFooterHint.value = data.chatFooterHint || 'AI 可能犯错，请核实重要信息。'
  } catch {
    prompts.value = [...defaultPrompts]
  }
}

const openDialog = (row: any) => {
  if (row && row.index !== undefined) {
    editIndex.value = row.index
    formText.value = row.text
  } else {
    editIndex.value = -1
    formText.value = ''
  }
  dialogVisible.value = true
}

const handleSavePrompt = () => {
  if (!formText.value.trim()) { ElMessage.warning('请输入提示词'); return }
  if (editIndex.value >= 0) {
    prompts.value[editIndex.value].text = formText.value.trim()
  } else {
    prompts.value.push({ text: formText.value.trim() })
  }
  dialogVisible.value = false
}

const handleDelete = (i: number) => {
  prompts.value.splice(i, 1)
}

const saveAll = async () => {
  saving.value = true
  try {
    await axios.post('/api/config/ui', {
      quickPrompts: prompts.value,
      chatFooterHint: chatFooterHint.value
    })
    ElMessage.success('已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.prompt-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
.tabs { display: flex; gap: 8px; border-bottom: 1px solid #E7E5E4; margin-bottom: 20px; }
.tab-btn { border: none; background: transparent; padding: 10px 14px; cursor: pointer; color: #78716C; font-size: 0.875rem; border-bottom: 2px solid transparent; }
.tab-btn.active { color: #2563EB; border-bottom-color: #2563EB; font-weight: 600; }
.tab-panel { max-width: 720px; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-head h3 { font-size: 0.9375rem; color: #1C1917; margin: 0; }
.add-btn { width: 36px; height: 36px; border: 1px solid #E7E5E4; background: #fff; border-radius: 8px; cursor: pointer; color: #44403C; display: flex; align-items: center; justify-content: center; transition: all 150ms; }
.add-btn:hover { border-color: #2563EB; color: #2563EB; background: #EFF6FF; }
.prompt-list { display: flex; flex-direction: column; gap: 8px; }
.prompt-card { display: flex; align-items: center; justify-content: space-between; background: #fff; border: 1px solid #E7E5E4; border-radius: 10px; padding: 14px 16px; }
.prompt-text { font-size: 0.9375rem; color: #1C1917; }
.prompt-actions { display: flex; gap: 4px; }
.act-btn { width: 28px; height: 28px; border: 1px solid #E7E5E4; background: #fff; border-radius: 6px; cursor: pointer; color: #A8A29E; display: flex; align-items: center; justify-content: center; transition: all 150ms; }
.act-btn .el-icon { font-size: 14px; }
.act-btn.edit:hover { border-color: #2563EB; color: #2563EB; background: #EFF6FF; }
.act-btn.del:hover { border-color: #DC2626; color: #DC2626; background: #FEF2F2; }
.empty { text-align: center; color: #A8A29E; padding: 40px; }
.footer-form textarea { width: 100%; padding: 10px 12px; border: 1px solid #E7E5E4; border-radius: 8px; font-size: 0.875rem; color: #1C1917; outline: none; resize: vertical; font-family: inherit; }
.footer-form textarea:focus { border-color: #2563EB; box-shadow: 0 0 0 3px rgba(37,99,235,0.06); }
.preview { margin-top: 12px; padding: 12px; background: #FAFAF9; border: 1px solid #F5F5F4; border-radius: 8px; font-size: 0.8125rem; color: #78716C; }
.preview-label { color: #44403C; font-weight: 600; }
.page-actions { margin-top: 24px; }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.modal { background: #fff; border-radius: 14px; width: 400px; box-shadow: 0 24px 48px rgba(0,0,0,0.12); overflow: hidden; animation: modalIn 200ms ease-out; }
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(0.97); } to { opacity: 1; transform: none; } }
.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #F5F5F4; }
.modal-head h3 { font-size: 0.9375rem; font-weight: 600; color: #1C1917; margin: 0; }
.close-btn { width: 28px; height: 28px; border: none; background: transparent; font-size: 1.25rem; color: #A8A29E; cursor: pointer; border-radius: 6px; }
.close-btn:hover { background: #FAFAF9; color: #44403C; }
.modal-body { padding: 20px; }
.field label { display: block; font-size: 0.8125rem; font-weight: 500; color: #44403C; margin-bottom: 6px; }
.field em { color: #DC2626; font-style: normal; }
.field input { width: 100%; padding: 8px 12px; border: 1px solid #E7E5E4; border-radius: 8px; font-size: 0.875rem; color: #1C1917; outline: none; transition: border-color 150ms, box-shadow 150ms; }
.field input:focus { border-color: #2563EB; box-shadow: 0 0 0 3px rgba(37,99,235,0.06); }
.field input::placeholder { color: #D6D3D1; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid #F5F5F4; }
.btn-cancel { padding: 8px 18px; border: 1px solid #E7E5E4; background: #fff; border-radius: 8px; font-size: 0.8125rem; color: #44403C; cursor: pointer; }
.btn-cancel:hover { background: #FAFAF9; }
.btn-ok { padding: 8px 20px; border: none; background: #2563EB; border-radius: 8px; font-size: 0.8125rem; font-weight: 500; color: #fff; cursor: pointer; }
.btn-ok:hover { background: #1D4ED8; }
.btn-ok:disabled { background: #93C5FD; cursor: not-allowed; }
</style>
