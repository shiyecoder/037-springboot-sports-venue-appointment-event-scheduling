<template>
  <div class="lg-page">
    <!-- 左侧品牌区：场馆实景背景 + 深钴蓝渐变遮罩 -->
    <section class="lg-left">
      <img class="bg" :src="bg" alt="校园体育馆" />
      <div class="mask"></div>
      <div class="in">
        <div class="lg-logo"><span class="lm">体</span>体育场馆预约与赛事排期管理系统</div>
        <div class="lg-mid">
          <h1>一馆一档 · 时段可控<br />预约、排期与冲突协同在同一处闭环</h1>
          <p>面向校园场馆运营：场地档案与开放时段、个人与团体预约审批、入场核验与设备借用、赛事编排与裁判场地冲突校验，全部按规则留痕。</p>
          <div class="lg-stats">
            <div><b class="num">18</b><span>在册场馆</span></div>
            <div><b class="num">1,008</b><span>可约时段</span></div>
            <div><b class="num">92.4%</b><span>时段利用率</span></div>
          </div>
        </div>
        <div class="lg-foot">场馆运营科 · 内部业务系统 · 建议使用 Chrome 浏览器</div>
      </div>
    </section>

    <!-- 右侧登录表单 -->
    <section class="lg-right">
      <div class="lg-form">
        <h2>账号登录</h2>
        <p class="sb">请使用场馆运营科分配的账号登录，不同角色进入不同工作台</p>
        <form @submit.prevent="doLogin">
          <div class="fi">
            <label>账号</label>
            <input v-model.trim="form.username" class="inp" placeholder="请输入账号" autocomplete="username" />
          </div>
          <div class="fi">
            <label>密码</label>
            <input v-model.trim="form.password" class="inp" type="password" placeholder="请输入密码" autocomplete="current-password" />
          </div>
          <div class="row">
            <span>登录失败 5 次将锁定账号 30 分钟</span>
          </div>
          <button class="go" type="submit" :disabled="loading">{{ loading ? '登录中…' : '登 录' }}</button>
          <p v-if="error" class="err" style="display:block">{{ error }}</p>
        </form>
        <div class="lg-hint">
          <b>演示账号</b>（密码均为角色名+123）<br />
          admin / admin123 · staff1 / staff123 · event1 / event123<br />
          captain1 / captain123 · user1 / user123
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../http'
import { setLogin } from '../store'
import bg from '../assets/login-bg.jpg'

const router = useRouter()
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)
const error = ref('')

async function doLogin() {
  if (!form.username || !form.password) {
    error.value = '请输入账号与密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await http.post('/auth/login', { username: form.username, password: form.password })
    setLogin(data)
    // 普通用户进前台门户，其余角色进管理后台
    router.replace(data.user.roleCode === 'USER' ? '/portal' : '/dashboard')
  } catch (e) {
    error.value = e.message || '账号或密码错误'
  } finally {
    loading.value = false
  }
}

// 登录页自行铺满视口，不叠加 body.login 的两列栅格（否则只占左列）
</script>

<style scoped>
.lg-page { position: fixed; inset: 0; display: grid; grid-template-columns: 1.05fr 0.95fr; background: #0f3b5d; overflow: auto; }
.lg-left { position: relative; overflow: hidden; color: #fff; padding: 52px 56px; display: flex; flex-direction: column; }
.lg-left .bg { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: 0.55; }
.lg-left .mask { position: absolute; inset: 0; background: linear-gradient(150deg, rgba(15,59,93,.94), rgba(15,59,93,.74) 55%, rgba(40,120,181,.62)); }
.lg-left .in { position: relative; z-index: 1; display: flex; flex-direction: column; height: 100%; }
.lg-logo { display: flex; align-items: center; gap: 11px; font-size: 16px; font-weight: 650; }
.lg-logo .lm { width: 34px; height: 34px; border-radius: 9px; background: linear-gradient(135deg, #2878b5, #4fa3d9); display: grid; place-items: center; font-size: 15px; }
.lg-mid { margin-top: auto; }
.lg-mid h1 { font-size: 30px; line-height: 1.35; font-weight: 660; letter-spacing: .5px; }
.lg-mid p { margin-top: 14px; color: #c3daeb; font-size: 13.5px; max-width: 452px; line-height: 1.85; }
.lg-stats { display: flex; gap: 36px; margin-top: 34px; }
.lg-stats b { font-size: 22px; font-weight: 660; display: block; }
.lg-stats span { font-size: 11.5px; color: #a9c6db; }
.lg-foot { margin-top: auto; padding-top: 30px; font-size: 11.5px; color: #8fb2c9; }
.lg-right { background: #fff; display: grid; place-items: center; padding: 40px; }
.lg-form { width: 368px; }
.lg-form h2 { font-size: 21px; font-weight: 660; }
.lg-form .sb { color: #5a6d7c; font-size: 12.5px; margin: 7px 0 26px; line-height: 1.7; }
.fi { margin-bottom: 15px; }
.fi label { display: block; font-size: 12.5px; color: #5a6d7c; margin-bottom: 6px; }
.fi .inp { width: 100%; height: 40px; }
.row { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #8a9aa8; margin: 2px 0 18px; }
.go { width: 100%; height: 42px; border: none; border-radius: 6px; background: #0f3b5d; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; letter-spacing: 2px; }
.go:hover { background: #0c314e; }
.go:disabled { opacity: .7; cursor: not-allowed; }
.err { color: #b22b31; font-size: 12px; margin-top: 10px; }
.lg-hint { margin-top: 18px; background: #f5f9fc; border: 1px solid #e3edf4; border-radius: 6px; padding: 10px 12px; font-size: 11.5px; color: #5a6d7c; line-height: 1.9; }
.lg-hint b { color: #0f3b5d; }
</style>
