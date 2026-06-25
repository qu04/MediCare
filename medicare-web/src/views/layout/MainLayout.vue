<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapse ? '88px' : '284px'" class="sidebar">
      <div class="brand-panel">
        <div class="brand-mark">
          <el-icon :size="22"><FirstAidKit /></el-icon>
        </div>
        <div v-show="!isCollapse" class="brand-copy">
          <strong>{{ APP_NAME }}</strong>
          <span>{{ APP_SUBTITLE }}</span>
        </div>
      </div>

      <div v-show="!isCollapse" class="nav-summary glass-card">
        <div class="nav-summary__head">
          <span class="muted">当前角色</span>
          <el-tag :type="ROLE_BADGE_TYPES[userRole]" effect="plain">{{ ROLE_LABELS[userRole] }}</el-tag>
        </div>
        <strong>{{ greetingText }}，{{ userStore.currentUser?.realName }}</strong>
        <p>当前系统已接入业务后端，可直接用于演示、联调与二次开发。</p>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="route.path" :collapse="isCollapse" router class="app-menu">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" class="app-menu__item">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>
              <div class="menu-copy">
                <span>{{ item.title }}</span>
                <small>{{ item.description }}</small>
              </div>
            </template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="workspace">
      <el-header class="workspace-header">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <div>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>工作台</el-breadcrumb-item>
              <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
            <h1 class="header-title">{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="header-right">
          <div class="status-tile">
            <span class="muted">系统时间</span>
            <strong>{{ currentTime }}</strong>
          </div>
          <div class="status-tile">
            <span class="muted">系统状态</span>
            <strong>业务服务在线</strong>
          </div>
          <div class="profile-card">
            <div class="profile-avatar">{{ avatarText }}</div>
            <div class="profile-copy">
              <strong>{{ userStore.currentUser?.realName }}</strong>
              <span>{{ ROLE_LABELS[userRole] }}</span>
            </div>
            <el-button text @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main class="workspace-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { APP_MENU_ITEMS, APP_NAME, APP_SUBTITLE, ROLE_BADGE_TYPES, ROLE_LABELS } from '../../constants/app'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const now = ref(new Date())
let timer: number | undefined

const userRole = computed<'admin' | 'doctor' | 'pharmacist'>(() => userStore.currentUser?.role || 'admin')
const menuItems = computed(() => APP_MENU_ITEMS.filter((item) => item.roles.includes(userRole.value)))
const pageTitle = computed(() => (route.meta.title as string) || '工作台')
const avatarText = computed(() => userStore.currentUser?.realName?.slice(0, 1) || 'M')
const greetingText = computed(() => {
  const hour = now.value.getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const currentTime = computed(() =>
  now.value.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }),
)

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = new Date()
  }, 1000 * 30)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前账户吗？系统将返回登录页。', '退出登录', {
      type: 'warning',
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
    })

    await userStore.logoutAndClear()
    router.push('/login')
  } catch {
    // User canceled logout.
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px 16px;
  background:
    linear-gradient(180deg, #0d1d30 0%, #12253c 100%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 26%);
  color: #edf6fb;
  transition: width 0.28s ease;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 12px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.94), rgba(13, 148, 136, 0.78));
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.25);
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  font-size: 18px;
}

.brand-copy span {
  margin-top: 4px;
  color: rgba(237, 246, 251, 0.68);
  font-size: 12px;
}

.nav-summary {
  padding: 18px;
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.08);
  color: #edf6fb;
}

.nav-summary__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.nav-summary strong {
  display: block;
  margin-bottom: 8px;
  font-size: 18px;
}

.nav-summary p {
  margin: 0;
  color: rgba(237, 246, 251, 0.7);
  line-height: 1.7;
  font-size: 13px;
}

.menu-scroll {
  flex: 1;
}

:deep(.app-menu) {
  border-right: none;
  background: transparent;
}

:deep(.app-menu__item) {
  height: auto;
  min-height: 62px;
  margin-bottom: 8px;
  border-radius: 16px;
  color: rgba(237, 246, 251, 0.76);
  line-height: 1.2;
}

:deep(.app-menu__item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

:deep(.app-menu .is-active) {
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.94), rgba(13, 148, 136, 0.78));
  color: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.2);
}

.menu-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-copy small {
  color: inherit;
  opacity: 0.7;
  font-size: 12px;
}

.workspace {
  background: transparent;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 92px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--mc-border);
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(16px);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.collapse-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

.header-title {
  margin: 8px 0 0;
  font-size: 24px;
}

.status-tile,
.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--mc-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
}

.status-tile {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.status-tile strong {
  font-size: 14px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.18), rgba(13, 148, 136, 0.32));
  color: var(--mc-primary-deep);
  font-weight: 700;
}

.profile-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-copy span {
  color: var(--mc-text-soft);
  font-size: 13px;
}

.workspace-main {
  padding: 24px;
  background: transparent;
}
</style>
