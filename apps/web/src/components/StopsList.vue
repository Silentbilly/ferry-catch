<script setup>
const props = defineProps({
  stops: {
    type: Array,
    required: true,
  },
})
</script>

<template>
  <ol style="margin: 10px 0; padding-left: 18px;">
    <li v-for="s in props.stops" :key="s.sequence" style="margin: 6px 0;">
      {{ s.sequence }}. {{ s.stopName }} — {{ s.time }}
    </li>
  </ol>
</template>
