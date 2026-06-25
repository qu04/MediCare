<template>
  <div class="donut-card">
    <div class="donut-card__canvas">
      <svg viewBox="0 0 220 220" class="donut-svg" role="img" :aria-label="title">
        <circle cx="110" cy="110" r="70" class="donut-track" />
        <circle
          v-for="segment in segments"
          :key="segment.label"
          cx="110"
          cy="110"
          r="70"
          class="donut-segment"
          :class="{ 'donut-segment--static': !animated }"
          :stroke="segment.color"
          :stroke-dasharray="segment.dasharray"
          :stroke-dashoffset="segment.offset"
        />
      </svg>
      <div class="donut-card__center">
        <strong>{{ total }}</strong>
        <span>{{ centerLabel }}</span>
      </div>
    </div>

    <div class="donut-card__legend">
      <div class="donut-card__title">
        <h4>{{ title }}</h4>
        <p>{{ subtitle }}</p>
      </div>

      <div v-for="item in legendItems" :key="item.label" class="legend-item">
        <div class="legend-label">
          <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
          <strong>{{ item.label }}</strong>
        </div>
        <span>{{ item.value }} · {{ item.ratio }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface DonutItem {
  label: string
  value: number
}

const props = withDefaults(
  defineProps<{
    title: string
    subtitle: string
    centerLabel: string
    items: DonutItem[]
    animated?: boolean
  }>(),
  {
    animated: true,
  },
)

const palette = ['#0f766e', '#f59e0b', '#dc2626', '#2563eb', '#7c3aed', '#0891b2']
const circumference = 2 * Math.PI * 70

const total = computed(() => props.items.reduce((sum, item) => sum + item.value, 0))

const legendItems = computed(() => {
  const safeTotal = total.value || 1
  return props.items.map((item, index) => ({
    ...item,
    color: palette[index % palette.length],
    ratio: ((item.value / safeTotal) * 100).toFixed(1),
  }))
})

const segments = computed(() => {
  let offsetAccumulator = 0
  const safeTotal = total.value || 1
  return legendItems.value.map((item) => {
    const segmentLength = (item.value / safeTotal) * circumference
    const currentOffset = circumference - offsetAccumulator
    offsetAccumulator += segmentLength
    return {
      ...item,
      dasharray: `${segmentLength} ${circumference - segmentLength}`,
      offset: currentOffset,
    }
  })
})
</script>

<style scoped>
.donut-card {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: center;
}

.donut-card__canvas {
  position: relative;
  width: 220px;
  height: 220px;
}

.donut-svg {
  width: 220px;
  height: 220px;
  transform: rotate(-90deg);
}

.donut-track,
.donut-segment {
  fill: none;
  stroke-width: 22;
}

.donut-track {
  stroke: rgba(148, 163, 184, 0.16);
}

.donut-segment {
  stroke-linecap: round;
  animation: donut-grow 0.8s ease;
}

.donut-segment--static {
  animation: none;
}

.donut-card__center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
}

.donut-card__center strong {
  display: block;
  font-size: 34px;
  line-height: 1;
}

.donut-card__center span {
  color: var(--mc-text-soft);
}

.donut-card__title h4 {
  margin: 0 0 6px;
  font-size: 18px;
}

.donut-card__title p {
  margin: 0 0 16px;
  color: var(--mc-text-soft);
}

.donut-card__legend {
  display: grid;
  gap: 12px;
}

.legend-item,
.legend-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
}

.legend-label {
  justify-content: flex-start;
}

@keyframes donut-grow {
  from {
    opacity: 0.2;
    stroke-dasharray: 0 999;
  }

  to {
    opacity: 1;
  }
}
</style>
