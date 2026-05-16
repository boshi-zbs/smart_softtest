import request from '@/utils/request'

// 登录（携带角色ID）
export function login(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

// 注册（携带角色ID）
export function register(data) {
    return request({
        url: '/auth/register',
        method: 'post',
        data
    })
}