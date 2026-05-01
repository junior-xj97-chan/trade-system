<template>
  <div class="account-view">
    <el-row :gutter="16">
      <!-- 账户信息 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>账户信息</span>
            <el-button type="primary" size="small" @click="loadData">刷新</el-button>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">
              {{ userStore.userInfo?.id ?? '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ userStore.userInfo?.username ?? '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="可用余额">
              <span class="money">¥{{ accountInfo?.balance?.toFixed(2) ?? '--' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="冻结金额">
              <span class="frozen">¥{{ accountInfo?.frozenAmount?.toFixed(2) ?? '--' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 充值 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>账户充值</template>

          <el-form :model="rechargeForm" label-width="80px">
            <el-form-item label="充值金额" required>
              <el-input-number v-model="rechargeForm.amount" :min="1" :max="1000000" placeholder="请输入充值金额" style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button @click="handleReset">重置</el-button>
              <el-button type="primary" :loading="recharging" :disabled="!rechargeForm.amount || rechargeForm.amount < 1" @click="handleRecharge">
                确认充值
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { accountApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Account } from '@/types'

const userStore = useUserStore()
const accountInfo = ref<Account | null>(null)
const recharging = ref(false)
const rechargeForm = reactive({ amount: undefined as number | undefined })

async function loadData() {
  const userId = userStore.userInfo?.id
  if (!userId) return

  try {
    const res = await accountApi.getByUserId(userId)
    accountInfo.value = res.data.data
  } catch {
    ElMessage.error('加载账户信息失败')
  }
}

async function handleRecharge() {
  const userId = userStore.userInfo?.id
  if (!userId) return

  // 校验：充值金额不能低于1
  if (!rechargeForm.amount || rechargeForm.amount < 1) {
    ElMessage.warning('充值金额不能低于 ¥1')
    return
  }

  recharging.value = true
  try {
    await accountApi.recharge(userId, rechargeForm.amount)
    ElMessage.success(`充值 ¥${rechargeForm.amount} 成功`)
    rechargeForm.amount = undefined  // 充值成功后清空
    loadData()
  } catch {
    // 错误已统一处理
  } finally {
    recharging.value = false
  }
}

function handleReset() {
  rechargeForm.amount = undefined
}

onMounted(loadData)
</script>

<style scoped>
.account-view { height: 100%; }

.money {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;
}

.frozen {
  color: #f56c6c;
  font-weight: bold;
}
</style>
