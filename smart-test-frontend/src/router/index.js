import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'   // 新增
import Home from '@/views/Home.vue'
import Profile from '@/views/Profile.vue'  // 导入个人资料组件
import store from '@/store'
import { ElMessage } from 'element-plus'

// 管理员模块
import UserRoleManagement from '@/views/admin/UserRoleManagement.vue'
// 测试人员模块
import TestCaseManagement from '@/views/tester/TestCaseManagement.vue'
// 开发人员模块
import MyTodo from '@/views/dev/MyTodo.vue'

const routes = [
    {
        path: '/login',
        name: 'LoginPage',
        component: Login,
        meta: { requiresAuth: false }
    },
    {
        path: '/register',                     // 新增注册路由
        name: 'RegisterPage',
        component: Register,
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        name: 'HomePage',
        component: Home,
        meta: { requiresAuth: true },
        children: [
            // 管理员模块
            { path: 'admin/users', name: 'UserRoleManagement', component: UserRoleManagement, meta: { requiredRole: 'ROLE_ADMIN' } },
            { path: 'admin/projects', name: 'ProjectManagement', component: () => import('@/views/admin/ProjectManagement.vue'), meta: { requiredRole: 'ROLE_ADMIN' } }, // 占位，可后续实现
            {path: 'admin/projects/members',name: 'ProjectMember',component: () => import('@/views/admin/ProjectMember.vue'),meta: { requiredRole: 'ROLE_ADMIN' }},
            {
                path: 'admin/projects/modules',
                name: 'ProjectModule',
                component: () => import('@/views/admin/ProjectModule.vue'),
                meta: { requiredRole: 'ROLE_ADMIN' }
            },
            { path: 'admin/requirements', name: 'RequirementManagement', component: () => import('@/views/admin/RequirementManagement.vue'), meta: { requiredRole: 'ROLE_ADMIN' } },
            { path: 'admin/test-plans', name: 'TestPlanManagement', component: () => import('@/views/admin/TestPlanManagement.vue'), meta: { requiredRole: 'ROLE_ADMIN' } },
            {
                path: 'admin/reports/dashboard',
                name: 'ReportDashboard',
                component: () => import('@/views/admin/ReportDashboard.vue'),
                meta: { requiredRole: 'ROLE_ADMIN' }  // 管理员或测试人员根据需要调整
            },
            {
                path: 'admin/reports/project/:id',
                name: 'ProjectReport',
                component: () => import('@/views/admin/ProjectReport.vue'),
                meta: { requiredRole: 'ROLE_ADMIN' }
            },
            { path: 'admin/logs', name: 'OperationLog', component: () => import('@/views/admin/OperationLog.vue'), meta: { requiredRole: 'ROLE_ADMIN' } },
            // 测试人员模块
            { path: 'tester/cases', name: 'TestCaseManagement', component: TestCaseManagement, meta: { requiredRole: 'ROLE_TESTER' } },
            { path: 'tester/tasks', name: 'TestTaskExecution', component: () => import('@/views/tester/TestTaskExecution.vue'), meta: { requiredRole: 'ROLE_TESTER' } },
            { path: 'tester/defects', name: 'DefectManagement', component: () => import('@/views/tester/DefectManagement.vue'), meta: { requiredRole: 'ROLE_TESTER' } },
            {
                path: 'tester/defects/create',
                name: 'DefectCreate',
                component: () => import('@/views/tester/DefectManagement.vue'),
                meta: { requiredRole: 'ROLE_TESTER' }
            },
            {
            path: 'tester/auto-test',
            name: 'AutoTestManagement',
            component: () => import('@/views/tester/AutoTestManagement.vue'),
            meta: { requiredRole: 'ROLE_TESTER' }
            },

            // 开发人员模块
            { path: 'dev/todo', name: 'MyTodo', component: MyTodo, meta: { requiredRole: 'ROLE_DEV' } },
            {
                path: 'dev/requirements',
                name: 'RequirementDevelopment',
                component: () => import('@/views/dev/RequirementDevelopment.vue'),
                meta: { requiredRole: 'ROLE_DEV' }
            },
            {
                path: 'dev/requirements/:id',
                name: 'RequirementDetailDev',
                component: () => import('@/views/dev/RequirementDetailDev.vue'),
                meta: { requiredRole: 'ROLE_DEV' }
            },
            { path: 'dev/defects', name: 'DefectWorkflow', component: () => import('@/views/dev/DefectWorkflow.vue'), meta: { requiredRole: 'ROLE_DEV' } },
           {
            path: 'dev/defects/:id',
            name: 'DefectDetailDev',
            component: () => import('@/views/dev/DefectDetailDev.vue'), 
            meta: { requiredRole: 'ROLE_DEV' }
            },
            { path: 'dev/ai-analysis', name: 'AIAnalysis', component: () => import('@/views/dev/AIAnalysis.vue'), meta: { requiredRole: 'ROLE_DEV' } },
            // 个人资料（所有角色可见）
            { path: 'profile', name: 'Profile', component: Profile, meta: { requiresAuth: true } },
            {
                path: 'messages',
                name: 'MessageCenter',
                component: () => import('@/views/MessageCenter.vue'),
                meta: { requiresAuth: true }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 检查角色权限
function checkRole(to) {
    const roles = store.state.roles
    const requiredRole = to.meta.requiredRole
    if (requiredRole && !roles.includes(requiredRole)) {
        ElMessage.error('无权访问该页面')
        return false
    }
    return true
}

// 关键修改：将回调函数改为 async
router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')
    // const isAuthenticated = store.getters.isAuthenticated
    const user = store.state.user

    if (to.meta.requiresAuth) {
        if (!token) {
            next('/login')
        } else {
            // 有 token 但无用户信息，尝试获取
            if (!user) {
                try {
                    await store.dispatch('fetchUserInfo')
                    // 获取后检查角色
                    if (checkRole(to)) {
                        next()
                    } else {
                        next('/')
                    }
                } catch (error) {
                    // 获取失败，跳转登录
                    next('/login')
                }
            } else {
                // 已有用户信息，直接检查角色
                if (checkRole(to)) {
                    next()
                } else {
                    next('/')
                }
            }
        }
    } else {
        // 如果已登录且访问登录页，跳转首页
        if (token && (to.path === '/login' || to.path === '/register')) {
            next('/')
        } else {
            next()
        }
    }
})


export default router