<template>
  <div>
    <div class="ph">
      <div><h1>设备借用</h1><p>场馆器材库存与借还台账：借出扣减库存，归还登记完好或损坏赔付</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="load">刷新</button></div>
    </div>
    <div class="card">
      <div class="tabs">
        <a :class="{ on: tab === 'eq' }" @click="tab = 'eq'">器材台账 <b class="num">{{ equipments.length }}</b></a>
        <a :class="{ on: tab === 'bw' }" @click="tab = 'bw'">借用记录 <b class="num">{{ bwTotal }}</b></a>
      </div>

      <template v-if="tab === 'eq'">
        <table>
          <thead><tr><th>器材编号</th><th>名称</th><th>类别</th><th>所属场馆</th><th class="nowrap">库存 / 可用</th><th>状态</th><th class="nowrap">库存占用</th></tr></thead>
          <tbody>
            <tr v-for="r in equipments" :key="r.id">
              <td class="no">{{ r.equipmentNo }}</td>
              <td class="cellmain">{{ r.equipmentName }}</td>
              <td>{{ r.equipmentType }}</td>
              <td>{{ venueName(r.venueId) }}</td>
              <td class="num">{{ r.totalCount }} / {{ r.availableCount }}</td>
              <td><Tag :code="r.status" /></td>
              <td style="width:150px">
                <div class="prog"><i :style="{ width: pct(r) + '%' }"></i></div>
                <span class="tip num">{{ pct(r) }}% 借出</span>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <template v-else>
        <table>
          <thead><tr><th>借用单号</th><th>器材</th><th class="nowrap">数量</th><th class="nowrap">借出时间</th><th class="nowrap">应还时间</th><th>状态</th><th>损坏说明</th><th class="nowrap">操作</th></tr></thead>
          <tbody>
            <tr v-for="r in borrows" :key="r.id">
              <td class="no">{{ r.borrowNo }}</td>
              <td>{{ eqName(r.equipmentId) }}</td>
              <td class="num">{{ r.borrowCount }}</td>
              <td class="nowrap num">{{ fmtShort(r.borrowTime) }}</td>
              <td class="nowrap num">{{ fmtShort(r.expectReturn) }}</td>
              <td><Tag :code="r.status" /></td>
              <td>{{ r.damageDesc || '—' }}<span v-if="r.damageFee" class="tip">赔付 {{ fmtMoney(r.damageFee) }}</span></td>
              <td class="nowrap"><button v-if="r.status === 'BORROWED'" class="btn sm" @click="giveBack(r)">登记归还</button></td>
            </tr>
            <tr v-if="!borrows.length"><td colspan="8" style="text-align:center;color:var(--text3);padding:30px">暂无借用记录</td></tr>
          </tbody>
        </table>
        <div class="pager">
          <span>共 {{ bwTotal }} 条</span><div class="sp"></div>
          <el-pagination layout="prev, pager, next" :total="bwTotal" :current-page="bwPage" :page-size="10" @current-change="p => { bwPage = p; loadBorrow() }" />
        </div>
      </template>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtMoney, fmtShort } from '../utils/format'

const tab = ref('eq')
const equipments = ref([])
const borrows = ref([])
const venues = ref([])
const bwTotal = ref(0)
const bwPage = ref(1)

const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '场馆 #' + id
const eqName = id => (equipments.value.find(e => e.id === id) || {}).equipmentName || '器材 #' + id
const pct = r => Math.round((1 - (r.availableCount || 0) / (r.totalCount || 1)) * 100)

async function load() {
  const d = await http.get('/equipment/page')
  equipments.value = Array.isArray(d) ? d : d.list || []
  loadBorrow()
}

async function loadBorrow() {
  const d = await http.get('/equipment/borrow/page', { params: { page: bwPage.value, size: 10 } })
  borrows.value = Array.isArray(d) ? d : d.list || []
  bwTotal.value = Array.isArray(d) ? d.length : d.total || 0
}

async function giveBack(r) {
  const res = await ElMessageBox.prompt('归还登记：器材完好请留空，如有损坏请说明（将生成赔付金额）', '登记归还', { inputPlaceholder: '如：球网被拉断，需更换' })
  await http.post('/equipment/borrow/' + r.id + '/return', null, { params: { damageDesc: res.value || '' } })
  load()
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>
