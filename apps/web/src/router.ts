import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import RoutesView from "./views/RoutesView.vue";
import RouteDetailsView from "./views/RouteDetailsView.vue";
import { normalizeLang } from "./i18n";

const LANG_KEY = "ferry-lang";

export const routes: RouteRecordRaw[] = [
  {
    path: "/:lang(en|tr|ru)?",
    name: "routes",
    component: RoutesView,
  },
  {
    path: "/:lang(en|tr|ru)?/ferry/:fromSlug-:toSlug",
    name: "routes-with-slug",
    component: RoutesView,
  },
  {
    path: "/:lang(en|tr|ru)?/route-details/:fromSlug-:toSlug",
    name: "route-details",
    component: RouteDetailsView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const routeLang =
    typeof to.params.lang === "string" ? normalizeLang(to.params.lang) : null;

  if (routeLang) {
    localStorage.setItem(LANG_KEY, routeLang);
    return true;
  }

  const savedLangRaw = localStorage.getItem(LANG_KEY);
  const savedLang = savedLangRaw ? normalizeLang(savedLangRaw) : "en";

  if (savedLang === "en") {
    return true;
  }

  return {
    name: to.name || "routes",
    params: {
      ...to.params,
      lang: savedLang,
    },
    query: to.query,
    hash: to.hash,
    replace: true,
  };
});

export default router;