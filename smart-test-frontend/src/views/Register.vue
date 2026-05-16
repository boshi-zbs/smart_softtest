<template>
    <div class="register-container">
        <el-card class="register-card">
            <h2>用户注册</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="form.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="form.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机" prop="phone">
                    <el-input v-model="form.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="角色" prop="roleId">
                    <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
                        <el-option
                            v-for="item in roleOptions"
                            :key="item.id"
                            :label="item.roleName"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleRegister" style="width:100%">注册</el-button>
                </el-form-item>
                <el-form-item>
                    <el-button @click="goToLogin" style="width:100%">返回登录</el-button>
                </el-form-item>
            </el-form>
        </el-card>
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
const roleOptions = ref([])

const form = ref({
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    email: '',
    phone: '',
    roleId: null  // 新增角色ID
})

// 自定义验证：确认密码
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
        { required: false, type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    phone: [
        { required: false, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    roleId: [
        { required: true, message: '请选择角色', trigger: 'change' }
    ]
}

// 获取角色列表
const fetchRoles = async () => {
    try {
        const res = await getPublicRoles()
        // 过滤掉管理员角色（ROLE_ADMIN），只允许注册普通角色
        roleOptions.value = res.data.filter(role => role.roleCode !== 'ROLE_ADMIN')
    } catch (error) {
        console.error('获取角色列表失败', error)
    }
}

onMounted(() => {
    fetchRoles()
})

const handleRegister = async () => {
    await formRef.value.validate()
    try {
        // confirmPassword 并不需要单独提取。
        const { ...registerData } = form.value
        const res = await register(registerData)
        if (res.code === 200) {
            ElMessage.success('注册成功，请登录')
            router.push('/login')
        }
    } catch (error) {
         console.error('注册失败:', error);
    }
}

const goToLogin = () => {
    router.push('/login')
}
</script>
<script>
    export default {
    name: 'RegisterPage'  // 或者 'HomeView'
    }
</script>

<style scoped>
.register-container {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f0f2f5;
}
.register-card {
    width: 500px;
}
</style>