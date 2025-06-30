import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/deviceInfo',
    },
    {
      path: '/appUpdate',
      name: 'AppUpdate',
      component: () => import('@/views/AppUpdateView.vue'),
    },
    {
      path: '/deviceInfo',
      name: 'DeviceInfo',
      component: () => import('@/views/DeviceInfoView.vue'),
    },
  ],
})

export default router
