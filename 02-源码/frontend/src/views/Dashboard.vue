<template>
  <div>
    <div class="ph">
      <div>
        <h1>场馆经营看板</h1>
        <p>数据截止 {{ today }} · 覆盖 {{ kpi.venueTotal || 18 }} 个在册场馆的时段占用、预约流转与赛事排期</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="load">刷新数据</button>
        <button class="btn pri" @click="$router.push('/bookings/new')">新建预约</button>
      </div>
    </div>

    <!-- 6 项核心指标 -->
    <div class="kpis">
      <div class="kpi">
        <div class="lb">时段利用率</div>
        <div class="vl num">{{ d.kpi?.venueUsage ?? 0 }}<small>%</small></div>
        <div class="df">已约 / 可约时段</div>
      </div>
      <div class="kpi">
        <div class="lb">本月预约</div>
        <div class="vl num">{{ d.kpi?.bookingCount ?? 0 }}<small>单</small></div>
        <div class="df">含个人与团体</div>
      </div>
      <div class="kpi">
        <div class="lb">赛事场次</div>
        <div class="vl num">{{ d.kpi?.matchCount ?? 0 }}<small>场</small></div>
        <div class="df">已编排赛程</div>
      </div>
      <div class="kpi">
        <div class="lb">冲突预警</div>
        <div class="vl num">{{ d.kpi?.conflictCount ?? 0 }}<small>条</small></div>
        <div class="df"><i class="dn">需协调 {{ coordCount }} 条</i></div>
      </div>
      <div class="kpi">
        <div class="lb">入场核验率</div>
        <div class="vl num">{{ d.kpi?.checkinRate ?? 0 }}<small>%</small></div>
        <div class="df">核销 / 应到</div>
      </div>
      <div class="kpi">
        <div class="lb">场地收入</div>
        <div class="vl num">¥{{ d.kpi?.income ?? 0 }}</div>
        <div class="df">近 30 天累计</div>
      </div>
    </div>

    <!-- 图表区：两行共 5 个图 + 1 条采纳率横带 -->
    <div class="grid2-1">
      <div class="card">
        <div class="hd"><h3>近 14 天预约走势</h3><div class="sp"></div><span class="tip">单位：单</span></div>
        <div class="bd"><Chart :option="trendOpt" height="182px" /></div>
      </div>
      <div class="card">
        <div class="hd"><h3>预约状态分布</h3></div>
        <div class="bd"><Chart :option="statusOpt" height="182px" /></div>
      </div>
    </div>

    <div class="grid3">
      <div class="card">
        <div class="hd"><h3>运动项目热度</h3></div>
        <div class="bd"><Chart :option="sportOpt" height="152px" /></div>
      </div>
      <div class="card">
        <div class="hd"><h3>时段占用热度</h3></div>
        <div class="bd"><Chart :option="hourOpt" height="152px" /></div>
      </div>
      <div class="card">
        <div class="hd"><h3>冲突协同评级</h3></div>
        <div class="bd"><Chart :option="gradeOpt" height="152px" /></div>
      </div>
    </div>

    <!-- 规则采纳率横带：创新点执行情况 -->
    <div class="card adopt">
      <div class="adopt-in">
        <div class="at">
          <b>协同规则采纳率 {{ d.adopt?.adoptRate ?? 0 }}%</b>
          <span>规则 {{ ruleVersion }} · 共 {{ d.adopt?.total ?? 0 }} 次评估，人工修正 {{ d.adopt?.adjusted ?? 0 }} 次，每次修正均留痕可追溯</span>
        </div>
        <div class="prog g" style="flex:1;max-width:420px"><i :style="{ width: (d.adopt?.adoptRate || 0) + '%' }"></i></div>
        <div class="achips">
          <span>候补 {{ d.waitlistCount ?? 0 }}</span>
          <span>设备损坏 {{ d.damageCount ?? 0 }}</span>
          <span>违约 {{ d.violationCount ?? 0 }}</span>
          <span>维护中 {{ d.maintenanceCount ?? 0 }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../http'
import Chart from '../components/Chart.vue'
import { statusText } from '../utils/format'

const d = ref({})
const today = new Date().toISOString().slice(0, 10)
const PALETTE = ['#0F3B5D', '#2878B5', '#4FA3D9', '#1F7653', '#A9711F', '#B22B31', '#8A9AA8']
const axis = {
  axisLine: { lineStyle: { color: '#DDE7EF' } },
  axisLabel: { color: '#8A9AA8', fontSize: 10.5 },
  axisTick: { show: false }
}

const kpi = computed(() => d.value.kpi || {})
const ruleVersion = ref('R2026.10-v3')
const coordCount = computed(() => (d.value.gradeDist || []).find(g => g.name === 'COORDINATE')?.value || 0)

const trendOpt = computed(() => {
  const list = d.value.trend || []
  return {
    grid: { left: 6, right: 10, top: 18, bottom: 2, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map(i => i.date), ...axis },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F8' } }, ...axis },
    series: [{
      type: 'line', smooth: true, showSymbol: false, data: list.map(i => i.value),
      lineStyle: { width: 2, color: '#0F3B5D' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(40,120,181,.28)' }, { offset: 1, color: 'rgba(40,120,181,.02)' }]
        }
      }
    }]
  }
})

const statusOpt = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#5A6D7C' } },
  color: PALETTE,
  series: [{
    type: 'pie', radius: ['44%', '66%'], center: ['50%', '44%'], avoidLabelOverlap: true,
    label: { show: false }, labelLine: { show: false },
    data: (d.value.statusDist || []).map(i => ({ name: statusText(i.name), value: i.value }))
  }]
}))

const sportOpt = computed(() => {
  const list = d.value.sportHeat || []
  return {
    grid: { left: 6, right: 14, top: 10, bottom: 2, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F8' } }, ...axis },
    yAxis: { type: 'category', data: list.map(i => i.name), ...axis },
    series: [{ type: 'bar', barWidth: 12, itemStyle: { color: '#2878B5', borderRadius: [0, 3, 3, 0] }, data: list.map(i => i.value) }]
  }
})

const hourOpt = computed(() => {
  const list = d.value.hourHeat || []
  return {
    grid: { left: 6, right: 8, top: 10, bottom: 2, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map(i => i.name), ...axis, axisLabel: { color: '#8A9AA8', fontSize: 9.5, interval: 1 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF3F8' } }, ...axis },
    series: [{ type: 'bar', barWidth: '58%', itemStyle: { color: '#4FA3D9', borderRadius: [3, 3, 0, 0] }, data: list.map(i => i.value) }]
  }
})

const gradeOpt = computed(() => {
  const map = { RECOMMEND: '#1F7653', COORDINATE: '#A9711F', REJECT: '#B22B31' }
  const list = d.value.gradeDist || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#5A6D7C' } },
    series: [{
      type: 'pie', radius: ['46%', '68%'], center: ['50%', '44%'],
      label: { show: true, formatter: '{c}', fontSize: 11, color: '#1B2A38' },
      data: list.map(i => ({ name: statusText(i.name), value: i.value, itemStyle: { color: map[i.name] || '#8A9AA8' } }))
    }]
  }
})

async function load() {
  d.value = await http.get('/dashboard')
  try {
    const r = await http.get('/rule/current')
    if (r && r.rule) ruleVersion.value = r.rule.ruleVersion
  } catch (e) { /* 无规则权限时忽略 */ }
}

onMounted(load)
</script>

<style scoped>
.ph { margin-bottom: 10px; }
.ph p { margin-top: 2px; }
.ph h1 { font-size: 18px; }
.kpi { padding: 7px 12px; }
.kpi .vl { font-size: 19px; margin-top: 2px; }
.kpi .df { margin-top: 1px; }
.card .hd { height: 38px; }
.card .bd { padding: 6px 10px 5px; }
.grid2-1, .grid3 { gap: 12px; margin-bottom: 12px; }
.adopt { margin-bottom: 0; }
.adopt-in { display: flex; align-items: center; gap: 18px; padding: 10px 16px; }
.adopt-in .at b { font-size: 13px; color: var(--primary); display: block; }
.adopt-in .at span { font-size: 11.5px; color: var(--text3); }
.achips { display: flex; gap: 8px; margin-left: auto; }
.achips span { font-size: 11.5px; color: var(--text2); background: #F1F6FA; border: 1px solid #E1EBF3; border-radius: 4px; padding: 3px 8px; }
</style>
