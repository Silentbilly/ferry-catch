<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { formatHHmm, formatYYYYMMDD } from '../helpers/dateFormat'
import { useRouter, useRoute } from 'vue-router'
import { ApiError } from '../api/client'
import { listStops, searchNext } from '../api'
import type { SearchResponse } from '../api/types'

const router = useRouter()
const route = useRoute()

const stops = ref<string[]>([])
const from = ref<string>(String(route.query.from ?? ''))
const to = ref<string>(String(route.query.to ?? ''))

const loadingStops = ref(false)
const loadingSearch = ref(false)
const error = ref<string | null>(null)
const result = ref<SearchResponse | null>(null)

const canSearch = computed(() => from.value && to.value && from.value !== to.value)

async function loadStops() {
  loadingStops.value = true
  error.value = null
  try {
    stops.value = await listStops()
  } catch (e: unknown) {
    if (e instanceof ApiError) error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message
    else if (e instanceof Error) error.value = e.message
    else error.value = 'Failed to load stops'
  } finally {
    loadingStops.value = false
  }
}

async function doSearch() {
  if (!canSearch.value) return

  loadingSearch.value = true
  error.value = null
  try {
    const res = await searchNext({
      from: from.value,
      to: to.value,
    })
    result.value = res

    // сохраняем в URL, чтобы работал share/back
    router.replace({ query: { from: from.value, to: to.value } })
  } catch (e: unknown) {
    result.value = null
    if (e instanceof ApiError) error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message
    else if (e instanceof Error) error.value = e.message
    else error.value = 'Search failed'
  } finally {
    loadingSearch.value = false
  }
}

function openDetails() {
  if (!from.value || !to.value) return
  router.push({ name: 'route-details', query: { from: from.value, to: to.value } })
}

onMounted(loadStops)

// если юзер пришёл по ссылке с query — попробуем сразу искать
watch(
  () => [from.value, to.value],
  () => {
    result.value = null
  }
)
</script>

<template>
  <main class="page">
    <header class="header">
      <h1 class="h1">Find ferry</h1>
    </header>

    <p v-if="loadingStops">Loading stops…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <section class="card">
      <label class="label">From</label>
      <select v-model="from" class="select">
        <option value="">Select…</option>
        <option v-for="s in stops" :key="s" :value="s">{{ s }}</option>
      </select>

      <label class="label" style="margin-top:10px;">To</label>
      <select v-model="to" class="select">
        <option value="">Select…</option>
        <option v-for="s in stops" :key="s" :value="s">{{ s }}</option>
      </select>

      <button class="primaryBtn" @click="doSearch" :disabled="!canSearch || loadingSearch" style="margin-top:12px;">
        {{ loadingSearch ? 'Searching…' : 'Find next' }}
      </button>
    </section>

    <section v-if="result" class="card" style="margin-top:12px;">
      <h2 class="h2">Next Ferry</h2>
      <div><b>In:</b> {{ result.minutesUntil }} min</div>
      <div><b>Dep:</b> {{ formatHHmm(result.trip.departureTime) }} · {{ formatYYYYMMDD(result.trip.arrivalTime) }}</div>
      <div><b>Arr:</b> {{ formatHHmm(result.trip.arrivalTime) }} · {{ formatYYYYMMDD(result.trip.arrivalTime) }}</div>
      <div><b>Operator:</b> {{ result.trip.operator }} </div>

      <button class="ghostBtn" style="margin-top:12px;" @click="openDetails">
        Timetable
      </button>

      <ol style="margin: 10px 0 0; padding-left: 18px;">
        <li v-for="s in result.trip.stops" :key="s.sequence" style="margin: 6px 0;">
          {{ s.stopName }} — {{ formatHHmm(s.time) }} 
        </li>
      </ol>
    </section>
<section class="card" style="margin-top:12px;">
<h2 class="h2">Wind map</h2>

  <div class="windyWrap" style="margin-top:10px;">
    <iframe
      class="windyWrap__frame"
      src="https://embed.windy.com/embed2.html?lat=40.99&lon=29.02&zoom=10&level=surface&overlay=wind"
      frameborder="0"
    ></iframe>
  </div>
</section>

  </main>
</template>

<style scoped>
.page {
  max-width: 520px;
  margin: 0 auto;
  padding: 16px;
  font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
  background: #f5fbff;
  color: #111827;
}

.header { display:flex; align-items:baseline; justify-content:space-between; gap:12px; }

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
  background: #0891b2; /* твой primary */
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
  box-shadow: 0 1px 2px rgba(0,0,0,.06);
}

.label { display:block; font-size:13px; color:#374151; margin-bottom:4px; }

/* Inputs */
.select {
  width: 100%;
  padding: 10px 10px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #111827;
  outline: none;
  transition: border-color .15s ease, box-shadow .15s ease;
}

.select:hover { border-color: #9ca3af; }

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

  /* чуть темнее EPAM Scooter (#39C2D7), чтобы белый читался лучше */
  background: #0891b2;            /* близко к cyan-600 */
  border: 1px solid #0891b2;
  color: #fff;

  font-weight: 800;
  cursor: pointer;
  transition: transform .02s ease, background-color .15s ease, border-color .15s ease, box-shadow .15s ease;
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
  background: rgba(8, 145, 178, 0.35);   /* тот же тон, но бледнее */
  border-color: rgba(8, 145, 178, 0.20);
  color: #97a3bc;
  box-shadow: none;
  opacity: 1;                             /* не гасим всю кнопку */
  cursor: not-allowed;
}

/* Ghost button */
.ghostBtn {
  border: 1px solid rgba(57, 194, 215, 0.55);
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  color: #0e7490; /* чуть темнее циана, чтобы читалось */
  font-weight: 700;
  transition: background-color .15s ease, border-color .15s ease, box-shadow .15s ease;
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

.error { color:#dc2626; }

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

.windyWrap__frame { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }

</style>
