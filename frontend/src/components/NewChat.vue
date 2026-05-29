<script setup lang="ts">
import { ref, nextTick, onMounted, watch, onUnmounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Promotion, CloseBold, MagicStick, Loading, ChatLineRound } from '@element-plus/icons-vue'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import axios from 'axios'

const props = defineProps<{
  convId?: number | null
  initialAgent?: any
}>()
const emit = defineEmits(['save-conversation'])
const route = useRoute()
const authStore = useAuthStore()

const inputMsg = ref('')
const loading = ref(false)
const messages = ref<any[]>([])
const scrollRef = ref<any>(null)
const inputRef = ref<any>(null)
const collapsedThinks = ref<Record<number, boolean>>({})
const conversationTitle = ref('')
const difyConvId = ref('')
const dbConvId = ref<number | null>(null)
const currentAgentId = ref<number | null>(null)
const currentAgent = ref<any>(null)
const historyLoaded = ref(false)
let abortCtrl: AbortController | null = null
let streamSeq = 0

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

const defaultQuickPrompts = [
  { text: '帮我写一份周报' },
  { text: '解释这段代码' },
  { text: '翻译这段文字' },
  { text: '帮我制定计划' },
]
const quickPrompts = ref(defaultQuickPrompts)
const chatFooterHint = ref('AI 可能犯错，请核实重要信息。')

const loadUiConfig = async () => {
  try {
    const res = await axios.get('/api/config/ui')
    const data = res.data.data || {}
    quickPrompts.value = Array.isArray(data.quickPrompts) ? data.quickPrompts : defaultQuickPrompts
    chatFooterHint.value = data.chatFooterHint || 'AI 可能犯错，请核实重要信息。'
  } catch {
    quickPrompts.value = defaultQuickPrompts
  }
}

// 从 DB 加载历史
const loadHistory = async (cid: number) => {
  cancelActiveStream()
  historyLoaded.value = true
  dbConvId.value = cid
  difyConvId.value = ''
  try {
    const res = await axios.get(`/api/conversations/${cid}/messages`)
    const msgs = res.data.messages as Array<{role: string, content: string}>
    // 恢复 Dify conversation ID
    if (res.data.difyConversationId) {
      difyConvId.value = res.data.difyConversationId
    }
    // 恢复 agent ID 和智能体展示信息，保证历史续聊仍绑定原智能体
    if (res.data.agentId) {
      currentAgentId.value = Number(res.data.agentId)
    } else {
      currentAgentId.value = null
      currentAgent.value = null
    }
    if (res.data.agent) {
      currentAgent.value = res.data.agent
    } else if (res.data.agentId) {
      currentAgent.value = { id: Number(res.data.agentId), agentName: res.data.agentName || '历史智能体' }
    }
    
    messages.value = msgs.map(m => ({
      role: m.role,
      content: m.content || '',
      renderedContent: renderMarkdown(m.content || ''),
      thinkContent: ''
    }))
    const firstUser = msgs.find(m => m.role === 'user')
    conversationTitle.value = firstUser?.content?.slice(0, 20) || ''
  } catch (e) {
    console.error('加载历史消息失败:', e)
    messages.value = []
  }
  scrollToBottom()
  nextTick(() => inputRef.value?.focus())
}

const cancelActiveStream = () => {
  streamSeq++
  loading.value = false
  if (abortCtrl) {
    abortCtrl.abort()
    abortCtrl = null
  }
}

// 重置对话内容。clearAgent=true 表示回到普通新对话，必须清掉智能体绑定。
const resetState = (clearAgent = false) => {
  historyLoaded.value = false
  dbConvId.value = null
  difyConvId.value = ''
  conversationTitle.value = ''
  messages.value = []
  inputMsg.value = ''
  loading.value = false
  cancelActiveStream()
  if (clearAgent) {
    currentAgentId.value = null
    currentAgent.value = null
  }
}

// 监听 initialAgent 变化（智能体广场选中后进入对话）
watch(() => props.initialAgent?.id, (newAgentId) => {
  if (newAgentId) {
    currentAgentId.value = Number(newAgentId)
    currentAgent.value = props.initialAgent
  } else if (!props.convId) {
    currentAgentId.value = null
    currentAgent.value = null
  }
}, { immediate: true })

const getRouteConvId = () => {
  const raw = route.params.convId ?? props.convId
  const val = Array.isArray(raw) ? raw[0] : raw
  return val ? Number(val) : null
}

// 监听 convId（历史对话加载）。从历史回到 /chat 时必须清空历史智能体绑定。
watch(getRouteConvId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadHistory(newId)
  } else if ((newId === null || newId === undefined) && oldId) {
    resetState(true)
    nextTick(() => inputRef.value?.focus())
  }
}, { immediate: true })

const toggleThink = (index: number) => { collapsedThinks.value[index] = !collapsedThinks.value[index] }
const sendQuickPrompt = (prompt: any) => { inputMsg.value = prompt.text; sendMessage() }
const finishGenerating = (token?: number) => {
  if (token !== undefined && token !== streamSeq) return
  loading.value = false
  if (abortCtrl) {
    abortCtrl.abort()
    abortCtrl = null
  }
}

const stopGenerating = () => {
  cancelActiveStream()
}

const buildHistoryFallback = () => {
  // 当前连续对话优先走 Dify conversation_id。
  // 从数据库历史点进来的续聊，不复用旧 Dify conversation_id，避免旧会话/工作流 conversation_id 失效导致一直 ping、无回复；改用本地历史兜底。
  if (!historyLoaded.value && difyConvId.value) return undefined
  if (messages.value.length === 0) return undefined
  return messages.value
    .filter(m => (m.role === 'user' || m.role === 'assistant') && (m.content || '').trim())
    .slice(-8)
    .map(m => ({ role: m.role, content: m.content }))
}

// 核心：发送消息
const sendMessage = async () => {
  const text = inputMsg.value.trim()
  if (!text || loading.value) return
  
  const historyMessages = buildHistoryFallback()
  const requestConversationId = historyLoaded.value ? '' : difyConvId.value
  
  // 如果是第一条消息，设置标题
  if (messages.value.length === 0) {
    conversationTitle.value = text.slice(0, 20) + (text.length > 20 ? '...' : '')
    historyLoaded.value = false
  }
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    renderedContent: renderMarkdown(text)
  })
  inputMsg.value = ''
  loading.value = true
  
  // 添加 AI 占位
  const aiIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', renderedContent: '', thinkContent: '' })
  scrollToBottom()

  abortCtrl = new AbortController()
  const requestRouteFullPath = route.fullPath
  const streamToken = ++streamSeq
  const isCurrentStream = () => streamToken === streamSeq && route.fullPath === requestRouteFullPath && !!messages.value[aiIdx]
  let savedThisRound = false
  const saveOnce = () => {
    if (savedThisRound || !isCurrentStream()) return
    savedThisRound = true
    if (messages.value.length > 1) saveToDB()
  }

  try {
    await fetchEventSource('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {})
      },
      body: JSON.stringify({
        query: text,
        agentId: currentAgentId.value,
        conversationId: requestConversationId,
        historyMessages
      }),
      signal: abortCtrl.signal,
      onmessage(ev) {
        if (!isCurrentStream()) return
        try {
          const data = JSON.parse(ev.data)
          // 从任意事件中提取 conversation_id（不限于 answer 事件）
          if (data.conversation_id) {
            difyConvId.value = data.conversation_id
          }
          if (data.answer) {
            messages.value[aiIdx].content += data.answer
            const thinkMatch = messages.value[aiIdx].content.match(/<think>([\s\S]*?)<\/think>/)
            if (thinkMatch) {
              messages.value[aiIdx].thinkContent = renderMarkdown(thinkMatch[1].trim())
              messages.value[aiIdx].renderedContent = renderMarkdown(
                messages.value[aiIdx].content.replace(/<think>[\s\S]*?<\/think>/, '').trim()
              )
            } else {
              messages.value[aiIdx].renderedContent = renderMarkdown(messages.value[aiIdx].content)
            }
            scrollToBottom()
          }
          // Dify 主动报错事件：必须结束 loading，否则会一直显示“回答中”。
          if (data.event === 'error') {
            const msg = data.message || data.error || '对话失败，请重试'
            messages.value[aiIdx].content = msg
            messages.value[aiIdx].renderedContent = `<p style="color:#e54d42">${msg}</p>`
            finishGenerating(streamToken)
            return
          }
          // Dify 工作流/对话结束事件。不能只等 onclose，否则 UI 会一直停在“回答中”。
          if (['message_end', 'workflow_finished', 'tts_message_end'].includes(data.event)) {
            finishGenerating(streamToken)
            saveOnce()
          }
        } catch (e) {}
      },
      onclose() {
        finishGenerating(streamToken)
        saveOnce()
      },
      onerror(err) {
        if (!isCurrentStream()) throw err
        finishGenerating(streamToken)
        messages.value[aiIdx].renderedContent = '<p style="color:#e54d42">请求失败，请重试</p>'
        throw err
      }
    })
  } catch (err) {
    if (streamToken === streamSeq) {
      loading.value = false
      abortCtrl = null
    }
  }
}

// 保存到数据库
const saveToDB = async () => {
  if (!conversationTitle.value) return
  try {
    const res = await axios.post('/api/conversations/save', {
      userId: authStore.userId ? String(authStore.userId) : 'default_user',
      title: conversationTitle.value,
      conversationId: dbConvId.value,
      difyConversationId: difyConvId.value,
      agentId: currentAgentId.value,
      messages: messages.value.map(m => ({ role: m.role, content: m.content }))
    })
    dbConvId.value = res.data.conversationId
    emit('save-conversation', { conversationId: res.data.conversationId, title: conversationTitle.value })
    window.dispatchEvent(new CustomEvent('conversation-saved'))
  } catch (e) {
    console.error('保存对话失败:', e)
  }
}

const scrollToBottom = () => nextTick(() => {
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
})

// 从 URL 参数加载智能体（支持 onMounted + onActivated + watch）
const loadAgentFromUrl = () => {
  const urlAgentId = route.query.agentId as string | undefined
  if (urlAgentId) {
    const nextAgentId = Number(urlAgentId)
    // 从广场点击智能体时，明确开启一条新的智能体对话，不能沿用常规新对话或其他智能体上下文
    if (currentAgentId.value !== nextAgentId || messages.value.length > 0 || dbConvId.value || difyConvId.value) {
      resetState()
    }
    currentAgentId.value = nextAgentId
    axios.get(`/api/agents/active`).then(res => {
      const list = res.data.data || []
      const found = list.find((a: any) => String(a.id) === urlAgentId)
      if (found) {
        currentAgent.value = found
      }
    }).catch(() => {})
  } else if (!route.params.convId) {
    // 进入 /chat 且没有 agentId 时，就是常规新对话，必须清掉上一次智能体绑定
    resetState(true)
  }
}

onMounted(() => {
  loadUiConfig()
  loadAgentFromUrl()
  nextTick(() => inputRef.value?.focus())
})

// keep-alive 缓存后再次进入时触发
onActivated(() => {
  loadAgentFromUrl()
  nextTick(() => inputRef.value?.focus())
})

// 监听 URL agentId / convId 变化（keep-alive 同一组件内路由切换）
watch(() => [route.query.agentId, route.params.convId, route.path], () => {
  loadAgentFromUrl()
})

onUnmounted(() => { if (abortCtrl) abortCtrl.abort() })
</script>

<template>
  <div class="new-chat-page">
    <div v-if="currentAgent" class="agent-context-bar">
      <div class="agent-pill">
        <img v-if="currentAgent.iconUrl" :src="currentAgent.iconUrl" alt="" />
        <span v-else class="agent-dot"></span>
        <span>当前智能体：{{ currentAgent.agentName || '未命名智能体' }}</span>
      </div>

    </div>
    <div class="chat-content" ref="scrollRef">
      <!-- 欢迎页 -->
      <div v-if="!historyLoaded && messages.length === 0" class="welcome-area">
        <div class="welcome-inner">
          <h1>{{ currentAgent ? `和 ${currentAgent.agentName || '智能体'} 对话` : '有什么可以帮你？' }}</h1>
          <p v-if="currentAgent?.description" class="agent-desc-line">{{ currentAgent.description }}</p>
          <div class="quick-prompts">
            <div v-for="(prompt, idx) in quickPrompts" :key="idx" class="prompt-card" @click="sendQuickPrompt(prompt)">
              <el-icon :size="16"><ChatLineRound /></el-icon>
              <span>{{ prompt.text }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-else class="message-list">
        <div v-for="(msg, index) in messages" :key="index" :class="['msg-row', msg.role]">
          <div class="msg-avatar">
            <div :class="['avatar-circle', msg.role]">
              <span v-if="msg.role === 'user'">我</span>
              <svg v-else viewBox="0 0 24 24" fill="none">
                <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
          <div class="msg-content">
            <div v-if="msg.thinkContent" class="think-box">
              <div class="think-trigger" @click="toggleThink(index)">
                <el-icon :class="{ rotating: loading && index === messages.length - 1 }">
                  <Loading v-if="loading && index === messages.length - 1"/><MagicStick v-else/>
                </el-icon>
                <span>推理过程</span>
              </div>
              <div v-if="!collapsedThinks[index]" class="think-body" v-html="msg.thinkContent"></div>
            </div>
            <div class="msg-text" v-html="msg.renderedContent"></div>
          </div>
        </div>
        <div v-if="loading" class="msg-row assistant">
          <div class="msg-avatar"><div class="avatar-circle assistant"><svg viewBox="0 0 24 24" fill="none"><path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg></div></div>
          <div class="loading-dots"><span></span><span></span><span></span></div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <div class="input-container">
        <textarea v-model="inputMsg" :placeholder="messages.length === 0 ? '发消息...' : '继续提问...'" @keydown.enter.prevent="sendMessage" ref="inputRef" rows="1"></textarea>
        <div class="input-bar">
          <button :class="['send-btn', { 'stop-btn': loading }]" @click="loading ? stopGenerating() : sendMessage()">
            <el-icon v-if="loading"><CloseBold /></el-icon>
            <el-icon v-else><Promotion /></el-icon>
          </button>
        </div>
      </div>
      <p v-if="chatFooterHint" class="input-hint">{{ chatFooterHint }}</p>
    </div>
  </div>
</template>

<style scoped>
.new-chat-page { display: flex; flex-direction: column; height: 100vh; background: #FAFAF9; width: 100%; }
.agent-context-bar { height: 48px; flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; border-bottom: 1px solid #E7E5E4; background: rgba(255,255,255,0.75); backdrop-filter: blur(10px); }
.agent-pill { display: inline-flex; align-items: center; gap: 8px; padding: 6px 10px; background: #EFF6FF; color: #1D4ED8; border-radius: 999px; font-size: 0.8125rem; font-weight: 600; }
.agent-pill img { width: 18px; height: 18px; border-radius: 50%; object-fit: cover; }
.agent-dot { width: 8px; height: 8px; border-radius: 50%; background: #2563EB; box-shadow: 0 0 0 3px rgba(37,99,235,0.12); }
.context-warning { font-size: 0.75rem; color: #B45309; background: #FFFBEB; border: 1px solid #FDE68A; padding: 4px 8px; border-radius: 999px; }
.chat-content { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
.welcome-area { flex: 1; display: flex; align-items: center; justify-content: center; padding: 0 24px; }
.welcome-inner { display: flex; flex-direction: column; align-items: center; max-width: 680px; width: 100%; }
.welcome-inner h1 { font-size: 1.75rem; font-weight: 600; color: #1C1917; letter-spacing: -0.025em; margin: 0 0 10px; text-align: center; }
.agent-desc-line { max-width: 560px; color: #78716C; font-size: 0.875rem; line-height: 1.6; text-align: center; margin: 0 0 28px; }
.quick-prompts { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; width: 100%; }
.prompt-card { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: #fff; border: 1px solid #E7E5E4; border-radius: 12px; cursor: pointer; transition: border-color 150ms, box-shadow 150ms; font-size: 0.875rem; color: #44403C; }
.prompt-card:hover { border-color: #2563EB; box-shadow: 0 1px 4px rgba(37,99,235,0.08); }
.prompt-card .el-icon { color: #2563EB; }
.message-list { flex: 1; max-width: 680px; width: 100%; margin: 0 auto; padding: 24px 24px 0; }
.msg-row { display: flex; gap: 16px; padding: 20px 0; }
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.avatar-circle { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; font-weight: 600; color: #fff; }
.avatar-circle.user { background: #78716C; }
.avatar-circle.assistant { background: #2563EB; }
.avatar-circle svg { width: 16px; height: 16px; }
.msg-content { flex: 1; min-width: 0; }
.msg-text { font-size: 0.9375rem; line-height: 1.7; color: #1C1917; }
.user .msg-text { background: #fff; border: 1px solid #E7E5E4; border-radius: 16px; border-bottom-right-radius: 4px; padding: 12px 16px; max-width: 80%; align-self: flex-end; }
.assistant .msg-text { padding: 4px 0; }
.think-box { background: #F5F5F4; border-radius: 10px; margin-bottom: 12px; border: 1px solid #E7E5E4; }
.think-trigger { padding: 8px 12px; font-size: 0.8rem; color: #78716C; cursor: pointer; display: flex; align-items: center; gap: 6px; user-select: none; }
.think-trigger:hover { color: #44403C; }
.think-body { padding: 0 12px 12px; font-size: 0.8125rem; color: #78716C; border-top: 1px solid #E7E5E4; padding-top: 10px; line-height: 1.6; }
.loading-dots { display: flex; gap: 4px; padding: 12px 0; }
.loading-dots span { width: 5px; height: 5px; background: #A8A29E; border-radius: 50%; animation: pulse 1.4s infinite; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes pulse { 0%, 100% { opacity: 0.3; } 50% { opacity: 1; } }
.rotating { animation: spin 2s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.input-area { padding: 12px 24px 20px; flex-shrink: 0; }
.input-container { max-width: 680px; margin: 0 auto; background: #fff; border: 1px solid #E7E5E4; border-radius: 16px; padding: 8px; transition: border-color 150ms, box-shadow 150ms; }
.input-container:focus-within { border-color: #2563EB; box-shadow: 0 0 0 3px rgba(37,99,235,0.06); }
.input-container textarea { width: 100%; border: none; outline: none; background: transparent; color: #1C1917; font-size: 0.9375rem; font-family: inherit; line-height: 1.5; resize: none; padding: 8px 12px; min-height: 40px; max-height: 200px; }
.input-container textarea::placeholder { color: #A8A29E; }
.input-bar { display: flex; justify-content: flex-end; padding: 2px 4px 4px; }
.send-btn { width: 32px; height: 32px; border: none; background: #2563EB; border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #fff; transition: background 150ms; }
.send-btn:hover { background: #1D4ED8; }
.send-btn:disabled { background: #E7E5E4; color: #A8A29E; cursor: not-allowed; }
.send-btn .el-icon { font-size: 16px; }
.send-btn.stop-btn { background: #DC2626; }
.send-btn.stop-btn:hover { background: #B91C1C; }
.send-btn.stop-btn .el-icon { font-size: 14px; }
.input-hint { text-align: center; font-size: 0.75rem; color: #A8A29E; margin: 8px 0 0; }
</style>