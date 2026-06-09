<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  height: {
    type: String,
    default: '280px'
  }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart && chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  if (chart) {
    chart.setOption(props.option, true)
  }
}

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartRef.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartRef" class="echart-box" :style="{ height }" />
</template>
