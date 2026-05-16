import request from '@/utils/request'

export function getModulesByProject(projectId) {
  return request({
    url: `/project-modules/project/${projectId}`,
    method: 'get'
  })
}

export function getModule(id) {
  return request({
    url: `/project-modules/${id}`,
    method: 'get'
  })
}

export function createModule(data) {
  return request({
    url: '/project-modules',
    method: 'post',
    data
  })
}

export function updateModule(id, data) {
  return request({
    url: `/project-modules/${id}`,
    method: 'put',
    data
  })
}

export function deleteModule(id) {
  return request({
    url: `/project-modules/${id}`,
    method: 'delete'
  })
}