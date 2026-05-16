<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" content="缺陷详情" />
    <el-card style="margin-top: 20px;">
      <el-descriptions :column="2" border>
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
       <!-- 附件展示区域 -->
      <div v-if="defect.attachments && defect.attachments.length" style="margin-top: 20px;">
        <h4>附件</h4>
        <div style="display: flex; flex-wrap: wrap; gap: 12px;">
          <div v-for="(att, index) in defect.attachments" :key="index" style="width: 100px; text-align: center;">
            <el-image
              v-if="isImage(att.fileType)"
              :src="att.filePath"
              :preview-src-list="[att.filePath]"
              fit="cover"
              style="width: 100px; height: 100px; border-radius: 4px;"
            />
            <div v-else style="width: 100px; height: 100px; border: 1px solid #dcdfe6; border-radius: 4px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
              <el-icon size="40"><Document /></el-icon>
              <a :href="att.filePath" target="_blank" style="font-size: 12px; word-break: break-all;">{{ att.fileName }}</a>
            </div>
            <div style="font-size: 12px; margin-top: 4px; word-break: break-all;">{{ att.fileName }}</div>
          </div>
        </div>
      </div>
      <!-- 评论区域 -->
      <div style="margin-top: 30px;">
        <h4>评论历史</h4>
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

        <!-- 添加评论 -->
        <div style="margin-top: 20px;">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="请输入评论..."
          />
          <el-button type="primary" style="margin-top: 10px;" @click="submitComment">发表评论</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDefect, getDefectComments, addDefectComment } from '@/api/defect'
import { ElMessage } from 'element-plus'
import { completeMessage } from '@/api/message'
import { Document } from '@element-plus/icons-vue'
const route = useRoute()
const router = useRouter()
const defectId = route.params.id

const loading = ref(false)
const defect = ref({})
const commentList = ref([])
const newComment = ref('')

const fetchDefect = async () => {
  loading.value = true
  try {
    const res = await getDefect(defectId)
    defect.value = res.data
  } catch (error) {
    console.error('加载缺陷失败', error)
    ElMessage.error('加载缺陷失败')
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
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
    await addDefectComment(defectId, newComment.value)
    ElMessage.success('评论成功')
    newComment.value = ''
    fetchComments()
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

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchDefect()
  fetchComments()

   // 自动完成待办消息
  const messageId = route.query.messageId
  if (messageId) {
    completeMessage(messageId).catch(err => console.error('自动完成待办失败', err))
  }
})
// 判断文件类型是否为图片
const isImage = (fileType) => {
  return fileType && fileType.startsWith('image/')
}
</script>