<template>
  <div>
    <div class="ph">
      <div>
        <h1>新建预约</h1>
        <p>提交后按人数与时长自动判定个人 / 团体通道：团体需场馆审批，个人直接待场地确认</p>
      </div>
      <div class="sp"></div>
      <div class="acts"><button class="btn" @click="$router.back()">返回</button></div>
    </div>

    <div class="grid2-1">
      <div class="card">
        <div class="hd"><h3>预约信息</h3></div>
        <div class="bd">
          <div class="fl"><label>场馆</label><div class="v">
            <el-select v-model="f.venueId" style="width:100%" placeholder="请选择场馆" @change="onVenue">
              <el-option v-for="v in venues" :key="v.id" :label="v.venueName + ' · ' + v.sportType" :value="v.id" />
            </el-select>
          </div></div>
          <div class="fl"><label>预约日期</label><div class="v">
            <el-date-picker v-model="f.date" type="date" value-format="YYYY-MM-DD" style="width:180px" />
            <span class="tip" style="margin-left:10px">开放时间 {{ cur.openTime || '08:00-22:00' }}</span>
          </div></div>
          <div class="fl"><label>起止时间</label><div class="v" style="display:flex;gap:8px;align-items:center">
            <el-time-select v-model="f.start" start="08:00" end="21:00" step="01:00" style="width:130px" />
            <span class="tip">至</span>
            <el-time-select v-model="f.end" start="09:00" end="22:00" step="01:00" style="width:130px" />
          </div></div>
          <div class="fl"><label>预约类型</label><div class="v">
            <el-radio-group v-model="f.bookingType">
              <el-radio value="PERSONAL">个人预约</el-radio>
              <el-radio value="TEAM">团体预约</el-radio>
            </el-radio-group>
          </div></div>
          <div class="fl"><label>申请人 / 团体</label><div class="v"><el-input v-model="f.teamName" placeholder="个人填姓名，团体填队名" /></div></div>
          <div class="fl"><label>联系电话</label><div class="v"><el-input v-model="f.contactPhone" style="width:200px" /></div></div>
          <div class="fl"><label>参与人数</label><div class="v">
            <el-input v-model.number="f.memberCount" style="width:140px" />
            <span class="tip" style="margin-left:10px">场馆容量 {{ cur.capacity || '—' }} 人</span>
          </div></div>
          <div class="fl"><label>使用用途</label><div class="v"><el-input v-model="f.purpose" type="textarea" :rows="2" placeholder="如：院系篮球联赛训练" /></div></div>
          <div class="fl"><label>随借设备</label><div class="v"><el-input v-model="f.equipmentDesc" placeholder="如：球网 2、计分器 1" /></div></div>
          <div class="fl"><label>安全承诺书</label><div class="v">
            <el-upload :action="uploadUrl" :headers="headers" :show-file-list="false" :on-success="r => onUpload(r, 'safetyFile')">
              <button class="btn sm" type="button">{{ f.safetyFile ? '已上传，重新上传' : '上传文件' }}</button>
            </el-upload>
            <span class="tip" style="margin-left:8px">{{ f.safetyFile || '支持 PDF / 图片' }}</span>
          </div></div>
          <div class="fl"><label>成员名单</label><div class="v">
            <el-upload :action="uploadUrl" :headers="headers" :show-file-list="false" :on-success="r => onUpload(r, 'memberFile')">
              <button class="btn sm" type="button">{{ f.memberFile ? '已上传，重新上传' : '上传文件' }}</button>
            </el-upload>
            <span class="tip" style="margin-left:8px">{{ f.memberFile || '团体预约建议上传' }}</span>
          </div></div>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="hd"><h3>费用与规则</h3></div>
          <div class="bd">
            <div class="dl one">
              <div class="r"><span class="k">场地单价</span><span class="v num">{{ fmtMoney(cur.priceHour) }} / 小时</span></div>
              <div class="r"><span class="k">学生价</span><span class="v num">{{ fmtMoney(cur.priceStudent) }} / 小时</span></div>
              <div class="r"><span class="k">预计时长</span><span class="v num">{{ hours }} 小时</span></div>
              <div class="r"><span class="k">预计费用</span><span class="v num" style="color:var(--danger);font-weight:650">{{ fmtMoney(fee) }}</span></div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>协同规则提示</h3></div>
          <div class="bd rules">
            <div>· 同一场地同一时段只允许一条有效占用，冲突时请改时段或加入候补</div>
            <div>· 布场 60 分钟、撤场 30 分钟，与前序场次间隔需 ≥30 分钟</div>
            <div>· 团体预约（人数 &gt;10 或时长 &gt;3 小时）需场馆工作人员审批</div>
            <div>· 提交后系统按 6 项因素加权给出冲突协同评分，评分与人工修正全程留痕</div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>提交</h3></div>
          <div class="bd">
            <button class="btn pri" style="width:100%;justify-content:center" :disabled="saving" @click="submit">
              {{ saving ? '提交中…' : '提交预约申请' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../http'
import { store } from '../store'
import { fmtMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()
const venues = ref([])
const saving = ref(false)
const uploadUrl = '/api/file/upload'
const headers = computed(() => ({ Authorization: 'Bearer ' + store.token }))

const f = reactive({
  venueId: null,
  date: new Date().toISOString().slice(0, 10),
  start: '18:00',
  end: '20:00',
  bookingType: 'PERSONAL',
  teamName: '',
  contactPhone: '',
  memberCount: 6,
  purpose: '',
  equipmentDesc: '',
  safetyFile: '',
  memberFile: ''
})

const cur = computed(() => venues.value.find(v => v.id === f.venueId) || {})
const hours = computed(() => Math.max(1, Number(f.end.slice(0, 2)) - Number(f.start.slice(0, 2))))
const fee = computed(() => (cur.value.priceStudent || 0) * hours.value)

function onVenue() {
  if (f.memberCount > (cur.value.capacity || 0)) f.memberCount = Math.min(10, cur.value.capacity)
}

function onUpload(res, key) {
  const data = res && res.data ? res.data : res
  f[key] = (data && data.path) || ''
}

async function submit() {
  if (!f.venueId) return
  saving.value = true
  try {
    const body = {
      venueId: f.venueId,
      bookingType: f.bookingType,
      teamName: f.teamName || (store.user && store.user.realName),
      contactPhone: f.contactPhone,
      memberCount: f.memberCount,
      purpose: f.purpose,
      equipmentDesc: f.equipmentDesc,
      safetyFile: f.safetyFile,
      memberFile: f.memberFile,
      startTime: f.date + 'T' + f.start + ':00',
      endTime: f.date + 'T' + f.end + ':00'
    }
    const d = await http.post('/booking', body)
    router.replace('/bookings/' + d.id)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  venues.value = await http.get('/venue/options')
  const q = route.query
  if (q.venueId) f.venueId = Number(q.venueId)
  else if (venues.value.length) f.venueId = venues.value[0].id
  if (q.date) f.date = q.date
  if (q.hour) {
    const h = String(q.hour).padStart(2, '0')
    f.start = h + ':00'
    f.end = String(Number(h) + 2).padStart(2, '0') + ':00'
  }
  f.contactPhone = (store.user && store.user.phone) || ''
  onVenue()
})
</script>

<style scoped>
.fl { display: flex; margin-bottom: 14px; font-size: 12.5px; }
.fl label { width: 104px; flex: 0 0 104px; color: var(--text2); padding-top: 7px; }
.fl .v { flex: 1; }
.rules div { font-size: 12px; color: var(--text2); line-height: 2; }
</style>
