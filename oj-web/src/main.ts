import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
// 设计体系 v6「工作台」：冷灰白底 + 白色面板，EP 使用亮色变量集
import './styles/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
// P2-20：不指定 locale 时 EP 内置文案（分页"共 x 条"、日期面板、表格空态）是英文
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
