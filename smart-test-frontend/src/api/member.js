import request from '@/utils/request'

// 分页查询成员（可带 projectId 等条件）
export function getMemberList(params) {
  return request({
    url: '/project-members',
    method: 'get',
    params
  })
}

// 根据项目ID获取成员列表（不分页）
export function getMembersByProject(projectId) {
  return request({
    url: `/project-members/project/${projectId}`,
    method: 'get'
  })
}

// 添加成员
export function addMember(data) {
  return request({
    url: '/project-members',
    method: 'post',
    data
  })
}

// 更新成员
export function updateMember(data) {  // data 应包含 projectId, userId, roleInProject
  return request({ url: '/project-members', method: 'put', data })
}

// 删除成员
export function deleteMember(projectId, userId) {
  return request({ url: `/project-members/${projectId}/${userId}`, method: 'delete' })
}

// 批量删除成员
export function deleteMembersBatch(members) {
  return request({
    url: '/project-members/batch',
    method: 'delete',
    data: members
  })
}