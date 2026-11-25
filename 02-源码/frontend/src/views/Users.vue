<template>
  <div>
    <div class="ph">
      <div><h1>用户管理</h1><p>账号按角色分配数据权限，登录失败 5 次锁定 30 分钟</p></div>
      <div class="sp"></div>
      <div class="acts">
        <button class="btn" @click="load">刷新</button>
        <button class="btn pri" @click="openEdit()">新增用户</button>
      </div>
    </div>
    <div class="card">
      <div class="filters">
        <input v-model="keyword" class="inp w220" placeholder="姓名 / 账号 / 手机号" @keyup.enter="search" />
        <select v-model="roleId" class="sel"><option value="">全部角色</option>
          <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}</option>
        </select>
        <div class="sp"></div>
        <button class="btn pri" @click="search">查询</button>
      </div>
      <table>
        <thead><tr><th>用户</th><th>账号</th><th>角色</th><th>所属单位</th><th class="nowrap">手机号</th><th class="nowrap">信用分</th><th>状态</th><th class="nowrap">最近登录</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>
              <div style="display:flex;gap:9px;align-items:center">
                <img class="ava" :src="r.avatar || '/uploads/seed/avatar/01.jpg'" style="width:30px;height:30px;border-radius:50%;object-fit:cover" alt="" />
                <div><div class="cellmain">{{ r.realName }}</div><div class="cellsub">{{ r.email }}</div></div>
              </div>
            </td>
            <td class="no">{{ r.username }}</td>
            <td>{{ r.roleName }}</td>
            <td>{{ r.orgName }}</td>
            <td class="num">{{ r.phone }}</td>
            <td class="num">{{ r.creditScore ?? '—' }}</td>
            <td><span class="tag" :class="r.status === 1 ? 't-green' : 't-gray'">{{ r.status === 1 ? '正常' : '停用' }}</span></td>
            <td class="num nowrap">{{ fmtTime(r.lastLogin) }}</td>
            <td class="nowrap">
              <button class="btn sm" @click="openEdit(r)">编辑</button>
              <button class="btn sm" @click="resetPwd(r)">重置密码</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="9" style="text-align:center;color:var(--text3);padding:30px">无匹配用户</td></tr>
        </tbody>
      </table>
      <div class="pager">
        <span>共 {{ total }} 个账号</span><div class="sp"></div>
        <el-pagination layout="sizes, prev, pager, next" :total="total" :current-page="page" :page-size="size"
          :page-sizes="[10, 20, 50]" @current-change="p => { page = p; load() }" @size-change="s => { size = s; page = 1; load() }" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <div class="fl"><label>账号</label><div class="v"><el-input v-model="form.username" :disabled="!!form.id" /></div></div>
      <div class="fl"><label>姓名</label><div class="v"><el-input v-model="form.realName" /></div></div>
      <div class="fl"><label>角色</label><div class="v">
        <el-select v-model="form.roleId" style="width:100%"><el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" /></el-select>
      </div></div>
      <div class="fl"><label>所属单位</label><div class="v">
        <el-select v-model="form.orgId" style="width:100%"><el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" /></el-select>
      </div></div>
      <div class="fl"><label>手机号</label><div class="v"><el-input v-model="form.phone" /></div></div>
      <div class="fl"><label>邮箱</label><div class="v"><el-input v-model="form.email" /></div></div>
      <div class="fl"><label>状态</label><div class="v">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
      </div></div>
      <template #footer>
        <button class="btn" @click="dlg = false">取消</button>
        <button class="btn pri" @click="save">保存</button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../http'
import { fmtTime } from '../utils/format'

const list = ref([])
const roles = ref([])
const orgs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const roleId = ref('')
const dlg = ref(false)
const form = reactive({ status: 1 })

async function load() {
  const params = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  if (roleId.value) params.roleId = roleId.value
  const d = await http.get('/system/user/page', { params })
  list.value = d.list || []
  total.value = d.total || 0
}
function search() { page.value = 1; load() }

function openEdit(r) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r || { status: 1, roleId: 5, orgId: 1 })
  dlg.value = true
}

async function save() {
  if (form.id) await http.put('/system/user/' + form.id, form)
  else await http.post('/system/user', form)
  dlg.value = false
  load()
}

async function resetPwd(r) {
  const res = await ElMessageBox.prompt('请输入新密码（不少于 6 位）', '重置密码 · ' + r.realName, { inputValue: '123456' })
  await http.post('/system/user/' + r.id + '/reset-password', { password: res.value })
  ElMessage.success('密码已重置')
}

onMounted(async () => {
  roles.value = await http.get('/system/role/list')
  orgs.value = await http.get('/system/org/list')
  load()
})
</script>
<style scoped>
.fl { display: flex; margin-bottom: 12px; font-size: 12.5px; }
.fl label { width: 84px; flex: 0 0 84px; color: var(--text2); padding-top: 7px; }
.fl .v { flex: 1; }
</style>
