import request from '@/utils/request'

// 获取操作日志列表
export function getOperationLogList(params) {
  return request({
    url: '/operation-logs',
    method: 'get',
    params
  })
}

// 获取所有模块选项
export function getLogModules() {
  return request({
    url: '/operation-logs/modules',
    method: 'get'
  })
}

// 获取所有操作类型选项
export function getLogOperations() {
  return request({
    url: '/operation-logs/operations',
    method: 'get'
  })
}


// 批量删除日志
export function deleteLogsBatch(ids) {
  return request({
    url: '/operation-logs/batch',
    method: 'delete',
    data: ids
  })
}

// 清空所有日志
export function clearAllLogs() {
  return request({
    url: '/operation-logs/clear-all',
    method: 'delete'
  })
}