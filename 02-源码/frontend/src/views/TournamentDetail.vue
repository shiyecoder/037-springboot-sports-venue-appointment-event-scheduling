<template>
  <div v-if="t.id">
    <div class="ph">
      <div><h1>{{ t.tournamentName }}</h1><p>{{ t.tournamentNo }} · {{ t.sportType }} · {{ t.level }} · {{ v.venueName }}</p></div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="$router.back()">返回</button>
        <button v-if="t.status === 'DRAFT'" class="btn pri" @click="publish">发布赛事</button>
        <button v-if="t.status === 'PUBLISHED'" class="btn pri" @click="setStatus('ONGOING')">开始进行</button>
        <button v-if="t.status === 'ONGOING'" class="btn pri" @click="setStatus('FINISHED')">结束归档</button>
      </div>
    </div>

    <div class="grid2-1">
      <div>
        <div class="card">
          <div class="hd"><h3>赛事信息</h3><div class="sp"></div><Tag :code="t.status" /></div>
          <div class="bd" style="display:flex;gap:14px">
            <img :src="t.poster" style="width:220px;height:140px;object-fit:cover;border-radius:6px" alt="赛事海报" />
            <div class="dl" style="flex:1;grid-template-columns:1fr 1fr">
              <div class="r"><span class="k">起止</span><span class="v num">{{ fmtShort(t.startTime) }} — {{ fmtShort(t.endTime) }}</span></div>
              <div class="r"><span class="k">队伍数</span><span class="v num">{{ t.teamCount }}</span></div>
              <div class="r"><span class="k">场次数</span><span class="v num">{{ t.matchCount }}</span></div>
              <div class="r"><span class="k">优先级</span><span class="v">P{{ t.priority }}</span></div>
              <div class="r"><span class="k">安保材料</span><span class="v"><a v-if="t.safetyFile" :href="t.safetyFile" target="_blank">查看</a><span v-else>—</span></span></div>
              <div class="r"><span class="k">备注</span><span class="v">{{ t.remark || '—' }}</span></div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="hd"><h3>赛程编排</h3><div class="sp"></div><span class="tip">同队两场间隔 ≥45 分钟</span></div>
          <table>
            <thead><tr><th>场次</th><th>轮次</th><th>对阵</th><th class="nowrap">时间</th><th>裁判</th><th class="nowrap">比分</th><th class="nowrap">操作</th></tr></thead>
            <tbody>
              <tr v-for="m in matches" :key="m.id">
                <td class="no">{{ m.matchNo }}</td>
                <td>{{ m.roundName }}</td>
                <td class="cellmain">{{ m.homeTeam }} vs {{ m.awayTeam }}</td>
                <td class="num nowrap">{{ fmtShort(m.startTime) }}</td>
                <td>{{ refName(m.refereeId) }}</td>
                <td class="num">{{ m.scoreHome ?? '-' }} : {{ m.scoreAway ?? '-' }}</td>
                <td class="nowrap"><button class="btn sm" @click="score(m)">录入比分</button></td>
              </tr>
              <tr v-if="!matches.length"><td colspan="7" style="text-align:center;color:var(--text3);padding:24px">尚未编排赛程</td></tr>
            </tbody>
          </table>
        </div>

        <div class="card">
          <div class="hd"><h3>参赛队伍</h3></div>
          <table>
            <thead><tr><th>队伍</th><th>领队</th><th class="nowrap">人数</th><th class="nowrap">已赛</th><th class="nowrap">积分</th></tr></thead>
            <tbody>
              <tr v-for="x in teams" :key="x.id">
                <td class="cellmain">{{ x.teamName }}</td><td>{{ x.leader }}</td>
                <td class="num">{{ x.memberCount }}</td><td class="num">{{ x.played }}</td><td class="num">{{ x.points }}</td>
              </tr>
              <tr v-if="!teams.length"><td colspan="5" style="text-align:center;color:var(--text3);padding:24px">暂无队伍报名</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <div class="card">
          <div class="hd"><h3>排期冲突检测</h3><div class="sp"></div><span class="tip">{{ conflicts.length }} 项</span></div>
          <div class="bd">
            <div v-for="(c, i) in conflicts" :key="i" class="cf">
              <div class="cft"><span class="tag t-red nodot">{{ c.type }}</span><span class="num tip">{{ c.matchNo }}</span></div>
              <div class="cfd">{{ c.advice }}（当前 {{ c.time }}）</div>
            </div>
            <div v-if="!conflicts.length" class="ok">排期校验通过，无场地 / 裁判 / 休息间隔冲突</div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>受影响预约</h3></div>
          <div class="bd">
            <div v-for="b in affected" :key="b.id" class="af">
              <div class="cellmain">{{ b.bookingNo }}</div>
              <div class="cellsub">{{ b.teamName }} · {{ fmtShort(b.startTime) }}</div>
            </div>
            <div v-if="!affected.length" class="tip">无受影响预约</div>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>可用裁判</h3></div>
          <div class="bd">
            <div v-for="r in referees.slice(0, 6)" :key="r.id" class="ref">
              <span>{{ r.realName }}<em>{{ r.roleType }} · {{ r.sportType }}</em></span>
              <span class="num tip">本月 {{ r.monthCount }} 场</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import http from '../http'
import Tag from '../components/Tag.vue'
import { fmtShort } from '../utils/format'

const route = useRoute()
const t = ref({})
const v = ref({})
const teams = ref([])
const matches = ref([])
const referees = ref([])
const conflicts = ref([])
const affected = ref([])

const refName = id => (referees.value.find(r => r.id === id) || {}).realName || '—'

async function load() {
  const d = await http.get('/tournament/' + route.params.id)
  t.value = d.tournament || {}
  v.value = d.venue || {}
  teams.value = d.teams || []
  matches.value = d.matches || []
  referees.value = d.referees || []
  conflicts.value = d.conflicts || []
  affected.value = d.affectedBookings || []
}

async function setStatus(status) {
  await http.put('/tournament/' + t.value.id, { ...t.value, status })
  load()
}

async function publish() {
  if (conflicts.value.length) {
    await ElMessageBox.alert('存在 ' + conflicts.value.length + ' 项排期冲突，请先调整赛程再发布', '无法发布', { type: 'warning' })
    return
  }
  setStatus('PUBLISHED')
}

async function score(m) {
  const a = await ElMessageBox.prompt('请输入主队得分', '录入比分 · ' + m.matchNo)
  const b = await ElMessageBox.prompt('请输入客队得分', '录入比分 · ' + m.matchNo)
  await http.post('/tournament/match/' + m.id + '/score', { scoreHome: Number(a.value), scoreAway: Number(b.value) })
  load()
}

onMounted(load)
</script>
<style scoped>
.cf { padding: 8px 0; border-bottom: 1px dashed #E9F0F5; }
.cf:last-child { border-bottom: none; }
.cft { display: flex; justify-content: space-between; align-items: center; }
.cfd { font-size: 11.5px; color: var(--text3); margin-top: 4px; }
.ok { font-size: 12.5px; color: var(--success); background: #EDF7F2; border: 1px solid #C9E5D8; border-radius: 6px; padding: 9px 11px; }
.af, .ref { padding: 6px 0; border-bottom: 1px dashed #E9F0F5; font-size: 12.5px; }
.af:last-child, .ref:last-child { border-bottom: none; }
.ref { display: flex; justify-content: space-between; }
.ref em { font-style: normal; color: var(--text3); font-size: 11px; margin-left: 8px; }
</style>
