<template>
  <div>
    <div class="ph">
      <div>
        <h1>预约周历</h1>
        <p>按场馆与周次查看时段占用，已满时段可转候补排队</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="shift(-7)">上一周</button>
        <button class="btn" @click="shift(7)">下一周</button>
        <button class="btn pri" @click="$router.push('/bookings/new')">新建预约</button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <div class="fld">
          <select v-model="venueId" class="sel" @change="load">
            <option v-for="x in venues" :key="x.id" :value="x.id">{{ x.venueName }}</option>
          </select>
        </div>
        <div class="fld"><span class="tip">{{ start }} 至 {{ end }}</span></div>
        <div class="sp"></div>
        <div class="legend">
          <span><i style="background:#EAF1F6"></i>可预约</span>
          <span><i style="background:#A9711F"></i>已约满</span>
          <span><i style="background:#0F3B5D"></i>使用中</span>
          <span><i style="background:#8A9AA8"></i>维护锁定</span>
        </div>
      </div>
      <div style="overflow:auto">
        <table class="gantt">
          <thead>
            <tr>
              <th style="width:96px">时段</th>
              <th v-for="d in days" :key="d">{{ d }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="h in hours" :key="h">
              <td class="num" style="color:var(--text2)">{{ h }}:00</td>
              <td v-for="d in days" :key="d" @click="cellClick(d, h)">
                <div v-if="cell(d, h)" class="track" :style="{ background: color(cell(d, h).status) }">
                  {{ cell(d, h).bookedCount }}/{{ cell(d, h).capacity }}
                </div>
                <div v-else class="track" style="background:#F4F8FB;color:#B7C7D3">—</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../http'

const route = useRoute()
const venues = ref([])
const venueId = ref(null)
const slots = ref([])
const start = ref('')
const end = ref('')

const hours = Array.from({ length: 14 }, (_, i) => String(i + 8).padStart(2, '0'))
const days = ref([])

const color = s => ({ FREE: '#EAF1F6', FULL: '#A9711F', IN_PROGRESS: '#0F3B5D', LOCKED: '#8A9AA8' }[s] || '#EAF1F6')

function cell(day, h) {
  return slots.value.find(s => s.slotDate === day && String(s.startTime).slice(0, 2) === h)
}

function fmt(d) {
  const x = new Date(d)
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`
}

function range(offset = 0) {
  const base = new Date()
  base.setDate(base.getDate() + offset)
  const arr = []
  for (let i = 0; i < 7; i++) {
    const x = new Date(base)
    x.setDate(base.getDate() + i)
    arr.push(fmt(x))
  }
  return arr
}

async function load() {
  const d = await http.get('/calendar', { params: { venueId: venueId.value, start: start.value, end: end.value } })
  slots.value = Array.isArray(d) ? d : d.list || []
}

async function shift(n) {
  const base = new Date(start.value)
  base.setDate(base.getDate() + n)
  const arr = range(0)
  start.value = fmt(base)
  const e = new Date(base); e.setDate(base.getDate() + 6)
  end.value = fmt(e)
  days.value = Array.from({ length: 7 }, (_, i) => { const x = new Date(base); x.setDate(base.getDate() + i); return fmt(x) })
  load()
}

function cellClick(d, h) {
  const s = cell(d, h)
  if (!s || s.status === 'FREE') window.location.href = '/bookings/new?venueId=' + venueId.value + '&date=' + d + '&hour=' + h
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  const q = route.query.venueId
  venueId.value = q ? Number(q) : (venues.value[0] && venues.value[0].id)
  days.value = range(0)
  start.value = days.value[0]
  end.value = days.value[6]
  load()
})
</script>
