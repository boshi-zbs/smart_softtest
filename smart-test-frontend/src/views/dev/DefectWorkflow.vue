<template>
  <div>
    <!-- 查询栏 -->
    <div class="action-bar">
      <el-form :inline="true" :model="searchForm" class="search-form">
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
            <el-option label="驳回" value="驳回" />
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
      </el-form>
      <div class="action-group">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </div>

    <!-- 缺陷列表 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchDefects"
      :searchParams="searchForm"
    >
      <template #columns>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="id" label="ID" width="80" />
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
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 处理缺陷对话框（包含详情、状态变更、评论） -->
    <el-dialog title="缺陷处理" v-model="dialogVisible" width="800px" @close="handleDialogClose">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="标题">{{ currentDefect.title }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ currentDefect.projectName }}</el-descriptions-item>
            <el-descriptions-item label="严重程度">{{ currentDefect.severity }}</el-descriptions-item>
            <el-descriptions-item label="优先级">{{ currentDefect.priority }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(currentDefect.status)">{{ currentDefect.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="指派人">{{ currentDefect.assigneeName || '未指派' }}</el-descriptions-item>
            <el-descriptions-item label="报告人">{{ currentDefect.reporterName }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentDefect.createTime }}</el-descriptions-item>
            <el-descriptions-item label="发现版本">{{ currentDefect.foundVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="修复版本">{{ currentDefect.fixedVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentDefect.description }}</el-descriptions-item>
          </el-descriptions>
          <!-- 附件展示 -->
          <div v-if="currentDefect.attachments && currentDefect.attachments.length" style="margin-top: 20px;">
            <h4>附件</h4>
            <div style="display: flex; flex-wrap: wrap; gap: 12px;">
              <div v-for="(att, index) in currentDefect.attachments" :key="index" style="width: 100px; text-align: center;">
                <el-image
                  v-if="isImage(att.fileType)"
                  :src="att.filePath"
                  :preview-src-list="[att.filePath]"
                  fit="cover"
                  style="width: 100px; height: 100px; border-radius: 4px;"
                />
                <div v-else style="width: 100px; height: 100px; border: 1px solid #dcdfe6; border-radius: 4px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
                  <el-icon size="40"><Document /></el-icon>
                  <a :href="att.filePath" target="_blank" style="font-size: 12px;">{{ att.fileName }}</a>
                </div>
                <div style="font-size: 12px; margin-top: 4px; word-break: break-all;">{{ att.fileName }}</div>
              </div>
            </div>
          </div>

          <!-- 状态变更操作区 -->
          <div style="margin-top: 20px;">
            <span style="font-weight: bold;">操作：</span>
            <el-button-group>
              <el-button
                v-if="['新建', '已指派'].includes(currentDefect.status)"
                type="warning"
                @click="openStatusDialog('修复中')"
              >开始修复</el-button>
              <el-button
                v-if="currentDefect.status === '修复中'"
                type="success"
                @click="openStatusDialog('已修复')"
              >修复完成</el-button>
              <el-button
                v-if="['新建', '已指派', '修复中'].includes(currentDefect.status)"
                type="info"
                @click="openStatusDialog('驳回')"
              >驳回</el-button>
              <el-button
                v-if="currentDefect.status === '验证中'"
                type="primary"
                @click="openStatusDialog('已关闭')"
              >确认关闭</el-button>
            </el-button-group>
            <el-button type="primary" plain @click="openAssignDialog">重新指派</el-button>
          </div>
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
        </span>
      </template>
    </el-dialog>

    <!-- 状态变更确认对话框（可填写备注） -->
    <el-dialog title="状态变更" v-model="statusDialogVisible" width="400px" append-to-body>
      <el-form :model="statusForm">
        <el-form-item label="备注">
          <el-input v-model="statusForm.comment" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="statusDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmStatusChange">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重新指派对话框 -->
    <el-dialog title="重新指派" v-model="assignDialogVisible" width="400px" append-to-body>
      <el-form :model="assignForm">
        <el-form-item label="指派给">
          <el-select v-model="assignForm.assigneeId" placeholder="请选择用户" style="width:100%">
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="item.username + (item.realName ? ' (' + item.realName + ')' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.comment" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assignDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAssign">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getDefectList, getDefect, getDefectComments, addDefectComment, changeDefectStatus, updateDefect } from '@/api/defect'
import { getUserList } from '@/api/user'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { Document } from '@element-plus/icons-vue'

const store = useStore()

const route = useRoute()
const defectIdFromUrl = route.params.id 
onMounted(async () => {
  await fetchUsers()
  
  // 如果URL中包含缺陷ID，则自动加载详情并打开对话框
  if (defectIdFromUrl) {
    try {
      const res = await getDefect(defectIdFromUrl)
      currentDefect.value = res.data
      await loadComments(defectIdFromUrl)
      dialogVisible.value = true  // 打开处理对话框
       if (route.query.activeTab === 'ai') {
        activeTab.value = 'ai'
      }
    } catch (error) {
      console.error('加载缺陷失败', error)
      ElMessage.error('加载缺陷失败')
    }
  }
})

const tableRef = ref()
const searchForm = ref({
  title: '',
  status: '',
  severity: ''
})

// 用户选项（用于重新指派）
const userOptions = ref([])
const fetchUsers = async () => {
  try {
    const res = await getUserList({ page: 1, size: 1000 })
    userOptions.value = res.data.records
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}
onMounted(fetchUsers)

// 获取缺陷列表（默认只显示指派给当前用户的）
const fetchDefects = async (params) => {
  params.assigneeId = store.state.user?.id  // 添加当前用户ID
  const res = await getDefectList(params)
  return res
}

const handleSearch = () => tableRef.value?.triggerSearch()
const resetSearch = () => {
  searchForm.value = { title: '', status: '', severity: '' }
  tableRef.value?.triggerSearch()
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
    '驳回': 'danger'
  }
  return map[status] || ''
}

// 处理缺陷对话框
const dialogVisible = ref(false)
const activeTab = ref('info')
const currentDefect = ref({})
const commentList = ref([])
const newComment = ref('')


const loadComments = async (defectId) => {
  try {
    const res = await getDefectComments(defectId)
    commentList.value = res.data
  } catch (error) {
    console.error('加载评论失败', error)
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await addDefectComment(currentDefect.value.id, newComment.value)
    ElMessage.success('评论成功')
    newComment.value = ''
    loadComments(currentDefect.value.id)
  } catch (error) {
    console.error('评论失败', error)
  }
}

const handleDialogClose = () => {
  activeTab.value = 'info'
  currentDefect.value = {}
  commentList.value = []
  newComment.value = ''
}

// 状态变更相关
const statusDialogVisible = ref(false)
const statusForm = ref({
  newStatus: '',
  comment: ''
})

const openStatusDialog = (newStatus) => {
  statusForm.value = { newStatus, comment: '' }
  statusDialogVisible.value = true
}

const confirmStatusChange = async () => {
  try {
    await changeDefectStatus(currentDefect.value.id, statusForm.value.newStatus, statusForm.value.comment)
    ElMessage.success('状态更新成功')
    statusDialogVisible.value = false
    // 刷新详情
    const res = await getDefect(currentDefect.value.id)
    currentDefect.value = res.data
    // 刷新评论
    loadComments(currentDefect.value.id)
    // 刷新列表
    tableRef.value?.refresh()
  } catch (error) {
    console.error('状态变更失败', error)
  }
}

// 重新指派
const assignDialogVisible = ref(false)
const assignForm = ref({
  assigneeId: null,
  comment: ''
})

const openAssignDialog = () => {
  assignForm.value = {
    assigneeId: currentDefect.value.assigneeId,
    comment: ''
  }
  assignDialogVisible.value = true
}

const confirmAssign = async () => {
  if (!assignForm.value.assigneeId) {
    ElMessage.warning('请选择指派人')
    return
  }
  try {
    // 更新缺陷的指派人
    const updatedDefect = { ...currentDefect.value, assigneeId: assignForm.value.assigneeId }
    await updateDefect(currentDefect.value.id, updatedDefect)
    // 添加一条评论记录变更
    if (assignForm.value.comment) {
      await addDefectComment(currentDefect.value.id, assignForm.value.comment)
    }
    ElMessage.success('指派成功')
    assignDialogVisible.value = false
    // 刷新详情
    const res = await getDefect(currentDefect.value.id)
    currentDefect.value = res.data
    // 刷新评论
    loadComments(currentDefect.value.id)
    // 刷新列表
    tableRef.value?.refresh()
  } catch (error) {
    console.error('指派失败', error)
  }
}
// 在打开对话框后，根据路由参数设置 activeTab
const handleProcess = async (row) => {
  // 获取完整详情
  const res = await getDefect(row.id)
  currentDefect.value = res.data
  // 加载评论
  loadComments(row.id)
  dialogVisible.value = true
  // 如果路由参数中有 activeTab=ai，则切换到AI标签页
  if (route.query.activeTab === 'ai') {
    activeTab.value = 'ai'
  }
}
const isImage = (fileType) => {
  return fileType && fileType.startsWith('image/')
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
</style>