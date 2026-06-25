<template>
  <div class="spark-card">
    <div class="spark-card__header">
      <div>
        <h4>{{ title }}</h4>
        <p>{{ subtitle }}</p>
      </div>
      <strong>{{ totalLabel }}</strong>
    </div>

    <svg viewBox="0 0 360 180" class="spark-chart" preserveAspectRatio="none" role="img" :aria-label="title">
      <line
        v-for="tick in 4"
        :key="tick"
        x1="24"
        :y1="26 + (tick - 1) * 34"
        x2="336"
        :y2="26 + (tick - 1) * 34"
        class="spark-grid"
      />
      <polyline :points="areaPoints" class="spark-area" />
      <polyline :points="linePoints" class="spark-line" :class="{ 'spark-line--static': !animated }" />
      <g v-for="(point, index) in normalizedPoints" :key="`${point.label}-${index}`">
        <circle :cx="point.x" :cy="point.y" r="4.5" class="spark-dot" />
        <text :x="point.x" y="166" text-anchor="middle" class="spark-label">{{ shortLabel(point.label) }}</text>
        <text
          v-if="index === normalizedPoints.length - 1 || point.value === maxValue"
          :x="point.x"
          :y="point.y - 10"
          text-anchor="middle"
          class="spark-value"
        >
          {{ point.value }}
        </text>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface ChartPoint {
  label: string
  value: number
}

const props = withDefaults(
  defineProps<{
    title: string
    subtitle: string
    points: ChartPoint[]
    animated?: boolean
  }>(),
  {
    animated: true,
  },
)

const maxValue = computed(() => Math.max(...props.points.map((item) => item.value), 1))
const totalLabel = computed(() => `${props.points.reduce((sum, item) => sum + item.value, 0)} / 周`)

const normalizedPoints = computed(() => {
  const safePoints = props.points.length ? props.points : [{ label: '暂无', value: 0 }]
  const step = safePoints.length === 1 ? 0 : 312 / (safePoints.length - 1)
  return safePoints.map((item, index) => {
    const x = 24 + step * index
    const ratio = item.value / maxValue.value
    const y = 128 - ratio * 102
    return { ...item, x, y }
  })
})

const linePoints = computed(() => normalizedPoints.value.map((item) => `${item.x},${item.y}`).join(' '))
const areaPoints = computed(() => {
  const points = normalizedPoints.value
  if (!points.length) return ''
  const first = points[0]
  const last = points[points.length - 1]
  return [`${first.x},128`, ...points.map((item) => `${item.x},${item.y}`), `${last.x},128`].join(' ')
})

function shortLabel(label: string) {
  return label.length > 5 ? label.slice(5) : label
}
</script>

<style scoped>
.spark-card {
  display: grid;
  gap: 14px;
}

.spark-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.spark-card__header h4 {
  margin: 0 0 6px;
  font-size: 18px;
}

.spark-card__header p {
  margin: 0;
  color: var(--mc-text-soft);
}

.spark-card__header strong {
  font-size: 26px;
  line-height: 1;
}

.spark-chart {
  width: 100%;
  height: 180px;
}

.spark-grid {
  stroke: rgba(148, 163, 184, 0.22);
  stroke-dasharray: 4 6;
}

.spark-area {
  fill: rgba(15, 118, 110, 0.12);
}

.spark-line {
  fill: none;
  stroke: var(--mc-primary);
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  animation: chart-draw 1.1s ease;
}

.spark-line--static {
  animation: none;
}

.spark-dot {
  fill: #fff;
  stroke: var(--mc-primary);
  stroke-width: 3;
}

.spark-label,
.spark-value {
  fill: var(--mc-text-soft);
  font-size: 11px;
}

.spark-value {
  fill: var(--mc-primary);
  font-weight: 700;
}

@keyframes chart-draw {
  from {
    opacity: 0.2;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
