import request from '@/utils/request'

// 模块
export function getModules(projectId) { return request({ url: '/api-tester/modules', params: { projectId } }) }
export function addModule(data) { return request({ url: '/api-tester/module', method: 'post', data }) }
export function updateModule(data) { return request({ url: '/api-tester/module', method: 'put', data }) }
export function deleteModule(id) { return request({ url: `/api-tester/module/${id}`, method: 'delete' }) }

// 接口
export function getApis(params) { return request({ url: '/api-tester/apis', params }) }
export function getApi(id) { return request({ url: `/api-tester/api/${id}` }) }
export function addApi(data) { return request({ url: '/api-tester/api', method: 'post', data }) }
export function updateApi(data) { return request({ url: '/api-tester/api', method: 'put', data }) }
export function deleteApi(id) { return request({ url: `/api-tester/api/${id}`, method: 'delete' }) }

// 测试用例
export function getTestCases(apiId) { return request({ url: '/api-tester/test-cases', params: { apiId } }) }
export function addTestCase(data) { return request({ url: '/api-tester/test-case', method: 'post', data }) }
export function updateTestCase(data) { return request({ url: '/api-tester/test-case', method: 'put', data }) }
export function deleteTestCase(id) { return request({ url: `/api-tester/test-case/${id}`, method: 'delete' }) }
export function executeTestCase(caseId) { return request({ url: `/api-tester/test-case/${caseId}/execute`, method: 'post' }) }
export function getExecutions(caseId) { return request({ url: `/api-tester/executions/${caseId}` }) }
export function batchExecuteTestCases(caseIds) { return request({ url: '/api-tester/test-cases/batch-execute', method: 'post', data: caseIds }) }
export function batchDeleteTestCases(caseIds) { return request({ url: '/api-tester/test-cases/batch-delete', method: 'delete', data: caseIds }) }
export function getLastFailedExecution(caseId) { return request({ url: `/api-tester/test-case/${caseId}/last-failed-execution` }) }

// AI
export function generateFromDoc(data) { return request({ url: '/api-tester/ai-generate-from-doc', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } }) }

export function getEnv(projectId) {
  return request({ url: `/api-tester/env/${projectId}`, method: 'get' })
}

export function saveEnv(data) {
  return request({ url: '/api-tester/env', method: 'post', data })
}

export function batchDeleteExecutions(ids) {
  return request({
    url: '/api-tester/executions/batch-delete',
    method: 'delete',
    data: ids
  })
}