<template>
  <div class="auth-page">
    <div class="auth-card sy-card">
      <div class="auth-logo">
        <span class="logo-mark">之友</span>
        <span class="logo-text">开发者社区</span>
      </div>
      <h2 class="auth-title">创建账号</h2>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（3-50 位）">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可选，默认同用户名）">
            <template #prefix><el-icon><Avatar /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（6-50 位）" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button class="auth-btn" type="primary" size="large" :loading="loading" @click="submit">
          注 册
        </el-button>
      </el-form>

      <div class="auth-footer">
        已有账号？<router-link class="auth-link" to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3-50 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '长度 6-50 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => (v === form.password ? cb() : cb(new Error('两次密码不一致'))),
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
