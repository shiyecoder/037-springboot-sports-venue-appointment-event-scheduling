<template>
  <div>
    <div class="ph">
      <div>
        <h1>预约管理</h1>
        <p>个人与团体预约的统一受理台：审批、场地确认、入场核验、离场均在此流转</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="load">刷新</button>
        <button class="btn pri" @click="$router.push('/bookings/new')">新建预约</button>
      </div>
    </div>

    <div class="card">
      <div class="tabs">
        <a v-for="t in tabs" :key="t.key" :class="{ on: tab === t.key }" @click="switchTab(t.key)">
          {{ t.label }} <b class="num">{{ t.count }}</b>
        </a>
      </div>
      <div class="filters">
        <div class="fld">
          <input v-model="q.keyword" class="inp w220" placeholder="单号 / 申请人 / 用途" @keyup.enter="search" />
        </div>
        <div class="fld">
          <select v-model="q.venueId" class="sel">
            <option value="">全部场馆</option>
            <option v-for="v in venues" :key="v.id" :value="v.id">{{ v.venueName }}</option>
          </select>
        </div>
        <div class="fld">
          <select v-model="q.status" class="sel">
            <option value="">全部状态</option>
            <option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v[0] }}</option>
          </select>
        </div>
        <div class="sp"></div>
        <button class="btn" @click="reset">重置</button>
        <button class="btn pri" @click="search">查询</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>预约单号</th>
            <th>场馆 / 时段</th>
            <th>申请人 / 团体</th>
            <th>类型</th>
            <th class="nowrap">人数</th>
            <th class="nowrap">费用</th>
            <th>状态</th>
            <th class="nowrap">创建时间</th>
            <th class="nowrap">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="no">{{ r.bookingNo }}<div class="cellsub">{{ r.bookingCode }}</div></td>
            <td>
              <div class="cellmain">{{ venueName(r.venueId) }}</div>
              <div class="cellsub">{{ fmtShort(r.startTime) }} — {{ timeOf(r.endTime) }}</div>
            </td>
            <td>{{ r.teamName }}<div class="cellsub">{{ r.contactPhone }}</div></td>
            <td>{{ BOOKING_TYPE[r.bookingType] || r.bookingType }}</td>
            <td class="num">{{ r.memberCount }} / {{ r.capacityLimit }}</td>
            <td class="num">{{ fmtMoney(r.feeAmount) }}
              <div class="cellsub">{{ FEE_STATUS[r.feeStatus] || r.feeStatus }}</div>
            </td>
            <td><Tag :code="r.status" /></td>
            <td class="num nowrap">{{ fmtTime(r.createTime) }}</td>
            <td class="nowrap">
              <div class="ops">
                <button class="btn sm lnk" @click="$router.push('/bookings/' + r.id)">详情</button>
                <button v-if="canAct(r)" class="btn sm" @click="openAudit(r)">{{ actLabel(r.status) }}</button>
              </div>
            </td>
          </tr>
          <tr v-if="!list.length">
            <td colspan="9" style="text-align:center;color:var(--text3);padding:34px">
              当前筛选无记录，清空筛选条件或新建预约
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pager">
        <span>共 {{ total }} 条</span>
        <div class="sp"></div>
        <el-pagination
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="p => { page = p; load() }"
          @size-change="s => { size = s; page = 1; load() }"
        />
      </div>
    </div>

    <!-- 审批 / 流转抽屉，保留单据摘要 -->
    <el-drawer v-model="drawer" :title="auditTitle" size="480px">
      <div v-if="cur">
        <div class="dl one" style="margin-bottom:14px">
          <div class="r"><span class="k">预约单号</span><span class="v">{{ cur.bookingNo }}</span></div>
          <div class="r"><span class="k">场馆</span><span class="v">{{ venueName(cur.venueId) }}</span></div>
          <div class="r"><span class="k">时段</span><span class="v">{{ fmtShort(cur.startTime) }} — {{ timeOf(cur.endTime) }}</span></div>
          <div class="r"><span class="k">申请人</span><span class="v">{{ cur.teamName }} · {{ cur.contactPhone }}</span></div>
          <div class="r"><span class="k">人数</span><span class="v">{{ cur.memberCount }} 人 / 容量 {{ cur.capacityLimit }}</span></div>
          <div class="r"><span class="k">用途</span><span class="v">{{ cur.purpose }}</span></div>
          <div class="r"><span class="k">设备</span><span class="v">{{ cur.equipmentDesc || '无' }}</span></div>
        </div>

        <!-- 冲突协同评估：规则命中逐项留痕 -->
        <div class="card" v-if="ev">
          <div class="hd"><h3>协同规则评估</h3><div class="sp"></div><Tag :code="ev.grade" /></div>
          <div class="bd">
            <div class="score-line">
              <b class="num">{{ ev.totalScore }}</b><span>分 · {{ ev.ruleVersion }}</span>
            </div>
            <div v-for="f in ev.factors || []" :key="f.key" class="factor">
              <div class="fk">{{ f.name }}<em>{{ f.weight }}%</em></div>
              <div class="prog" :class="f.score >= 80 ? 'g' : (f.score >= 60 ? '' : 'r')"><i :style="{ width: f.score + '%' }"></i></div>
              <div class="fv">{{ f.value }} · 得分 {{ f.score }}</div>
            </div>
          </div>
        </div>

        <div class="fl" style="margin-top:14px">
          <label>处理意见</label>
          <div class="v">
            <el-input v-model="reason" type="textarea" :rows="3" placeholder="驳回或转协调时必填，不少于 5 个字" />
          </div>
        </div>
        <div class="fl" v-if="needActual">
          <label>实到人数</label>
          <div class="v"><el-input v-model.number="actualCount" style="width:140px" /></div>
        </div>
      </div>
      <template #footer>
        <button class="btn" @click="drawer = false">取消</button>
        <button class="btn dgr" v-if="cur && cur.status === 'PENDING_APPROVAL'" @click="submit('reject')">驳回</button>
        <button class="btn pri" @click="submit(mainAction)">{{ actLabel(cur && cur.status) }}</button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../http'
import Tag from '../components/Tag.vue'
import { BOOKING_TYPE, FEE_STATUS, STATUS, fmtMoney, fmtShort, fmtTime } from '../utils/format'

const statusMap = STATUS
const list = ref([])
const venues = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tab = ref('')
const q = reactive({ keyword: '', venueId: '', status: '' })
const drawer = ref(false)
const cur = ref(null)
const ev = ref(null)
const reason = ref('')
const actualCount = ref(null)

const tabs = computed(() => [
  { key: '', label: '全部', count: total.value },
  { key: 'todo', label: '待我处理', count: list.value.filter(canAct).length },
  { key: 'PENDING_APPROVAL', label: '待审批', count: 0 },
  { key: 'PENDING_ENTRY', label: '待入场', count: 0 },
  { key: 'COMPLETED', label: '已完成', count: 0 }
])

const auditTitle = computed(() => (cur.value ? actLabel(cur.value.status) + ' · ' + cur.value.bookingNo : '预约处理'))
const needActual = computed(() => cur.value && ['PENDING_ENTRY', 'IN_PROGRESS'].includes(cur.value.status))
const mainAction = computed(() => {
  const s = cur.value && cur.value.status
  return { PENDING_APPROVAL: 'approve', PENDING_CONFIRM: 'confirm', PENDING_ENTRY: 'checkin', IN_PROGRESS: 'complete' }[s] || 'confirm'
})

const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || ('场馆 #' + id)
const timeOf = v => (v ? String(v).replace('T', ' ').slice(11, 16) : '—')

// 按状态给出可执行动作：只有落在当前状态的单据才显示按钮
function canAct(r) {
  return ['PENDING_APPROVAL', 'PENDING_CONFIRM', 'PENDING_ENTRY', 'IN_PROGRESS', 'WAITLIST'].includes(r.status)
}
function actLabel(s) {
  return { PENDING_APPROVAL: '审批', PENDING_CONFIRM: '场地确认', PENDING_ENTRY: '核验入场', IN_PROGRESS: '离场检查', WAITLIST: '处理候补' }[s] || '处理'
}

async function load() {
  const params = { page: page.value, size: size.value }
  if (q.keyword) params.keyword = q.keyword
  if (q.venueId) params.venueId = q.venueId
  if (q.status) params.status = q.status
  if (tab.value && tab.value !== 'todo') params.tab = tab.value
  const d = await http.get('/booking/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}

function search() { page.value = 1; load() }
function reset() { q.keyword = ''; q.venueId = ''; q.status = ''; tab.value = ''; page.value = 1; load() }
function switchTab(k) { tab.value = k; page.value = 1; load() }

async function openAudit(r) {
  cur.value = r
  reason.value = ''
  actualCount.value = r.memberCount
  drawer.value = true
  ev.value = null
  try { ev.value = await http.post('/booking/' + r.id + '/evaluate') } catch (e) { /* 已有评估记录时静默 */ }
}

async function submit(action) {
  if ((action === 'reject' || action === 'cancel') && (!reason.value || reason.value.length < 5)) {
    return
  }
  const body = { action, reason: reason.value || null, actualCount: actualCount.value }
  await http.post('/booking/' + cur.value.id + '/transition', body)
  drawer.value = false
  load()
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>

<style scoped>
.tabs a { cursor: pointer; }
.score-line { display: flex; align-items: baseline; gap: 6px; margin-bottom: 12px; }
.score-line b { font-size: 26px; color: var(--primary); font-weight: 660; }
.score-line span { font-size: 11.5px; color: var(--text3); }
.factor { margin-bottom: 9px; }
.fk { display: flex; justify-content: space-between; font-size: 12px; color: var(--text2); margin-bottom: 4px; }
.fk em { font-style: normal; color: var(--text3); font-size: 11px; }
.fv { font-size: 11px; color: var(--text3); margin-top: 3px; }
</style>
