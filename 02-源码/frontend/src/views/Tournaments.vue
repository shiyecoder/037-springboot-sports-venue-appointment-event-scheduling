<template>
  <div>
    <div class="ph">
      <div><h1>赛事台账</h1><p>赛事从草稿到结束的全过程台账，排期需校验场地、裁判与队伍休息间隔</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="load">刷新</button></div>
    </div>
    <div class="card">
      <div class="filters">
        <div class="fld"><input v-model="q.keyword" class="inp w220" placeholder="赛事名称 / 编号" @keyup.enter="search" /></div>
        <div class="fld">
          <select v-model="q.status" class="sel" @change="search">
            <option value="">全部状态</option>
            <option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option>
            <option value="ONGOING">进行中</option><option value="FINISHED">已结束</option>
          </select>
        </div>
        <div class="fld">
          <select v-model="q.sportType" class="sel" style="width:160px" @change="search">
            <option value="">全部项目</option>
            <option v-for="s in sports" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="sp"></div>
        <button class="btn pri" @click="search">查询</button>
      </div>
      <table>
        <thead><tr><th>赛事</th><th>项目</th><th>级别</th><th>场馆</th><th class="nowrap">起止时间</th><th class="nowrap">队伍 / 场次</th><th>状态</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>
              <div style="display:flex;gap:9px;align-items:center">
                <img class="thumb" :src="r.poster" :alt="r.tournamentName" />
                <div><div class="cellmain">{{ r.tournamentName }}</div><div class="cellsub">{{ r.tournamentNo }}</div></div>
              </div>
            </td>
            <td>{{ r.sportType }}</td>
            <td>{{ r.level }}</td>
            <td>{{ venueName(r.venueId) }}</td>
            <td class="num nowrap">{{ fmtShort(r.startTime) }}<div class="cellsub">{{ fmtShort(r.endTime) }}</div></td>
            <td class="num">{{ r.teamCount }} / {{ r.matchCount }}</td>
            <td><Tag :code="r.status" /></td>
            <td class="nowrap"><button class="btn sm lnk" @click="$router.push('/tournaments/' + r.id)">详情</button></td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" style="text-align:center;color:var(--text3);padding:30px">暂无赛事记录</td></tr>
        </tbody>
      </table>
      <div class="pager">
        <span>共 {{ total }} 项赛事</span><div class="sp"></div>
        <el-pagination layout="sizes, prev, pager, next" :total="total" :current-page="page" :page-size="size"
          :page-sizes="[10, 20, 50]" @current-change="p => { page = p; load() }" @size-change="s => { size = s; page = 1; load() }" />
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtShort } from '../utils/format'

const sports = ['篮球', '羽毛球', '乒乓球', '游泳', '足球', '网球', '排球', '田径']
const list = ref([])
const venues = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = reactive({ keyword: '', status: '', sportType: '' })
const venueName = id => (venues.value.find(v => v.id === id) || {}).venueName || '场馆 #' + id

async function load() {
  const params = { page: page.value, size: size.value }
  if (q.status) params.status = q.status
  if (q.sportType) params.sportType = q.sportType
  const d = await http.get('/tournament/page', { params })
  let rows = d.list || []
  if (q.keyword) rows = rows.filter(r => (r.tournamentName + r.tournamentNo).includes(q.keyword))
  list.value = rows
  total.value = d.total || 0
}
function search() { page.value = 1; load() }

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  load()
})
</script>
