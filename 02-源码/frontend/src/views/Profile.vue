<template>
  <div>
    <div class="ph"><div><h1>个人中心</h1><p>维护联系方式与登录密码，头像上传后右上角即时同步</p></div></div>
    <div class="grid2">
      <div class="card">
        <div class="hd"><h3>基本信息</h3></div>
        <div class="bd">
          <div class="ava-row">
            <img :src="form.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <div>
              <div class="cellmain" style="font-size:14px">{{ form.realName }}</div>
              <div class="cellsub">{{ form.username }} · {{ form.roleName }}</div>
              <el-upload :action="uploadUrl" :headers="headers" :show-file-list="false" :on-success="onAvatar" style="margin-top:8px">
                <button class="btn sm">更换头像</button>
              </el-upload>
            </div>
          </div>
          <div class="fl"><label>姓名</label><div class="v"><el-input v-model="form.realName" /></div></div>
          <div class="fl"><label>手机号</label><div class="v"><el-input v-model="form.phone" /></div></div>
          <div class="fl"><label>邮箱</label><div class="v"><el-input v-model="form.email" /></div></div>
          <div class="fl"><label>所属单位</label><div class="v"><el-input :model-value="form.orgName" disabled /></div></div>
          <button class="btn pri" @click="saveProfile">保存资料</button>
        </div>
      </div>
      <div>
        <div class="card">
          <div class="hd"><h3>修改密码</h3></div>
          <div class="bd">
            <div class="fl"><label>原密码</label><div class="v"><el-input v-model="pwd.old" type="password" show-password /></div></div>
            <div class="fl"><label>新密码</label><div class="v"><el-input v-model="pwd.n1" type="password" show-password /></div></div>
            <div class="fl"><label>确认新密码</label><div class="v"><el-input v-model="pwd.n2" type="password" show-password /></div></div>
            <button class="btn pri" @click="savePwd">更新密码</button>
          </div>
        </div>
        <div class="card">
          <div class="hd"><h3>账号信息</h3></div>
          <div class="bd">
            <div class="dl one">
              <div class="r"><span class="k">账号</span><span class="v num">{{ form.username }}</span></div>
              <div class="r"><span class="k">角色</span><span class="v">{{ form.roleName }}</span></div>
              <div class="r"><span class="k">数据范围</span><span class="v">{{ form.dataScope }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../http'
import { store, setUser } from '../store'

const form = reactive({})
const pwd = reactive({ old: '', n1: '', n2: '' })
const uploadUrl = '/api/file/upload'
const headers = computed(() => ({ Authorization: 'Bearer ' + store.token }))

async function load() {
  const d = await http.get('/auth/profile')
  Object.assign(form, d)
}

async function saveProfile() {
  await http.put('/auth/profile', { realName: form.realName, phone: form.phone, email: form.email })
  setUser({ ...store.user, realName: form.realName, phone: form.phone, email: form.email })
  ElMessage.success('资料已保存')
}

async function savePwd() {
  if (!pwd.n1 || pwd.n1 !== pwd.n2) return ElMessage.warning('两次输入的新密码不一致')
  await http.put('/auth/password', { oldPassword: pwd.old, newPassword: pwd.n1 })
  ElMessage.success('密码已更新')
  pwd.old = pwd.n1 = pwd.n2 = ''
}

async function onAvatar(res) {
  const d = res && res.data ? res.data : res
  form.avatar = (d && d.avatar) || (d && d.path)
  setUser({ ...store.user, avatar: form.avatar })
  ElMessage.success('头像已更新')
}

onMounted(load)
</script>
<style scoped>
.ava-row { display: flex; gap: 14px; align-items: center; padding-bottom: 14px; border-bottom: 1px solid var(--line); margin-bottom: 14px; }
.ava-row img { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; background: #DCE8F1; }
.fl { display: flex; margin-bottom: 12px; font-size: 12.5px; }
.fl label { width: 84px; flex: 0 0 84px; color: var(--text2); padding-top: 7px; }
.fl .v { flex: 1; }
</style>
