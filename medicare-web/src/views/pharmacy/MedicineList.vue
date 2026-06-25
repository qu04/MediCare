<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="chip">Pharmacy Control</div>
      <h2 class="page-hero__title">药品库存与风险补货中心</h2>
      <p class="page-hero__subtitle">
        面向药房与运营管理者统一维护药品档案、出入库和库存风险，支持演示级与商用级的库存预警体验。
      </p>
    </section>

    <section class="metrics-grid">
      <article class="glass-card metric-card">
        <span>药品总数</span>
        <strong>{{ tableData.length }}</strong>
        <small>当前检索范围内的药品数量</small>
      </article>
      <article class="glass-card metric-card">
        <span>风险库存</span>
        <strong>{{ warningCount }}</strong>
        <small>低于或接近安全库存阈值</small>
      </article>
      <article class="glass-card metric-card">
        <span>缺货品种</span>
        <strong>{{ criticalCount }}</strong>
        <small>需要立即补货或调拨</small>
      </article>
    </section>

    <section class="glass-card content-card">
      <header class="card-header">
        <div>
          <h3 class="section-title">库存看板</h3>
          <p class="section-subtitle">支持模糊检索、低库存筛选与快速出入库</p>
        </div>
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索药品名称、拼音码" clearable @keyup.enter="loadData">
            <template #append>
              <el-button @click="loadData">查询</el-button>
            </template>
          </el-input>
          <el-button type="warning" @click="showLowStock">只看预警</el-button>
          <el-button type="primary" @click="openMedDialog()">
            <el-icon><Plus /></el-icon>
            新建药品
          </el-button>
        </div>
      </header>

      <div class="insight-grid">
        <article class="insight-box danger">
          <strong>创新点：库存风险分层</strong>
          <p>系统自动识别“正常 / 关注 / 紧急”三档风险，便于药房安排补货优先级。</p>
        </article>
        <article class="insight-box success">
          <strong>补货建议</strong>
          <p>建议优先处理 {{ criticalCount }} 个缺货品种，其次补齐 {{ warningCount - criticalCount }} 个警戒库存品种。</p>
        </article>
      </div>

      <el-table :data="tableData" border stripe>
        <el-table-column prop="name" label="药品名称" min-width="180" />
        <el-table-column prop="spec" label="规格" min-width="140">
          <template #default="{ row }">{{ row.spec || '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="90">
          <template #default="{ row }">{{ row.unit || '-' }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="当前库存" width="110" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column label="风险等级" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row as Medicine)">{{ riskText(row as Medicine) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="零售价" width="120">
          <template #default="{ row }">{{ formatCurrency(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" min-width="120">
          <template #default="{ row }">{{ formatDate(row.expiryDate) }}</template>
        </el-table-column>
        <el-table-column prop="manufacturer" label="生产厂家" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" @click="openStockDialog(row as Medicine, 'in')">入库</el-button>
            <el-button text type="warning" @click="openStockDialog(row as Medicine, 'out')">出库</el-button>
            <el-button text type="primary" @click="openMedDialog(row as Medicine)">编辑</el-button>
            <el-popconfirm title="确认删除该药品？" @confirm="handleDelete(row.id!)">
              <template #reference>
                <el-button text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="medDialogVisible" :title="medIsEdit ? '编辑药品' : '新建药品'" width="640px" destroy-on-close>
      <el-form ref="medFormRef" :model="medForm" :rules="medRules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="药品名称" prop="name">
            <el-input v-model="medForm.name" />
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="medForm.spec" />
          </el-form-item>
          <el-form-item label="单位">
            <el-input v-model="medForm.unit" />
          </el-form-item>
          <el-form-item label="零售价">
            <el-input-number v-model="medForm.price" :precision="2" :min="0" />
          </el-form-item>
          <el-form-item label="安全库存">
            <el-input-number v-model="medForm.safetyStock" :min="0" />
          </el-form-item>
          <el-form-item label="拼音码">
            <el-input v-model="medForm.pinyinCode" />
          </el-form-item>
        </div>
        <el-form-item label="生产厂家">
          <el-input v-model="medForm.manufacturer" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMed">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockDialogVisible" :title="stockType === 'in' ? '药品入库' : '药品出库'" width="480px" destroy-on-close>
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="96px">
        <el-form-item label="药品">
          <el-input :model-value="stockMedicine?.name" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :model-value="stockMedicine?.stock" disabled />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item v-if="stockType === 'in'" label="批号">
          <el-input v-model="stockForm.batchNo" />
        </el-form-item>
        <el-form-item v-if="stockType === 'in'" label="有效期">
          <el-date-picker v-model="stockForm.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doStock">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createMedicine, deleteMedicine, listLowStock, listMedicines, stockIn, stockOut, updateMedicine } from '../../api/medicine'
import type { Medicine, StockRequest } from '../../types'
import { formatCurrency, formatDate } from '../../utils/format'
import { medicineRiskLevel } from '../../utils/insights'

const tableData = ref<Medicine[]>([])
const keyword = ref('')

const medDialogVisible = ref(false)
const medIsEdit = ref(false)
const medFormRef = ref<FormInstance>()
const medForm = reactive<Medicine>({
  name: '',
  spec: '',
  unit: '盒',
  stock: 0,
  safetyStock: 10,
  price: 0,
  pinyinCode: '',
  manufacturer: '',
  status: 1,
})
const medRules: FormRules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
}

const stockDialogVisible = ref(false)
const stockType = ref<'in' | 'out'>('in')
const stockMedicine = ref<Medicine | null>(null)
const stockFormRef = ref<FormInstance>()
const stockForm = reactive<StockRequest>({ quantity: 1, batchNo: '', expiryDate: '', remark: '' })
const stockRules: FormRules = {
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
}

const warningCount = computed(() => tableData.value.filter((item) => medicineRiskLevel(item) !== 'healthy').length)
const criticalCount = computed(() => tableData.value.filter((item) => medicineRiskLevel(item) === 'critical').length)

function createMedicineForm() {
  return {
    name: '',
    spec: '',
    unit: '盒',
    stock: 0,
    safetyStock: 10,
    price: 0,
    pinyinCode: '',
    manufacturer: '',
    status: 1,
  }
}

function riskTagType(row: Medicine) {
  const level = medicineRiskLevel(row)
  if (level === 'critical') return 'danger'
  if (level === 'warning') return 'warning'
  return 'success'
}

function riskText(row: Medicine) {
  const level = medicineRiskLevel(row)
  if (level === 'critical') return '紧急'
  if (level === 'warning') return '关注'
  return '正常'
}

async function loadData() {
  const res = await listMedicines(keyword.value.trim() || undefined)
  tableData.value = res.data
}

async function showLowStock() {
  const res = await listLowStock()
  tableData.value = res.data
}

function openMedDialog(row?: Medicine) {
  medIsEdit.value = !!row
  Object.assign(medForm, row ? { ...row } : createMedicineForm())
  medDialogVisible.value = true
}

async function saveMed() {
  const valid = await medFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (medIsEdit.value && medForm.id) {
    await updateMedicine(medForm.id, { ...medForm })
  } else {
    await createMedicine({ ...medForm })
  }
  ElMessage.success('药品信息已保存')
  medDialogVisible.value = false
  await loadData()
}

async function handleDelete(id: number) {
  await deleteMedicine(id)
  ElMessage.success('药品已删除')
  await loadData()
}

function openStockDialog(row: Medicine, type: 'in' | 'out') {
  stockType.value = type
  stockMedicine.value = row
  Object.assign(stockForm, { quantity: 1, batchNo: '', expiryDate: '', remark: '' })
  stockDialogVisible.value = true
}

async function doStock() {
  if (!stockMedicine.value) return
  if (stockType.value === 'in') {
    await stockIn(stockMedicine.value.id!, { ...stockForm })
  } else {
    await stockOut(stockMedicine.value.id!, { ...stockForm })
  }
  ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
  stockDialogVisible.value = false
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

.insight-box.danger {
  background: rgba(194, 65, 12, 0.08);
}

.insight-box.success {
  background: rgba(21, 128, 61, 0.08);
}

.insight-box p {
  margin: 8px 0 0;
  color: var(--mc-text-soft);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
</style>
