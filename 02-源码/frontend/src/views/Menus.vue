<template>
  <div>
    <div class="ph">
      <div><h1>菜单管理</h1><p>目录、页面与按钮三级结构，按钮权限用于控制行内操作显隐</p></div>
      <div class="sp"></div>
      <div class="acts"><button class="btn pri" @click="openEdit()">新增菜单</button></div>
    </div>
    <div class="card">
      <table>
        <thead><tr><th style="width:44%">菜单名称</th><th>类型</th><th>路由路径</th><th>权限标识</th><th class="nowrap">排序</th><th>可见</th><th class="nowrap">操作</th></tr></thead>
        <tbody>
          <template v-for="g in tree" :key="g.id">
            <tr style="background:#FAFCFE">
              <td class="cellmain">{{ g.name }}</td>
              <td><span class="tag t-blue nodot">目录</span></td>
              <td class="no">{{ g.path }}</td>
              <td class="no">—</td>
              <td class="num">{{ g.sort }}</td>
              <td>{{ g.visible ? '显示' : '隐藏' }}</td>
              <td class="nowrap"><button class="btn sm lnk" @click="openEdit(g)">编辑</button></td>
            </tr>
            <tr v-for="c in g.children || []" :key="c.id">
              <td style="padding-left:32px">{{ c.name }}</td>
              <td><span class="tag t-green nodot">页面</span></td>
              <td class="no">{{ c.path }}</td>
              <td class="no">{{ c.perm || '—' }}</td>
              <td class="num">{{ c.sort }}</td>
              <td>{{ c.visible ? '显示' : '隐藏' }}</td>
              <td class="nowrap"><button class="btn sm lnk" @click="openEdit(c)">编辑</button></td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="dlg" title="菜单信息" width="520px">
      <div class="fl"><label>名称</label><div class="v"><el-input v-model="form.menuName" /></div></div>
      <div class="fl"><label>上级</label><div class="v">
        <el-select v-model="form.parentId" style="width:100%">
          <el-option label="顶级目录" :value="0" />
          <el-option v-for="g in tree" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
      </div></div>
      <div class="fl"><label>类型</label><div class="v">
        <el-select v-model="form.menuType" style="width:100%">
          <el-option label="目录" value="M" /><el-option label="页面" value="C" /><el-option label="按钮" value="B" />
        </el-select>
      </div></div>
      <div class="fl"><label>路径</label><div class="v"><el-input v-model="form.path" placeholder="/venues" /></div></div>
      <div class="fl"><label>权限标识</label><div class="v"><el-input v-model="form.perm" placeholder="venue:view" /></div></div>
      <div class="fl"><label>排序</label><div class="v"><el-input-number v-model="form.sort" :min="0" /></div></div>
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

const tree = ref([])
const dlg = ref(false)
const form = reactive({ parentId: 0, menuType: 'C', sort: 1, visible: 1 })

async function load() { tree.value = await http.get('/system/menu/tree') }

function openEdit(m) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, m ? { ...m, parentId: m.parentId || 0 } : { parentId: 0, menuType: 'C', sort: 1, visible: 1 })
  dlg.value = true
}

async function save() {
  await http.post('/system/menu', form)
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
