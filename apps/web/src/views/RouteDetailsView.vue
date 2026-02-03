<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { TimetableResponse, SearchResponse, TripDto } from "../api/types";
import { getUpcomingTimetable, searchNext } from "../api";
import { ApiError } from "../api/client";
import { formatHHmm, formatYYYYMMDD } from "../helpers/dateFormat";

const route = useRoute();
const router = useRouter();

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
    limit: 20,
  };
}

function getStopsNumber(trip: TripDto): string {
  const stopsCount = Math.max(0, trip.stops.length - 2);

  if (stopsCount === 0) return " non-stop";
  return ` ${stopsCount} ${stopsCount === 1 ? "stop" : "stops"}`;
}

function timeUntil(isoTime: string): string {
  const depMs = new Date(isoTime).getTime();
  const nowMs = Date.now();

  const totalMinutes = Math.ceil((depMs - nowMs) / 1000 / 60);
  if (totalMinutes < 0) return "departed";

  if (totalMinutes < 60) return `${totalMinutes} min`;

  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}h ${m} min`;
}

function goBack() {
  if (window.history.length > 1) router.back();
  else router.push({ name: "routes" });
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
      error.value = "Failed to load data";
    }

    nextData.value = null;
    timetable.value = null;
  } finally {
    loading.value = false;
  }
}

watch(
  () => [route.query.from, route.query.to, route.query.operator],
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
        ← Back
      </button>
    </header>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <section v-else class="stack">
      <section class="card">
        <h2 class="h2">Next ferry</h2>

        <div v-if="nextData" class="stack">
          <div><b>In:</b> {{ nextData.minutesUntil }} min</div>
          <div><b>Dep:</b> {{ formatHHmm(nextData.trip.departureTime) }}</div>
          <div><b>Arr:</b> {{ formatHHmm(nextData.trip.arrivalTime) }}</div>
          <div><b>Operator:</b> {{ nextData.trip.operator }}</div>

          <ol class="list">
            <li
              v-for="s in nextData.trip.stops"
              :key="s.sequence"
              class="listItem"
            >
              {{ s.stopName }} — {{ formatHHmm(s.time) }}
            </li>
          </ol>
        </div>
      </section>

      <section class="card">
        <h2 class="h2">Timetable</h2>

        <div v-if="timetable" class="grid">
          <div v-for="t in timetable.trips" :key="t.tripId" class="ttItem">
            <div class="ttTime">
              {{ formatHHmm(t.departureTime) }} →
              {{ formatHHmm(t.arrivalTime) }}
            </div>
            <div class="ttMeta">
              {{ formatYYYYMMDD(t.departureTime) }} · in
              {{ timeUntil(t.departureTime) }} ·
              {{ getStopsNumber(t).trim() }}
            </div>
            <div class="ttOp">{{ t.operator }}</div>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.backBtn { white-space: nowrap; padding: 8px 10px; }
.header { display:flex; align-items:center; justify-content:space-between; gap:12px; }

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
  color: #111827;
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
  color: #111827;
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

/* Lists */
.list {
  margin: 10px 0 0;
  padding-left: 18px;
}
.listItem {
  margin: 6px 0;
}

/* Timetable grid */
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

/* (опционально) если позже добавишь кнопки на эту страницу — можно переиспользовать */
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
</style>
