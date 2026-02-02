<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { TimetableResponse, SearchResponse } from "../api/types";
import { getTimetable, searchNext } from "../api";
import { ApiError } from "../api/client";
import { formatHHmm } from "../helpers/dateFormat";

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

async function load() {
  if (!from.value || !to.value) return;

  loading.value = true;
  error.value = null;

  try {
    const q = buildQuery();

    const [next, tt] = await Promise.all([
      searchNext(q),
      getTimetable(q),
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

onMounted(load);
watch(
  () => [route.query.from, route.query.to, route.query.operator],
  load
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
        <div><b>Dep:</b> {{ formatHHmm(nextData.trip.departureTime) }} ({{ nextData.trip.departureTime }})</div>
        <div><b>Arr:</b> {{ formatHHmm(nextData.trip.arrivalTime) }} ({{ nextData.trip.arrivalTime }})</div>        
        <div><b>Operator:</b> {{ nextData.trip.operator }} </div>

        <ol style="margin-top: 10px">
          <li v-for="s in nextData.trip.stops" :key="s.sequence">
            {{ s.stopName }} — {{ formatHHmm(s.time) }}  —  {{ s.sequence }} stop
          </li>
        </ol>
      </div>

      <h2 style="margin-top: 18px">Timetable ({{ timetable?.date }})</h2>
      <div v-if="timetable" style="display: grid; gap: 12px">
        <div
          v-for="t in timetable.trips"
          :key="t.tripId"
          style="padding: 12px; border: 1px solid #eee; border-radius: 12px"
        >
          <div style="font-weight: 600">
            {{ formatHHmm(t.departureTime) }} → {{ formatHHmm(t.arrivalTime) }}
          </div>
          <div style="opacity: 0.7; font-size: 13px"> {{ t.operator }}</div>
        </div>
      </div>
    </section>
  </main>
</template>
