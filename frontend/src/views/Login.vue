<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="36" color="#2563EB"><Platform /></el-icon>
        </div>
        <h1>企业 AI 门户</h1>
        <p>Enterprise AI Portal</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
      </el-form>


    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { User, Lock, Platform } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const errorMsg = ref('')

const handleLogin = async () => {
  errorMsg.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    // 优先用 router 跳转，失败则强制刷新
    try {
      await router.push('/chat')
    } catch {
      window.location.href = '/chat'
    }
  } catch (err: any) {
    errorMsg.value = err?.message || '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #EFF6FF 0%, #F5F5F4 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.logo {
  width: 64px;
  height: 64px;
  background: #EFF6FF;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.login-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1C1917;
  margin: 0 0 4px;
}
.login-header p {
  font-size: 0.875rem;
  color: #A8A29E;
  margin: 0;
}
.login-form {
  margin-bottom: 16px;
}
.login-btn {
  width: 100%;
  font-weight: 500;
}
.login-footer {
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid #F5F5F4;
}
.login-footer p {
  font-size: 0.75rem;
  color: #A8A29E;
  margin: 0;
}
.error-tip {
  text-align: center;
  font-size: 0.8125rem;
  color: #DC2626;
  margin-top: -8px;
  margin-bottom: 8px;
}
</style>
