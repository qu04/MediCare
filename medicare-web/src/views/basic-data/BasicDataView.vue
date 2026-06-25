<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Master Data Hub</div>
      <h2 class="page-hero__title">基础主数据与产能编排</h2>
      <p class="page-hero__subtitle">
        把科室、医生、排班集中治理，形成可复用、可运营、可扩展的门诊主数据底座。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>科室数量</span>
        <strong>{{ deptList.length }}</strong>
        <small>支撑挂号分诊与医生归属</small>
      </article>
      <article class="glass-card metric-card">
        <span>在岗医生</span>
        <strong>{{ activeDoctorCount }}</strong>
        <small>当前可参与排班的医生人数</small>
      </article>
      <article class="glass-card metric-card">
        <span>今日排班</span>
        <strong>{{ schedList.length }}</strong>
        <small>当前日期下的已编排班次数</small>
      </article>
    </section>

    <section class="glass-card content-card">
      <header class="card-header">
        <div>
          <h3 class="section-title">运营编排中心</h3>
          <p class="section-subtitle">统一维护主数据并监控排班压力</p>
        </div>
      </header>

      <div class="insight-grid">
        <article class="insight-box warning">
          <strong>创新点：排班压力预警</strong>
          <p>{{ busySchedules.length }} 个班次余号紧张，建议优先补充医生或释放号源。</p>
        </article>
        <article class="insight-box success">
          <strong>主数据完备度</strong>
          <p>{{ deptList.length }} 个科室、{{ doctorList.length }} 名医生已形成可调度资源池。</p>
        </article>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="科室管理" name="dept">
          <div class="toolbar">
            <el-button type="primary" @click="openDeptDialog()">
              <el-icon><Plus /></el-icon>
              新建科室
            </el-button>
          </div>
          <el-table :data="deptList" border stripe>
            <el-table-column prop="name" label="科室名称" min-width="180" />
            <el-table-column prop="location" label="位置" min-width="180">
              <template #default="{ row }">{{ row.location || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="联系电话" min-width="140">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openDeptDialog(row as Department)">编辑</el-button>
                <el-popconfirm title="确认删除该科室？" @confirm="handleDeleteDept(row.id!)">
                  <template #reference>
                    <el-button text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="医生管理" name="doctor">
          <div class="toolbar">
            <el-select v-model="deptFilter" clearable placeholder="按科室筛选" @change="loadDoctors">
              <el-option v-for="item in deptList" :key="item.id" :label="item.name" :value="item.id!" />
            </el-select>
            <el-button type="primary" @click="openDoctorDialog()">
              <el-icon><Plus /></el-icon>
              新建医生
            </el-button>
          </div>
          <el-table :data="doctorList" border stripe>
            <el-table-column prop="name" label="医生姓名" min-width="140" />
            <el-table-column prop="departmentName" label="所属科室" min-width="140" />
            <el-table-column prop="title" label="职称" min-width="140">
              <template #default="{ row }">{{ row.title || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '在岗' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openDoctorDialog(row as Doctor)">编辑</el-button>
                <el-popconfirm title="确认删除该医生档案？" @confirm="handleDeleteDoctor(row.id!)">
                  <template #reference>
                    <el-button text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="排班管理" name="schedule">
          <div class="toolbar">
            <el-date-picker v-model="schedDate" type="date" value-format="YYYY-MM-DD" @change="loadSchedules" />
            <el-select v-model="schedDeptFilter" clearable placeholder="按科室筛选" @change="loadSchedules">
              <el-option v-for="item in deptList" :key="item.id" :label="item.name" :value="item.id!" />
            </el-select>
            <el-button type="primary" @click="openSchedDialog()">
              <el-icon><Plus /></el-icon>
              新建排班
            </el-button>
          </div>
          <el-table :data="schedList" border stripe>
            <el-table-column prop="doctorName" label="医生" min-width="120" />
            <el-table-column prop="departmentName" label="科室" min-width="120" />
            <el-table-column prop="workDate" label="日期" min-width="120" />
            <el-table-column prop="timeSlot" label="时段" min-width="100" />
            <el-table-column prop="totalSlots" label="总号源" width="100" />
            <el-table-column prop="remainSlots" label="剩余号源" width="120">
              <template #default="{ row }">
                <el-tag :type="pressureTag(row as Schedule)">
                  {{ row.remainSlots }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="班次压力" width="120">
              <template #default="{ row }">
                <span>{{ pressureText(row as Schedule) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openSchedDialog(row as Schedule)">编辑</el-button>
                <el-popconfirm title="确认删除该排班？" @confirm="handleDeleteSched(row.id!)">
                  <template #reference>
                    <el-button text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="deptDialogVisible" :title="deptIsEdit ? '编辑科室' : '新建科室'" width="480px" destroy-on-close>
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="88px">
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="deptForm.name" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="deptForm.location" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="deptForm.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="doctorDialogVisible" :title="doctorIsEdit ? '编辑医生' : '新建医生'" width="520px" destroy-on-close>
      <el-form ref="doctorFormRef" :model="doctorForm" :rules="doctorRules" label-width="88px">
        <el-form-item label="医生姓名" prop="name">
          <el-input v-model="doctorForm.name" />
        </el-form-item>
        <el-form-item label="所属科室" prop="departmentId">
          <el-select v-model="doctorForm.departmentId" style="width: 100%">
            <el-option v-for="item in deptList" :key="item.id" :label="item.name" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="doctorForm.title" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="doctorForm.status">
            <el-radio :value="1">在岗</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="doctorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDoctor">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="schedDialogVisible" :title="schedIsEdit ? '编辑排班' : '新建排班'" width="520px" destroy-on-close>
      <el-form ref="schedFormRef" :model="schedForm" :rules="schedRules" label-width="88px">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="schedForm.doctorId" style="width: 100%">
            <el-option
              v-for="item in activeDoctors"
              :key="item.id"
              :label="`${item.name} · ${item.departmentName || '未分配科室'}`"
                :value="item.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker v-model="schedForm.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-select v-model="schedForm.timeSlot" style="width: 100%">
            <el-option label="上午" value="上午" />
            <el-option label="下午" value="下午" />
            <el-option label="晚间" value="晚间" />
          </el-select>
        </el-form-item>
        <el-form-item label="总号源" prop="totalSlots">
          <el-input-number v-model="schedForm.totalSlots" :min="1" />
        </el-form-item>
        <el-form-item label="剩余号源" prop="remainSlots">
          <el-input-number v-model="schedForm.remainSlots" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schedDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSched">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createDepartment, deleteDepartment, listDepartments, updateDepartment } from '../../api/department'
import { createDoctor, deleteDoctor, listDoctors, updateDoctor } from '../../api/doctor'
import { createSchedule, deleteSchedule, listSchedules, updateSchedule } from '../../api/schedule'
import type { Department, Doctor, Schedule } from '../../types'
import { schedulePressure } from '../../utils/insights'

const activeTab = ref('dept')

const deptList = ref<Department[]>([])
const doctorList = ref<Doctor[]>([])
const schedList = ref<Schedule[]>([])

const deptFilter = ref<number>()
const schedDeptFilter = ref<number>()
const schedDate = ref(new Date().toISOString().slice(0, 10))

const deptDialogVisible = ref(false)
const deptIsEdit = ref(false)
const deptFormRef = ref<FormInstance>()
const deptForm = reactive<Department>({ name: '', location: '', phone: '' })
const deptRules: FormRules = {
  name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
}

const doctorDialogVisible = ref(false)
const doctorIsEdit = ref(false)
const doctorFormRef = ref<FormInstance>()
const doctorForm = reactive<Doctor>({ name: '', departmentId: 0, title: '', status: 1 })
const doctorRules: FormRules = {
  name: [{ required: true, message: '请输入医生姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }],
}

const schedDialogVisible = ref(false)
const schedIsEdit = ref(false)
const schedFormRef = ref<FormInstance>()
const schedForm = reactive<Schedule>({
  doctorId: 0,
  workDate: '',
  timeSlot: '上午',
  totalSlots: 20,
  remainSlots: 20,
})
const schedRules: FormRules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }],
}

const activeDoctorCount = computed(() => doctorList.value.filter((item) => item.status === 1).length)
const activeDoctors = computed(() => doctorList.value.filter((item) => item.status === 1))
const busySchedules = computed(() => schedList.value.filter((item) => schedulePressure(item) !== 'healthy'))

function createDeptForm() {
  return { name: '', location: '', phone: '' }
}

function createDoctorForm() {
  return { name: '', departmentId: deptList.value[0]?.id || 0, title: '', status: 1 }
}

function createSchedForm() {
  return { doctorId: activeDoctors.value[0]?.id || 0, workDate: schedDate.value, timeSlot: '上午', totalSlots: 20, remainSlots: 20 }
}

function pressureTag(row: Schedule) {
  const pressure = schedulePressure(row)
  if (pressure === 'critical') return 'danger'
  if (pressure === 'busy') return 'warning'
  return 'success'
}

function pressureText(row: Schedule) {
  const pressure = schedulePressure(row)
  if (pressure === 'critical') return '紧张'
  if (pressure === 'busy') return '繁忙'
  return '充足'
}

async function loadDepts() {
  const res = await listDepartments()
  deptList.value = res.data
}

async function loadDoctors() {
  const res = await listDoctors(deptFilter.value)
  doctorList.value = res.data
}

async function loadSchedules() {
  const res = await listSchedules(schedDate.value, schedDeptFilter.value)
  schedList.value = res.data
}

function openDeptDialog(row?: Department) {
  deptIsEdit.value = !!row
  Object.assign(deptForm, row ? { ...row } : createDeptForm())
  deptDialogVisible.value = true
}

function openDoctorDialog(row?: Doctor) {
  doctorIsEdit.value = !!row
  Object.assign(doctorForm, row ? { ...row } : createDoctorForm())
  doctorDialogVisible.value = true
}

function openSchedDialog(row?: Schedule) {
  schedIsEdit.value = !!row
  Object.assign(schedForm, row ? { ...row } : createSchedForm())
  schedDialogVisible.value = true
}

async function saveDept() {
  const valid = await deptFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (deptIsEdit.value && deptForm.id) {
    await updateDepartment(deptForm.id, { ...deptForm })
  } else {
    await createDepartment({ ...deptForm })
  }
  ElMessage.success('科室信息已保存')
  deptDialogVisible.value = false
  await loadDepts()
}

async function saveDoctor() {
  const valid = await doctorFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (doctorIsEdit.value && doctorForm.id) {
    await updateDoctor(doctorForm.id, { ...doctorForm })
  } else {
    await createDoctor({ ...doctorForm })
  }
  ElMessage.success('医生信息已保存')
  doctorDialogVisible.value = false
  await loadDoctors()
}

async function saveSched() {
  const valid = await schedFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (schedIsEdit.value && schedForm.id) {
    await updateSchedule(schedForm.id, { ...schedForm })
  } else {
    await createSchedule({ ...schedForm })
  }
  ElMessage.success('排班信息已保存')
  schedDialogVisible.value = false
  await loadSchedules()
}

async function handleDeleteDept(id: number) {
  await deleteDepartment(id)
  ElMessage.success('科室已删除')
  await loadDepts()
}

async function handleDeleteDoctor(id: number) {
  await deleteDoctor(id)
  ElMessage.success('医生档案已删除')
  await loadDoctors()
}

async function handleDeleteSched(id: number) {
  await deleteSchedule(id)
  ElMessage.success('排班已删除')
  await loadSchedules()
}

onMounted(async () => {
  await loadDepts()
  await loadDoctors()
  await loadSchedules()
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

.card-header {
  margin-bottom: 18px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.insight-box {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--mc-border);
}

.insight-box.warning {
  background: rgba(217, 119, 6, 0.08);
}

.insight-box.success {
  background: rgba(21, 128, 61, 0.08);
}

.insight-box p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar :deep(.el-select),
.toolbar :deep(.el-date-editor) {
  width: 220px;
}
</style>
