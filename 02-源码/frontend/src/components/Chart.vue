<template>
  <div ref="box" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

// 通用 ECharts 容器：option 变化即重绘，容器尺寸变化自动 resize
const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '152px' }
})

const box = ref(null)
let chart = null
let ro = null

onMounted(() => {
  chart = echarts.init(box.value)
  chart.setOption(props.option || {})
  ro = new ResizeObserver(() => chart && chart.resize())
  ro.observe(box.value)
})

watch(() => props.option, o => { if (chart && o) chart.setOption(o, true) }, { deep: true })

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
  if (chart) chart.dispose()
})
</script>
