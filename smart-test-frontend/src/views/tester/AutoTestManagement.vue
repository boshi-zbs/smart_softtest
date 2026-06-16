<template>
  <div>
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新建自动化用例</el-button>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目">
          <el-select v-model="searchForm.projectId" placeholder="全部" clearable style="width:150px;">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用例名称">
          <el-input v-model="searchForm.caseName" placeholder="名称" clearable style="width:150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="action-group">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleBatchExecute" :disabled="selectedRows.length === 0">
          批量执行
        </el-button>
      </div>
    </div>

    <CommonTable
      ref="tableRef"
      :fetchData="fetchCases"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <!-- <el-table-column prop="id" label="ID" width="80" /> -->
        <el-table-column prop="caseName" label="用例名称" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="url" label="URL" show-overflow-tooltip />
        <el-table-column prop="latestStatus" label="最近执行状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.latestStatus)">
              {{ statusTagText(row.latestStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleExecute(row)">执行</el-button>
            <el-button size="small" type="warning" @click="handleSubmitDefectFromList(row)">提交缺陷</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="800px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目" prop="projectId" required>
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width:100%">
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用例名称" prop="caseName" required>
          <el-input v-model="form.caseName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="URL" prop="url" required>
          <el-input v-model="form.url" placeholder="例如 http://localhost:8080/login" />
        </el-form-item>
        <el-form-item label="运行模式" prop="headless" required>
          <el-radio-group v-model="form.headless">
            <el-radio :label="true">无头模式（后台运行，不显示界面）</el-radio>
            <el-radio :label="false">有头模式（显示浏览器界面）</el-radio>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            提示：无头模式适合服务器运行，有头模式适合调试时观察执行过程
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider>测试步骤</el-divider>
        <div v-for="(step, index) in form.steps" :key="index" style="margin-bottom: 16px; border: 1px solid #eee; padding: 16px; border-radius: 4px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
            <span style="font-weight: bold;">步骤 {{ index + 1 }}</span>
            <el-button type="danger" size="small" @click="removeStep(index)">删除</el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="6">
              <el-form-item label="操作" :prop="'steps.' + index + '.actionType'" :rules="stepRules.actionType">
                <el-select v-model="step.actionType" placeholder="请选择" style="width:100%">
                  <el-option label="打开URL" value="open" />
                  <el-option label="点击" value="click" />
                  <el-option label="输入" value="input" />
                  <el-option label="清除" value="clear" />
                  <el-option label="选择下拉框" value="select" />
                  <el-option label="悬停" value="hover" />
                  <el-option label="双击" value="doubleClick" />
                  <el-option label="右键点击" value="contextClick" />
                  <el-option label="断言文本" value="assertText" />
                  <el-option label="断言标题" value="assertTitle" />
                  <el-option label="断言元素存在" value="assertElementPresent" />
                  <el-option label="断言元素可见" value="assertElementVisible" />
                  <el-option label="获取属性" value="getAttribute" />
                  <el-option label="执行JavaScript" value="executeScript" />
                  <el-option label="滚动到元素" value="scrollIntoView" />
                  <el-option label="切换iframe" value="switchToFrame" />
                  <el-option label="切换回主文档" value="switchToDefaultContent" />
                  <el-option label="切换窗口" value="switchToWindow" />
                  <el-option label="上传文件" value="uploadFile" />
                  <el-option label="按键" value="keyPress" />
                  <el-option label="等待" value="wait" />
                  <el-option label="后退" value="back" />
                  <el-option label="前进" value="forward" />
                  <el-option label="刷新" value="refresh" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6" v-if="!['open','wait','assertTitle','switchToDefaultContent','executeScript','back','forward','refresh'].includes(step.actionType)">
              <el-form-item label="定位方式" :prop="'steps.' + index + '.locatorType'">
                <el-select v-model="step.locatorType" placeholder="请选择" style="width:100%">
                  <el-option label="id" value="id" />
                  <el-option label="name" value="name" />
                  <el-option label="xpath" value="xpath" />
                  <el-option label="css" value="css" />
                  <el-option label="linkText" value="linkText" />
                  <el-option label="partialLinkText" value="partialLinkText" />
                  <el-option label="tagName" value="tagName" />
                  <el-option label="className" value="className" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12" v-if="!['open','wait','assertTitle','switchToDefaultContent','executeScript','back','forward','refresh'].includes(step.actionType)">
              <el-form-item label="定位值" :prop="'steps.' + index + '.locatorValue'">
                <el-input v-model="step.locatorValue" placeholder="定位表达式" />
              </el-form-item>
            </el-col>
            <el-col :span="20" v-if="['input','assertText','getAttribute','keyPress','uploadFile','executeScript','switchToWindow','assertTitle'].includes(step.actionType)">
              <el-form-item label="值" :prop="'steps.' + index + '.inputValue'">
                <el-input
                  v-model="step.inputValue"
                  :placeholder="getInputPlaceholder(step.actionType)"
                  :type="step.actionType === 'executeScript' ? 'textarea' : 'text'"
                  :rows="step.actionType === 'executeScript' ? 2 : undefined"
                />
              </el-form-item>
            </el-col>
            <el-col :span="6" v-if="step.actionType === 'wait'">
              <el-form-item label="等待秒数" :prop="'steps.' + index + '.waitSeconds'">
                <el-input-number v-model="step.waitSeconds" :min="0" style="width:100%" controls-position="right"/>
              </el-form-item>
            </el-col>
            <el-col :span="20" v-if="step.actionType === 'open'">
              <el-form-item label="相对路径" :prop="'steps.' + index + '.locatorValue'">
                <el-input v-model="step.locatorValue" placeholder="相对路径，如 /login" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述">
            <el-input v-model="step.description" placeholder="步骤描述" />
          </el-form-item>
        </div>
        <el-button type="primary" plain @click="addStep">添加步骤</el-button>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 执行结果对话框 -->
    <el-dialog 
      title="执行结果" 
      v-model="resultDialogVisible" 
      width="700px" 
      @close="stopPolling"
      class="execution-result-dialog"
    >
      <div class="result-container">
        <div class="result-header">
          <el-result
            :icon="executionSuccess ? 'success' : 'error'"
            :title="executionSuccess ? '执行成功' : '执行失败'"
            :sub-title="executionSuccess ? '' : '可提交缺陷给开发人员'"
          >
          </el-result>
        </div>
        <!-- 截图预览区域 -->
        <div v-if="screenshotUrl" class="screenshot-section">
          <h4>执行截图</h4>
          <el-image 
            :src="screenshotUrl" 
            :preview-src-list="[screenshotUrl]"
            fit="contain"
            style="max-width: 100%; max-height: 200px; border: 1px solid #dcdfe6; border-radius: 4px;"
          />
        </div>
        <div class="log-container">
          <div class="log-header">
            <span class="log-title">执行日志</span>
            <el-button size="small" @click="copyLog" :data-clipboard-text="executionResult">
              <el-icon><DocumentCopy /></el-icon>
              复制日志
            </el-button>
          </div>
          <div class="log-content" ref="logContentRef">
            <pre class="log-pre">{{ executionResult || '暂无日志信息' }}</pre>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <el-button v-if="!executionSuccess" type="danger" @click="handleSubmitDefect" :loading="submitting">
            提交缺陷
          </el-button>
          <el-button @click="resultDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { DocumentCopy } from '@element-plus/icons-vue'  // 需要导入图标

import {
  getAutoTestCaseList,
  getAutoTestCase,
  createAutoTestCase,
  updateAutoTestCase,
  deleteAutoTestCase,
  executeAutoTestCase,
  getAutoTestExecution,
  batchExecuteAutoTestCase,
  getLastFailedExecution
} from '@/api/autotest'
import { getProjectList } from '@/api/project'

const router = useRouter()
const tableRef = ref()
const projectOptions = ref([])
const searchForm = ref({
  projectId: null,
  caseName: '',
  status: null
})

const statusTagType = (status) => {
  const map = {
    'success': 'success',
    'failed': 'danger',
    'running': 'warning',
    'none': 'info'
  }
  return map[status] || 'info'
}

const statusTagText = (status) => {
  const map = {
    'success': '成功',
    'failed': '失败',
    'running': '执行中',
    'none': '未执行'
  }
  return map[status] || status
}

const selectedRows = ref([])

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const fetchProjects = async () => {
  const res = await getProjectList({ page: 1, size: 1000 })
  projectOptions.value = res.data.records
}
onMounted(fetchProjects)

const fetchCases = async (params) => {
  const res = await getAutoTestCaseList(params)
  return res
}
const handleSearch = () => tableRef.value?.triggerSearch()
const resetSearch = () => {
  searchForm.value = { projectId: null, caseName: '', status: null }
  tableRef.value?.triggerSearch()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增自动化用例')
const formRef = ref()
const saving = ref(false)

const form = ref({
  id: null,
  projectId: null,
  caseName: '',
  description: '',
  url: '',
  headless: true,
  status: 1,
  steps: []
})

const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  caseName: [{ required: true, message: '请输入用例名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入URL', trigger: 'blur' }],
  headless: [{ required: true, message: '请选择运行模式', trigger: 'change' }]
}

const stepRules = {
  actionType: [{ required: true, message: '请选择操作', trigger: 'change' }]
}

const getInputPlaceholder = (actionType) => {
  const map = {
    input: '要输入的值',
    assertText: '期望的文本',
    getAttribute: '属性名 (如 value, href)',
    keyPress: '按键 (ENTER, TAB 等)',
    uploadFile: '文件绝对路径',
    executeScript: 'JavaScript 脚本',
    switchToWindow: '窗口标题或句柄',
    assertTitle: '期望的页面标题'
  }
  return map[actionType] || '输入值'
}

const addStep = () => {
  form.value.steps.push({
    actionType: 'click',
    locatorType: 'id',
    locatorValue: '',
    inputValue: '',
    waitSeconds: 0,
    description: '',
    stepOrder: form.value.steps.length + 1
  })
}

const removeStep = (index) => {
  form.value.steps.splice(index, 1)
}

const handleAdd = () => {
  dialogTitle.value = '新增自动化用例'
  form.value = {
    id: null,
    projectId: null,
    caseName: '',
    description: '',
    url: '',
    headless: true,
    status: 1,
    steps: []
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑自动化用例'
  const res = await getAutoTestCase(row.id)
  form.value = {
    ...res.data,
    headless: res.data.headless === 1,
    steps: res.data.steps || []
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleClose = () => {
  formRef.value?.resetFields()
}

const handleSave = async () => {
  if (!form.value.steps || form.value.steps.length === 0) {
    ElMessage.warning('请至少添加一个步骤')
    return
  }

  const stepsWithOrder = form.value.steps.map((step, index) => ({
    ...step,
    stepOrder: index + 1
  }))

  for (let i = 0; i < stepsWithOrder.length; i++) {
    const step = stepsWithOrder[i]
    if (!step.actionType) {
      ElMessage.warning(`步骤 ${i+1} 的操作类型不能为空`)
      return
    }
    if (!['wait', 'assertTitle', 'switchToDefaultContent', 'executeScript', 'open', 'back', 'forward', 'refresh'].includes(step.actionType) && !step.locatorType) {
      ElMessage.warning(`步骤 ${i+1} 的定位方式不能为空`)
      return
    }
    if (['input', 'assertText', 'getAttribute', 'keyPress', 'uploadFile', 'executeScript', 'switchToWindow', 'assertTitle'].includes(step.actionType) && !step.inputValue && step.inputValue !== 0) {
      ElMessage.warning(`步骤 ${i+1} 的输入值不能为空`)
      return
    }
  }

  await formRef.value.validate()
  saving.value = true

  const requestData = {
    testCase: {
      id: form.value.id,
      projectId: form.value.projectId,
      caseName: form.value.caseName,
      description: form.value.description,
      url: form.value.url,
      headless: form.value.headless ? 1 : 0,
      status: form.value.status
    },
    steps: stepsWithOrder
  }

  try {
    if (form.value.id) {
      await updateAutoTestCase(form.value.id, requestData)
      ElMessage.success('修改成功')
    } else {
      await createAutoTestCase(requestData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败，请检查数据或联系管理员')
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用例 ${row.caseName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteAutoTestCase(row.id)
    ElMessage.success('删除成功')
    tableRef.value?.refresh()
  }).catch(() => {})
}

const resultDialogVisible = ref(false)
const executionSuccess = ref(false)
const executionResult = ref('')
const screenshotUrl = ref('')  // 新增：截图 URL
const currentExecutionId = ref(null)
const currentCaseId = ref(null)
const pollingTimer = ref(null)
const submitting = ref(false)

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const pollExecutionResult = async (executionId) => {
  try {
    const res = await getAutoTestExecution(executionId)
    const execution = res.data
     // 获取截图 URL
    screenshotUrl.value = execution.screenshotUrl || ''
    if (execution.status === 'success') {
      executionSuccess.value = true
      executionResult.value = execution.result || '执行成功'
      
      stopPolling()
      resultDialogVisible.value = true
    } else if (execution.status === 'failed') {
      executionSuccess.value = false
      executionResult.value = execution.result || '执行失败'
      stopPolling()
      resultDialogVisible.value = true
    }
  } catch (error) {
    console.error('查询执行结果失败', error)
    stopPolling()
    ElMessage.error('查询执行结果失败')
  }
}

const handleExecute = async (row) => {
  try {
    stopPolling()
    currentCaseId.value = row.id
    const headlessValue = row.headless === 1
    const res = await executeAutoTestCase(row.id, { headless: headlessValue })
    const execution = res.data
    currentExecutionId.value = execution.id
    ElMessage.info('任务已提交，正在执行中...')
    await pollExecutionResult(execution.id)
    pollingTimer.value = setInterval(() => {
      pollExecutionResult(execution.id)
    }, 2000)
    tableRef.value?.refresh()
  } catch (error) {
    console.error('执行失败', error)
    ElMessage.error('执行失败：' + (error.message || '未知错误'))
    stopPolling()
  }
}

const handleSubmitDefect = () => {
  router.push({
    path: '/tester/defects/create',
    query: {
      caseId: currentCaseId.value,
      executionId: currentExecutionId.value,
      log: executionResult.value,
      screenshot: screenshotUrl.value   // 传递截图 URL
    }
  })
  resultDialogVisible.value = false
}

onUnmounted(() => {
  stopPolling()
})

const handleBatchExecute = async () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  try {
    await batchExecuteAutoTestCase(ids)
    ElMessage.success(`已提交 ${ids.length} 个用例执行，请稍后查看状态`)
    tableRef.value?.refresh()
  } catch (error) {
    console.error('批量执行失败', error)
    ElMessage.error('批量执行失败')
  }
}

const handleSubmitDefectFromList = async (row) => {
  try {
    const res = await getLastFailedExecution(row.id)
    const execution = res.data
    router.push({
      path: '/tester/defects/create',
      query: {
        caseId: row.id,
        executionId: execution.id,
        log: execution.result,
        screenshot: execution.screenshotUrl || ''   // 传递截图 URL
      }
    })
  } catch (error) {
    console.error('获取失败执行记录失败', error)
    ElMessage.error('该用例暂无失败的执行记录，无法提交缺陷')
  }
}
// 添加复制功能


const copyLog = async () => {
  try {
    await navigator.clipboard.writeText(executionResult.value)
    ElMessage.success('日志已复制到剪贴板')
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = executionResult.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('日志已复制到剪贴板')
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
/* 执行结果对话框样式 */
.execution-result-dialog :deep(.el-dialog__body) {
  padding: 0 20px 10px 20px;
}

.result-container {
  min-height: 400px;
}

.result-header {
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}
.screenshot-section {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
.screenshot-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}
.log-container {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f5f7fa;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background-color: #f0f2f5;
  border-bottom: 1px solid #dcdfe6;
}

.log-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.log-content {
  height: 350px;
  overflow-y: auto;
  overflow-x: auto;
  background-color: #1e1e2e;
  position: relative;
}

.log-pre {
  margin: 0;
  padding: 16px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #e4e4e4;
  background-color: #1e1e2e;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
}

/* 自定义滚动条样式 */
.log-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.log-content::-webkit-scrollbar-track {
  background: #2d2d3d;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb {
  background: #666;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb:hover {
  background: #888;
}

/* 执行成功时的颜色 */
.execution-result-dialog .el-result__title .el-result__title {
  color: #67c23a;
}

/* 执行失败时的颜色 */
.execution-result-dialog .el-result__title.is-error {
  color: #f56c6c;
}
</style>