<script setup lang="ts">
import logoUrl from "../../logo.png";
import { computed, onMounted, ref, watch } from "vue";
import { formatHHmm, timeUntil } from "../helpers/dateFormat";
import { useRouter, useRoute } from "vue-router";
import { ApiError } from "../api/client";
import { listStops, searchNext } from "../api";
import type { SearchResponse } from "../api/types";
import { slugify } from "../helpers/slugify";
import { messages, normalizeLang, getLocale, type Lang } from "../i18n";

type FavoriteRoute = { from: string; to: string };

const router = useRouter();
const route = useRoute();

const lang = computed<Lang>(() => normalizeLang(route.params.lang));
const t = computed(() => messages[lang.value]);
const locale = computed(() => getLocale(lang.value));

const stops = ref<string[]>([]);
const from = ref<string>(String(route.query.from ?? ""));
const to = ref<string>(String(route.query.to ?? ""));

const loadingStops = ref(false);
const loadingSearch = ref(false);
const error = ref<string | null>(null);
const result = ref<SearchResponse | null>(null);

const canSearch = computed(
  () => from.value && to.value && from.value !== to.value,
);

const languageOptions: Array<{ code: Lang; label: string }> = [
  { code: "en", label: "EN" },
  { code: "tr", label: "TR" },
  { code: "ru", label: "RU" },
];

function switchLanguage(nextLang: Lang) {
  if (nextLang === lang.value) return;

  router.push({
    name: (route.name as string) || "routes",
    params: {
      ...route.params,
      lang: nextLang,
    },
    query: route.query,
  });
}

async function loadStops() {
  loadingStops.value = true;
  error.value = null;
  try {
    stops.value = await listStops();
  } catch (e: unknown) {
    if (e instanceof ApiError)
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message;
    else if (e instanceof Error) error.value = e.message;
    else error.value = t.value.failedToLoadStops;
  } finally {
    loadingStops.value = false;
  }
}

async function doSearch() {
  if (!canSearch.value) return;

  loadingSearch.value = true;
  error.value = null;
  try {
    const res = await searchNext({
      from: from.value,
      to: to.value,
    });
    result.value = res;

    router.replace({
      name: "routes-with-slug",
      params: {
        lang: lang.value,
        fromSlug: slugify(from.value),
        toSlug: slugify(to.value),
      },
      query: { from: from.value, to: to.value },
    });
  } catch (e: unknown) {
    if (e instanceof ApiError)
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message;
    else if (e instanceof Error) error.value = e.message;
  } finally {
    loadingSearch.value = false;
  }
}

function openDetails() {
  if (!from.value || !to.value) return;

  router.push({
    name: "route-details",
    params: {
      lang: lang.value,
      fromSlug: slugify(from.value),
      toSlug: slugify(to.value),
    },
    query: { from: from.value, to: to.value },
  });
}

function swapStops() {
  if (!from.value && !to.value) return;
  const oldFrom = from.value;
  from.value = to.value;
  to.value = oldFrom;
}
const FAVORITES_KEY = "ferry-favorites";
const LAST_ROUTE_KEY = "ferry-last-route";
const LANG_KEY = "ferry-lang";

const favorites = ref<FavoriteRoute[]>([]);

const currentIsFavorite = computed(
  () =>
    !!favorites.value.find((f) => f.from === from.value && f.to === to.value),
);

onMounted(() => {
  loadStops();

  const rawFavorites = localStorage.getItem(FAVORITES_KEY);
  if (rawFavorites) {
    try {
      favorites.value = JSON.parse(rawFavorites);
    } catch {
      favorites.value = [];
    }
  }

  const rawLastRoute = localStorage.getItem(LAST_ROUTE_KEY);
  if (rawLastRoute && !route.query.from && !route.query.to) {
    try {
      const parsed = JSON.parse(rawLastRoute) as {
        from?: string;
        to?: string;
      };

      if (parsed.from) from.value = parsed.from;
      if (parsed.to) to.value = parsed.to;
    } catch {
      localStorage.removeItem(LAST_ROUTE_KEY);
    }
  }

  const routeLang = route.params.lang;
  const savedLangRaw = localStorage.getItem(LANG_KEY);
  const savedLang = savedLangRaw ? normalizeLang(savedLangRaw) : "en";

  if (!routeLang && savedLang !== "en") {
    router.replace({
      name: (route.name as string) || "routes",
      params: {
        ...route.params,
        lang: savedLang,
      },
      query: route.query,
    });
    return;
  }

  if (routeLang) {
    localStorage.setItem(LANG_KEY, lang.value);
  }
});

watch(
  favorites,
  (val) => {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(val));
  },
  { deep: true },
);

watch(
  () => lang.value,
  (value) => {
    localStorage.setItem(LANG_KEY, value);
  },
  { immediate: true },
);

watch(
  () => [from.value, to.value],
  ([newFrom, newTo]) => {
    result.value = null;

    if (!newFrom && !newTo) {
      localStorage.removeItem(LAST_ROUTE_KEY);
      return;
    }

    localStorage.setItem(
      LAST_ROUTE_KEY,
      JSON.stringify({
        from: newFrom,
        to: newTo,
      }),
    );
  },
);

function toggleFavorite() {
  if (!from.value || !to.value || from.value === to.value) return;

  const idx = favorites.value.findIndex(
    (f) => f.from === from.value && f.to === to.value,
  );

  if (idx >= 0) {
    favorites.value.splice(idx, 1);
  } else {
    favorites.value.unshift({ from: from.value, to: to.value });
  }
}

function applyFavorite(f: FavoriteRoute) {
  from.value = f.from;
  to.value = f.to;
}
</script>

<template>
  <main class="page">
    <header class="header">
      <div class="logoRow">
        <img class="logoTop" :src="logoUrl" alt="Catch a Ferry" />
      </div>

      <div class="titleRow">
        <h1 class="h1">{{ t.pageTitle }}</h1>

        <div class="langSwitch" aria-label="Language switcher">
          <button
            v-for="item in languageOptions"
            :key="item.code"
            type="button"
            class="langBtn"
            :class="{ 'langBtn--active': lang === item.code }"
            @click="switchLanguage(item.code)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </header>

    <p v-if="loadingStops">{{ t.loadingStops }}</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <section class="card">
      <h2 class="h2">{{ t.route }}</h2>

      <div v-if="favorites.length" class="favChips">
        <button
          v-for="fav in favorites"
          :key="fav.from + '→' + fav.to"
          type="button"
          class="favChip"
          @click="applyFavorite(fav)"
        >
          {{ fav.from }} → {{ fav.to }}
        </button>
      </div>

      <label class="label" for="from-stop">{{ t.from }}</label>
      <select id="from-stop" name="from-stop" v-model="from" class="select">
        <option value="">{{ t.selectPlaceholder }}</option>
        <option v-for="s in stops" :key="s" :value="s">{{ s }}</option>
      </select>

      <div class="betweenRow">
        <button type="button" class="swapBtn" @click="swapStops">⇅</button>
        <button
          type="button"
          class="favBtn"
          :class="{ 'favBtn--active': currentIsFavorite }"
          @click="toggleFavorite"
        >
          ★
        </button>
      </div>

      <label class="label" for="to-stop" style="margin-top: 10px">
        {{ t.to }}
      </label>
      <select id="to-stop" name="to-stop" v-model="to" class="select">
        <option value="">{{ t.selectPlaceholder }}</option>
        <option v-for="s in stops" :key="s" :value="s">{{ s }}</option>
      </select>

      <button
        class="primaryBtn"
        @click="doSearch"
        :disabled="!canSearch || loadingSearch"
        style="margin-top: 12px"
      >
        {{ loadingSearch ? t.searching : t.findNext }}
      </button>
    </section>

    <section v-if="result" class="card" style="margin-top: 12px">
      <div class="cardHead">
        <h2 class="h2 cardHead__title">{{ t.nextFerry }}</h2>
        <button class="ghostBtn cardHead__more" @click="openDetails">
          {{ t.more }}
        </button>
      </div>

      <div>
        <b>{{ t.in }}</b> {{ timeUntil(result.trip.departureTime, lang) }}
      </div>
      <div>
        <b>{{ t.dep }}</b>
        {{ formatHHmm(result.trip.departureTime, "local", locale) }}
      </div>
      <div>
        <b>{{ t.arr }}</b>
        {{ formatHHmm(result.trip.arrivalTime, "local", locale) }}
      </div>
      <div>
        <b>{{ t.operator }}</b> {{ result.trip.operator }}
      </div>

      <ol style="margin: 10px 0 0; padding-left: 18px">
        <li
          v-for="s in result.trip.stops"
          :key="s.sequence"
          style="margin: 6px 0"
        >
          {{ s.stopName }} —
          {{ formatHHmm(s.time, "local", locale) }}
        </li>
      </ol>
    </section>

    <section class="card" style="margin-top: 12px">
      <h2 class="h2">{{ t.windMap }}</h2>

      <div class="windyWrap" style="margin-top: 10px">
        <iframe
          class="windyWrap__frame"
          src="https://embed.windy.com/embed2.html?lat=40.99&lon=29.02&zoom=10&level=surface&overlay=wind"
          frameborder="0"
        ></iframe>
      </div>
    </section>

    <section class="card seo-text">
      <details>
        <summary class="seoSummary">{{ t.moreAbout }}</summary>
        <div class="seoBody">
          <p>{{ t.seo.paragraph1 }}</p>
          <p>{{ t.seo.paragraph2 }}</p>
          <p>{{ t.seo.paragraph3 }}</p>
          <p>{{ t.seo.paragraph4 }}</p>
          <p class="seoNotice">
            {{ t.seo.paragraph5 }}
            {{ t.seo.operatorsPrefix }}
            <a
              href="https://www.sehirhatlari.istanbul"
              target="_blank"
              rel="noopener noreferrer"
            >
              Şehir Hatları </a
            >{{ t.seo.operatorsSeparator }}
            <a
              href="https://www.mavimarmara.net"
              target="_blank"
              rel="noopener noreferrer"
            >
              Mavi Marmara </a
            >{{ t.seo.operatorsLastSeparator }}
            <a
              href="https://www.prenstur.net"
              target="_blank"
              rel="noopener noreferrer"
            >
              Prenstur </a
            >.
          </p>
        </div>
      </details>
    </section>
  </main>
</template>

<style scoped>
.page {
  max-width: 520px;
  margin: 0 auto;
  padding: 16px;
  font-family:
    system-ui,
    -apple-system,
    Segoe UI,
    Roboto,
    Arial,
    sans-serif;
  background: #f5fbff;
  color: #111827;
}

.header {
  margin-bottom: 8px;
}

.logoRow {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.logoTop {
  height: 50px;
  width: auto;
  display: block;
}

.titleRow {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.langSwitch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid #dbe3ea;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  flex: 0 0 auto;
  margin-top: 2px;
}

.langBtn {
  min-width: 40px;
  height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #4b5563;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease;
}

.langBtn:hover {
  background: #eef6fb;
  color: #0e7490;
}

.langBtn:focus-visible {
  outline: 2px solid rgba(8, 145, 178, 0.45);
  outline-offset: 2px;
}

.langBtn--active {
  background: #0891b2;
  color: #ffffff;
  box-shadow: 0 1px 2px rgba(8, 145, 178, 0.28);
}

.h1 {
  margin: 0;
  font-size: 21px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #30384f;
  position: relative;
  padding-bottom: 8px;
  flex: 1;
  min-width: 0;
}

.h1::after {
  content: "";
  display: block;
  width: 56px;
  height: 4px;
  border-radius: 999px;
  background: #0891b2;
  margin-top: 10px;
}

.h2 {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: #30384f;
  position: relative;
  padding-left: 12px;
}

.h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0.15em;
  bottom: 0.15em;
  width: 4px;
  border-radius: 999px;
  background: #0891b2;
}

.swapRow {
  display: flex;
  justify-content: center;
  margin: 5px;
}

.swapBtn {
  border-radius: 999px;
  border: 1px solid #d1d5db00;
  background: #f9fafb;
  padding: 4px 10px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.swapBtn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px 16px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.label {
  display: block;
  font-size: 13px;
  color: #374151;
  margin-bottom: 4px;
  margin-top: 0px;
}

.select {
  width: 100%;
  padding: 10px 10px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #111827;
  outline: none;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
  margin-bottom: 8px;
}

.select:hover {
  border-color: #9ca3af;
}

.select:focus-visible {
  border-color: #111827;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.18);
}

.primaryBtn {
  width: 100%;
  padding: 12px 12px;
  border-radius: 12px;
  background: #0891b2;
  border: 1px solid #0891b2;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.02s ease,
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.primaryBtn:hover:not(:disabled) {
  background: #0e7490;
  border-color: #0e7490;
}

.primaryBtn:active:not(:disabled) {
  transform: translateY(1px);
}

.primaryBtn:focus-visible {
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.35);
}

.primaryBtn:disabled {
  background: rgba(8, 145, 178, 0.35);
  border-color: rgba(8, 145, 178, 0.2);
  color: #97a3bc;
  box-shadow: none;
  opacity: 1;
  cursor: not-allowed;
}

.ghostBtn {
  border: 1px solid rgba(57, 194, 215, 0.55);
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  color: #0e7490;
  font-weight: 700;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.ghostBtn:hover {
  background: rgba(57, 194, 215, 0.08);
  border-color: rgba(57, 194, 215, 0.85);
}

.ghostBtn:focus-visible {
  outline: 2px solid rgba(57, 194, 215, 0.85);
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(57, 194, 215, 0.18);
}

.cardHead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.cardHead__title {
  margin: 0;
  flex: 1;
  min-width: 0;
}

.cardHead__more {
  white-space: nowrap;
}

.error {
  color: #dc2626;
}

.windyWrap {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%;
  height: 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.windyWrap__frame {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
}

.seoSummary {
  cursor: pointer;
  list-style: none;
  font-weight: 600;
  color: #3c465e;
}

.seoSummary::before {
  content: "▸";
  display: inline-block;
  margin-right: 6px;
  transition: transform 0.15s ease;
  color: #0891b2;
}

details[open] .seoSummary::before {
  transform: rotate(90deg);
}

.seoBody {
  margin-top: 10px;
}

.seo-text {
  margin-top: 12px;
}

.seo-text p {
  margin: 8px 0;
}

.seo-text p:first-child {
  margin-top: 0;
}

.seo-text p:last-child {
  margin-bottom: 0;
}

.seoNotice {
  margin: 10px 0 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fffbeb;
  border: 1px solid #facc15;
}

.favChips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.favChip {
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 4px 10px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
}

.favChip:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.betweenRow {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin: 4px 0 0;
}

.swapBtn {
  border-radius: 999px;
  border: 1px solid #d1d5db00;
  background: #f9fafb;
  padding: 4px 10px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.swapBtn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.favBtn {
  border-radius: 999px;
  border: 1px solid #d1d5db00;
  background: #f9fafb;
  padding: 4px 10px;
  font-size: 13px;
  color: #9ca3af;
  cursor: pointer;
}

.favBtn--active {
  color: #f59e0b;
}

.notice {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fffbeb;
  border: 1px solid #facc15;
  color: #5b4a00;
  font-size: 13px;
  line-height: 1.4;
}

.notice b {
  color: #3f3200;
}

.notice div + div {
  margin-top: 4px;
}

@media (max-width: 420px) {
  .logoTop {
    height: 44px;
  }

  .titleRow {
    align-items: flex-start;
    gap: 8px;
  }

  .langSwitch {
    margin-top: 2px;
  }

  .langBtn {
    min-width: 36px;
    height: 30px;
    padding: 0 8px;
    font-size: 11px;
  }
}
</style>
