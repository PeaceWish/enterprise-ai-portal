import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export interface MenuItem {
  id: number
  permCode: string
  permName: string
  permType: string
  parentId: number
  path: string
  icon?: string
  sortOrder: number
  children?: MenuItem[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref<number | null>(null)
  const username = ref('')
  const realName = ref('')
  const deptName = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<MenuItem[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => roles.value.includes('super_admin') || roles.value.includes('admin'))

  function hasPermission(perm: string): boolean {
    if (roles.value.includes('super_admin')) return true
    return permissions.value.includes(perm)
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  async function login(usernameVal: string, passwordVal: string) {
    const res = await axios.post('/api/auth/login', { username: usernameVal, password: passwordVal })
    const data = res.data.data
    setToken(data.token)
    setUserInfo(data)
    return data
  }

  async function fetchUserInfo() {
    const res = await axios.get('/api/auth/user-info')
    const data = res.data.data
    setUserInfo(data)
    return data
  }

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
    axios.defaults.headers.common['Authorization'] = `Bearer ${val}`
  }

  function setUserInfo(data: any) {
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName || data.username
    deptName.value = data.deptName || ''
    avatar.value = data.avatar || ''
    roles.value = data.roles || []
    menus.value = data.menus || []
    // 使用后端返回的完整权限列表（包含菜单 + 按钮权限）
    permissions.value = data.permissions || []
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    deptName.value = ''
    avatar.value = ''
    roles.value = []
    permissions.value = []
    menus.value = []
    localStorage.removeItem('token')
    delete axios.defaults.headers.common['Authorization']
    // 强制刷新页面，彻底清除所有组件缓存状态
    window.location.reload()
  }

  // 初始化时如果有 token，设置到 axios
  if (token.value) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
  }

  return {
    token, userId, username, realName, deptName, avatar, roles, permissions, menus,
    isLoggedIn, isAdmin,
    hasPermission, hasRole,
    login, fetchUserInfo, logout, setToken, setUserInfo
  }
})
