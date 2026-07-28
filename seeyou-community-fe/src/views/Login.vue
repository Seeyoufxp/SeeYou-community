<template>
  <div class="auth-page">
    <div class="auth-card sy-card">
      <div class="auth-logo">
        <span class="logo-mark">之友</span>
        <span class="logo-text">开发者社区</span>
      </div>
      <h2 class="auth-title">欢迎回来</h2>

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
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f5 0%, #f6f7ff 50%, #eef4ff 100%);
}
.auth-card {
  width: 400px;
  padding: 40px 36px;
}
.auth-logo {
  text-align: center;
}
.logo-mark {
  font-size: 26px;
  font-weight: 800;
  color: var(--sy-primary);
}
.logo-text {
  font-size: 18px;
  font-weight: 600;
  margin-left: 6px;
}
.auth-title {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  margin: 12px 0 24px;
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
