<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { NextResponse, TimetableResponse } from '../api/types'

const route = useRoute()

const from = computed(() => String(route.query.from ?? ''))
const to = computed(() => String(route.query.to ?? ''))
const operator = computed(() => String(route.query.operator ?? ''))

const nextData = ref<NextResponse | null>(null)
const timetable = ref<TimetableResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  if (!from.value || !to.value) return

  loading.value = true
  error.value = null
  try {
    const q = new URLSearchParams({ from: from.value, to: to.value, operator: operator.value })

    const [nextRes, ttRes] = await Promise.all([
      fetch(`/api/v1/next?${q}`),
      fetch(`/api/v1/timetable?${q}`),
    ])

    if (!nextRes.ok) throw new Error(`NEXT HTTP ${nextRes.status}`)
    if (!ttRes.ok) throw new Error(`TT HTTP ${ttRes.status}`)

    nextData.value = await nextRes.json()
    timetable.value = await ttRes.json()
  } catch (e: any) {
    error.value = e?.message ?? 'Failed to load data'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.query, load)
</script>

<template>
  <main style="max-width: 520px; margin: 0 auto; padding: 16px;">
    <h1>{{ from }} → {{ to }}</h1>
    <p style="opacity:.7">{{ operator }}</p>

    <p v-if="loading">Loading…</p>
    <p v-else-if="error">{{ error }}</p>

    <section v-else>
      <h2>Next</h2>
      <div v-if="nextData" style="padding: 12px; border: 1px solid #ddd; border-radius: 12px;">
        <div><b>In:</b> {{ nextData.minutesUntil }} min</div>
        <div><b>Dep:</b> {{ nextData.trip.departureTime }}</div>
        <div><b>Arr:</b> {{ nextData.trip.arrivalTime }}</div>

        <ol style="margin-top: 10px;">
          <li v-for="s in nextData.trip.stops" :key="s.sequence">
            {{ s.sequence }}. {{ s.stopName }} — {{ s.time }}
          </li>
        </ol>
      </div>

      <h2 style="margin-top: 18px;">Timetable ({{ timetable?.date }})</h2>
      <div v-if="timetable" style="display: grid; gap: 12px;">
        <div
          v-for="t in timetable.trips"
          :key="t.tripId"
          style="padding: 12px; border: 1px solid #eee; border-radius: 12px;"
        >
          <div style="font-weight:600">{{ t.departureTime }} → {{ t.arrivalTime }}</div>
          <div style="opacity:.7; font-size: 13px;">Trip: {{ t.tripId }}</div>
        </div>
      </div>
    </section>
  </main>
</template>