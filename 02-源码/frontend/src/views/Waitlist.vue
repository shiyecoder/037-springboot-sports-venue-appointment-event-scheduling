<template>
  <div>
    <div class="ph">
      <div><h1>候补管理</h1><p>时段约满后按排队序号候补，空位释放后 30 分钟内确认，逾期自动顺延下一位</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="load">刷新</button></div>
    </div>
    <div class="card">
      <div class="filters">
        <div class="fld">
          <select v-model="status" class="sel" @change="load">
            <option value="">全部状态</option>
            <option value="WAITING">候补中</option>
            <option value="CONFIRMED">已转正</option>
            <option value="EXPIRED">已过期</option>
          </select>
        </div>
        <div class="sp"></div>
        <span class="tip">共 {{ total }} 条候补记录</span>
      </div>
      <table>
        <thead><tr><th>候补单号</th><th>场馆 / 时段</th><th>排队序号</th><th class="nowrap">人数</th><th>状态</th><th class="nowrap">通知时间</th><th class="nowrap">确认截止</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="no">{{ r.waitlistNo }}</td>
            <td>{{ venueName(r.venueId) }}<div class="cellsub">时段 #{{ r.slotId }}</div></td>
            <td class="num">第 {{ r.queueNo }} 位</td>
            <td class="num">{{ r.memberCount }}</td>
            <td><Tag :code="r.status" /></td>
            <td class="num nowrap">{{ fmtTime(r.notifyTime) }}</td>
            <td class="num nowrap">{{ fmtTime(r.confirmDeadline) }}</td>
            <td class="nowrap">
              <button v-if="r.status === 'WAITING'" class="btn sm" @click="confirm(r)">确认候补</button>
              <span v-else class="tip">—</span>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" style="text-align:center;color:var(--text3);padding:30px">暂无候补记录</td></tr>
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
import { ElMessage } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtTime } from '../utils/format'

const list = ref([])
const venues = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')

const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '场馆 #' + id

async function load() {
  const params = { page: page.value, size: size.value }
  if (status.value) params.status = status.value
  const d = await http.get('/waitlist/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}

async function confirm(r) {
  await http.post('/waitlist/' + r.id + '/confirm')
  ElMessage.success('候补已转正，请到预约管理查看')
  load()
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>
