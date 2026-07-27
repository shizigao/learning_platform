import { createPinia } from 'pinia'
import { createApp } from 'vue'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/message/style/css'

import App from './App.vue'
import router from './router'
import './style.css'

// 应用仅在此处装配全局插件；业务依赖应通过模块导入或组合式函数显式使用。
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
