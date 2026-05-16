<template>
    <div class="login-container">
        <el-card class="login-card">
            <h2>智能软件测试过程管理平台</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
                <el-form-item prop="username">
                    <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
                </el-form-item>
                <el-form-item prop="roleId">
                    <el-select v-model="form.roleId" placeholder="请选择登录角色" style="width:100%">
                        <el-option
                            v-for="item in roleOptions"
                            :key="item.id"
                            :label="item.roleName"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleLogin" style="width:100%">登录</el-button>
                </el-form-item>
                <el-form-item>
                    <el-button @click="goToRegister" style="width:100%">注册</el-button>
                </el-form-item>
            </el-form>
        </el-card>
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
const roleOptions = ref([])

const form = ref({
    username: '',
    password: '',
    roleId: null  // 新增角色ID
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
        roleOptions.value = res.data
    } catch (error) {
        console.error('获取角色列表失败', error)
    }
}

onMounted(() => {
    fetchRoles()
})

const handleLogin = async () => {
    // 确保表单引用存在
    if (!formRef.value) {
        ElMessage.error('表单未就绪，请刷新页面重试');
        return;
    }
    
    // 表单校验（失败时显示提示并终止）
    try {
        await formRef.value.validate();
    } catch (errors) {
        // 提取第一个错误信息并提示
        const firstErrorMessage = errors?.[Object.keys(errors)[0]]?.[0];
        ElMessage.warning(firstErrorMessage || '请正确填写表单');
        return;
    }
    
    // 调用登录接口
    try {
        const res = await login(form.value);
        store.commit('setToken', res.data);
        await store.dispatch('fetchUserInfo');
        ElMessage.success('登录成功');
        router.push('/');
    } catch (error) {
        console.error('登录失败:', error);
        ElMessage.error(error.message || '登录失败，请检查网络或联系管理员');
    }
}
const goToRegister = () => {
    router.push('/register')
}
</script>
<script>
    export default {
    name: 'LoginPage'  // 或者 'HomeView'
    }
</script>

<style scoped>
.login-container {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f0f2f5;
}
.login-card {
    width: 400px;
}
</style>