<template>
  <div>
    <!-- 查询条件（可选） -->
    <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px;">
      <el-form-item label="标题">
        <el-input v-model="searchForm.title" placeholder="标题" clearable style="width: 200px;" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 150px;">
          <el-option label="待处理" value="待处理" />
          <el-option label="处理中" value="处理中" />
          <el-option label="待测试" value="待测试" />
          <el-option label="已完成" value="已完成" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <CommonTable
      ref="tableRef"
      :fetchData="fetchRequirements"
      :searchParams="searchForm"
    >
      <template #columns>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="priority" label="优先级">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)">{{ priorityText(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CommonTable from '@/components/CommonTable.vue'
import { getRequirementList } from '@/api/requirement'
import { useStore } from 'vuex'

const router = useRouter()
const tableRef = ref()
const searchForm = ref({
  title: '',
  status: ''
})

const fetchRequirements = async (params) => {
  const store = useStore()
  const user = store.state.user
  if (user && user.id) {
    params.assigneeId = user.id
  }
  const res = await getRequirementList(params)
  return res
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
  const map = {
    '待处理': 'info',
    '处理中': 'warning',
    '待测试': 'primary',
    '已完成': 'success',
    '已关闭': ''
  }
  return map[status] || undefined
}

const handleSearch = () => tableRef.value?.triggerSearch()
const resetSearch = () => {
  searchForm.value = { title: '', status: '' }
  tableRef.value?.triggerSearch()
}

const handleProcess = (row) => {
  router.push(`/dev/requirements/${row.id}`)
}
</script>