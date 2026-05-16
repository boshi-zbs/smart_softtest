<template>
  <div>
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新增计划</el-button>

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
        <el-form-item label="计划名称">
          <el-input v-model="searchForm.planName" placeholder="名称" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
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

    <!-- 表格 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchTestPlans"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="planName" label="计划名称" />
        <el-table-column prop="projectName" label="所属项目" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始日期" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column label="结束日期" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleManageCases(row)">关联用例</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.progress || 0)" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑计划对话框 -->
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
        <el-form-item label="计划名称" prop="planName" required>
          <el-input v-model="form.planName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="datetime"
            placeholder="选择开始日期时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="datetime"
            placeholder="选择结束日期时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status" required>
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
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

    <!-- 关联用例对话框 -->
    <el-dialog title="关联用例" v-model="caseDialogVisible" width="700px" @close="handleCaseDialogClose">
      <div style="margin-bottom: 16px;">
        <span>当前计划：{{ currentPlanName }}</span>
        <el-button style="margin-left: 20px;" type="primary" @click="handleAddCase">添加用例</el-button>
      </div>
      <el-table :data="planCaseList" border style="width: 100%">
        <el-table-column prop="id" label="用例ID" width="80" />
        <el-table-column prop="title" label="用例标题" />
        <el-table-column prop="priorityText" label="优先级" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleRemoveCase(row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 添加用例的选择框（在添加时弹出） -->
      <el-dialog title="选择用例" v-model="selectCaseDialogVisible" width="500px" append-to-body>
        <el-select v-model="selectedCaseId" placeholder="请选择用例" style="width:100%">
          <el-option
            v-for="item in availableCases"
            :key="item.id"
            :label="item.title"
            :value="item.id"
          />
        </el-select>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="selectCaseDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmAddCase">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getTestPlanList, createTestPlan, updateTestPlan, deleteTestPlan, deleteTestPlansBatch,
         addCaseToPlan, removeCaseFromPlan, getPlanCases } from '@/api/testplan'
import { getProjectList } from '@/api/project'
import { getTestCaseList } from '@/api/testcase' // 假设已有

const tableRef = ref()
const selectedRows = ref([])

// 在已有变量后添加
const currentPlan = ref({})          // 存储当前计划详情


// 查询条件
const searchForm = ref({
  projectId: null,
  planName: '',
  status: ''
})

// 项目选项
const projectOptions = ref([])
// 加载项目列表
const fetchProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('获取项目列表失败', error)
  }
}

onMounted(() => {
  fetchProjects()
})

// 状态标签样式
const statusType = (status) => {
  const map = { '未开始': 'info', '进行中': 'warning', '已完成': 'success', '已取消': '' }
  return map[status] || ''
}

// 获取计划列表
const fetchTestPlans = async (params) => {
  const res = await getTestPlanList(params)
  return res
}

// 查询
const handleSearch = () => {
  tableRef.value?.triggerSearch()
}
const resetSearch = () => {
  searchForm.value = {
    projectId: null,
    planName: '',
    status: ''
  }
  tableRef.value?.triggerSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 新增/编辑计划对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增计划')
const formRef = ref()
const saving = ref(false)
const form = ref({
  id: null,
  projectId: null,
  planName: '',
  description: '',
  startDate: null,
  endDate: null,
  status: '未开始'
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  planName: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const handleAdd = () => {
  dialogTitle.value = '新增计划'
  form.value = {
    id: null,
    projectId: null,
    planName: '',
    description: '',
    startDate: null,
    endDate: null,
    status: '未开始'
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = (row) => {
   console.log('编辑行原始数据:', row);
  console.log('startDate:', row.startDate);
  console.log('endDate:', row.endDate);
  dialogTitle.value = '编辑计划'
  form.value = {
    id: row.id,
    projectId: row.projectId,
    planName: row.planName,
    description: row.description,
    startDate: row.startDate,
    endDate: row.endDate,
    status: row.status
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleClose = () => {
  formRef.value?.resetFields()
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateTestPlan(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createTestPlan(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除计划 ${row.planName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTestPlan(row.id)
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('删除失败', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个计划吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTestPlansBatch(ids)
      ElMessage.success('批量删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}

// 关联用例相关
const caseDialogVisible = ref(false)
const currentPlanId = ref(null)
const currentPlanName = ref('')
const planCaseList = ref([])

const handleManageCases = (row) => {
  currentPlan.value = row              // 保存整个计划对象（包含 projectId）
  currentPlanId.value = row.id
  currentPlanName.value = row.planName
  loadPlanCases(row.id)
  caseDialogVisible.value = true
}

// 加载计划已关联的用例
const loadPlanCases = async (planId) => {
  try {
    const res = await getPlanCases(planId)  // 返回 { code:200, data: [ { id, title, priority, ... } ] }
    planCaseList.value = res.data.map(c => ({
      ...c,
      priorityText: {1:'最高',2:'高',3:'中',4:'低'}[c.priority] || '中'
    }))
  } catch (error) {
    console.error('加载关联用例失败', error)
  }
}

// 添加用例
const selectCaseDialogVisible = ref(false)
const availableCases = ref([]) // 可用的用例列表（同一项目下的）
const selectedCaseId = ref(null)

const handleAddCase = async () => {
  if (!currentPlan.value.projectId) {
    ElMessage.error('无法获取项目ID，请重新打开')
    return
  }
  try {
    // 获取当前项目下的所有用例（假设已有 getTestCaseList 接口）
    const res = await getTestCaseList({ 
      projectId: currentPlan.value.projectId, 
      page: 1, 
      size: 1000 
    })
    // 已关联用例的ID列表
    const associatedIds = planCaseList.value.map(c => c.id)
    // 过滤出未关联的用例
    availableCases.value = res.data.records.filter(c => !associatedIds.includes(c.id))
    selectCaseDialogVisible.value = true
  } catch (error) {
    console.error('获取用例列表失败', error)
  }
}
const confirmAddCase = async () => {
  if (!selectedCaseId.value) {
    ElMessage.warning('请选择用例')
    return
  }
  try {
    await addCaseToPlan(currentPlanId.value, selectedCaseId.value)
    ElMessage.success('添加成功')
    selectCaseDialogVisible.value = false
    selectedCaseId.value = null
    // 重新加载关联用例列表
    loadPlanCases(currentPlanId.value)
  } catch (error) {
    // 错误已在拦截器中显示，无需额外处理
    console.error('添加失败', error)
  }
}

const handleRemoveCase = async (caseId) => {
  try {
    await removeCaseFromPlan(currentPlanId.value, caseId)
    ElMessage.success('移除成功')
    loadPlanCases(currentPlanId.value)
  } catch (error) {
    console.error('移除失败', error)
  }
}

const handleCaseDialogClose = () => {
  caseDialogVisible.value = false
}

// 格式化日期时间，去掉秒
const formatDateTime = (datetime) => {
  if (!datetime) return ''
  // 如果字符串长度足够，截取前16位 (YYYY-MM-DD HH:mm)
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
  margin-bottom: 0;
}
</style>