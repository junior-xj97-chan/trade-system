<template>
  <div class="dashboard">
    <!-- 统计卡片区 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--blue">
          <div class="stat-icon"><Wallet size="28" /></div>
          <div class="stat-info">
            <div class="stat-label">可用余额</div>
            <div class="stat-value">¥ {{ accountInfo?.balance?.toFixed(2) ?? '--' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--purple">
          <div class="stat-icon"><Lock size="28" /></div>
          <div class="stat-info">
            <div class="stat-label">冻结金额</div>
            <div class="stat-value">¥ {{ accountInfo?.frozenAmount?.toFixed(2) ?? '--' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--green">
          <div class="stat-icon"><TrendCharts size="28" /></div>
          <div class="stat-info">
            <div class="stat-label">持仓数量</div>
            <div class="stat-value">{{ displayPositions.length }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--orange">
          <div class="stat-icon"><Money size="28" /></div>
          <div class="stat-info">
            <div class="stat-label">浮动盈亏</div>
            <div class="stat-value" :class="totalProfitLoss >= 0 ? 'profit' : 'loss'">
              {{ totalProfitLoss >= 0 ? '+' : '' }}¥{{ totalProfitLoss.toFixed(2) }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 持仓概览：图表区 -->
    <el-card class="position-card">
      <template #header>
        <div class="card-header">
          <span>
            持仓概览
            <span v-if="realtimeCount > 0" class="realtime-badge">
              <el-icon class="blink"><Connection /></el-icon> 实时行情
            </span>
          </span>
          <el-button type="primary" size="small" @click="$router.push('/position')">
            持仓明细 →
          </el-button>
        </div>
      </template>

      <el-empty v-if="displayPositions.length === 0 && !loading" description="暂无持仓" />

      <el-row v-else :gutter="16" class="chart-row">
        <!-- 左：持仓市值饼图 -->
        <el-col :span="10">
          <div class="chart-title">持仓市值分布</div>
          <div ref="pieChartEl" class="chart-container" />
        </el-col>

        <!-- 右：盈亏柱状图 -->
        <el-col :span="14">
          <div class="chart-title">各股浮动盈亏</div>
          <div ref="barChartEl" class="chart-container" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { accountApi, positionApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Account, Position } from '@/types'
import { Wallet, Lock, TrendCharts, Money, Connection } from '@element-plus/icons-vue'
import { subscribeMarket } from '@/utils/marketPoller'
import type { StockTick } from '@/api/market'
import * as echarts from 'echarts'

const userStore = useUserStore()
const accountInfo = ref<Account | null>(null)
const positions = ref<Position[]>([])
const loading = ref(false)

// 实时价格缓存
const realtimePrices = ref<Record<string, StockTick>>({})

// 有多少股票接到实时行情
const realtimeCount = computed(() =>
  Object.keys(realtimePrices.value).length
)

// 合并静态持仓 + 实时价格
const displayPositions = computed(() => {
  return positions.value.map(p => {
    const code = p.productCode || ''
    const tick = code ? realtimePrices.value[code] : null
    if (tick) {
      const newPrice = tick.price
      const avgCost = Number(p.avgCost)
      const quantity = Number(p.quantity)
      const profitLoss = (newPrice - avgCost) * quantity
      const profitLossPercent = avgCost > 0 ? (profitLoss / (avgCost * quantity)) * 100 : 0
      return { ...p, currentPrice: newPrice, profitLoss, profitLossPercent, _realtime: true }
    }
    return { ...p, _realtime: false }
  })
})

const totalProfitLoss = computed(() =>
  displayPositions.value.reduce((sum, p) => sum + (p.profitLoss || 0), 0),
)

// ==================== ECharts ====================
const pieChartEl = ref<HTMLElement | null>(null)
const barChartEl = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

function initCharts() {
  if (pieChartEl.value && !pieChart) {
    pieChart = echarts.init(pieChartEl.value)
  }
  if (barChartEl.value && !barChart) {
    barChart = echarts.init(barChartEl.value)
  }
}

function updateCharts() {
  const data = displayPositions.value
  if (!data.length) return

  // 饼图：持仓市值分布
  const pieData = data.map(p => ({
    name: p.productCode || (p.productName?.replace(/股票$/, '') ?? ''),
    value: parseFloat((Number(p.currentPrice) * Number(p.quantity)).toFixed(2)),
  }))

  pieChart?.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) =>
        `${params.name}<br/>市值: ¥${params.value.toLocaleString()}<br/>占比: ${params.percent}%`,
    },
    legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data: pieData,
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' },
      },
    }],
  }, true)

  // 柱状图：盈亏对比
  const names = data.map(p => p.productCode || '')
  const profitValues = data.map(p => parseFloat((p.profitLoss || 0).toFixed(2)))
  const colors = profitValues.map(v => (v >= 0 ? '#f56c6c' : '#67c23a'))

  barChart?.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>浮动盈亏: ${p.value >= 0 ? '+' : ''}¥${p.value}`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => `¥${v}`, fontSize: 11 },
      splitLine: { lineStyle: { type: 'dashed' } },
    },
    series: [{
      type: 'bar',
      data: profitValues.map((v, i) => ({ value: v, itemStyle: { color: colors[i] } })),
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => (p.value >= 0 ? `+${p.value}` : `${p.value}`),
        fontSize: 11,
        color: '#606266',
      },
      barMaxWidth: 60,
    }],
  }, true)
}

// ==================== 实时行情订阅 ====================
let unsubscribe: (() => void) | null = null
onUnmounted(() => {
  unsubscribe?.()
  pieChart?.dispose()
  barChart?.dispose()
})

watch(positions, (newData) => {
  const codes = newData
    .map(p => p.productCode || '')
    .filter(Boolean)
  if (codes.length > 0) {
    unsubscribe?.()
    unsubscribe = subscribeMarket(codes, (ticks) => {
      realtimePrices.value = { ...realtimePrices.value, ...ticks }
    })
  }
}, { immediate: false })

// 实时价格更新后刷新图表
watch(displayPositions, () => {
  updateCharts()
}, { deep: true })

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    const [accountRes, positionRes] = await Promise.all([
      accountApi.getByUserId(userId).catch(() => null),
      positionApi.getByUserId(userId).catch(() => null),
    ])

    if (accountRes) accountInfo.value = accountRes.data.data
    if (positionRes) {
      const raw = positionRes.data.data || []
      // 兼容旧数据：从 productName 提取股票代码
      positions.value = raw.map((p: any) => {
        const code = p.productCode || (p.productName?.match(/\((\d{6})\)/)?.[1] ?? '')
        return { ...p, productCode: code }
      })
    }

    // 等 DOM 渲染后初始化图表
    await nextTick()
    initCharts()
    updateCharts()
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row { flex-shrink: 0; }

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-card--blue .stat-icon   { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-card--purple .stat-icon { background: linear-gradient(135deg, #7c3aed, #a78bfa); }
.stat-card--green .stat-icon  { background: linear-gradient(135deg, #10b981, #34d399); }
.stat-card--orange .stat-icon { background: linear-gradient(135deg, #f59e0b, #fbbf24); }

.stat-info { flex: 1; min-width: 0; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 4px; }
.stat-value { font-size: 20px; font-weight: bold; color: #303133; }

.profit { color: #f56c6c; }
.loss   { color: #67c23a; }

.position-card { flex: 1; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 实时标记 */
.realtime-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  font-size: 12px;
  color: #67c23a;
  font-weight: normal;
}

.realtime-price { color: #409eff; font-weight: bold; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.2; }
}
.blink { animation: blink 1.5s infinite; }

/* 图表区 */
.chart-row { margin-bottom: 8px; }

.chart-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  text-align: center;
}

.chart-container {
  width: 100%;
  height: 260px;
}
</style>
