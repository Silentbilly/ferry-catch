<script setup lang="ts">
import logoUrl from "../../logo.png";
import { computed, onMounted, ref, watch } from "vue";
import { formatHHmm, timeUntil } from "../helpers/dateFormat";
import { useRouter, useRoute } from "vue-router";
import { ApiError } from "../api/client";
import { listStops, searchNext } from "../api";
import type { SearchResponse } from "../api/types";
import { slugify } from "../helpers/slugify";

const router = useRouter();
const route = useRoute();

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

async function loadStops() {
  loadingStops.value = true;
  error.value = null;
  try {
    stops.value = await listStops();
  } catch (e: unknown) {
    if (e instanceof ApiError)
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message;
    else if (e instanceof Error) error.value = e.message;
    else error.value = "Failed to load stops";
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

    const lang = String(route.params.lang || "en");

    router.replace({
      name: "routes-with-slug",
      params: {
        lang,
        fromSlug: slugify(from.value),
        toSlug: slugify(to.value),
      },
      query: { from: from.value, to: to.value },
    });
  } catch (e: unknown) {
  } finally {
    loadingSearch.value = false;
  }
}

function openDetails() {
  if (!from.value || !to.value) return;
  const lang = String(route.params.lang || "en");

  router.push({
    name: "route-details",
    params: {
      lang,
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

onMounted(loadStops);

// если юзер пришёл по ссылке с query — попробуем сразу искать
watch(
  () => [from.value, to.value],
  () => {
    result.value = null;
  },
);

// Favorite routes
type FavoriteRoute = { from: string; to: string };

const FAVORITES_KEY = "ferry-favorites";

const favorites = ref<FavoriteRoute[]>([]);

const currentIsFavorite = computed(
  () =>
    !!favorites.value.find((f) => f.from === from.value && f.to === to.value),
);

// загрузка из localStorage
onMounted(() => {
  loadStops(); // уже есть
  const raw = localStorage.getItem(FAVORITES_KEY);
  if (raw) {
    try {
      favorites.value = JSON.parse(raw);
    } catch {
      favorites.value = [];
    }
  }
});

// автосохранение
watch(
  favorites,
  (val) => {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(val));
  },
  { deep: true },
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
      <img class="logoTop" :src="logoUrl" alt="Catch a Ferry" />
      <h1 class="h1">Istanbul Ferries (Islands)</h1>
    </header>

    <p v-if="loadingStops">Loading stops…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <section class="card">
      <h2 class="h2">Route</h2>

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

      <label class="label" for="from-stop">From</label>
      <select id="from-stop" name="from-stop" v-model="from" class="select">
        <option value="">Select…</option>
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

      <label class="label" for="to-stop" style="margin-top: 10px">To</label>
      <select id="to-stop" name="to-stop" v-model="to" class="select">
        <option value="">Select…</option>
        <option v-for="s in stops" :key="s" :value="s">{{ s }}</option>
      </select>

      <button
        class="primaryBtn"
        @click="doSearch"
        :disabled="!canSearch || loadingSearch"
        style="margin-top: 12px"
      >
        {{ loadingSearch ? "Searching…" : "Find next" }}
      </button>
    </section>

    <section v-if="result" class="card" style="margin-top: 12px">
      <div class="cardHead">
        <h2 class="h2 cardHead__title">Next Ferry</h2>
        <button class="ghostBtn cardHead__more" @click="openDetails">
          More →
        </button>
      </div>

      <div><b>In:</b> {{ timeUntil(result.trip.departureTime) }}</div>
      <div><b>Dep:</b> {{ formatHHmm(result.trip.departureTime) }}</div>
      <div><b>Arr:</b> {{ formatHHmm(result.trip.arrivalTime) }}</div>
      <div><b>Operator:</b> {{ result.trip.operator }}</div>

      <ol style="margin: 10px 0 0; padding-left: 18px">
        <li
          v-for="s in result.trip.stops"
          :key="s.sequence"
          style="margin: 6px 0"
        >
          {{ s.stopName }} — {{ formatHHmm(s.time) }}
        </li>
      </ol>
    </section>

    <section class="card" style="margin-top: 12px">
      <h2 class="h2">Wind map</h2>

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
        <summary class="seoSummary">More about ferries & schedule</summary>
        <div class="seoBody">
          <p>
            If you are looking for an Istanbul to Princes' Islands ferry
            (Adalar), here you can quickly see the next departures from ports
            like Kabataş, Kadıköy, Beşiktaş, Bostancı, Maltepe and Kartal.
          </p>
          <p>
            Ferries to Adalar (Princes' Islands) run throughout the day, and
            this Istanbul ferries schedule helps you find the next departure
            without checking multiple websites.
          </p>
          <p>
            Each operator uses its own pier, so always check the information
            boards and ask on site to make sure the ferry you need departs from
            that pier and arrives at the correct island.
          </p>
          <p>
            Here you can quickly check the Istanbul ferries schedule for your
            route: departure and arrival times, intermediate stops and
            operators, then open full details for a specific sailing when you
            need more information.
          </p>
          <p class="seoNotice">
            In case of strong wind or adverse weather conditions, ferry
            operators may cancel or change departures and routes, so always
            double-check the latest notices on official operator websites such
            as
            <a
              href="https://www.sehirhatlari.istanbul"
              target="_blank"
              rel="noopener"
            >
              Şehir Hatları </a
            >,
            <a
              href="https://www.mavimarmara.net"
              target="_blank"
              rel="noopener"
            >
              Mavi Marmara
            </a>
            or
            <a href="https://www.prenstur.net" target="_blank" rel="noopener">
              Prenstur
            </a>
            before you travel.
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
  position: relative;
  padding-top: 44px;
  display: block;
}

.logoTop {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  height: 50px;
  width: auto;
  display: block;
}

.h1 {
  margin: 0 0 8px;
  font-size: 21px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #30384f;
  position: relative;
  padding-bottom: 8px;
}

.h1::after {
  content: "";
  display: block;
  width: 56px;
  height: 4px;
  border-radius: 999px;
  background: #0891b2; /* primary */
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

/* Inputs */
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

/* Keep select focus dark (no blue/cyan ring) */
.select:focus-visible {
  border-color: #111827;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.18);
}

/* Primary button */
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

/* Ghost button */
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

/* Windy embed */
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

/* Favorite routes */
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
</style>
