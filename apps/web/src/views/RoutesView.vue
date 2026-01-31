<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RouteWithNextDto } from '../api/types'
import { listRoutes } from '../api'
import { ApiError } from '../api/client'

const router = useRouter()

const routes = ref<RouteWithNextDto[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

function formatHHmm(iso?: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) // HH:MM [web:799]
}

function etaText(r: RouteWithNextDto): string {
  const m = r.nextMinutesUntil
  const t = formatHHmm(r.nextDepartureTime)
  if (m == null && !t) return ''
  if (m == null) return t
  if (!t) return `${m}m`
  return `${m}m · ${t}`
}

function etaClass(r: RouteWithNextDto): string {
  const m = r.nextMinutesUntil
  if (m == null) return 'eta eta--unknown'
  if (m < 10) return 'eta eta--soon'
  if (m < 20) return 'eta eta--mid'
  return 'eta eta--ok'
}

async function loadRoutes() {
  loading.value = true
  error.value = null
  try {
    routes.value = await listRoutes()
  } catch (e: unknown) {
    if (e instanceof ApiError) {
      error.value = e.status ? `${e.message} (HTTP ${e.status})` : e.message
    } else if (e instanceof Error) {
      error.value = e.message
    } else {
      error.value = 'Failed to load routes'
    }
  } finally {
    loading.value = false
  }
}

function openRoute(r: RouteWithNextDto) {
  router.push({
    name: 'route-details',
    query: { from: r.from, to: r.to, operator: r.operator },
  })
}

onMounted(loadRoutes)
</script>

<template>
  <main class="page">
    <header class="header">
      <h1 class="h1">Routes</h1>
      <button class="ghostBtn" @click="loadRoutes" :disabled="loading">
        Refresh
      </button>
    </header>

    <p v-if="loading">Loading…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <ul v-else class="list">
      <li v-for="r in routes" :key="r.id">
        <button class="cardBtn" @click="openRoute(r)">
          <div class="row">
            <div class="left">
              <div class="title">{{ r.from }} → {{ r.to }}</div>
              <div class="subtitle">{{ r.operator }}</div>
            </div>

            <div v-if="etaText(r)" :class="etaClass(r)">
              {{ etaText(r) }}
            </div>
          </div>
        </button>
      </li>
    </ul>
  </main>
</template>

<style scoped>
.page {
  max-width: 520px;
  margin: 0 auto;
  padding: 16px;
  font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
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
}

.ghostBtn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
}

.list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 12px;
}

.cardBtn {
  width: 100%;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0,0,0,.06);
}

.cardBtn:hover {
  box-shadow: 0 10px 24px rgba(0,0,0,.10);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.left {
  min-width: 0;
}

.title {
  font-weight: 750;
}

.subtitle {
  opacity: .7;
  margin-top: 2px;
  font-size: 14px;
}

.eta {
  font-weight: 800;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.eta--soon { color: #dc2626; }   /* red */
.eta--mid  { color: #d97706; }   /* amber */
.eta--ok   { color: #047857; }   /* green */
.eta--unknown { color: #6b7280; } /* gray */

.error { color: #dc2626; }
</style>
