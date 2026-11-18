import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// 全站暗色主题：EP 的暗色变量集 + html.dark 开关，
// 让表格/输入框/下拉/对话框全部跟随暗色（Linear 式暗色基准）
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'

// 暗色是全站唯一主题：根元素常驻 dark 类
document.documentElement.classList.add('dark')

const app = createApp(App)
app.use(createPinia())
app.use(router)
// P2-20：不指定 locale 时 EP 内置文案（分页"共 x 条"、日期面板、表格空态）是英文
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
