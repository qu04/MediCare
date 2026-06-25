<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Patient CRM</div>
      <h2 class="page-hero__title">患者全生命周期中心</h2>
      <p class="page-hero__subtitle">
        统一管理患者基础信息、过敏史和关键标签，并通过分层视图帮助前台与医生快速识别重点患者。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>患者总数</span>
        <strong>{{ tableData.length }}</strong>
        <small>当前可检索档案规模</small>
      </article>
      <article class="glass-card metric-card">
        <span>高关注患者</span>
        <strong>{{ riskPatients.length }}</strong>
        <small>含过敏史或特殊年龄标签</small>
      </article>
      <article class="glass-card metric-card">
        <span>信息完整率</span>
        <strong>{{ completenessRate }}</strong>
        <small>手机号和身份证均已登记</small>
      </article>
    </section>

    <section class="glass-card content-card">
      <header class="card-header">
        <div>
          <h3 class="section-title">患者档案列表</h3>
          <p class="section-subtitle">支持搜索、快速建档和风险标签识别</p>
        </div>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索姓名、身份证或手机号"
            clearable
            @clear="loadData"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">查询</el-button>
            </template>
          </el-input>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>
            新建患者
          </el-button>
        </div>
      </header>

      <div class="insight-banner">
        <div>
          <strong>创新点：患者分层标签</strong>
          <p>系统自动识别老年、儿童、过敏史和信息完整度，减少接诊前人工翻阅档案时间。</p>
        </div>
        <el-tag type="danger" effect="dark">{{ riskPatients.length }} 位需重点关注</el-tag>
      </div>

      <el-table :data="tableData" border stripe>
        <el-table-column prop="name" label="患者姓名" min-width="140">
          <template #default="{ row }">
            <div class="patient-name">
              <strong>{{ row.name }}</strong>
              <span class="muted">{{ genderText(row.gender) }} · {{ ageText(row.birthDate) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="birthDate" label="出生日期" min-width="120">
          <template #default="{ row }">{{ formatDate(row.birthDate) }}</template>
        </el-table-column>
        <el-table-column label="风险标签" min-width="240">
          <template #default="{ row }">
            <div class="tag-group">
              <el-tag
                v-for="tag in patientTags(row)"
                :key="tag.label"
                :type="tag.type"
                effect="light"
              >
                {{ tag.label }}
              </el-tag>
              <span v-if="patientTags(row).length === 0" class="muted">暂无特殊标签</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="allergyInfo" label="过敏史" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.allergyInfo || '无' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDialog(row as Patient)">编辑</el-button>
            <el-popconfirm title="确认删除该患者档案？" @confirm="handleDelete(row.id!)">
              <template #reference>
                <el-button text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑患者' : '新建患者'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="患者姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入患者姓名" />
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="form.idCard" maxlength="18" placeholder="请输入身份证号" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="0">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生日期" prop="birthDate">
            <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="联系地址">
            <el-input v-model="form.address" placeholder="请输入联系地址" />
          </el-form-item>
        </div>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergyInfo" type="textarea" :rows="3" placeholder="如无可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createPatient, deletePatient, listPatients, searchPatients, updatePatient } from '../../api/patient'
import type { Patient } from '../../types'
import { formatDate, computeAge } from '../../utils/format'
import { buildPatientTags } from '../../utils/insights'

const tableData = ref<Patient[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const createDefaultForm = (): Patient => ({
  idCard: '',
  name: '',
  gender: 1,
  birthDate: '',
  phone: '',
  address: '',
  allergyInfo: '',
})

const form = reactive<Patient>(createDefaultForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

const riskPatients = computed(() =>
  tableData.value.filter((item) => buildPatientTags(item).some((tag) => ['danger', 'warning'].includes(tag.type))),
)

const completenessRate = computed(() => {
  if (!tableData.value.length) return '0%'
  const complete = tableData.value.filter((item) => item.idCard && item.phone).length
  return `${Math.round((complete / tableData.value.length) * 100)}%`
})

function genderText(gender: number) {
  return gender === 1 ? '男' : gender === 0 ? '女' : '未知'
}

function ageText(birthDate?: string) {
  const age = computeAge(birthDate)
  return age === null ? '年龄待补充' : `${age} 岁`
}

function patientTags(row: unknown) {
  return buildPatientTags(row as Patient)
}

async function loadData() {
  const res = await listPatients()
  tableData.value = res.data
}

async function handleSearch() {
  if (!keyword.value.trim()) {
    await loadData()
    return
  }
  const res = await searchPatients(keyword.value.trim())
  tableData.value = res.data
}

function openDialog(row?: Patient) {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : createDefaultForm())
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value && form.id) {
    await updatePatient(form.id, { ...form })
  } else {
    await createPatient({ ...form })
  }
  ElMessage.success('患者档案保存成功')
  dialogVisible.value = false
  await loadData()
}

async function handleDelete(id: number) {
  await deletePatient(id)
  ElMessage.success('患者档案已删除')
  await loadData()
}

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

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar :deep(.el-input) {
  width: 320px;
}

.insight-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(194, 65, 12, 0.18);
  border-radius: 16px;
  background: rgba(194, 65, 12, 0.06);
}

.insight-banner p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.patient-name {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
</style>
