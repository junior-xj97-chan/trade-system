<template>
  <el-container class="main-layout">
    <!-- 左侧导航 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area">
        <span v-if="!isCollapsed" class="logo-text">金融交易系统</span>
        <span v-else class="logo-text logo-text--short">金</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :router="true"
        @select="handleMenuSelect"
        background-color="#1a1a2e"
        text-color="#a0a0b0"
        active-text-color="#409eff"
        class="layout-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/product">
          <el-icon><Goods /></el-icon>
          <template #title>商品列表</template>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <template #title>商品搜索</template>
        </el-menu-item>
        <el-menu-item index="/position">
          <el-icon><TrendCharts /></el-icon>
          <template #title>持仓管理</template>
        </el-menu-item>
        <el-menu-item index="/order">
          <el-icon><List /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/account">
          <el-icon><Wallet /></el-icon>
          <template #title>账户管理</template>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="isCollapsed = !isCollapsed">
            <el-icon size="20"><Expand v-if="isCollapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="headerAvatarUrl" style="background: #409eff">
                <span v-if="!headerAvatarUrl">{{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}</span>
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Odometer, Goods, Search, TrendCharts, List, Wallet, User,
  Fold, Expand, ArrowDown,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title as string || '')

// 提供给子组件的刷新触发器（点击已激活菜单项时递增）
const refreshTrigger = ref(0)
provide('refreshTrigger', refreshTrigger)

function handleMenuSelect(index: string) {
  if (index === route.path) {
    // 点击已激活的菜单项，触发刷新
    refreshTrigger.value++
  }
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

// 头像完整 URL
const headerAvatarUrl = computed(() => {
  const avatar = userStore.userInfo?.avatar
  if (!avatar) return ''
  // /static 开头的地址由 Vite 直接代理到 user-service，不经过 /api(Gateway)
  if (avatar.startsWith('/static')) return avatar
  if (avatar.startsWith('/')) return '/api' + avatar
  return avatar
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  background: #f0f2f5;
}

.layout-aside {
  background: #1a1a2e;
  transition: width 0.3s;
  overflow-x: hidden;
  overflow-y: auto;
}

.layout-aside::-webkit-scrollbar {
  width: 0;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #2a2a4a;
}

.logo-text {
  color: #409eff;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logo-text--short {
  font-size: 20px;
}

.layout-menu {
  border-right: none;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #333;
}

.layout-main {
  padding: 16px;
  overflow-y: auto;
}
</style>
