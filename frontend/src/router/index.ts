import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/sso/callback',
    name: 'SsoCallback',
    component: () => import('../views/SsoCallback.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/chat',
    children: [
      {
        path: 'chat/:convId?',
        name: 'Chat',
        component: () => import('../components/NewChat.vue'),
        props: route => ({ convId: route.params.convId ? Number(route.params.convId) : null }),
        meta: { permission: 'chat', title: 'AI对话' }
      },
      { path: 'market', name: 'Market', component: () => import('../components/AgentMarketplace.vue'), meta: { permission: 'agent_market', title: '智能体广场' } },
      { path: 'workflow', name: 'Workflow', component: () => import('../components/WorkflowPlaceholder.vue'), meta: { permission: 'workflow', title: '业务工作流' } },
      { path: 'knowledge', name: 'Knowledge', component: () => import('../components/KnowledgePlaceholder.vue'), meta: { permission: 'knowledge', title: '知识库' } },
      { path: 'admin/agents', name: 'AdminAgents', component: () => import('../components/AdminPanel.vue'), meta: { permission: 'admin:agents', title: '智能体管理' } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/UserManage.vue'), meta: { permission: 'admin:users', title: '用户管理' } },
      { path: 'admin/roles', name: 'AdminRoles', component: () => import('../views/RoleManage.vue'), meta: { permission: 'admin:roles', title: '角色权限' } },
      { path: 'admin/depts', name: 'AdminDepts', component: () => import('../views/DeptManage.vue'), meta: { permission: 'admin:depts', title: '部门管理' } },
      { path: 'admin/stats', name: 'AdminStats', component: () => import('../views/StatsView.vue'), meta: { permission: 'admin:stats', title: '使用统计' } },
      { path: 'admin/menus', name: 'AdminMenus', component: () => import('../views/MenuManage.vue'), meta: { permission: 'admin:menus', title: '菜单管理' } },
      { path: 'admin/prompts', name: 'AdminPrompts', component: () => import('../components/PromptManage.vue'), meta: { permission: 'admin:prompts', title: '提示词配置' } },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!authStore.token) {
    next('/login')
    return
  }

  // 如果有 token 但权限未加载，先拉取用户信息
  if (authStore.token && authStore.permissions.length === 0) {
    try {
      await authStore.fetchUserInfo()
    } catch {
      authStore.logout()
      next('/login')
      return
    }
  }

  // 检查路由权限
  const requiredPerm = to.meta.permission as string
  if (requiredPerm && !authStore.hasPermission(requiredPerm)) {
    // 避免无限重定向：如果已经在默认页，跳登录页
    if (to.path === '/chat' || to.path === '/') {
      authStore.logout()
      next('/login')
    } else {
      next('/chat')
    }
    return
  }

  next()
})

export default router
