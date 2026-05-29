<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>角色权限</h1>
        <p>配置系统角色及对应权限</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
    </div>

    <el-table :data="roleList" v-loading="loading" border stripe>
      <el-table-column type="index" width="50" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="roleType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.roleType === 'system'" type="danger">系统</el-tag>
          <el-tag v-else>自定义</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" disabled />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openDialog(row)">编辑权限</el-button>
          <el-button size="small" type="success" :icon="UserFilled" @click="openUserDialog(row)">分配用户</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分配用户弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="`给角色「${currentRole?.roleName || ''}」分配用户`" width="760px">
      <div class="assign-toolbar">
        <el-input v-model="userKeyword" placeholder="搜索用户名/姓名/工号" clearable style="width: 320px" @keyup.enter="fetchUsers">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button :icon="Search" @click="fetchUsers">搜索</el-button>
        <el-button @click="userKeyword = ''; fetchUsers()">重置</el-button>
      </div>
      <el-transfer
        v-model="selectedUserIds"
        filterable
        :filter-placeholder="'输入用户名/姓名/工号过滤'"
        :data="userOptions"
        :titles="['可选用户', '已分配用户']"
        :props="{ key: 'key', label: 'label', disabled: 'disabled' }"
        style="width: 100%;"
      />
      <p class="assign-tip">禁用用户会标记为“已禁用”，可保留已有授权；如需禁止登录，请以用户状态为准。</p>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingUsers" @click="handleSaveUsers">保存分配</el-button>
      </template>
    </el-dialog>

    <!-- 角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色编码" v-if="!form.id">
          <el-input v-model="form.roleCode" placeholder="唯一编码，如: developer" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" placeholder="角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="权限配置">
          <el-tree
            ref="permTreeRef"
            :data="permTree"
            show-checkbox
            node-key="id"
            :default-checked-keys="checkedPerms"
            :props="{ label: 'permName', children: 'children' }"
            style="max-height: 400px; overflow-y: auto;"
          />
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
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, UserFilled, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const roleList = ref<any[]>([])
const permTree = ref<any[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const checkedPerms = ref<number[]>([])
const permTreeRef = ref()
const userDialogVisible = ref(false)
const savingUsers = ref(false)
const currentRole = ref<any>(null)
const userKeyword = ref('')
const userOptions = ref<any[]>([])
const selectedUserIds = ref<number[]>([])

const form = ref<any>({ id: null, roleCode: '', roleName: '', description: '' })

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/roles')
    roleList.value = res.data.data || []
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const fetchPerms = async () => {
  try {
    const res = await axios.get('/api/admin/roles/permissions/tree')
    permTree.value = buildTree(res.data.data || [])
  } catch {}
}

const buildTree = (items: any[], parentId = 0): any[] => {
  return items
    .filter((item: any) => item.parentId === parentId)
    .map((item: any) => ({
      ...item,
      children: buildTree(items, item.id)
    }))
}

const fetchUsers = async () => {
  try {
    const res = await axios.get('/api/admin/users', {
      params: { page: 1, size: 10000, keyword: userKeyword.value }
    })
    const records = res.data.data?.records || []
    userOptions.value = records.map((u: any) => ({
      key: u.id,
      label: `${u.realName || u.username}（${u.username || '-'} / ${u.userCode || '-'}）${u.status === 0 ? ' - 已禁用' : ''}`,
      disabled: false
    }))
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载用户失败')
  }
}

const openUserDialog = async (row: any) => {
  currentRole.value = row
  userKeyword.value = ''
  selectedUserIds.value = []
  userDialogVisible.value = true
  try {
    await fetchUsers()
    const res = await axios.get(`/api/admin/roles/${row.id}/users`)
    selectedUserIds.value = res.data.data || []
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载角色用户失败')
  }
}

const handleSaveUsers = async () => {
  if (!currentRole.value?.id) return
  savingUsers.value = true
  try {
    await axios.put(`/api/admin/roles/${currentRole.value.id}/users`, selectedUserIds.value)
    ElMessage.success('分配成功')
    userDialogVisible.value = false
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '分配失败')
  } finally {
    savingUsers.value = false
  }
}

const openDialog = async (row?: any) => {
  if (row) {
    form.value = { ...row }
    const res = await axios.get(`/api/admin/roles/${row.id}/permissions`)
    checkedPerms.value = res.data.data || []
  } else {
    form.value = { id: null, roleCode: '', roleName: '', description: '' }
    checkedPerms.value = []
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    const keys = permTreeRef.value?.getCheckedKeys() || []
    const halfKeys = permTreeRef.value?.getHalfCheckedKeys() || []
    const allKeys = [...keys, ...halfKeys]

    if (form.value.id) {
      await axios.put(`/api/admin/roles/${form.value.id}`, form.value, { params: { permissionIds: allKeys } })
    } else {
      await axios.post('/api/admin/roles', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRoles()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '确认删除', { type: 'warning' }).then(async () => {
    try {
      await axios.delete(`/api/admin/roles/${row.id}`)
      ElMessage.success('删除成功')
      fetchRoles()
    } catch (err: any) {
      ElMessage.error(err.response?.data?.message || '删除失败')
    }
  })
}

onMounted(() => {
  fetchRoles()
  fetchPerms()
})
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
.assign-toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
.assign-tip { margin: 12px 0 0; font-size: 12px; color: #909399; }
:deep(.el-transfer-panel) { width: 300px; }
</style>
