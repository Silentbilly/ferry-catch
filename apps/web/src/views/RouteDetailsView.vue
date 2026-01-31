<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { NextResponse, TimetableResponse } from '../api/types'
import { getNextDeparture, getTimetable } from '../api'
import { ApiError } from '../api/client'

const route = useRoute()

const from = computed(() => String(route.query.from ?? ''))
const to = computed(() => String(route.query.to ?? ''))
const operator = computed(() => String(route.query.operator ?? ''))

const nextData = ref<NextResponse | null>(null)
const timetable = ref<TimetableResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

function buildQuery() {
  const op = operator.value.trim()
  return {
    from: from.value,
    to: to.value,
    operator: op ? op : undefined,
  }
}

async function load() {
  if (!from.value || !to.value) return

  loading.value = true
  error.value = null

  try {
    const q = buildQuery()

    const [next, tt] = await Promise.all([
      getNextDeparture(q),
      getTimetable(q),
    ])

    nextData.value = next
    timetable.value = tt
  } catch (e: unknown) {
    if (e instanceof ApiError) {
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message
    } else if (e instanceof Error) {
      error.value = e.message
    } else {
      error.value = 'Failed to load data'
    }

    nextData.value = null
    timetable.value = null
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
    <p style="opacity:.7" v-if="operator">{{ operator }}</p>

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
