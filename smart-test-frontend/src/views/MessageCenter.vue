<template>
  <div>
    <!-- 顶部操作栏 -->
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px;">
      <div>
        <el-radio-group v-model="filterType" @change="handleFilterChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="通知">通知</el-radio-button>
          <el-radio-button label="待办">待办</el-radio-button>
          <el-radio-button label="提醒">提醒</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="showOnlyPending" @change="handleFilterChange" style="margin-left: 16px;">
          仅显示未完成待办
        </el-checkbox>
      </div>
      <el-button type="primary" plain @click="markAllRead" :disabled="unreadCount === 0">
        全部标记为已读
      </el-button>
    </div>

    <!-- 消息列表 -->
    <el-timeline>
      <el-timeline-item
        v-for="item in messageList"
        :key="item.id"
        :type="item.isRead ? 'info' : 'primary'"
        :timestamp="item.createTime"
        placement="top"
      >
        <el-card :class="{ 'todo-card': item.type === '待办' && item.status === 'pending' }">
          <div style="display: flex; justify-content: space-between;">
            <div>
              <span v-if="item.type === '待办' && item.status === 'pending'" class="todo-badge">待处理</span>
              <span v-if="!item.isRead" class="unread-dot"></span>
              <span style="font-weight: bold;">{{ item.title }}</span>
            </div>
            <div>
              <el-button v-if="!item.isRead" type="primary" link @click="handleMarkRead(item.id)">标为已读</el-button>
              <el-button v-if="item.type === '待办' && item.status === 'pending'" 
                         type="success" size="small" @click="handleComplete(item.id)">标记完成</el-button>
            </div>
          </div>
          <div>{{ item.content }}</div>
          <div v-if="item.senderName" style="color: #909399; font-size: 12px; margin-top: 8px;">
            发送人：{{ item.senderName }}
          </div>
          <div v-if="item.relatedId" style="margin-top: 8px;">
            <el-button type="primary" link @click="handleRelated(item)">
              查看关联 {{ getRelatedTypeName(item.relatedType) }}
            </el-button>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <!-- 分页 -->
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref,  onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMessageList, getUnreadCount, markAsRead, markAllAsRead, completeMessage } from '@/api/message'

const router = useRouter()

const filterType = ref('')
const showOnlyPending = ref(false)
const messageList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const unreadCount = ref(0)

// 加载消息列表
const loadMessages = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      type: filterType.value
    }
    if (showOnlyPending.value) {
      params.status = 'pending'
    }
    const res = await getMessageList(params)
    messageList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('加载消息失败', error)
  }
}

// 加载未读数量
const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (error) {
    console.error('获取未读数失败', error)
  }
}

onMounted(() => {
  loadMessages()
  loadUnreadCount()
})

// 筛选条件变化
const handleFilterChange = () => {
  currentPage.value = 1
  loadMessages()
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  loadMessages()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadMessages()
}

// 标记单条已读
const handleMarkRead = async (id) => {
  try {
    await markAsRead(id)
    const msg = messageList.value.find(m => m.id === id)
    if (msg) {
      msg.isRead = true
    }
    unreadCount.value--
    ElMessage.success('已标记为已读')
  } catch (error) {
    console.error('标记失败', error)
  }
}

// 全部已读
const markAllRead = async () => {
  try {
    await markAllAsRead()
    messageList.value.forEach(m => m.isRead = true)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记失败', error)
  }
}

// 标记待办完成
const handleComplete = async (id) => {
  try {
    await completeMessage(id)
    ElMessage.success('已标记为完成')
    // 刷新列表（或者直接更新本地状态）
    loadMessages()
    loadUnreadCount()
  } catch (error) {
    console.error('标记完成失败', error)
  }
}

// 关联类型显示名称
const getRelatedTypeName = (type) => {
  const map = {
    defect: '缺陷',
    requirement: '需求',
    project: '项目',
    testplan: '测试计划',
    testcase: '测试用例',
    autotest: '自动化用例'
  }
  return map[type] || type
}

// 点击关联跳转
const handleRelated = (item) => {
  const routes = {
    defect: '/dev/defects',
    requirement: '/dev/requirements',
    project: '/admin/projects',
    testplan: '/tester/tasks',
    autotest: '/tester/auto-test'
  }
  const basePath = routes[item.relatedType]
  if (basePath) {
    // 跳转时带上 messageId，以便业务页面自动完成待办
    router.push(`${basePath}?id=${item.relatedId}&messageId=${item.id}`)
  } else {
    ElMessage.warning('无法跳转到关联内容')
  }
}
</script>

<style scoped>
.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background-color: #409EFF;
  border-radius: 50%;
  margin-right: 8px;
}
.todo-card {
  background-color: #fef0e6;
  border-left: 4px solid #e6a23c;
}
.todo-badge {
  background-color: #e6a23c;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}
</style>