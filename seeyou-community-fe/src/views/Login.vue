<template>
  <div class="auth-page">
    <div class="auth-glow auth-glow-a" />
    <div class="auth-glow auth-glow-b" />
    <div class="auth-card sy-card">
      <div class="auth-logo">
        <span class="logo-badge">ZY</span>
        <span class="logo-text">之友<em>开发者社区</em></span>
      </div>
      <h2 class="auth-title">欢迎回来</h2>
      <p class="auth-sub">登录后继续你的技术之旅</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button class="auth-btn" type="primary" size="large" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？<router-link class="auth-link" to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f5 0%, #f6f7ff 50%, #eef4ff 100%);
}
.auth-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
  pointer-events: none;
}
.auth-glow-a {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(230, 0, 18, 0.22), transparent 70%);
  top: -100px;
  left: -80px;
}
.auth-glow-b {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(22, 140, 255, 0.2), transparent 70%);
  bottom: -140px;
  right: -80px;
}
.auth-card {
  width: 400px;
  padding: 40px 36px;
  border-radius: var(--sy-radius-lg);
  box-shadow: var(--sy-shadow-lg);
  position: relative;
}
.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.logo-badge {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--sy-primary-grad);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(230, 0, 18, 0.3);
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.logo-text em {
  font-style: normal;
  font-size: 13px;
  font-weight: 500;
  color: var(--sy-text-secondary);
}
.auth-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  margin: 18px 0 2px;
}
.auth-sub {
  text-align: center;
  font-size: 13px;
  color: var(--sy-text-secondary);
  margin-bottom: 22px;
}
.auth-btn {
  width: 100%;
  margin-top: 4px;
}
.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--sy-text-secondary);
}
.auth-link {
  color: var(--sy-primary);
}
</style>
