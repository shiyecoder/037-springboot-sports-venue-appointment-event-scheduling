<template>
  <div>
    <div class="ph">
      <div><h1>赛程编排</h1><p>按场馆维度查看单日赛程占用，同一队伍两场之间需留出 ≥45 分钟休息</p></div>
      <div class="sp"></div>
      <div class="acts">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width:150px" @change="load" />
        <button class="btn pri" @click="$router.push('/tournaments')">赛事台账</button>
      </div>
    </div>

    <div class="card">
      <div class="hd"><h3>{{ date }} 场馆赛程占用</h3><div class="sp"></div>
        <div class="legend"><span><i style="background:#0F3B5D"></i>已排场次</span><span><i style="background:#EAF1F6"></i>空闲</span></div>
      </div>
      <div class="bd np" style="padding:0 0 8px">
        <table class="gantt">
          <thead>
            <tr>
              <th style="width:200px;text-align:left;padding-left:14px">场馆</th>
              <th v-for="h in hours" :key="h">{{ h }}:00</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in venues" :key="v.id">
              <td style="padding-left:14px">
                <div class="cellmain">{{ v.venueName }}</div>
                <div class="cellsub">{{ v.sportType }} · 容量 {{ v.capacity }}</div>
              </td>
              <td :colspan="hours.length" style="position:relative;padding:6px 0">
                <div class="lane" style="position:relative;height:26px;background:repeating-linear-gradient(90deg,#F4F8FB 0,#F4F8FB 6.8%,#EDF3F8 6.8%,#EDF3F8 7.14%)">
                  <div v-for="m in matchesOf(v.id)" :key="m.id" class="track"
                    :style="{ position:'absolute', left: pct(m.startTime) + '%', width: widthPct(m) + '%', background:'#0F3B5D' }"
                    :title="m.matchNo + ' ' + m.homeTeam + ' vs ' + m.awayTeam">
                    {{ m.matchNo }} {{ m.homeTeam }}
                  </div>
                </div>
              </td>
            </tr>
            <tr v-if="!venues.length"><td colspan="15" style="text-align:center;color:var(--text3);padding:30px">无场馆数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="hd"><h3>当日场次明细</h3><div class="sp"></div><span class="tip">{{ matches.length }} 场</span></div>
      <table>
        <thead><tr><th>场次</th><th>赛事</th><th>对阵</th><th>场馆</th><th class="nowrap">时间</th><th>裁判</th></tr></thead>
        <tbody>
          <tr v-for="m in matches" :key="m.id">
            <td class="no">{{ m.matchNo }}</td>
            <td>{{ tourName(m.tournamentId) }}</td>
            <td class="cellmain">{{ m.homeTeam }} vs {{ m.awayTeam }}</td>
            <td>{{ venueName(m.venueId) }}</td>
            <td class="num nowrap">{{ shortTime(m.startTime) }} — {{ shortTime(m.endTime) }}</td>
            <td>{{ refName(m.refereeId) }}</td>
          </tr>
          <tr v-if="!matches.length"><td colspan="6" style="text-align:center;color:var(--text3);padding:30px">当日无排期，请到赛事详情编排赛程</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../http'

const date = ref(new Date().toISOString().slice(0, 10))
const venues = ref([])
const matches = ref([])
const tours = ref([])
const referees = ref([])
const hours = Array.from({ length: 14 }, (_, i) => String(i + 8).padStart(2, '0'))

const matchesOf = id => matches.value.filter(m => m.venueId === id)
const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '—'
const tourName = id => (tours.value.find(t => t.id === id) || {}).tournamentName || '—'
const refName = id => (referees.value.find(r => r.id === id) || {}).realName || '—'
const shortTime = x => (x ? String(x).replace('T', ' ').slice(11, 16) : '—')

// 一天 08:00-22:00 共 14 小时，按小时宽度折算百分比
function pct(t) {
  const h = Number(String(t).slice(11, 13)) + Number(String(t).slice(14, 16)) / 60
  return Math.max(0, Math.min(100, ((h - 8) / 14) * 100))
}
function widthPct(m) {
  return Math.max(4, pct(m.endTime) - pct(m.startTime))
}

async function load() {
  const d = await http.get('/tournament/gantt', { params: { date: date.value } })
  venues.value = d.venues || []
  matches.value = d.matches || []
  referees.value = d.referees || []
}

onMounted(async () => {
  const t = await http.get('/tournament/page', { params: { page: 1, size: 50 } })
  tours.value = t.list || []
  load()
})
</script>
<style scoped>
.gantt th { border-right: 1px solid #EDF3F8; }
</style>
