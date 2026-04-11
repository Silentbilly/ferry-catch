import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import RoutesView from "./views/RoutesView.vue";
import RouteDetailsView from "./views/RouteDetailsView.vue";
import { normalizeLang } from "./i18n";

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

const LANG_KEY = "ferry-lang";

router.beforeEach((to) => {
  const routeLang = to.params.lang;
  const savedLangRaw = localStorage.getItem(LANG_KEY);
  const savedLang = savedLangRaw ? normalizeLang(savedLangRaw) : "en";

  if (!routeLang && savedLang !== "en") {
    return {
      name: to.name ?? "routes",
      params: {
        ...to.params,
        lang: savedLang,
      },
      query: to.query,
      hash: to.hash,
    };
  }

  if (routeLang) {
    localStorage.setItem(LANG_KEY, normalizeLang(routeLang));
  }
});

export default router;