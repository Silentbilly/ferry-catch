import { createRouter, createWebHistory } from 'vue-router'
import RoutesView from './views/RoutesView.vue'
import RouteDetailsView from './views/RouteDetailsView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'routes', component: RoutesView },
    { path: '/route', name: 'route-details', component: RouteDetailsView },
  ],
})
