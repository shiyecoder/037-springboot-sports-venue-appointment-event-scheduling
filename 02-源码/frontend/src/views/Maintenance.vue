<template>
  <div>
    <div class="ph">
      <div><h1>场地维护</h1><p>维护计划锁定对应时段，受影响预约自动进入协调清单并通知申请人</p></div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="load">刷新</button>
        <button class="btn pri" @click="dlg = true">新增维护</button>
      </div>
    </div>
    <div class="card">
      <div class="filters">
        <div class="fld">
          <select v-model="status" class="sel" @change="load">
            <option value="">全部状态</option>
            <option value="PENDING">待处理</option>
            <option value="APPROVED">已批准</option>
            <option value="FINISHED">已完成</option>
          </select>
        </div>
        <div class="sp"></div>
        <span class="tip">共 {{ total }} 条维护计划</span>
      </div>
      <table>
        <thead><tr><th>维护单号</th><th>场馆</th><th>类型</th><th>影响范围</th><th class="nowrap">开始</th><th class="nowrap">结束</th><th>责任单位</th><th>状态</th><th>影响说明</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="no">{{ r.maintenanceNo }}</td>
            <td class="cellmain">{{ venueName(r.venueId) }}</td>
            <td>{{ r.maintenanceType }}</td>
            <td>{{ r.scope }}</td>
            <td class="num nowrap">{{ fmtShort(r.startTime) }}</td>
            <td class="num nowrap">{{ fmtShort(r.endTime) }}</td>
            <td>{{ r.owner }}</td>
            <td><Tag :code="r.status" /></td>
            <td>{{ r.affectDesc }}</td>
          </tr>
          <tr v-if="!list.length"><td colspan="9" style="text-align:center;color:var(--text3);padding:30px">暂无维护计划</td></tr>
        </tbody>
      </table>
      <div class="pager">
        <span>共 {{ total }} 条</span><div class="sp"></div>
        <el-pagination layout="prev, pager, next" :total="total" :current-page="page" :page-size="size" @current-change="p => { page = p; load() }" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新增维护计划" width="560px">
      <div class="fl"><label>场馆</label><div class="v">
        <el-select v-model="f.venueId" style="width:100%">
          <el-option v-for="v in venues" :key="v.id" :label="v.venueName" :value="v.id" />
        </el-select>
      </div></div>
      <div class="fl"><label>维护类型</label><div class="v">
        <el-select v-model="f.maintenanceType" style="width:100%">
          <el-option label="保养" value="保养" /><el-option label="维修" value="维修" /><el-option label="改造" value="改造" />
        </el-select>
      </div></div>
      <div class="fl"><label>开始时间</label><div class="v"><el-date-picker v-model="f.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></div></div>
      <div class="fl"><label>结束时间</label><div class="v"><el-date-picker v-model="f.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></div></div>
      <div class="fl"><label>影响范围</label><div class="v"><el-input v-model="f.scope" placeholder="整段禁用 / 半场禁用" /></div></div>
      <div class="fl"><label>责任单位</label><div class="v"><el-input v-model="f.owner" /></div></div>
      <div class="fl"><label>影响说明</label><div class="v"><el-input v-model="f.affectDesc" type="textarea" :rows="2" /></div></div>
      <template #footer>
        <button class="btn" @click="dlg = false">取消</button>
        <button class="btn pri" @click="save">提交</button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtShort } from '../utils/format'

const list = ref([])
const venues = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const dlg = ref(false)
const f = reactive({ maintenanceType: '保养', scope: '整段禁用', owner: '场馆运营科', status: 'PENDING' })

const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '场馆 #' + id

async function load() {
  const params = { page: page.value, size: size.value }
  if (status.value) params.status = status.value
  const d = await http.get('/maintenance/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}

async function save() {
  await http.post('/maintenance', f)
  dlg.value = false
  load()
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>
<style scoped>
.fl { display: flex; margin-bottom: 12px; font-size: 12.5px; }
.fl label { width: 84px; flex: 0 0 84px; color: var(--text2); padding-top: 7px; }
.fl .v { flex: 1; }
</style>
