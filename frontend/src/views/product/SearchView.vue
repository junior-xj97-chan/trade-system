<template>
  <div class="search-view">
    <el-card>
      <template #header>
        <span>商品搜索（ES + IK 分词）</span>
      </template>

      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索商品名称或代码..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 筛选区 -->
      <div class="filter-row">
        <el-select v-model="filter.market" placeholder="市场" clearable style="width: 120px">
          <el-option label="沪市" value="SH" />
          <el-option label="深市" value="SZ" />
          <el-option label="港股" value="HK" />
          <el-option label="美股" value="US" />
        </el-select>
        <el-input v-model="filter.minPrice" placeholder="最低价" style="width: 120px" />
        <span style="color: #909399">—</span>
        <el-input v-model="filter.maxPrice" placeholder="最高价" style="width: 120px" />
        <el-select v-model="filter.sortBy" placeholder="排序" style="width: 140px">
          <el-option label="默认" value="" />
          <el-option label="价格升序" value="price_asc" />
          <el-option label="价格降序" value="price_desc" />
          <el-option label="涨跌幅升序" value="changePercent_asc" />
          <el-option label="涨跌幅降序" value="changePercent_desc" />
        </el-select>
        <el-button type="primary" @click="handleSearch">应用筛选</el-button>
      </div>

      <el-divider />

      <!-- 结果 -->
      <el-table v-loading="loading" :data="results" stripe>
        <el-table-column prop="productCode" label="代码" width="140" />
        <el-table-column prop="productName" label="名称" />
        <el-table-column label="市场" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.exchangeCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">¥{{ row.currentPrice?.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column label="涨跌幅" width="100" align="right">
          <template #default="{ row }">
            <span :class="(row.changePercent ?? 0) >= 0 ? 'profit' : 'loss'">
              {{ (row.changePercent ?? 0) >= 0 ? '+' : '' }}{{ (row.changePercent ?? 0).toFixed(2) }}%
            </span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && results.length === 0 && searched" description="未找到匹配的商品" />
      <el-empty v-if="!loading && results.length === 0 && !searched" description="输入关键词开始搜索" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="searchReq.page"
        v-model:page-size="searchReq.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { searchApi } from '@/api'
import { ElMessage } from 'element-plus'
import type { Product } from '@/types'
import { Search } from '@element-plus/icons-vue'

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const results = ref<Product[]>([])
const total = ref(0)

const filter = reactive({
  market: '',
  minPrice: '',
  maxPrice: '',
  sortBy: '',
})

const searchReq = reactive({
  page: 1,
  pageSize: 10,
})

async function handleSearch() {
  if (!keyword.value.trim() && !filter.market) {
    ElMessage.warning('请输入搜索关键词或选择市场')
    return
  }

  loading.value = true
  searched.value = true
  try {
    // 解析 sortBy 格式：price_asc -> sortField=price, sortOrder=asc
    const sortParts = (filter.sortBy || '').split('_')
    const sortField = sortParts[0] || undefined
    // sortOrder 只接受 'asc' | 'desc'
    const rawOrder = sortParts[1]
    const sortOrder: 'asc' | 'desc' | undefined =
      rawOrder === 'asc' || rawOrder === 'desc' ? rawOrder : undefined

    // 转换前端分页（从1开始）到后端分页（从0开始）
    const backendPage = searchReq.page - 1

    const res = await searchApi.search({
      keyword: keyword.value || undefined,
      market: filter.market || undefined,
      minPrice: filter.minPrice ? Number(filter.minPrice) : undefined,
      maxPrice: filter.maxPrice ? Number(filter.maxPrice) : undefined,
      sortField: sortField,
      sortOrder: sortOrder,
      page: backendPage,
      pageSize: searchReq.pageSize,
    })

    results.value = res.data.data.records || res.data.data.products || []
    total.value = res.data.data.total || 0
  } catch (e: any) {
    console.error('搜索失败:', e)
    ElMessage.error('搜索失败: ' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-view {
  height: 100%;
}

.search-box {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profit { color: #f56c6c; }
.loss { color: #67c23a; }
</style>
