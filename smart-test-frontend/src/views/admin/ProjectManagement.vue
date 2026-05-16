<template>
  <div>
    <!-- 操作按钮行 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新增项目</el-button>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目名称">
          <el-input v-model="searchForm.projectName" placeholder="项目名称" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="进行中" :value="1" />
            <el-option label="已归档" :value="0" />
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
      :fetchData="fetchProjects"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="projectName" label="项目名称" />
        <el-table-column prop="projectKey" label="项目标识" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="开始日期" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column label="结束日期" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '进行中' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createUserName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="400">  <!-- 增加宽度以容纳新按钮 -->
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleManageMembers(row)">成员管理</el-button>
            <el-button size="small" type="primary" @click="handleModules(row)">模块管理</el-button>
            <el-button size="small" type="info" @click="handleGitConfig(row)">Git配置</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑项目对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="projectName" required>
          <el-input v-model="form.projectName" />
        </el-form-item>
        <el-form-item label="项目标识" prop="projectKey" required>
          <el-input v-model="form.projectKey" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="datetime" placeholder="选择开始日期时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" style="width:100%"/>
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="datetime" placeholder="选择结束日期时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" style="width:100%"/>
        </el-form-item>
        <el-form-item label="状态" prop="status" required>
          <el-radio-group v-model="form.status">
            <el-radio :value="1">进行中</el-radio>
            <el-radio :value="0">已归档</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Git仓库配置对话框 -->
    <el-dialog title="Git仓库配置" v-model="gitDialogVisible" width="500px">
      <el-form ref="gitFormRef" :model="gitForm" :rules="gitRules" label-width="100px">
        <el-form-item label="仓库地址" prop="repoUrl" required>
          <el-input v-model="gitForm.repoUrl" placeholder="https://github.com/user/repo.git" />
        </el-form-item>
        <el-form-item label="仓库类型" prop="repoType">
          <el-radio-group v-model="gitForm.repoType">
            <el-radio value="public">公开仓库</el-radio>
            <el-radio value="private">私有仓库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Access Token" prop="accessToken" v-if="gitForm.repoType === 'private'">
          <el-input v-model="gitForm.accessToken" type="password" placeholder="GitHub Personal Access Token" />
          <div class="form-tip">
            <a href="https://github.com/settings/tokens" target="_blank">如何获取Token？</a>
          </div>
        </el-form-item>
        <el-form-item label="默认分支" prop="defaultBranch">
          <el-input v-model="gitForm.defaultBranch" placeholder="main" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gitDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleSyncCode(currentProject)" :loading="syncLoading[currentProject?.id]">
          同步代码
        </el-button>
        <el-button type="primary" @click="saveGitConfig" :loading="savingGit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getProjectList, createProject, updateProject, deleteProject, deleteProjectsBatch } from '@/api/project'
import { useRouter } from 'vue-router'
import { getGitConfig, saveGitConfig as saveGitConfigApi, syncRepository } from '@/api/aiAnalysis'

const router = useRouter()

// Git配置相关
const gitDialogVisible = ref(false)
const currentProject = ref(null)
const gitFormRef = ref()
const savingGit = ref(false)
const gitForm = ref({
  repoUrl: '',
  repoType: 'public',
  accessToken: '',
  defaultBranch: 'main'
})
const gitRules = {
  repoUrl: [{ required: true, message: '请输入仓库地址', trigger: 'blur' }]
}

// 同步代码的loading状态（按项目ID）
const syncLoading = ref({})

// 打开Git配置对话框
const handleGitConfig = async (row) => {
  currentProject.value = row
  gitDialogVisible.value = true
  try {
    const res = await getGitConfig(row.id)
    const config = res.data
    // 稳健合并：保证所有字段都有值，避免丢失
    gitForm.value = {
      repoUrl: config?.repoUrl || '',
      repoType: config?.repoType || 'public',
      accessToken: config?.accessToken || '',  // 若后端不返回token，留空是合理的
      defaultBranch: config?.defaultBranch || 'main'
    }
  } catch (error) {
    console.error('获取Git配置失败', error)
    // 如果接口报错，保留默认空表单，不清空已有字段
    gitForm.value = {
      repoUrl: '',
      repoType: 'public',
      accessToken: '',
      defaultBranch: 'main'
    }
  }
}

// 保存Git配置
const saveGitConfig = async () => {
  await gitFormRef.value.validate()
  savingGit.value = true
  try {
    const data = { ...gitForm.value, projectId: currentProject.value.id }
    await saveGitConfigApi(data)
    ElMessage.success('保存成功')
    gitDialogVisible.value = false
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  } finally {
    savingGit.value = false
  }
}

// 同步代码（供操作列和Git对话框共用）
const handleSyncCode = async (row) => {
  if (!row || !row.id) return
  const projectId = row.id
  // 设置loading
  syncLoading.value[projectId] = true
  try {
    await syncRepository(projectId)
    ElMessage.success('代码同步成功')
  } catch (error) {
    console.error('同步失败', error)
    ElMessage.error('同步失败：' + (error.message || '请检查Git配置'))
  } finally {
    syncLoading.value[projectId] = false
  }
}

const tableRef = ref()
const selectedRows = ref([])

const searchForm = ref({
  projectName: '',
  status: null
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增项目')
const formRef = ref()
const saving = ref(false)

const form = ref({
  id: null,
  projectName: '',
  projectKey: '',
  description: '',
  startDate: null,
  endDate: null,
  status: 1
})

const handleModules = (row) => {
  router.push({ path: '/admin/projects/modules', query: { projectId: row.id } })
}

const rules = {
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  projectKey: [
    { required: true, message: '请输入项目标识', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '只能包含字母、数字、下划线和连字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const fetchProjects = async (params) => {
  const res = await getProjectList(params)
  return res
}

const handleSearch = () => {
  tableRef.value?.triggerSearch()
}

const resetSearch = () => {
  searchForm.value = {
    projectName: '',
    status: null
  }
  tableRef.value?.triggerSearch()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleAdd = () => {
  dialogTitle.value = '新增项目'
  form.value = {
    id: null,
    projectName: '',
    projectKey: '',
    description: '',
    startDate: null,
    endDate: null,
    status: 1
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑项目'
  form.value = {
    id: row.id,
    projectName: row.projectName,
    projectKey: row.projectKey,
    description: row.description,
    startDate: row.startDate,
    endDate: row.endDate,
    status: row.status
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
      await updateProject(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createProject(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存项目失败', error)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除项目 ${row.projectName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProject(row.id)
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('删除项目失败', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个项目吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProjectsBatch(ids)
      ElMessage.success('批量删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}

const handleManageMembers = (row) => {
  router.push({ path: '/admin/projects/members', query: { projectId: row.id } })
}

// 格式化日期时间，去掉秒，显示为 YYYY-MM-DD HH:mm
const formatDateTime = (datetime) => {
  if (!datetime) return ''
  if (datetime.length >= 16) {
    return datetime.substring(0, 16)
  }
  return datetime
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
  margin-bottom: 0px;
}
</style>