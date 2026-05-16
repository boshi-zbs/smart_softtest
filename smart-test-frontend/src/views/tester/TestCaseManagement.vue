<template>
  <div>
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="success" @click="openAIDialog">AI生成用例</el-button>
      <el-button type="primary" @click="handleAdd">新增用例</el-button>
      <el-button type="success" @click="handleCopy" :disabled="selectedRows.length !== 1">复制用例</el-button>

      <!-- 计划选择 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="测试计划">
          <el-select v-model="selectedPlanId" placeholder="请选择计划" style="width: 250px;" @change="handlePlanChange">
            <el-option
              v-for="item in planOptions"
              :key="item.id"
              :label="item.planName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 查询条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="标题" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 120px;">
            <el-option label="功能测试" value="功能测试" />
            <el-option label="性能测试" value="性能测试" />
            <el-option label="安全测试" value="安全测试" />
            <el-option label="兼容性测试" value="兼容性测试" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="有效" value="有效" />
            <el-option label="无效" value="无效" />
            <el-option label="废弃" value="废弃" />
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
      </el-form>

      <div class="action-group">
        <el-button type="primary" @click="handleSearch" :disabled="!selectedPlanId">查询</el-button>
        <el-button @click="resetSearch" :disabled="!selectedPlanId">重置</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchTestCases"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="moduleName" label="关联模块" width="150" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="priorityText" label="优先级" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '有效' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastResult" label="上次执行结果" width="120">
          <template #default="{ row }">
            <el-tag :type="resultType(row.lastResult)">{{ row.lastResult || '未执行' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requirementTitle" label="关联需求" show-overflow-tooltip />
        <el-table-column label="关联计划" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="name in row.planNames" :key="name" size="small" style="margin-right: 4px;">{{ name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleCopyFrom(row)">复制</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑用例对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="700px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="关联模块" prop="moduleId">
          <el-select v-model="form.moduleId" placeholder="可选" clearable style="width:100%">
            <el-option
              v-for="item in moduleOptions"
              :key="item.id"
              :label="item.moduleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="前置条件" prop="precondition">
          <el-input v-model="form.precondition" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="测试步骤" prop="steps" required>
          <el-input v-model="form.steps" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="预期结果" prop="expectedResult" required>
          <el-input v-model="form.expectedResult" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="类型" prop="type" required>
          <el-select v-model="form.type" placeholder="请选择" style="width:100%">
            <el-option label="功能测试" value="功能测试" />
            <el-option label="性能测试" value="性能测试" />
            <el-option label="安全测试" value="安全测试" />
            <el-option label="兼容性测试" value="兼容性测试" />
          </el-select>
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
          <el-select v-model="form.status" placeholder="请选择" style="width:100%">
            <el-option label="有效" value="有效" />
            <el-option label="无效" value="无效" />
            <el-option label="废弃" value="废弃" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联需求" prop="requirementId">
          <el-select v-model="form.requirementId" placeholder="可选" clearable style="width:100%">
            <el-option
              v-for="item in filteredRequirements"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <!-- 新增时显示计划选择（必须） -->
        <el-form-item v-if="!form.id" label="所属计划" prop="planId" required>
          <el-select v-model="form.planId" placeholder="请选择计划" style="width:100%">
            <el-option
              v-for="item in planOptions"
              :key="item.id"
              :label="item.planName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <!-- 编辑时显示当前计划名称（不可改） -->
        <el-form-item v-else label="所属计划">
          <el-input :value="currentPlanName" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 复制用例对话框（选择目标计划） -->
    <el-dialog title="复制用例" v-model="copyDialogVisible" width="400px">
      <el-form :model="copyForm">
        <el-form-item label="目标计划" prop="targetPlanId" required>
          <el-select v-model="copyForm.targetPlanId" placeholder="请选择计划" style="width:100%">
            <el-option
              v-for="item in planOptions"
              :key="item.id"
              :label="item.planName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCopy">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>

    <!-- AI生成用例对话框 -->
  <el-dialog title="AI生成测试用例" v-model="aiDialogVisible" width="1000px" @close="resetAIDialog">
    <el-steps :active="step" finish-status="success" simple style="margin-bottom:20px">
      <el-step title="上传需求" />
      <el-step title="预览编辑" />
      <el-step title="选择计划" />
    </el-steps>

    <!-- Step 1 -->
    <div v-show="step === 0">
      <el-form label-width="100px">
        <el-form-item label="关联项目">
          <el-select v-model="aiProjectId" placeholder="请选择项目" clearable style="width:300px">
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求文档">
          <el-upload :auto-upload="false" :on-change="handleFileChange" :limit="1" accept=".txt,.md,.docx">
            <el-button>上传文件</el-button>
            <template #tip>支持 .txt, .md, .docx</template>
          </el-upload>
        </el-form-item>
        <el-form-item label="或直接粘贴">
          <el-input v-model="aiContent" type="textarea" :rows="8" placeholder="请输入需求描述..." />
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateCases" :loading="generating">生成用例</el-button>
      </div>
    </div>

    <!-- Step 2 -->
    <div v-show="step === 1">
      <el-table :data="aiCases" border max-height="400">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.title" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.type" size="small">
              <el-option label="功能测试" value="功能测试" />
              <el-option label="性能测试" value="性能测试" />
              <el-option label="安全测试" value="安全测试" />
              <el-option label="兼容性测试" value="兼容性测试" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-select v-model="row.priority" size="small">
              <el-option label="最高" :value="1" />
              <el-option label="高" :value="2" />
              <el-option label="中" :value="3" />
              <el-option label="低" :value="4" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button type="danger" size="small" @click="aiCases.splice($index,1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" text style="margin-top:10px" @click="addManualCase">+ 手动添加一行</el-button>
      <div class="dialog-footer">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" @click="step = 2">下一步</el-button>
      </div>
    </div>

    <!-- Step 3 -->
    <div v-show="step === 2">
      <el-form label-width="100px">
        <el-form-item label="目标测试计划">
          <el-select v-model="targetPlanId" placeholder="请选择计划" style="width:300px">
            <el-option v-for="p in filteredPlanOptions" :key="p.id" :label="p.planName" :value="p.id" />
          </el-select>
          <el-button type="primary" text style="margin-left:10px" @click="openCreatePlanDialog">新建计划</el-button>
        </el-form-item>
        <div style="color:#999; margin-left:100px">将为选中的用例添加到该计划</div>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" @click="saveAICases" :loading="savingAI">保存用例</el-button>
      </div>
    </div>
  </el-dialog>

  <!-- 快速新建计划对话框 -->
  <el-dialog title="新建测试计划" v-model="createPlanDialogVisible" width="500px">
    <el-form :model="newPlanForm" label-width="80px">
      <el-form-item label="计划名称" required>
        <el-input v-model="newPlanForm.planName" />
      </el-form-item>
      <el-form-item label="所属项目" required>
        <el-select v-model="newPlanForm.projectId" placeholder="请选择项目" style="width:100%">
          <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="newPlanForm.description" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createPlanDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmCreatePlan">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, nextTick, computed ,watch} from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getTestCaseList, createTestCase, updateTestCase, deleteTestCase, deleteTestCasesBatch } from '@/api/testcase'
import { getRequirementList } from '@/api/requirement'
import { getTestPlanList } from '@/api/testplan'
import { getModulesByProject } from '@/api/projectModule'
import { getProjectList } from '@/api/project'
import { createTestPlan } from '@/api/testplan'
import { aiGenerateTestCases, batchCreateTestCase } from '@/api/testcase'
const tableRef = ref()
const selectedRows = ref([])
const selectedPlanId = ref(null)
const planOptions = ref([])
const allRequirements = ref([])
const currentPlanName = ref('') // 用于编辑时显示
// 项目选项（用于AI步骤和新建计划）
const projectOptions = ref([])

// AI对话框相关
const aiDialogVisible = ref(false)
const step = ref(0)
const aiProjectId = ref(null)
const aiContent = ref('')
const aiFile = ref(null)
const generating = ref(false)
const aiCases = ref([])
const targetPlanId = ref(null)
const savingAI = ref(false)

// 快速新建计划
const createPlanDialogVisible = ref(false)
const newPlanForm = ref({ planName: '', description: '', projectId: null })

// 过滤后的需求（只显示当前计划所属项目的需求）
const filteredRequirements = computed(() => {
  if (!selectedPlanId.value) return []
  const selectedPlan = planOptions.value.find(p => p.id === selectedPlanId.value)
  if (!selectedPlan) return []
  const projectId = selectedPlan.projectId // 需要确保 planOptions 中包含 projectId
  return allRequirements.value.filter(r => r.projectId === projectId)
})

const moduleOptions = ref([])

// 监听计划变化，加载对应项目的模块
watch(selectedPlanId, async (newVal) => {
  if (!newVal) {
    moduleOptions.value = []
    return
  }
  const plan = planOptions.value.find(p => p.id === newVal)
  if (plan && plan.projectId) {
    try {
      const res = await getModulesByProject(plan.projectId)
      moduleOptions.value = res.data
    } catch (error) {
      console.error('加载模块失败', error)
    }
  }
}, { immediate: true })

// 查询条件
const searchForm = ref({
  title: '',
  type: '',
  status: '',
  priority: null
})

// 加载测试计划列表（包含项目ID）
const fetchPlans = async () => {
  try {
    const res = await getTestPlanList({ page: 1, size: 1000 })
    planOptions.value = res.data.records
  } catch (error) {
    console.error('获取计划列表失败', error)
  }
}

// 加载所有需求
const fetchRequirements = async () => {
  try {
    const res = await getRequirementList({ page: 1, size: 1000 })
    allRequirements.value = res.data.records
  } catch (error) {
    console.error('获取需求列表失败', error)
  }
}

onMounted(() => {
  fetchPlans()
  fetchRequirements()
    fetchProjects()  // 新增
})

// 获取用例列表（根据所选计划）
const fetchTestCases = async (params) => {
  if (!selectedPlanId.value) {
    return { data: { records: [], total: 0 } }
  }
  const res = await getTestCaseList({ planId: selectedPlanId.value, ...params })
  // 转换优先级为文字
  res.data.records.forEach(item => {
    item.priorityText = {1:'最高',2:'高',3:'中',4:'低'}[item.priority] || ''
  })
  return res
}

// 计划切换
const handlePlanChange = (planId) => {
  if (planId) {
    const plan = planOptions.value.find(p => p.id === planId)
    currentPlanName.value = plan ? plan.planName : ''
  } else {
    currentPlanName.value = ''
  }
  tableRef.value?.triggerSearch()
}

// 查询
const handleSearch = () => {
  tableRef.value?.triggerSearch()
}
const resetSearch = () => {
  searchForm.value = {
    title: '',
    type: '',
    status: '',
    priority: null
  }
  tableRef.value?.triggerSearch()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 结果标签样式
const resultType = (result) => {
  const map = { '通过': 'success', '失败': 'danger', '阻塞': 'warning', '跳过': 'info' }
  return map[result] || ''
}

// 新增/编辑对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增用例')
const formRef = ref()
const saving = ref(false)

const form = ref({
  id: null,
  title: '',
  precondition: '',
  steps: '',
  expectedResult: '',
  type: '功能测试',
  priority: 2,
  status: '有效',
  requirementId: null,
  planId: null
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  steps: [
    { required: true, message: '请输入测试步骤', trigger: 'blur' }
  ],
  expectedResult: [
    { required: true, message: '请输入预期结果', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  planId: [
    { required: true, message: '请选择所属计划', trigger: 'change' }
  ]
}

const handleAdd = () => {
  dialogTitle.value = '新增用例'
  form.value = {
    id: null,
    title: '',
    precondition: '',
    steps: '',
    expectedResult: '',
    type: '功能测试',
    priority: 2,
    status: '有效',
    requirementId: null,
    planId: selectedPlanId.value,
    moduleId: null 
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用例'
  form.value = {
    id: row.id,
    title: row.title,
    precondition: row.precondition,
    steps: row.steps,
    expectedResult: row.expectedResult,
    type: row.type,
    priority: row.priority,
    status: row.status,
    requirementId: row.requirementId,
    moduleId: row.moduleId
    // 编辑时不需要 planId
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
    return
  }

  saving.value = true
  try {
    if (form.value.id) {
      await updateTestCase(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createTestCase(form.value)
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
  ElMessageBox.confirm(`确认删除用例 ${row.title} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTestCase(row.id)
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
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个用例吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTestCasesBatch(ids)
      ElMessage.success('批量删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}

// 复制功能
const copyDialogVisible = ref(false)
const copyForm = ref({
  targetPlanId: null
})
const sourceCase = ref(null)

const handleCopy = () => {
  if (selectedRows.value.length !== 1) return
  sourceCase.value = selectedRows.value[0]
  copyForm.value.targetPlanId = null
  copyDialogVisible.value = true
}

const handleCopyFrom = (row) => {
  sourceCase.value = row
  copyForm.value.targetPlanId = null
  copyDialogVisible.value = true
}

// 加载项目列表（用于下拉框）
const fetchProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('获取项目列表失败', error)
  }
}

// 过滤当前项目下的计划
const filteredPlanOptions = computed(() => {
  if (!aiProjectId.value) return planOptions.value
  return planOptions.value.filter(p => p.projectId === aiProjectId.value)
})

// 打开AI对话框
const openAIDialog = () => {
  step.value = 0
  aiProjectId.value = selectedPlanId.value ? planOptions.value.find(p => p.id === selectedPlanId.value)?.projectId : null
  aiContent.value = ''
  aiFile.value = null
  aiCases.value = []
  targetPlanId.value = selectedPlanId.value || null
  aiDialogVisible.value = true
}

const resetAIDialog = () => {
  step.value = 0
  aiCases.value = []
}

const handleFileChange = (file) => {
  aiFile.value = file
}

const generateCases = async () => {
  if (!aiContent.value && !aiFile.value) {
    ElMessage.warning('请上传文件或输入内容')
    return
  }
  generating.value = true
  try {
    const formData = new FormData()
    if (aiFile.value) formData.append('file', aiFile.value.raw)
    if (aiContent.value) formData.append('content', aiContent.value)
    if (aiProjectId.value) formData.append('projectId', aiProjectId.value)
    const res = await aiGenerateTestCases(formData)
    // 为每个用例添加默认选中标记
    aiCases.value = res.data.map(c => ({ ...c, selected: true }))
    step.value = 1
  } catch (error) {
    console.error('生成失败', error)
  } finally {
    generating.value = false
  }
}

const addManualCase = () => {
  aiCases.value.push({
    title: '',
    precondition: '',
    steps: '',
    expectedResult: '',
    type: '功能测试',
    priority: 2
  })
}

const saveAICases = async () => {
  if (!targetPlanId.value) {
    ElMessage.warning('请选择目标测试计划')
    return
  }
  // 获取表格中勾选的行（通过ref获取表格组件略复杂，简化为所有非手动删除的行）
  if (aiCases.value.length === 0) {
    ElMessage.warning('没有可保存的用例')
    return
  }
  savingAI.value = true
  try {
    await batchCreateTestCase({ cases: aiCases.value, planId: targetPlanId.value })
    ElMessage.success(`成功创建 ${aiCases.value.length} 个用例`)
    aiDialogVisible.value = false
    if (selectedPlanId.value === targetPlanId.value) {
      tableRef.value?.refresh()
    }
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    savingAI.value = false
  }
}

const openCreatePlanDialog = () => {
  newPlanForm.value = { planName: '', description: '', projectId: aiProjectId.value }
  createPlanDialogVisible.value = true
}

const confirmCreatePlan = async () => {
  if (!newPlanForm.value.planName || !newPlanForm.value.projectId) {
    ElMessage.warning('请填写计划名称和项目')
    return
  }
  try {
    await createTestPlan(newPlanForm.value)
    ElMessage.success('计划创建成功')
    createPlanDialogVisible.value = false
    await fetchPlans() // 刷新计划列表
  } catch (error) {
    console.error('创建计划失败', error)
  }
}

const confirmCopy = async () => {
  if (!copyForm.value.targetPlanId) {
    ElMessage.warning('请选择目标计划')
    return
  }
  try {
    // 复制用例：创建新用例，内容相同，但关联到新计划
    const newCase = {
      title: sourceCase.value.title + ' (复制)',
      precondition: sourceCase.value.precondition,
      steps: sourceCase.value.steps,
      expectedResult: sourceCase.value.expectedResult,
      type: sourceCase.value.type,
      priority: sourceCase.value.priority,
      status: sourceCase.value.status,
      requirementId: sourceCase.value.requirementId,
      planId: copyForm.value.targetPlanId
    }
    await createTestCase(newCase)
    ElMessage.success('复制成功')
    copyDialogVisible.value = false
    if (selectedPlanId.value === copyForm.value.targetPlanId) {
      tableRef.value?.refresh()
    }
  } catch (error) {
    console.error('复制失败', error)
  }
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
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
</style>