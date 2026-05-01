<template>
  <div class="order-detail">
    <!-- 顶部导航 -->
    <div class="detail-nav">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回订单列表
      </el-button>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：订单信息 -->
      <el-col :span="16">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>订单信息</span>
              <el-tag :type="statusTagType(detail?.status)" size="large">
                {{ statusText(detail?.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单编号">
              <span class="mono">{{ detail?.id }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="交易方向">
              <el-tag :type="detail?.direction === 1 ? 'danger' : 'success'" size="small">
                {{ detail?.direction === 1 ? '买入' : '卖出' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="股票代码">
              <span class="mono">{{ detail?.productCode }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="股票名称">
              {{ detail?.productName }}
            </el-descriptions-item>
            <el-descriptions-item label="下单价格">
              <span class="money">¥{{ (detail?.price ?? 0).toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="下单数量">
              {{ (detail?.quantity ?? 0).toLocaleString() }} 股
            </el-descriptions-item>
            <el-descriptions-item label="订单总额" :span="2">
              <span class="money total">¥{{ (detail?.amount ?? 0).toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ detail?.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ detail?.updateTime }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 状态流转时间线 -->
        <el-card class="timeline-card">
          <template #header>
            <span>订单状态</span>
          </template>
          <el-steps :active="activeStep" finish-status="success" align-center>
            <el-step title="创建订单" :description="detail?.createTime" />
            <el-step title="已支付" :description="paidDesc" />
            <el-step :title="detail?.direction === 1 ? '持仓已更新' : '资金已到账'" :description="doneDesc" />
          </el-steps>

          <!-- 状态说明 -->
          <el-alert
            v-if="detail?.status === 1"
            title="请尽快完成支付，支付后订单将进入待处理状态"
            type="warning"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          />
          <el-alert
            v-else-if="detail?.status === 2"
            title="支付成功，订单正在处理中..."
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          />
          <el-alert
            v-else-if="detail?.status === 3"
            :title="detail.direction === 1 ? '买入成功，持仓已增加' : '卖出成功，资金已到账'"
            type="success"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          />
          <el-alert
            v-else-if="detail?.status === 4"
            title="订单已取消"
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          />
        </el-card>
      </el-col>

      <!-- 右侧：操作区 -->
      <el-col :span="8">
        <!-- 待支付 -->
        <el-card v-if="detail?.status === 1" class="action-card" shadow="never">
          <template #header>
            <span>待支付订单</span>
          </template>
          <div class="action-body">
            <div class="amount-tip">
              需支付 <strong>¥{{ (detail?.amount ?? 0).toFixed(2) }}</strong>
            </div>
            <el-button type="primary" size="large" style="width: 100%" :loading="paying" @click="handlePay">
              立即支付
            </el-button>
            <el-button type="danger" text style="width: 100%; margin-top: 8px" @click="handleCancel">
              取消订单
            </el-button>
          </div>
        </el-card>

        <!-- 已支付（处理中） -->
        <el-card v-else-if="detail?.status === 2" class="action-card" shadow="never">
          <template #header>
            <span>处理中</span>
          </template>
          <div class="action-body">
            <el-icon color="#909399" size="40"><Loading /></el-icon>
            <p style="color: #909399; margin-top: 12px">订单已支付，等待系统处理...</p>
            <el-button type="danger" text style="width: 100%; margin-top: 16px" @click="handleCancel">
              申请取消
            </el-button>
          </div>
        </el-card>

        <!-- 已完成 -->
        <el-card v-else-if="detail?.status === 3" class="action-card" shadow="never">
          <template #header>
            <span>已完成</span>
          </template>
          <div class="action-body">
            <el-icon color="#67c23a" size="40"><CircleCheck /></el-icon>
            <p style="color: #67c23a; margin-top: 12px">
              {{ detail.direction === 1 ? '买入完成，持仓已增加' : '卖出完成，资金已到账' }}
            </p>
            <el-button
              v-if="detail.direction === 1"
              type="success"
              style="margin-top: 16px; width: 100%"
              @click="handleSell"
            >
              卖出此股票
            </el-button>
          </div>
        </el-card>

        <!-- 已取消 -->
        <el-card v-else-if="detail?.status === 4" class="action-card" shadow="never">
          <template #header>
            <span>已取消</span>
          </template>
          <div class="action-body">
            <el-icon color="#909399" size="40"><Close /></el-icon>
            <p style="color: #909399; margin-top: 12px">订单已取消</p>
          </div>
        </el-card>

        <!-- 温馨提示 -->
        <el-card class="tips-card">
          <template #header>
            <span>温馨提示</span>
          </template>
          <ul class="tips-list">
            <li>买入订单支付后，持仓将立即增加</li>
            <li>卖出订单支付后，资金将立即到账</li>
            <li>取消订单后，被冻结的资金将解冻</li>
            <li>如有疑问，请联系客服处理</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading, CircleCheck, Close } from '@element-plus/icons-vue'
import type { Order, OrderStatus } from '@/types'

const route = useRoute()
const router = useRouter()

const detail = ref<Order | null>(null)
const paying = ref(false)

const paidDesc = computed(() => {
  const s = detail.value?.status
  return s !== undefined && s >= 2 ? (detail.value?.updateTime || detail.value?.createTime) : ''
})
const doneDesc = computed(() => {
  return detail.value?.status === 3 ? detail.value?.updateTime : ''
})

const activeStep = computed(() => {
  if (!detail.value) return 0
  switch (detail.value.status) {
    case 1: return 0   // 待支付 - 第0步（创建订单）已完成，等待支付
    case 2: return 1   // 已支付 - 第1步激活
    case 3: return 3   // 已完成 - 所有步骤完成（finish-status 生效）
    case 4: return 0   // 已取消
    default: return 0
  }
})

function statusText(status: OrderStatus | undefined) {
  if (!status) return '--'
  const map: Record<OrderStatus, string> = { 1: '待支付', 2: '已支付', 3: '已完成', 4: '已取消' }
  return map[status]
}

function statusTagType(status: OrderStatus | undefined) {
  if (!status) return 'info'
  const map: Record<OrderStatus, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] as any
}

async function loadDetail() {
  const id = route.params.id as string
  if (!id) {
    ElMessage.error('订单ID为空')
    router.back()
    return
  }
  try {
    const res = await orderApi.getById(id)
    const order = res.data.data as any
    if (order) {
      detail.value = {
        ...order,
        id: String(order.id ?? ''),
        userId: Number(order.userId ?? 0),
        productId: Number(order.productId ?? 0),
        price: Number(order.price ?? 0),
        quantity: Number(order.quantity ?? 0),
        amount: Number(order.amount ?? 0),
      }
    }
  } catch (e: any) {
    console.error('【加载订单详情失败】', e)
    ElMessage.error('加载订单详情失败：' + (e?.message || '未知错误'))
    router.back()
  }
}

// 防止 watch + onActivated 同时触发导致重复请求
let loadingLock = false
async function loadDetailSafe() {
  if (loadingLock) return
  loadingLock = true
  try {
    await loadDetail()
  } finally {
    loadingLock = false
  }
}

async function handlePay() {
  if (!detail.value) return
  paying.value = true
  try {
    await orderApi.pay(detail.value.id)
    ElMessage.success('支付成功')
    await loadDetailSafe()
  } catch {
    // 错误已由 axios 拦截器统一处理
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  if (!detail.value) return
  try {
    await orderApi.cancel(detail.value.id)
    ElMessage.success('取消成功')
    await loadDetailSafe()
  } catch {
    // 错误已由 axios 拦截器统一处理
  }
}

function handleSell() {
  if (!detail.value) return
  // 跳转到商品列表页，并带上产品ID，让商品页自动弹出卖出对话框
  router.push({ path: '/product', query: { sell: String(detail.value.productId) } })
}

onMounted(loadDetailSafe)
// 监听路由参数变化（从 /order/1 跳转到 /order/2 时触发）
watch(() => route.params.id, (newId) => {
  if (newId) loadDetailSafe()
})
// 从其他页面切回时也重新加载（keep-alive 场景）
onActivated(loadDetailSafe)
</script>

<style scoped>
.order-detail {
  height: 100%;
  overflow-y: auto;
  padding: 0 4px;
}

.detail-nav {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 订单信息 */
.info-card {
  margin-bottom: 16px;
}

.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  color: #606266;
}

.money {
  color: #409eff;
  font-weight: bold;
}

.total {
  font-size: 18px;
}

/* 状态时间线 */
.timeline-card {
  margin-bottom: 16px;
}

/* 右侧操作卡片 */
.action-card {
  margin-bottom: 16px;
}

.action-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0 8px;
  text-align: center;
}

.amount-tip {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}

.amount-tip strong {
  font-size: 24px;
  color: #409eff;
}

/* 温馨提示 */
.tips-card :deep(.el-card__header) {
  font-size: 14px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #909399;
  font-size: 13px;
  line-height: 2;
}
</style>
