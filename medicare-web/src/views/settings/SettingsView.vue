<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">System Governance</div>
      <h2 class="page-hero__title">系统设置与安全治理中心</h2>
      <p class="page-hero__subtitle">
        从成熟商用系统的视角统一管理账户、密码安全与操作留痕，帮助管理员完成上线后的日常治理。
      </p>
    </section>

    <section class="settings-grid">
      <article v-if="userStore.hasRole('admin')" class="glass-card security-panel">
        <header class="card-header">
          <div>
            <h3 class="section-title">安全概况</h3>
            <p class="section-subtitle">聚焦账户活跃度、异常登录与审计规模</p>
          </div>
          <el-button text @click="loadSecurityCenter">刷新</el-button>
        </header>

        <div class="security-metrics">
          <div class="metric-card">
            <span>系统用户</span>
            <strong>{{ securityOverview.totalUsers }}</strong>
          </div>
          <div class="metric-card">
            <span>活跃账号</span>
            <strong>{{ securityOverview.activeUsers }}</strong>
          </div>
          <div class="metric-card warning">
            <span>锁定账号</span>
            <strong>{{ securityOverview.lockedUsers }}</strong>
          </div>
          <div class="metric-card danger">
            <span>24h 失败登录</span>
            <strong>{{ securityOverview.recentFailedLogins }}</strong>
          </div>
        </div>
      </article>

      <article class="glass-card account-panel">
        <header class="card-header">
          <div>
            <h3 class="section-title">当前账户</h3>
            <p class="section-subtitle">查看最近登录状态和基础信息</p>
          </div>
        </header>

        <div class="account-summary">
          <div class="account-summary__main">
            <div class="account-avatar">{{ userStore.currentUser?.realName?.slice(0, 1) || 'M' }}</div>
            <div>
              <strong>{{ userStore.currentUser?.realName }}</strong>
              <p>{{ roleText(userStore.currentUser?.role || '') }} · {{ userStore.currentUser?.username }}</p>
            </div>
          </div>
          <el-tag :type="(userStore.currentUser?.status || 1) === 1 ? 'success' : 'danger'">
            {{ (userStore.currentUser?.status || 1) === 1 ? '已启用' : '已停用' }}
          </el-tag>
        </div>

        <div class="account-kv">
          <div>
            <span>最近登录时间</span>
            <strong>{{ formatDateTime(userStore.currentUser?.lastLoginTime) }}</strong>
          </div>
          <div>
            <span>最近登录 IP</span>
            <strong>{{ userStore.currentUser?.lastLoginIp || '暂无记录' }}</strong>
          </div>
          <div>
            <span>密码安全状态</span>
            <strong>{{ userStore.currentUser?.lockedUntil ? '账号锁定中' : '正常' }}</strong>
          </div>
        </div>
      </article>
    </section>

    <section class="glass-card tab-shell">
      <el-tabs v-model="activeTab">
        <el-tab-pane v-if="userStore.hasRole('admin')" label="账户管理" name="users">
          <div class="toolbar">
            <el-button type="primary" @click="openUserDialog()">
              <el-icon><Plus /></el-icon>
              新建用户
            </el-button>
          </div>

          <el-table :data="userList" border stripe>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="roleTagType(row.role)">{{ roleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近登录" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
            </el-table-column>
            <el-table-column label="登录 IP" min-width="150">
              <template #default="{ row }">{{ row.lastLoginIp || '-' }}</template>
            </el-table-column>
            <el-table-column label="锁定状态" width="180">
              <template #default="{ row }">
                <el-tag v-if="row.lockedUntil" type="warning">锁定至 {{ formatDateTime(row.lockedUntil) }}</el-tag>
                <span v-else class="muted">正常</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openUserDialog(row as SysUser)">编辑</el-button>
                <el-button size="small" type="danger" :disabled="row.username === 'admin'" @click="handleDeleteUser(row as SysUser)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-dialog v-model="userDialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="520px" destroy-on-close>
            <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="92px">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="userForm.username" :disabled="isEdit" />
              </el-form-item>
              <el-form-item v-if="!isEdit" label="初始密码" prop="password">
                <el-input v-model="userForm.password" type="password" show-password />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="userForm.realName" />
              </el-form-item>
              <el-form-item label="角色" prop="role">
                <el-select v-model="userForm.role" @change="onRoleChange">
                  <el-option label="系统管理员" value="admin" />
                  <el-option label="门诊医生" value="doctor" />
                  <el-option label="药剂师" value="pharmacist" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="userForm.role === 'doctor'" label="关联医生" prop="doctorId">
                <el-select v-model="userForm.doctorId" clearable placeholder="请选择关联医生">
                  <el-option v-for="doctor in doctorOptions" :key="doctor.id" :label="doctor.name" :value="doctor.id!" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="userForm.status">
                  <el-radio :value="1">启用</el-radio>
                  <el-radio :value="0">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="userDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="handleSaveUser">保存</el-button>
            </template>
          </el-dialog>
        </el-tab-pane>

        <el-tab-pane label="密码安全" name="password">
          <div class="password-layout">
            <div class="password-copy">
              <h3 class="section-title">修改密码</h3>
              <p class="section-subtitle">
                系统已支持连续失败锁定机制，建议定期更换密码，并避免与其他系统复用。
              </p>
            </div>

            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="110px" class="password-form">
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleChangePassword">提交修改</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="userStore.hasRole('admin')" label="操作审计" name="audit">
          <div class="audit-toolbar">
            <el-select v-model="auditFilters.module" clearable placeholder="按模块筛选" style="width: 160px">
              <el-option label="认证" value="AUTH" />
              <el-option label="用户" value="USERS" />
              <el-option label="患者" value="PATIENTS" />
              <el-option label="药品" value="MEDICINES" />
              <el-option label="处方" value="PRESCRIPTIONS" />
              <el-option label="挂号" value="REGISTRATIONS" />
              <el-option label="科室" value="DEPARTMENTS" />
              <el-option label="医生" value="DOCTORS" />
            </el-select>
            <el-select v-model="auditFilters.status" clearable placeholder="按结果筛选" style="width: 160px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-input v-model="auditFilters.keyword" clearable placeholder="操作人 / 摘要 / 路径" style="width: 260px" />
            <el-button type="primary" @click="loadOperationLogs">查询</el-button>
          </div>

          <div class="audit-tip">
            <strong>创新点：操作留痕中心</strong>
            <p>便于演示系统具备合规审计能力，也能支撑商用环境的责任追踪与问题回溯。</p>
          </div>

          <el-table :data="operationLogs" border stripe>
            <el-table-column prop="createTime" label="时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="action" label="动作" width="140" />
            <el-table-column prop="operatorName" label="操作人" width="120">
              <template #default="{ row }">{{ row.operatorName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="operatorRole" label="角色" width="120">
              <template #default="{ row }">{{ row.operatorRole ? roleText(row.operatorRole) : '-' }}</template>
            </el-table-column>
            <el-table-column prop="message" label="摘要" min-width="200" show-overflow-tooltip />
            <el-table-column prop="requestUri" label="接口" min-width="180" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP" width="140" />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listDoctors } from '../../api/doctor'
import { getSecurityOverview, listOperationLogs } from '../../api/system'
import { createUser, deleteUser, listUsers, updatePassword, updateUser } from '../../api/user'
import type { Doctor, OperationLog, SecurityOverview, SysUser } from '../../types'
import { useUserStore } from '../../stores/user'
import { formatDateTime } from '../../utils/format'

const userStore = useUserStore()
const activeTab = ref(userStore.hasRole('admin') ? 'users' : 'password')

const userList = ref<SysUser[]>([])
const userDialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref<FormInstance>()
const doctorOptions = ref<Doctor[]>([])

const securityOverview = reactive<SecurityOverview>({
  totalUsers: 0,
  activeUsers: 0,
  lockedUsers: 0,
  recentFailedLogins: 0,
  totalOperationLogs: 0,
})

const operationLogs = ref<OperationLog[]>([])
const auditFilters = reactive({
  module: '',
  status: undefined as number | undefined,
  keyword: '',
})

const userForm = reactive({
  id: 0,
  username: '',
  password: '',
  realName: '',
  role: 'admin',
  status: 1,
  doctorId: null as number | null,
})

const userRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function roleText(role: string) {
  const map: Record<string, string> = { admin: '系统管理员', doctor: '门诊医生', pharmacist: '药剂师' }
  return map[role] || role
}

function roleTagType(role: string) {
  const map: Record<string, 'danger' | 'primary' | 'success'> = { admin: 'danger', doctor: 'primary', pharmacist: 'success' }
  return map[role] || 'info'
}

function onRoleChange(role: string) {
  if (role !== 'doctor') {
    userForm.doctorId = null
  }
}

function openUserDialog(row?: SysUser) {
  isEdit.value = !!row
  if (row) {
    Object.assign(userForm, {
      id: row.id,
      username: row.username,
      password: '',
      realName: row.realName,
      role: row.role,
      status: row.status,
      doctorId: row.doctorId || null,
    })
  } else {
    Object.assign(userForm, {
      id: 0,
      username: '',
      password: '',
      realName: '',
      role: 'admin',
      status: 1,
      doctorId: null,
    })
  }
  userDialogVisible.value = true
}

async function handleSaveUser() {
  await userFormRef.value?.validate()
  if (isEdit.value) {
    await updateUser(userForm.id, { ...userForm } as SysUser)
    ElMessage.success('用户更新成功')
  } else {
    await createUser({ ...userForm } as SysUser)
    ElMessage.success('用户创建成功')
  }
  userDialogVisible.value = false
  await loadUsers()
  await loadSecurityCenter()
}

async function handleDeleteUser(row: SysUser) {
  if (row.username === 'admin') return
  await ElMessageBox.confirm(`确认删除用户“${row.realName}”吗？该操作会影响其登录。`, '删除用户', {
    type: 'warning',
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await loadUsers()
  await loadSecurityCenter()
}

async function handleChangePassword() {
  await pwdFormRef.value?.validate()
  const userId = userStore.currentUser?.id
  if (!userId) return
  await updatePassword(userId, { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  await userStore.logoutAndClear()
  window.location.href = '/login'
}

async function loadUsers() {
  const res = await listUsers()
  userList.value = res.data
}

async function loadDoctors() {
  const res = await listDoctors()
  doctorOptions.value = res.data
}

async function loadSecurityCenter() {
  if (!userStore.hasRole('admin')) return
  const overviewRes = await getSecurityOverview()
  Object.assign(securityOverview, overviewRes.data)
}

async function loadOperationLogs() {
  if (!userStore.hasRole('admin')) return
  const res = await listOperationLogs({
    module: auditFilters.module || undefined,
    status: auditFilters.status,
    keyword: auditFilters.keyword || undefined,
    limit: 100,
  })
  operationLogs.value = res.data
}

onMounted(async () => {
  await userStore.bootstrap(true)
  if (userStore.hasRole('admin')) {
    await Promise.all([loadUsers(), loadDoctors(), loadSecurityCenter(), loadOperationLogs()])
  }
})
</script>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.security-panel,
.account-panel,
.tab-shell {
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.security-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border: 1px solid var(--mc-border);
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.05);
}

.metric-card.warning {
  background: rgba(217, 119, 6, 0.06);
}

.metric-card.danger {
  background: rgba(194, 65, 12, 0.06);
}

.metric-card span {
  display: block;
  margin-bottom: 10px;
  color: var(--mc-text-soft);
}

.metric-card strong {
  font-size: 28px;
}

.account-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.account-summary__main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.account-avatar {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(15, 118, 110, 0.14);
  color: var(--mc-primary);
  font-size: 22px;
  font-weight: 700;
}

.account-summary__main strong {
  display: block;
  margin-bottom: 6px;
}

.account-summary__main p {
  margin: 0;
  color: var(--mc-text-soft);
}

.account-kv {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.account-kv > div {
  padding: 16px;
  border: 1px solid var(--mc-border);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.02);
}

.account-kv span {
  display: block;
  margin-bottom: 8px;
  color: var(--mc-text-soft);
  font-size: 13px;
}

.toolbar,
.audit-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.audit-tip {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(15, 118, 110, 0.2);
  background: rgba(15, 118, 110, 0.08);
}

.audit-tip p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.password-layout {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 24px;
  align-items: start;
}

.password-form {
  max-width: 520px;
}
</style>
