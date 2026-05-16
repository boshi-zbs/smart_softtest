import request from '@/utils/request'

// 获取测试计划列表（分页）
export function getTestPlanList(params) {
  return request({
    url: '/test-plans',
    method: 'get',
    params
  })
}

// 获取单个计划
export function getTestPlan(id) {
  return request({
    url: `/test-plans/${id}`,
    method: 'get'
  })
}

// 获取计划关联的用例详情列表（用于任务执行）
export function getPlanCases(planId) {
  return request({
    url: `/test-plans/${planId}/cases`,
    method: 'get'
  })
}

// 获取计划关联的用例ID列表（用于关联用例对话框）
export function getPlanCaseIds(planId) {
  return request({
    url: `/test-plans/${planId}/cases/ids`,
    method: 'get'
  })
}
// // 获取计划关联的用例ID列表
// export function getPlanCaseIds(planId) {
//   return request({
//     url: `/test-plans/${planId}/cases`,
//     method: 'get'
//   })
// }

// 创建计划
export function createTestPlan(data) {
  return request({
    url: '/test-plans',
    method: 'post',
    data
  })
}

// 更新计划
export function updateTestPlan(id, data) {
  return request({
    url: `/test-plans/${id}`,
    method: 'put',
    data
  })
}

// 删除计划
export function deleteTestPlan(id) {
  return request({
    url: `/test-plans/${id}`,
    method: 'delete'
  })
}

// 批量删除计划
export function deleteTestPlansBatch(ids) {
  return request({
    url: '/test-plans/batch',
    method: 'delete',
    data: ids
  })
}

// 添加用例到计划
export function addCaseToPlan(planId, caseId) {
  return request({
    url: `/test-plans/${planId}/cases/${caseId}`,
    method: 'post'
  })
}

// 从计划移除用例
export function removeCaseFromPlan(planId, caseId) {
  return request({
    url: `/test-plans/${planId}/cases/${caseId}`,
    method: 'delete'
  })
}

