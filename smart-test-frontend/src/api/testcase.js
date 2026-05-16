import request from '@/utils/request'

// 获取测试用例列表（分页）
export function getTestCaseList(params) {
  return request({
    url: '/test-cases',
    method: 'get',
    params
  })
}

// 获取单个用例
export function getTestCase(id) {
  return request({
    url: `/test-cases/${id}`,
    method: 'get'
  })
}

// 创建用例
export function createTestCase(data) {
  return request({
    url: '/test-cases',
    method: 'post',
    data
  })
}

// 更新用例
export function updateTestCase(id, data) {
  return request({
    url: `/test-cases/${id}`,
    method: 'put',
    data
  })
}

// 删除用例
export function deleteTestCase(id) {
  return request({
    url: `/test-cases/${id}`,
    method: 'delete'
  })
}

// 批量删除用例
export function deleteTestCasesBatch(ids) {
  return request({
    url: '/test-cases/batch',
    method: 'delete',
    data: ids
  })
}

// AI生成测试用例
export function aiGenerateTestCases(data) {
  return request({
    url: '/test-cases/ai-generate',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 批量创建用例
export function batchCreateTestCase(data) {
  return request({
    url: '/test-cases/batch-create',
    method: 'post',
    data
  })
}