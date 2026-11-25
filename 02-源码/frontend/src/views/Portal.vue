<template>
  <div>
    <section class="phero">
      <h1>找场地，约时间</h1>
      <p>校园 {{ venues.length }} 个场馆开放预约，个人即时确认，团体走审批</p>
      <div class="psearch">
        <select v-model="q.sport" class="sel" style="width:150px">
          <option value="">全部项目</option>
          <option v-for="s in sports" :key="s" :value="s">{{ s }}</option>
        </select>
        <input v-model="q.keyword" class="inp" style="flex:1" placeholder="搜索场馆名称 / 位置" />
        <button class="btn pri" style="height:36px" @click="load">搜索</button>
      </div>
    </section>

    <div class="pbody">
      <div class="sec">
        <h3>场馆推荐</h3>
        <div class="sp"></div>
        <span class="tip">共 {{ filtered.length }} 个场馆</span>
      </div>
      <div class="venue-grid">
        <div v-for="v in filtered" :key="v.id" class="vcard">
          <img :src="v.coverImage" :alt="v.venueName" />
          <div class="vb">
            <div class="vt">{{ v.venueName }}<span class="tag t-green nodot">{{ v.sportType }}</span></div>
            <div class="vm"><span>{{ v.area }}</span><span>容量 {{ v.capacity }}</span></div>
            <div class="vm"><span class="num">{{ v.openTime }}</span><span>{{ v.floorType }}</span></div>
            <div class="vp num">¥{{ v.priceStudent }}<small> / 小时起</small></div>
            <button class="btn pri" style="width:100%;justify-content:center;margin-top:9px" @click="$router.push('/bookings/new?venueId=' + v.id)">立即预约</button>
          </div>
        </div>
      </div>
      <div v-if="!filtered.length" class="empty">没有匹配的场馆，试试更换项目或清空关键字</div>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../http'

const venues = ref([])
const q = reactive({ sport: '', keyword: '' })
const sports = ['篮球', '羽毛球', '乒乓球', '游泳', '足球', '网球', '排球', '田径']

const filtered = computed(() => venues.value.filter(v => {
  const s = !q.sport || v.sportType === q.sport
  const k = !q.keyword || (v.venueName + v.area).includes(q.keyword)
  return s && k
}))

async function load() {
  const d = await http.get('/venue/page', { params: { page: 1, size: 50, status: 'NORMAL' } })
  venues.value = d.list || []
}

onMounted(load)
</script>
<style scoped>
.sec { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.sec h3 { font-size: 15px; font-weight: 650; }
.empty { text-align: center; color: var(--text3); padding: 50px; font-size: 13px; }
.vcard .vt { font-size: 13.5px; }
</style>
