<template>
  <div>
    <div class="ph">
      <div><h1>角色管理</h1><p>角色决定菜单可见性与数据范围，权限变更后重新登录生效</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn pri" @click="addRole">新增角色</button></div>
    </div>
    <div class="card">
      <table>
        <thead><tr><th>角色名称</th><th>角色编码</th><th>数据范围</th><th>说明</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td class="cellmain">{{ r.roleName }}</td>
            <td class="no">{{ r.roleCode }}</td>
            <td>{{ scopeText(r.dataScope) }}</td>
            <td>{{ r.remark }}</td>
            <td class="nowrap">
              <button class="btn sm" @click="openAssign(r)">分配菜单</button>
              <button class="btn sm" @click="editRole(r)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分配菜单：树形勾选，隐藏「菜单管理」项本身 -->
    <el-drawer v-model="drawer" :title="'分配菜单 · ' + (cur.roleName || '')" size="440px">
      <el-tree ref="treeRef" :data="tree" node-key="id" show-checkbox default-expand-all
        :props="{ label: 'name', children: 'children' }" style="font-size:12.5px" />
      <template #footer>
        <button class="btn" @click="drawer = false">取消</button>
        <button class="btn pri" @click="saveAssign">保存授权</button>
      </template>
    </el-drawer>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <div class="fl"><label>角色名称</label><div class="v"><el-input v-model="form.roleName" /></div></div>
      <div class="fl"><label>角色编码</label><div class="v"><el-input v-model="form.roleCode" placeholder="如 CAPTAIN" /></div></div>
      <div class="fl"><label>数据范围</label><div class="v">
        <el-select v-model="form.dataScope" style="width:100%">
          <el-option label="全部数据" value="ALL" /><el-option label="本部门" value="DEPT" /><el-option label="仅本人" value="SELF" />
        </el-select>
      </div></div>
      <div class="fl"><label>说明</label><div class="v"><el-input v-model="form.remark" type="textarea" :rows="2" /></div></div>
      <template #footer>
        <button class="btn" @click="dlg = false">取消</button>
        <button class="btn pri" @click="saveRole">保存</button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'

const list = ref([])
const tree = ref([])
const drawer = ref(false)
const dlg = ref(false)
const cur = ref({})
const treeRef = ref(null)
const form = reactive({ dataScope: 'SELF', status: 1 })

const scopeText = s => ({ ALL: '全部数据', DEPT: '本部门数据', SELF: '仅本人数据' }[s] || s)

async function load() { list.value = await http.get('/system/role/list') }

async function openAssign(r) {
  cur.value = r
  drawer.value = true
  tree.value = await http.get('/system/menu/tree')
  // 菜单管理节点不参与授权（规范要求隐藏该选项）
  const strip = nodes => nodes.filter(n => n.id !== 43).map(n => ({ ...n, children: n.children ? strip(n.children) : undefined }))
  tree.value = strip(tree.value)
  const ids = await http.get('/system/role/' + r.id + '/menus')
  await nextTick()
  if (treeRef.value) {
    treeRef.value.setCheckedKeys([])
    ids.forEach(id => {
      const node = treeRef.value.getNode(id)
      if (node && node.isLeaf) treeRef.value.setChecked(id, true, false)
    })
  }
}

async function saveAssign() {
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await http.post('/system/role/' + cur.value.id + '/menus', { menuIds: ids })
  ElMessage.success('授权已保存，重新登录后生效')
  drawer.value = false
}

function addRole() {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { dataScope: 'SELF', status: 1 })
  dlg.value = true
}
function editRole(r) { Object.keys(form).forEach(k => delete form[k]); Object.assign(form, r); dlg.value = true }

async function saveRole() {
  await http.post('/system/role', form)
  dlg.value = false
  load()
}

onMounted(load)
</script>
<style scoped>
.fl { display: flex; margin-bottom: 12px; font-size: 12.5px; }
.fl label { width: 84px; flex: 0 0 84px; color: var(--text2); padding-top: 7px; }
.fl .v { flex: 1; }
</style>
