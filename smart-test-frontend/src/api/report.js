import request from '@/utils/request'

export function getDashboardStats() {
  return request({
    url: '/reports/dashboard',
    method: 'get'
  })
}

export function getProjectReport(projectId) {
  return request({
    url: `/reports/project/${projectId}`,
    method: 'get'
  })
}

// 新增：带时间范围的项目报告
export function getProjectReportWithDateRange(projectId, params) {
  return request({
    url: `/reports/project/${projectId}/detail`,
    method: 'get',
    params
  })
}

// 新增：自动化测试统计
export function getAutoTestStats(projectId) {
  return request({
    url: `/reports/project/${projectId}/auto-test`,
    method: 'get'
  })
}
