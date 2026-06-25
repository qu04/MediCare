<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Clinical Intelligence</div>
      <h2 class="page-hero__title">电子病例与病例动态分析中心</h2>
      <p class="page-hero__subtitle">
        病例录入、趋势分析和质控建议统一联动。折线图和诊断结构图都支持直接编辑，改完后图表会立即刷新。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>病例总数</span>
        <strong>{{ filteredRecords.length }}</strong>
        <small>当前筛选条件下的病例数量</small>
      </article>
      <article class="glass-card metric-card">
        <span>病例完整率</span>
        <strong>{{ completionRate }}%</strong>
        <small>主诉、诊断、医嘱齐全即视为完整</small>
      </article>
      <article class="glass-card metric-card">
        <span>待完善病例</span>
        <strong>{{ incompleteRecords.length }}</strong>
        <small>系统自动归档到质控提醒中</small>
      </article>
    </section>

    <section class="content-grid content-grid--analytics">
      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">病例折线图</h3>
            <p class="section-subtitle">直接修改每日病例数，图表会实时重绘</p>
          </div>
        </div>

        <SparkLineChart
          title="近 7 日病例新增趋势"
          subtitle="支持按演示或复盘口径调整"
          :points="recordTrend"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>折线图数据编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetTrendDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveTrendDraft">应用到折线图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in trendDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
              <el-input v-model="item.label" />
              <el-input-number v-model="item.value" :min="0" :step="1" />
            </div>
          </div>
        </div>
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">诊断结构图</h3>
            <p class="section-subtitle">直接修改诊断占比，摘要和环图同步变化</p>
          </div>
        </div>

        <DonutChart
          title="诊断主题结构"
          subtitle="自动聚合常见诊断，也支持手动修订"
          center-label="诊断主题"
          :items="diagnosisSummary.slice(0, 5)"
        />

        <div class="editor-panel">
          <div class="editor-panel__header">
            <strong>诊断结构编辑器</strong>
            <div class="editor-actions">
              <el-button size="small" @click="resetDiagnosisDraft">恢复真实数据</el-button>
              <el-button size="small" type="primary" @click="saveDiagnosisDraft">应用到结构图</el-button>
            </div>
          </div>
          <div class="chart-edit-list">
            <div v-for="(item, index) in diagnosisDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
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
            <h3 class="section-title">病例档案库</h3>
            <p class="section-subtitle">支持按患者、医生、主诉和诊断快速筛选</p>
          </div>
        </header>

        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索患者、医生、主诉或诊断" clearable />
          <el-select v-model="diagnosisFilter" clearable placeholder="按诊断筛选">
            <el-option
              v-for="item in diagnosisSummary"
              :key="item.label"
              :label="`${item.label} (${item.value})`"
              :value="item.label"
            />
          </el-select>
          <el-select v-model="qualityFilter" placeholder="病例完整度">
            <el-option label="全部病例" value="all" />
            <el-option label="仅看待完善" value="incomplete" />
            <el-option label="仅看完整病例" value="complete" />
          </el-select>
        </div>

        <div class="insight-banner">
          <div>
            <strong>创新点：病例录入与图表编辑双通道联动</strong>
            <p>既能依靠真实病例驱动图表变化，也能直接编辑图表数据来模拟不同阶段的门诊病例结构。</p>
          </div>
          <el-tag type="warning" effect="dark">{{ incompleteRecords.length }} 份需完善</el-tag>
        </div>

        <el-table :data="filteredRecords" border stripe>
          <el-table-column prop="patientName" label="患者" min-width="120" />
          <el-table-column prop="doctorName" label="医生" min-width="120" />
          <el-table-column prop="chiefComplaint" label="主诉" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.chiefComplaint || '未填写' }}</template>
          </el-table-column>
          <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.diagnosis || '未填写' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="质控状态" width="110">
            <template #default="{ row }">
              <el-tag :type="isRecordComplete(row as MedicalRecord) ? 'success' : 'warning'">
                {{ isRecordComplete(row as MedicalRecord) ? '完整' : '待完善' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="viewDetail(row as MedicalRecord)">详情</el-button>
              <el-button text type="success" @click="openEdit(row as MedicalRecord)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">智能病例摘要</h3>
            <p class="section-subtitle">自动提炼高频诊断和改进建议</p>
          </div>
        </header>

        <div class="summary-stack">
          <div v-for="item in diagnosisSummary.slice(0, 5)" :key="item.label" class="summary-item">
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.value }} 份病例涉及该诊断主题</p>
            </div>
            <el-tag type="info">{{ item.value }}</el-tag>
          </div>
        </div>

        <el-divider />

        <div class="summary-box">
          <strong>自动质控建议</strong>
          <ul class="summary-box__list">
            <li v-for="item in qualitySuggestions" :key="item">{{ item }}</li>
          </ul>
        </div>
      </article>
    </section>

    <el-dialog v-model="detailVisible" title="病例详情" width="760px">
      <el-descriptions v-if="currentRecord" :column="1" border>
        <el-descriptions-item label="患者">{{ currentRecord.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ currentRecord.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="主诉">{{ currentRecord.chiefComplaint || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="现病史">{{ currentRecord.presentIllness || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="既往史">{{ currentRecord.pastHistory || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="体格检查">{{ currentRecord.physicalExam || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ currentRecord.diagnosis || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="医嘱">{{ currentRecord.advice || '未填写' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑病例" width="820px" destroy-on-close>
      <el-form v-if="editForm" :model="editForm" label-width="92px">
        <div class="template-row">
          <span class="muted">快捷模板</span>
          <div class="template-actions">
            <el-button size="small" @click="applyTemplate('respiratory')">呼吸道模板</el-button>
            <el-button size="small" @click="applyTemplate('digestive')">消化系统模板</el-button>
            <el-button size="small" @click="applyTemplate('followup')">复诊随访模板</el-button>
          </div>
        </div>
        <el-form-item label="主诉">
          <el-input v-model="editForm.chiefComplaint" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input v-model="editForm.presentIllness" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input v-model="editForm.pastHistory" />
        </el-form-item>
        <el-form-item label="体格检查">
          <el-input v-model="editForm.physicalExam" />
        </el-form-item>
        <el-form-item label="诊断">
          <el-input v-model="editForm.diagnosis" />
        </el-form-item>
        <el-form-item label="医嘱">
          <el-input v-model="editForm.advice" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存病例</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listMedicalRecords, updateMedicalRecord } from '../../api/medical-record'
import DonutChart from '../../components/charts/DonutChart.vue'
import SparkLineChart from '../../components/charts/SparkLineChart.vue'
import {
  applyDistributionOverride,
  applyTrendOverride,
  buildDiagnosisDistribution,
  buildSevenDayTrend,
  type DistributionPoint,
  type TrendPoint,
} from '../../utils/dashboard'
import type { MedicalRecord } from '../../types'
import { formatDateTime } from '../../utils/format'

type QualityFilter = 'all' | 'incomplete' | 'complete'
type TemplateKey = 'respiratory' | 'digestive' | 'followup'

interface RecordAnalyticsState {
  chartOverrides: {
    recordTrend?: TrendPoint[]
    diagnosisSummary?: DistributionPoint[]
  }
}

const STORAGE_KEY = 'medicare-record-analytics-state'

const recordList = ref<MedicalRecord[]>([])
const detailVisible = ref(false)
const editVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)
const editForm = ref<MedicalRecord | null>(null)
const analyticsState = ref<RecordAnalyticsState>(loadAnalyticsState())
const trendDraft = ref<TrendPoint[]>([])
const diagnosisDraft = ref<DistributionPoint[]>([])

const keyword = ref('')
const diagnosisFilter = ref('')
const qualityFilter = ref<QualityFilter>('all')

const rawDiagnosisSummary = computed(() => buildDiagnosisDistribution(recordList.value))
const rawRecordTrend = computed(() => buildSevenDayTrend(recordList.value))
const diagnosisSummary = computed(() =>
  applyDistributionOverride(rawDiagnosisSummary.value, analyticsState.value.chartOverrides.diagnosisSummary),
)
const recordTrend = computed(() => applyTrendOverride(rawRecordTrend.value, analyticsState.value.chartOverrides.recordTrend))

const incompleteRecords = computed(() => recordList.value.filter((item) => !isRecordComplete(item)))
const completionRate = computed(() => {
  if (!recordList.value.length) return 0
  return Math.round(((recordList.value.length - incompleteRecords.value.length) / recordList.value.length) * 100)
})

const qualitySuggestions = computed(() => {
  const suggestions: string[] = []
  if (incompleteRecords.value.length > 0) {
    suggestions.push(`当前仍有 ${incompleteRecords.value.length} 份病例缺少关键字段，建议优先补齐主诉、诊断和医嘱。`)
  }
  const topDiagnosis = diagnosisSummary.value[0]
  if (topDiagnosis) {
    suggestions.push(`近期高频诊断主题为“${topDiagnosis.label}”，可沉淀专用模板提升录入效率。`)
  }
  suggestions.push(
    completionRate.value >= 90
      ? '病例完整率已达到较高水平，可继续加强复诊随访和处方联动。'
      : '建议将病例完整率提升到 90% 以上，以支撑更稳定的病历质控演示。',
  )
  return suggestions
})

const filteredRecords = computed(() =>
  recordList.value.filter((item) => {
    const haystack = [item.patientName, item.doctorName, item.chiefComplaint, item.diagnosis]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    const matchesKeyword = !keyword.value.trim() || haystack.includes(keyword.value.trim().toLowerCase())
    const matchesDiagnosis = !diagnosisFilter.value || (item.diagnosis?.trim() || '未标注诊断') === diagnosisFilter.value
    const complete = isRecordComplete(item)
    const matchesQuality =
      qualityFilter.value === 'all' ||
      (qualityFilter.value === 'complete' && complete) ||
      (qualityFilter.value === 'incomplete' && !complete)
    return matchesKeyword && matchesDiagnosis && matchesQuality
  }),
)

const recordTemplates: Record<TemplateKey, Partial<MedicalRecord>> = {
  respiratory: {
    chiefComplaint: '咳嗽、咽痛数日',
    presentIllness: '患者自诉近期出现咳嗽、咽痛，伴轻度乏力，无明显胸闷气促。',
    diagnosis: '上呼吸道感染',
    advice: '注意休息，多饮水，遵医嘱服药，如症状加重及时复诊。',
  },
  digestive: {
    chiefComplaint: '腹痛、腹胀不适',
    presentIllness: '患者自诉近两日腹部不适，进食后加重，无明显呕吐。',
    diagnosis: '消化不良',
    advice: '清淡饮食，避免刺激性食物，规律作息，必要时复诊。',
  },
  followup: {
    chiefComplaint: '复诊随访',
    presentIllness: '患者按约定时间复诊，症状较前好转，生命体征平稳。',
    diagnosis: '复诊评估',
    advice: '继续当前治疗方案，按时复诊并观察症状变化。',
  },
}

function loadAnalyticsState(): RecordAnalyticsState {
  if (typeof window === 'undefined') return { chartOverrides: {} }
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return { chartOverrides: {} }
  try {
    const parsed = JSON.parse(raw) as RecordAnalyticsState
    return { chartOverrides: parsed.chartOverrides || {} }
  } catch {
    return { chartOverrides: {} }
  }
}

function persistAnalyticsState() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(analyticsState.value))
}

function syncDrafts() {
  trendDraft.value = recordTrend.value.map((item) => ({ ...item }))
  diagnosisDraft.value = diagnosisSummary.value.map((item) => ({ ...item }))
}

function isRecordComplete(record: MedicalRecord) {
  return !!record.chiefComplaint?.trim() && !!record.diagnosis?.trim() && !!record.advice?.trim()
}

async function loadData() {
  const res = await listMedicalRecords()
  recordList.value = res.data
}

function viewDetail(row: MedicalRecord) {
  currentRecord.value = row
  detailVisible.value = true
}

function openEdit(row: MedicalRecord) {
  editForm.value = { ...row }
  editVisible.value = true
}

function applyTemplate(key: TemplateKey) {
  if (!editForm.value) return
  Object.assign(editForm.value, recordTemplates[key])
}

function saveTrendDraft() {
  analyticsState.value.chartOverrides.recordTrend = trendDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistAnalyticsState()
  ElMessage.success('病例折线图已更新')
}

function resetTrendDraft() {
  delete analyticsState.value.chartOverrides.recordTrend
  persistAnalyticsState()
  syncDrafts()
  ElMessage.success('病例折线图已恢复真实数据')
}

function saveDiagnosisDraft() {
  analyticsState.value.chartOverrides.diagnosisSummary = diagnosisDraft.value.map((item) => ({
    label: item.label,
    value: Number(item.value) || 0,
  }))
  persistAnalyticsState()
  ElMessage.success('诊断结构图已更新')
}

function resetDiagnosisDraft() {
  delete analyticsState.value.chartOverrides.diagnosisSummary
  persistAnalyticsState()
  syncDrafts()
  ElMessage.success('诊断结构图已恢复真实数据')
}

async function saveEdit() {
  if (!editForm.value?.id) return
  await updateMedicalRecord(editForm.value.id, editForm.value)
  ElMessage.success('病例已更新，分析图表同步刷新')
  editVisible.value = false
  await loadData()
}

watch([recordTrend, diagnosisSummary], syncDrafts, { immediate: true })

onMounted(loadData)
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
  grid-template-columns: 1.2fr 0.8fr;
  gap: 18px;
}

.content-grid--analytics {
  grid-template-columns: 1.05fr 0.95fr;
}

.card-header,
.chart-head,
.editor-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr 0.8fr;
  gap: 12px;
  margin-bottom: 16px;
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

.insight-banner p,
.summary-item p,
.summary-box p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.summary-stack,
.chart-edit-list {
  display: grid;
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--mc-border);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.02);
}

.summary-box,
.editor-panel {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.08);
}

.summary-box__list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: var(--mc-text-soft);
  line-height: 1.8;
}

.template-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

.template-actions,
.editor-actions {
  display: flex;
  gap: 10px;
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

  .template-row,
  .editor-panel__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
