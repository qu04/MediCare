<template>
  <div class="page-shell">
    <section class="page-hero page-hero--analytics">
      <div class="chip">Dynamic Command Center</div>
      <h2 class="page-hero__title">门诊运营动态驾驶舱</h2>
      <p class="page-hero__subtitle">
        首页数字、折线图和饼图都支持手动编辑。你既可以保留真实业务数据，也可以为了演示或运营复盘临时调整展示值。
      </p>

      <div class="hero-actions">
        <el-button type="primary" @click="goalDialogVisible = true">编辑经营目标</el-button>
        <el-switch
          v-model="editableState.animationEnabled"
          active-text="动态图表"
          inactive-text="静态图表"
          @change="persistEditableState"
        />
        <el-button @click="loadDashboard">刷新真实数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.key" class="metric-panel glass-card">
        <div class="metric-panel__icon" :class="card.theme">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="metric-panel__body">
          <div class="metric-panel__topline">
            <span>{{ card.label }}</span>
            <el-tag v-if="card.isEdited" type="warning" effect="plain">手动修订</el-tag>
          </div>
          <strong>{{ card.displayValue }}<small v-if="card.suffix">{{ card.suffix }}</small></strong>
          <small>{{ card.helper }}</small>
        </div>
        <button type="button" class="metric-panel__edit" @click="openMetricEditor(card)">编辑</button>
      </article>
    </section>

    <section class="content-grid content-grid--charts">
      <article class="glass-card content-card">
        <div class="chart-head">
          <span class="section-title">病历趋势图</span>
          <el-button text type="primary" @click="openChartEditor('recordTrend')">编辑图表</el-button>
        </div>
        <SparkLineChart
          title="近7日病历生成走势"
          subtitle="修改数据后折线图会即时自动重绘"
          :points="recordTrend"
          :animated="editableState.animationEnabled"
        />
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <span class="section-title">诊断结构图</span>
          <el-button text type="primary" @click="openChartEditor('diagnosisDistribution')">编辑图表</el-button>
        </div>
        <DonutChart
          title="诊断主题占比"
          subtitle="支持直接修改每个诊断主题的数量"
          center-label="病历主题"
          :items="diagnosisDistribution"
          :animated="editableState.animationEnabled"
        />
      </article>
    </section>

    <section class="content-grid content-grid--charts">
      <article class="glass-card content-card">
        <div class="chart-head">
          <span class="section-title">挂号波动图</span>
          <el-button text type="primary" @click="openChartEditor('registrationTrend')">编辑图表</el-button>
        </div>
        <SparkLineChart
          title="近7日挂号波动"
          subtitle="可模拟节假日高峰或分流后的波动"
          :points="registrationTrend"
          :animated="editableState.animationEnabled"
        />
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <span class="section-title">处方状态图</span>
          <el-button text type="primary" @click="openChartEditor('prescriptionDistribution')">编辑图表</el-button>
        </div>
        <DonutChart
          title="处方状态结构"
          subtitle="收费、药房、医生三方协同状态一目了然"
          center-label="处方状态"
          :items="prescriptionDistribution"
          :animated="editableState.animationEnabled"
        />
      </article>
    </section>

    <section class="content-grid">
      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">经营目标与达成</h3>
            <p class="section-subtitle">支持编辑目标值，形成“真实值 + 目标值 + 演示值”三层运营视角</p>
          </div>
        </header>

        <div class="goal-grid">
          <div class="goal-item">
            <span>门诊接诊目标</span>
            <strong>{{ editableState.goals.outpatientTarget }} 人</strong>
            <small>当前完成 {{ stats.todayRegCount }} 人</small>
          </div>
          <div class="goal-item">
            <span>病历完整率目标</span>
            <strong>{{ editableState.goals.completionTarget }}%</strong>
            <small>当前 {{ recordCompletionRate }}%</small>
          </div>
          <div class="goal-item">
            <span>服务满意度目标</span>
            <strong>{{ editableState.goals.serviceSatisfaction }}%</strong>
            <small>用于演示运营目标追踪</small>
          </div>
          <div class="goal-item">
            <span>随访任务目标</span>
            <strong>{{ editableState.goals.followUpTarget }} 例</strong>
            <small>当前建议跟进 {{ followUpSuggestionCount }} 例</small>
          </div>
        </div>
      </article>

      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">本次动态升级点</h3>
            <p class="section-subtitle">不只是动一下，而是让你能控制数据本身</p>
          </div>
        </header>

        <div class="alert-list">
          <div v-for="(item, index) in innovationNotes" :key="item" class="alert-item">
            <div class="alert-dot" :class="index % 3 === 0 ? 'success' : index % 3 === 1 ? 'warning' : 'danger'"></div>
            <div>
              <strong>创新点 {{ index + 1 }}</strong>
              <p>{{ item }}</p>
            </div>
          </div>
        </div>
      </article>
    </section>

    <el-dialog v-model="goalDialogVisible" title="编辑经营目标" width="620px">
      <el-form label-width="120px">
        <el-form-item label="门诊接诊目标">
          <el-input-number v-model="editableState.goals.outpatientTarget" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="病历完整率目标">
          <el-input-number v-model="editableState.goals.completionTarget" :min="0" :max="100" :step="1" />
        </el-form-item>
        <el-form-item label="服务满意度目标">
          <el-input-number v-model="editableState.goals.serviceSatisfaction" :min="0" :max="100" :step="1" />
        </el-form-item>
        <el-form-item label="随访任务目标">
          <el-input-number v-model="editableState.goals.followUpTarget" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goalDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="persistEditableState">保存目标</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="metricDialogVisible" title="编辑首页指标" width="520px">
      <el-form label-width="110px">
        <el-form-item label="指标名称">
          <span>{{ activeMetric?.label }}</span>
        </el-form-item>
        <el-form-item label="当前真实值">
          <strong>{{ activeMetric?.actual ?? 0 }}{{ activeMetric?.suffix || '' }}</strong>
        </el-form-item>
        <el-form-item label="展示覆写值">
          <el-input-number v-model="metricOverrideDraft" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearMetricOverride">恢复真实值</el-button>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMetricOverride">保存展示值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="chartDialogVisible" :title="chartDialogTitle" width="680px">
      <div class="chart-edit-list">
        <div v-for="(item, index) in chartDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
          <el-input v-model="item.label" />
          <el-input-number v-model="item.value" :min="0" :step="1" />
        </div>
      </div>
      <template #footer>
        <el-button @click="resetChartOverride">恢复真实图表</el-button>
        <el-button @click="chartDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChartOverride">保存图表数据</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDepartments } from '../../api/department'
import { listDoctors } from '../../api/doctor'
import { listMedicalRecords } from '../../api/medical-record'
import { listLowStock } from '../../api/medicine'
import { listPrescriptions } from '../../api/prescription'
import { listRegistrations } from '../../api/registration'
import { getDashboardStats } from '../../api/user'
import DonutChart from '../../components/charts/DonutChart.vue'
import SparkLineChart from '../../components/charts/SparkLineChart.vue'
import {
  applyDistributionOverride,
  applyMetricOverrides,
  applyTrendOverride,
  buildDiagnosisDistribution,
  buildInnovationNotes,
  buildPrescriptionStatusDistribution,
  buildRegistrationTrend,
  buildSevenDayTrend,
  loadDashboardEditableState,
  saveDashboardEditableState,
  type DashboardEditableState,
  type DistributionPoint,
  type EditableMetric,
  type TrendPoint,
} from '../../utils/dashboard'
import type { DashboardStats, MedicalRecord, Medicine, Prescription, Registration } from '../../types'

type ChartKey = 'recordTrend' | 'registrationTrend' | 'diagnosisDistribution' | 'prescriptionDistribution'

const stats = ref<DashboardStats>({
  todayRegCount: 0,
  waitingCount: 0,
  stockAlertCount: 0,
  pendingPrescriptionCount: 0,
  completedRecordCount: 0,
  cancelledRegCount: 0,
})
const lowStockItems = ref<Medicine[]>([])
const records = ref<MedicalRecord[]>([])
const registrations = ref<Registration[]>([])
const prescriptions = ref<Prescription[]>([])

const editableState = reactive<DashboardEditableState>(loadDashboardEditableState())
const goalDialogVisible = ref(false)
const metricDialogVisible = ref(false)
const chartDialogVisible = ref(false)
const activeMetric = ref<EditableMetric | null>(null)
const metricOverrideDraft = ref(0)
const activeChartKey = ref<ChartKey>('recordTrend')
const chartDraft = ref<Array<TrendPoint | DistributionPoint>>([])

const recordCompletionRate = computed(() => {
  if (!records.value.length) return 0
  const completed = records.value.filter((item) => item.chiefComplaint && item.diagnosis && item.advice).length
  return Math.round((completed / records.value.length) * 100)
})
const followUpSuggestionCount = computed(() =>
  records.value.filter((item) => item.diagnosis?.includes('复诊') || item.advice?.includes('复诊')).length,
)

const metricSource = computed<EditableMetric[]>(() => [
  { key: 'todayRegCount', label: '今日挂号量', actual: stats.value.todayRegCount, helper: '支持改成演示值或阶段目标值', icon: 'Tickets', theme: 'primary' },
  { key: 'waitingCount', label: '候诊人数', actual: stats.value.waitingCount, helper: '可模拟高峰时段压力场景', icon: 'Timer', theme: 'warning' },
  { key: 'recordCompletionRate', label: '病历完整率', actual: recordCompletionRate.value, helper: '会随病例编辑自动变化，也允许人工覆写', icon: 'DataAnalysis', theme: 'success', suffix: '%' },
  { key: 'stockAlertCount', label: '库存预警', actual: stats.value.stockAlertCount, helper: '可按演示需要调成更贴近运营的数据', icon: 'WarningFilled', theme: 'danger' },
])

const rawRecordTrend = computed(() => buildSevenDayTrend(records.value))
const rawRegistrationTrend = computed(() => buildRegistrationTrend(registrations.value))
const rawDiagnosisDistribution = computed(() => buildDiagnosisDistribution(records.value))
const rawPrescriptionDistribution = computed(() => buildPrescriptionStatusDistribution(prescriptions.value))

const metricCards = computed(() => applyMetricOverrides(metricSource.value, editableState.overrides))
const recordTrend = computed(() => applyTrendOverride(rawRecordTrend.value, editableState.chartOverrides.recordTrend))
const registrationTrend = computed(() => applyTrendOverride(rawRegistrationTrend.value, editableState.chartOverrides.registrationTrend))
const diagnosisDistribution = computed(() =>
  applyDistributionOverride(rawDiagnosisDistribution.value, editableState.chartOverrides.diagnosisDistribution),
)
const prescriptionDistribution = computed(() =>
  applyDistributionOverride(rawPrescriptionDistribution.value, editableState.chartOverrides.prescriptionDistribution),
)
const innovationNotes = computed(() => buildInnovationNotes(stats.value, records.value, editableState))
const chartDialogTitle = computed(() => {
  const labels: Record<ChartKey, string> = {
    recordTrend: '编辑病历趋势图',
    registrationTrend: '编辑挂号波动图',
    diagnosisDistribution: '编辑诊断结构图',
    prescriptionDistribution: '编辑处方状态图',
  }
  return labels[activeChartKey.value]
})

async function loadDashboard() {
  try {
    const [statsRes, stockRes, recordsRes, registrationRes, prescriptionRes] = await Promise.all([
      getDashboardStats(),
      listLowStock(),
      listMedicalRecords(),
      listRegistrations(),
      listPrescriptions(undefined, false),
      listDepartments(),
      listDoctors(),
    ])
    stats.value = statsRes.data
    lowStockItems.value = stockRes.data.slice(0, 5)
    records.value = recordsRes.data
    registrations.value = registrationRes.data
    prescriptions.value = prescriptionRes.data
  } catch {
    ElMessage.warning('动态数据加载未完成，已保留当前内容。')
  }
}

function persistEditableState() {
  saveDashboardEditableState(editableState)
  goalDialogVisible.value = false
  ElMessage.success('配置已保存')
}

function openMetricEditor(metric: EditableMetric & { displayValue?: number }) {
  activeMetric.value = metric
  metricOverrideDraft.value = editableState.overrides[metric.key] ?? metric.actual
  metricDialogVisible.value = true
}

function saveMetricOverride() {
  if (!activeMetric.value) return
  editableState.overrides[activeMetric.value.key] = metricOverrideDraft.value
  saveDashboardEditableState(editableState)
  metricDialogVisible.value = false
  ElMessage.success('首页指标已更新')
}

function clearMetricOverride() {
  if (!activeMetric.value) return
  delete editableState.overrides[activeMetric.value.key]
  saveDashboardEditableState(editableState)
  metricDialogVisible.value = false
  ElMessage.success('已恢复真实值')
}

function openChartEditor(key: ChartKey) {
  activeChartKey.value = key
  const sourceMap: Record<ChartKey, Array<TrendPoint | DistributionPoint>> = {
    recordTrend: recordTrend.value,
    registrationTrend: registrationTrend.value,
    diagnosisDistribution: diagnosisDistribution.value,
    prescriptionDistribution: prescriptionDistribution.value,
  }
  chartDraft.value = sourceMap[key].map((item) => ({ ...item }))
  chartDialogVisible.value = true
}

function saveChartOverride() {
  if (activeChartKey.value === 'recordTrend' || activeChartKey.value === 'registrationTrend') {
    editableState.chartOverrides[activeChartKey.value] = chartDraft.value.map((item) => ({
      label: item.label,
      value: Number(item.value) || 0,
    }))
  } else {
    editableState.chartOverrides[activeChartKey.value] = chartDraft.value.map((item) => ({
      label: item.label,
      value: Number(item.value) || 0,
    }))
  }
  saveDashboardEditableState(editableState)
  chartDialogVisible.value = false
  ElMessage.success('图表数据已更新')
}

function resetChartOverride() {
  delete editableState.chartOverrides[activeChartKey.value]
  saveDashboardEditableState(editableState)
  chartDialogVisible.value = false
  ElMessage.success('图表已恢复真实数据')
}

onMounted(loadDashboard)
</script>

<style scoped>
.page-hero--analytics {
  position: relative;
  overflow: hidden;
}

.page-hero--analytics::after {
  content: '';
  position: absolute;
  inset: auto -120px -120px auto;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(15, 118, 110, 0.2), transparent 68%);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
  align-items: center;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-panel {
  position: relative;
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 16px;
  align-items: center;
  padding: 22px;
}

.metric-panel__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  font-size: 24px;
}

.metric-panel__icon.primary { background: rgba(15, 118, 110, 0.14); color: var(--mc-primary); }
.metric-panel__icon.warning { background: rgba(217, 119, 6, 0.14); color: var(--mc-accent); }
.metric-panel__icon.danger { background: rgba(194, 65, 12, 0.14); color: var(--mc-danger); }
.metric-panel__icon.success { background: rgba(21, 128, 61, 0.14); color: var(--mc-success); }

.metric-panel__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-panel__topline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-panel__body span,
.metric-panel__body small {
  color: var(--mc-text-soft);
}

.metric-panel__body strong {
  font-size: 30px;
  line-height: 1.1;
}

.metric-panel__body strong small {
  margin-left: 4px;
  font-size: 18px;
}

.metric-panel__edit {
  position: absolute;
  top: 14px;
  right: 14px;
  border: 0;
  background: rgba(15, 118, 110, 0.1);
  color: var(--mc-primary);
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.content-card {
  padding: 24px;
}

.chart-head,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.goal-grid,
.alert-list,
.chart-edit-list {
  display: grid;
  gap: 14px;
}

.goal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.goal-item {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--mc-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(241, 245, 249, 0.92));
}

.goal-item span,
.goal-item small {
  color: var(--mc-text-soft);
}

.goal-item strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 28px;
}

.alert-item {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 14px;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid var(--mc-border);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.02);
}

.alert-item strong {
  display: block;
  margin-bottom: 6px;
}

.alert-item p {
  margin: 0;
  color: var(--mc-text-soft);
  line-height: 1.7;
}

.alert-dot {
  width: 12px;
  height: 12px;
  margin-top: 4px;
  border-radius: 999px;
}

.alert-dot.warning { background: var(--mc-accent); }
.alert-dot.danger { background: var(--mc-danger); }
.alert-dot.success { background: var(--mc-success); }

.chart-edit-row {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}

@media (max-width: 1200px) {
  .metric-grid,
  .goal-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
