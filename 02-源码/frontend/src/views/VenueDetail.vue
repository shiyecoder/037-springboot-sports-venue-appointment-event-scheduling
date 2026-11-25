<template>
  <div v-if="v.id">
    <div class="ph">
      <div>
        <h1>{{ v.venueName }}</h1>
        <p>{{ v.venueNo }} · {{ v.sportType }} · {{ v.area }} · 容量 {{ v.capacity }} 人</p>
      </div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="$router.back()">返回</button>
        <button class="btn" @click="$router.push('/calendar?venueId=' + v.id)">查看周历</button>
        <button class="btn pri" @click="$router.push('/bookings/new?venueId=' + v.id)">预约该场馆</button>
      </div>
    </div>

    <div class="grid2-1">
      <div>
        <div class="card">
          <div class="hd"><h3>场馆影像</h3><div class="sp"></div><Tag :code="v.status" /></div>
          <div class="bd" style="display:flex;gap:10px">
            <img v-for="(img, i) in images" :key="i" :src="img" style="flex:1;height:168px;object-fit:cover;border-radius:6px" alt="场馆图" />
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>场馆档案</h3></div>
          <div class="bd">
            <div class="dl">
              <div class="r"><span class="k">运动项目</span><span class="v">{{ v.sportType }}</span></div>
              <div class="r"><span class="k">地面材质</span><span class="v">{{ v.floorType }}</span></div>
              <div class="r"><span class="k">开放时间</span><span class="v num">{{ v.openTime }}</span></div>
              <div class="r"><span class="k">管理单位</span><span class="v">{{ v.manager }}</span></div>
              <div class="r"><span class="k">场地单价</span><span class="v num">{{ fmtMoney(v.priceHour) }} / 小时</span></div>
              <div class="r"><span class="k">学生价</span><span class="v num">{{ fmtMoney(v.priceStudent) }} / 小时</span></div>
              <div class="r"><span class="k">配套设施</span><span class="v">{{ v.equipment }}</span></div>
              <div class="r"><span class="k">备注</span><span class="v">{{ v.remark || '—' }}</span></div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>近期预约</h3><div class="sp"></div><span class="tip">最多 5 条</span></div>
          <table>
            <thead><tr><th>单号</th><th>时段</th><th>申请人</th><th>人数</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="b in bookings" :key="b.id">
                <td class="no">{{ b.bookingNo }}</td>
                <td class="nowrap">{{ fmtShort(b.startTime) }} — {{ timeOf(b.endTime) }}</td>
                <td>{{ b.teamName }}</td>
                <td class="num">{{ b.memberCount }}</td>
                <td><Tag :code="b.status" /></td>
              </tr>
              <tr v-if="!bookings.length"><td colspan="5" style="text-align:center;color:var(--text3);padding:22px">该场馆近期无预约</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="hd"><h3>时段与占用</h3><div class="sp"></div><span class="tip">近 2 周</span></div>
          <div class="bd" style="max-height:300px;overflow:auto">
            <div v-for="s in slots.slice(0, 26)" :key="s.id" class="slot-row">
              <span class="num">{{ fmtDate(s.slotDate) }} {{ s.startTime }}</span>
              <Tag :code="s.status" />
              <span class="num tip">{{ s.bookedCount }}/{{ s.capacity }}</span>
            </div>
            <div v-if="!slots.length" class="tip">暂无时段</div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>维护记录</h3></div>
          <div class="bd">
            <div v-for="m in maintenances" :key="m.id" class="mt-row">
              <div class="cellmain">{{ m.maintenanceType }} · {{ m.scope }}</div>
              <div class="cellsub num">{{ fmtShort(m.startTime) }} — {{ fmtShort(m.endTime) }}</div>
              <div class="cellsub">{{ m.affectDesc }}</div>
            </div>
            <div v-if="!maintenances.length" class="tip">无维护记录</div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>归档材料</h3></div>
          <div class="bd">
            <div v-for="a in attachments" :key="a.id" class="att">
              <a :href="a.path" target="_blank">{{ a.fileName }}</a>
              <span class="tip">{{ a.uploader }}</span>
            </div>
            <div v-if="!attachments.length" class="tip">暂无附件</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtDate, fmtMoney, fmtShort } from '../utils/format'

const route = useRoute()
const v = ref({})
const slots = ref([])
const bookings = ref([])
const maintenances = ref([])
const attachments = ref([])
const images = computed(() => [v.value.coverImage, v.value.image2, v.value.image3].filter(Boolean))
const timeOf = x => (x ? String(x).replace('T', ' ').slice(11, 16) : '—')

onMounted(async () => {
  const d = await http.get('/venue/' + route.params.id)
  v.value = d.venue || {}
  slots.value = d.slots || []
  bookings.value = d.bookings || []
  maintenances.value = d.maintenances || []
  attachments.value = d.attachments || []
})
</script>

<style scoped>
.slot-row { display: flex; align-items: center; gap: 8px; padding: 5px 0; border-bottom: 1px dashed #E9F0F5; font-size: 12px; }
.slot-row:last-child { border-bottom: none; }
.mt-row { padding: 7px 0; border-bottom: 1px dashed #E9F0F5; }
.mt-row:last-child { border-bottom: none; }
.att { display: flex; justify-content: space-between; padding: 6px 0; font-size: 12.5px; border-bottom: 1px dashed #E9F0F5; }
.att:last-child { border-bottom: none; }
</style>
