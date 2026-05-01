<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>金融交易系统</h2>
        <p>{{ isRegister ? '用户注册' : '用户登录' }}</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-if="!isRegister"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-else
        ref="regFormRef"
        :model="regForm"
        :rules="regRules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="regForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="regForm.password"
            type="password"
            placeholder="请输入密码（6位以上）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="regForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span v-if="!isRegister">
          没有账号？
          <el-link type="primary" @click="goRegister">去注册</el-link>
        </span>
        <span v-else>
          已有账号？
          <el-link type="primary" @click="goLogin">去登录</el-link>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isRegister = computed(() => router.currentRoute.value.query?.redirect === 'register')

function goRegister() {
  router.push({ path: '/login', query: { redirect: 'register' } })
}

function goLogin() {
  router.push('/login')
}

// ==================== 登录 ====================
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误已在 axios 拦截器处理
  } finally {
    loading.value = false
  }
}

// ==================== 注册 ====================
const regFormRef = ref<FormInstance>()

const regForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
})

function validateConfirm(_rule: any, value: string, callback: any) {
  if (value !== regForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const regRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少3位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await regFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userApi.register({
      username: regForm.value.username,
      password: regForm.value.password,
    })
    ElMessage.success('注册成功，请登录')
    goLogin()
  } catch {
    // 错误已在 axios 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  margin: 0 0 8px;
  color: #1a1a2e;
  font-size: 24px;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.login-footer {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 16px;
}
</style>
