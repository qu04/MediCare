import type { DashboardStats, MedicalRecord, Prescription, Registration } from '../types'

export interface TrendPoint {
  label: string
  value: number
}

export interface DistributionPoint {
  label: string
  value: number
}

export interface EditableMetric {
  key: string
  label: string
  actual: number
  helper: string
  suffix?: string
  icon: string
  theme: 'primary' | 'warning' | 'danger' | 'success'
}

export interface DashboardEditableState {
  overrides: Record<string, number | null>
  chartOverrides: {
    recordTrend?: TrendPoint[]
    registrationTrend?: TrendPoint[]
    diagnosisDistribution?: DistributionPoint[]
    prescriptionDistribution?: DistributionPoint[]
  }
  animationEnabled: boolean
  goals: {
    outpatientTarget: number
    completionTarget: number
    serviceSatisfaction: number
    followUpTarget: number
  }
}

export interface DoctorWorkbenchState {
  overrides: Record<string, number | null>
  queueTrend?: TrendPoint[]
  animationEnabled: boolean
}

const DEFAULT_DASHBOARD_STATE: DashboardEditableState = {
  overrides: {},
  chartOverrides: {},
  animationEnabled: true,
  goals: {
    outpatientTarget: 32,
    completionTarget: 92,
    serviceSatisfaction: 96,
    followUpTarget: 18,
  },
}

const DEFAULT_DOCTOR_STATE: DoctorWorkbenchState = {
  overrides: {},
  animationEnabled: true,
}

export function loadDashboardEditableState() {
  if (typeof window === 'undefined') return DEFAULT_DASHBOARD_STATE
  const raw = window.localStorage.getItem('medicare-dashboard-state')
  if (!raw) return DEFAULT_DASHBOARD_STATE
  try {
    const parsed = JSON.parse(raw) as DashboardEditableState
    return {
      overrides: parsed.overrides || {},
      chartOverrides: parsed.chartOverrides || {},
      animationEnabled: parsed.animationEnabled ?? true,
      goals: { ...DEFAULT_DASHBOARD_STATE.goals, ...(parsed.goals || {}) },
    }
  } catch {
    return DEFAULT_DASHBOARD_STATE
  }
}

export function saveDashboardEditableState(state: DashboardEditableState) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem('medicare-dashboard-state', JSON.stringify(state))
}

export function loadDoctorWorkbenchState() {
  if (typeof window === 'undefined') return DEFAULT_DOCTOR_STATE
  const raw = window.localStorage.getItem('medicare-doctor-workbench-state')
  if (!raw) return DEFAULT_DOCTOR_STATE
  try {
    const parsed = JSON.parse(raw) as DoctorWorkbenchState
    return {
      overrides: parsed.overrides || {},
      queueTrend: parsed.queueTrend || undefined,
      animationEnabled: parsed.animationEnabled ?? true,
    }
  } catch {
    return DEFAULT_DOCTOR_STATE
  }
}

export function saveDoctorWorkbenchState(state: DoctorWorkbenchState) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem('medicare-doctor-workbench-state', JSON.stringify(state))
}

export function applyMetricOverrides(metrics: EditableMetric[], overrides: Record<string, number | null>) {
  return metrics.map((item) => ({
    ...item,
    displayValue: overrides[item.key] ?? item.actual,
    isEdited: overrides[item.key] !== undefined && overrides[item.key] !== null,
  }))
}

export function applyTrendOverride(points: TrendPoint[], override?: TrendPoint[]) {
  return override?.length ? override : points
}

export function applyDistributionOverride(points: DistributionPoint[], override?: DistributionPoint[]) {
  return override?.length ? override : points
}

export function buildSevenDayTrend(records: MedicalRecord[]) {
  const map = buildRecentDayMap()
  for (const record of records) {
    if (!record.createTime) continue
    const label = formatMonthDay(record.createTime)
    if (map.has(label)) map.set(label, (map.get(label) || 0) + 1)
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value }))
}

export function buildRegistrationTrend(registrations: Registration[]) {
  const map = buildRecentDayMap()
  for (const item of registrations) {
    const raw = item.createTime || item.regTime
    if (!raw) continue
    const label = formatMonthDay(raw)
    if (map.has(label)) map.set(label, (map.get(label) || 0) + 1)
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value }))
}

export function buildDiagnosisDistribution(records: MedicalRecord[]) {
  const counter = new Map<string, number>()
  for (const item of records) {
    const key = item.diagnosis?.trim() || '未标注诊断'
    counter.set(key, (counter.get(key) || 0) + 1)
  }
  return Array.from(counter.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
}

export function buildPrescriptionStatusDistribution(prescriptions: Prescription[]) {
  const counter = new Map<string, number>([
    ['待缴费', 0],
    ['待发药', 0],
    ['已完成', 0],
    ['已作废', 0],
  ])
  for (const item of prescriptions) {
    if (item.status === 0) counter.set('待缴费', (counter.get('待缴费') || 0) + 1)
    else if (item.status === 1) counter.set('待发药', (counter.get('待发药') || 0) + 1)
    else if (item.status === 2) counter.set('已完成', (counter.get('已完成') || 0) + 1)
    else counter.set('已作废', (counter.get('已作废') || 0) + 1)
  }
  return Array.from(counter.entries()).map(([label, value]) => ({ label, value }))
}

export function buildQueueTrend(waiting: number, active: number, completed: number) {
  return [
    { label: '待诊', value: waiting },
    { label: '接诊中', value: active },
    { label: '已完成', value: completed },
    { label: '复诊建议', value: Math.max(1, Math.round(completed * 0.35)) },
  ]
}

export function buildInnovationNotes(
  stats: DashboardStats,
  records: MedicalRecord[],
  editableState: DashboardEditableState,
) {
  const completionRate = records.length
    ? Math.round((records.filter((item) => item.chiefComplaint && item.diagnosis && item.advice).length / records.length) * 100)
    : 0

  return [
    `图表数据支持手动编辑，修改后折线图、环图会即时重绘，当前病历完整率约 ${completionRate}%。`,
    `首页数字和经营目标可以分开维护，当前门诊接诊目标设为 ${editableState.goals.outpatientTarget} 人。`,
    `动画开关打开后，医生站波形图会持续运动，用于强化“监测中”的动态感。`,
    `候诊人数高于 ${Math.max(3, Math.round(stats.waitingCount * 0.7))} 时，可以直接用编辑器模拟高峰场景。`,
  ]
}

function buildRecentDayMap() {
  const map = new Map<string, number>()
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    map.set(formatMonthDay(date.toISOString()), 0)
  }
  return map
}

function formatMonthDay(value: string) {
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
