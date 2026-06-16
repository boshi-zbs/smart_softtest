<template>
    <el-container style="height:100vh">
        <el-header style="background-color: #409EFF; color: white; display: flex; align-items: center;">
            <span style="font-size: 20px;">智能软件测试过程管理系统</span>
            <div style="flex:1"></div>
            <!-- 消息图标（全角色可见） -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item" style="margin-right: 20px;">
                <el-icon :size="20" color="white" @click="goToMessageCenter"><Notification /></el-icon>
            </el-badge>
            <el-dropdown @command="handleCommand">
                <span style="color: white; cursor: pointer;">
                    {{ username }}<el-icon><arrow-down /></el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                        <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </el-header>
        <el-container>
            <el-aside width="200px">
                <el-menu :default-active="$route.path" router>
                    <!-- 动态渲染菜单 -->
                    <template v-for="item in menuItems" :key="item.title">
                        <el-sub-menu v-if="item.children" :index="item.title">
                            <template #title>
                                <el-icon><component :is="item.icon" /></el-icon>
                                <span>{{ item.title }}</span>
                            </template>
                            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                                <el-icon><component :is="child.icon" /></el-icon>
                                <span>{{ child.title }}</span>
                            </el-menu-item>
                        </el-sub-menu>
                        <el-menu-item v-else :index="item.path">
                            <el-icon><component :is="item.icon" /></el-icon>
                            <span>{{ item.title }}</span>
                        </el-menu-item>
                    </template>
                </el-menu>
            </el-aside>
            <el-main>
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRoute,useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Folder,Connection, Document,DataLine,Lock, Warning, User, TrendCharts, Tickets, List, Edit, ChatDotRound, Notification,Monitor} from '@element-plus/icons-vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { getUnreadCount } from '@/api/message'
const route = useRoute()
const unreadCount = ref(0)
const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (error) {
    console.error('获取未读消息数失败', error)
  }
}

// 定时轮询（每30秒）
let interval
onMounted(() => {
  loadUnreadCount()
  interval = setInterval(loadUnreadCount, 30000)
})
onUnmounted(() => {
  clearInterval(interval)
})

// 点击消息图标跳转到消息中心
const goToMessageCenter = () => {
  router.push('/messages')
}

const store = useStore()
const router = useRouter()

const username = computed(() => store.state.user?.realName || store.state.user?.username || '用户')
const roles = computed(() => store.state.roles || [])
onMounted(() => {
  // 如果当前路由是根路径，则根据角色跳转到默认页面
  if (route.path === '/') {
    // 角色对应的默认路径
    const roleDefaultPath = {
      'ROLE_ADMIN': '/admin/users',
      'ROLE_TESTER': '/tester/cases',
      'ROLE_DEV': '/dev/todo'
    }
    let defaultPath = null
    for (const role of roles.value) {
      if (roleDefaultPath[role]) {
        defaultPath = roleDefaultPath[role]
        break
      }
    }
    if (defaultPath) {
      router.replace(defaultPath)
    } else {
      ElMessage.error('未分配有效角色，请联系管理员')
      router.push('/login')
    }
  }
})
// 根据角色生成菜单
const menuItems = computed(() => {
    const items = []

    // 全角色通用：首页、消息中心（单独处理，不放在侧边栏也可以）

   // 管理员菜单项（直接添加子项）
    if (roles.value.includes('ROLE_ADMIN')) {
        items.push(
            { path: '/admin/users', title: '用户与角色管理', icon: User },
            { path: '/admin/projects', title: '项目管理', icon: Folder },
            { path: '/admin/requirements', title: '功能需求管理', icon: Document },
            { path: '/admin/test-plans', title: '测试计划与进度', icon: Tickets },
            { path: '/admin/reports/dashboard', title: '测试报告与仪表盘', icon: TrendCharts },
            { path: '/admin/logs', title: '操作日志', icon: List }
        )
    }

    // 测试人员菜单项
  // 测试人员菜单项
    if (roles.value.includes('ROLE_TESTER')) {
    items.push({
        title: '测试用例管理',
        icon: Tickets,
        children: [
        { path: '/tester/cases', title: '全部用例', icon: List },
        { path: '/tester/cases/functional', title: '功能测试', icon: Document },
        { path: '/tester/cases/performance', title: '性能测试', icon: DataLine },
        { path: '/tester/cases/security', title: '安全测试', icon: Lock },
        { path: '/tester/cases/compatibility', title: '兼容性测试', icon: Monitor }
        ]
    });
    items.push(
        { path: '/tester/tasks', title: '测试任务执行', icon: Edit },
        { path: '/tester/auto-test', title: '自动化测试执行', icon: Monitor },
        { path: '/tester/defects', title: '缺陷提交与跟踪', icon: Warning },
        { path: '/tester/api-tester', title: '接口测试', icon: Connection }
    );
    }

    // 开发人员菜单项
    if (roles.value.includes('ROLE_DEV')) {
        items.push(
            { path: '/dev/todo', title: '我的待办', icon: List },
            { path: '/dev/requirements', title: '需求开发', icon: Document },
            { path: '/dev/defects', title: '缺陷处理工作流', icon: Edit },
            { path: '/dev/ai-analysis', title: 'AI缺陷分析', icon: ChatDotRound }
        )
    }

    // 全角色：通知与消息中心（可放在顶部或侧边栏底部）
    // 可以单独加一个菜单项，这里先不加在侧边栏，而是在顶部显示一个消息图标

    return items
})

const handleCommand = (command) => {
    if (command === 'logout') {
        store.dispatch('logout')
        ElMessage.success('已退出登录')
        router.push('/login')
    } else if (command === 'profile') {
        router.push('/profile')
    }
}
</script>
<script>
    export default {
    name: 'HomePage'  // 或者 'HomeView'
    }
</script>

<style scoped>
/* 让侧边栏填满整个容器高度 */
.el-aside {
  height: 100%;
  overflow-y: auto;          /* 菜单项过多时出现滚动条 */
  background-color: #fff;    /* 可根据喜好改成深色或渐变 */
  border-right: 1px solid #eef2f6;
}

/* 让菜单组件占满侧边栏的高度 */
.el-menu {
  height: 100%;
  border-right: none;
}

/* 提升菜单项的视觉质感（与你之前的全局风格一致） */
.el-menu-item, .el-sub-menu__title {
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.el-menu-item.is-active {
  background-color: #eef2ff;
  color: #1677ff;
  font-weight: 500;
}

.el-menu-item:hover, .el-sub-menu__title:hover {
  background-color: #f5f7fa;
}
</style>