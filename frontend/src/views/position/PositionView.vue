<template>
  <div class="position-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>持仓管理</span>
          <el-button type="primary" size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="displayData" stripe>
        <el-table-column prop="productCode" label="代码" width="130" />
        <el-table-column prop="productName" label="名称" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column label="成本" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.avgCost) }}</template>
        </el-table-column>
        <el-table-column label="现价" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.currentPrice) }}</template>
        </el-table-column>
        <el-table-column label="市值" width="100" align="right">
          <template #default="{ row }">
            {{ formatMoney(row.currentPrice * row.quantity) }}
          </template>
        </el-table-column>
        <el-table-column label="浮动盈亏" width="120" align="right">
          <template #default="{ row }">
            <span :class="(row.profitLoss || 0) >= 0 ? 'profit' : 'loss'">
              {{ (row.profitLoss || 0) >= 0 ? '+' : '' }}¥{{ formatMoney(row.profitLoss || 0).replace('¥', '') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="盈亏%" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.profitLossPercent >= 0 ? 'profit' : 'loss'">
              {{ row.profitLossPercent >= 0 ? '+' : '' }}{{ row.profitLossPercent.toFixed(2) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '持仓中' : '已清仓' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="success"
              size="small"
              @click="handleSell(row)"
            >
              卖出
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无持仓记录" />

      <!-- 总览统计 -->
      <div v-if="tableData.length > 0" class="summary">
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="summary-item">
              <span class="label">总持仓市值</span>
              <span class="value">{{ formatMoney(totalMarket) }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-item">
              <span class="label">总浮动盈亏</span>
              <span class="value" :class="totalProfitLoss >= 0 ? 'profit' : 'loss'">
                {{ totalProfitLoss >= 0 ? '+' : '' }}¥{{ formatMoney(totalProfitLoss).replace('¥', '') }}
              </span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-item">
              <span class="label">持仓品种数</span>
              <span class="value">{{ tableData.filter(p => p.status === 1).length }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 卖出对话框 -->
    <el-dialog v-model="sellDialogVisible" title="卖出" width="420px" :close-on-click-modal="false">
      <el-form :model="sellForm" label-width="80px">
        <el-form-item label="股票">
          <el-input
            :model-value="sellPosition ? `${sellPosition.productName} (${sellPosition.productCode})` : ''"
            disabled
          />
        </el-form-item>
        <el-form-item label="持仓数量">
          <el-input :model-value="sellPosition ? `${sellPosition.quantity} 股` : ''" disabled />
        </el-form-item>
        <el-form-item label="成本价格">
          <el-input :model-value="sellPosition ? `¥${sellPosition.avgCost.toFixed(2)}` : ''" disabled />
        </el-form-item>
        <el-form-item label="当前价格">
          <el-input
            :model-value="sellProduct ? `¥${sellProduct.currentPrice.toFixed(2)}` : '加载中...'"
            disabled
          />
        </el-form-item>
        <el-form-item label="卖出数量" required>
          <el-input-number
            v-model="sellForm.quantity"
            :min="minQuantity"
            :max="maxQuantity"
            :step="quantityStep"
            :step-strictly="isAShare"
            style="width: 160px"
          />
          <span v-if="isAShare" class="unit-hint">1手 = 100股</span>
        </el-form-item>
        <el-form-item v-if="isAShare" class="hint-row">
          <el-alert type="info" :closable="false" show-icon>
            <template #title>A股卖出数量必须为100的整数倍</template>
          </el-alert>
        </el-form-item>
        <el-form-item label="预计金额">
          <span style="color: #409eff; font-weight: bold">
            ¥{{ sellProduct ? (sellProduct.currentPrice * sellForm.quantity).toFixed(2) : '0.00' }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sellDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="selling" @click="handleSellSubmit">
          确认卖出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted, onUnmounted } from 'vue'
import { positionApi, orderApi, productApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Position, Product } from '@/types'
import { subscribeMarket } from '@/utils/marketPoller'
import type { StockTick } from '@/api/market'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref<Position[]>([])

// 实时价格缓存：code -> StockTick（不修改 tableData，避免死循环）
const realtimePrices = ref<Record<string, StockTick>>({})

// 表格展示数据：合并静态持仓 + 实时价格
const displayData = computed(() => {
  return tableData.value.map(p => {
    const tick = realtimePrices.value[p.productCode]
    if (tick) {
      const newPrice = tick.price
      const quantity = p.quantity
      const avgCost = p.avgCost
      const profitLoss = (newPrice - avgCost) * quantity
      const profitLossPercent = avgCost > 0 ? (profitLoss / (avgCost * quantity)) * 100 : 0
      return {
        ...p,
        currentPrice: newPrice,
        profitLoss,
        profitLossPercent,
      }
    }
    return p
  })
})

// 卖出对话框状态
const sellDialogVisible = ref(false)
const sellPosition = ref<Position | null>(null)
const sellProduct = ref<Product | null>(null)
const sellForm = reactive({ quantity: 100 })
const selling = ref(false)

// 是否A股（SH/SZ）：数量必须为100的整数倍
const isAShare = computed(() => {
  const m = sellProduct.value?.market
  return m === 'SH' || m === 'SZ'
})

const minQuantity = computed(() => (isAShare.value ? 100 : 1))
const quantityStep = computed(() => (isAShare.value ? 100 : 1))
const maxQuantity = computed(() => sellPosition.value?.quantity ?? 0)

/**
 * 格式化金额
 */
function formatMoney(value: number | string): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (Number.isInteger(num)) {
    return '¥' + num
  }
  return '¥' + parseFloat(num.toFixed(2)).toString()
}

/**
 * 加载持仓列表
 */
async function loadData() {
  const userId = userStore.userInfo?.id
  if (!userId) return

  loading.value = true
  try {
    const res = await positionApi.getByUserId(userId)
    const raw = res.data.data || []
    // 优先用后端返回的 productCode，没有再从 productName 提取（兼容旧数据/未执行SQL迁移）
    tableData.value = raw.map((p: any) => {
      const code = p.productCode || (p.productName?.match(/\((\d{6})\)/) ? p.productName.match(/\((\d{6})\)/)[1] : '')
      return { ...p, productCode: code }
    })
  } catch {
    ElMessage.error('加载持仓失败')
  } finally {
    loading.value = false
  }
}

/**
 * 点击"卖出"按钮：打开卖出对话框
 */
async function handleSell(position: Position) {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录')
    return
  }

  sellPosition.value = position
  sellProduct.value = null
  sellDialogVisible.value = true

  // 先设一个默认值，A股后面会改成100
  sellForm.quantity = 1

  // 查商品详情（拿 market 字段做A股校验，同时拿最新价格）
  try {
    const res = await productApi.getById(position.productId)
    sellProduct.value = res.data.data
    // A股默认100，其他默认1
    sellForm.quantity = isAShare.value ? 100 : 1
  } catch {
    ElMessage.error('获取商品信息失败')
    sellDialogVisible.value = false
  }
}

/**
 * 提交卖出订单
 */
async function handleSellSubmit() {
  if (!sellPosition.value || !sellProduct.value || !userStore.userInfo?.id) return

  // A股数量校验
  if (isAShare.value && sellForm.quantity % 100 !== 0) {
    ElMessage.warning('A股卖出数量必须为100的整数倍')
    return
  }

  // 持仓不足校验
  if (sellForm.quantity > (sellPosition.value.quantity ?? 0)) {
    ElMessage.warning(`卖出数量不能超过持仓数量（${sellPosition.value.quantity} 股）`)
    return
  }

  selling.value = true
  try {
    await orderApi.create({
      userId: userStore.userInfo.id,
      productId: sellProduct.value.id,
      productName: sellProduct.value.productName,
      productCode: sellProduct.value.productCode,
      direction: 2, // 卖出
      price: sellProduct.value.currentPrice,
      quantity: sellForm.quantity,
    })
    ElMessage.success('卖出订单创建成功，请前往订单页支付')
    sellDialogVisible.value = false
    loadData() // 刷新持仓列表
  } catch {
    // 错误已由 axios 拦截器统一处理
  } finally {
    selling.value = false
  }
}

const totalMarket = computed(() =>
  displayData.value.reduce((sum, p) => sum + p.currentPrice * p.quantity, 0),
)
const totalProfitLoss = computed(() =>
  displayData.value.reduce((sum, p) => sum + (p.profitLoss || 0), 0),
)

onMounted(loadData)

// 订阅持仓股票的实时行情
let unsubscribe: (() => void) | null = null
onUnmounted(() => { unsubscribe?.() })

// 当持仓数据加载完成后，订阅实时行情
watch(tableData, (newData) => {
  // 收集所有持仓的股票代码（loadData 已保证 productCode 有值）
  const codes = newData.map(p => p.productCode).filter(Boolean)
  if (codes.length > 0) {
    unsubscribe?.()
    unsubscribe = subscribeMarket(codes, (ticks) => {
      realtimePrices.value = { ...realtimePrices.value, ...ticks }
    })
  }
}, { immediate: false })
</script>

<style scoped>
.position-view { height: 100%; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary {
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item .label {
  color: #909399;
  font-size: 13px;
}

.summary-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.profit { color: #f56c6c; }
.loss { color: #67c23a; }

.unit-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}

.hint-row {
  margin-bottom: 0;
}
.hint-row :deep(.el-form-item__content) {
  line-height: 1;
}
</style>
