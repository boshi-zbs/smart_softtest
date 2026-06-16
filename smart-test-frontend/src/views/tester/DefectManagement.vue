<template>
  <div>
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">提交缺陷</el-button>

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
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="标题" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="新建" value="新建" />
            <el-option label="已指派" value="已指派" />
            <el-option label="修复中" value="修复中" />
            <el-option label="已修复" value="已修复" />
            <el-option label="验证中" value="验证中" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="searchForm.severity" placeholder="全部" clearable style="width: 120px;">
            <el-option label="致命" value="致命" />
            <el-option label="严重" value="严重" />
            <el-option label="一般" value="一般" />
            <el-option label="轻微" value="轻微" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="全部" clearable style="width: 120px;">
            <el-option label="最高" value="最高" />
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
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
      :fetchData="fetchDefects"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <!-- <el-table-column prop="id" label="ID" width="80" /> -->
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="severity" label="严重程度" width="100" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报告人" width="120" />
        <el-table-column prop="assigneeName" label="指派人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="350">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">查看</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button 
            v-if="['已修复', '驳回'].includes(row.status)" 
            size="small" 
            type="warning" 
            @click="handleReopen(row)"
          >重新打开</el-button>
               <el-button 
            v-if="row.status === '已修复'" 
            size="small" 
            type="success" 
            @click="handleVerify(row)"
          >验证</el-button>
           <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 缺陷详情对话框（查看/编辑/新增共用） -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="700px" @close="handleDialogClose">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="标题" prop="title" required>
              <el-input v-model="form.title" :disabled="dialogType === 'view'" />
            </el-form-item>
            <el-form-item label="描述" prop="description" required>
              <el-input v-model="form.description" type="textarea" :rows="4" :disabled="dialogType === 'view'" />
            </el-form-item>
            <el-form-item label="所属项目" prop="projectId" required>
              <el-select v-model="form.projectId" placeholder="请选择项目" :disabled="dialogType === 'view'" style="width:100%">
                <el-option
                  v-for="item in projectOptions"
                  :key="item.id"
                  :label="item.projectName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="关联用例">
              <el-select v-model="form.testCaseId" placeholder="可选" clearable :disabled="dialogType === 'view'" style="width:100%">
                <el-option
                  v-for="item in testCaseOptions"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="关联需求">
              <el-select v-model="form.requirementId" placeholder="可选" clearable :disabled="dialogType === 'view'" style="width:100%">
                <el-option
                  v-for="item in requirementOptions"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="严重程度" prop="severity" required>
              <el-select v-model="form.severity" placeholder="请选择" :disabled="dialogType === 'view'" style="width:100%">
                <el-option label="致命" value="致命" />
                <el-option label="严重" value="严重" />
                <el-option label="一般" value="一般" />
                <el-option label="轻微" value="轻微" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级" prop="priority" required>
              <el-select v-model="form.priority" placeholder="请选择" :disabled="dialogType === 'view'" style="width:100%">
                <el-option label="最高" value="最高" />
                <el-option label="高" value="高" />
                <el-option label="中" value="中" />
                <el-option label="低" value="低" />
              </el-select>
            </el-form-item>
            <el-form-item label="指派人" prop="assigneeId">
              <el-select v-model="form.assigneeId" placeholder="可选" clearable :disabled="dialogType === 'view'" style="width:100%">
                <el-option
                  v-for="item in userOptions"
                  :key="item.id"
                  :label="item.username + (item.realName ? ' (' + item.realName + ')' : '')"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="发现版本">
              <el-input v-model="form.foundVersion" :disabled="dialogType === 'view'" />
            </el-form-item>
            <el-form-item label="修复版本">
              <el-input v-model="form.fixedVersion" :disabled="dialogType === 'view'" />
            </el-form-item>
            <el-form-item label="附件">
              <el-upload
                v-model:file-list="fileList"
                :action="uploadUrl"
                :headers="uploadHeaders"
                list-type="picture-card"
                :on-preview="handlePreview"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :before-upload="beforeUpload"
                multiple
              >
                <el-button type="primary" link>点击上传附件（截图/日志）</el-button>
                <template #tip>
                  <div class="el-upload__tip">支持图片、日志文件，大小不超过 10MB</div>
                </template>
              </el-upload>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="评论历史" name="comments">
          <div style="margin-bottom: 16px;">
            <el-input v-model="newComment" type="textarea" :rows="2" placeholder="请输入评论..." />
            <el-button type="primary" style="margin-top: 8px;" @click="submitComment">发表评论</el-button>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in commentList"
              :key="index"
              :timestamp="item.createTime"
              placement="top"
            >
              <div>
                <span style="font-weight: bold;">{{ item.userName }}</span> 
                <span v-if="item.action === '状态变更'" style="color: #409EFF;">
                  将状态从 {{ item.oldValue }} 改为 {{ item.newValue }}
                </span>
                <span v-else>评论：</span>
              </div>
              <div v-if="item.content">{{ item.content }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">关闭</el-button>
          <el-button v-if="dialogType !== 'view'" type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 图片预览组件 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrlList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getDefectList, getDefect, createDefect, updateDefect, deleteDefect, deleteDefectsBatch,
         getDefectComments, addDefectComment,changeDefectStatus } from '@/api/defect'
import { getProjectList } from '@/api/project'
import { getTestCaseList } from '@/api/testcase'
import { getRequirementList } from '@/api/requirement'
import { getUserList } from '@/api/user'
import { getTestCase } from '@/api/testcase'
import { getRequirement } from '@/api/requirement'
import { useRoute} from 'vue-router'
import { getAutoTestCase } from '@/api/autotest'   // 导入自动化用例详情接口
import { getToken } from '@/utils/auth'
import { ElImageViewer } from 'element-plus'
import { getPublicRoles } from '@/api/role'

// 添加响应式变量
const devRoleId = ref(null)

// 上传相关
const uploadUrl = ref('/api/defects/upload-attachment')
const uploadHeaders = ref({
  Authorization: `Bearer ${getToken()}`
})
const fileList = ref([])

// 预览相关
const previewVisible = ref(false)
const previewUrlList = ref([])
const previewIndex = ref(0)

const route = useRoute()
const tableRef = ref()
const selectedRows = ref([])
const projectOptions = ref([])
const testCaseOptions = ref([])
const requirementOptions = ref([])
const userOptions = ref([])

// 查询条件
const searchForm = ref({
  projectId: null,
  title: '',
  status: '',
  severity: '',
  priority: ''
})

// 加载基础数据
const fetchProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('获取项目列表失败', error)
  }
}
const fetchTestCases = async () => {
  try {
    const res = await getTestCaseList({ page: 1, size: 1000 })
    testCaseOptions.value = res.data.records
  } catch (error) {
    console.error('获取用例列表失败', error)
  }
}
const fetchRequirements = async () => {
  try {
    const res = await getRequirementList({ page: 1, size: 1000 })
    requirementOptions.value = res.data.records
  } catch (error) {
    console.error('获取需求列表失败', error)
  }
}
// 获取开发人员角色ID
const fetchDevRoleId = async () => {
  try {
    const res = await getPublicRoles()
    const devRole = res.data.find(r => r.roleCode === 'ROLE_DEV')
    if (devRole) devRoleId.value = devRole.id
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}
const fetchUsers = async () => {
  try {
    const params = { page: 1, size: 1000 }
    if (devRoleId.value) {
      params.roleId = devRoleId.value
    }
    const res = await getUserList(params)
    userOptions.value = res.data.records
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}

onMounted(async () => {
   // 新增：获取开发角色ID
  await fetchDevRoleId()
  
  console.log('DefectManagement onMounted, route.query:', route.query);
  await fetchProjects()
  await fetchTestCases()
  await fetchRequirements()
  await fetchUsers()

  // 优先判断是否为接口测试提交（新增）
  if (route.query.source === 'api') {
    handleAddFromApiTest(route.query)
  }
  // 根据 executionId 判断是否为自动化执行跳转
  else if (route.query.executionId) {
    await handleAddFromAutoTest(route.query)
  }
  // 如果有 caseId 但没有 log，可能是手动执行跳转
  else if (route.query.caseId) {
    handleAddFromExecution(route.query)
  }
})
const handleAddFromApiTest = (query) => {
  dialogType.value = 'add'
  dialogTitle.value = '提交缺陷'
  form.value = {
    id: null,
    title: query.title || '',
    description: query.description || '',
    projectId: query.projectId ? parseInt(query.projectId) : null,
    testCaseId: query.caseId ? parseInt(query.caseId) : null,
    severity: '一般',
    priority: '中',
    status: '新建',
    assigneeId: null,
    foundVersion: '',
    fixedVersion: '',
    autoExecutionId: null
  }
  fileList.value = []
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}
// 从执行页面跳转过来时，自动打开新增对话框并填充数据
const handleAddFromExecution = (query) => {
  dialogType.value = 'add'
  dialogTitle.value = '提交缺陷'
  form.value = {
    id: null,
    title: query.title || '',
    description: query.description || '',
    projectId: query.projectId ? parseInt(query.projectId) : null,
    testCaseId: query.caseId ? parseInt(query.caseId) : null,
    requirementId: null, // 如果查询中有需求ID，也可以填充
    severity: '一般',
    priority: '中',
    status: '新建',   // 添加状态字段
    assigneeId: null,
    foundVersion: query.foundVersion || '',
    fixedVersion: '',
    autoExecutionId: null   // 手动执行没有关联自动化执行ID
  }
if (query.attachments) {
  try {
    const attachments = JSON.parse(query.attachments);
    fileList.value = attachments.map(att => ({
      name: att.fileName,
      url: att.filePath,   // 注意字段名对应
      fileName: att.fileName,
      fileSize: att.fileSize,
      fileType: att.fileType,
      status: 'success'
    }));
  } catch (e) {
    console.error('解析附件失败', e);
  }
}
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}
// 从自动化执行页面跳转过来时，自动打开新增对话框并填充数据
const handleAddFromAutoTest = async (query) => {
   console.log('handleAddFromAutoTest called with query:', query);
  try {
    // 获取用例详情（获取项目ID和用例名称）
    const caseRes = await getAutoTestCase(query.caseId)
    console.log('getAutoTestCase result:', caseRes);
    const testCase = caseRes.data

    dialogType.value = 'add'
    dialogTitle.value = '提交缺陷'
    form.value = {
      id: null,
      title: `自动化测试失败：${testCase.caseName || '未知用例'}`,
      description: query.log || '自动化执行失败，详情请查看日志。',
      projectId: testCase.projectId || null,
      testCaseId: parseInt(query.caseId),
      requirementId: null,
      severity: '一般',
      priority: '中',
      status: '新建',
      assigneeId: null,
      foundVersion: '',
      fixedVersion: '',
      autoExecutionId: query.executionId ? parseInt(query.executionId) : null,  // 新增字段
    }
   
    // 处理附件
    fileList.value = []
    // 如果有截图参数，添加到附件列表
    if (query.screenshot) {
      fileList.value.push({
        name: '执行截图.png',
        url: query.screenshot,
        fileName: '执行截图.png',
        status: 'success'
      })
    }
    dialogVisible.value = true
    nextTick(() => {
      formRef.value?.clearValidate()
    })
  } catch (error) {
    console.error('获取自动化用例详情失败', error)
    ElMessage.error('无法加载用例信息，请手动填写')
    // 降级处理：仍打开新增对话框，但只填充日志
    dialogType.value = 'add'
    dialogTitle.value = '提交缺陷'
    form.value = {
      id: null,
      title: '自动化测试失败',
      description: query.log || '自动化执行失败',
      projectId: null,
      testCaseId: parseInt(query.caseId),
      requirementId: null,
      severity: '一般',
      priority: '中',
      status: '新建',
      assigneeId: null,
      foundVersion: '',
      fixedVersion: '',
      autoExecutionId: query.executionId ? parseInt(query.executionId) : null
    }
    fileList.value = []
    if (query.screenshot) {
      fileList.value.push({
        name: '执行截图.png',
        url: query.screenshot,
        fileName: '执行截图.png',
        status: 'success'
      })
    }
    dialogVisible.value = true
    console.log('dialogVisible set to true (catch)');
    nextTick(() => {
      formRef.value?.clearValidate()
    })
  }
}
// 获取缺陷列表
const fetchDefects = async (params) => {
  const res = await getDefectList(params)
  return res
}

// 查询
const handleSearch = () => tableRef.value?.triggerSearch()
const resetSearch = () => {
  searchForm.value = {
    projectId: null,
    title: '',
    status: '',
    severity: '',
    priority: ''
  }
  tableRef.value?.triggerSearch()
}
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 状态标签样式
const statusType = (status) => {
  const map = {
    '新建': 'info',
    '已指派': 'warning',
    '修复中': 'warning',
    '已修复': 'success',
    '验证中': 'primary',
    '已关闭': '',
    '重新打开': 'danger',
    '驳回': 'danger'
  }
  return map[status] || ''
}

// 缺陷详情对话框
const dialogVisible = ref(false)
const dialogTitle = ref('缺陷详情')
const dialogType = ref('view') // view, add, edit
const activeTab = ref('info')
const formRef = ref()
const saving = ref(false)
const form = ref({
  id: null,
  title: '',
  description: '',
  projectId: null,
  testCaseId: null,
  requirementId: null,
  severity: '一般',
  priority: '中',
  assigneeId: null,
  foundVersion: '',
  fixedVersion: '',
  status: '新建',
  autoExecutionId: null
})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

// 评论相关
const commentList = ref([])
const newComment = ref('')

// 查看
const handleView = async (row) => {
  dialogType.value = 'view'
  dialogTitle.value = '缺陷详情'
  const res = await getDefect(row.id)
  form.value = res.data
   // ✅ 回显附件
  if (res.data.attachments) {
    fileList.value = res.data.attachments.map(att => ({
      name: att.fileName,
      url: att.filePath,
      fileName: att.fileName,
      fileSize: att.fileSize,
      fileType: att.fileType,
      status: 'success'
    }))
  } else {
    fileList.value = []
  }
  loadComments(row.id)
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑缺陷'
  try {
    const res = await getDefect(row.id)
    const d = res.data

    // 重新构造表单对象，确保所有字段都存在
    form.value = {
      id: d.id,
      title: d.title || '',
      description: d.description || '',
      projectId: d.projectId ? Number(d.projectId) : null,
      testCaseId: d.testCaseId ? Number(d.testCaseId) : null,
      requirementId: d.requirementId ? Number(d.requirementId) : null,
      severity: d.severity || '一般',
      priority: d.priority || '中',
      status: d.status || '新建',  
      assigneeId: d.assigneeId ? Number(d.assigneeId) : null,
      foundVersion: d.foundVersion || '',
      fixedVersion: d.fixedVersion || ''
    }
    if (d.attachments) {
    fileList.value = d.attachments.map(att => ({
      name: att.fileName,
      url: att.filePath,
      fileName: att.fileName,
      fileSize: att.fileSize,
      fileType: att.fileType,
      status: 'success'
    }))
  } else {
    fileList.value = []
  }
    // 补充缺失的用例选项
    if (form.value.testCaseId && !testCaseOptions.value.some(c => c.id === form.value.testCaseId)) {
      const caseRes = await getTestCase(form.value.testCaseId)
      testCaseOptions.value.push(caseRes.data)
    }
    // 补充缺失的需求选项
    if (form.value.requirementId && !requirementOptions.value.some(r => r.id === form.value.requirementId)) {
      const reqRes = await getRequirement(form.value.requirementId)
      requirementOptions.value.push(reqRes.data)
    }

    loadComments(row.id)
    dialogVisible.value = true
  } catch (error) {
    console.error('加载缺陷失败', error)
    ElMessage.error('加载缺陷信息失败')
  }
}
// 新增
const handleAdd = () => {
  dialogType.value = 'add'
  dialogTitle.value = '提交缺陷'
  form.value = {
    id: null,
    title: '',
    description: '',
    projectId: null,
    testCaseId: null,
    requirementId: null,
    severity: '一般',
    priority: '中',
    status: '新建',               // 新增
    assigneeId: null,
    foundVersion: '',
    fixedVersion: '',
    autoExecutionId: null
  }
  commentList.value = []
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
  fileList.value = []  // ✅ 清空附件
}

// 加载评论
const loadComments = async (defectId) => {
  try {
    const res = await getDefectComments(defectId)
    commentList.value = res.data
  } catch (error) {
    console.error('加载评论失败', error)
  }
}

// 提交评论
const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await addDefectComment(form.value.id, newComment.value)
    ElMessage.success('评论成功')
    newComment.value = ''
    loadComments(form.value.id)
  } catch (error) {
    console.error('评论失败', error)
  }
}

// 保存
const handleSave = async () => {
  await formRef.value.validate()
   // ✅ 组装附件数据
  const attachments = fileList.value.map(file => ({
    fileName: file.fileName || file.name,
    filePath: file.url || file.response?.data?.url,
    fileSize: file.fileSize || file.size,
    fileType: file.fileType || file.type
  }))

  // 将 attachments 添加到提交数据中
  const defectData = {
    ...form.value,
    attachments: attachments
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateDefect(form.value.id, defectData)
      ElMessage.success('修改成功')
    } else {
      await createDefect(defectData)
      ElMessage.success('提交成功')
    }
    dialogVisible.value = false
    // 刷新列表
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存失败', error)
    // 如果后端返回错误信息，会通过拦截器显示，这里可以不用额外处理
  } finally {
    saving.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.clearValidate()  // 清除校验状态
  commentList.value = []          // 清空评论列表
  newComment.value = ''           // 清空评论输入
  activeTab.value = 'info'        // 重置标签页
  fileList.value = []  // ✅ 清空附件
}

// 重新打开缺陷
const handleReopen = (row) => {
  ElMessageBox.prompt('请输入重新打开的原因（可选）', '重新打开缺陷', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请说明重新打开的原因...'
  }).then(async ({ value }) => {
    try {
      await changeDefectStatus(row.id, '重新打开', value || '')
      ElMessage.success('缺陷已重新打开')
      // 刷新列表
      tableRef.value?.refresh()
    } catch (error) {
      console.error('重新打开失败', error)
    }
  }).catch(() => {})
}
// 验证缺陷
const handleVerify = (row) => {
  ElMessageBox.confirm('请确认该缺陷是否已修复通过？', '验证缺陷', {
    confirmButtonText: '通过（关闭）',
    cancelButtonText: '不通过（重新打开）',
    distinguishCancelAndClose: true,
    type: 'info'
  }).then(async () => {
    // 用户点击“通过（关闭）”
    try {
      await changeDefectStatus(row.id, '已关闭', '测试验证通过')
      ElMessage.success('缺陷已关闭')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('关闭失败', error)
    }
  }).catch((action) => {
    if (action === 'cancel') {
      // 用户点击“不通过（重新打开）”
      ElMessageBox.prompt('请输入重新打开的原因（可选）', '重新打开缺陷', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请说明不通过的原因...'
      }).then(async ({ value }) => {
        try {
          await changeDefectStatus(row.id, '重新打开', value || '验证不通过')
          ElMessage.success('缺陷已重新打开')
          tableRef.value?.refresh()
        } catch (error) {
          console.error('重新打开失败', error)
        }
      }).catch(() => {})
    }
  })
}
// 删除单个
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除缺陷 ${row.title} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDefect(row.id)
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
    } catch (error) {
      console.error('删除失败', error)
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个缺陷吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDefectsBatch(ids)
      ElMessage.success('批量删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}

const handleUploadSuccess = (response, uploadFile, uploadFiles) => {
  if (response.code === 200) {
    uploadFile.url = response.data.url
    uploadFile.fileName = response.data.fileName
    uploadFile.fileSize = response.data.fileSize
    uploadFile.fileType = response.data.fileType
  } else {
    ElMessage.error('上传失败：' + response.message)
    const index = uploadFiles.indexOf(uploadFile)
    if (index > -1) uploadFiles.splice(index, 1)
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败，请检查网络')
}

const beforeUpload = (file) => {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.warning('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handlePreview = (uploadFile) => {
  previewUrlList.value = fileList.value
    .map(f => f.url || f.response?.data?.url)
    .filter(url => url)  // 过滤掉空 URL
  previewIndex.value = previewUrlList.value.indexOf(uploadFile.url)
  previewVisible.value = true
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