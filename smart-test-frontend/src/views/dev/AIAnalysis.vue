<template>
  <div class="ai-analysis-container">
    <el-row :gutter="24">
      <!-- 左侧：缺陷列表 -->
      <el-col :xs="24" :sm="12" :md="12" :lg="12">
        <el-card class="defect-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">缺陷列表</span>
              <div class="header-actions">
                <el-button type="primary" size="small" @click="loadDefects" :icon="Refresh">刷新</el-button>
                <el-button 
                  type="success" 
                  size="small" 
                  :disabled="selectedDefects.length === 0"
                  @click="startBatchAnalysis"
                  :icon="List"
                >
                  批量分析 ({{ selectedDefects.length }})
                </el-button>
              </div>
            </div>
          </template>
          
          <!-- 查询条件 -->
          <el-form :inline="true" :model="searchForm" size="small" class="search-form">
            <el-form-item>
              <el-input v-model="searchForm.title" placeholder="缺陷标题" clearable prefix-icon="Search" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.projectId" placeholder="选择项目" clearable filterable style="width: 200px">
                <el-option
                  v-for="item in projectOptions"
                  :key="item.id"
                  :label="item.projectName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadDefects">查询</el-button>
            </el-form-item>
          </el-form>
          
          <el-table 
            :data="defectList" 
            @selection-change="handleSelectionChange"
            stripe
            height="500"
            border
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="projectName" label="项目名称" width="150" show-overflow-tooltip />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="analyzeSingle(row)">分析</el-button>
                <el-button type="info" link @click="viewHistory(row)">历史</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <!-- 右侧：分析结果 -->
      <el-col :xs="24" :sm="12" :md="12" :lg="12">
        <!-- 文件树卡片 -->
        <el-card class="file-tree-card" shadow="hover" v-if="currentProjectId">
          <template #header>
            <div class="card-header">
              <span class="title">
                <el-icon><FolderOpened /></el-icon> 代码文件树
              </span>
              <div class="header-actions">
                <el-button size="small" @click="toggleSelectAll" :icon="selectAllIcon">
                  {{ selectAllText }}
                </el-button>
                <el-button size="small" @click="searchCodeByKeyword" :icon="Search">搜索</el-button>
              </div>
            </div>
          </template>
          <div class="file-tree-body">
            <div v-if="fileTree.length === 0 && !isLoadingFileTree" class="empty-tree-tip">
              <el-empty description="暂无代码文件，请检查Git配置" :image-size="80" />
            </div>
            <el-tree
              v-else-if="fileTree.length > 0"
              :data="fileTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="path"
              show-checkbox
              ref="treeRef"
              @check="handleTreeCheck"
              :default-expand-all="false"
              :expand-on-click-node="false"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon v-if="data.type === 'file'"><Document /></el-icon>
                  <el-icon v-else><Folder /></el-icon>
                  {{ node.label }}
                </span>
              </template>
            </el-tree>
            <div v-if="isLoadingFileTree" class="loading-tree">
              <el-icon class="is-loading"><Loading /></el-icon> 加载代码树中...
            </div>
          </div>
          <div class="file-tree-footer" v-if="selectedFiles.length > 0">
            <el-tag type="info">已选择 {{ selectedFiles.length }} 个文件</el-tag>
          </div>
        </el-card>

        <!-- 分析结果卡片 -->
        <el-card class="analysis-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">
                <el-icon><DataAnalysis /></el-icon> 分析结果
              </span>
              <el-tag v-if="currentDefect" type="info" size="small" effect="plain">
                当前缺陷：{{ currentDefect.title }}
              </el-tag>
            </div>
          </template>
          
          <!-- 分析结果展示 -->
          <div class="analysis-result" v-if="analysisResult">
            <div class="result-section problem">
              <div class="section-title">
                <el-icon><Position /></el-icon> 问题定位
              </div>
              <div class="section-content">{{ analysisResult.problemLocation || '暂无' }}</div>
            </div>
            <div class="result-section solution">
              <div class="section-title">
                <el-icon><Tools /></el-icon> 修复方案
              </div>
              <div class="section-content" v-html="renderMarkdown(analysisResult.fixSuggestion || '暂无')"></div>
            </div>
            <div class="result-section caution" v-if="analysisResult.precautions">
              <div class="section-title">
                <el-icon><Warning /></el-icon> 注意事项
              </div>
              <div class="section-content">{{ analysisResult.precautions }}</div>
            </div>
            <div class="result-section files" v-if="analysisResult.affectedFiles && analysisResult.affectedFiles.length">
              <div class="section-title">
                <el-icon><Files /></el-icon> 相关文件
              </div>
              <div class="section-content">
                <el-tag v-for="file in analysisResult.affectedFiles" :key="file" size="small" class="file-tag">
                  {{ file }}
                </el-tag>
              </div>
            </div>
          </div>
          <div v-else-if="isAnalyzing" class="analyzing">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>AI 正在深度分析中，请稍候...</span>
          </div>
          <div v-else class="empty-result">
            <el-empty description="选择缺陷后点击分析，AI 将提供智能修复建议" :image-size="120">
              <template #extra>
                <el-button type="primary" @click="loadDefects">刷新缺陷列表</el-button>
              </template>
            </el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录对话框 -->
    <el-dialog 
      :title="`缺陷「${historyDefect?.title}」分析历史`" 
      v-model="historyDialogVisible" 
      width="800px"
      destroy-on-close
    >
      <el-table v-loading="historyLoading" :data="historyList" border stripe>
        <el-table-column prop="createTime" label="分析时间" width="160" />
        <el-table-column prop="analysisType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.analysisType === 'single' ? 'primary' : 'success'">
              {{ row.analysisType === 'single' ? '单缺陷' : '批量' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fixSuggestions" label="修复建议" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link @click="showAnalysisDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="historyDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 统一的分析详情对话框 -->
    <el-dialog 
      title="分析详情" 
      v-model="detailDialogVisible" 
      width="750px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="detail-content-wrapper" v-if="currentAnalysis">
        <div class="detail-section">
          <h4>问题定位</h4>
          <div class="detail-text">{{ currentAnalysis.problemLocation || '暂无' }}</div>
        </div>
        <div class="detail-section">
          <h4>修复方案</h4>
          <div class="detail-text" v-html="renderMarkdown(currentAnalysis.fixSuggestions || '暂无')"></div>
        </div>
        <div class="detail-section" v-if="currentAnalysis.precautions">
          <h4>注意事项</h4>
          <div class="detail-text">{{ currentAnalysis.precautions }}</div>
        </div>
        <div class="detail-section">
          <h4>相关文件</h4>
          <div class="detail-text">{{ currentAnalysis.affectedFiles || '暂无' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 批量分析进度对话框 -->
    <el-dialog title="批量分析" v-model="batchDialogVisible" width="600px" destroy-on-close @close="stopProgressTimer">
      <div class="batch-progress">
        <el-progress :percentage="batchProgress" :status="batchStatus === 'failed' ? 'exception' : undefined" />
        <div class="batch-results" v-if="batchResults.length > 0">
          <div v-for="result in batchResults" :key="result.defectId" class="batch-result-item">
            <span>{{ result.defectTitle }}</span>
            <div>
              <el-tag :type="result.fixSuggestion ? 'success' : 'danger'" size="small">
                {{ result.fixSuggestion ? '已完成' : '失败' }}
              </el-tag>
              <el-button link type="primary" size="small" @click="viewBatchDetail(result.defectId)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, Document, Folder, Loading, Refresh, List, 
  FolderOpened, DataAnalysis, Position, Tools, Warning, Files 
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import { getDefectList } from '@/api/defect'
import { getProject } from '@/api/project'
import { getProjectList } from '@/api/project'
import { getFileTree, analyzeSingleDefect, analyzeBatchDefects, getAnalysisHistory } from '@/api/aiAnalysis'

const route = useRoute()

// 项目选项
const projectOptions = ref([])

// 项目ID（路由参数）
const projectId = ref(parseInt(route.query.id) || null)
const projectName = ref('')

// 缺陷列表
const defectList = ref([])
const selectedDefects = ref([])
const searchForm = ref({ title: '', projectId: null })
const currentDefect = ref(null)

// 当前选中的项目ID（用于文件树加载）
const currentProjectId = ref(null)

// 代码文件树
const fileTree = ref([])
const treeRef = ref()
const selectedFiles = ref([])
const isLoadingFileTree = ref(false)

// 分析结果
const analysisResult = ref(null)
const isAnalyzing = ref(false)

// 批量分析
const batchDialogVisible = ref(false)
const batchProgress = ref(0)
const batchStatus = ref('')
const batchResults = ref([])
let progressTimer = null

// 历史记录对话框
const historyDialogVisible = ref(false)
const historyDefect = ref(null)
const historyList = ref([])
const historyLoading = ref(false)

// 统一的分析详情对话框
const detailDialogVisible = ref(false)
const currentAnalysis = ref(null)

// 状态标签样式
const statusTagType = (status) => {
  const map = { '新建': 'info', '已指派': 'warning', '修复中': 'warning', '已修复': 'success' }
  return map[status] || ''
}

// Markdown 渲染
const renderMarkdown = (content) => {
  if (!content) return ''
  return marked.parse(content)
}

// 加载项目列表
const loadProjects = async () => {
  try {
    const res = await getProjectList({ page: 1, size: 1000 })
    projectOptions.value = res.data.records
  } catch (error) {
    console.error('加载项目列表失败', error)
  }
}

// 加载缺陷列表
const loadDefects = async () => {
  try {
    const params = {
      title: searchForm.value.title,
      projectId: searchForm.value.projectId
    }
    const res = await getDefectList(params)
    defectList.value = res.data.records
  } catch (error) {
    console.error('加载缺陷列表失败', error)
  }
}

// 查看缺陷的历史分析记录
const viewHistory = async (defect) => {
  historyDefect.value = defect
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    const res = await getAnalysisHistory()
    const all = res.data || []
    historyList.value = all
      .filter(item => item.defectId === defect.id)
      .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  } catch (error) {
    console.error('加载历史记录失败', error)
    ElMessage.error('加载历史记录失败')
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

// 展示分析详情（统一入口）
const showAnalysisDetail = (record) => {
  try {
    const result = JSON.parse(record.analysisResult || '{}')
    currentAnalysis.value = {
      problemLocation: result.problemLocation || '',
      fixSuggestions: record.fixSuggestions || result.fixSuggestion || '',
      precautions: result.precautions || '',
      affectedFiles: record.affectedFiles || ''
    }
  } catch (e) {
    currentAnalysis.value = {
      problemLocation: '',
      fixSuggestions: record.analysisResult || record.fixSuggestions || '',
      precautions: '',
      affectedFiles: record.affectedFiles || ''
    }
  }
  detailDialogVisible.value = true
}

// 查看批量分析详情（复用统一对话框）
const viewBatchDetail = async (defectId) => {
  try {
    const res = await getAnalysisHistory()
    const records = res.data
    const analysis = records
      .filter(r => r.defectId === defectId)
      .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))[0]
    if (!analysis) {
      ElMessage.warning('未找到该缺陷的分析记录')
      return
    }
    // 为了显示缺陷标题，从 batchResults 中获取
    const batchItem = batchResults.value.find(r => r.defectId === defectId)
    if (batchItem) analysis.defectTitle = batchItem.defectTitle
    showAnalysisDetail(analysis)
  } catch (error) {
    console.error('获取详情失败', error)
    ElMessage.error('获取详情失败')
  }
}

// 加载文件树
const loadFileTree = async (projectId) => {
  if (!projectId) return
  isLoadingFileTree.value = true
  try {
    const res = await getFileTree(projectId)
    fileTree.value = res.data || []
    if (fileTree.value.length === 0) {
      ElMessage.warning('该项目的Git仓库暂无代码文件，请检查仓库配置')
    }
  } catch (error) {
    console.error('加载文件树失败', error)
    ElMessage.error('加载代码文件树失败，请检查项目Git配置')
    fileTree.value = []
  } finally {
    isLoadingFileTree.value = false
  }
}

// 全选/取消全选
const selectAllText = computed(() => {
  const allKeys = getAllFileKeys()
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  return checkedKeys.length === allKeys.length && allKeys.length > 0 ? '取消全选' : '全选'
})
const selectAllIcon = computed(() => {
  const allKeys = getAllFileKeys()
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  return checkedKeys.length === allKeys.length && allKeys.length > 0 ? 'Check' : 'Select'
})
const getAllFileKeys = () => {
  const keys = []
  const traverse = (nodes) => {
    nodes.forEach(node => {
      if (node.type === 'file') keys.push(node.path)
      if (node.children) traverse(node.children)
    })
  }
  traverse(fileTree.value)
  return keys
}
const toggleSelectAll = () => {
  const allKeys = getAllFileKeys()
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  if (checkedKeys.length === allKeys.length && allKeys.length > 0) {
    treeRef.value?.setCheckedKeys([])
  } else {
    treeRef.value?.setCheckedKeys(allKeys)
  }
  handleTreeCheck()
}

// 树节点选择处理
const handleTreeCheck = () => {
  selectedFiles.value = treeRef.value?.getCheckedKeys() || []
}

// 分析单个缺陷
const analyzeSingle = async (defect) => {
  currentDefect.value = defect
  analysisResult.value = null
  isAnalyzing.value = true
  
  if (currentProjectId.value !== defect.projectId) {
    currentProjectId.value = defect.projectId
    await loadFileTree(defect.projectId)
  }
  
  const filePaths = treeRef.value?.getCheckedKeys() || []
  
  try {
    const res = await analyzeSingleDefect({
      defectId: defect.id,
      filePaths: filePaths
    })
    analysisResult.value = res.data
    ElMessage.success('分析完成')
  } catch (error) {
    console.error('分析失败', error)
    ElMessage.error('分析失败：' + (error.message || '未知错误'))
  } finally {
    isAnalyzing.value = false
  }
}

// 批量分析
const startBatchAnalysis = async () => {
  if (selectedDefects.value.length === 0) return
  
  batchDialogVisible.value = true
  batchProgress.value = 0
  batchStatus.value = ''
  batchResults.value = []
  
  if (progressTimer) clearInterval(progressTimer)
  progressTimer = setInterval(() => {
    if (batchProgress.value < 90) {
      const increment = Math.floor(Math.random() * 7) + 2
      batchProgress.value = Math.min(batchProgress.value + increment, 90)
    }
  }, 200)

  const filePaths = treeRef.value?.getCheckedKeys() || []
  const defectIds = selectedDefects.value.map(d => d.id)
  
  try {
    const res = await analyzeBatchDefects({
      defectIds: defectIds,
      filePaths: filePaths
    })
    batchResults.value = res.data
    batchProgress.value = 100
    batchStatus.value = 'success'
    ElMessage.success(`批量分析完成，共分析 ${defectIds.length} 个缺陷`)
  } catch (error) {
    console.error('批量分析失败', error)
    batchStatus.value = 'failed'
    ElMessage.error('批量分析失败')
  } finally {
    if (progressTimer) {
      clearInterval(progressTimer)
      progressTimer = null
    }
  }
}

// 关键词搜索（预留）
const searchCodeByKeyword = async () => {
  const { value: keyword } = await ElMessageBox.prompt('请输入搜索关键词', '代码搜索', {
    confirmButtonText: '搜索',
    cancelButtonText: '取消',
    inputPlaceholder: '类名、方法名或关键词'
  })
  if (keyword) {
    ElMessage.info('搜索功能开发中，敬请期待')
  }
}

const stopProgressTimer = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 处理表格选择
const handleSelectionChange = (selection) => {
  selectedDefects.value = selection
}

// 加载项目信息（路由参数）
const loadProject = async () => {
  if (projectId.value) {
    const res = await getProject(projectId.value)
    projectName.value = res.data.projectName
  }
}

// 监听项目选择变化，加载文件树
watch(() => searchForm.value.projectId, async (newProjectId) => {
  if (newProjectId) {
    currentProjectId.value = newProjectId
    await loadFileTree(newProjectId)
  } else {
    fileTree.value = []
    currentProjectId.value = null
  }
})

// 初始化
onMounted(() => {
  loadProjects()
  loadProject()
  loadDefects()
})
</script>

<style scoped>
/* 原有样式保持不变，只增加/修改以下部分 */
.ai-analysis-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

/* ... 中间省略与原代码相同的样式 ... */
/* 您原有样式全部保留，只需确保最后添加了详情对话框的样式（已在原样式中存在） */
.detail-content-wrapper {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}
.detail-section {
  margin-bottom: 20px;
}
.detail-section h4 {
  margin: 0 0 8px 0;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.detail-text {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
  color: #2c3e50;
}
.detail-text :deep(pre) {
  background-color: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}
.detail-text :deep(code) {
  font-family: 'Monaco', 'Menlo', monospace;
  background-color: #f0f2f5;
  padding: 2px 4px;
  border-radius: 4px;
  color: #e96900;
}
</style>