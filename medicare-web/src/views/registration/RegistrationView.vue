<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Smart Registration</div>
      <h2 class="page-hero__title">挂号预约与分诊指挥台</h2>
      <p class="page-hero__subtitle">
        排班、号源、挂号记录和演示数据统一在一个界面里处理。你可以新建排班、编辑排班、创建真实挂号，也可以导入演示挂号来测试完整流程。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>今日排班</span>
        <strong>{{ schedList.length }}</strong>
        <small>当前日期下的可见班次</small>
      </article>
      <article class="glass-card metric-card">
        <span>今日挂号</span>
        <strong>{{ visibleRegs.length }}</strong>
        <small>真实数据与演示数据合并统计</small>
      </article>
      <article class="glass-card metric-card">
        <span>紧张号源</span>
        <strong>{{ tightSchedules.length }}</strong>
        <small>余号低于 35% 的班次</small>
      </article>
    </section>

    <section class="content-grid content-grid--analytics">
      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">号源负荷折线</h3>
            <p class="section-subtitle">展示各时段已占用号源，也可手动改成演示数据</p>
          </div>
        </div>

        <SparkLineChart
          title="班次号源负荷"
          subtitle="按时段观察号源使用情况"
          :points="scheduleLoadTrend"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>号源折线编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetScheduleTrendDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveScheduleTrendDraft">应用到折线图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in scheduleTrendDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
              <el-input v-model="item.label" />
              <el-input-number v-model="item.value" :min="0" :step="1" />
            </div>
          </div>
        </div>
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">挂号状态结构</h3>
            <p class="section-subtitle">等待、就诊中、完成和取消状态可直接编辑</p>
          </div>
        </div>

        <DonutChart
          title="挂号状态分布"
          subtitle="用于演示分诊压力和流转效率"
          center-label="挂号状态"
          :items="registrationStatusDistribution"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>状态结构编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetRegistrationDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveRegistrationDraft">应用到结构图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in registrationDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
              <el-input v-model="item.label" />
              <el-input-number v-model="item.value" :min="0" :step="1" />
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">排班与号源管理</h3>
            <p class="section-subtitle">支持真实排班新建、编辑和删除</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="openScheduleDialog()">新建排班</el-button>
            <el-button :disabled="!selectedSchedule" @click="openScheduleDialog(selectedSchedule)">编辑排班</el-button>
            <el-button type="danger" :disabled="!selectedSchedule" @click="handleDeleteSchedule">删除排班</el-button>
          </div>
        </header>

        <div class="toolbar">
          <el-date-picker v-model="queryDate" type="date" value-format="YYYY-MM-DD" @change="reloadAll" />
          <el-select v-model="queryDeptId" clearable placeholder="按科室筛选" @change="loadSchedules">
            <el-option v-for="item in deptList" :key="item.id" :label="item.name" :value="item.id!" />
          </el-select>
        </div>

        <div class="insight-banner">
          <div>
            <strong>创新点：排班创建和号源热度联动</strong>
            <p>你新增或修改排班后，上方号源趋势和紧张号源统计会立即刷新。</p>
          </div>
          <el-tag type="warning" effect="dark">{{ tightSchedules.length }} 个班次需关注</el-tag>
        </div>

        <el-table :data="schedList" border stripe highlight-current-row @row-click="handleSelectSchedule">
          <el-table-column prop="doctorName" label="医生" min-width="120" />
          <el-table-column prop="departmentName" label="科室" min-width="120" />
          <el-table-column prop="timeSlot" label="时段" width="110" />
          <el-table-column prop="totalSlots" label="总号源" width="100" />
          <el-table-column prop="remainSlots" label="剩余号源" width="110">
            <template #default="{ row }">
              <el-tag :type="scheduleTagType(row as Schedule)">{{ row.remainSlots }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="热度" width="110">
            <template #default="{ row }">{{ scheduleHeatText(row as Schedule) }}</template>
          </el-table-column>
          <el-table-column label="建议动作" min-width="180">
            <template #default="{ row }">{{ scheduleAdvice(row as Schedule) }}</template>
          </el-table-column>
        </el-table>
      </article>

      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">挂号记录与演示导入</h3>
            <p class="section-subtitle">真实挂号继续可用，空数据时可导入演示挂号</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" :disabled="!selectedSchedule" @click="openRegDialog">真实挂号</el-button>
            <el-button :disabled="!selectedSchedule" @click="openMockRegDialog">导入演示挂号</el-button>
          </div>
        </header>

        <div v-if="selectedSchedule" class="selected-summary">
          <strong>{{ selectedSchedule.doctorName }} / {{ selectedSchedule.departmentName }}</strong>
          <span class="muted">{{ selectedSchedule.workDate }} {{ selectedSchedule.timeSlot }}，余号 {{ selectedSchedule.remainSlots }}</span>
        </div>

        <el-table :data="visibleRegs" border stripe>
          <el-table-column prop="seqNo" label="序号" width="80" />
          <el-table-column prop="patientName" label="患者" min-width="120" />
          <el-table-column prop="doctorName" label="医生" min-width="120" />
          <el-table-column prop="departmentName" label="科室" min-width="120" />
          <el-table-column prop="timeSlot" label="时段" width="110" />
          <el-table-column label="来源" width="90">
            <template #default="{ row }">
              <el-tag :type="isMockRegistration(row as Registration | MockRegistration) ? 'info' : 'success'">
                {{ isMockRegistration(row as Registration | MockRegistration) ? '演示' : '真实' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag((row as Registration).status)">{{ registrationStatusText((row as Registration).status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190">
            <template #default="{ row }">
              <el-button
                v-if="isMockRegistration(row as Registration | MockRegistration)"
                text
                type="primary"
                @click="cycleMockStatus((row as MockRegistration).id!)"
              >
                切换状态
              </el-button>
              <el-button
                v-if="isMockRegistration(row as Registration | MockRegistration)"
                text
                type="danger"
                @click="removeMockRegistration((row as MockRegistration).id!)"
              >
                删除
              </el-button>
              <el-button
                v-if="!isMockRegistration(row as Registration | MockRegistration) && (row as Registration).status === 0"
                text
                type="danger"
                @click="handleCancel((row as Registration).id!)"
              >
                取消
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>

    <el-dialog v-model="scheduleDialogVisible" :title="scheduleForm.id ? '编辑排班' : '新建排班'" width="680px">
      <el-form label-width="110px">
        <el-form-item label="医生">
          <el-select v-model="scheduleForm.doctorId" placeholder="选择医生" @change="syncScheduleDoctor">
            <el-option v-for="item in doctorList" :key="item.id" :label="`${item.name} / ${item.departmentName || '未分配科室'}`" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="scheduleForm.workDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="scheduleForm.timeSlot">
            <el-option label="上午" value="上午" />
            <el-option label="下午" value="下午" />
            <el-option label="夜间" value="夜间" />
          </el-select>
        </el-form-item>
        <el-form-item label="总号源">
          <el-input-number v-model="scheduleForm.totalSlots" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="剩余号源">
          <el-input-number v-model="scheduleForm.remainSlots" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存排班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="regDialogVisible" title="选择患者并完成真实挂号" width="780px" destroy-on-close>
      <div class="dialog-toolbar">
        <el-input v-model="patientKw" placeholder="搜索患者姓名、身份证或手机号" clearable @keyup.enter="handleSearchPatients">
          <template #append>
            <el-button @click="handleSearchPatients">查询</el-button>
          </template>
        </el-input>
      </div>
      <el-table :data="patientList" border stripe highlight-current-row @row-click="handleSelectPatient">
        <el-table-column prop="name" label="患者姓名" min-width="120" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="allergyInfo" label="过敏史" min-width="200">
          <template #default="{ row }">{{ row.allergyInfo || '无' }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="regDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedPatient || !selectedSchedule" @click="handleRegister">
          确认挂号
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mockRegDialogVisible" title="导入演示挂号记录" width="720px">
      <el-form label-width="108px">
        <el-form-item label="患者姓名">
          <el-input v-model="mockRegForm.patientName" placeholder="例如：演示患者 A" />
        </el-form-item>
        <el-form-item label="序号">
          <el-input-number v-model="mockRegForm.seqNo" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="mockRegForm.status">
            <el-option label="待诊" :value="0" />
            <el-option label="就诊中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mockRegDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMockRegistration">导入挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listDepartments } from '../../api/department'
import { listDoctors } from '../../api/doctor'
import { listPatients, searchPatients } from '../../api/patient'
import { cancelRegistration, listRegistrations, register } from '../../api/registration'
import { createSchedule, deleteSchedule, getAvailableSchedules, updateSchedule } from '../../api/schedule'
import DonutChart from '../../components/charts/DonutChart.vue'
import SparkLineChart from '../../components/charts/SparkLineChart.vue'
import { applyDistributionOverride, applyTrendOverride, type DistributionPoint, type TrendPoint } from '../../utils/dashboard'
import type { Department, Doctor, Patient, Registration, Schedule } from '../../types'
import { registrationStatusText, schedulePressure } from '../../utils/insights'

type MockRegistration = Registration & { isMock: true }

interface RegistrationPageState {
  chartOverrides: {
    scheduleLoadTrend?: TrendPoint[]
    registrationDistribution?: DistributionPoint[]
  }
  mockRegistrations: MockRegistration[]
}

const STORAGE_KEY = 'medicare-registration-page-state'

const deptList = ref<Department[]>([])
const doctorList = ref<Doctor[]>([])
const schedList = ref<Schedule[]>([])
const regList = ref<Registration[]>([])
const patientList = ref<Patient[]>([])
const pageState = ref<RegistrationPageState>(loadPageState())

const queryDate = ref(new Date().toISOString().slice(0, 10))
const queryDeptId = ref<number>()
const selectedSchedule = ref<Schedule | null>(null)
const regDialogVisible = ref(false)
const mockRegDialogVisible = ref(false)
const scheduleDialogVisible = ref(false)
const patientKw = ref('')
const selectedPatient = ref<Patient | null>(null)

const scheduleTrendDraft = ref<TrendPoint[]>([])
const registrationDraft = ref<DistributionPoint[]>([])

const scheduleForm = reactive<Partial<Schedule>>({
  id: undefined,
  doctorId: undefined,
  workDate: queryDate.value,
  timeSlot: '上午',
  totalSlots: 30,
  remainSlots: 30,
})

const mockRegForm = reactive({
  patientName: '',
  seqNo: 1,
  status: 0,
})

const tightSchedules = computed(() => schedList.value.filter((item) => schedulePressure(item) !== 'healthy'))
const visibleRegs = computed(() => {
  const selectedId = selectedSchedule.value?.id
  const merged = [...regList.value, ...pageState.value.mockRegistrations]
  if (!selectedId) return merged
  return merged.filter((item) => item.scheduleId === selectedId)
})

const rawScheduleLoadTrend = computed<TrendPoint[]>(() =>
  (schedList.value.length ? schedList.value : [{ timeSlot: '暂无', totalSlots: 0, remainSlots: 0 } as Schedule]).map((item) => ({
    label: item.timeSlot || '时段',
    value: Math.max(0, (item.totalSlots || 0) - (item.remainSlots || 0)),
  })),
)

const rawRegistrationDistribution = computed<DistributionPoint[]>(() => {
  const counter = new Map<string, number>([
    ['待诊', 0],
    ['就诊中', 0],
    ['已完成', 0],
    ['已取消', 0],
  ])
  for (const item of visibleRegs.value) {
    counter.set(registrationStatusText(item.status), (counter.get(registrationStatusText(item.status)) || 0) + 1)
  }
  return Array.from(counter.entries()).map(([label, value]) => ({ label, value }))
})

const scheduleLoadTrend = computed(() =>
  applyTrendOverride(rawScheduleLoadTrend.value, pageState.value.chartOverrides.scheduleLoadTrend),
)
const registrationStatusDistribution = computed(() =>
  applyDistributionOverride(rawRegistrationDistribution.value, pageState.value.chartOverrides.registrationDistribution),
)

function loadPageState(): RegistrationPageState {
  if (typeof window === 'undefined') return { chartOverrides: {}, mockRegistrations: [] }
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return { chartOverrides: {}, mockRegistrations: [] }
  try {
    const parsed = JSON.parse(raw) as RegistrationPageState
    return {
      chartOverrides: parsed.chartOverrides || {},
      mockRegistrations: parsed.mockRegistrations || [],
    }
  } catch {
    return { chartOverrides: {}, mockRegistrations: [] }
  }
}

function persistPageState() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(pageState.value))
}

function syncDrafts() {
  scheduleTrendDraft.value = scheduleLoadTrend.value.map((item) => ({ ...item }))
  registrationDraft.value = registrationStatusDistribution.value.map((item) => ({ ...item }))
}

function isMockRegistration(row: Registration | MockRegistration) {
  return (row as MockRegistration).isMock === true
}

function scheduleTagType(row: Schedule) {
  const pressure = schedulePressure(row)
  if (pressure === 'critical') return 'danger'
  if (pressure === 'busy') return 'warning'
  return 'success'
}

function scheduleHeatText(row: Schedule) {
  const pressure = schedulePressure(row)
  if (pressure === 'critical') return '临界'
  if (pressure === 'busy') return '紧张'
  return '充足'
}

function scheduleAdvice(row: Schedule) {
  const pressure = schedulePressure(row)
  if (pressure === 'critical') return '优先引导其他时段或增补号源'
  if (pressure === 'busy') return '建议前台关注排队速度'
  return '可继续推荐该班次'
}

function statusTag(status: number) {
  if (status === 0) return 'warning'
  if (status === 1) return 'primary'
  if (status === 2) return 'success'
  return 'info'
}

async function loadSchedules() {
  const res = await getAvailableSchedules(queryDate.value, queryDeptId.value)
  schedList.value = res.data
  if (selectedSchedule.value) {
    selectedSchedule.value = schedList.value.find((item) => item.id === selectedSchedule.value?.id) || null
  }
}

async function loadRegs() {
  const res = await listRegistrations(queryDate.value)
  regList.value = res.data
}

async function reloadAll() {
  await Promise.all([loadSchedules(), loadRegs()])
}

function handleSelectSchedule(row: Schedule) {
  selectedSchedule.value = row
}

function openScheduleDialog(row?: Schedule | null) {
  if (row) {
    Object.assign(scheduleForm, { ...row })
  } else {
    Object.assign(scheduleForm, {
      id: undefined,
      doctorId: doctorList.value[0]?.id,
      workDate: queryDate.value,
      timeSlot: '上午',
      totalSlots: 30,
      remainSlots: 30,
    })
  }
  scheduleDialogVisible.value = true
}

function syncScheduleDoctor() {
  const doctor = doctorList.value.find((item) => item.id === scheduleForm.doctorId)
  if (doctor?.departmentId) {
    queryDeptId.value = doctor.departmentId
  }
}

async function saveSchedule() {
  if (!scheduleForm.doctorId || !scheduleForm.workDate || !scheduleForm.timeSlot) {
    ElMessage.warning('请先补齐排班信息')
    return
  }
  const doctor = doctorList.value.find((item) => item.id === scheduleForm.doctorId)
  const payload = {
    doctorId: scheduleForm.doctorId,
    workDate: scheduleForm.workDate,
    timeSlot: scheduleForm.timeSlot,
    totalSlots: scheduleForm.totalSlots || 0,
    remainSlots: scheduleForm.remainSlots || 0,
    doctorName: doctor?.name,
    departmentName: doctor?.departmentName,
  } as Schedule
  if (scheduleForm.id) {
    await updateSchedule(scheduleForm.id, { ...payload, id: scheduleForm.id })
    ElMessage.success('排班已更新')
  } else {
    await createSchedule(payload)
    ElMessage.success('排班已创建')
  }
  scheduleDialogVisible.value = false
  await reloadAll()
}

async function handleDeleteSchedule() {
  if (!selectedSchedule.value?.id) return
  await deleteSchedule(selectedSchedule.value.id)
  ElMessage.success('排班已删除')
  selectedSchedule.value = null
  await reloadAll()
}

async function openRegDialog() {
  selectedPatient.value = null
  patientKw.value = ''
  regDialogVisible.value = true
  const res = await listPatients()
  patientList.value = res.data
}

async function handleSearchPatients() {
  if (!patientKw.value.trim()) {
    const res = await listPatients()
    patientList.value = res.data
    return
  }
  const res = await searchPatients(patientKw.value.trim())
  patientList.value = res.data
}

function handleSelectPatient(row: Patient) {
  selectedPatient.value = row
}

async function handleRegister() {
  if (!selectedSchedule.value || !selectedPatient.value) return
  await register({ patientId: selectedPatient.value.id!, scheduleId: selectedSchedule.value.id! })
  ElMessage.success('挂号成功')
  regDialogVisible.value = false
  await reloadAll()
}

async function handleCancel(id: number) {
  await cancelRegistration(id)
  ElMessage.success('挂号已取消')
  await reloadAll()
}

function openMockRegDialog() {
  mockRegForm.patientName = ''
  mockRegForm.seqNo = visibleRegs.value.length + 1
  mockRegForm.status = 0
  mockRegDialogVisible.value = true
}

function saveMockRegistration() {
  if (!selectedSchedule.value) return
  const mockId = Date.now()
  pageState.value.mockRegistrations.push({
    id: mockId,
    patientId: mockId,
    scheduleId: selectedSchedule.value.id!,
    doctorId: selectedSchedule.value.doctorId,
    regTime: new Date().toISOString(),
    status: mockRegForm.status,
    seqNo: mockRegForm.seqNo,
    fee: 0,
    patientName: mockRegForm.patientName || `演示患者 ${pageState.value.mockRegistrations.length + 1}`,
    doctorName: selectedSchedule.value.doctorName,
    departmentName: selectedSchedule.value.departmentName,
    timeSlot: selectedSchedule.value.timeSlot,
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    isMock: true,
  })
  persistPageState()
  mockRegDialogVisible.value = false
  syncDrafts()
  ElMessage.success('演示挂号已导入')
}

function cycleMockStatus(id: number) {
  const target = pageState.value.mockRegistrations.find((item) => item.id === id)
  if (!target) return
  target.status = target.status >= 3 ? 0 : target.status + 1
  persistPageState()
  syncDrafts()
  ElMessage.success('演示挂号状态已更新')
}

function removeMockRegistration(id: number) {
  pageState.value.mockRegistrations = pageState.value.mockRegistrations.filter((item) => item.id !== id)
  persistPageState()
  syncDrafts()
  ElMessage.success('演示挂号已删除')
}

function saveScheduleTrendDraft() {
  pageState.value.chartOverrides.scheduleLoadTrend = scheduleTrendDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistPageState()
  ElMessage.success('号源折线图已更新')
}

function resetScheduleTrendDraft() {
  delete pageState.value.chartOverrides.scheduleLoadTrend
  persistPageState()
  syncDrafts()
  ElMessage.success('号源折线图已恢复真实数据')
}

function saveRegistrationDraft() {
  pageState.value.chartOverrides.registrationDistribution = registrationDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistPageState()
  ElMessage.success('挂号状态结构图已更新')
}

function resetRegistrationDraft() {
  delete pageState.value.chartOverrides.registrationDistribution
  persistPageState()
  syncDrafts()
  ElMessage.success('挂号状态结构图已恢复真实数据')
}

watch([scheduleLoadTrend, registrationStatusDistribution], syncDrafts, { immediate: true })

onMounted(async () => {
  const [deptRes, doctorRes] = await Promise.all([listDepartments(), listDoctors()])
  deptList.value = deptRes.data
  doctorList.value = doctorRes.data
  await reloadAll()
})
</script>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.metric-card,
.content-card {
  padding: 22px 24px;
}

.metric-card span,
.metric-card small {
  color: var(--mc-text-soft);
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 30px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.content-grid--analytics {
  grid-template-columns: 1.05fr 0.95fr;
}

.card-header,
.chart-head,
.editor-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.header-actions,
.editor-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar,
.dialog-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar :deep(.el-select),
.toolbar :deep(.el-date-editor),
.dialog-toolbar :deep(.el-input) {
  width: 220px;
}

.insight-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(217, 119, 6, 0.2);
  border-radius: 16px;
  background: rgba(217, 119, 6, 0.08);
}

.insight-banner p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.selected-summary,
.editor-panel {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(15, 118, 110, 0.08);
  margin-bottom: 16px;
}

.selected-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chart-edit-list {
  display: grid;
  gap: 12px;
}

.chart-edit-row {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}

@media (max-width: 1200px) {
  .metrics-grid,
  .content-grid,
  .content-grid--analytics {
    grid-template-columns: 1fr;
  }
}
</style>
