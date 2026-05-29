<template>
  <div class="chat-main glass-panel">
    <!-- Header -->
    <div class="chat-info-bar">
      <div class="left">
        <el-avatar :size="32" :src="agent?.iconUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + agent?.agentName" />
        <div class="name-status">
          <span class="name">{{ agent?.agentName }}</span>
          <span class="dot-status active">在线</span>
        </div>
      </div>
      <div class="right">
        <el-button link icon="Refresh" @click="messages = []">重置</el-button>
      </div>
    </div>

    <!-- Message List -->
    <div class="scroller" ref="scrollRef">
      <div v-for="(msg, index) in messages" :key="index" :class="['msg-row', msg.role]">
        <div class="bubble-container">
          <!-- Think Process -->
          <div v-if="msg.content.includes('<think>')" class="think-box">
            <div class="think-trigger" @click="toggleThink(index)">
              <el-icon :class="{ rotating: loading && index === messages.length - 1 }"><Loading v-if="loading && index === messages.length - 1"/><MagicStick v-else/></el-icon>
              <span>推理逻辑</span>
            </div>
            <div v-if="!collapsedThinks[index]" class="think-body" v-html="formatThink(msg.content).think"></div>
          </div>
          
          <!-- Content Bubble -->
          <div class="bubble shadow">
            <div class="markdown-body" v-html="renderMarkdown(formatThink(msg.content).content)"></div>
          </div>
          <span class="time">{{ new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
        </div>
      </div>
      <div v-if="loading && !messages[messages.length-1]?.content" class="msg-row assistant">
        <div class="bubble-container loading-bubble">
          <div class="dot-loading"><span></span><span></span><span></span></div>
        </div>
      </div>
    </div>

    <!-- Input Footer -->
    <div class="footer-input">
      <div class="input-inner glass-panel">
        <el-input
          v-model="inputMsg"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 5 }"
          placeholder="提问..."
          @keydown.ctrl.enter="sendMessage"
        />
        <div class="actions">
          <el-button type="primary" circle icon="Promotion" @click="sendMessage" :disabled="!inputMsg.trim()"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { Promotion, MagicStick, Loading, Refresh } from '@element-plus/icons-vue'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{ agent: any }>()
const authStore = useAuthStore()
const messages = ref<any[]>([])
const inputMsg = ref('')
const loading = ref(false)
const scrollRef = ref<any>(null)
const collapsedThinks = ref<Record<number, boolean>>({})

const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext'
      return hljs.highlight(code, { language }).value
    }
  })
)

const renderMarkdown = (text: string) => marked.parse(text || '')

const formatThink = (text: string) => {
  const thinkMatch = text.match(/<think>([\s\S]*?)<\/think>/)
  if (thinkMatch) {
    return {
      think: renderMarkdown(thinkMatch[1].trim()),
      content: text.replace(/<think>[\s\S]*?<\/think>/, '').trim()
    }
  }
  return { think: '', content: text }
}

const toggleThink = (index: number) => collapsedThinks.value[index] = !collapsedThinks.value[index]

const sendMessage = async () => {
  if (!inputMsg.value.trim() || loading.value) return
  const userText = inputMsg.value
  messages.value.push({ role: 'user', content: userText })
  inputMsg.value = ''
  loading.value = true
  const aiIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  scrollToBottom()

  try {
    await fetchEventSource('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {})
      },
      body: JSON.stringify({ query: userText, agentId: props.agent.id }),
      onmessage(ev) {
        try {
          const data = JSON.parse(ev.data)
          if (data.answer) {
            messages.value[aiIdx].content += data.answer
            scrollToBottom()
          }
        } catch (e) {}
      },
      onclose() { loading.value = false },
      onerror(err) { loading.value = false; throw err }
    })
  } catch (err) {
    loading.value = false
  }
}

const scrollToBottom = () => nextTick(() => scrollRef.value && (scrollRef.value.scrollTop = scrollRef.value.scrollHeight))

onMounted(() => messages.value.push({ role: 'assistant', content: `您好，我是 **${props.agent?.agentName}**。请问有什么可以帮您？` }))
</script>

<style scoped>
.chat-main {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 900px;
  height: calc(100vh - 60px);
  margin: 0 auto;
  border-radius: 20px;
}

.chat-info-bar {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left { display: flex; align-items: center; gap: 12px; }
.name-status { display: flex; flex-direction: column; }
.name { font-size: 14px; font-weight: 600; color: var(--text-main); }
.dot-status { font-size: 11px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; }
.dot-status.active::before { content: ''; width: 6px; height: 6px; background: #10b981; border-radius: 50%; }

.scroller {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.msg-row { display: flex; width: 100%; }
.msg-row.user { justify-content: flex-end; }

.bubble-container { max-width: 80%; display: flex; flex-direction: column; gap: 6px; }
.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14.5px;
  line-height: 1.6;
}

.user .bubble {
  background: var(--primary-color);
  color: white;
  border-bottom-right-radius: 4px;
}

.assistant .bubble {
  background: rgba(255, 255, 255, 0.04);
  color: #e2e8f0;
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 4px;
}

.think-box {
  background: rgba(0,0,0,0.2);
  border-radius: 10px;
  margin-bottom: 8px;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.think-trigger {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--primary-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  user-select: none;
}

.think-body {
  padding: 0 12px 10px;
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.time { font-size: 10px; color: var(--text-muted); padding: 0 4px; }
.user .time { text-align: right; }

.footer-input { padding: 20px 24px 30px; }
.input-inner {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(0,0,0,0.3) !important;
}

:deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: var(--text-main) !important;
  font-size: 14px !important;
}

.actions { padding-bottom: 4px; }

.rotating { animation: rotate 2s linear infinite; }
@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.dot-loading { display: flex; gap: 4px; padding: 10px; }
.dot-loading span { width: 4px; height: 4px; background: var(--text-muted); border-radius: 50%; animation: dot 1.4s infinite; }
.dot-loading span:nth-child(2) { animation-delay: 0.2s; }
.dot-loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dot { 0%, 100% { opacity: 0.3; } 50% { opacity: 1; } }
</style>
