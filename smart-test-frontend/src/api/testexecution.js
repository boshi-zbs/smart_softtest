import request from '@/utils/request'

// 获取执行记录列表（分页）
export function getExecutionList(params) {
  return request({
    url: '/test-executions',
    method: 'get',
    params
  })
}

// 获取单条执行记录
export function getExecution(id) {
  return request({
    url: `/test-executions/${id}`,
    method: 'get'
  })
}

// 执行用例（创建执行记录）
export function executeCase(data) {
  return request({
    url: '/test-executions',
    method: 'post',
    data
  })
}

// 更新执行记录
export function updateExecution(id, data) {
  return request({
    url: `/test-executions/${id}`,
    method: 'put',
    data
  })
}

// 删除执行记录
export function deleteExecution(id) {
  return request({
    url: `/test-executions/${id}`,
    method: 'delete'
  })
}

// 获取计划统计信息
export function getPlanStatistics(planId) {
  return request({
    url: `/test-executions/statistics/plan/${planId}`,
    method: 'get'
  })
}
// 获取用例最近一次执行记录
export function getLatestExecution(caseId) {
  return request({
    url: `/test-executions/latest/${caseId}`,
    method: 'get'
  })
}