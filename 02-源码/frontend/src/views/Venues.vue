<template>
  <div>
    <div class="ph">
      <div>
        <h1>场馆档案</h1>
        <p>一馆一档：场地规格、开放时间、分时价格与维护状态，是预约与排期的资源底账</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="load">刷新</button>
        <button class="btn pri" @click="openEdit()">新增场馆</button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <div class="fld"><input v-model="q.keyword" class="inp w220" placeholder="场馆名称 / 编号" @keyup.enter="search" /></div>
        <div class="fld">
          <select v-model="q.sportType" class="sel">
            <option value="">全部项目</option>
            <option v-for="s in sports" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="fld">
          <select v-model="q.status" class="sel" style="width:160px">
            <option value="">全部状态</option>
            <option value="NORMAL">正常开放</option>
            <option value="MAINTAINING">维护中</option>
            <option value="DISABLED">停用</option>
          </select>
        </div>
        <div class="sp"></div>
        <button class="btn" @click="reset">重置</button>
        <button class="btn pri" @click="search">查询</button>
      </div>

      <table>
        <thead>
          <tr>
            <th>场馆</th>
            <th>项目</th>
            <th>位置</th>
            <th class="nowrap">容量</th>
            <th>地面</th>
            <th class="nowrap">开放时间</th>
            <th class="nowrap">价格（校内 / 学生）</th>
            <th>状态</th>
            <th class="nowrap">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>
              <div style="display:flex;gap:9px;align-items:center">
                <img class="thumb" :src="r.coverImage" :alt="r.venueName" />
                <div>
                  <div class="cellmain">{{ r.venueName }}</div>
                  <div class="cellsub">{{ r.venueNo }}</div>
                </div>
              </div>
            </td>
            <td>{{ r.sportType }}</td>
            <td>{{ r.area }}</td>
            <td class="num">{{ r.capacity }}</td>
            <td>{{ r.floorType }}</td>
            <td class="num nowrap">{{ r.openTime }}</td>
            <td class="num nowrap">{{ fmtMoney(r.priceHour) }} / {{ fmtMoney(r.priceStudent) }}</td>
            <td><Tag :code="r.status" /></td>
            <td class="nowrap">
              <div class="ops">
                <button class="btn sm lnk" @click="$router.push('/venues/' + r.id)">详情</button>
                <button class="btn sm" @click="openEdit(r)">编辑</button>
                <button class="btn sm" @click="toggle(r)">{{ r.status === 'NORMAL' ? '停用' : '启用' }}</button>
              </div>
            </td>
          </tr>
          <tr v-if="!list.length">
            <td colspan="9" style="text-align:center;color:var(--text3);padding:34px">暂无场馆记录，点击右上角新增场馆</td>
          </tr>
        </tbody>
      </table>

      <div class="pager">
        <span>共 {{ total }} 个场馆</span>
        <div class="sp"></div>
        <el-pagination layout="sizes, prev, pager, next" :total="total" :current-page="page" :page-size="size"
          :page-sizes="[10, 20, 50]" @current-change="p => { page = p; load() }" @size-change="s => { size = s; page = 1; load() }" />
      </div>
    </div>

    <!-- 新增 / 编辑馆档 -->
    <el-dialog v-model="dlg" :title="form.id ? '编辑场馆' : '新增场馆'" width="720px">
      <div class="form2">
        <div class="fl"><label>场馆名称</label><div class="v"><el-input v-model="form.venueName" /></div></div>
        <div class="fl"><label>场馆编号</label><div class="v"><el-input v-model="form.venueNo" placeholder="V-DX-001" /></div></div>
        <div class="fl"><label>运动项目</label><div class="v">
          <el-select v-model="form.sportType" style="width:100%"><el-option v-for="s in sports" :key="s" :label="s" :value="s" /></el-select>
        </div></div>
        <div class="fl"><label>位置</label><div class="v"><el-input v-model="form.area" /></div></div>
        <div class="fl"><label>容量（人）</label><div class="v"><el-input v-model.number="form.capacity" /></div></div>
        <div class="fl"><label>地面材质</label><div class="v"><el-input v-model="form.floorType" /></div></div>
        <div class="fl"><label>开放时间</label><div class="v"><el-input v-model="form.openTime" placeholder="08:00-22:00" /></div></div>
        <div class="fl"><label>场地单价</label><div class="v"><el-input v-model.number="form.priceHour" /></div></div>
        <div class="fl"><label>学生价</label><div class="v"><el-input v-model.number="form.priceStudent" /></div></div>
        <div class="fl"><label>管理单位</label><div class="v"><el-input v-model="form.manager" /></div></div>
      </div>
      <div class="fl" style="padding:0 4px"><label>配套设施</label><div class="v"><el-input v-model="form.equipment" type="textarea" :rows="2" /></div></div>
      <div class="fl" style="padding:0 4px"><label>备注</label><div class="v"><el-input v-model="form.remark" type="textarea" :rows="2" /></div></div>
      <template #footer>
        <button class="btn" @click="dlg = false">取消</button>
        <button class="btn pri" @click="save">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtMoney } from '../utils/format'

const sports = ['篮球', '羽毛球', '乒乓球', '游泳', '足球', '网球', '排球', '田径']
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = reactive({ keyword: '', sportType: '', status: '' })
const dlg = ref(false)
const form = reactive({})

async function load() {
  const params = { page: page.value, size: size.value }
  Object.entries(q).forEach(([k, v]) => { if (v) params[k] = v })
  const d = await http.get('/venue/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}
function search() { page.value = 1; load() }
function reset() { q.keyword = ''; q.sportType = ''; q.status = ''; page.value = 1; load() }

function openEdit(r) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r || { status: 'NORMAL', sportType: '篮球', capacity: 100, priceHour: 100, priceStudent: 50 })
  dlg.value = true
}

async function save() {
  if (!form.venueName) return
  if (form.id) await http.put('/venue/' + form.id, form)
  else await http.post('/venue', form)
  dlg.value = false
  load()
}

async function toggle(r) {
  await http.post('/venue/' + r.id + '/transition', { action: r.status === 'NORMAL' ? 'disable' : 'enable' })
  load()
}

onMounted(load)
</script>

<style scoped>
.form2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.form2 .fl { display: flex; margin-bottom: 12px; font-size: 12.5px; }
.form2 .fl label { width: 88px; flex: 0 0 88px; color: var(--text2); padding-top: 7px; }
.form2 .fl .v { flex: 1; }
</style>
