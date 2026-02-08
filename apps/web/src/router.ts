import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import RoutesView from './views/RoutesView.vue'
import RouteDetailsView from './views/RouteDetailsView.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/:lang(en|tr|ru)?',
    name: 'routes',
    component: RoutesView,
  },
  {
    path: '/:lang(en|tr|ru)?/ferry/:fromSlug-:toSlug',
    name: 'routes-with-slug',
    component: RoutesView,
  },
  {
    path: '/:lang(en|tr|ru)?/route-details/:fromSlug-:toSlug',
    name: 'route-details',
    component: RouteDetailsView,
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
