import request from '@/utils/request'

// 获取当前用户信息
export function getUserInfo() {
    return request({
        url: '/users/me',
        method: 'get'
    })
}
// 更新用户信息
export function updateUserInfo(data) {
    return request({
        url: '/users/me',
        method: 'put',
        data
    })
}
// 获取个人资料（可复用 /users/me，也可以单独定义）
export function getProfile() {
    return request({
        url: '/users/profile',
        method: 'get'
    })
}

// 更新个人资料
export function updateProfile(data) {
    return request({
        url: '/users/profile',
        method: 'put',
        data
    })
}

// 修改密码
export function changePassword(data) {
    return request({
        url: '/users/password',
        method: 'put',
        data
    })
}
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export function getUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

export function getUserRoles(id) {
  return request({
    url: `/users/${id}/roles`,
    method: 'get'
  })
}

// 批量删除用户
export function deleteUsersBatch(ids) {
  return request({
    url: '/users/batch',
    method: 'delete',
    data: ids  // 直接发送数组
  })
}