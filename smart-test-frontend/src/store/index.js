import { createStore } from 'vuex'
import { getUserInfo } from '@/api/user'

export default createStore({
    state: {
        user: null,
        token: localStorage.getItem('token') || '',
        roles: [] // 角色列表
    },
    mutations: {
        setUser(state, user) {
            state.user = user
            if (user && user.roles) {
                state.roles = user.roles
            }
        },
        setToken(state, token) {
            state.token = token
            localStorage.setItem('token', token)
        },
        logout(state) {
            state.user = null
            state.roles = []
            state.token = ''
            localStorage.removeItem('token')
        }
    },
    actions: {
        login({ commit }, token) {
            commit('setToken', token)
        },
        // 获取用户信息并保存
        async fetchUserInfo({ commit, state }) {
            if (!state.token) return Promise.reject('未登录')
            try {
                const res = await getUserInfo()
                commit('setUser', res.data)
                return res.data
            } catch (error) {
                // 如果获取用户信息失败，可能 token 无效，退出登录
                commit('logout')
                throw error
            }
        },
        logout({ commit }) {
            commit('logout')
        }
    },
    getters: {
        isAuthenticated: state => !!state.token,
        currentUser: state => state.user,
        userRoles: state => state.roles,
        // 判断是否拥有某个角色
        hasRole: state => (role) => state.roles.includes(role)
    }
})