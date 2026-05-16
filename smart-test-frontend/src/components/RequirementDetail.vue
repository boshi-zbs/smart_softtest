/* eslint-disable */
<template>
  <el-dialog title="需求详情" v-model="dialogVisible" width="600px" @close="handleClose">
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
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getRequirement } from '@/api/requirement'

const props = defineProps({
  modelValue: Boolean,
  requirementId: Number
})

const emit = defineEmits(['update:modelValue'])

const dialogVisible = ref(props.modelValue)
const requirement = ref({})
const loading = ref(false)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.requirementId) {
    loadRequirement()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const loadRequirement = async () => {
  loading.value = true
  try {
    const res = await getRequirement(props.requirementId)
    requirement.value = res.data
  } catch (error) {
    console.error('加载需求失败', error)
  } finally {
    loading.value = false
  }
}

const priorityText = (val) => {
  const map = { 1: '最高', 2: '高', 3: '中', 4: '低' }
  return map[val] || val
}
const priorityType = (val) => {
  const map = { 1: 'danger', 2: 'warning', 3: 'info', 4: '' }
  return map[val] || undefined
}
const statusType = (status) => {
  const map = { '待处理': 'info', '处理中': 'warning', '已完成': 'success', '已关闭': '' }
  return map[status] || undefined
}

const handleClose = () => {
  dialogVisible.value = false
}
</script>