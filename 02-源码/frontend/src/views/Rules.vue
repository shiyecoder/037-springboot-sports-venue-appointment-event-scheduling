<template>
  <div>
    <div class="ph">
      <div><h1>冲突协同规则</h1><p>六项因素加权评分 + 双向阈值判定，规则版本可追溯，人工修正全程留痕</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn pri" @click="save">保存为新版本</button></div>
    </div>

    <div class="grid2-1">
      <div class="card">
        <div class="hd"><h3>当前生效规则 · {{ rule.ruleVersion }}</h3><div class="sp"></div><span class="tip">权重合计 {{ weightSum }}%</span></div>
        <div class="bd">
          <div v-for="w in weights" :key="w.key" class="wrow">
            <div class="wl"><b>{{ w.name }}</b><span class="tip">{{ w.rule }}</span></div>
            <el-slider v-model="w.value" :max="40" :step="5" style="width:220px" />
            <span class="wv num">{{ w.value }}%</span>
          </div>
          <div class="thres">
            <div class="fl"><label>建议通过阈值</label><div class="v"><el-input-number v-model="thresholds.recommend" :min="50" :max="100" /></div></div>
            <div class="fl"><label>需协调阈值</label><div class="v"><el-input-number v-model="thresholds.coordinate" :min="30" :max="95" /></div></div>
          </div>
          <div class="tip" style="margin-top:10px">得分 ≥ {{ thresholds.recommend }} 判定「建议通过」；{{ thresholds.coordinate }}–{{ thresholds.recommend }} 判定「需协调」；&lt; {{ thresholds.coordinate }} 判定「建议驳回」。权重合计必须为 100%。</div>
        </div>
      </div>

      <div class="card">
        <div class="hd"><h3>规则版本历史</h3></div>
        <div class="bd">
          <div v-for="h in history" :key="h.id" class="hrow">
            <div>
              <div class="cellmain">{{ h.ruleVersion }}</div>
              <div class="cellsub">{{ h.ruleName }} · {{ h.createBy }}</div>
            </div>
            <Tag :code="h.status === 'ACTIVE' ? 'NORMAL' : 'FINISHED'" />
          </div>
          <div v-if="!history.length" class="tip">暂无历史版本</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="hd"><h3>计算留痕与人工修正</h3><div class="sp"></div><span class="tip">最近 20 条评估记录</span></div>
      <table>
        <thead><tr><th>业务单号</th><th class="nowrap">总分</th><th>判定</th><th>规则版本</th><th class="nowrap">评估时间</th><th>说明</th></tr></thead>
        <tbody>
          <tr v-for="t in traces" :key="t.id">
            <td class="no">{{ t.bizNo }}</td>
            <td class="num">{{ t.totalScore }}</td>
            <td><Tag :code="t.grade" /></td>
            <td>{{ t.ruleVersion }}</td>
            <td class="num nowrap">{{ fmtTime(t.createTime) }}</td>
            <td>{{ t.remark || '系统自动评估' }}</td>
          </tr>
          <tr v-if="!traces.length"><td colspan="6" style="text-align:center;color:var(--text3);padding:26px">暂无评估记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtTime } from '../utils/format'

const rule = ref({})
const history = ref([])
const traces = ref([])
const thresholds = ref({ recommend: 80, coordinate: 60 })
const weights = ref([
  { key: 'slot', name: '场地时段', value: 25, rule: '同一场地同一时段只允许一条有效占用' },
  { key: 'setup', name: '布撤场时间', value: 15, rule: '布场 60 分钟、撤场 30 分钟，间隔 ≥30 分钟' },
  { key: 'priority', name: '赛事优先级', value: 20, rule: '校级及以上赛事优先于日常预约' },
  { key: 'equipment', name: '器材库存', value: 15, rule: '所选器材可用量需满足申领数量' },
  { key: 'referee', name: '裁判排班', value: 15, rule: '裁判同一时段不得执裁两场' },
  { key: 'capacity', name: '容量匹配', value: 10, rule: '参与人数不得超过场地容量' }
])

const weightSum = computed(() => weights.value.reduce((s, w) => s + w.value, 0))

async function load() {
  const d = await http.get('/rule/current')
  rule.value = d.rule || {}
  history.value = d.history || []
  try {
    const w = JSON.parse(rule.value.weightsJson || '{}')
    weights.value.forEach(x => { if (w[x.key] !== undefined) x.value = w[x.key] })
    if (rule.value.thresholdsJson) thresholds.value = JSON.parse(rule.value.thresholdsJson)
  } catch (e) { /* 解析失败保留默认 */ }
  traces.value = await http.get('/booking/traces', { params: { limit: 20 } })
}

async function save() {
  if (weightSum.value !== 100) return ElMessage.warning('权重合计必须为 100%，当前 ' + weightSum.value + '%')
  const w = {}
  weights.value.forEach(x => { w[x.key] = x.value })
  const v = 'R' + new Date().toISOString().slice(0, 7).replace('-', '.') + '-v' + (history.value.length + 2)
  await http.post('/rule', { name: rule.value.ruleName || '预约与赛事资源冲突协同规则', version: v, weights: w, thresholds: thresholds.value })
  ElMessage.success('新规则版本已生效')
  load()
}

onMounted(load)
</script>
<style scoped>
.wrow { display: flex; align-items: center; gap: 14px; padding: 9px 0; border-bottom: 1px dashed #E9F0F5; }
.wrow:last-of-type { border-bottom: none; }
.wl { width: 260px; }
.wl b { font-size: 12.5px; display: block; }
.wv { width: 44px; text-align: right; font-weight: 650; color: var(--primary); }
.thres { display: flex; gap: 24px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--line); }
.thres .fl { display: flex; align-items: center; gap: 8px; font-size: 12.5px; }
.thres label { color: var(--text2); }
.hrow { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px dashed #E9F0F5; }
.hrow:last-child { border-bottom: none; }
</style>
