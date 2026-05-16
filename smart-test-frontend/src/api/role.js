import request from '@/utils/request'


// 获取所有角色（管理员接口）
export function getAllRoles() {
  return request({
    url: '/roles',
    method: 'get'
  })
}

// 获取公开角色列表（用于注册）
export function getPublicRoles() {
  return request({
    url: '/roles/public/list',
    method: 'get'
  })
}