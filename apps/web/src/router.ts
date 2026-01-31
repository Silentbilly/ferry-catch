import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import RoutesView from './views/RoutesView.vue'
import RouteDetailsView from './views/RouteDetailsView.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'routes', component: RoutesView },
  { path: '/route', name: 'route-details', component: RouteDetailsView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
