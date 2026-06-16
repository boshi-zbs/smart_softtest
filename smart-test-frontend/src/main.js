import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue' // 可选
import '@/styles/global.scss'   // 新增
import ECharts from 'vue-echarts'
import 'echarts'

// 修复 ResizeObserver 循环错误
const debounce = (fn, delay) => {
  let timer
  return (...args) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
  constructor(callback) {
    callback = debounce(callback, 16)
    super(callback)
  }
}

const app = createApp(App)
app.component('VChart', ECharts)

app.use(router)
app.use(store)
app.use(ElementPlus)

// 注册所有图标（可选）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.mount('#app')