<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ApiError } from '../api/client'
import { listStops, searchNext } from '../api'
import type { SearchResponse } from '../api/types'

const router = useRouter()
const route = useRoute()

const operator = ref<string>('Mavi Marmara') // пока фикс; позже можно загрузить /operators
const stops = ref<string[]>([])
const from = ref<string>(String(route.query.from ?? ''))
const to = ref<string>(String(route.query.to ?? ''))

const loadingStops = ref(false)
const loadingSearch = ref(false)
const error = ref<string | null>(null)
const result = ref<SearchResponse | null>(null)

function formatHHmm(iso?: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

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
      <button class="ghostBtn" @click="loadStops" :disabled="loadingStops">
        Reload stops
      </button>
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
      <div><b>In:</b> {{ result.minutesUntil }} min</div>
      <div><b>Dep:</b> {{ formatHHmm(result.trip.departureTime) }} ({{ result.trip.departureTime }})</div>
      <div><b>Arr:</b> {{ formatHHmm(result.trip.arrivalTime) }} ({{ result.trip.arrivalTime }})</div>
      <div><b>Operator:</b> {{ result.trip.operator }} </div>

      <button class="ghostBtn" style="margin-top:12px;" @click="openDetails">
        Open details
      </button>

      <ol style="margin: 10px 0 0; padding-left: 18px;">
        <li v-for="s in result.trip.stops" :key="s.sequence" style="margin: 6px 0;">
          {{ s.stopName }} — {{ formatHHmm(s.time) }}  —  {{ s.sequence }} stop. 
        </li>
      </ol>
    </section>
  </main>
</template>

<style scoped>
.page { max-width: 520px; margin: 0 auto; padding: 16px; font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; }
.header { display:flex; align-items:baseline; justify-content:space-between; gap:12px; }
.h1 { margin:0 0 8px; font-size:34px; line-height:1.1; }
.card { border:1px solid #e5e7eb; border-radius:14px; padding:14px 16px; background:#fff; box-shadow:0 1px 2px rgba(0,0,0,.06); }
.label { display:block; font-size:13px; opacity:.7; margin-bottom:4px; }
.select { width:100%; padding:10px 10px; border-radius:10px; border:1px solid #e5e7eb; }
.primaryBtn { width:100%; padding:12px 12px; border-radius:12px; border:1px solid #111827; background:#111827; color:#fff; font-weight:700; cursor:pointer; }
.primaryBtn:disabled { opacity:.5; cursor:not-allowed; }
.ghostBtn { border:1px solid #e5e7eb; background:#fff; border-radius:10px; padding:8px 10px; cursor:pointer; }
.error { color:#dc2626; }
</style>
