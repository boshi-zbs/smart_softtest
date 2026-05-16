<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">📋</div>
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">总日志数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">📈</div>
          <div class="stat-value">{{ todayCount }}</div>
          <div class="stat-label">今日新增</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 查询表单 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作人">
          <el-input v-model="searchForm.username" placeholder="用户名" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="searchForm.module" placeholder="全部" clearable style="width: 150px;">
            <el-option
              v-for="item in moduleOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operation" placeholder="全部" clearable style="width: 150px;">
            <el-option
              v-for="item in operationOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0">批量删除</el-button>
          <el-button type="warning" @click="handleClearAll">清空全部</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchLogs"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作类型" width="120" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targetId" label="对象ID" width="80" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === '成功' ? 'success' : 'danger'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue'
import { getOperationLogList, getLogModules, getLogOperations, deleteLogsBatch, clearAllLogs } from '@/api/operationlog'

const tableRef = ref()
const selectedRows = ref([])
const searchForm = ref({
  username: '',
  module: '',
  operation: '',
  startTime: null,
  endTime: null
})
const dateRange = ref([])
const moduleOptions = ref([])
const operationOptions = ref([])
const totalCount = ref(0)
const todayCount = ref(0)

const loadOptions = async () => {
  try {
    const [modulesRes, opsRes] = await Promise.all([
      getLogModules(),
      getLogOperations()
    ])
    moduleOptions.value = modulesRes.data
    operationOptions.value = opsRes.data
  } catch (error) {
    console.error('加载选项失败', error)
  }
}

const loadStatistics = async () => {
  try {
    const params = { page: 1, size: 1 }
    const res = await getOperationLogList(params)
    totalCount.value = res.data.total || 0
    const today = new Date().toISOString().slice(0,10)
    const todayRes = await getOperationLogList({ page: 1, size: 1, startTime: today })
    todayCount.value = todayRes.data.total || 0
  } catch (error) {
    console.error('获取统计失败', error)
  }
}

onMounted(() => {
  loadOptions()
  loadStatistics()
})

watch(dateRange, (val) => {
  if (val && val.length === 2) {
    searchForm.value.startTime = val[0]
    searchForm.value.endTime = val[1]
  } else {
    searchForm.value.startTime = null
    searchForm.value.endTime = null
  }
})

const fetchLogs = async (params) => {
  const res = await getOperationLogList(params)
  totalCount.value = res.data.total
  return res
}

const handleSearch = () => {
  tableRef.value?.triggerSearch()
  loadStatistics()
}

const resetSearch = () => {
  searchForm.value = {
    username: '',
    module: '',
    operation: '',
    startTime: null,
    endTime: null
  }
  dateRange.value = []
  tableRef.value?.triggerSearch()
  loadStatistics()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除ID为 ${row.id} 的日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteLogsBatch([row.id])
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
      loadStatistics()
    } catch (error) {
      console.error('删除失败', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteLogsBatch(ids)
      ElMessage.success('删除成功')
      tableRef.value?.refresh()
      selectedRows.value = []
      loadStatistics()
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}

const handleClearAll = () => {
  ElMessageBox.confirm('⚠️ 确认清空所有操作日志吗？此操作不可恢复！', '警告', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await clearAllLogs()
      ElMessage.success('清空成功')
      tableRef.value?.refresh()
      loadStatistics()
    } catch (error) {
      console.error('清空失败', error)
    }
  }).catch(() => {})
}
</script>

<style scoped>
.stat-card {
  text-align: center;
  cursor: default;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.2);
}
.stat-icon {
  font-size: 36px;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
}
.stat-label {
  color: #909399;
  margin-top: 5px;
}
.search-card {
  margin-bottom: 16px;
}
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
:deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}
</style>