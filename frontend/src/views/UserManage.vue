<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>用户管理</h1>
        <p>管理系统用户，支持本地创建和OA同步</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增用户</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/姓名/工号" clearable style="width: 300px">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" width="50" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="userCode" label="工号" width="120" />
      <el-table-column prop="deptName" label="部门" width="150">
        <template #default="{ row }">
          <span>{{ row.deptName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'super_admin'" type="danger">超级管理员</el-tag>
          <el-tag v-else-if="row.role === 'admin'" type="warning">管理员</el-tag>
          <el-tag v-else type="info">普通用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.source === 'oa_sync'" type="success">OA同步</el-tag>
          <el-tag v-else-if="row.source === 'system'" type="danger">系统</el-tag>
          <el-tag v-else>本地</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @change="fetchData"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="工号" prop="userCode">
          <el-input v-model="form.userCode" placeholder="OA工号" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" clearable style="width: 100%">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const deptList = ref<any[]>([])
const roleList = ref<any[]>([])

const form = reactive<any>({
  id: null,
  username: '',
  password: '',
  realName: '',
  userCode: '',
  deptId: null,
  roleIds: [],
  phone: '',
  email: '',
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/users', {
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    const data = res.data.data
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const fetchDepts = async () => {
  try {
    const res = await axios.get('/api/admin/depts')
    deptList.value = res.data.data || []
  } catch {}
}

const fetchRoles = async () => {
  try {
    const res = await axios.get('/api/admin/roles')
    roleList.value = res.data.data || []
  } catch {}
}

const openDialog = async (row?: any) => {
  // 打开弹窗时刷新部门列表，避免缓存导致新添加的部门看不到
  await fetchDepts()
  await fetchRoles()

  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      realName: row.realName,
      userCode: row.userCode,
      deptId: row.deptId,
      roleIds: row.roleIds || [],
      phone: row.phone,
      email: row.email,
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null, username: '', password: '', realName: '', userCode: '',
      deptId: null, roleIds: [], phone: '', email: '', status: 1
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await axios.put(`/api/admin/users/${form.id}`, form, { params: { roleIds: form.roleIds } })
    } else {
      await axios.post('/api/admin/users', form, { params: { roleIds: form.roleIds } })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '确认删除', { type: 'warning' }).then(async () => {
    try {
      await axios.delete(`/api/admin/users/${row.id}`)
      ElMessage.success('删除成功')
      fetchData()
    } catch (err: any) {
      ElMessage.error(err.response?.data?.message || '删除失败')
    }
  })
}

onMounted(() => {
  fetchData()
  fetchDepts()
  fetchRoles()
})
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
