import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/global.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import { installApiNoticeDedupe } from './utils/apiNoticeDedupe'
import { installFormMouseleaveValidation } from './utils/formMouseleaveValidation'

/** Pinia 根实例，承载登录、主题等全局状态。 */
const pinia = createPinia()
/** Vue 应用根实例，负责挂载路由、Element Plus 和全局插件。 */
const app = createApp(App)

app.use(pinia)
useThemeStore(pinia).applyTheme()
installApiNoticeDedupe()
installFormMouseleaveValidation(app)

app
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#app')
