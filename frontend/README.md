# 金融交易系统 - Vue3 前端

> Vue 3 + TypeScript + Element Plus + Pinia + Vue Router

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| TypeScript | 5.7.x | 类型安全 |
| Vite | 6.x | 构建工具 |
| Element Plus | 2.9.x | UI 组件库 |
| Pinia | 2.3.x | 状态管理 |
| Vue Router | 4.5.x | 路由管理 |
| Axios | 1.7.x | HTTP 客户端 |

## 项目结构

```
src/
├── api/          # Axios 封装 + 接口方法
├── components/   # 通用组件
│   └── layout/  # 主布局（侧边栏 + 顶部栏）
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── types/        # TypeScript 类型定义
├── utils/        # 工具函数
└── views/        # 页面组件
    ├── dashboard/   # 工作台
    ├── product/      # 商品列表 + ES 搜索
    ├── position/     # 持仓管理
    ├── order/        # 订单管理
    └── account/      # 账户管理
```

## 快速启动

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
# 访问 http://localhost:5173

# 3. 类型检查
npm run type-check
```

## 接口代理

开发环境通过 Vite Proxy 将请求代理到后端 Gateway（`http://127.0.0.1:9000`），无需处理跨域。

| 前端路径 | 代理目标 |
|---------|---------|
| `/user/**` | Gateway → user-service |
| `/product/**` | Gateway → product-service |
| `/order/**` | Gateway → order-service |
| `/account/**` | Gateway → account-service |
| `/position/**` | Gateway → trade-service |
| `/trade/**` | Gateway → trade-service |
| `/api/search/**` | Gateway → search-service |

## 页面说明

| 页面 | 路由 | 功能 |
|------|------|------|
| 登录 | `/login` | 用户登录 |
| 工作台 | `/dashboard` | 账户概览 + 持仓汇总 |
| 商品列表 | `/product` | 分页查询 + 买入/卖出下单 |
| 商品搜索 | `/search` | ES + IK 分词搜索 |
| 持仓管理 | `/position` | 持仓明细 + 盈亏统计 |
| 订单管理 | `/order` | 订单列表 + 支付/取消 |
| 账户管理 | `/account` | 余额展示 + 充值 |

## 后端依赖

本前端依赖 trade-system 后端服务，请确保以下服务已启动：

- `gateway` (端口 9000)
- `user-service` (端口 9001)
- `order-service` (端口 9002)
- `trade-service` (端口 9003)
- `account-service` (端口 9004)
- `product-service` (端口 9005)
- `search-service` (端口 9006)
