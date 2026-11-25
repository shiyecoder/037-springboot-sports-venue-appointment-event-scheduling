<template>
  <div class="pbody" style="padding-top:22px">
    <div class="sec">
      <h3>我的预约</h3>
      <div class="sp"></div>
      <button class="btn pri" @click="$router.push('/bookings/new')">新建预约</button>
    </div>
    <div class="card">
      <div class="tabs">
        <a v-for="t in tabs" :key="t.key" :class="{ on: tab === t.key }" @click="switchTab(t.key)">{{ t.label }}</a>
      </div>
      <table>
        <thead><tr><th>单号</th><th>场馆</th><th class="nowrap">时段</th><th class="nowrap">人数</th><th class="nowrap">费用</th><th>状态</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="no">{{ r.bookingNo }}<div class="cellsub">{{ r.bookingCode }}</div></td>
            <td class="cellmain">{{ venueName(r.venueId) }}</td>
            <td class="num nowrap">{{ fmtShort(r.startTime) }} — {{ timeOf(r.endTime) }}</td>
            <td class="num">{{ r.memberCount }}</td>
            <td class="num">{{ fmtMoney(r.feeAmount) }}</td>
            <td><Tag :code="r.status" /></td>
            <td class="nowrap">
              <button class="btn sm lnk" @click="$router.push('/bookings/' + r.id)">详情</button>
              <button v-if="canCancel(r)" class="btn sm dgr" @click="cancel(r)">取消</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="7" style="text-align:center;color:var(--text3);padding:34px">暂无预约，去场馆资源看看</td></tr>
        </tbody>
      </table>
      <div class="pager">
        <span>共 {{ total }} 条</span><div class="sp"></div>
        <el-pagination layout="prev, pager, next" :total="total" :current-page="page" :page-size="size" @current-change="p => { page = p; load() }" />
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtMoney, fmtShort } from '../utils/format'

const list = ref([])
const venues = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tab = ref('')
const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING_ENTRY', label: '待入场' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '场馆 #' + id
const timeOf = x => (x ? String(x).replace('T', ' ').slice(11, 16) : '—')
const canCancel = r => ['PENDING_APPROVAL', 'PENDING_CONFIRM', 'PENDING_ENTRY'].includes(r.status)

async function load() {
  const params = { page: page.value, size: size.value }
  if (tab.value) params.status = tab.value
  const d = await http.get('/booking/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}
function switchTab(k) { tab.value = k; page.value = 1; load() }

async function cancel(r) {
  const res = await ElMessageBox.prompt('请填写取消原因（不少于 5 个字）', '取消预约', { inputPlaceholder: '如：课程冲突，无法按时到场' })
  await http.post('/booking/' + r.id + '/transition', { action: 'cancel', reason: res.value })
  load()
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>
<style scoped>
.sec { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.sec h3 { font-size: 15px; font-weight: 650; }
.pbody { max-width: 1440px; margin: 0 auto; }
</style>
