<template>
  <div class="dashboard-sample">
    <div ref="chart" style="width:100%;height:320px"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, ref } from 'vue'

export default {
  name: 'DashboardSample',
  setup() {
    const chart = ref(null)
    let instance = null
    onMounted(() => {
      instance = echarts.init(chart.value)
      const option = {
        xAxis: { type: 'category', data: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'] },
        yAxis: { type: 'value' },
        series: [{ data: [120, 200, 150, 80, 70, 110, 130], type: 'bar' }]
      }
      instance.setOption(option)
      window.addEventListener('resize', () => instance.resize())
    })
    return { chart }
  }
}
</script>

<style scoped>
.dashboard-sample { padding: 8px }
</style>
