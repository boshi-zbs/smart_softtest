<template>
  <div>
    <!-- 查询表单插槽（保留，但不再自动监听） -->
    <div v-if="$slots.searchForm" class="search-form" style="margin-bottom: 16px;">
      <slot name="searchForm" :searchParams="searchParams" />
    </div>

     <el-table
      v-loading="loading"
      :data="tableData"
      border
      style="width: 100%"
      row-key="id"
      @selection-change="handleTableSelectionChange"
    >
      <slot name="columns" />

    </el-table>
     <!-- 分页器（保持不变） -->
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineExpose,defineEmits, onMounted } from 'vue'


const props = defineProps({
  fetchData: { type: Function, required: true },
  searchParams: { type: Object, default: () => ({}) },
  immediate: { type: Boolean, default: true }
})

const emit = defineEmits(['selection-change'])  // 新增：定义事件

const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)


const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...props.searchParams
    }
    const res = await props.fetchData(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadData()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadData()
}

// 触发查询（重置到第一页并加载）
const triggerSearch = () => {
  currentPage.value = 1
  loadData()
}

defineExpose({ refresh: loadData, triggerSearch })


// 将表格选择变化事件传递给父组件
const handleTableSelectionChange = (selection) => {
  emit('selection-change', selection)
}

// 初始化加载
if (props.immediate) {
  onMounted(() => {
    loadData()
  })
}
</script>