<template>
    <div class="profile-container">
        <el-card>
            <template #header>
                <div class="card-header" style="display: flex; align-items: center;">
                    <el-button type="primary" @click="goBack" style="margin-right: 10px;">返回</el-button>
                    <span>个人资料</span>
                </div>
            </template>
            <el-tabs v-model="activeTab">
                <el-tab-pane label="基本信息" name="info">
                    <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="100px" style="max-width:500px">
                        <el-form-item label="用户名">
                            <el-input v-model="infoForm.username" disabled />
                        </el-form-item>
                        <el-form-item label="真实姓名" prop="realName">
                            <el-input v-model="infoForm.realName" />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="infoForm.email" />
                        </el-form-item>
                        <el-form-item label="手机" prop="phone">
                            <el-input v-model="infoForm.phone" />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="handleUpdateInfo">保存修改</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>
                <el-tab-pane label="修改密码" name="password">
                    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width:500px">
                        <el-form-item label="原密码" prop="oldPassword">
                            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                        </el-form-item>
                        <el-form-item label="新密码" prop="newPassword">
                            <el-input v-model="passwordForm.newPassword" type="password" show-password />
                        </el-form-item>
                        <el-form-item label="确认新密码" prop="confirmPassword">
                            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'  // 导入 useRouter
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api/user'
import { useStore } from 'vuex'

const router = useRouter()  // 获取 router 实例
const store = useStore()
const activeTab = ref('info')

// 基本信息表单
const infoFormRef = ref()
const infoForm = ref({
    username: '',
    realName: '',
    email: '',
    phone: ''
})

const infoRules = {
    email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// 密码表单
const passwordFormRef = ref()
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
    if (value !== passwordForm.value.newPassword) {
        callback(new Error('两次输入密码不一致'))
    } else {
        callback()
    }
}

const passwordRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirm, trigger: 'blur' }
    ]
}

// 加载个人资料
const loadProfile = async () => {
    try {
        const res = await getProfile()
        infoForm.value = {
            username: res.data.username,
            realName: res.data.realName || '',
            email: res.data.email || '',
            phone: res.data.phone || ''
        }
    } catch (error) {
        ElMessage.error('获取资料失败')
    }
}

onMounted(() => {
    loadProfile()
})

// 保存基本信息
const handleUpdateInfo = async () => {
    await infoFormRef.value.validate()
    try {
        await updateProfile({
            realName: infoForm.value.realName,
            email: infoForm.value.email,
            phone: infoForm.value.phone
        })
        ElMessage.success('修改成功')
        // 更新 Vuex 中的用户信息（可选）
        await store.dispatch('fetchUserInfo')
    } catch (error) {
        console.error('更新资料失败:', error) // 添加错误日志
        // 错误已在拦截器处理，这里只记录
    }
}

// 修改密码
const handleChangePassword = async () => {
    await passwordFormRef.value.validate()
    try {
        await changePassword({
            oldPassword: passwordForm.value.oldPassword,
            newPassword: passwordForm.value.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')
        // 清空表单
        passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
        // 可自动退出登录或提示用户重新登录
        // 这里只提示，用户可手动退出
    } catch (error) {
       console.error('修改密码失败:', error) // 添加错误日志
    }
}
// 返回上一页
const goBack = () => {
    router.back()
}
</script>
<script>
export default {
  name: 'ProfilePage'  // 改为多单词组件名
}
</script>
<style scoped>
.profile-container {
    padding: 20px;
}
</style>