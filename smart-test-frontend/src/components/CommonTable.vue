<template>
  <div>
    <div v-if="$slots.searchForm" class="search-form" style="margin-bottom: 16px;">
      <slot name="searchForm" :searchParams="searchParams" />
    </div>

    <el-table
      v-loading="loading"
      :data="tableData"
      border
      style="width: 100%"
      row-key="id"
      stripe
      @selection-change="handleTableSelectionChange"
      :header-cell-style="{ background: '#f5f6f8', fontWeight: '500' }"
    >
      <slot name="columns" />
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
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
import { ref, defineProps, defineExpose, defineEmits, onMounted, watch } from 'vue'

const props = defineProps({
  fetchData: { type: Function, required: true },
  searchParams: { type: Object, default: () => ({}) },
  immediate: { type: Boolean, default: true }
})

const emit = defineEmits(['selection-change'])

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

const triggerSearch = () => {
  currentPage.value = 1
  loadData()
}

defineExpose({ refresh: loadData, triggerSearch })

const handleTableSelectionChange = (selection) => {
  emit('selection-change', selection)
}

// 监听 searchParams 变化，自动刷新表格
watch(() => props.searchParams, () => {
  triggerSearch()
}, { deep: true })

if (props.immediate) {
  onMounted(() => {
    loadData()
  })
}
</script>