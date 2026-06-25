<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="brand-chip">Commercial Edition</div>
      <h1>{{ APP_NAME }}</h1>
      <p class="brand-subtitle">{{ APP_SUBTITLE }}</p>
      <p class="brand-description">{{ APP_DESCRIPTION }}</p>

      <div class="brand-metrics">
        <article class="metric-card glass-card">
          <span>运营协同</span>
          <strong>预约、接诊、库存一体化</strong>
        </article>
        <article class="metric-card glass-card">
          <span>安全可控</span>
          <strong>基于角色的页面与操作权限</strong>
        </article>
        <article class="metric-card glass-card">
          <span>随时上线</span>
          <strong>支持本地演示与业务迭代验证</strong>
        </article>
      </div>
    </section>

    <section class="login-panel glass-card">
      <div class="login-panel__header">
        <div>
          <h2>欢迎登录</h2>
          <p>登录后进入门诊运营工作台</p>
        </div>
        <el-tag effect="dark" type="success">系统在线</el-tag>
      </div>

      <el-alert
        title="演示环境默认账号：admin / 12345"
        type="info"
        :closable="false"
        show-icon
        class="demo-alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="User"
            placeholder="请输入用户名"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            prefix-icon="Lock"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="rememberAccount">记住账号</el-checkbox>
          <span class="login-hint">建议使用 Chrome 或 Edge 浏览器</span>
        </div>

        <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="handleLogin">
          进入工作台
        </el-button>
      </el-form>

      <div class="account-grid">
        <button
          v-for="account in quickAccounts"
          :key="account.role"
          type="button"
          class="account-card"
          @click="fillDemoAccount(account.username)"
        >
          <strong>{{ account.label }}</strong>
          <span>{{ account.username }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { APP_DESCRIPTION, APP_NAME, APP_SUBTITLE } from '../../constants/app'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

const REMEMBER_KEY = 'medicare_remembered_account'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberAccount = ref(!!localStorage.getItem(REMEMBER_KEY))

const form = reactive({
  username: localStorage.getItem(REMEMBER_KEY) || 'admin',
  password: '12345',
})

const quickAccounts = [
  { label: '系统管理员', username: 'admin', role: 'admin' },
  { label: '门诊医生', username: 'doctor1', role: 'doctor' },
  { label: '药剂师', username: 'pharmacist1', role: 'pharmacist' },
]

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }],
}

function fillDemoAccount(username: string) {
  form.username = username
  form.password = '12345'
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const res = await login(form)
    userStore.setUser(res.data)

    if (rememberAccount.value) {
      localStorage.setItem(REMEMBER_KEY, form.username)
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }

    ElMessage.success('登录成功，正在进入工作台')
    router.push('/dashboard')
  } catch {
    // Response error is handled globally by axios interceptors.
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(380px, 460px);
  gap: 36px;
  align-items: stretch;
  min-height: 100vh;
  padding: 40px;
}

.login-brand {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 54px;
  border-radius: 32px;
  background:
    linear-gradient(140deg, rgba(6, 78, 59, 0.92), rgba(15, 118, 110, 0.9)),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 28%);
  color: #f7fffd;
  box-shadow: 0 24px 55px rgba(15, 23, 42, 0.18);
}

.login-brand::after {
  content: '';
  position: absolute;
  inset: auto -40px -60px auto;
  width: 260px;
  height: 260px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.brand-chip {
  display: inline-flex;
  width: fit-content;
  margin-bottom: 24px;
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-brand h1 {
  margin: 0;
  font-size: 48px;
  line-height: 1.05;
}

.brand-subtitle {
  margin: 14px 0 0;
  font-size: 26px;
  font-weight: 700;
}

.brand-description {
  max-width: 620px;
  margin: 18px 0 0;
  color: rgba(247, 255, 253, 0.82);
  line-height: 1.85;
}

.brand-metrics {
  display: grid;
  gap: 16px;
  margin-top: 34px;
}

.metric-card {
  padding: 18px 20px;
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.12);
  color: #f7fffd;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  color: rgba(247, 255, 253, 0.72);
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.metric-card strong {
  font-size: 18px;
  font-weight: 700;
}

.login-panel {
  align-self: center;
  padding: 32px;
}

.login-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.login-panel__header h2 {
  margin: 0;
  font-size: 28px;
}

.login-panel__header p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.demo-alert {
  margin-bottom: 24px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.login-hint {
  color: var(--mc-text-soft);
  font-size: 13px;
}

.login-submit {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--mc-primary), #0f9d8f);
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.22);
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.account-card {
  padding: 14px 12px;
  border: 1px solid var(--mc-border);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.02);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.account-card:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.28);
  background: rgba(15, 118, 110, 0.06);
}

.account-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--mc-text);
}

.account-card span {
  color: var(--mc-text-soft);
  font-size: 13px;
}
</style>
