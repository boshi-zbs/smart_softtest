<template>
  <div>
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新增模块</el-button>
      <span style="margin-left: auto;">当前项目：{{ projectName }}</span>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-table :data="moduleList" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="moduleName" label="模块名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑模块对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模块名称" prop="moduleName" required>
          <el-input v-model="form.moduleName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModulesByProject, createModule, updateModule, deleteModule } from '@/api/projectModule'
import { getProject } from '@/api/project' // 需要先有获取单个项目的接口

const route = useRoute()
const router = useRouter()
const projectId = route.query.projectId ? parseInt(route.query.projectId) : null
const projectName = ref('')
const moduleList = ref([])

const fetchProjectName = async () => {
  if (!projectId) return
  try {
    const res = await getProject(projectId)
    projectName.value = res.data.projectName
  } catch (error) {
    console.error('获取项目名称失败', error)
  }
}

const fetchModules = async () => {
  if (!projectId) return
  try {
    const res = await getModulesByProject(projectId)
    moduleList.value = res.data
  } catch (error) {
    console.error('获取模块列表失败', error)
  }
}

onMounted(() => {
  fetchProjectName()
  fetchModules()
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增模块')
const formRef = ref()
const saving = ref(false)
const form = ref({
  id: null,
  moduleName: '',
  description: '',
  projectId: projectId
})

const rules = {
  moduleName: [
    { required: true, message: '请输入模块名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const handleAdd = () => {
  dialogTitle.value = '新增模块'
  form.value = {
    id: null,
    moduleName: '',
    description: '',
    projectId: projectId
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑模块'
  form.value = {
    id: row.id,
    moduleName: row.moduleName,
    description: row.description,
    projectId: row.projectId
  }
  dialogVisible.value = true
}

const handleClose = () => {
  formRef.value?.resetFields()
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.id) {
      await updateModule(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createModule(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchModules()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除模块 ${row.moduleName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteModule(row.id)
      ElMessage.success('删除成功')
      fetchModules()
    } catch (error) {
      console.error('删除失败', error)
    }
  }).catch(() => {})
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
</style>