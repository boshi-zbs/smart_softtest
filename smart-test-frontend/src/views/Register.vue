<template>
  <div class="register-container">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="register-card">
      <div class="brand">
        <div class="logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M24 4L4 14L24 24L44 14L24 4Z" stroke="currentColor" stroke-width="2" fill="none"/>
            <path d="M4 24L24 34L44 24" stroke="currentColor" stroke-width="2" fill="none"/>
            <path d="M4 34L24 44L44 34" stroke="currentColor" stroke-width="2" fill="none"/>
          </svg>
        </div>
        <h1>创建新账号</h1>
        <p>加入智能软件测试平台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="register-form" label-width="80px" label-position="right">
        <el-form-item label="用户名" prop="username" required>
          <el-input v-model="form.username" placeholder="3-20个字符" size="large" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password" required>
          <el-input v-model="form.password" type="password" placeholder="至少6位" size="large" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword" required>
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入" size="large" show-password />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="选填" size="large" clearable />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="example@xx.com" size="large" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="11位手机号" size="large" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="角色" prop="roleId" required>
          <el-select v-model="form.roleId" placeholder="请选择" size="large" style="width:100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleRegister" :loading="loading" class="register-btn">
            注册
          </el-button>
        </el-form-item>

        <div class="extra-links">
          <span>已有账号？</span>
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { getPublicRoles } from '@/api/role'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const roleOptions = ref([])

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  roleId: null
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

const fetchRoles = async () => {
  try {
    const res = await getPublicRoles()
    roleOptions.value = res.data.filter(role => role.roleCode !== 'ROLE_ADMIN')
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

onMounted(() => {
  fetchRoles()
})

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const registerData = {
      username: form.value.username,
      password: form.value.password,
      realName: form.value.realName,
      email: form.value.email,
      phone: form.value.phone,
      roleId: form.value.roleId
    }
    const res = await register(registerData)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }
  } catch (error) {
    console.error('注册失败', error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>
<script>
export default {
  name: 'RegisterPage'
}
</script>
<style scoped>
/* 背景与装饰圆同登录页，保持不变 */
.register-container {
  position: relative;
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #409EFF 0%, #409EFF 100%);
  overflow: auto;
  padding: 20px;
}

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

.register-card {
  position: relative;
  z-index: 1;
  width: 620px;
  max-width: 95%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 32px;
  padding: 40px 32px 48px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s ease;
}
.register-card:hover {
  transform: translateY(-4px);
}

.brand {
  text-align: center;
  margin-bottom: 32px;
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

.register-form {
  margin-top: 8px;
}
.register-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #1f2f3d;
}
.register-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: all 0.2s;
  background-color: white;
}
.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #667eea inset;
}
.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2), 0 0 0 1px #667eea inset;
}
.register-form :deep(.el-select .el-input__wrapper) {
  border-radius: 12px;
}
.register-btn {
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
.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.5);
}
.register-btn:active {
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

/* 响应式适配 */
@media (max-width: 576px) {
  .register-card {
    padding: 28px 20px 32px;
  }
  .brand h1 {
    font-size: 24px;
  }
  .register-form :deep(.el-row) {
    margin: 0 !important;
  }
  .register-form :deep(.el-col) {
    padding: 0 4px !important;
  }
}
</style>