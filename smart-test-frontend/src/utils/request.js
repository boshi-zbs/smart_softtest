import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import.meta.env.VITE_API_BASE_URL

// 创建 axios 实例
const request = axios.create({
    baseURL: 'http://localhost:8080/api', // 后端地址
    timeout: 60000
})

// 请求拦截器：添加 token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：处理响应和错误
request.interceptors.response.use(
    response => {
        const res = response.data
        // 如果自定义代码不是 200，则视为错误
        if (res.code !== 200) {
            ElMessage.error(res.message || '系统错误')
            // 如果 token 无效或过期，跳转到登录页
            if (res.code === 401) {
                localStorage.removeItem('token')
                router.push('/login')
            }
            return Promise.reject(new Error(res.message || 'Error'))
        }
        return res
    },
    error => {
        // 处理 HTTP 错误状态
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    ElMessage.error('未授权，请重新登录')
                    localStorage.removeItem('token')
                    router.push('/login')
                    break
                case 403:
                    ElMessage.error('权限不足')
                    break
                case 500:
                    ElMessage.error('服务器错误')
                    break
                default:
                    ElMessage.error(error.message)
            }
        } else {
            ElMessage.error('网络错误')
        }
        return Promise.reject(error)
    }
)

export default request