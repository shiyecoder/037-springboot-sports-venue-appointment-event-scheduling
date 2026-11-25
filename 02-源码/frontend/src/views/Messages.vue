<template>
  <div>
    <div class="ph">
      <div><h1>消息中心</h1><p>预约审批、候补转正、规则变更与违约处理结果均在此送达</p></div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="readAll">全部标记已读</button>
      </div>
    </div>
    <div class="card">
      <div class="tabs">
        <a :class="{ on: tab === 'all' }" @click="tab = 'all'">全部 <b class="num">{{ total }}</b></a>
        <a :class="{ on: tab === 'unread' }" @click="tab = 'unread'">未读 <b class="num">{{ unread }}</b></a>
      </div>
      <div class="bd">
        <div v-for="m in list" :key="m.id" class="msg" :class="{ un: m.isRead === 0 }" @click="read(m)">
          <div class="mi">
            <span class="tag t-blue nodot">{{ m.msgType }}</span>
            <div>
              <div class="mt">{{ m.title }}</div>
              <div class="mc">{{ m.content }}</div>
              <div class="cellsub num">{{ fmtTime(m.createTime) }} · {{ m.bizNo || '系统通知' }}</div>
            </div>
          </div>
          <span v-if="m.isRead === 0" class="dot2"></span>
        </div>
        <div v-if="!list.length" style="text-align:center;color:var(--text3);padding:34px">暂无消息</div>
      </div>
      <div class="pager">
        <span>共 {{ total }} 条</span><div class="sp"></div>
        <el-pagination layout="prev, pager, next" :total="total" :current-page="page" :page-size="size" @current-change="p => { page = p; load() }" />
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref, watch } from 'vue'
import http from '../http'
import { fmtTime } from '../utils/format'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tab = ref('all')
const unread = ref(0)

async function load() {
  const d = await http.get('/message/page', { params: { page: page.value, size: size.value, isRead: tab.value === 'unread' ? 0 : undefined } })
  list.value = d.list || []
  total.value = d.total || 0
  try {
    const u = await http.get('/message/unread')
    unread.value = typeof u === 'number' ? u : (u && u.count) || 0
  } catch (e) { unread.value = 0 }
}

async function read(m) {
  if (m.isRead === 0) {
    await http.post('/message/' + m.id + '/read')
    load()
  }
}

async function readAll() {
  await http.post('/message/read-all')
  load()
}

watch(tab, () => { page.value = 1; load() })
onMounted(load)
</script>
<style scoped>
.msg { display: flex; align-items: flex-start; justify-content: space-between; padding: 11px 2px; border-bottom: 1px solid #EDF3F8; cursor: pointer; }
.msg:last-child { border-bottom: none; }
.msg:hover { background: #FAFCFE; }
.msg.un .mt { font-weight: 650; }
.mi { display: flex; gap: 12px; align-items: flex-start; }
.mt { font-size: 13px; }
.mc { font-size: 12px; color: var(--text2); margin-top: 3px; }
.dot2 { width: 7px; height: 7px; border-radius: 50%; background: var(--danger); margin-top: 8px; }
</style>
