/**
 * 实时行情 Pinia Store
 * 由各页面共享，避免重复轮询
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchMarketTicks } from '@/api/market'
import type { StockTick } from '@/api/market'

export const useMarketStore = defineStore('market', () => {
  /** code -> StockTick */
  const ticks = ref<Record<string, StockTick>>({})

  /** 是否正在加载 */
  const loading = ref(false)

  /** 上次更新时间 */
  const lastUpdate = ref('')

  /** 获取某只股票行情（只读计算属性） */
  function getTick(code: string): StockTick | undefined {
    return ticks.value[code]
  }

  /** 批量更新行情 */
  async function refresh(codes: string[]): Promise<void> {
    if (codes.length === 0) return
    loading.value = true
    try {
      const data = await fetchMarketTicks(codes)
      ticks.value = { ...ticks.value, ...data }
      lastUpdate.value = new Date().toLocaleTimeString()
    } finally {
      loading.value = false
    }
  }

  return {
    ticks,
    loading,
    lastUpdate,
    getTick,
    refresh,
  }
})
