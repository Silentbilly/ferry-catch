<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { TimetableResponse, SearchResponse, TripDto } from "../api/types";
import { getUpcomingTimetable, searchNext } from "../api";
import { ApiError } from "../api/client";
import { formatHHmm, formatYYYYMMDD } from "../helpers/dateFormat";

const route = useRoute();

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
  const depMs = new Date(isoTime).getTime()
  const nowMs = Date.now()

  const totalMinutes = Math.ceil((depMs - nowMs) / 1000 / 60)
  if (totalMinutes < 0) return 'departed'

  if (totalMinutes < 60) return `${totalMinutes} min`

  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  return `${h}h ${m} min`
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
  <main style="max-width: 520px; margin: 0 auto; padding: 16px">
    <h1>{{ from }} → {{ to }}</h1>

    <p v-if="loading">Loading…</p>
    <p v-else-if="error">{{ error }}</p>

    <section v-else>
      <h2>Next ferry</h2>
      <div
        v-if="nextData"
        style="padding: 12px; border: 1px solid #ddd; border-radius: 12px"
      >
        <div><b>In:</b> {{ nextData.minutesUntil }} min</div>
        <div><b>Dep:</b> {{ formatHHmm(nextData.trip.departureTime) }}</div>
        <div><b>Arr:</b> {{ formatHHmm(nextData.trip.arrivalTime) }}</div>
        <div><b>Operator:</b> {{ nextData.trip.operator }}</div>

        <ol style="margin-top: 10px">
          <li v-for="s in nextData.trip.stops" :key="s.sequence">
            {{ s.stopName }} — {{ formatHHmm(s.time) }}
          </li>
        </ol>
      </div>

      <h2 style="margin-top: 18px">Timetable</h2>
      <div v-if="timetable" style="display: grid; gap: 12px">
        <div
          v-for="t in timetable.trips"
          :key="t.tripId"
          style="padding: 12px; border: 1px solid #eee; border-radius: 12px"
        >
          <div style="font-weight: 600">
            {{ formatHHmm(t.departureTime) }} → {{ formatHHmm(t.arrivalTime) }}
          </div>
          <div style="opacity: 0.75; font-size: 13px">
            {{ formatYYYYMMDD(t.departureTime) }} · in
            {{ timeUntil(t.departureTime) }} ·
            {{ getStopsNumber(t).trim() }}
          </div>
          <div style="opacity: 0.7; font-size: 13px">{{ t.operator }}</div>
        </div>
      </div>
    </section>
  </main>
</template>
