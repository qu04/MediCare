import type { Medicine, Patient, PrescriptionItem, Registration, Schedule } from '../types'
import { computeAge } from './format'

export function buildPatientTags(patient: Patient) {
  const tags: Array<{ label: string; type: 'danger' | 'warning' | 'success' | 'info' }> = []
  const age = computeAge(patient.birthDate)
  if (age !== null && age >= 65) tags.push({ label: '老年患者', type: 'warning' })
  if (age !== null && age <= 12) tags.push({ label: '儿童患者', type: 'info' })
  if (patient.allergyInfo?.trim()) tags.push({ label: '过敏史', type: 'danger' })
  if (patient.phone?.trim()) tags.push({ label: '信息完整', type: 'success' })
  return tags
}

export function schedulePressure(schedule: Schedule) {
  if (!schedule.totalSlots) return 'unknown'
  const ratio = schedule.remainSlots / schedule.totalSlots
  if (ratio <= 0.15) return 'critical'
  if (ratio <= 0.35) return 'busy'
  return 'healthy'
}

export function registrationStatusText(status: number) {
  return ['待诊', '就诊中', '已完成', '已取消'][status] || '未知'
}

export function medicineRiskLevel(medicine: Medicine) {
  if (medicine.stock <= 0) return 'critical'
  if (medicine.stock <= medicine.safetyStock / 2) return 'critical'
  if (medicine.stock <= medicine.safetyStock) return 'warning'
  return 'healthy'
}

export function prescriptionWarnings(items: PrescriptionItem[], patient?: Patient | null) {
  const warnings: string[] = []
  if (patient?.allergyInfo?.trim()) {
    warnings.push(`患者存在过敏史：${patient.allergyInfo}`)
  }

  const highQuantityItems = items.filter((item) => item.quantity >= 5)
  if (highQuantityItems.length > 0) {
    warnings.push(`有 ${highQuantityItems.length} 项药品数量较高，请复核剂量`)
  }

  const duplicated = new Set<number>()
  const seen = new Set<number>()
  for (const item of items) {
    if (seen.has(item.medicineId)) duplicated.add(item.medicineId)
    seen.add(item.medicineId)
  }
  if (duplicated.size > 0) {
    warnings.push('处方中存在重复药品，请确认是否重复开立')
  }

  return warnings
}

export function workloadSummary(registrations: Registration[]) {
  const waiting = registrations.filter((item) => item.status === 0).length
  const active = registrations.filter((item) => item.status === 1).length
  const completed = registrations.filter((item) => item.status === 2).length
  return { waiting, active, completed }
}
