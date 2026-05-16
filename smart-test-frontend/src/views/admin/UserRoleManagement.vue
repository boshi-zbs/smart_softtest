<template>
 <div>
    <!-- 查询表单及操作按钮行 -->
    <div class="action-bar">
      

      <!-- 查询条件表单（使用 inline 表单） -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="searchForm.realName" placeholder="真实姓名" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="searchForm.email" placeholder="邮箱" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="searchForm.phone" placeholder="手机" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleId" placeholder="全部" clearable style="width: 150px;">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 查询/重置按钮 + 批量删除按钮 -->
      <div class="action-group">
        <!-- 新增用户按钮 -->
        <el-button type="primary" @click="handleAdd" class="action-btn">新增用户</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
          批量删除
        </el-button>
      </div>
    </div>
    <!-- 通用表格组件（包含分页） -->
    <CommonTable
      ref="tableRef"
      :fetchData="fetchUsers"
      :searchParams="searchForm"
      @selection-change="handleSelectionChange"
    >
      <template #columns>
        <!-- 多选框列 -->
        <el-table-column type="selection" width="55" />
        <!-- 新增序号列 -->
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <!-- 假设后端返回的 user 对象中包含 roles 字段 -->
            <el-tag v-if="row.roles && row.roles[0]" size="small">
              {{ row.roles[0].roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
    </CommonTable>

    <!-- 新增/编辑用户对话框（保持不变） -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username" required>
          <el-input v-model="form.username" />
        </el-form-item>

        <el-form-item
          :label="isEdit ? '新密码' : '密码'"
          prop="password"
          :required="!isEdit"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '不填则不修改' : '请输入密码'"
          />
        </el-form-item>

        <el-form-item
          :label="isEdit ? '确认新密码' : '确认密码'"
          prop="confirmPassword"
          :required="!isEdit"
        >
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            :placeholder="isEdit ? '不填则不修改' : '请再次输入密码'"
          />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleId" required>
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommonTable from '@/components/CommonTable.vue' // 引入通用表格组件
import { getUserList, createUser, updateUser, deleteUser, deleteUsersBatch } from '@/api/user'  // 合并后的导入
import { getAllRoles } from '@/api/role'

// 表格引用，用于手动刷新
const tableRef = ref()

// 查询按钮点击
const handleSearch = () => {
  tableRef.value?.triggerSearch()
}
// 重置查询条件
const resetSearch = () => {
  searchForm.value = {
    username: '',
    realName: '',
    email: '',
    phone: '',
    status: null,
    roleId: null
  }
  // 重置后立即触发查询
  tableRef.value?.triggerSearch()
}

// 查询条件
const searchForm = ref({
  username: '',
  realName: '',
  email: '',
  phone: '',
  status: null,
  roleId: null
})

// 角色选项（供对话框使用）
const roleOptions = ref([])

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref()
const isEdit = ref(false)
const saving = ref(false)

const form = ref({
  id: null,
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  status: 1,
  roleId: null
})

// 表单校验规则（保持不变）
const passwordValidator = (rule, value, callback) => {
  if (!isEdit.value && !value) {
    callback(new Error('请输入密码'))
  } else if (value && value.length < 6) {
    callback(new Error('密码长度至少6位'))
  } else {
    callback()
  }
}

const confirmPasswordValidator = (rule, value, callback) => {
  if (!isEdit.value && !value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.value.password) {
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
    { validator: passwordValidator, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: confirmPasswordValidator, trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 加载角色选项（供对话框使用）
const loadRoles = async () => {
  try {
    const res = await getAllRoles()
    roleOptions.value = res.data
    console.log('角色选项:', roleOptions.value)
  } catch (error) {
    console.error('加载角色列表失败', error)
  }
}

onMounted(() => {
  loadRoles()
})


// 获取用户列表（分页）
const fetchUsers = async (params) => {
  // params 包含 page, size 以及 searchParams 中的所有字段（已自动合并）
  const res = await getUserList(params)
  // 期望后端返回格式：{ code:200, data: { records: [], total: 0 } }
  return res
}

// 新增用户
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  form.value = {
    id: null,
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    email: '',
    phone: '',
    status: 1,
    roleId: null
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

// 编辑用户
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  console.log('编辑用户行:', row)
  // 假设 row 中已包含 roles 字段（后端联表返回）
  const roleId = row.roles && row.roles.length > 0 ? row.roles[0].id : null
  form.value = {
    id: row.id,
    username: row.username,
    password: '',
    confirmPassword: '',
    realName: row.realName,
    email: row.email,
    phone: row.phone,
    status: row.status,
    roleId: roleId
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}



// 关闭对话框
const handleClose = () => {
  formRef.value?.resetFields()
}

// 保存用户
const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch (error) {
    console.log('表单校验未通过')
    return
  }

  saving.value = true
  try {
    const submitData = {
      username: form.value.username,
      realName: form.value.realName,
      email: form.value.email,
      phone: form.value.phone,
      status: form.value.status,
      roleId: form.value.roleId
    }
    if (form.value.password) {
      submitData.password = form.value.password
    }

    if (form.value.id) {
      await updateUser(form.value.id, submitData)
      ElMessage.success('修改成功')
    } else {
      if (!form.value.password) {
        ElMessage.error('密码不能为空')
        saving.value = false
        return
      }
      await createUser(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    // 刷新表格数据
    tableRef.value?.refresh()
  } catch (error) {
    console.error('保存用户失败', error)
  } finally {
    saving.value = false
  }
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      // 刷新表格
      tableRef.value?.refresh()
    } catch (error) {
      console.error('删除用户失败', error)
    }
  }).catch(() => {})
}

// 选中行数据
const selectedRows = ref([])

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUsersBatch(ids)  // 需要定义该 API
      ElMessage.success('批量删除成功')
      // 刷新表格
      tableRef.value?.refresh()
      // 清除选中状态（可选）
      selectedRows.value = []
    } catch (error) {
      console.error('批量删除失败', error)
    }
  }).catch(() => {})
}
</script>
<style scoped>
/* 解决表单抖动问题：固定最小高度，使用 flex-wrap */
.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-height: 56px;
  margin-bottom: 16px;
}
.el-form-item {
  margin-right: 16px;
  margin-bottom: 16px;
}
.action-group{
  margin-bottom: 30px;
}
</style>