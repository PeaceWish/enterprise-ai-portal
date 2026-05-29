<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>部门管理</h1>
        <p>管理部门信息（支持 OA 同步和手动创建）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增部门</el-button>
    </div>

    <el-table :data="deptList" v-loading="loading" border stripe>
      <el-table-column type="index" width="50" />
      <el-table-column prop="deptName" label="部门名称" width="200" />
      <el-table-column prop="deptCode" label="部门编码" width="150" />
      <el-table-column prop="deptLeader" label="负责人" width="150" />
      <el-table-column prop="source" label="来源" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.source === 'oa_sync'" type="success">OA同步</el-tag>
          <el-tag v-else>本地</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" disabled />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="部门名称">
          <el-input v-model="form.deptName" placeholder="部门名称" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.deptLeader" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { ElMessage } from 'element-plus'
import { Plus, Edit } from '@element-plus/icons-vue'

const loading = ref(false)
const deptList = ref<any[]>([])
const dialogVisible = ref(false)
const saving = ref(false)

const form = reactive<any>({ id: null, deptName: '', deptCode: '', deptLeader: '', sortOrder: 0, status: 1 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/depts')
    deptList.value = res.data.data || []
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { ...row })
  } else {
    Object.assign(form, { id: null, deptName: '', deptCode: '', deptLeader: '', sortOrder: 0, status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (form.id) {
      await axios.put(`/api/admin/depts/${form.id}`, form)
    } else {
      await axios.post('/api/admin/depts', form)
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

onMounted(fetchData)
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
</style>
