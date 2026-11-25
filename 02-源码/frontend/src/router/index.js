import { createRouter, createWebHistory } from 'vue-router'
import { store } from '../store'

// 路由与菜单 path 一一对应，页面按角色权限由后端菜单决定
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { open: true } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '场馆经营看板' } },
      { path: 'venues', component: () => import('../views/Venues.vue'), meta: { title: '场馆档案' } },
      { path: 'venues/:id', component: () => import('../views/VenueDetail.vue'), meta: { title: '场馆详情' } },
      { path: 'calendar', component: () => import('../views/Calendar.vue'), meta: { title: '预约周历' } },
      { path: 'bookings', component: () => import('../views/Bookings.vue'), meta: { title: '预约管理' } },
      { path: 'bookings/new', component: () => import('../views/BookingNew.vue'), meta: { title: '新建预约' } },
      { path: 'bookings/:id', component: () => import('../views/BookingDetail.vue'), meta: { title: '预约详情' } },
      { path: 'waitlist', component: () => import('../views/Waitlist.vue'), meta: { title: '候补管理' } },
      { path: 'checkin', component: () => import('../views/Checkin.vue'), meta: { title: '入场核验' } },
      { path: 'equipment', component: () => import('../views/Equipment.vue'), meta: { title: '设备借用' } },
      { path: 'tournaments', component: () => import('../views/Tournaments.vue'), meta: { title: '赛事台账' } },
      { path: 'tournaments/:id', component: () => import('../views/TournamentDetail.vue'), meta: { title: '赛事详情' } },
      { path: 'schedule', component: () => import('../views/Schedule.vue'), meta: { title: '赛程编排' } },
      { path: 'maintenance', component: () => import('../views/Maintenance.vue'), meta: { title: '场地维护' } },
      { path: 'violations', component: () => import('../views/Violations.vue'), meta: { title: '违约与申诉' } },
      { path: 'rules', component: () => import('../views/Rules.vue'), meta: { title: '冲突协同规则' } },
      { path: 'users', component: () => import('../views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'roles', component: () => import('../views/Roles.vue'), meta: { title: '角色管理' } },
      { path: 'menus', component: () => import('../views/Menus.vue'), meta: { title: '菜单管理' } },
      { path: 'logs', component: () => import('../views/Logs.vue'), meta: { title: '操作日志' } },
      { path: 'messages', component: () => import('../views/Messages.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('../layout/PortalLayout.vue'),
    children: [
      { path: '', component: () => import('../views/Portal.vue'), meta: { title: '场馆资源门户' } },
      { path: '/my-bookings', component: () => import('../views/MyBookings.vue'), meta: { title: '我的预约' } },
      { path: '/staff-portal', component: () => import('../views/StaffPortal.vue'), meta: { title: '岗位工作台' } },
      { path: '/portal/messages', component: () => import('../views/Messages.vue'), meta: { title: '消息中心' } },
      { path: '/portal/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫：未登录回登录页；纯普通用户不允许进后台
router.beforeEach(to => {
  if (to.meta.open) return true
  if (!store.token) return { path: '/login' }
  const role = store.user && store.user.roleCode
  if (role === 'USER' && !to.path.startsWith('/portal') && to.path !== '/my-bookings') {
    return { path: '/portal' }
  }
  return true
})

export default router
