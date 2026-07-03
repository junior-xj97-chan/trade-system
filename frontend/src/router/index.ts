import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductView.vue'),
        meta: { title: '商品列表', icon: 'Goods' },
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/product/SearchView.vue'),
        meta: { title: '商品搜索', icon: 'Search' },
      },
      {
        path: 'position',
        name: 'Position',
        component: () => import('@/views/position/PositionView.vue'),
        meta: { title: '持仓管理', icon: 'TrendCharts' },
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/OrderView.vue'),
        meta: { title: '订单管理', icon: 'List' },
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetailView.vue'),
        meta: { title: '订单详情', icon: 'List' },
      },
      {
        path: 'account',
        name: 'Account',
        component: () => import('@/views/account/AccountView.vue'),
        meta: { title: '账户管理', icon: 'Wallet' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'User' },
      },
      
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || ''} - 金融交易系统`.trim()

  // 需要登录的页面
  if (to.meta.requiresAuth !== false) {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ name: 'Login' })
      return
    }
  }

  // 已登录访问登录页则跳转到首页
  if (to.path === '/login') {
    const token = localStorage.getItem('token')
    if (token) {
      next({ name: 'Dashboard' })
      return
    }
  }

  next()
})

export default router