import request from '@/utils/request'

// 获取缺陷列表（分页）
export function getDefectList(params) {
  return request({
    url: '/defects',
    method: 'get',
    params
  })
}

// 获取单个缺陷
export function getDefect(id) {
  return request({
    url: `/defects/${id}`,
    method: 'get'
  })
}

// 创建缺陷
export function createDefect(data) {
  return request({
    url: '/defects',
    method: 'post',
    data
  })
}

// 更新缺陷
export function updateDefect(id, data) {
  return request({
    url: `/defects/${id}`,
    method: 'put',
    data
  })
}

// 删除缺陷
export function deleteDefect(id) {
  return request({
    url: `/defects/${id}`,
    method: 'delete'
  })
}

// 批量删除缺陷
export function deleteDefectsBatch(ids) {
  return request({
    url: '/defects/batch',
    method: 'delete',
    data: ids
  })
}

// 获取缺陷评论
export function getDefectComments(id) {
  return request({
    url: `/defects/${id}/comments`,
    method: 'get'
  })
}

// 添加评论
export function addDefectComment(id, content) {
  return request({
    url: `/defects/${id}/comments`,
    method: 'post',
    data: { content }
  })
}

// 变更缺陷状态
export function changeDefectStatus(id, status, comment) {
  return request({
    url: `/defects/${id}/status`,
    method: 'post',
    data: { status, comment }
  })
}