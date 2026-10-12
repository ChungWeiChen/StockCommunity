<template>
    <span>{{ formatted }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: String,
    default: '',
  },
})

const formatter = new Intl.DateTimeFormat('zh-TW', {
  timeZone: 'Asia/Taipei',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
})

const formatted = computed(() => {
  if (!props.value) return '—'

  // 後端小數秒可能超過三位，縮為毫秒
  let value = props.value.trim().replace(
    /(\.\d{3})\d+/,
    '$1',
  )

  // 後端無時區的時間已確認為 UTC
  if (!/(Z|[+-]\d{2}:\d{2})$/i.test(value)) {
    value += 'Z'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  const parts = Object.fromEntries(
    formatter.formatToParts(date).map(({ type, value }) => [
      type,
      value,
    ]),
  )

  return `${parts.year}/${parts.month}/${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
})
</script>

<style scoped>

</style>