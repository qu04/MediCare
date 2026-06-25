<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Doctor Cockpit</div>
      <h2 class="page-hero__title">医生工作站与接诊流转台</h2>
      <p class="page-hero__subtitle">
        接诊队列、工作站指标和病历编辑区现在都支持联动演示。你可以导入模拟数据、推进叫号和完成就诊，并让病历内容自动回填。
      </p>

      <div class="hero-actions">
        <el-switch
          v-model="editableState.animationEnabled"
          active-text="波形开"
          inactive-text="波形关"
          @change="persistWorkbenchState"
        />
        <el-button type="primary" @click="openImportDialog">导入演示队列</el-button>
        <el-button @click="loadWaiting">刷新工作站</el-button>
      </div>
    </section>

    <section class="metrics-grid">
      <article v-for="card in workloadCards" :key="card.key" class="glass-card metric-card metric-card--editable">
        <span>{{ card.label }}</span>
        <strong>{{ card.displayValue }}</strong>
        <small>{{ card.helper }}</small>
        <el-tag v-if="card.isEdited" size="small" type="warning">手动修订</el-tag>
        <button class="metric-edit" type="button" @click="openWorkloadEditor(card)">编辑</button>
      </article>
    </section>

    <section class="content-grid content-grid--doctor">
      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">接诊波形监测</h3>
            <p class="section-subtitle">当你调整接诊结构或切换波形开关时，波形强度会同步变化</p>
          </div>
          <el-button text type="primary" @click="openQueueEditor">编辑波形数据</el-button>
        </div>

        <WavePulseChart
          title="接诊负荷波形"
          subtitle="等待、接诊中、已完成三段数据共同驱动动态波形"
          :value="waveCurrentValue"
          :max="waveMaxValue"
          :center-text="waveCenterText"
          :active="editableState.animationEnabled"
        />
      </article>

      <article class="glass-card content-card">
        <div class="chart-head">
          <div>
            <h3 class="section-title">接诊结构折线</h3>
            <p class="section-subtitle">支持手动编辑等待、接诊中、已完成和复诊建议四个阶段的数据</p>
          </div>
          <el-button text type="primary" @click="openQueueEditor">编辑折线</el-button>
        </div>

        <SparkLineChart
          title="工作站状态趋势"
          subtitle="接诊结构与指标卡统一联动"
          :points="queueTrend"
          :animated="editableState.animationEnabled"
        />
      </article>
    </section>

    <section class="content-grid">
      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">接诊队列</h3>
            <p class="section-subtitle">可查看真实挂号数据，也可导入本地演示队列来测试完整流程</p>
          </div>
        </header>

        <div class="toolbar">
          <el-select v-model="selectedDoctorId" placeholder="选择医生" @change="loadWaiting">
            <el-option
              v-for="item in doctorList"
              :key="item.id"
              :label="`${item.name} / ${item.departmentName || '未分配科室'}`"
              :value="item.id!"
            />
          </el-select>
          <el-button @click="openImportDialog">新增演示患者</el-button>
          <el-button text type="danger" @click="clearMockQueue">清空演示队列</el-button>
        </div>

        <div class="insight-banner">
          <div>
            <strong>队列现在支持真实数据 + 演示数据混合联动</strong>
            <p>如果真实环境里挂号样本不够，你可以先导入模拟患者，再测试叫号、接诊和病历填写整套流程。</p>
          </div>
          <el-tag type="primary" effect="dark">{{ totalPendingDisplay }} 人待处理</el-tag>
        </div>

        <el-table :data="waitingList" border stripe highlight-current-row @row-click="handleSelectReg">
          <el-table-column prop="seqNo" label="序号" width="80" />
          <el-table-column prop="patientName" label="患者姓名" min-width="120" />
          <el-table-column prop="departmentName" label="科室" min-width="120" />
          <el-table-column label="数据源" width="92">
            <template #default="{ row }">
              <el-tag :type="isMockRegistration(row as Registration | MockRegistration) ? 'info' : 'success'">
                {{ isMockRegistration(row as Registration | MockRegistration) ? '演示' : '真实' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'primary' : 'success'">
                {{ registrationStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="action-row">
          <el-button type="primary" :disabled="!selectedReg || selectedReg.status !== 0" @click="handleCall">叫号就诊</el-button>
          <el-button type="success" :disabled="!selectedReg || selectedReg.status !== 1" @click="handleComplete">完成就诊</el-button>
          <el-button :disabled="!selectedReg || !isMockRegistration(selectedReg)" @click="removeMockRegistration(selectedReg?.id)">
            删除演示患者
          </el-button>
        </div>
      </article>

      <article class="glass-card content-card">
        <header class="card-header">
          <div>
            <h3 class="section-title">电子病历编辑区</h3>
            <p class="section-subtitle">选中接诊患者后自动带出患者信息、既有病历和建议内容</p>
          </div>
        </header>

        <div v-if="selectedReg" class="selected-summary">
          <div class="selected-summary__head">
            <strong>{{ selectedReg.patientName }} / {{ selectedReg.doctorName || activeDoctorName }}</strong>
            <el-tag :type="selectedReg.status === 1 ? 'primary' : selectedReg.status === 2 ? 'success' : 'warning'">
              {{ registrationStatusText(selectedReg.status) }}
            </el-tag>
          </div>
          <div class="selected-summary__meta">
            <span>科室：{{ selectedReg.departmentName || activeDepartmentName }}</span>
            <span>挂号序号：{{ selectedReg.seqNo || '-' }}</span>
            <span>时间段：{{ selectedReg.timeSlot || '现场接诊' }}</span>
          </div>
          <div class="selected-summary__meta">
            <span>患者ID：{{ selectedReg.patientId }}</span>
            <span>挂号ID：{{ selectedReg.id }}</span>
            <span>{{ isMockRegistration(selectedReg) ? '当前为演示导入数据' : '当前为真实业务数据' }}</span>
          </div>
        </div>

        <div v-if="selectedReg" class="record-assistant">
          <div class="assistant-card">
            <span>病历状态</span>
            <strong>{{ currentRecordId ? '已存在病历' : '待新建病历' }}</strong>
            <small>{{ recordStatusHint }}</small>
          </div>
          <div class="assistant-card">
            <span>智能建议</span>
            <strong>{{ autoSuggestionTitle }}</strong>
            <small>{{ autoSuggestionText }}</small>
          </div>
        </div>

        <el-form v-if="selectedReg" :model="recordForm" label-width="92px">
          <el-form-item label="主诉">
            <el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" placeholder="请输入主诉" />
          </el-form-item>
          <el-form-item label="现病史">
            <el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" placeholder="请输入现病史" />
          </el-form-item>
          <el-form-item label="既往史">
            <el-input v-model="recordForm.pastHistory" placeholder="请输入既往史" />
          </el-form-item>
          <el-form-item label="体格检查">
            <el-input v-model="recordForm.physicalExam" placeholder="请输入体格检查结果" />
          </el-form-item>
          <el-form-item label="诊断">
            <el-input v-model="recordForm.diagnosis" placeholder="请输入初步诊断" />
          </el-form-item>
          <el-form-item label="医嘱">
            <el-input v-model="recordForm.advice" type="textarea" :rows="3" placeholder="请输入医嘱建议" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="selectedReg.status === 0" @click="saveRecord">
              {{ currentRecordId ? '更新病历' : '保存病历' }}
            </el-button>
            <el-button @click="applySmartTemplate">一键填充示例</el-button>
            <el-button @click="resetRecordForm">清空编辑区</el-button>
          </el-form-item>
        </el-form>

        <el-empty v-else description="请先从左侧接诊队列中选择一位患者" />
      </article>
    </section>

    <el-dialog v-model="metricDialogVisible" title="编辑医生站指标" width="520px">
      <el-form label-width="110px">
        <el-form-item label="指标名称">
          <span>{{ activeMetric?.label }}</span>
        </el-form-item>
        <el-form-item label="当前真实值">
          <strong>{{ activeMetric?.actual ?? 0 }}</strong>
        </el-form-item>
        <el-form-item label="展示覆盖值">
          <el-input-number v-model="metricOverrideDraft" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearMetricOverride">恢复真实值</el-button>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMetricOverride">保存展示值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="queueDialogVisible" title="编辑工作站图形数据" width="680px">
      <div class="chart-edit-list">
        <div v-for="(item, index) in queueDraft" :key="`${item.label}-${index}`" class="chart-edit-row">
          <el-input v-model="item.label" />
          <el-input-number v-model="item.value" :min="0" :step="1" />
        </div>
      </div>
      <template #footer>
        <el-button @click="resetQueueEditor">恢复真实图形</el-button>
        <el-button @click="queueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQueueEditor">保存图形数据</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入演示接诊数据" width="760px">
      <el-form label-width="108px">
        <el-form-item label="患者姓名">
          <el-input v-model="importForm.patientName" placeholder="例如：演示患者 A" />
        </el-form-item>
        <el-form-item label="科室名称">
          <el-input v-model="importForm.departmentName" placeholder="例如：内科" />
        </el-form-item>
        <el-form-item label="挂号序号">
          <el-input-number v-model="importForm.seqNo" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-select v-model="importForm.status">
            <el-option label="待诊" :value="0" />
            <el-option label="接诊中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="主诉示例">
          <el-input v-model="importForm.chiefComplaint" type="textarea" :rows="2" placeholder="可选，用于自动填充病历编辑区" />
        </el-form-item>
        <el-form-item label="诊断示例">
          <el-input v-model="importForm.diagnosis" placeholder="可选，例如：高血压复诊" />
        </el-form-item>
        <el-form-item label="医嘱示例">
          <el-input v-model="importForm.advice" type="textarea" :rows="2" placeholder="可选，用于演示病历和图表联动" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveImportedMock">导入到接诊队列</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDoctors } from '../../api/doctor'
import {
  createMedicalRecord,
  getMedicalRecordByRegistration,
  updateMedicalRecord,
} from '../../api/medical-record'
import { callPatient, completeRegistration, listRegistrations } from '../../api/registration'
import SparkLineChart from '../../components/charts/SparkLineChart.vue'
import WavePulseChart from '../../components/charts/WavePulseChart.vue'
import {
  applyMetricOverrides,
  applyTrendOverride,
  buildQueueTrend,
  loadDoctorWorkbenchState,
  saveDoctorWorkbenchState,
  type DoctorWorkbenchState,
  type EditableMetric,
  type TrendPoint,
} from '../../utils/dashboard'
import type { Doctor, MedicalRecord, Registration } from '../../types'
import { registrationStatusText, workloadSummary } from '../../utils/insights'

type MockRegistration = Registration & {
  isMock: true
  mockRecord?: Partial<MedicalRecord>
}

const MOCK_QUEUE_STORAGE_KEY = 'medicare-doctor-workbench-mock-queue'

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number>()
const waitingList = ref<Array<Registration | MockRegistration>>([])
const selectedReg = ref<Registration | MockRegistration | null>(null)
const editableState = reactive<DoctorWorkbenchState>(loadDoctorWorkbenchState())
const mockRegistrations = ref<MockRegistration[]>(loadMockRegistrations())
const currentRecordId = ref<number | null>(null)

const metricDialogVisible = ref(false)
const queueDialogVisible = ref(false)
const importDialogVisible = ref(false)
const activeMetric = ref<EditableMetric | null>(null)
const metricOverrideDraft = ref(0)
const queueDraft = ref<TrendPoint[]>([])

const recordForm = reactive<Partial<MedicalRecord>>({
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExam: '',
  diagnosis: '',
  advice: '',
})

const importForm = reactive({
  patientName: '',
  departmentName: '',
  seqNo: 1,
  status: 0,
  chiefComplaint: '',
  diagnosis: '',
  advice: '',
})

const workload = computed(() => workloadSummary(waitingList.value))
const rawQueueTrend = computed(() => buildQueueTrend(workload.value.waiting, workload.value.active, workload.value.completed))
const queueTrend = computed(() => applyTrendOverride(rawQueueTrend.value, editableState.queueTrend))
const waveCurrentValue = computed(() => queueTrend.value.reduce((sum, item) => sum + item.value, 0))
const waveMaxValue = computed(() => Math.max(12, waveCurrentValue.value + 4))
const waveCenterText = computed(() => `${totalPendingDisplay.value} 人待处理`)

const workloadSource = computed<EditableMetric[]>(() => [
  { key: 'waiting', label: '待诊患者', actual: workload.value.waiting, helper: '支持演示态模拟候诊高峰', icon: 'Timer', theme: 'warning' },
  { key: 'active', label: '接诊中', actual: workload.value.active, helper: '支持模拟多诊室同步接诊', icon: 'DataAnalysis', theme: 'primary' },
  { key: 'completed', label: '已完成', actual: workload.value.completed, helper: '支持模拟阶段性接诊成果', icon: 'CircleCheckFilled', theme: 'success' },
  { key: 'followup', label: '复诊建议', actual: Math.max(1, Math.round(workload.value.completed * 0.35)), helper: '会跟随已完成病例动态变化', icon: 'Bell', theme: 'danger' },
])
const workloadCards = computed(() => applyMetricOverrides(workloadSource.value, editableState.overrides))
const totalPendingDisplay = computed(() =>
  Number(workloadCards.value.find((item) => item.key === 'waiting')?.displayValue || 0) +
  Number(workloadCards.value.find((item) => item.key === 'active')?.displayValue || 0),
)
const activeDoctor = computed(() => doctorList.value.find((item) => item.id === selectedDoctorId.value) || null)
const activeDoctorName = computed(() => activeDoctor.value?.name || '当前医生')
const activeDepartmentName = computed(() => activeDoctor.value?.departmentName || '门诊')
const recordStatusHint = computed(() =>
  currentRecordId.value
    ? '已自动载入该挂号对应病历，保存时会直接更新。'
    : '当前尚无病历，可填写后直接保存为新病历。',
)
const autoSuggestionTitle = computed(() => {
  const chiefComplaint = recordForm.chiefComplaint?.trim()
  if (chiefComplaint?.includes('复诊')) return '复诊随访模板'
  if (chiefComplaint?.includes('咳') || chiefComplaint?.includes('发热')) return '呼吸道就诊模板'
  if (chiefComplaint?.includes('腹') || chiefComplaint?.includes('胃')) return '消化系统模板'
  return '通用初诊模板'
})
const autoSuggestionText = computed(() => {
  if (recordForm.diagnosis?.trim()) {
    return `当前已录入诊断“${recordForm.diagnosis}”，可继续补充体格检查和随访建议。`
  }
  return '可点击“一键填充示例”快速生成主诉、现病史和医嘱示例内容。'
})

function loadMockRegistrations(): MockRegistration[] {
  if (typeof window === 'undefined') return []
  const raw = window.localStorage.getItem(MOCK_QUEUE_STORAGE_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as MockRegistration[]
  } catch {
    return []
  }
}

function persistMockRegistrations() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(MOCK_QUEUE_STORAGE_KEY, JSON.stringify(mockRegistrations.value))
}

function isMockRegistration(row: Registration | MockRegistration | null | undefined): row is MockRegistration {
  return !!row && (row as MockRegistration).isMock === true
}

function persistWorkbenchState() {
  saveDoctorWorkbenchState(editableState)
}

function resetRecordForm() {
  currentRecordId.value = null
  Object.assign(recordForm, {
    chiefComplaint: '',
    presentIllness: '',
    pastHistory: '',
    physicalExam: '',
    diagnosis: '',
    advice: '',
  })
}

function resetImportForm() {
  Object.assign(importForm, {
    patientName: '',
    departmentName: activeDepartmentName.value,
    seqNo: waitingList.value.length + 1,
    status: 0,
    chiefComplaint: '',
    diagnosis: '',
    advice: '',
  })
}

async function loadDoctors() {
  const res = await listDoctors()
  doctorList.value = res.data
  if (!selectedDoctorId.value && doctorList.value.length) {
    selectedDoctorId.value = doctorList.value[0].id
  }
}

async function loadWaiting() {
  if (!selectedDoctorId.value) return
  const res = await listRegistrations()
  const realQueue = (res.data as Registration[]).filter(
    (item) => item.doctorId === selectedDoctorId.value && [0, 1, 2].includes(item.status),
  )
  const mockQueue = mockRegistrations.value.filter((item) => item.doctorId === selectedDoctorId.value)
  waitingList.value = [...realQueue, ...mockQueue].sort((a, b) => (a.seqNo || 0) - (b.seqNo || 0))
  if (selectedReg.value) {
    const latest = waitingList.value.find((item) => item.id === selectedReg.value?.id) || null
    selectedReg.value = latest
    if (latest) {
      await hydrateRecordForm(latest)
    } else {
      resetRecordForm()
    }
  }
}

async function hydrateRecordForm(row: Registration | MockRegistration) {
  resetRecordForm()
  if (isMockRegistration(row) && row.mockRecord) {
    Object.assign(recordForm, row.mockRecord)
    return
  }
  try {
    const res = await getMedicalRecordByRegistration(row.id!)
    if (res.data) {
      currentRecordId.value = res.data.id || null
      Object.assign(recordForm, {
        chiefComplaint: res.data.chiefComplaint || '',
        presentIllness: res.data.presentIllness || '',
        pastHistory: res.data.pastHistory || '',
        physicalExam: res.data.physicalExam || '',
        diagnosis: res.data.diagnosis || '',
        advice: res.data.advice || '',
      })
      return
    }
  } catch {
    // No existing record is acceptable; keep using template data below.
  }
  Object.assign(recordForm, {
    chiefComplaint: row.patientName ? `${row.patientName} 就诊` : '',
    presentIllness: row.status === 1 ? '当前已进入接诊中，建议完善现病史和体格检查。' : '',
    pastHistory: '',
    physicalExam: '',
    diagnosis: '',
    advice: '',
  })
}

async function handleSelectReg(row: Registration | MockRegistration) {
  selectedReg.value = row
  await hydrateRecordForm(row)
}

async function handleCall() {
  if (!selectedReg.value) return
  if (isMockRegistration(selectedReg.value)) {
    selectedReg.value.status = 1
    persistMockRegistrations()
    ElMessage.success('演示患者已进入接诊中')
    await loadWaiting()
    return
  }
  await callPatient(selectedReg.value.id!)
  ElMessage.success('已通知患者进入诊室')
  await loadWaiting()
}

async function handleComplete() {
  if (!selectedReg.value) return
  if (isMockRegistration(selectedReg.value)) {
    selectedReg.value.status = 2
    selectedReg.value.mockRecord = { ...recordForm }
    persistMockRegistrations()
    ElMessage.success('演示患者就诊已完成')
    await loadWaiting()
    return
  }
  await completeRegistration(selectedReg.value.id!)
  ElMessage.success('该患者就诊已完成')
  await loadWaiting()
}

async function saveRecord() {
  if (!selectedReg.value) return
  const payload = {
    registrationId: selectedReg.value.id!,
    patientId: selectedReg.value.patientId,
    doctorId: selectedReg.value.doctorId || selectedDoctorId.value!,
    chiefComplaint: recordForm.chiefComplaint,
    presentIllness: recordForm.presentIllness,
    pastHistory: recordForm.pastHistory,
    physicalExam: recordForm.physicalExam,
    diagnosis: recordForm.diagnosis,
    advice: recordForm.advice,
  } as MedicalRecord

  if (isMockRegistration(selectedReg.value)) {
    selectedReg.value.mockRecord = { ...recordForm }
    persistMockRegistrations()
    ElMessage.success('演示病历已保存到本地工作站')
    return
  }

  if (currentRecordId.value) {
    await updateMedicalRecord(currentRecordId.value, { ...payload, id: currentRecordId.value })
    ElMessage.success('病历已更新')
  } else {
    const res = await createMedicalRecord(payload)
    currentRecordId.value = res.data.id || null
    ElMessage.success('病历已保存')
  }
}

function applySmartTemplate() {
  const baseComplaint = recordForm.chiefComplaint?.trim()
  if (baseComplaint?.includes('复诊')) {
    Object.assign(recordForm, {
      presentIllness: recordForm.presentIllness || '患者按约定时间复诊，症状较前稳定，建议继续随访观察。',
      diagnosis: recordForm.diagnosis || '复诊评估',
      advice: recordForm.advice || '继续当前治疗方案，2 周后复诊并复测指标。',
    })
    return
  }
  if (baseComplaint?.includes('咳') || baseComplaint?.includes('发热')) {
    Object.assign(recordForm, {
      presentIllness: recordForm.presentIllness || '患者自诉近 3 日咳嗽、咽痛，伴轻度乏力，无明显呼吸困难。',
      diagnosis: recordForm.diagnosis || '上呼吸道感染',
      advice: recordForm.advice || '注意休息，多饮水，遵医嘱服药，症状加重及时复诊。',
    })
    return
  }
  Object.assign(recordForm, {
    chiefComplaint: recordForm.chiefComplaint || `${selectedReg.value?.patientName || '患者'} 门诊就诊`,
    presentIllness: recordForm.presentIllness || '建议补充起病时间、持续时长、伴随症状和既往处理情况。',
    advice: recordForm.advice || '请结合检查结果继续完善诊疗方案，并安排复诊或随访。',
  })
}

function openWorkloadEditor(metric: EditableMetric & { displayValue?: number }) {
  activeMetric.value = metric
  metricOverrideDraft.value = editableState.overrides[metric.key] ?? metric.actual
  metricDialogVisible.value = true
}

function saveMetricOverride() {
  if (!activeMetric.value) return
  editableState.overrides[activeMetric.value.key] = metricOverrideDraft.value
  saveDoctorWorkbenchState(editableState)
  metricDialogVisible.value = false
  ElMessage.success('医生站指标已更新')
}

function clearMetricOverride() {
  if (!activeMetric.value) return
  delete editableState.overrides[activeMetric.value.key]
  saveDoctorWorkbenchState(editableState)
  metricDialogVisible.value = false
  ElMessage.success('已恢复真实值')
}

function openQueueEditor() {
  queueDraft.value = queueTrend.value.map((item) => ({ ...item }))
  queueDialogVisible.value = true
}

function saveQueueEditor() {
  editableState.queueTrend = queueDraft.value.map((item) => ({ label: item.label, value: Number(item.value) || 0 }))
  saveDoctorWorkbenchState(editableState)
  queueDialogVisible.value = false
  ElMessage.success('工作站图形数据已更新')
}

function resetQueueEditor() {
  editableState.queueTrend = undefined
  saveDoctorWorkbenchState(editableState)
  queueDialogVisible.value = false
  ElMessage.success('工作站图形已恢复真实数据')
}

function openImportDialog() {
  resetImportForm()
  importDialogVisible.value = true
}

async function saveImportedMock() {
  if (!selectedDoctorId.value) return
  const doctor = activeDoctor.value
  const mockId = Date.now()
  const mockRow: MockRegistration = {
    id: mockId,
    patientId: mockId,
    scheduleId: mockId,
    doctorId: selectedDoctorId.value,
    regTime: new Date().toISOString(),
    status: importForm.status,
    seqNo: importForm.seqNo,
    patientName: importForm.patientName || `演示患者 ${mockRegistrations.value.length + 1}`,
    doctorName: doctor?.name || '当前医生',
    departmentName: importForm.departmentName || doctor?.departmentName || '门诊',
    timeSlot: '演示时段',
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
    isMock: true,
    mockRecord: {
      chiefComplaint: importForm.chiefComplaint,
      diagnosis: importForm.diagnosis,
      advice: importForm.advice,
    },
  }
  mockRegistrations.value.push(mockRow)
  persistMockRegistrations()
  importDialogVisible.value = false
  ElMessage.success('演示接诊数据已导入')
  await loadWaiting()
}

async function clearMockQueue() {
  mockRegistrations.value = mockRegistrations.value.filter((item) => item.doctorId !== selectedDoctorId.value)
  persistMockRegistrations()
  if (selectedReg.value && isMockRegistration(selectedReg.value)) {
    selectedReg.value = null
    resetRecordForm()
  }
  ElMessage.success('当前医生的演示队列已清空')
  await loadWaiting()
}

async function removeMockRegistration(id?: number) {
  if (!id) return
  mockRegistrations.value = mockRegistrations.value.filter((item) => item.id !== id)
  persistMockRegistrations()
  if (selectedReg.value?.id === id) {
    selectedReg.value = null
    resetRecordForm()
  }
  ElMessage.success('演示患者已删除')
  await loadWaiting()
}

onMounted(async () => {
  await loadDoctors()
  await loadWaiting()
})
</script>

<style scoped>
.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
  flex-wrap: wrap;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card,
.content-card {
  padding: 22px 24px;
}

.metric-card {
  position: relative;
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

.metric-edit {
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
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
}

.content-grid--doctor {
  grid-template-columns: 0.92fr 1.08fr;
}

.chart-head,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar :deep(.el-select) {
  width: 280px;
}

.insight-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(15, 118, 110, 0.2);
  border-radius: 16px;
  background: rgba(15, 118, 110, 0.08);
}

.insight-banner p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.selected-summary {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}

.selected-summary__head,
.selected-summary__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.record-assistant {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.assistant-card {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--mc-border);
  background: rgba(15, 23, 42, 0.02);
}

.assistant-card span,
.assistant-card small {
  color: var(--mc-text-soft);
}

.assistant-card strong {
  display: block;
  margin: 8px 0 6px;
}

.chart-edit-list {
  display: grid;
  gap: 14px;
}

.chart-edit-row {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}

@media (max-width: 1200px) {
  .metrics-grid,
  .content-grid,
  .content-grid--doctor,
  .record-assistant {
    grid-template-columns: 1fr;
  }
}
</style>
