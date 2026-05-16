<template>
  <div v-loading="loading">
    <div class="header">
      <el-button @click="goBack">返回</el-button>
      <h2>项目报告 - {{ report.projectName }}</h2>
    </div>

    <!-- 需求覆盖率卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ report.totalRequirements || 0 }}</div>
          <div class="stat-label">总需求</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ report.coveredRequirements || 0 }}</div>
          <div class="stat-label">已覆盖需求</div>
          <div class="stat-sub">覆盖率 {{ (report.requirementCoverage || 0).toFixed(1) }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ report.executionStats?.totalCases || 0 }}</div>
          <div class="stat-label">总用例</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ report.executionStats?.executedCases || 0 }}</div>
          <div class="stat-label">已执行</div>
          <div class="stat-sub">进度 {{ (report.executionStats?.progress || 0).toFixed(1) }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 执行结果分布 + 缺陷分布 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>测试执行结果分布</template>
          <v-chart class="chart" :option="executionPieOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>缺陷状态分布</template>
          <v-chart class="chart" :option="defectStatusOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 缺陷严重程度 + 优先级 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>缺陷严重程度</template>
          <v-chart class="small-chart" :option="defectSeverityOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>缺陷优先级</template>
          <v-chart class="small-chart" :option="defectPriorityOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 缺陷趋势 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>缺陷新增趋势</template>
          <v-chart class="trend-chart" :option="defectTrendOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 自动化测试统计（若后端提供） -->
    <el-row style="margin-top: 20px;" v-if="autoTestStats">
      <el-col :span="24">
        <el-card>
          <template #header>自动化测试统计</template>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="auto-stat">总自动化用例：{{ autoTestStats.totalAutoCases || 0 }}</div>
            </el-col>
            <el-col :span="6">
              <div class="auto-stat">已执行：{{ autoTestStats.executedCases || 0 }}</div>
            </el-col>
            <el-col :span="6">
              <div class="auto-stat">通过率：{{ (autoTestStats.passRate || 0).toFixed(1) }}%</div>
            </el-col>
          </el-row>
          <v-chart class="small-chart" :option="autoTestStatusOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectReport } from '@/api/report'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const report = ref({})
const autoTestStats = ref(null)  // 若后端返回自动化统计

const fetchReport = async () => {
  loading.value = true
  try {
    const res = await getProjectReport(route.params.id)
    report.value = res.data
    // 如果后端在同一个接口中返回了 autoTestStats，则赋值
    if (res.data.autoTestStats) {
      autoTestStats.value = res.data.autoTestStats
    }
  } catch (error) {
    console.error('获取报告失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

const goBack = () => router.back()

// 执行结果饼图
const executionPieOption = computed(() => {
  const exec = report.value.executionStats || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: exec.passed || 0, name: '通过' },
        { value: exec.failed || 0, name: '失败' },
        { value: exec.blocked || 0, name: '阻塞' },
        { value: exec.skipped || 0, name: '跳过' }
      ]
    }]
  }
})

// 缺陷状态饼图
const defectStatusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: Object.entries(report.value.defectStats?.byStatus || {}).map(([n, v]) => ({ name: n, value: v }))
  }]
}))

const defectSeverityOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: Object.entries(report.value.defectStats?.bySeverity || {}).map(([n, v]) => ({ name: n, value: v }))
  }]
}))

const defectPriorityOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: Object.entries(report.value.defectStats?.byPriority || {}).map(([n, v]) => ({ name: n, value: v }))
  }]
}))

const defectTrendOption = computed(() => {
  const trend = report.value.defectStats?.trend || []
  const dates = trend.map(t => t.date)
  const counts = trend.map(t => t.count)
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: counts, color: '#409EFF' }]
  }
})

// 自动化测试状态分布（若有）
const autoTestStatusOption = computed(() => {
  const dist = autoTestStats.value?.statusDistribution || {}
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: Object.entries(dist).map(([n, v]) => ({ name: n, value: v }))
    }]
  }
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
}
.stat-label {
  color: #909399;
  margin-top: 5px;
}
.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.chart {
  height: 300px;
}
.small-chart {
  height: 200px;
}
.trend-chart {
  height: 350px;
}
.auto-stat {
  font-size: 16px;
  padding: 10px;
  text-align: center;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>