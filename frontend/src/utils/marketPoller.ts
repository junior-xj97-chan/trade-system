/**
 * 实时行情轮询调度器
 * 单例模式，多个组件同时使用时共享同一个定时器
 * 当订阅的 code 列表为空时自动停止轮询
 */
import { fetchMarketTicks } from '@/api/market'
import type { StockTick } from '@/api/market'

type Listener = (ticks: Record<string, StockTick>) => void

const INTERVAL = 4000 // 4秒轮询一次

let timer: ReturnType<typeof setInterval> | null = null
let codes = new Set<string>()
const listeners = new Set<Listener>()

/** 通知所有监听器 */
function notify(ticks: Record<string, StockTick>) {
  for (const fn of listeners) {
    try {
      fn(ticks)
    } catch (e) {
      console.error('【marketPoller listener error】', e)
    }
  }
}

/** 执行一次行情拉取 */
async function poll() {
  const list = Array.from(codes)
  if (list.length === 0) return
  const data = await fetchMarketTicks(list)
  notify(data)
}

function start() {
  if (timer) return
  timer = setInterval(poll, INTERVAL)
  // 立即执行一次
  poll()
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/**
 * 订阅行情
 * @param codesList 需要订阅的股票代码列表
 * @param onUpdate  行情更新回调
 * @returns 取消订阅函数
 */
export function subscribeMarket(codesList: string[], onUpdate: Listener) {
  // 合并 code 到全局 Set
  codesList.forEach(c => codes.add(c))
  listeners.add(onUpdate)

  // 如果已有数据缓存，立即回调一次
  start()

  // 返回取消订阅函数
  return () => {
    listeners.delete(onUpdate)
    // 只有当没有其他监听者时才清理 codes
    if (listeners.size === 0) {
      codes.clear()
      stop()
    }
  }
}
