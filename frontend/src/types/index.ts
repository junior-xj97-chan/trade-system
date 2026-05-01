// ========== 通用响应 ==========
export interface R<T = any> {
  code: number
  message: string
  data: T
}

// ========== 用户 ==========
export interface User {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  gender?: 0 | 1 | 2       // 0=未知 1=男 2=女
  avatar?: string
  status?: number
  createTime?: string
}

export interface LoginReq {
  username: string
  password: string
}

export interface LoginResp {
  token: string
  userId: number
  username: string
}

// ========== 账户 ==========
export interface Account {
  id: number
  userId: number
  balance: number          // 可用余额
  frozenAmount: number      // 冻结金额
  createTime: string
  updateTime: string
}

// ========== 商品 ==========
export interface Product {
  id: number
  productCode: string      // 股票代码，如 600519
  productName: string      // 股票名称
  currentPrice: number     // 当前价格
  market: string           // 市场：SH/SZ/HK/US
  changePercent: number    // 涨跌幅 %
  category: number         // 分类：1=股票 2=基金 3=商品 4=其他
  status: number           // 1=正常 0=停牌
  createTime: string
  updateTime: string
}

// ========== 订单 ==========
export type OrderDirection = 1 | 2      // 1=买入 2=卖出
export type OrderStatus = 1 | 2 | 3 | 4 // 1=待支付 2=已支付 3=已完成 4=已取消

export interface Order {
  id: string | number   // 雪花ID，大数字用 string 避免精度丢失
  userId: string | number
  productId: string | number
  productCode: string
  productName: string
  direction: OrderDirection
  price: number            // 下单价格
  quantity: number         // 数量
  amount: number           // 总金额
  status: OrderStatus
  createTime: string
  updateTime: string
}

// ========== 持仓 ==========
export type PositionStatus = 0 | 1      // 0=已清仓 1=正常（持仓中）

export interface Position {
  id: number
  userId: number
  productId: number
  productCode: string
  productName: string
  quantity: number         // 持有数量
  avgCost: number         // 平均成本
  currentPrice: number    // 当前价格
  profitLoss: number      // 浮动盈亏 = (currentPrice - avgCost) * quantity
  profitLossPercent: number // 盈亏比例 %
  status: PositionStatus
  createTime: string
  updateTime: string
}

// ========== 分页 ==========
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageReq {
  current?: number
  size?: number
}

// ========== 搜索 ==========
// 后端 SearchRequest 字段
export interface SearchReq {
  keyword?: string
  market?: string           // 市场代码：SH/SZ/HK/US
  exchangeCode?: string     // 交易所代码（与 market 相同）
  productType?: string      // 商品类型：stock/fund/bond/future
  minPrice?: number
  maxPrice?: number
  sortField?: string        // 排序字段：price/changePercent
  sortOrder?: 'asc' | 'desc'
  sortBy?: string           // 排序简写：price_asc（会被解析为 sortField + sortOrder）
  page?: number             // 页码（后端从0开始）
  size?: number
  pageSize?: number         // 每页大小（别名）
}

// 后端 SearchResponse 字段
export interface SearchResp {
  total: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
  products: Product[]        // 商品列表（兼容旧接口）
  records: Product[]        // 商品列表（前端兼容）
  took: number              // 搜索耗时(ms)
}
