import './assets/main.css'
import '@/styles/index.scss'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { ElTableColumn } from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

ElTableColumn.props.showOverflowTooltip = {
  type: Boolean,
  default: true,
}

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
