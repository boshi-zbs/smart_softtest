<template>
  <div>
    <h2>测试仪表盘</h2>

    <!-- 统计卡片（保持不变） -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">📁</div>
          <div class="stat-value">{{ stats.totalProjects || 0 }}</div>
          <div class="stat-label">总项目</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">📋</div>
          <div class="stat-value">{{ stats.totalRequirements || 0 }}</div>
          <div class="stat-label">总需求</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">🧪</div>
          <div class="stat-value">{{ stats.totalTestCases || 0 }}</div>
          <div class="stat-label">总用例</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon">🐞</div>
          <div class="stat-value">{{ stats.totalDefects || 0 }}</div>
          <div class="stat-label">总缺陷</div>
          <div class="stat-sub">未关闭：{{ stats.openDefects || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 项目摘要列表 + 缺陷分布（左右两栏） -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="14">
        <el-card>
          <template #header>
            <span>项目进度摘要</span>
          </template>
          <el-table :data="stats.projectSummaries" border style="width: 100%">
            <el-table-column prop="projectName" label="项目名称" min-width="150" />
            <el-table-column prop="projectKey" label="标识" width="100" />
            <el-table-column prop="totalCases" label="总用例" width="80" />
            <el-table-column prop="executedCases" label="已执行" width="80" />
            <el-table-column label="通过率" width="100">
              <template #default="{ row }">
                <el-progress :percentage="row.passRate || 0" :stroke-width="8" :show-text="false" />
                <span style="font-size: 12px;">{{ (row.passRate || 0).toFixed(1) }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="activeDefects" label="活跃缺陷" width="90" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="viewProjectReport(row.projectId)">查看报告</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>缺陷状态分布</span>
          </template>
          <v-chart class="small-chart" :option="defectStatusOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 执行趋势（近7天） -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>近7天测试执行趋势</span>
          </template>
          <v-chart class="trend-chart" :option="trendOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>最近活动</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, idx) in stats.recentActivities"
              :key="idx"
              :timestamp="item.createTime"
              placement="top"
            >
              <div>
                <span style="font-weight: bold;">{{ item.operatorName }}</span>
                {{ item.description }}
              </div>
            </el-timeline-item>
            <el-timeline-item v-if="!stats.recentActivities || stats.recentActivities.length === 0">
              暂无活动
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats } from '@/api/report'

const router = useRouter()
const stats = ref({})

const fetchDashboard = async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
    console.log('仪表盘数据:', stats.value)
  } catch (error) {
    console.error('获取仪表盘数据失败', error)
  }
}

onMounted(() => {
  fetchDashboard()
})

const viewProjectReport = (projectId) => {
  router.push(`/admin/reports/project/${projectId}`)
}

// 缺陷分布饼图（从后端获取）
const defectStatusOption = computed(() => {
  const data = stats.value.defectDistribution || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '缺陷状态',
        type: 'pie',
        radius: '50%',
        data: Object.entries(data).map(([name, value]) => ({ name, value })),
        emphasis: { itemStyle: { shadowBlur: 10 } }
      }
    ]
  }
})

// 趋势图（近7天执行用例数 & 新增缺陷）
const trendOption = computed(() => {
  const dailyStats = stats.value.trend?.dailyStats || []
  const dates = dailyStats.map(d => d.date)
  const executed = dailyStats.map(d => d.executedCases || 0)
  const newDefects = dailyStats.map(d => d.newDefects || 0)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['执行用例数', '新增缺陷'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '执行用例数', type: 'line', data: executed, smooth: true, color: '#409EFF' },
      { name: '新增缺陷', type: 'line', data: newDefects, smooth: true, color: '#F56C6C' }
    ]
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.2);
}
.stat-icon {
  font-size: 48px;
  margin-bottom: 10px;
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
.small-chart {
  height: 260px;
}
.trend-chart {
  height: 350px;
}
</style>