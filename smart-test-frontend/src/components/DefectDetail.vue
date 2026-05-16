/* eslint-disable */
<template>
  <el-dialog title="缺陷详情" v-model="dialogVisible" width="700px" @close="handleClose">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border v-loading="loading">
          <el-descriptions-item label="标题">{{ defect.title }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ defect.projectName }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">{{ defect.severity }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ defect.priority }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(defect.status)">{{ defect.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指派人">{{ defect.assigneeName || '未指派' }}</el-descriptions-item>
          <el-descriptions-item label="报告人">{{ defect.reporterName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ defect.createTime }}</el-descriptions-item>
          <el-descriptions-item label="发现版本">{{ defect.foundVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="修复版本">{{ defect.fixedVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联用例">{{ defect.testCaseTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联需求">{{ defect.requirementTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ defect.description }}</el-descriptions-item>
        </el-descriptions>
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
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="goToWorkflow">处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDefect, getDefectComments, addDefectComment } from '@/api/defect'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  defectId: Number
})

const emit = defineEmits(['update:modelValue'])
const router = useRouter()

const dialogVisible = ref(props.modelValue)
const activeTab = ref('info')
const defect = ref({})
const loading = ref(false)
const commentList = ref([])
const newComment = ref('')

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.defectId) {
    loadDefect()
    loadComments()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const loadDefect = async () => {
  loading.value = true
  try {
    const res = await getDefect(props.defectId)
    defect.value = res.data
  } catch (error) {
    console.error('加载缺陷失败', error)
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  try {
    const res = await getDefectComments(props.defectId)
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
    await addDefectComment(props.defectId, newComment.value)
    ElMessage.success('评论成功')
    newComment.value = ''
    loadComments()
  } catch (error) {
    console.error('评论失败', error)
  }
}

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

const goToWorkflow = () => {
  dialogVisible.value = false
  router.push(`/dev/defects/${props.defectId}`)
}

const handleClose = () => {
  dialogVisible.value = false
}
</script>