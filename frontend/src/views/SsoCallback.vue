<template>
  <div class="sso-callback">
    <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
    <p class="status-text">{{ statusText }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const statusText = ref('正在验证单点登录...')

const getRawQueryParam = (search: string, name: string): string => {
  const query = search.startsWith('?') ? search.slice(1) : search
  const pairs = query.split('&')
  const prefix = `${name}=`

  for (const pair of pairs) {
    if (pair === name) {
      return ''
    }
    if (pair.startsWith(prefix)) {
      return pair.slice(prefix.length)
    }
  }

  return ''
}

onMounted(async () => {
  const ticket = getRawQueryParam(window.location.search, 'ticket')

  if (!ticket) {
    statusText.value = '登录参数缺失，正在跳转...'
    setTimeout(() => router.replace('/login'), 1500)
    return
  }

  try {
    const res = await axios.get('/api/auth/sso/callback', {
      params: { ticket }
    })

    const data = res.data.data
    if (!data || !data.token) {
      throw new Error('登录响应异常')
    }

    authStore.setToken(data.token)
    authStore.setUserInfo(data)

    statusText.value = '登录成功，正在进入...'
    setTimeout(() => router.replace('/dashboard'), 500)

  } catch (err: any) {
    const msg = err.response?.data?.message || '单点登录失败'
    statusText.value = msg + '，正在跳转...'
    ElMessage.error(msg)
    setTimeout(() => router.replace('/login'), 2000)
  }
})
</script>

<style scoped>
.sso-callback {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #FAFAF9;
}
.loading-icon {
  color: #2563EB;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.status-text {
  margin-top: 20px;
  font-size: 0.9375rem;
  color: #44403C;
}
</style>
