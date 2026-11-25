<template>
  <div class="portal-wrap">
    <!-- 门户导航：前台风格，深蓝 64px 顶栏 -->
    <header class="pnav">
      <div class="pb"><span class="plogo">体</span>体育场馆预约平台</div>
      <nav class="pl">
        <router-link to="/portal" :class="{ on: route.path === '/portal' }">场馆资源</router-link>
        <router-link to="/my-bookings" :class="{ on: route.path === '/my-bookings' }">我的预约</router-link>
        <router-link to="/portal/messages" :class="{ on: route.path === '/portal/messages' }">消息中心</router-link>
        <router-link to="/portal/profile" :class="{ on: route.path === '/portal/profile' }">个人中心</router-link>
      </nav>
      <div class="sp"></div>
      <el-dropdown trigger="click" @command="onCmd">
        <div class="pwho">
          <img :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="" />
          <span>{{ user.realName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="canBackend" command="backend">进入管理后台</el-dropdown-item>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, clearLogin } from '../store'

const route = useRoute()
const router = useRouter()
const user = computed(() => store.user || {})
const canBackend = computed(() => ['ADMIN', 'STAFF', 'EVENT_ADMIN', 'CAPTAIN'].includes(user.value.roleCode))

function onCmd(cmd) {
  if (cmd === 'logout') {
    clearLogin()
    router.replace('/login')
  } else if (cmd === 'backend') {
    router.push('/dashboard')
  } else {
    router.push('/portal/' + cmd)
  }
}
</script>

<style scoped>
.portal-wrap { min-height: 100vh; background: #EFF5F9; }
.plogo { width: 30px; height: 30px; border-radius: 8px; background: linear-gradient(135deg, #2878B5, #4FA3D9); display: grid; place-items: center; font-size: 14px; }
.pnav a { text-decoration: none; }
.pwho { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; font-size: 13px; }
.pwho img { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; border: 1px solid rgba(255,255,255,.35); }
</style>
