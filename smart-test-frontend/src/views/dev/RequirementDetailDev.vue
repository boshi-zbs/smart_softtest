<template>
  <div>
    <el-page-header @back="goBack" content="需求处理" />
    <el-card style="margin-top: 16px;">
      <template #header>
        <span>需求详情</span>
      </template>
      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="标题">{{ requirement.title }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ requirement.projectName }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="priorityType(requirement.priority)">{{ priorityText(requirement.priority) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(requirement.status)">{{ requirement.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">{{ requirement.assigneeName || '未指派' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ requirement.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ requirement.createTime }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ requirement.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div style="margin-top: 20px; text-align: right;">
        <el-button-group>
          <el-button
            v-if="requirement.status === '待处理'"
            type="warning"
            @click="changeStatus('处理中')"
          >开始处理</el-button>
          <el-button
            v-if="requirement.status === '处理中'"
            type="primary"
            @click="changeStatus('待测试')"
          >提交测试</el-button>
          <el-button
            v-if="requirement.status === '待测试'"
            type="info"
            disabled
          >已提交测试（等待验证）</el-button>
          <el-button
            v-if="requirement.status === '已完成'"
            type="success"
            disabled
          >已完成</el-button>
        </el-button-group>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage} from 'element-plus'
import { getRequirement, updateRequirement } from '@/api/requirement'
import { completeMessage } from '@/api/message'

const route = useRoute()
const router = useRouter()
const requirement = ref({})
const loading = ref(false)

const loadRequirement = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getRequirement(id)
    requirement.value = res.data
  } catch (error) {
    console.error('加载需求失败', error)
    ElMessage.error('加载需求失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRequirement()
  
  const messageId = route.query.messageId
  if (messageId) {
    completeMessage(messageId).catch(err => console.error('自动完成待办失败', err))
  }
})

const priorityText = (val) => {
  const map = { 1: '最高', 2: '高', 3: '中', 4: '低' }
  return map[val] || val
}
const priorityType = (val) => {
  const map = { 1: 'danger', 2: 'warning', 3: 'info', 4: '' }
  return map[val] || undefined
}
const statusType = (status) => {
  const map = {
    '待处理': 'info',
    '处理中': 'warning',
    '待测试': 'primary',
    '已完成': 'success',
    '已关闭': ''
  }
  return map[status] || undefined
}

const changeStatus = async (newStatus) => {
  try {
    await updateRequirement(requirement.value.id, {
      ...requirement.value,
      status: newStatus
    })
    ElMessage.success('状态更新成功')
    loadRequirement() // 刷新
  } catch (error) {
    console.error('状态更新失败', error)
    ElMessage.error('状态更新失败')
  }
}

const goBack = () => {
  router.back()
}
</script>