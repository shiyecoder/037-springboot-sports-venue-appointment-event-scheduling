<template>
  <span class="tag" :class="statusClass(code)">{{ statusText(code) }}</span>
</template>

<script setup>
import { statusClass, statusText } from '../utils/format'
defineProps({ code: { type: String, default: '' } })
</script>
