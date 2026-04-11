<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { TimetableResponse, SearchResponse, TripDto } from "../api/types";
import { getUpcomingTimetable, searchNext } from "../api";
import { ApiError } from "../api/client";
import { formatHHmm, formatYYYYMMDD, timeUntil } from "../helpers/dateFormat";
import { getLocale, messages, normalizeLang } from "../i18n";

const route = useRoute();
const router = useRouter();

const lang = computed(() => normalizeLang(route.params.lang));
const locale = computed(() => getLocale(lang.value));
const t = computed(() => messages[lang.value]);

const from = computed(() => String(route.query.from ?? ""));
const to = computed(() => String(route.query.to ?? ""));

const nextData = ref<SearchResponse | null>(null);
const timetable = ref<TimetableResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

function buildQuery() {
  return {
    from: from.value,
    to: to.value,
  };
}

function upcomingQuery() {
  return {
    from: from.value,
    to: to.value,
    limit: null,
  };
}

function getStopsNumber(trip: TripDto): string {
  const stopsCount = Math.max(0, trip.stops.length - 2);

  if (stopsCount === 0) return t.value.direct;
  if (stopsCount === 1) return `1 ${t.value.stop}`;
  return `${stopsCount} ${t.value.stops}`;
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push({
    name: "routes",
    params: { lang: lang.value },
    query: {
      from: from.value,
      to: to.value,
    },
  });
}

async function load() {
  if (!from.value || !to.value) return;

  loading.value = true;
  error.value = null;

  try {
    const q = buildQuery();
    const u = upcomingQuery();

    const [next, tt] = await Promise.all([
      searchNext(q),
      getUpcomingTimetable(u),
    ]);

    nextData.value = next;
    timetable.value = tt;
  } catch (e: unknown) {
    if (e instanceof ApiError) {
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message;
    } else if (e instanceof Error) {
      error.value = e.message;
    } else {
      error.value = t.value.failedToLoadData;
    }

    nextData.value = null;
    timetable.value = null;
  } finally {
    loading.value = false;
  }
}

watch(
  () => [route.query.from, route.query.to, route.query.operator, route.params.lang],
  () => {
    void load();
  },
  { immediate: true },
);
</script>

<template>
  <main class="page">
    <header class="header">
      <h1 class="h1">{{ from }} → {{ to }}</h1>
      <button class="ghostBtn backBtn" type="button" @click="goBack">
        {{ t.back }}
      </button>
    </header>

    <p v-if="loading" class="muted">{{ t.loading }}</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <section v-else class="stack">
      <section class="card">
        <h2 class="h2">{{ t.nextFerry }}</h2>

        <div v-if="nextData" class="stack">
          <div><b>{{ t.in }}</b> {{ timeUntil(nextData.trip.departureTime, lang) }}</div>
          <div>
            <b>{{ t.dep }}</b>
            {{ formatHHmm(nextData.trip.departureTime, "local", locale) }}
          </div>
          <div>
            <b>{{ t.arr }}</b>
            {{ formatHHmm(nextData.trip.arrivalTime, "local", locale) }}
          </div>
          <div><b>{{ t.operator }}</b> {{ nextData.trip.operator }}</div>

          <ol class="list">
            <li
              v-for="s in nextData.trip.stops"
              :key="s.sequence"
              class="listItem"
            >
              {{ s.stopName }} — {{ formatHHmm(s.time, "local", locale) }}
            </li>
          </ol>
        </div>
      </section>

      <section class="card">
        <h2 class="h2">{{ t.timetable }}</h2>

        <div v-if="timetable" class="grid">
          <div v-for="trip in timetable.trips" :key="trip.tripId" class="ttItem">
            <div class="ttTime">
              {{ formatHHmm(trip.departureTime, "local", locale) }} →
              {{ formatHHmm(trip.arrivalTime, "local", locale) }}
            </div>
            <div class="ttMeta">
              {{ formatYYYYMMDD(trip.departureTime) }} · {{ t.inSmall }}
              {{ timeUntil(trip.departureTime, lang) }} ·
              {{ getStopsNumber(trip) }}
            </div>
            <div class="ttOp">{{ trip.operator }}</div>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.backBtn {
  white-space: nowrap;
  padding: 8px 10px;
}

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
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.h1 {
  margin: 0 0 8px;
  font-size: 34px;
  line-height: 1.1;
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
  background: #0891b2;
  margin-top: 10px;
}

.h2 {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 800;
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

.card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px 16px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.stack {
  display: grid;
  gap: 12px;
}

.muted {
  opacity: 0.75;
}

.error {
  color: #dc2626;
}

.list {
  margin: 10px 0 0;
  padding-left: 18px;
}

.listItem {
  margin: 6px 0;
}

.grid {
  display: grid;
  gap: 12px;
}

.ttItem {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.ttTime {
  font-weight: 800;
  color: #353e55;
}

.ttMeta {
  opacity: 0.75;
  font-size: 13px;
  margin-top: 2px;
}

.ttOp {
  opacity: 0.7;
  font-size: 13px;
  margin-top: 4px;
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
</style>