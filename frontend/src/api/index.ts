import http from './axios'
import type {
  R,
  User,
  LoginReq,
  LoginResp,
  Account,
  Product,
  Order,
  Position,
  PageResult,
  PageReq,
} from '@/types'

const UPLOAD_BASE = ''   // 上传走 /user/avatar，由 Vite 代理到 user-service（绕过 Gateway）

async function uploadFetch(path: string, body: FormData, token: string): Promise<{ data: R<string> }> {
  const res = await fetch(UPLOAD_BASE + path, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body,
  })
  return res.json()
}

// ========== 用户 ==========
export const userApi = {
  login(data: LoginReq) {
    return http.post<R<LoginResp>>('/user/login', data)
  },
  register(data: LoginReq & { email?: string }) {
    return http.post<R>('/user/register', data)
  },
  getInfo(userId: number) {
    return http.get<R<User>>(`/user/${userId}`)
  },
  /** 修改个人资料 */
  updateProfile(data: {
    nickname?: string
    email?: string
    phone?: string
    gender?: 0 | 1 | 2
    avatar?: string
  }) {
    return http.put<R>('/user/profile', data)
  },
  /** 修改密码 */
  changePassword(data: { oldPassword: string; newPassword: string }) {
    return http.put<R>('/user/password', data)
  },
  /** 退出登录（使服务端 Token 失效） */
  logout() {
    return http.post<R>('/user/logout')
  },
  /** 上传头像，返回头像 URL（绕过 Gateway，直接调用 user-service） */
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem('token') || ''
    return uploadFetch('/user/avatar', formData, token)
  },
}

// ========== 账户 ==========
export const accountApi = {
  getByUserId(userId: number) {
    return http.get<R<Account>>(`/account/${userId}`)
  },
  create(userId: number) {
    return http.post<R>('/account/create', { userId })
  },
  recharge(userId: number, amount: number) {
    return http.post<R>(`/account/recharge?userId=${userId}&amount=${amount}`)
  },
}

// ========== 商品 ==========
export const productApi = {
  page(params: PageReq & { keyword?: string }) {
    return http.get<R<PageResult<Product>>>('/product/page', { params })
  },
  getById(id: number) {
    return http.get<R<Product>>(`/product/${id}`)
  },
  getByCode(code: string) {
    return http.get<R<Product>>(`/product/code/${code}`)
  },
}

// ========== 订单 ==========
export const orderApi = {
  create(data: {
    userId: number
    productId: number
    productName: string
    productCode: string
    direction: 1 | 2
    price: number
    quantity: number
  }) {
    return http.post<R<Order>>('/order/create', data)
  },
  getById(id: string | number) {
    return http.get<R<Order>>(`/order/${id}`)
  },
  // 分页查询当前用户的订单
  page(params: PageReq & { userId: number }) {
    return http.get<R<PageResult<Order>>>('/order/page', { params })
  },
  pay(id: string | number) {
    return http.put<R>(`/order/pay/${id}`)
  },
  sell(id: string | number) {
    return http.put<R>(`/order/sell/${id}`)
  },
  cancel(id: string | number) {
    return http.put<R>(`/order/cancel/${id}`)
  },
}

// ========== 持仓（属于 trade-service） ==========
export const positionApi = {
  getByUserId(userId: number) {
    return http.get<R<Position[]>>(`/trade/position/user/${userId}`)
  },
  getDetail(userId: number, productId: number) {
    return http.get<R<Position>>(`/trade/position/user/${userId}/product/${productId}`)
  },
  page(params: PageReq & { userId?: number }) {
    return http.get<R<PageResult<Position>>>(`/trade/position/page`, { params })
  },
}


