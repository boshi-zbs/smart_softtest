<template>
  <div>
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新增需求</el-button>

      <!-- 查询条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目">
          <el-select v-model="searchForm.projectId" placeholder="全部" clearable style="width: 150px;">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="需求标题">
          <el-input v-model="searchForm.title" placeholder="标题" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待处理" value="待处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="全部" clearable style="width: 120px;">
            <el-option label="最高" :value="1" />
            <el-option label="高" :value="2" />
            <el-option label="中" :value="3" />
            <el-option label="低" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="searchForm.assigneeId" placeholder="全部" clearable style="width: 150px;">
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="item.username + (item.realName ? ' (' + item.realName + ')' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="action-group">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 通用表格组件 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchRequirements"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="projectName" label="所属项目" />
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)">
              {{ priorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="120" />
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑需求对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId" required>
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width:100%">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority" required>
          <el-radio-group v-model="form.priority">
            <el-radio :value="1">最高</el-radio>
            <el-radio :value="2">高</el-radio>
            <el-radio :value="3">中</el-radio>
            <el-radio :value="4">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status" required>
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
            <el-option label="待处理" value="待处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="assigneeId">
          <el-select v-model="form.assigneeId" placeholder="请选择负责人" clearable style="width:100%">
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="item.username + (item.realName ? ' (' + item.realName + ')' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getRequirementList, createRequirement, updateRequirement, deleteRequirement, deleteRequirementsBatch } from '@/api/requirement'
import { getProjectList } from '@/api/project'
import { getUserList } from '@/api/user'

const tableRef = ref()
const selectedRows = ref([])

// 查询条件
const searchForm = ref({
  projectId: null,
  title: '',
  status: '',
  priority: null,
  assigneeId: null
})

// 项目选项
const projectOptions = ref([])
// 用户选项
const userOptions = ref([])

// 加载项目列表
const fetchProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('获取项目列表失败', error)
  }
}

// 加载用户列表
const fetchUsers = async () => {
  try {
    const res = await getUserList({ page: 1, size: 1000 })
    userOptions.value = res.data.records
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}

onMounted(() => {
  fetchProjects()
  fetchUsers()
})

// 获取需求列表（分页）
const fetchRequirements = async (params) => {
  const res = await getRequirementList(params)
  return res
}

// 查询
const handleSearch = () => {
  tableRef.value?.triggerSearch()
}

// 重置
const resetSearch = () => {
  searchForm.value = {
    projectId: null,
    title: '',
    status: '',
    priority: null,
    assigneeId: null
  }
  tableRef.value?.triggerSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 优先级显示辅助
const priorityText = (val) => {
  const map = { 1: '最高', 2: '高', 3: '中', 4: '低' }
  return map[val] || val
}
const priorityType = (val) => {
  const map = { 1: 'danger', 2: 'warning', 3: 'info', 4: '' }
  return map[val]
}
const statusType = (status) => {
  const map = { '待处理': 'info', '处理中': 'warning', '已完成': 'success', '已关闭': '' }
  return map[status]
}

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增需求')
const formRef = ref()
const saving = ref(false)

const form = ref({
  id: null,
  projectId: null,
  title: '',
  description: '',
  priority: 2,    // 默认高
  status: '待处理',
  assigneeId: null
})

const rules = {
  projectId: [
    { required: true, message: '请选择项目', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const handleAdd = () => {
  dialogTitle.value = '新增需求'
  form.value = {
    id: null,
    projectId: null,
    title: '',
    description: '',
    priority: 2,
    status: '待处理',
    assigneeId: null
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑需求'
  form.value = {
    id: row.id,
    projectId: row.projectId,
    title: row.title,
    description: row.description,
    priority: row.priority,
    status: row.status,
    assigneeId: row.assigneeId
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleClose = () => {
  formRef.value?.resetFields()
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch (error) {
    console.log('表单校验未通过')
    return
  }

  saving.value = true
  try {
    if (form.value.id) {
      await updateRequirement(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createRequirement(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存需求失败', error)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除需求 ${row.title} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRequirement(row.id)
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('删除需求失败', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个需求吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRequirementsBatch(ids)
      ElMessage.success('批量删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}
</script>

<style scoped>
.action-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.search-form {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
}
.action-group {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
:deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}
</style>