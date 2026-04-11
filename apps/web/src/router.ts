import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import RoutesView from "./views/RoutesView.vue";
import RouteDetailsView from "./views/RouteDetailsView.vue";

const LANG_KEY = "ferry-lang";

function getSavedLang(): "en" | "tr" | "ru" {
  const savedLang = localStorage.getItem(LANG_KEY);
  if (savedLang === "tr" || savedLang === "ru" || savedLang === "en") {
    return savedLang;
  }
  return "en";
}

export const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: () => {
      const savedLang = getSavedLang();
      return savedLang === "en" ? "/en" : `/${savedLang}`;
    },
  },
  {
    path: "/:lang(en|tr|ru)",
    name: "routes",
    component: RoutesView,
  },
  {
    path: "/:lang(en|tr|ru)/ferry/:fromSlug-:toSlug",
    name: "routes-with-slug",
    component: RoutesView,
  },
  {
    path: "/:lang(en|tr|ru)/route-details/:fromSlug-:toSlug",
    name: "route-details",
    component: RouteDetailsView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;