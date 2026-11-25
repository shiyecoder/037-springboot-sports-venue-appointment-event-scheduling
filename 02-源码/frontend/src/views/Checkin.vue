<template>
  <div>
    <div class="ph">
      <div><h1>入场核验</h1><p>凭核销码或预约单号核验入场，实到人数与设备核对后放行并锁定场地</p></div>
    </div>
    <div class="grid2-1">
      <div class="card">
        <div class="hd"><h3>核销查询</h3></div>
        <div class="bd">
          <div class="scan">
            <input v-model.trim="code" class="inp" style="width:100%;height:40px;font-size:13px" placeholder="扫描或输入核销码 / 预约单号，回车查询" @keyup.enter="verify" />
            <button class="btn pri" style="height:40px" @click="verify">核验</button>
          </div>
          <div v-if="err" class="errbox">{{ err }}</div>
          <div v-if="b.id" style="margin-top:16px">
            <div class="hd2">
              <div><b>{{ b.bookingNo }}</b><span class="tip" style="margin-left:10px">{{ b.teamName }} · {{ b.contactPhone }}</span></div>
              <Tag :code="b.status" />
            </div>
            <div class="dl">
              <div class="r"><span class="k">场馆</span><span class="v">{{ v.venueName }}</span></div>
              <div class="r"><span class="k">时段</span><span class="v num">{{ fmtTime(b.startTime) }} — {{ timeOf(b.endTime) }}</span></div>
              <div class="r"><span class="k">预约人数</span><span class="v num">{{ b.memberCount }} 人</span></div>
              <div class="r"><span class="k">类型</span><span class="v">{{ BOOKING_TYPE[b.bookingType] }}</span></div>
              <div class="r"><span class="k">设备</span><span class="v">{{ b.equipmentDesc || '无' }}</span></div>
              <div class="r"><span class="k">费用</span><span class="v num">{{ fmtMoney(b.feeAmount) }} · {{ FEE_STATUS[b.feeStatus] }}</span></div>
            </div>
            <div class="fl" style="margin-top:12px">
              <label>实到人数</label>
              <div class="v"><el-input v-model.number="actual" style="width:140px" /></div>
            </div>
            <div style="display:flex;gap:9px;margin-top:14px">
              <button class="btn pri" @click="act('checkin')">核验入场</button>
              <button class="btn" @click="act('leave')">申请离场检查</button>
              <button class="btn dgr" @click="act('no-show')">登记未到</button>
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="hd"><h3>待入场清单</h3><div class="sp"></div><span class="tip">今日</span></div>
        <table>
          <thead><tr><th>单号</th><th>时段</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="x in today" :key="x.id" style="cursor:pointer" @click="pick(x)">
              <td class="no">{{ x.bookingNo }}</td>
              <td class="nowrap">{{ timeOf(x.startTime) }}</td>
              <td><Tag :code="x.status" /></td>
            </tr>
            <tr v-if="!today.length"><td colspan="3" style="text-align:center;color:var(--text3);padding:24px">今日暂无待入场预约</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { BOOKING_TYPE, FEE_STATUS, fmtMoney, fmtTime } from '../utils/format'

const code = ref('')
const b = ref({})
const v = ref({})
const err = ref('')
const actual = ref(0)
const today = ref([])
const timeOf = x => (x ? String(x).replace('T', ' ').slice(11, 16) : '—')

async function verify() {
  err.value = ''
  b.value = {}
  try {
    const d = await http.get('/booking/verify', { params: { code: code.value } })
    b.value = d.booking || {}
    v.value = d.venue || {}
    actual.value = b.value.memberCount
  } catch (e) {
    err.value = '未找到该核销码对应的预约，请确认后重试'
  }
}

async function act(action) {
  await http.post('/booking/' + b.value.id + '/transition', { action, actualCount: actual.value })
  ElMessage.success('操作成功')
  verify()
  loadToday()
}

function pick(x) { code.value = x.bookingCode; verify() }

async function loadToday() {
  const d = await http.get('/booking/page', { params: { page: 1, size: 20, status: 'PENDING_ENTRY' } })
  today.value = d.list || []
}

onMounted(loadToday)
</script>
<style scoped>
.scan { display: flex; gap: 9px; }
.errbox { margin-top: 10px; color: var(--danger); font-size: 12.5px; background: #FBEEEF; border: 1px solid #EBCBCD; border-radius: 6px; padding: 8px 10px; }
.hd2 { display: flex; align-items: center; justify-content: space-between; padding-bottom: 10px; border-bottom: 1px solid var(--line); margin-bottom: 6px; }
.fl { display: flex; align-items: center; gap: 10px; font-size: 12.5px; }
.fl label { color: var(--text2); }
</style>
