<template>
  <div>
    <!-- 操作栏 + 查询表单 -->
    <div class="action-bar">
      <el-button @click="goBack">返回</el-button>
      <el-button type="primary" @click="handleAdd">添加成员</el-button>

      <!-- 查询条件：项目角色下拉框 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
       <el-form-item label="项目角色">
        <el-select v-model="searchForm.roleInProject" placeholder="请选择角色" clearable style="width:150px">
          <el-option label="测试人员" value="测试人员" />
          <el-option label="开发人员" value="开发人员" />
        </el-select>
      </el-form-item>
      </el-form>

      <!-- 查询/重置按钮组 -->
      <div class="action-group">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
        批量移除
      </el-button>
      <span style="margin-left: auto;">当前项目：{{ projectName }}</span>
    </div>

    <!-- 通用表格组件 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchMembers"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="userName" label="用户名" />
        <el-table-column prop="userRealName" label="真实姓名" />
        <el-table-column prop="roleInProject" label="项目角色" width="120" />
        <el-table-column prop="joinTime" label="加入时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑角色</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">移除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 添加/编辑成员对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目" prop="projectId" v-if="!projectIdFixed">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width:100%">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="请选择用户"
            style="width:100%"
            @change="handleUserChange"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="item.username + (item.realName ? ' (' + item.realName + ')' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目角色" prop="roleInProject">
        <el-select v-model="form.roleInProject" placeholder="请选择角色" style="width:100%">
          <el-option label="测试人员" value="测试人员" />
          <el-option label="开发人员" value="开发人员" />
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getMemberList, addMember, updateMember, deleteMember, deleteMembersBatch } from '@/api/member'
import { getProjectList } from '@/api/project'
import { getUserList } from '@/api/user'

const route = useRoute()
const router = useRouter()
const tableRef = ref()
const selectedRows = ref([])
const isEdit = ref(false)
// 如果路由中带有 projectId，则固定该项目
const projectIdFixed = route.query.projectId ? parseInt(route.query.projectId) : null
const projectName = ref('')

// 查询条件
const searchForm = ref({
  projectId: projectIdFixed,
  roleInProject: ''
})



// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加成员')
const formRef = ref()
const saving = ref(false)

const form = ref({
  projectId: projectIdFixed,
  userId: null,
  roleInProject: ''
})

// 项目选项（用于添加时选择）
const projectOptions = ref([])
// 用户选项（包含角色信息）
const userOptions = ref([])

const rules = {
  projectId: [
    { required: true, message: '请选择项目', trigger: 'change' }
  ],
  userId: [
    { required: true, message: '请选择用户', trigger: 'change' }
  ],
  roleInProject: [
    { required: true, message: '请选择项目角色', trigger: 'change' }
  ]
}

// 获取当前项目名称
const fetchProjectName = async () => {
  if (!projectIdFixed) return
  try {
    const res = await getProjectList({ page: 1, size: 100 })
    const project = res.data.records.find(p => p.id === projectIdFixed)
    if (project) {
      projectName.value = project.projectName
    }
  } catch (error) {
    console.error('获取项目名称失败', error)
  }
}

// 获取所有项目（用于下拉框）
const fetchProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('获取项目列表失败', error)
  }
}

// 获取所有用户（包含角色信息）
const fetchUsers = async () => {
  try {
    const res = await getUserList({ page: 1, size: 1000 })
    userOptions.value = res.data.records
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}

onMounted(() => {
  fetchProjectName()
  fetchProjects()
  fetchUsers()
})

const fetchMembers = async (params) => {
  if (projectIdFixed) {
    params.projectId = projectIdFixed
  }
  const res = await getMemberList(params)
  return res
}

// 查询
const handleSearch = () => {
  tableRef.value?.triggerSearch()
}

// 重置查询条件
const resetSearch = () => {
  searchForm.value = {
    projectId: projectIdFixed,
    roleInProject: ''
  }
  tableRef.value?.triggerSearch()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 用户选择变化：自动填充项目角色（基于用户的系统角色）
const handleUserChange = (userId) => {
  const selectedUser = userOptions.value.find(u => u.id === userId)
  if (!selectedUser || !selectedUser.roles || selectedUser.roles.length === 0) {
    form.value.roleInProject = ''
    return
  }
  // 取第一个角色的角色名称（如 "管理员"、"测试人员"、"开发人员"）
  const roleName = selectedUser.roles[0].roleName
  form.value.roleInProject = roleName
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加成员'
  form.value = {
    projectId: projectIdFixed,
    userId: null,
    roleInProject: ''
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑成员角色'
  form.value = {
    projectId: row.projectId,
    userId: row.userId,
    roleInProject: row.roleInProject
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
    if (isEdit.value) {
      // 编辑
      await updateMember({
        projectId: form.value.projectId,
        userId: form.value.userId,
        roleInProject: form.value.roleInProject
      })
      ElMessage.success('修改成功')
    } else {
      // 新增
      await addMember(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存成员失败', error)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认移除成员 ${row.userName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMember(row.projectId, row.userId)
      ElMessage.success('移除成功')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('移除成员失败', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  // 构建成员对象数组，每个成员包含 projectId 和 userId
  const members = selectedRows.value.map(row => ({
    projectId: row.projectId,
    userId: row.userId
  }))
  ElMessageBox.confirm(`确认移除选中的 ${members.length} 个成员吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMembersBatch(members)
      ElMessage.success('批量移除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量移除失败', error)
    }
  }).catch(() => {})
}
// 返回项目管理列表
const goBack = () => {
  router.push('/admin/projects')
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
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
}
.action-group {
  display: flex;
  gap: 8px;
}
:deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}
</style>