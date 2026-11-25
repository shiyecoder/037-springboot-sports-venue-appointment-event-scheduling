<template>
  <div class="pbody" style="padding-top:22px">
    <div class="sec"><h3>岗位工作台</h3><div class="sp"></div><span class="tip">{{ user.realName }} · {{ user.roleName }}</span></div>

    <div class="kpis">
      <div class="kpi"><div class="lb">待审批预约</div><div class="vl num">{{ counts.approve }}</div><div class="df">团体预约与超时申请</div></div>
      <div class="kpi"><div class="lb">待核验入场</div><div class="vl num">{{ counts.entry }}</div><div class="df">今日应到</div></div>
      <div class="kpi"><div class="lb">待归还设备</div><div class="vl num">{{ counts.borrow }}</div><div class="df">超期需提醒</div></div>
      <div class="kpi"><div class="lb">待处理维护</div><div class="vl num">{{ counts.maintenance }}</div><div class="df">影响时段已锁定</div></div>
      <div class="kpi"><div class="lb">待处理申诉</div><div class="vl num">{{ counts.appeal }}</div><div class="df">需 3 个工作日内答复</div></div>
      <div class="kpi"><div class="lb">待确认场地</div><div class="vl num">{{ counts.confirm }}</div><div class="df">个人预约即时确认</div></div>
    </div>

    <div class="grid2">
      <div class="card">
        <div class="hd"><h3>待我处理</h3><div class="sp"></div><span class="tip">按时间排序</span></div>
        <table>
          <thead><tr><th>单据</th><th>场馆</th><th class="nowrap">时段</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="r in pending" :key="r.id" style="cursor:pointer" @click="$router.push('/bookings/' + r.id)">
              <td class="no">{{ r.bookingNo }}<div class="cellsub">{{ r.teamName }}</div></td>
              <td>{{ r.venueName }}</td>
              <td class="num nowrap">{{ fmtShort(r.startTime) }}</td>
              <td><Tag :code="r.status" /></td>
            </tr>
            <tr v-if="!pending.length"><td colspan="4" style="text-align:center;color:var(--text3);padding:30px">当前没有待处理单据</td></tr>
          </tbody>
        </table>
      </div>
      <div class="card">
        <div class="hd"><h3>今日场地占用</h3><div class="sp"></div><span class="tip">{{ today }}</span></div>
        <div class="bd">
          <div v-for="s in slots" :key="s.id" class="slot">
            <div>
              <div class="cellmain">{{ s.venueName }}</div>
              <div class="cellsub num">{{ s.startTime }} — {{ s.endTime }}</div>
            </div>
            <div style="display:flex;align-items:center;gap:10px">
              <div class="prog" style="width:90px"><i :style="{ width: usePct(s) + '%' }"></i></div>
              <Tag :code="s.status" />
            </div>
          </div>
          <div v-if="!slots.length" class="tip">今日暂无时段数据</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../http'
import Tag from '../components/Tag.vue'
import { store } from '../store'
import { fmtShort } from '../utils/format'

const user = computed(() => store.user || {})
const counts = ref({ approve: 0, entry: 0, borrow: 0, maintenance: 0, appeal: 0, confirm: 0 })
const pending = ref([])
const slots = ref([])
const today = new Date().toISOString().slice(0, 10)
const usePct = s => Math.round(((s.bookedCount || 0) / (s.capacity || 1)) * 100)

async function load() {
  const [a, c, e] = await Promise.all([
    http.get('/booking/page', { params: { page: 1, size: 6, status: 'PENDING_APPROVAL' } }),
    http.get('/booking/page', { params: { page: 1, size: 6, status: 'PENDING_CONFIRM' } }),
    http.get('/booking/page', { params: { page: 1, size: 6, status: 'PENDING_ENTRY' } })
  ])
  counts.value.approve = a.total || 0
  counts.value.confirm = c.total || 0
  counts.value.entry = e.total || 0
  pending.value = [...(a.list || []), ...(c.list || [])].slice(0, 6)
  if (pending.value[0] && !pending.value[0].venueName) {
    const vs = await http.get('/venue/options')
    const map = Object.fromEntries(vs.map(v => [v.id, v.venueName]))
    pending.value.forEach(p => { p.venueName = map[p.venueId] || '场馆 #' + p.venueId })
  }
  try {
    const bw = await http.get('/equipment/borrow/page', { params: { page: 1, size: 50, status: 'BORROWED' } })
    counts.value.borrow = (bw.list || []).length
  } catch (err) { counts.value.borrow = 0 }
  try {
    const mt = await http.get('/maintenance/page', { params: { page: 1, size: 50, status: 'PENDING' } })
    counts.value.maintenance = mt.total || 0
  } catch (err) { counts.value.maintenance = 0 }
  try {
    const ap = await http.get('/violation/appeal/page')
    counts.value.appeal = (Array.isArray(ap) ? ap : ap.list || []).filter(x => x.status === 'PENDING').length
  } catch (err) { counts.value.appeal = 0 }
  const cal = await http.get('/calendar', { params: { start: today, end: today } })
  const list = Array.isArray(cal) ? cal : cal.list || []
  const vs = await http.get('/venue/options')
  const map = Object.fromEntries(vs.map(v => [v.id, v.venueName]))
  slots.value = list.slice(0, 8).map(s => ({ ...s, venueName: map[s.venueId] || '场馆 #' + s.venueId }))
}

onMounted(load)
</script>
<style scoped>
.sec { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.sec h3 { font-size: 15px; font-weight: 650; }
.pbody { max-width: 1440px; margin: 0 auto; }
.kpi { background: #fff; }
.slot { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px dashed #E9F0F5; }
.slot:last-child { border-bottom: none; }
</style>
