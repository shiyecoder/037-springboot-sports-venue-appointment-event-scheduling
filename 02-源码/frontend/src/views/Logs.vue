<template>
  <div>
    <div class="ph">
      <div><h1>操作日志</h1><p>关键动作留痕：谁在什么时间对哪张单据做了什么，结果如何</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="load">刷新</button></div>
    </div>
    <div class="card">
      <div class="filters">
        <input v-model="q.keyword" class="inp w220" placeholder="操作人 / 模块 / 详情" @keyup.enter="search" />
        <select v-model="q.result" class="sel" style="width:160px" @change="search">
          <option value="">全部结果</option><option value="成功">成功</option><option value="失败">失败</option>
        </select>
        <div class="sp"></div>
        <button class="btn pri" @click="search">查询</button>
      </div>
      <table>
        <thead><tr><th class="nowrap">时间</th><th>操作人</th><th>模块</th><th>动作</th><th>详情</th><th class="nowrap">IP</th><th>结果</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="num nowrap">{{ fmtTime(r.createTime) }}</td>
            <td>{{ r.userName }}</td>
            <td>{{ r.module }}</td>
            <td class="cellmain">{{ r.action }}</td>
            <td>{{ r.detail }}</td>
            <td class="num">{{ r.ip }}</td>
            <td><span class="tag" :class="r.result === '成功' ? 't-green' : 't-red'">{{ r.result }}</span></td>
          </tr>
          <tr v-if="!list.length"><td colspan="7" style="text-align:center;color:var(--text3);padding:30px">无日志记录</td></tr>
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
import { onMounted, reactive, ref } from 'vue'
import http from '../http'
import { fmtTime } from '../utils/format'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = reactive({ keyword: '', result: '' })

async function load() {
  const params = { page: page.value, size: size.value }
  Object.entries(q).forEach(([k, v]) => { if (v) params[k] = v })
  const d = await http.get('/system/log/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}
function search() { page.value = 1; load() }
onMounted(load)
</script>
