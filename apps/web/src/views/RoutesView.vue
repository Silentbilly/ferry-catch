<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { RouteDto } from '../api/types'

const router = useRouter()
const routes = ref<RouteDto[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function loadRoutes() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch('/api/v1/routes')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    routes.value = await res.json()
  } catch (e: any) {
    error.value = e?.message ?? 'Failed to load routes'
  } finally {
    loading.value = false
  }
}

function openRoute(r: RouteDto) {
  router.push({
    name: 'route-details',
    query: { from: r.from, to: r.to, operator: r.operator },
  })
}

onMounted(loadRoutes)
</script>

<template>
  <main style="max-width: 520px; margin: 0 auto; padding: 16px;">
    <h1>Routes</h1>

    <p v-if="loading">Loading…</p>
    <p v-else-if="error">{{ error }}</p>

    <ul v-else style="list-style: none; padding: 0; display: grid; gap: 12px;">
      <li v-for="r in routes" :key="r.id">
        <button
          style="width:100%; padding: 14px; border-radius: 12px;"
          @click="openRoute(r)"
        >
          <div style="font-weight:600">{{ r.from }} → {{ r.to }}</div>
          <div style="opacity:.7">{{ r.operator }}</div>
        </button>
      </li>
    </ul>
  </main>
</template>
