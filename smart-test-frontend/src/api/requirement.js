import request from '@/utils/request'

// 获取需求列表（分页）
export function getRequirementList(params) {
  return request({
    url: '/requirements',
    method: 'get',
    params
  })
}

// 获取单个需求
export function getRequirement(id) {
  return request({
    url: `/requirements/${id}`,
    method: 'get'
  })
}

// 创建需求
export function createRequirement(data) {
  return request({
    url: '/requirements',
    method: 'post',
    data
  })
}

// 更新需求
export function updateRequirement(id, data) {
  return request({
    url: `/requirements/${id}`,
    method: 'put',
    data
  })
}

// 删除需求
export function deleteRequirement(id) {
  return request({
    url: `/requirements/${id}`,
    method: 'delete'
  })
}

// 批量删除需求
export function deleteRequirementsBatch(ids) {
  return request({
    url: '/requirements/batch',
    method: 'delete',
    data: ids
  })
}