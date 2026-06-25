<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Prescription Flow</div>
      <h2 class="page-hero__title">处方流转与用药风控中心</h2>
      <p class="page-hero__subtitle">
        处方页面补上了动态图表、风控结构和演示模板。现在不仅能开处方，也能直观看到处方状态和风险变化。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>已完成就诊</span>
        <strong>{{ completedRegs.length }}</strong>
        <small>可继续开立处方的患者</small>
      </article>
      <article class="glass-card metric-card">
        <span>当前处方项</span>
        <strong>{{ activeItems.length }}</strong>
        <small>当前编辑或已存在处方中的药品数</small>
      </article>
      <article class="glass-card metric-card">
        <span>风控提醒</span>
        <strong>{{ warnings.length }}</strong>
        <small>重复用药、过敏史或高剂量提醒</small>
      </article>
    </section>

    <section class="content-grid content-grid--analytics">
      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">处方状态折线</h3>
            <p class="section-subtitle">待缴费、已缴费、已发药、已作废状态可直接改</p>
          </div>
        </div>

        <SparkLineChart
          title="处方状态趋势"
          subtitle="用于展示处方流转效率"
          :points="prescriptionStatusTrend"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>状态折线编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetStatusDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveStatusDraft">应用到折线图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in statusDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
              <el-input v-model="item.label" />
              <el-input-number v-model="item.value" :min="0" :step="1" />
            </div>
          </div>
        </div>
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">用药风险结构</h3>
            <p class="section-subtitle">普通、提示、高风险分布可直接编辑</p>
          </div>
        </div>

        <DonutChart
          title="处方风险分层"
          subtitle="用于展示处方审核压力"
          center-label="风险层级"
          :items="riskDistribution"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>风险结构编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetRiskDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveRiskDraft">应用到结构图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in riskDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
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
            <h3 class="section-title">待开方患者</h3>
            <p class="section-subtitle">从已完成就诊患者中快速进入处方编辑</p>
          </div>
        </header>

        <el-table :data="completedRegs" border stripe highlight-current-row @row-click="handleSelectPatient">
          <el-table-column prop="seqNo" label="序号" width="80" />
          <el-table-column prop="patientName" label="患者姓名" min-width="120" />
          <el-table-column prop="doctorName" label="医生" min-width="120" />
          <el-table-column prop="departmentName" label="科室" min-width="120" />
        </el-table>
      </article>

      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">处方编辑与审方</h3>
            <p class="section-subtitle">支持用药录入、风控提示、演示模板和发药流转</p>
          </div>
          <el-tag v-if="existingPrescription" :type="prescStatusType(existingPrescription.status)">
            {{ prescStatusText(existingPrescription.status) }}
          </el-tag>
        </header>

        <div v-if="selectedReg" class="selected-summary">
          <strong>{{ selectedReg.patientName }} / {{ selectedReg.doctorName }}</strong>
          <span class="muted">患者 ID：{{ selectedReg.patientId }}，挂号序号：{{ selectedReg.seqNo }}</span>
        </div>

        <template v-if="selectedReg && !existingPrescription">
          <div class="toolbar">
            <el-input v-model="medKw" placeholder="搜索药品名称" clearable @keyup.enter="searchMeds" />
            <el-select v-model="selectedMedId" placeholder="选择药品">
              <el-option
                v-for="item in medResults"
                :key="item.id"
                :label="`${item.name} ${item.spec || ''} ${formatCurrency(item.price)}`"
                :value="item.id!"
              />
            </el-select>
            <el-input-number v-model="itemQty" :min="1" :max="99" />
            <el-input v-model="itemUsage" placeholder="例如：每日 2 次，每次 1 片" />
            <el-button type="primary" @click="addItem">添加</el-button>
          </div>

          <div class="template-bar">
            <el-button @click="loadDemoTemplate('respiratory')">呼吸道模板</el-button>
            <el-button @click="loadDemoTemplate('hypertension')">高血压模板</el-button>
            <el-button @click="loadDemoTemplate('digestive')">消化系统模板</el-button>
          </div>

          <div class="warning-panel" v-if="warnings.length">
            <strong>创新点：审方风控提示</strong>
            <ul>
              <li v-for="item in warnings" :key="item">{{ item }}</li>
            </ul>
          </div>

          <el-table :data="items" border stripe>
            <el-table-column prop="medicineName" label="药品名称" min-width="160" />
            <el-table-column prop="medicineSpec" label="规格" min-width="120" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ formatCurrency(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="usageDesc" label="用法用量" min-width="180" />
            <el-table-column label="操作" width="100">
              <template #default="{ $index }">
                <el-button text type="danger" @click="items.splice($index, 1)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="footer-bar">
            <strong>处方总金额：{{ formatCurrency(totalAmount) }}</strong>
            <el-button type="primary" :disabled="items.length === 0" @click="savePrescription">保存处方</el-button>
          </div>
        </template>

        <template v-else-if="existingPrescription">
          <div class="warning-panel" v-if="warnings.length">
            <strong>创新点：审方风控提示</strong>
            <ul>
              <li v-for="item in warnings" :key="item">{{ item }}</li>
            </ul>
          </div>

          <el-table :data="existingPrescription.items || []" border stripe>
            <el-table-column prop="medicineName" label="药品名称" min-width="160" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ formatCurrency(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column prop="usageDesc" label="用法用量" min-width="180" />
          </el-table>

          <div class="footer-bar">
            <strong>处方总金额：{{ formatCurrency(existingPrescription.totalAmount) }}</strong>
            <div class="action-group">
              <el-button v-if="existingPrescription.status === 0" type="success" @click="handleDispense">确认发药</el-button>
              <el-button v-if="existingPrescription.status === 0" type="danger" @click="handleCancel">作废处方</el-button>
            </div>
          </div>
        </template>

        <el-empty v-else description="请先选择一位已完成就诊的患者" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listMedicalRecords } from '../../api/medical-record'
import { listMedicines } from '../../api/medicine'
import { listPatients } from '../../api/patient'
import { cancelPrescription, createPrescription, dispensePrescription, getByRecord, listPrescriptions } from '../../api/prescription'
import { listRegistrations } from '../../api/registration'
import DonutChart from '../../components/charts/DonutChart.vue'
import SparkLineChart from '../../components/charts/SparkLineChart.vue'
import { applyDistributionOverride, applyTrendOverride, type DistributionPoint, type TrendPoint } from '../../utils/dashboard'
import type { Medicine, Patient, Prescription, PrescriptionItem, Registration } from '../../types'
import { formatCurrency } from '../../utils/format'
import { prescriptionWarnings } from '../../utils/insights'

interface PrescriptionPageState {
  chartOverrides: {
    statusTrend?: TrendPoint[]
    riskDistribution?: DistributionPoint[]
  }
}

const STORAGE_KEY = 'medicare-prescription-page-state'

const completedRegs = ref<Registration[]>([])
const selectedReg = ref<Registration | null>(null)
const existingPrescription = ref<Prescription | null>(null)
const selectedPatient = ref<Patient | null>(null)
const allPrescriptions = ref<Prescription[]>([])

const medKw = ref('')
const medResults = ref<Medicine[]>([])
const selectedMedId = ref<number>()
const itemQty = ref(1)
const itemUsage = ref('遵医嘱')
const items = ref<PrescriptionItem[]>([])
const pageState = ref<PrescriptionPageState>(loadPageState())
const statusDraft = ref<TrendPoint[]>([])
const riskDraft = ref<DistributionPoint[]>([])

const activeItems = computed(() => existingPrescription.value?.items || items.value)
const totalAmount = computed(() => items.value.reduce((sum, item) => sum + (item.amount || 0), 0))
const warnings = computed(() => prescriptionWarnings(activeItems.value, selectedPatient.value))

const rawStatusTrend = computed<TrendPoint[]>(() => {
  const counter = [0, 0, 0, 0]
  for (const item of allPrescriptions.value) {
    counter[item.status] = (counter[item.status] || 0) + 1
  }
  return [
    { label: '待缴费', value: counter[0] || 0 },
    { label: '已缴费', value: counter[1] || 0 },
    { label: '已发药', value: counter[2] || 0 },
    { label: '已作废', value: counter[3] || 0 },
  ]
})

const rawRiskDistribution = computed<DistributionPoint[]>(() => {
  let normal = 0
  let warning = 0
  let critical = 0
  for (const prescription of allPrescriptions.value) {
    const count = prescriptionWarnings(prescription.items || [], selectedPatient.value).length
    if (count === 0) normal += 1
    else if (count === 1) warning += 1
    else critical += 1
  }
  return [
    { label: '普通', value: normal },
    { label: '提示', value: warning },
    { label: '高风险', value: critical },
  ]
})

const prescriptionStatusTrend = computed(() =>
  applyTrendOverride(rawStatusTrend.value, pageState.value.chartOverrides.statusTrend),
)
const riskDistribution = computed(() =>
  applyDistributionOverride(rawRiskDistribution.value, pageState.value.chartOverrides.riskDistribution),
)

function loadPageState(): PrescriptionPageState {
  if (typeof window === 'undefined') return { chartOverrides: {} }
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return { chartOverrides: {} }
  try {
    const parsed = JSON.parse(raw) as PrescriptionPageState
    return { chartOverrides: parsed.chartOverrides || {} }
  } catch {
    return { chartOverrides: {} }
  }
}

function persistPageState() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(pageState.value))
}

function syncDrafts() {
  statusDraft.value = prescriptionStatusTrend.value.map((item) => ({ ...item }))
  riskDraft.value = riskDistribution.value.map((item) => ({ ...item }))
}

function prescStatusText(status: number) {
  return ['待缴费', '已缴费', '已发药', '已作废'][status] || '未知'
}

function prescStatusType(status: number) {
  if (status === 0) return 'warning'
  if (status === 2) return 'success'
  if (status === 3) return 'info'
  return 'primary'
}

async function loadRegs() {
  const res = await listRegistrations()
  completedRegs.value = (res.data as Registration[]).filter((item) => item.status === 2)
}

async function loadPrescriptionStats() {
  const res = await listPrescriptions(undefined, false)
  allPrescriptions.value = res.data
}

async function handleSelectPatient(row: Registration) {
  selectedReg.value = row
  existingPrescription.value = null
  items.value = []
  itemQty.value = 1
  itemUsage.value = '遵医嘱'

  const patientRes = await listPatients()
  selectedPatient.value = patientRes.data.find((item) => item.id === row.patientId) || null

  const recordsRes = await listMedicalRecords(row.patientId, row.id)
  const record = recordsRes.data[0]
  if (!record) return

  try {
    const prescriptionRes = await getByRecord(record.id!)
    existingPrescription.value = prescriptionRes.data
  } catch {
    existingPrescription.value = null
  }
}

async function searchMeds() {
  const res = await listMedicines(medKw.value.trim() || undefined)
  medResults.value = res.data
}

function addItem() {
  const med = medResults.value.find((item) => item.id === selectedMedId.value)
  if (!med) {
    ElMessage.warning('请先选择药品')
    return
  }
  if (items.value.some((item) => item.medicineId === med.id)) {
    ElMessage.warning('该药品已在处方中')
    return
  }
  items.value.push({
    medicineId: med.id!,
    quantity: itemQty.value,
    usageDesc: itemUsage.value || '遵医嘱',
    unitPrice: med.price,
    amount: (med.price || 0) * itemQty.value,
    medicineName: med.name,
    medicineSpec: med.spec,
    medicineUnit: med.unit,
  })
  selectedMedId.value = undefined
  medKw.value = ''
  itemQty.value = 1
  itemUsage.value = '遵医嘱'
}

function loadDemoTemplate(type: 'respiratory' | 'hypertension' | 'digestive') {
  const templates: Record<string, PrescriptionItem[]> = {
    respiratory: [
      { medicineId: 9001, quantity: 2, usageDesc: '每日 2 次，每次 1 片', unitPrice: 16, amount: 32, medicineName: '阿莫西林', medicineSpec: '0.25g*24', medicineUnit: '盒' },
      { medicineId: 9002, quantity: 1, usageDesc: '每日 3 次，每次 10ml', unitPrice: 18, amount: 18, medicineName: '止咳糖浆', medicineSpec: '100ml', medicineUnit: '瓶' },
    ],
    hypertension: [
      { medicineId: 9101, quantity: 2, usageDesc: '每日 1 次，每次 1 片', unitPrice: 28, amount: 56, medicineName: '苯磺酸氨氯地平片', medicineSpec: '5mg*28', medicineUnit: '盒' },
      { medicineId: 9102, quantity: 1, usageDesc: '每日 1 次，每次 1 片', unitPrice: 34, amount: 34, medicineName: '缬沙坦胶囊', medicineSpec: '80mg*14', medicineUnit: '盒' },
    ],
    digestive: [
      { medicineId: 9201, quantity: 2, usageDesc: '每日 2 次，每次 1 袋', unitPrice: 12, amount: 24, medicineName: '蒙脱石散', medicineSpec: '3g*10', medicineUnit: '盒' },
      { medicineId: 9202, quantity: 1, usageDesc: '每日 2 次，每次 1 片', unitPrice: 22, amount: 22, medicineName: '奥美拉唑肠溶胶囊', medicineSpec: '20mg*14', medicineUnit: '盒' },
    ],
  }
  items.value = templates[type].map((item) => ({ ...item }))
  ElMessage.success('处方模板已载入')
}

async function savePrescription() {
  if (!selectedReg.value || items.value.length === 0) return
  const recordsRes = await listMedicalRecords(selectedReg.value.patientId, selectedReg.value.id)
  const record = recordsRes.data[0]
  if (!record) {
    ElMessage.error('未找到对应病历，请先在医生工作站完成病历书写')
    return
  }
  await createPrescription({
    prescription: {
      recordId: record.id!,
      patientId: selectedReg.value.patientId,
      doctorId: selectedReg.value.doctorId || 0,
      status: 0,
      totalAmount: totalAmount.value,
    } as Prescription,
    items: items.value,
  })
  ElMessage.success('处方保存成功')
  await loadPrescriptionStats()
  await handleSelectPatient(selectedReg.value)
}

async function handleDispense() {
  if (!existingPrescription.value) return
  await dispensePrescription(existingPrescription.value.id!)
  ElMessage.success('发药完成')
  await loadPrescriptionStats()
  if (selectedReg.value) await handleSelectPatient(selectedReg.value)
}

async function handleCancel() {
  if (!existingPrescription.value) return
  await cancelPrescription(existingPrescription.value.id!)
  ElMessage.success('处方已作废')
  await loadPrescriptionStats()
  if (selectedReg.value) await handleSelectPatient(selectedReg.value)
}

function saveStatusDraft() {
  pageState.value.chartOverrides.statusTrend = statusDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistPageState()
  ElMessage.success('处方状态折线已更新')
}

function resetStatusDraft() {
  delete pageState.value.chartOverrides.statusTrend
  persistPageState()
  syncDrafts()
  ElMessage.success('处方状态折线已恢复真实数据')
}

function saveRiskDraft() {
  pageState.value.chartOverrides.riskDistribution = riskDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistPageState()
  ElMessage.success('风险结构图已更新')
}

function resetRiskDraft() {
  delete pageState.value.chartOverrides.riskDistribution
  persistPageState()
  syncDrafts()
  ElMessage.success('风险结构图已恢复真实数据')
}

watch([prescriptionStatusTrend, riskDistribution], syncDrafts, { immediate: true })

onMounted(async () => {
  await Promise.all([loadRegs(), loadPrescriptionStats(), searchMeds()])
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
  grid-template-columns: 0.8fr 1.2fr;
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

.toolbar {
  display: grid;
  grid-template-columns: 1.1fr 1fr 120px 1.1fr auto;
  gap: 12px;
  margin-bottom: 18px;
}

.template-bar,
.editor-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.warning-panel,
.editor-panel {
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(194, 65, 12, 0.22);
  border-radius: 16px;
  background: rgba(194, 65, 12, 0.08);
}

.editor-panel {
  border-color: rgba(15, 118, 110, 0.18);
  background: rgba(15, 118, 110, 0.08);
}

.warning-panel ul {
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--mc-text-soft);
  line-height: 1.8;
}

.selected-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

.footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.action-group {
  display: flex;
  gap: 12px;
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
  .content-grid--analytics,
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
