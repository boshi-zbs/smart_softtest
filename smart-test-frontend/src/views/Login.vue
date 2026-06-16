<template>
  <div class="login-container">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="login-card">
      <div class="brand">
        <div class="logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M24 4L4 14L24 24L44 14L24 4Z" stroke="currentColor" stroke-width="2" fill="none"/>
            <path d="M4 24L24 34L44 24" stroke="currentColor" stroke-width="2" fill="none"/>
            <path d="M4 34L24 44L44 34" stroke="currentColor" stroke-width="2" fill="none"/>
          </svg>
        </div>
        <h1>智能软件测试过程管理系统</h1>
        <p>Smart Test Management Process Management System</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="roleId">
          <el-select v-model="form.roleId" placeholder="选择登录角色" size="large" style="width:100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="handleLogin" :loading="loading" class="login-btn">
            登录
          </el-button>
        </el-form-item>
        <div class="extra-links">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goToRegister">立即注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { getPublicRoles } from '@/api/role'

const router = useRouter()
const store = useStore()
const formRef = ref()
const loading = ref(false)
const roleOptions = ref([])

const form = ref({
  username: '',
  password: '',
  roleId: null
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择登录角色', trigger: 'change' }]
}

// 获取角色列表
const fetchRoles = async () => {
  try {
    const res = await getPublicRoles()
    // 过滤掉管理员（可选，根据需求决定）
    roleOptions.value = res.data
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

onMounted(() => {
  fetchRoles()
})

const handleLogin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await login(form.value)
    store.commit('setToken', res.data)
    await store.dispatch('fetchUserInfo')
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败', error)
    // 错误已在拦截器中处理，这里无需重复提示
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>
<script>
export default {
  name: 'LoginPage'
}
</script>
<style scoped>
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #409EFF 0%, #409EFF 100%);
  overflow: hidden;
}

/* 装饰性圆形 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}
.circle {
  position: absolute;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  animation: float 20s infinite ease-in-out;
}
.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}
.circle-2 {
  width: 400px;
  height: 400px;
  bottom: -150px;
  right: -100px;
  animation-delay: 5s;
}
.circle-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 60%;
  animation-delay: 10s;
}
@keyframes float {
  0% { transform: translate(0, 0); }
  50% { transform: translate(20px, 20px); }
  100% { transform: translate(0, 0); }
}

/* 登录卡片 */
.login-card {
  position: relative;
  z-index: 1;
  width: 460px;
  max-width: 90%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 32px;
  padding: 48px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s ease;
}
.login-card:hover {
  transform: translateY(-4px);
}

.brand {
  text-align: center;
  margin-bottom: 40px;
}
.logo {
  color: #667eea;
  margin-bottom: 16px;
  display: inline-block;
}
.brand h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0 0 8px 0;
  letter-spacing: -0.3px;
}
.brand p {
  color: #5a6874;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-top: 8px;
}
.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: all 0.2s;
  background-color: white;
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #667eea inset;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2), 0 0 0 1px #667eea inset;
}
.login-form :deep(.el-select .el-input__wrapper) {
  border-radius: 12px;
}
.login-btn {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(90deg, #0dd466 0%, #4ba265 100%);
  border: none;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.2s;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.5);
}
.login-btn:active {
  transform: translateY(0);
}

.extra-links {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #5a6874;
}
.extra-links span {
  margin-right: 8px;
}
.extra-links .el-link {
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 576px) {
  .login-card {
    padding: 32px 24px;
  }
  .brand h1 {
    font-size: 24px;
  }
}
</style>