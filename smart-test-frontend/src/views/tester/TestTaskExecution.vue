<template>
  <div>
    <!-- 计划选择 -->
    <el-card class="plan-selector">
      <div style="display: flex; align-items: center; gap: 16px;">
        <span style="font-weight: bold;">选择测试计划：</span>
        <el-select v-model="selectedPlanId" placeholder="请选择计划" style="width: 300px;" @change="handlePlanChange">
          <el-option
            v-for="item in planOptions"
            :key="item.id"
            :label="item.planName"
            :value="item.id"
          />
        </el-select>
        <span v-if="planStatistics.total > 0" style="margin-left: 20px;">
          总用例: {{ planStatistics.total }} | 已执行: {{ planStatistics.executed || 0 }}
          | 通过: {{ planStatistics.通过 || 0 }} | 失败: {{ planStatistics.失败 || 0 }}
          | 阻塞: {{ planStatistics.阻塞 || 0 }} | 跳过: {{ planStatistics.跳过 || 0 }}
        </span>
      </div>
    </el-card>

    <!-- 用例列表 -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>待执行用例列表</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="caseList" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="用例标题" />
        <el-table-column prop="priorityText" label="优先级" width="100" />
        <el-table-column prop="lastResult" label="上次结果" width="120">
          <template #default="{ row }">
            <el-tag :type="resultType(row.lastResult)">{{ row.lastResult || '未执行' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleExecute(row)">执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行用例对话框 -->
    <el-dialog title="执行用例" v-model="executeDialogVisible" width="600px" @close="handleExecuteClose">
      <el-form ref="executeFormRef" :model="executeForm" :rules="executeRules" label-width="100px">
        <el-form-item label="用例标题">
          <el-input v-model="currentCase.title" disabled />
        </el-form-item>
        <el-form-item label="前置条件" v-if="currentCase.precondition">
          <pre>{{ currentCase.precondition }}</pre>
        </el-form-item>
        <el-form-item label="测试步骤" required>
          <pre>{{ currentCase.steps }}</pre>
        </el-form-item>
        <el-form-item label="预期结果" required>
          <pre>{{ currentCase.expectedResult }}</pre>
        </el-form-item>
        <el-form-item label="执行结果" prop="result" required>
          <el-select v-model="executeForm.result" placeholder="请选择结果" style="width:100%">
            <el-option label="通过" value="通过" />
            <el-option label="失败" value="失败" />
            <el-option label="阻塞" value="阻塞" />
            <el-option label="跳过" value="跳过" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际结果" prop="actualResult">
          <el-input v-model="executeForm.actualResult" type="textarea" :rows="3" placeholder="请描述实际结果" />
        </el-form-item>
        <el-form-item label="耗时(ms)" prop="durationMs">
          <el-input-number v-model="executeForm.durationMs" :min="0" style="width:100%" />
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
        <el-form-item v-if="executeForm.result === '失败'" label="创建缺陷">
          <el-checkbox v-model="createDefectAfter">执行后创建缺陷</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="submitExecution" :loading="executing">确定</el-button>
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
import { ref, reactive,onMounted } from 'vue'
import { useRouter,useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTestPlanList } from '@/api/testplan'
import { getPlanCases } from '@/api/testplan'   // 获取计划下的用例详情
import { executeCase, getPlanStatistics } from '@/api/testexecution'
import { completeMessage } from '@/api/message'
import { getToken } from '@/utils/auth'  // 假设有获取 token 的方法
import { ElImageViewer } from 'element-plus'
import { getLatestExecution } from '@/api/testexecution'
// 上传相关
const uploadUrl = ref('/api/test-executions/upload-attachment')
const uploadHeaders = ref({
  Authorization: `Bearer ${getToken()}`
})
const previewIndex = ref(0)
const previewUrlList = ref([])
const fileList = ref([])  // 已上传的文件列表
const route = useRoute()
const router = useRouter()
const selectedPlanId = ref(null)
const planOptions = ref([])
const caseList = ref([])
const planStatistics = ref({ total: 0, executed: 0 })
const loading = ref(false)
// 预览相关
const previewVisible = ref(false)
// 加载测试计划列表
const fetchPlans = async () => {
  try {
    const res = await getTestPlanList({ page: 1, size: 1000 })
    planOptions.value = res.data.records
     // 如果 URL 中有 planId 参数，自动选中该计划
    const planIdParam = route.query.planId
    if (planIdParam) {
      const plan = planOptions.value.find(p => p.id === parseInt(planIdParam))
      if (plan) {
        selectedPlanId.value = plan.id
        await handlePlanChange(plan.id)
      }
    }
  } catch (error) {
    console.error('获取计划列表失败', error)
  }
}


// 切换计划
const handlePlanChange = async (planId) => {
  if (!planId) return
  loading.value = true
  try {
    const [casesRes, statsRes] = await Promise.all([
      getPlanCases(planId),
      getPlanStatistics(planId)
    ])
    caseList.value = casesRes.data.map(c => ({
      ...c,
      priorityText: {1:'最高',2:'高',3:'中',4:'低'}[c.priority] || '',
      lastResult: c.lastResult || null
    }))
    planStatistics.value = statsRes.data
  } catch (error) {
    console.error('加载用例失败', error)
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  fetchPlans()
  
  // 自动完成待办消息
  const messageId = route.query.messageId
  if (messageId) {
    completeMessage(messageId).catch(err => console.error('自动完成待办失败', err))
  }
})
// 执行对话框
const executeDialogVisible = ref(false)
const currentCase = ref({})
const executeFormRef = ref(null)
// 改用 reactive
const executeForm = reactive({
  planId: null,
  caseId: null,
  result: '',
  actualResult: '',
  durationMs: null
})
const executing = ref(false)
const createDefectAfter = ref(false)

const executeRules = {
  result: [{ required: true, message: '请选择执行结果', trigger: 'change' }],
  actualResult: [
    {
      validator: (rule, value, callback) => {
        if (executeForm.result === '失败' && !value) {
          callback(new Error('失败时请填写实际结果'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
// 重置 executeForm 的函数（因为 reactive 不能整体赋值）
const resetExecuteForm = () => {
  fileList.value = []
  executeForm.planId = selectedPlanId.value
  executeForm.caseId = null
  executeForm.result = ''
  executeForm.actualResult = ''
  executeForm.durationMs = null
}

const handleExecute = async (row) => {
  currentCase.value = row
  resetExecuteForm()          // 先重置
  executeForm.caseId = row.id
  executeForm.planId = selectedPlanId.value
  createDefectAfter.value = true

  // 加载该用例的上一次执行记录
  try {
    const res = await getLatestExecution(row.id)
    if (res.data && res.data.id) {
      const last = res.data
      // 回填表单字段
      executeForm.result = last.result || ''
      executeForm.actualResult = last.actualResult || ''
      executeForm.durationMs = last.durationMs || null
      
      // 回填附件列表（需转换为 el-upload 所需的格式）
      if (last.attachments && last.attachments.length) {
        fileList.value = last.attachments.map(att => ({
          name: att.fileName,
          url: att.filePath,
          fileName: att.fileName,
          fileSize: att.fileSize,
          fileType: att.fileType,
          status: 'success'   // 标记为已成功上传
        }))
      }
    }
  } catch (error) {
    console.error('获取上次执行记录失败', error)
    // 失败时不回填，保持空白
  }

  executeDialogVisible.value = true
}

const submitExecution = async () => {
  // 1. 基础校验
  if (!executeForm.result) {
    ElMessage.warning('请选择执行结果')
    return
  }

  // 2. 组装附件数据（从 fileList 中提取已上传文件的信息）
  const attachments = fileList.value.map(file => ({
    fileName: file.fileName || file.name,
    filePath: file.url || file.response?.data?.url,
    fileSize: file.fileSize || file.size,
    fileType: file.fileType || file.type
  }))

  // 3. 构造完整的执行数据（包含附件列表）
  const executionData = {
    planId: executeForm.planId,
    caseId: executeForm.caseId,
    result: executeForm.result,
    actualResult: executeForm.actualResult,
    durationMs: executeForm.durationMs,
    attachments: attachments   // ✅ 附件列表
  }

  console.log('提交数据:', executionData)

  executing.value = true
  try {
    // 4. 提交执行记录（只提交一次）
    await executeCase(executionData)
    ElMessage.success('执行成功')
    executeDialogVisible.value = false

    // 5. 刷新列表和统计
    await handlePlanChange(selectedPlanId.value)

    // 6. 失败时创建缺陷（条件判断）
    if (createDefectAfter.value && executeForm.result === '失败') {
      const projectId = currentCase.value.projectId
      if (!projectId) {
        ElMessage.warning('无法获取项目ID，不能自动创建缺陷')
        return
      }
      // 将附件信息转为JSON字符串传递
      const attachmentData = fileList.value.map(file => ({
        fileName: file.fileName || file.name,
        filePath: file.url || file.response?.data?.url,
        fileSize: file.fileSize || file.size,
        fileType: file.fileType || file.type
      }));
      router.push({
        path: '/tester/defects/create',
        query: {
          caseId: currentCase.value.id,
          title: `执行失败: ${currentCase.value.title}`,
          description: `执行结果：${executeForm.actualResult}`,
          projectId: projectId,
          foundVersion: currentCase.value.foundVersion || '',
          planId: selectedPlanId.value,
          attachments: JSON.stringify(attachmentData)  // 新增
        }
      }).catch(err => {
        console.error('路由跳转失败:', err)
        ElMessage.error('跳转到缺陷创建页面失败')
      })
    }
  } catch (error) {
    console.error('执行失败', error)
    ElMessage.error('执行失败，请重试')
  } finally {
    executing.value = false
  }
}

const handleExecuteClose = () => {
  executeFormRef.value?.clearValidate()
}
const handleCancel = () => {
  resetExecuteForm()          // 清空数据
  executeDialogVisible.value = false
}

const resultType = (result) => {
  const map = { '通过': 'success', '失败': 'danger', '阻塞': 'warning', '跳过': 'info' }
  return map[result] || ''
}

// 上传成功回调
const handleUploadSuccess = (response, uploadFile, uploadFiles) => {
  // response 是后端返回的 Result 对象，data 中包含 url、fileName 等
  if (response.code === 200) {
    // 将返回的信息附加到 file 对象上，便于后续提交
    uploadFile.url = response.data.url
    uploadFile.fileName = response.data.fileName
    uploadFile.fileSize = response.data.fileSize
    uploadFile.fileType = response.data.fileType
  } else {
    ElMessage.error('上传失败：' + response.message)
    // 从列表中移除失败的文件
    const index = uploadFiles.indexOf(uploadFile)
    if (index > -1) uploadFiles.splice(index, 1)
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败，请检查网络')
}

const beforeUpload = (file) => {
  const maxSize = 10 * 1024 * 1024  // 10MB
  if (file.size > maxSize) {
    ElMessage.warning('文件大小不能超过 10MB')
    return false
  }
  return true
}
const handlePreview = (uploadFile) => {
  previewUrlList.value = fileList.value.map(f => f.url || f.response?.data?.url)
  previewIndex.value = previewUrlList.value.indexOf(uploadFile.url)
  previewVisible.value = true
}
</script>

<style scoped>
.plan-selector {
  margin-bottom: 16px;
}
pre {
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>