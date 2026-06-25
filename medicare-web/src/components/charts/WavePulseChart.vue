<template>
  <div class="wave-card">
    <div class="wave-card__header">
      <div>
        <h4>{{ title }}</h4>
        <p>{{ subtitle }}</p>
      </div>
      <strong>{{ valueLabel }}</strong>
    </div>

    <div class="wave-shell" :class="{ 'wave-shell--active': active }">
      <div class="wave-shell__fill" :style="{ height: `${safePercent}%` }">
        <svg class="wave-svg" viewBox="0 0 240 80" preserveAspectRatio="none">
          <path
            class="wave-path wave-path--front"
            d="M0,28 C20,12 40,12 60,28 C80,44 100,44 120,28 C140,12 160,12 180,28 C200,44 220,44 240,28 L240,80 L0,80 Z"
          />
          <path
            class="wave-path wave-path--back"
            d="M0,36 C20,20 40,20 60,36 C80,52 100,52 120,36 C140,20 160,20 180,36 C200,52 220,52 240,36 L240,80 L0,80 Z"
          />
        </svg>
      </div>
      <div class="wave-shell__center">
        <span>{{ centerText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  subtitle: string
  value: number
  max: number
  centerText: string
  active: boolean
}>()

const safePercent = computed(() => {
  if (!props.max) return 0
  return Math.max(8, Math.min(100, Math.round((props.value / props.max) * 100)))
})

const valueLabel = computed(() => `${props.value}/${props.max}`)
</script>

<style scoped>
.wave-card {
  display: grid;
  gap: 16px;
}

.wave-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.wave-card__header h4 {
  margin: 0 0 6px;
  font-size: 18px;
}

.wave-card__header p {
  margin: 0;
  color: var(--mc-text-soft);
}

.wave-card__header strong {
  font-size: 24px;
}

.wave-shell {
  position: relative;
  height: 220px;
  overflow: hidden;
  border-radius: 28px;
  background:
    radial-gradient(circle at top, rgba(15, 118, 110, 0.14), transparent 60%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(226, 232, 240, 0.96));
  border: 1px solid rgba(15, 118, 110, 0.16);
}

wave-shell__fill {
  position: absolute;
}

.wave-shell__fill {
  position: absolute;
  inset: auto 0 0;
  transition: height 0.45s ease;
}

.wave-svg {
  position: absolute;
  inset: -22px 0 auto;
  width: 200%;
  height: 90px;
}

.wave-path {
  fill: rgba(15, 118, 110, 0.28);
}

.wave-path--back {
  fill: rgba(59, 130, 246, 0.18);
}

.wave-shell__center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--mc-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.wave-shell--active .wave-path--front {
  animation: wave-move 3.2s linear infinite;
}

.wave-shell--active .wave-path--back {
  animation: wave-move-reverse 4.2s linear infinite;
}

@keyframes wave-move {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(-50%);
  }
}

@keyframes wave-move-reverse {
  from {
    transform: translateX(-50%);
  }

  to {
    transform: translateX(0);
  }
}
</style>
