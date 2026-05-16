import request from '@/utils/request'

// 获取自动化用例列表（分页）
export function getAutoTestCaseList(params) {
  return request({
    url: '/auto-test/cases',
    method: 'get',
    params
  })
}

// 获取单个用例详情
export function getAutoTestCase(id) {
  return request({
    url: `/auto-test/cases/${id}`,
    method: 'get'
  })
}

// 创建用例
export function createAutoTestCase(data) {
  return request({
    url: '/auto-test/cases',
    method: 'post',
    data
  })
}

// 更新用例
export function updateAutoTestCase(id, data) {
  return request({
    url: `/auto-test/cases/${id}`,
    method: 'put',
    data
  })
}

// 删除用例
export function deleteAutoTestCase(id) {
  return request({
    url: `/auto-test/cases/${id}`,
    method: 'delete'
  })
}

// 执行用例
// 执行自动化用例（支持传 body 参数）
export const executeAutoTestCase = (id, data) => {
  return request({
    url: `/auto-test-cases/${id}/execute`,
    method: 'post',
    data: data  // 改为 data 而不是 params
  })
}
// 获取执行详情
export function getAutoTestExecution(id) {
  return request({
    url: `/auto-test/executions/${id}`,
    method: 'get'
  })
}
// 批量执行用例
export function batchExecuteAutoTestCase(ids) {
  return request({
    url: '/auto-test/cases/batch-execute',
    method: 'post',
    data: ids
  })
}
// 获取用例最近一次失败的执行记录
export function getLastFailedExecution(caseId) {
  return request({
    url: `/auto-test/cases/${caseId}/last-failed-execution`,
    method: 'get'
  })
}