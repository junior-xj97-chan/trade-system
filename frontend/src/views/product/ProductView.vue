<template>
  <div class="product-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品列表</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索代码/名称"
              clearable
              style="width: 200px"
              @clear="loadData"
              @keyup.enter="loadData"
            >
              <template #prefix><Search /></template>
            </el-input>
            <el-button type="primary" @click="loadData">查询</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="displayData" stripe>
        <el-table-column prop="productCode" label="代码" width="140" />
        <el-table-column prop="productName" label="名称" />
        <el-table-column label="市场" width="80">
          <template #default="{ row }">
            <el-tag :type="marketTagType(row.market)" size="small">
              {{ row.market }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">¥{{ (row.currentPrice ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="涨跌幅" width="100" align="right">
          <template #default="{ row }">
            <span :class="(row.changePercent ?? 0) >= 0 ? 'profit' : 'loss'">
              {{ (row.changePercent ?? 0) >= 0 ? '+' : '' }}{{ (row.changePercent ?? 0).toFixed(2) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停牌' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleBuy(row)">买入</el-button>
            <el-button type="success" size="small" @click="handleSell(row)">卖出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageQuery.current"
        v-model:page-size="pageQuery.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 买卖对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogType === 'buy' ? '买入' : '卖出'" width="400px">
      <el-form :model="orderForm" label-width="80px">
        <el-form-item label="商品">
          <el-input :model-value="currentProduct?.productName + ' (' + currentProduct?.productCode + ')'" disabled />
        </el-form-item>
        <el-form-item label="当前价格">
          <el-input :model-value="'¥' + (currentProduct?.currentPrice?.toFixed(2) ?? '--')" disabled />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number
            v-model="orderForm.quantity"
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
            <template #title>A股买卖数量必须为100的整数倍</template>
          </el-alert>
        </el-form-item>
        <el-form-item v-if="dialogType === 'sell'" class="hint-row">
          <el-alert type="warning" :closable="false">
            <template #title>可卖数量：{{ sellableQuantity }} 股</template>
          </el-alert>
        </el-form-item>
        <el-form-item label="金额">
          <span style="color: #409eff; font-weight: bold">
            ¥{{ ((currentProduct?.currentPrice ?? 0) * orderForm.quantity).toFixed(2) }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确认{{ dialogType === 'buy' ? '买入' : '卖出' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted, onUnmounted, onActivated, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { productApi, orderApi, accountApi, positionApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Product } from '@/types'
import { Search } from '@element-plus/icons-vue'
import { subscribeMarket } from '@/utils/marketPoller'
import type { StockTick } from '@/api/market'

const userStore = useUserStore()
const route = useRoute()
const loading = ref(false)
const keyword = ref('')

// 监听左侧菜单刷新信号
const refreshTrigger = inject<Ref<number>>('refreshTrigger')
if (refreshTrigger) {
  watch(refreshTrigger, () => loadData())
}

// keep-alive 激活时重新加载（首次激活不重复加载）
let isFirstActivate = true
onActivated(() => {
  if (isFirstActivate) { isFirstActivate = false; return }
  loadData()
})

const tableData = ref<Product[]>([])
const total = ref(0)
const pageQuery = reactive({ current: 1, size: 10 })

// 实时价格缓存：code -> StockTick（不修改 tableData，避免死循环）
const realtimePrices = ref<Record<string, StockTick>>({})

// 表格展示数据：合并静态数据 + 实时价格
const displayData = computed(() => {
  return tableData.value.map(p => {
    const tick = realtimePrices.value[p.productCode]
    if (tick) {
      return {
        ...p,
        currentPrice: tick.price,
        changePercent: tick.changePercent,
      }
    }
    return p
  })
})

// 买卖对话框
const dialogVisible = ref(false)
const dialogType = ref<'buy' | 'sell'>('buy')
const currentProduct = ref<Product | null>(null)
const orderForm = reactive({ quantity: 100 })
const submitting = ref(false)

// 账户余额（买入时计算最大可买数量）
const accountBalance = ref(0)
// 可卖持仓（卖出时限制最大可卖数量）
const sellableQuantity = ref(0)

// 是否A股（SH/SZ）：必须100股整数倍
const isAShare = computed(() => {
  const m = currentProduct.value?.market
  return m === 'SH' || m === 'SZ'
})

// 最小数量：A股=100，其他=1
const minQuantity = computed(() => isAShare.value ? 100 : 1)

// 每次步进：A股=100，其他=1
const quantityStep = computed(() => isAShare.value ? 100 : 1)

// 最大可买/可卖数量
const maxQuantity = computed(() => {
  const price = currentProduct.value?.currentPrice ?? 0
  if (!price) return 0
  if (dialogType.value === 'buy') {
    // 买入：最多用账户余额买多少股（A股需取整到100）
    const max = Math.floor(accountBalance.value / price)
    return isAShare.value ? Math.floor(max / 100) * 100 : max
  } else {
    // 卖出：最多可卖持仓（A股需取整到100）
    const max = sellableQuantity.value
    return isAShare.value ? Math.floor(max / 100) * 100 : max
  }
})

function marketTagType(market: string) {
  const map: Record<string, string> = { SH: 'primary', SZ: 'success', HK: 'warning', US: 'danger' }
  return (map[market] || '') as any
}

async function loadData() {
  loading.value = true
  try {
    const res = await productApi.page({
      current: pageQuery.current,
      size: pageQuery.size,
      keyword: keyword.value || undefined,
    })
    // res 是 AxiosResponse, res.data 是 R<Page<Product>>, res.data.data 才是分页对象
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } catch {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

async function handleBuy(product: Product) {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录')
    return
  }
  currentProduct.value = product
  dialogType.value = 'buy'
  orderForm.quantity = 100
  // 查询账户余额，计算最大可买数量
  try {
    const res = await accountApi.getByUserId(userStore.userInfo.id)
    accountBalance.value = res.data.data?.balance ?? 0
  } catch {
    accountBalance.value = 0
  }
  dialogVisible.value = true
}

async function handleSell(product: Product) {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录')
    return
  }
  currentProduct.value = product
  dialogType.value = 'sell'
  orderForm.quantity = 100
  // 查询持仓数量，计算最大可卖数量
  try {
    const res = await positionApi.getDetail(userStore.userInfo.id, product.id)
    sellableQuantity.value = res.data.data?.quantity ?? 0
  } catch {
    sellableQuantity.value = 0
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!currentProduct.value || !userStore.userInfo?.id) return

  // A股数量校验：必须100的整数倍
  if (isAShare.value && orderForm.quantity % 100 !== 0) {
    ElMessage.warning('A股买卖数量必须为100的整数倍')
    return
  }

  // 余额不足
  if (dialogType.value === 'buy') {
    const totalAmount = (currentProduct.value.currentPrice ?? 0) * orderForm.quantity
    if (totalAmount > accountBalance.value) {
      ElMessage.warning(`余额不足，当前可用余额 ¥${accountBalance.value.toFixed(2)}`)
      return
    }
  }

  // 持仓不足
  if (dialogType.value === 'sell' && orderForm.quantity > sellableQuantity.value) {
    ElMessage.warning(`持仓不足，当前可卖 ${sellableQuantity.value} 股`)
    return
  }

  submitting.value = true
  try {
    await orderApi.create({
      userId: userStore.userInfo.id,
      productId: currentProduct.value.id,
      productName: currentProduct.value.productName,
      productCode: currentProduct.value.productCode,
      direction: dialogType.value === 'buy' ? 1 : 2,
      price: currentProduct.value.currentPrice,
      quantity: orderForm.quantity,
    })
    ElMessage.success(`${dialogType.value === 'buy' ? '买入' : '卖出'}订单创建成功`)
    dialogVisible.value = false
  } catch {
    // 错误已统一处理
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)

// 监听 URL 参数 ?sell=产品ID，自动弹出卖出对话框（从订单详情页跳转过来）
watch(() => route.query.sell, (sellId) => {
  if (!sellId) return
  // 先从列表里找
  let product = tableData.value.find(p => String(p.id) === String(sellId))
  if (product) {
    handleSell(product)
  } else {
    // 列表里没有（还没加载完），直接从接口查
    productApi.getById(Number(sellId)).then(res => {
      const p = res.data.data
      if (p) handleSell(p)
    }).catch(() => {})
  }
}, { immediate: true })

// 订阅实时行情
let unsubscribe: (() => void) | null = null
onUnmounted(() => {
  unsubscribe?.()
})

// 当表格数据加载完成后，自动订阅实时行情
watch(tableData, (newData) => {
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
.product-view {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 8px;
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
