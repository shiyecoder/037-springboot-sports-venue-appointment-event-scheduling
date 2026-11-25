<template>
  <div class="app">
    <!-- 侧边栏：菜单由登录返回的角色菜单树动态渲染 -->
    <aside class="side">
      <div class="brand">
        <div class="logo">体</div>
        <b>体育场馆管理</b>
      </div>
      <nav class="menu">
        <template v-for="grp in menus" :key="grp.id">
          <div class="grp">{{ grp.name }}</div>
          <template v-for="m in leaves(grp)" :key="m.id">
            <router-link v-if="m.path" :to="m.path" :class="{ on: isOn(m.path) }">
              <svg viewBox="0 0 24 24"><use :href="'#i-' + (m.icon || 'dashboard')" /></svg>
              {{ m.name }}
            </router-link>
          </template>
        </template>
      </nav>
    </aside>

    <div class="main">
      <header class="top">
        <div class="crumb">{{ groupName }} / <b>{{ pageTitle }}</b></div>
        <div class="sp"></div>
        <div class="ico" title="消息中心" @click="$router.push('/messages')">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.6">
            <use href="#i-message" />
          </svg>
          <span v-if="unread > 0" class="dot"></span>
        </div>
        <el-dropdown trigger="click" @command="onCmd">
          <div class="who">
            <img class="ava" :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="" />
            <div>
              <div class="nm">{{ user.realName }}</div>
              <div class="rl">{{ user.roleName }}</div>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="portal">场馆资源门户</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../http'
import { store, clearLogin } from '../store'

const route = useRoute()
const router = useRouter()
const unread = ref(0)
const user = computed(() => store.user || {})
const menus = computed(() => (store.menus || []).filter(m => m.children && m.children.length))

// 一级目录下的可见子菜单（隐藏菜单管理类按钮项，只保留有 path 的页面）
const leaves = grp => (grp.children || []).filter(m => m.path)

const pageTitle = computed(() => route.meta.title || '场馆经营看板')
const groupName = computed(() => {
  const g = menus.value.find(m => (m.children || []).some(c => c.path === route.path))
  return g ? g.name : '运营总览'
})

const isOn = path => route.path === path || (path !== '/' && route.path.startsWith(path + '/'))

async function loadUnread() {
  try {
    const d = await http.get('/message/unread')
    unread.value = typeof d === 'number' ? d : (d && d.count) || 0
  } catch (e) {
    unread.value = 0
  }
}

function onCmd(cmd) {
  if (cmd === 'logout') {
    clearLogin()
    router.replace('/login')
  } else if (cmd === 'portal') {
    router.push('/portal')
  } else {
    router.push('/' + cmd)
  }
}

onMounted(loadUnread)
</script>

<style scoped>
.who { cursor: pointer; }
.who:hover .nm { color: var(--primary); }
.menu a { text-decoration: none; }
</style>
