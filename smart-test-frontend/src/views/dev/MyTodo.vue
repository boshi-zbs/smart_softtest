<template>
  <div>
    <CommonTable
      ref="tableRef"
      :fetchData="fetchTodos"
      :searchParams="{}"
    >
      <template #columns>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'requirement' ? 'info' : 'danger'">
              {{ row.type === 'requirement' ? '需求' : '缺陷' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 需求详情对话框 -->
    <RequirementDetail
      v-model="requirementDialogVisible"
      :requirementId="currentRequirementId"
    />

    <!-- 缺陷详情对话框 -->
    <DefectDetail
      v-model="defectDialogVisible"
      :defectId="currentDefectId"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CommonTable from '@/components/CommonTable.vue'
import RequirementDetail from '@/components/RequirementDetail.vue'
import DefectDetail from '@/components/DefectDetail.vue'
import { getMyTodoList } from '@/api/mytodo'
import { useRouter } from 'vue-router'
const router = useRouter()

const tableRef = ref()
const requirementDialogVisible = ref(false)
const defectDialogVisible = ref(false)
const currentRequirementId = ref(null)
const currentDefectId = ref(null)

const fetchTodos = async (params) => {
  const res = await getMyTodoList(params)
  console.log('待办列表返回原始数据:', res.data.records) // 调试
  return res
}

const handleView = (row) => {
   console.log('点击的row对象（完整）:', JSON.stringify(row, null, 2))
  if (row.type === 'requirement') {
    router.push(`/dev/requirements/${row.id}`)
  } else {
     // 跳转到缺陷处理工作流页面，传入缺陷ID
     
    router.push(`/dev/defects/${row.id}`)
  }
}
</script>