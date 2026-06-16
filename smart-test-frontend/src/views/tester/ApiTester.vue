<template>
  <div class="api-tester-container">
    <el-row :gutter="20">
      <!-- 左侧：项目 + 模块树 -->
      <el-col :span="6">
        <el-card class="tree-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>接口模块</span>
              <el-button type="primary" size="small" @click="openModuleDialog(null)">新建模块</el-button>
              <el-button type="info" size="small" @click="openEnvDialog" style="margin-left: 8px;">环境变量</el-button>
            </div>
          </template>
          <el-select v-model="selectedProjectId" placeholder="选择项目" filterable clearable @change="onProjectChange" style="width:100%; margin-bottom:16px;">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
          <el-tree
            :data="moduleTree"
            node-key="id"
            :props="{ label: 'moduleName', children: 'children' }"
            @node-click="onModuleClick"
            highlight-current
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon><FolderOpened /></el-icon>
                <span style="flex:1; margin-left:8px;">{{ node.label }}</span>
                <el-button link size="small" @click.stop="openModuleDialog(data)">编辑</el-button>
                <el-button link size="small" type="danger" @click.stop="deleteModuleNode(data.id)">删除</el-button>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 中间：接口列表 -->
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>接口列表</span>
              <el-button type="primary" size="small" @click="openApiDialog(null)" :disabled="!currentModuleId">新建接口</el-button>
              <el-button type="success" size="small" @click="openAIDocDialog" style="margin-left: 8px;">AI生成</el-button>
            </div>
          </template>
          <el-table :data="apiList" stripe @row-click="onApiSelect" highlight-current-row>
            <el-table-column prop="name" label="接口名称" />
            <el-table-column prop="method" label="方法" width="80">
              <template #default="{ row }"><el-tag :type="methodTag(row.method)">{{ row.method }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="url" label="URL" show-overflow-tooltip />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link size="small" @click.stop="openApiDialog(row)">编辑</el-button>
                <el-button link size="small" type="danger" @click.stop="deleteApiNode(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：测试用例 -->
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ currentApi ? currentApi.name : '测试用例' }}</span>
              <div>
                <el-button type="primary" size="small" @click="openTestCaseDialog(null)" :disabled="!currentApi">新建用例</el-button>
              </div>
            </div>
          </template>
          <el-table :data="testCaseList" stripe @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="caseName" label="用例名称" />
            <el-table-column label="最近结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.lastExecutionResult === '成功' ? 'success' : 'danger'">
                  {{ row.lastExecutionResult || '未执行' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button link type="primary" @click="execTestCase(row)">执行</el-button>
                <el-button link @click="openTestCaseDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteTestCaseNode(row.id)">删除</el-button>
                <el-button link @click="viewHistory(row.id)">日志</el-button>
                <el-button v-if="row.lastExecutionResult === '失败'" link type="warning" @click="submitDefect(row)">提bug</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px;">
            <el-button @click="batchExecute" :disabled="selectedCaseIds.length === 0">批量执行</el-button>
            <el-button type="danger" @click="batchDelete" :disabled="selectedCaseIds.length === 0">批量删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模块对话框 -->
    <el-dialog :title="moduleDialogTitle" v-model="moduleDialogVisible" width="500px">
      <el-form :model="moduleForm" label-width="80px">
        <el-form-item label="模块名称" required>
          <el-input v-model="moduleForm.moduleName" />
        </el-form-item>
        <el-form-item label="父模块">
          <el-tree-select v-model="moduleForm.parentId" :data="moduleTree" :props="{ label: 'moduleName', value: 'id' }" clearable placeholder="无" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="moduleForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModule">确定</el-button>
      </template>
    </el-dialog>

    <!-- 接口对话框 -->
    <el-dialog :title="apiDialogTitle" v-model="apiDialogVisible" width="600px">
      <el-form :model="apiForm" label-width="80px">
        <el-form-item label="接口名称" required>
          <el-input v-model="apiForm.name" />
        </el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="apiForm.method">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="apiForm.url" />
        </el-form-item>
        <el-form-item label="请求头">
          <el-input type="textarea" v-model="apiForm.headers" rows="3" placeholder='{"Content-Type":"application/json","Authorization":"Bearer {token}"}' />
          <div class="tip-text">提示：JSON格式会自动转换为 key:value,key:value 格式</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="apiForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveApi">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试用例对话框 -->
    <el-dialog :title="testCaseDialogTitle" v-model="testCaseDialogVisible" width="700px">
      <el-form :model="testCaseForm" label-width="100px">
        <el-form-item label="用例名称" required>
          <el-input v-model="testCaseForm.caseName" />
        </el-form-item>
        <el-form-item label="请求参数(JSON)">
          <el-input type="textarea" v-model="testCaseForm.requestParams" rows="4" placeholder='{"username":"test","password":"123"}' />
        </el-form-item>
        <el-form-item label="期望状态码">
          <el-input-number v-model="testCaseForm.expectedStatus" :min="100" :max="599" />
        </el-form-item>
        <el-form-item label="期望响应包含">
          <el-input v-model="testCaseForm.expectedResponse" placeholder="响应体中应包含的关键词" />
        </el-form-item>
        <el-form-item label="断言类型">
          <el-select v-model="testCaseForm.assertType">
            <el-option label="包含文本" value="contains" />
            <el-option label="状态码匹配" value="status" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="testCaseForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTestCase">确定</el-button>
      </template>
    </el-dialog>

   <!-- 执行历史对话框 -->
<el-dialog title="执行历史" v-model="historyDialogVisible" width="900px">
  <el-table :data="executionHistory" stripe @selection-change="handleHistorySelectionChange">
    <el-table-column type="selection" width="55" />
    <el-table-column prop="executeTime" label="执行时间" width="160" />
    <el-table-column prop="responseStatus" label="状态码" width="80" />
    <el-table-column prop="assertResult" label="断言结果" width="80">
      <template #default="{ row }">
        <el-tag :type="row.assertResult ? 'success' : 'danger'">
          {{ row.assertResult ? '成功' : '失败' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="assertMessage" label="断言信息" show-overflow-tooltip />
    <el-table-column prop="durationMs" label="耗时(ms)" width="80" />
    <el-table-column label="操作" width="100">
      <template #default="{ row }">
        <el-button link type="primary" @click="showResponseDetail(row)">查看响应</el-button>
      </template>
    </el-table-column>
  </el-table>
  <div style="margin-top: 16px; text-align: right;">
    <el-button type="danger" @click="batchDeleteHistory" :disabled="selectedHistoryIds.length === 0">
      批量删除 ({{ selectedHistoryIds.length }})
    </el-button>
  </div>
</el-dialog>

    <!-- 响应详情对话框 -->
    <el-dialog title="响应详情" v-model="responseDetailVisible" width="80%">
      <pre class="response-pre">{{ responseDetail }}</pre>
    </el-dialog>

    <!-- AI文档生成对话框 -->
    <el-dialog title="AI智能生成" v-model="aiDocDialogVisible" width="800px">
      <el-form label-width="100px">
        <el-form-item label="关联项目">
          <el-select v-model="aiDocProjectId" placeholder="选择项目">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文档">
          <el-upload :auto-upload="false" :on-change="handleAIDocFileChange" accept=".txt,.md,.docx">
            <el-button>上传文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="或粘贴内容">
          <el-input v-model="aiDocContent" type="textarea" rows="8" placeholder="请输入需求描述..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDocDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateByDoc" :loading="aiDocLoading">生成</el-button>
      </template>
    </el-dialog>

    <!-- 环境变量配置对话框 -->
    <el-dialog title="环境变量配置" v-model="envDialogVisible" width="600px">
      <div v-for="(value, key, index) in envVariables" :key="index" style="margin-bottom: 8px; display: flex; gap: 8px; align-items: center;">
        <span style="width: 150px;">{{ key }}</span>
        <el-input v-model="envVariables[key]" placeholder="变量值" style="flex:1;" />
        <el-button type="danger" @click="deleteEnvVar(key)">删除</el-button>
      </div>
      <div style="margin-top: 12px; display: flex; gap: 8px;">
        <el-input v-model="newKey" placeholder="新变量名" style="width: 150px;" />
        <el-input v-model="newValue" placeholder="变量值" style="flex:1;" />
        <el-button type="primary" @click="addEnvVar">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="envDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEnv">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened } from '@element-plus/icons-vue'
import { getProjectList } from '@/api/project'
import {
  getModules, addModule, updateModule, deleteModule,
  getApis, addApi, updateApi, deleteApi,
  getTestCases, addTestCase, updateTestCase, deleteTestCase,
  executeTestCase, getExecutions, batchExecuteTestCases, batchDeleteTestCases,
  getLastFailedExecution, generateFromDoc,batchDeleteExecutions
} from '@/api/apitest'
import { getEnv, saveEnv as saveEnvApi } from '@/api/apitest'
import { useStore } from 'vuex'
const router = useRouter()
const projects = ref([])
const selectedProjectId = ref(null)
const moduleTree = ref([])
const currentModuleId = ref(null)
const apiList = ref([])
const currentApi = ref(null)
const testCaseList = ref([])
const selectedCaseIds = ref([])
const store = useStore()
// 模块对话框
const moduleDialogVisible = ref(false)
const moduleDialogTitle = ref('')
const moduleForm = ref({ id: null, projectId: null, moduleName: '', parentId: 0, description: '' })

// 接口对话框
const apiDialogVisible = ref(false)
const apiDialogTitle = ref('')
const apiForm = ref({ id: null, projectId: null, moduleId: null, name: '', method: 'GET', url: '', headers: '', description: '' })

// 测试用例对话框
const testCaseDialogVisible = ref(false)
const testCaseDialogTitle = ref('')
const testCaseForm = ref({
  id: null, apiId: null, caseName: '', requestParams: '',
  expectedStatus: 200, expectedResponse: '', assertType: 'contains', description: ''
})

// 历史
const historyDialogVisible = ref(false)
const executionHistory = ref([])
const responseDetailVisible = ref(false)
const responseDetail = ref('')

// AI文档生成
const aiDocDialogVisible = ref(false)
const aiDocProjectId = ref(null)
const aiDocContent = ref('')
const aiDocFile = ref(null)
const aiDocLoading = ref(false)

const envDialogVisible = ref(false)
const envVariables = ref({})
const newKey = ref('')
const newValue = ref('')

// 历史记录选中项
const selectedHistoryIds = ref([])
const currentHistoryCaseId = ref(null)
// 处理表格选择变化
const handleHistorySelectionChange = (selection) => {
  selectedHistoryIds.value = selection.map(item => item.id)
}

// 批量删除历史记录
const batchDeleteHistory = async () => {
  if (selectedHistoryIds.value.length === 0) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedHistoryIds.value.length} 条执行记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    // 调用后端批量删除接口（需要您实现）
    await batchDeleteExecutions(selectedHistoryIds.value)
    ElMessage.success('删除成功')
    // 刷新当前用例的执行历史列表（假设当前选中的用例ID为 currentApiIdForHistory）
    if (currentHistoryCaseId.value) {
      const res = await getExecutions(currentHistoryCaseId.value)
      executionHistory.value = res.data
    }
    selectedHistoryIds.value = []
  } catch (error) {
    console.error('批量删除失败', error)
    ElMessage.error('删除失败')
  }
}


// 辅助
const methodTag = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

// ========== 关键：将JSON格式请求头转换为 key:value,key:value 格式 ==========
function convertHeadersToKV(headersStr) {
  if (!headersStr) return headersStr
  const trimmed = headersStr.trim()
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const obj = JSON.parse(trimmed)
      const pairs = []
      for (const [key, value] of Object.entries(obj)) {
        pairs.push(`${key}:${value}`)
      }
      return pairs.join(',')
    } catch (e) {
      console.warn('JSON转换失败，保留原值', e)
      return headersStr
    }
  }
  return headersStr
}

// 加载项目
const loadProjects = async () => {
  const res = await getProjectList({ page: 1, size: 1000 })
  projects.value = res.data.records
}

const onProjectChange = () => {
  currentModuleId.value = null
  currentApi.value = null
  apiList.value = []
  testCaseList.value = []
  if (selectedProjectId.value) loadModules()
}

// 加载模块树
const loadModules = async () => {
  const res = await getModules(selectedProjectId.value)
  const flat = res.data
  const map = {}
  const roots = []
  flat.forEach(m => { map[m.id] = { ...m, children: [] } })
  flat.forEach(m => {
    if (m.parentId && map[m.parentId]) map[m.parentId].children.push(map[m.id])
    else roots.push(map[m.id])
  })
  moduleTree.value = roots
}

const onModuleClick = (data) => {
  currentModuleId.value = data.id
  loadApis()
}

const loadApis = async () => {
  if (!currentModuleId.value) return
  const res = await getApis({ moduleId: currentModuleId.value })
  apiList.value = res.data
}

const onApiSelect = (row) => {
  currentApi.value = row
  loadTestCases()
}

const loadTestCases = async () => {
  if (!currentApi.value) return
  const res = await getTestCases(currentApi.value.id)
  testCaseList.value = res.data
}

// 模块 CRUD
const openModuleDialog = (data) => {
  if (data) {
    moduleDialogTitle.value = '编辑模块'
    moduleForm.value = { ...data, projectId: selectedProjectId.value }
  } else {
    moduleDialogTitle.value = '新建模块'
    moduleForm.value = { id: null, projectId: selectedProjectId.value, moduleName: '', parentId: 0, description: '' }
  }
  moduleDialogVisible.value = true
}
const saveModule = async () => {
  if (!moduleForm.value.moduleName) { ElMessage.warning('模块名称不能为空'); return }
  if (moduleForm.value.id) await updateModule(moduleForm.value)
  else await addModule(moduleForm.value)
  ElMessage.success('保存成功')
  moduleDialogVisible.value = false
  loadModules()
}
const deleteModuleNode = async (id) => {
  await ElMessageBox.confirm('删除模块会同时删除其下的接口和用例，确定删除？')
  await deleteModule(id)
  ElMessage.success('删除成功')
  if (currentModuleId.value === id) {
    currentModuleId.value = null
    apiList.value = []
    currentApi.value = null
    testCaseList.value = []
  }
  loadModules()
}

// 接口 CRUD
const openApiDialog = (data) => {
  if (data) {
    apiDialogTitle.value = '编辑接口'
    apiForm.value = { ...data, moduleId: currentModuleId.value, projectId: selectedProjectId.value }
  } else {
    apiDialogTitle.value = '新建接口'
    apiForm.value = { id: null, projectId: selectedProjectId.value, moduleId: currentModuleId.value, name: '', method: 'GET', url: '', headers: '', description: '' }
  }
  apiDialogVisible.value = true
}
const saveApi = async () => {
  if (!apiForm.value.name || !apiForm.value.url) { ElMessage.warning('接口名称和URL不能为空'); return }
  // 转换请求头
  apiForm.value.headers = convertHeadersToKV(apiForm.value.headers)
  if (apiForm.value.id) await updateApi(apiForm.value)
  else await addApi(apiForm.value)
  ElMessage.success('保存成功')
  apiDialogVisible.value = false
  loadApis()
}
const deleteApiNode = async (id) => {
  await ElMessageBox.confirm('删除接口会同时删除其下的测试用例，确定删除？')
  await deleteApi(id)
  ElMessage.success('删除成功')
  if (currentApi.value && currentApi.value.id === id) {
    currentApi.value = null
    testCaseList.value = []
  }
  loadApis()
}

// 测试用例 CRUD
const openTestCaseDialog = (data) => {
  if (data) {
    testCaseDialogTitle.value = '编辑用例'
    testCaseForm.value = { ...data, apiId: currentApi.value.id }
  } else {
    testCaseDialogTitle.value = '新建用例'
    testCaseForm.value = { id: null, apiId: currentApi.value.id, caseName: '', requestParams: '', expectedStatus: 200, expectedResponse: '', assertType: 'contains', description: '' }
  }
  testCaseDialogVisible.value = true
}
const saveTestCase = async () => {
  if (!testCaseForm.value.caseName) { ElMessage.warning('用例名称不能为空'); return }
  if (testCaseForm.value.id) await updateTestCase(testCaseForm.value)
  else await addTestCase(testCaseForm.value)
  ElMessage.success('保存成功')
  testCaseDialogVisible.value = false
  loadTestCases()
}
const deleteTestCaseNode = async (id) => {
  await ElMessageBox.confirm('确定删除此用例？')
  await deleteTestCase(id)
  ElMessage.success('删除成功')
  loadTestCases()
}

// 执行
const execTestCase = async (row) => {
  const res = await executeTestCase(row.id)
  ElMessage.success(`执行完成，结果：${res.data.assertResult ? '通过' : '失败'}`)
  loadTestCases()
}

// 批量执行
const batchExecute = async () => {
  if (selectedCaseIds.value.length === 0) return
  await batchExecuteTestCases(selectedCaseIds.value)
  ElMessage.success('批量执行完成')
  loadTestCases()
  selectedCaseIds.value = []
}

// 批量删除
const batchDelete = async () => {
  if (selectedCaseIds.value.length === 0) return
  await ElMessageBox.confirm('确认删除选中的用例吗？')
  await batchDeleteTestCases(selectedCaseIds.value)
  ElMessage.success('批量删除成功')
  loadTestCases()
  selectedCaseIds.value = []
}

const handleSelectionChange = (selection) => {
  selectedCaseIds.value = selection.map(item => item.id)
}

// 查看历史
const viewHistory = async (caseId) => {
  currentHistoryCaseId.value = caseId
  const res = await getExecutions(caseId)
  executionHistory.value = res.data
  historyDialogVisible.value = true
}

const showResponseDetail = (row) => {
  responseDetail.value = `状态码: ${row.responseStatus}\n响应体:\n${row.responseBody || '(空)'}`
  responseDetailVisible.value = true
}

// 提bug
const submitDefect = async (row) => {
  if (!currentApi.value) {
    ElMessage.warning('未选中接口')
    return
  }
  const res = await getLastFailedExecution(row.id)
  const execution = res.data
  if (!execution) {
    ElMessage.warning('没有找到失败执行记录')
    return
  }
  const query = {
    source: 'api',
    caseId: row.id,
    executionId: execution.id,
    title: `接口测试失败：${currentApi.value.name} - ${row.caseName}`,
    projectId: currentApi.value.projectId,
    description: `【接口信息】\n请求方法: ${currentApi.value.method}\n请求URL: ${currentApi.value.url}\n请求头: ${currentApi.value.headers || '无'}\n请求参数: ${execution.requestBody || '无'}\n\n【响应信息】\nHTTP状态码: ${execution.responseStatus}\n响应体: ${execution.responseBody || '无'}\n\n【断言信息】\n${execution.assertMessage || '无'}`
  }
  router.push({ path: '/tester/defects/create', query })
}

// AI文档生成
const openAIDocDialog = () => {
  aiDocProjectId.value = selectedProjectId.value
  aiDocContent.value = ''
  aiDocFile.value = null
  aiDocDialogVisible.value = true
}
const handleAIDocFileChange = (file) => {
  aiDocFile.value = file.raw
}
const generateByDoc = async () => {
  if (!aiDocProjectId.value) {
    ElMessage.warning('请选择项目')
    return
  }
  if (!aiDocContent.value && !aiDocFile.value) {
    ElMessage.warning('请上传文档或输入内容')
    return
  }
  aiDocLoading.value = true
  const formData = new FormData()
  formData.append('projectId', aiDocProjectId.value)
  if (aiDocContent.value) formData.append('content', aiDocContent.value)
  if (aiDocFile.value) formData.append('file', aiDocFile.value)
  try {
    const res = await generateFromDoc(formData)
    ElMessage.success(`成功生成 ${res.data.length} 个模块，请刷新模块树查看`)
    aiDocDialogVisible.value = false
    if (selectedProjectId.value === aiDocProjectId.value) {
      loadModules()
    }
  } catch (e) {
    ElMessage.error('生成失败')
  } finally {
    aiDocLoading.value = false
  }
}

const openEnvDialog = async () => {
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  const res = await getEnv(selectedProjectId.value)
  envVariables.value = res.data || {}
  envDialogVisible.value = true
}

const addEnvVar = () => {
  if (!newKey.value) return
  envVariables.value[newKey.value] = newValue.value
  newKey.value = ''
  newValue.value = ''
}

const deleteEnvVar = (key) => {
  delete envVariables.value[key]
}

const saveEnv = async () => {
  await saveEnvApi({ projectId: selectedProjectId.value, variables: envVariables.value })
  ElMessage.success('保存成功')
  envDialogVisible.value = false
}

onMounted(async () => {
    // 确保 token 存在，否则跳转登录
    const token = localStorage.getItem('token')
    if (!token) {
        router.push('/login')
        return
    }
    // 如果 store 中没有用户信息，尝试获取
    if (!store.state.user) {
        try {
            await store.dispatch('fetchUserInfo')
        } catch (error) {
            console.error('获取用户信息失败，可能 token 无效', error)
            // token 无效，清空并跳转登录
            localStorage.removeItem('token')
            router.push('/login')
            return
        }
    }
    // 加载项目列表
    await loadProjects()
})
</script>

<style scoped>
.api-tester-container { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.tree-card { height: calc(100vh - 120px); overflow: auto; }
.tree-node { display: flex; align-items: center; width: 100%; }
.response-pre { white-space: pre-wrap; word-wrap: break-word; background: #f5f7fa; padding: 12px; border-radius: 8px; }
.tip-text { font-size: 12px; color: #909399; margin-top: 4px; }
</style>