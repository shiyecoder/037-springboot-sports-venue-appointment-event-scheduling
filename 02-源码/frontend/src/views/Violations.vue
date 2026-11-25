<template>
  <div>
    <div class="ph">
      <div><h1>违约与申诉</h1><p>无故未到、超时离场等违约扣减信用分，当事人可申诉，通过后撤销处罚</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="load">刷新</button></div>
    </div>
    <div class="card">
      <div class="tabs">
        <a :class="{ on: tab === 'v' }" @click="tab = 'v'">违约记录 <b class="num">{{ vTotal }}</b></a>
        <a :class="{ on: tab === 'a' }" @click="tab = 'a'">申诉处理 <b class="num">{{ appeals.length }}</b></a>
      </div>

      <template v-if="tab === 'v'">
        <div class="filters">
          <select v-model="status" class="sel" @change="loadV">
            <option value="">全部状态</option>
            <option value="PENDING">待处理</option>
            <option value="APPEALING">申诉中</option>
            <option value="CONFIRMED">已生效</option>
            <option value="REVOKED">已撤销</option>
          </select>
          <div class="sp"></div>
          <span class="tip">共 {{ vTotal }} 条违约记录</span>
        </div>
        <table>
          <thead><tr><th>违约单号</th><th>当事人</th><th>类型</th><th>关联单号</th><th class="nowrap">发生日期</th><th>处罚</th><th class="nowrap">信用变化</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="r in list" :key="r.id">
              <td class="no">{{ r.violationNo }}</td>
              <td>{{ r.userName }}</td>
              <td>{{ r.violationType }}</td>
              <td class="no">{{ r.bizNo }}</td>
              <td class="num nowrap">{{ r.occurDate }}</td>
              <td>{{ r.punish }}</td>
              <td class="num" :style="{ color: r.creditDelta < 0 ? 'var(--danger)' : 'var(--success)' }">{{ r.creditDelta }}</td>
              <td><Tag :code="r.status" /></td>
            </tr>
            <tr v-if="!list.length"><td colspan="8" style="text-align:center;color:var(--text3);padding:30px">暂无违约记录</td></tr>
          </tbody>
        </table>
        <div class="pager">
          <span>共 {{ vTotal }} 条</span><div class="sp"></div>
          <el-pagination layout="prev, pager, next" :total="vTotal" :current-page="page" :page-size="size" @current-change="p => { page = p; loadV() }" />
        </div>
      </template>

      <template v-else>
        <table>
          <thead><tr><th>申诉单号</th><th>关联违约</th><th>申诉理由</th><th>证明材料</th><th>状态</th><th>处理意见</th><th class="nowrap">操作</th></tr></thead>
          <tbody>
            <tr v-for="r in appeals" :key="r.id">
              <td class="no">{{ r.appealNo }}</td>
              <td class="no">#{{ r.violationId }}</td>
              <td style="max-width:260px">{{ r.reason }}</td>
              <td><a v-if="r.files" :href="r.files" target="_blank">查看</a><span v-else class="tip">—</span></td>
              <td><Tag :code="r.status" /></td>
              <td>{{ r.handleRemark || '—' }}<div class="cellsub">{{ r.handleUser }} {{ r.handleTime ? fmtShort(r.handleTime) : '' }}</div></td>
              <td class="nowrap">
                <template v-if="r.status === 'PENDING'">
                  <button class="btn sm" @click="handle(r, true)">通过</button>
                  <button class="btn sm dgr" @click="handle(r, false)">驳回</button>
                </template>
                <span v-else class="tip">已处理</span>
              </td>
            </tr>
            <tr v-if="!appeals.length"><td colspan="7" style="text-align:center;color:var(--text3);padding:30px">暂无申诉</td></tr>
          </tbody>
        </table>
      </template>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtShort } from '../utils/format'

const tab = ref('v')
const list = ref([])
const appeals = ref([])
const vTotal = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')

async function loadV() {
  const params = { page: page.value, size: size.value }
  if (status.value) params.status = status.value
  const d = await http.get('/violation/page', { params })
  list.value = d.list || []
  vTotal.value = d.total || 0
}

async function loadA() {
  const d = await http.get('/violation/appeal/page')
  appeals.value = Array.isArray(d) ? d : d.list || []
}

async function handle(r, pass) {
  const res = await ElMessageBox.prompt('请填写处理意见（不少于 5 个字）', pass ? '通过申诉' : '驳回申诉', { inputPlaceholder: '说明核查结论' })
  await http.post('/violation/appeal/' + r.id + '/handle', { pass: pass ? 'true' : 'false', remark: res.value })
  loadA()
  loadV()
}

function load() { loadV(); loadA() }
onMounted(load)
</script>
