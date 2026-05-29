<template>
  <div class="admin-view">
    <div class="view-header">
      <div>
        <h1>菜单管理</h1>
        <p>管理系统菜单的名称、排序和显示状态</p>
      </div>
    </div>

    <el-table :data="menuList" v-loading="loading" border stripe row-key="id">
      <el-table-column type="index" width="50" />
      <el-table-column prop="permCode" label="权限编码" width="180" />
      <el-table-column prop="permName" label="菜单名称" width="150">
        <template #default="{ row }">
          <el-input v-if="row._editing" v-model="row._tempName" size="small" />
          <span v-else>{{ row.permName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="permType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.permType === 'menu'" type="success">菜单</el-tag>
          <el-tag v-else type="info">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="100">
        <template #default="{ row }">
          <el-input-number v-if="row._editing" v-model="row._tempSort" :min="0" size="small" style="width: 80px" />
          <span v-else>{{ row.sortOrder }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="显示" width="80">
        <template #default="{ row }">
          <el-switch
            v-if="row.permType === 'menu'"
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleToggleStatus(row)"
          />
          <span v-else class="na">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row._editing">
            <el-button size="small" type="primary" @click="handleSave(row)">保存</el-button>
            <el-button size="small" @click="row._editing = false">取消</el-button>
          </template>
          <template v-else>
            <el-button size="small" :icon="Edit" @click="startEdit(row)">编辑</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-alert type="info" :closable="false" style="margin-top: 16px">
      <p>提示：修改菜单后需要刷新页面才能看到效果。</p>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'

const loading = ref(false)
const menuList = ref<any[]>([])

const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/menus')
    const data = res.data.data || []
    // 扁平化展示
    menuList.value = flattenMenus(data)
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const flattenMenus = (items: any[], level = 0): any[] => {
  let result: any[] = []
  for (const item of items) {
    const node = { ...item, _level: level }
    result.push(node)
    if (item.children && item.children.length > 0) {
      result = result.concat(flattenMenus(item.children, level + 1))
    }
  }
  return result
}

const startEdit = (row: any) => {
  row._editing = true
  row._tempName = row.permName
  row._tempSort = row.sortOrder
}

const handleSave = async (row: any) => {
  try {
    await axios.put(`/api/admin/menus/${row.id}`, {
      permName: row._tempName,
      sortOrder: row._tempSort
    })
    row.permName = row._tempName
    row.sortOrder = row._tempSort
    row._editing = false
    ElMessage.success('保存成功')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  }
}

const handleToggleStatus = async (row: any) => {
  try {
    await axios.put(`/api/admin/menus/${row.id}/status`, null, {
      params: { status: row.status }
    })
    ElMessage.success(row.status === 1 ? '已显示' : '已隐藏')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '操作失败')
    // 回滚
    row.status = row.status === 1 ? 0 : 1
  }
}

onMounted(fetchMenus)
</script>

<style scoped>
.admin-view { width: 100%; padding: 32px 40px; overflow-y: auto; }
.view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.view-header h1 { font-size: 1.25rem; font-weight: 700; color: #1C1917; margin: 0 0 4px; }
.view-header p { font-size: 0.8125rem; color: #78716C; margin: 0; }
.na { color: #A8A29E; }
</style>
