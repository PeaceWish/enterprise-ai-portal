<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <el-icon :size="24" color="#2563EB"><Platform /></el-icon>
      <span class="brand">AI Portal</span>
    </div>

    <nav class="nav-menu">
      <template v-for="item in authStore.menus" :key="item.permCode">
        <!-- 有子菜单的：可折叠 -->
        <div v-if="item.children && item.children.length > 0" class="nav-group">
          <div
            class="nav-item group-header"
            :class="{ active: isGroupActive(item) }"
            @click="toggleGroup(item.permCode)"
          >
            <el-icon :size="18"><component :is="getIcon(item.permCode)" /></el-icon>
            <span>{{ item.permName }}</span>
            <el-icon :size="14" class="arrow" :class="{ expanded: expandedGroups.includes(item.permCode) }">
              <ArrowRight />
            </el-icon>
          </div>
          <transition name="slide">
            <div v-show="expandedGroups.includes(item.permCode)" class="sub-menu">
              <div
                v-for="sub in item.children"
                :key="sub.permCode"
                class="nav-item sub-item"
                :class="{ active: route.path === sub.path || route.path.startsWith(sub.path + '/') }"
                @click="navigate(sub.path)"
              >
                <span>{{ sub.permName }}</span>
              </div>
            </div>
          </transition>
        </div>
        <!-- 无子菜单的：平铺 -->
        <div
          v-else
          class="nav-item"
          :class="{ active: route.path === item.path || route.path.startsWith(item.path + '/') }"
          @click="navigate(item.path)"
        >
          <el-icon :size="18"><component :is="getIcon(item.permCode)" /></el-icon>
          <span>{{ item.permName }}</span>
        </div>
      </template>

      <div class="history-section" v-if="authStore.hasPermission('chat')">
        <div class="history-head">
          <span>历史对话</span>
        </div>
        <div class="history-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="history-item"
            :class="{ active: route.path === `/chat/${conv.id}` }"
            @click="openConversation(conv.id)"
            :title="conv.title"
          >
            <div class="history-main">
              <span class="history-title">{{ conv.title || '未命名对话' }}</span>
              <span v-if="conv.agentName" class="history-agent">{{ conv.agentName }}</span>
            </div>
            <button
              v-if="!authStore.roles.includes('super_admin')"
              class="history-delete"
              title="删除"
              @click.stop="deleteConversation(conv)"
            >×</button>
          </div>
          <div v-if="!loadingHistory && conversations.length === 0" class="history-empty">暂无历史</div>
        </div>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div class="user-info" v-if="authStore.isLoggedIn">
        <el-avatar :size="28" :src="authStore.avatar || ''">
          <el-icon><UserFilled /></el-icon>
        </el-avatar>
        <div class="user-meta">
          <span class="user-name">{{ authStore.username }}</span>
          <span class="user-role">{{ authStore.deptName || '未分配部门' }}</span>
        </div>
      </div>
      <button class="logout-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const expandedGroups = ref<string[]>([])
const conversations = ref<any[]>([])
const loadingHistory = ref(false)

const iconMap: Record<string, string> = {
  chat: 'ChatDotRound',
  agent_market: 'Shop',
  workflow: 'Connection',
  knowledge: 'Collection',
  admin_panel: 'Setting',
  'admin:menus': 'Menu',
  'admin:prompts': 'ChatLineRound',
  'admin:agents': 'Bot',
  'admin:users': 'User',
  'admin:roles': 'Key',
  'admin:depts': 'OfficeBuilding',
  'admin:stats': 'TrendCharts'
}

const getIcon = (code: string) => {
  return iconMap[code] || 'CircleCheck'
}

const isGroupActive = (item: any) => {
  if (!item.children) return false
  return item.children.some((sub: any) => route.path === sub.path || route.path.startsWith(sub.path + '/'))
}

const toggleGroup = (code: string) => {
  if (expandedGroups.value.includes(code)) {
    expandedGroups.value = expandedGroups.value.filter(c => c !== code)
  } else {
    expandedGroups.value.push(code)
  }
}

const navigate = (path: string) => {
  if (!path) return
  router.push(path)
}

const fetchConversations = async () => {
  if (!authStore.userId) return
  loadingHistory.value = true
  try {
    const res = await axios.get('/api/conversations/list')
    conversations.value = Array.isArray(res.data) ? res.data : []
  } catch {
    conversations.value = []
  } finally {
    loadingHistory.value = false
  }
}

const openConversation = (id: number) => {
  router.push(`/chat/${id}`)
}

const deleteConversation = (conv: any) => {
  ElMessageBox.confirm(`确定删除「${conv.title || '未命名对话'}」吗？`, '删除确认', { type: 'warning' }).then(async () => {
    try {
      const res = await axios.delete(`/api/conversations/${conv.id}`)
      if (res.data?.success === false) {
        ElMessage.error(res.data?.message || '删除失败')
        return
      }
      conversations.value = conversations.value.filter(item => item.id !== conv.id)
      if (route.path === `/chat/${conv.id}`) {
        router.push('/chat')
      }
      ElMessage.success('已删除')
    } catch {
      ElMessage.error('删除失败')
    }
  })
}

const onConversationSaved = () => fetchConversations()

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  })
}

onMounted(async () => {
  // 只要登录了就刷新用户信息，确保菜单是最新的
  if (authStore.token) {
    try {
      await authStore.fetchUserInfo()
    } catch {}
  }
  await fetchConversations()
  window.addEventListener('conversation-saved', onConversationSaved)
  // 自动展开当前所在的菜单组
  setTimeout(() => {
    for (const item of authStore.menus) {
      if (item.children && isGroupActive(item)) {
        if (!expandedGroups.value.includes(item.permCode)) {
          expandedGroups.value.push(item.permCode)
        }
      }
    }
  }, 100)
})

onUnmounted(() => {
  window.removeEventListener('conversation-saved', onConversationSaved)
})
</script>

<style scoped>
.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #E7E5E4;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid #F5F5F4;
}
.brand {
  font-size: 1rem;
  font-weight: 700;
  color: #1C1917;
}
.nav-menu {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.875rem;
  color: #44403C;
  transition: all 150ms;
  margin-bottom: 2px;
}
.nav-item:hover {
  background: #F5F5F4;
  color: #1C1917;
}
.nav-item.active {
  background: #EFF6FF;
  color: #2563EB;
  font-weight: 500;
}

/* 分组头部 */
.nav-group { margin-bottom: 2px; }
.group-header { position: relative; padding-right: 28px; }
.group-header .arrow {
  position: absolute;
  right: 10px;
  transition: transform 200ms;
  color: #A8A29E;
}
.group-header .arrow.expanded {
  transform: rotate(90deg);
}

/* 子菜单 */
.sub-menu { overflow: hidden; }
.sub-item {
  padding-left: 40px;
  font-size: 0.8125rem;
}

.history-section {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #F5F5F4;
}
.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 6px;
  font-size: 0.75rem;
  color: #A8A29E;
  font-weight: 600;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.history-item {
  position: relative;
  padding: 8px 34px 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  color: #44403C;
  display: flex;
  align-items: center;
  gap: 6px;
}
.history-item:hover { background: #F5F5F4; }
.history-item.active { background: #EFF6FF; color: #2563EB; }
.history-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}
.history-title {
  font-size: 0.8125rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-agent {
  font-size: 0.6875rem;
  color: #A8A29E;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-delete {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: #A8A29E;
  cursor: pointer;
  display: none;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  line-height: 1;
}
.history-item:hover .history-delete { display: flex; }
.history-delete:hover { background: #FEF2F2; color: #DC2626; }
.history-empty {
  padding: 8px 10px;
  font-size: 0.75rem;
  color: #A8A29E;
}

/* 动画 */
.slide-enter-active, .slide-leave-active {
  transition: all 200ms ease;
}
.slide-enter-from, .slide-leave-to {
  opacity: 0;
  max-height: 0;
}
.slide-enter-to, .slide-leave-from {
  opacity: 1;
  max-height: 300px;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #F5F5F4;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 4px;
}
.user-meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.user-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: #1C1917;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-role {
  font-size: 0.6875rem;
  color: #A8A29E;
}
.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  border: 1px solid #E7E5E4;
  background: #fff;
  border-radius: 8px;
  font-size: 0.8125rem;
  color: #78716C;
  cursor: pointer;
  transition: all 150ms;
}
.logout-btn:hover {
  border-color: #DC2626;
  color: #DC2626;
  background: #FEF2F2;
}
</style>
