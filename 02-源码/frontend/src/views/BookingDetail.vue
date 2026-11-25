<template>
  <div v-if="b.id">
    <div class="ph">
      <div>
        <h1>{{ b.bookingNo }}</h1>
        <p>{{ v.venueName || ('场馆 #' + b.venueId) }} · {{ fmtShort(b.startTime) }} — {{ timeOf(b.endTime) }}</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="$router.back()">返回</button>
        <button v-for="a in actions" :key="a.action" :class="['btn', a.primary ? 'pri' : '']" @click="doAction(a)">{{ a.label }}</button>
      </div>
    </div>

    <div class="grid2-1">
      <div>
        <div class="card">
          <div class="hd"><h3>单据信息</h3><div class="sp"></div><Tag :code="b.status" /></div>
          <div class="bd">
            <div class="dl">
              <div class="r"><span class="k">预约单号</span><span class="v num">{{ b.bookingNo }}</span></div>
              <div class="r"><span class="k">核销码</span><span class="v num" style="letter-spacing:1px;font-weight:650">{{ b.bookingCode }}</span></div>
              <div class="r"><span class="k">场馆</span><span class="v">{{ v.venueName }}</span></div>
              <div class="r"><span class="k">时段</span><span class="v num">{{ fmtTime(b.startTime) }} — {{ timeOf(b.endTime) }}</span></div>
              <div class="r"><span class="k">申请人</span><span class="v">{{ b.teamName }}</span></div>
              <div class="r"><span class="k">联系电话</span><span class="v num">{{ b.contactPhone }}</span></div>
              <div class="r"><span class="k">预约类型</span><span class="v">{{ BOOKING_TYPE[b.bookingType] }}</span></div>
              <div class="r"><span class="k">人数</span><span class="v num">{{ b.memberCount }} / 容量 {{ b.capacityLimit }}</span></div>
              <div class="r"><span class="k">费用</span><span class="v num">{{ fmtMoney(b.feeAmount) }} · {{ FEE_STATUS[b.feeStatus] }}</span></div>
              <div class="r"><span class="k">随借设备</span><span class="v">{{ b.equipmentDesc || '无' }}</span></div>
              <div class="r"><span class="k">用途</span><span class="v">{{ b.purpose || '—' }}</span></div>
              <div class="r"><span class="k">创建时间</span><span class="v num">{{ fmtTime(b.createTime) }}</span></div>
            </div>
            <div v-if="b.safetyFile || b.memberFile" style="margin-top:12px;display:flex;gap:16px">
              <a v-if="b.safetyFile" :href="b.safetyFile" target="_blank">安全承诺书</a>
              <a v-if="b.memberFile" :href="b.memberFile" target="_blank">成员名单</a>
            </div>
          </div>
        </div>

        <!-- 创新点：规则评分 + 逐项留痕 + 人工修正 -->
        <div class="card" v-if="score">
          <div class="hd"><h3>冲突协同评分</h3><div class="sp"></div><Tag :code="score.grade" /><span class="tip">{{ score.ruleVersion }}</span></div>
          <div class="bd">
            <div class="score-line">
              <b class="num">{{ score.totalScore }}</b><span>分 · 共 6 项因素加权</span>
              <div class="sp" style="flex:1"></div>
              <button class="btn sm" @click="adjust">人工修正</button>
            </div>
            <div v-for="f in factors" :key="f.key" class="factor">
              <div class="fk"><span>{{ f.name }}<em>权重 {{ f.weight }}%</em></span><span>{{ f.score }} 分</span></div>
              <div class="prog" :class="f.score >= 80 ? 'g' : (f.score >= 60 ? '' : 'r')"><i :style="{ width: f.score + '%' }"></i></div>
              <div class="fv">{{ f.value }} · {{ f.rule }}</div>
            </div>
          </div>
        </div>

        <div class="card" v-if="borrows.length">
          <div class="hd"><h3>设备借用</h3></div>
          <table>
            <thead><tr><th>借用单</th><th>数量</th><th>借出</th><th>应还</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="x in borrows" :key="x.id">
                <td class="no">{{ x.borrowNo }}</td>
                <td class="num">{{ x.borrowCount }}</td>
                <td class="nowrap">{{ fmtShort(x.borrowTime) }}</td>
                <td class="nowrap">{{ fmtShort(x.expectReturn) }}</td>
                <td><Tag :code="x.status" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="hd"><h3>流转记录</h3></div>
          <div class="bd">
            <div class="tl">
              <div v-for="(f, i) in flows" :key="f.id" class="it" :class="{ done: true }">
                <span class="pt"></span>
                <div>
                  <div class="tt">{{ f.action }}</div>
                  <div class="tm num">{{ fmtTime(f.createTime) }} · {{ f.operator }}</div>
                  <div v-if="f.reason" class="ds">{{ f.reason }}</div>
                  <div class="tm" style="margin-top:3px">{{ statusText(f.fromStatus) }} → {{ statusText(f.toStatus) }}</div>
                </div>
              </div>
              <div v-if="!flows.length" class="tip">暂无流转记录</div>
            </div>
          </div>
        </div>
        <div class="card" v-if="members.length">
          <div class="hd"><h3>团体成员</h3></div>
          <table>
            <thead><tr><th>姓名</th><th>角色</th><th>电话</th></tr></thead>
            <tbody>
              <tr v-for="(m, i) in members" :key="i"><td>{{ m.memberName }}</td><td>{{ m.role }}</td><td class="num">{{ m.phone }}</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { BOOKING_TYPE, FEE_STATUS, fmtMoney, fmtShort, fmtTime, statusText } from '../utils/format'

const route = useRoute()
const b = ref({})
const v = ref({})
const flows = ref([])
const members = ref([])
const borrows = ref([])
const score = ref(null)

const timeOf = x => (x ? String(x).replace('T', ' ').slice(11, 16) : '—')
const factors = computed(() => {
  if (!score.value) return []
  try { return JSON.parse(score.value.factorJson || '[]') } catch (e) { return [] }
})

const actions = computed(() => {
  const s = b.value.status
  const m = {
    PENDING_APPROVAL: [{ label: '审批通过', action: 'approve', primary: true }, { label: '驳回', action: 'reject' }],
    PENDING_CONFIRM: [{ label: '确认场地', action: 'confirm', primary: true }],
    PENDING_ENTRY: [{ label: '核验入场', action: 'checkin', primary: true }, { label: '登记未到', action: 'no-show' }],
    IN_PROGRESS: [{ label: '离场检查', action: 'complete', primary: true }]
  }
  return m[s] || (['PENDING_APPROVAL', 'PENDING_CONFIRM', 'PENDING_ENTRY'].includes(s) ? [{ label: '取消预约', action: 'cancel' }] : [])
})

async function load() {
  const d = await http.get('/booking/' + route.params.id)
  b.value = d.booking || {}
  v.value = d.venue || {}
  flows.value = d.flows || []
  members.value = d.members || []
  borrows.value = d.borrows || []
  score.value = d.score || null
}

async function doAction(a) {
  let reason = null
  if (a.action === 'reject' || a.action === 'cancel' || a.action === 'no-show') {
    const r = await ElMessageBox.prompt('请填写处理原因（不少于 5 个字）', '处理确认', { inputPlaceholder: '说明原因，将记入流转留痕' })
    reason = r.value
  }
  await http.post('/booking/' + b.value.id + '/transition', { action: a.action, reason, actualCount: b.value.memberCount })
  load()
}

async function adjust() {
  const r = await ElMessageBox.prompt('人工修正说明（将作为留痕写入）', '人工修正评分', { inputPlaceholder: '如：需与前序赛事协调布场时间' })
  await http.post('/rule/adjust/' + score.value.id, { remark: r.value, operator: '当前账号' })
  load()
}

onMounted(load)
</script>

<style scoped>
.score-line { display: flex; align-items: baseline; gap: 8px; margin-bottom: 12px; }
.score-line b { font-size: 26px; color: var(--primary); font-weight: 660; }
.score-line span { font-size: 11.5px; color: var(--text3); }
.factor { margin-bottom: 10px; }
.fk { display: flex; justify-content: space-between; font-size: 12px; color: var(--text2); margin-bottom: 4px; }
.fk em { font-style: normal; color: var(--text3); font-size: 11px; margin-left: 8px; }
.fv { font-size: 11px; color: var(--text3); margin-top: 3px; }
</style>
