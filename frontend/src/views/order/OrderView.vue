<template>
  <div class="order-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-button type="primary" size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-row">
        <el-select v-model="filter.status" placeholder="订单状态" clearable style="width: 140px">
          <el-option label="待支付" :value="1" />
          <el-option label="已支付" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <el-select v-model="filter.direction" placeholder="交易方向" clearable style="width: 120px">
          <el-option label="买入" :value="1" />
          <el-option label="卖出" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadData">筛选</el-button>
      </div>

      <el-divider />

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column label="订单ID" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="productCode" label="代码" width="130" />
        <el-table-column prop="productName" label="名称" />
        <el-table-column label="方向" width="70">
          <template #default="{ row }">
            <el-tag :type="row.direction === 1 ? 'danger' : 'success'" size="small">
              {{ row.direction === 1 ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="90" align="right">
          <template #default="{ row }">{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              type="info"
              size="small"
              @click="router.push('/order/' + String(row.id))"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="primary"
              size="small"
              @click="handlePay(row)"
            >
              支付
            </el-button>
            <el-button
              v-if="row.status === 1 || row.status === 2"
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无订单" />

      <el-pagination
        v-model:current-page="pageQuery.current"
        v-model:page-size="pageQuery.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, watch, inject, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Order, OrderStatus } from '@/types'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const tableData = ref<Order[]>([])
const total = ref(0)
const pageQuery = reactive({ current: 1, size: 10 })

const filter = reactive({ status: null as number | null, direction: null as number | null })

// 监听布局层发出的刷新信号（点击已激活菜单项时触发）
const refreshTrigger = inject<Ref<number>>('refreshTrigger')
if (refreshTrigger) {
  watch(refreshTrigger, () => loadData())
}

// 防止 onMounted 和 onActivated 在首次加载时重复调用 loadData
let isFirstActivate = true
onMounted(loadData)
onActivated(() => {
  if (isFirstActivate) {
    isFirstActivate = false
    return
  }
  loadData()
})

function statusText(status: OrderStatus) {
  const map: Record<OrderStatus, string> = { 1: '待支付', 2: '已支付', 3: '已完成', 4: '已取消' }
  return map[status]
}

function statusTagType(status: OrderStatus) {
  const map: Record<OrderStatus, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] as any
}

/**
 * 格式化金额：整数不显示小数位，有小数则保留
 * 例如：16800 → ¥16800，4550.50 → ¥4550.50
 */
function formatMoney(value: number | string): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  // 如果是整数，直接返回
  if (Number.isInteger(num)) {
    return '¥' + num
  }
  // 否则保留实际小数位（最多2位）
  return '¥' + parseFloat(num.toFixed(2)).toString()
}

async function loadData() {
  const userId = userStore.userInfo?.id
  if (!userId) return

  loading.value = true
  try {
    const res = await orderApi.page({
      ...pageQuery,
      userId,
      ...(filter.status ? { status: filter.status } : {}),
      ...(filter.direction ? { direction: filter.direction } : {}),
    } as any)
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } catch {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

async function handlePay(order: Order) {
  try {
    await orderApi.pay(order.id)
    ElMessage.success('支付成功')
    loadData()
  } catch (err: any) {
    console.error('【支付失败】orderId=', order.id, err)
    ElMessage.error(err?.response?.data?.message || err?.message || '支付失败，请重试')
  }
}

async function handleCancel(order: Order) {
  try {
    await orderApi.cancel(order.id)
    ElMessage.success('取消成功')
    loadData()
  } catch (err: any) {
    console.error('【取消失败】orderId=', order.id, err)
    ElMessage.error(err?.response?.data?.message || err?.message || '取消失败，请重试')
  }
}

onMounted(loadData)
onActivated(loadData)
</script>

<style scoped>
.order-view { height: 100%; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
