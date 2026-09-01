import { onMounted, onUnmounted, readonly, ref } from 'vue'

const currentTime = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null
let consumers = 0

function startClock() {
  currentTime.value = new Date()
  if (timer) return
  timer = setInterval(() => { currentTime.value = new Date() }, 30_000)
}

function stopClock() {
  if (!timer || consumers > 0) return
  clearInterval(timer)
  timer = null
}

export function useCurrentTime() {
  onMounted(() => {
    consumers++
    startClock()
  })
  onUnmounted(() => {
    consumers = Math.max(0, consumers - 1)
    stopClock()
  })
  return readonly(currentTime)
}
