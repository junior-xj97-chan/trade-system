<template>
  <div class="profile-page">
    <el-row :gutter="16">
      <!-- 左侧：头像 + 基本信息 -->
      <el-col :span="8">
        <el-card shadow="never" class="avatar-card">
          <div class="avatar-wrapper">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-avatar :size="100" :src="avatarUrl" style="cursor: pointer; background: linear-gradient(135deg, #409eff, #66b1ff)">
                <span v-if="!avatarUrl" class="avatar-letter">{{ avatarLetter }}</span>
                <div v-if="uploading" class="avatar-uploading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </div>
              </el-avatar>
            </el-upload>
            <div class="avatar-hint">点击更换头像</div>
          </div>
          <div class="user-id-row">
            <span class="label">用户ID</span>
            <span class="value">{{ userInfo?.id }}</span>
          </div>
          <div class="user-id-row">
            <span class="label">注册时间</span>
            <span class="value">{{ userInfo?.createTime?.split('T')[0] || '--' }}</span>
          </div>
          <el-divider />
          <div class="account-status">
            <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'" size="small">
              {{ userInfo?.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：资料编辑 + 修改密码 -->
      <el-col :span="16">
        <!-- 标签页 -->
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <!-- 个人资料 -->
            <el-tab-pane label="个人资料" name="profile">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="80px"
                class="profile-form"
              >
                <el-form-item label="用户名">
                  <el-input :model-value="userInfo?.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
                </el-form-item>
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="profileForm.gender">
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                    <el-radio :value="0">未知</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="profileSaving" @click="saveProfile">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 修改密码 -->
            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
                class="password-form"
              >
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码（6位以上）"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="passwordSaving" @click="changePassword">
                    确认修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 退出登录 -->
        <el-card shadow="never" style="margin-top: 16px">
          <div class="logout-section">
            <div>
              <div class="logout-title">退出登录</div>
              <div class="logout-desc">退出后需要重新输入账号密码登录</div>
            </div>
            <el-button type="danger" plain @click="handleLogout">
              退出登录
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const avatarLetter = computed(() => (userInfo.value?.nickname || userInfo.value?.username || '?')[0].toUpperCase())

// 头像完整 URL
const avatarUrl = computed(() => {
  const avatar = userInfo.value?.avatar
  if (!avatar) return ''
  // /static 开头的地址由 Vite 直接代理到 user-service，不经过 /api(Gateway)
  if (avatar.startsWith('/static')) return avatar
  if (avatar.startsWith('/')) return '/api' + avatar
  return avatar
})

// 上传状态
const uploading = ref(false)

// 上传前校验
function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
    return false
  }
  return true
}

// 上传头像
async function handleAvatarUpload(options: { file: File }) {
  uploading.value = true
  try {
    await userApi.uploadAvatar(options.file)
    // 重新拉取完整资料同步 store
    await userStore.fetchProfile()
    ElMessage.success('头像上传成功')
  } catch (e: any) {
    ElMessage.error((e as Error).message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const activeTab = ref('profile')

// ==================== 个人资料 ====================
const profileFormRef = ref<FormInstance>()
const profileSaving = ref(false)

const profileForm = ref({
  nickname: '',
  email: '',
  phone: '',
  gender: 0 as 0 | 1 | 2,
})

const profileRules: FormRules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

async function saveProfile() {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }
  profileSaving.value = true
  try {
    await userApi.updateProfile(profileForm.value)
    ElMessage.success('资料更新成功')
    // 重新拉取完整资料同步 store
    await userStore.fetchProfile()
  } catch (e: any) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    profileSaving.value = false
  }
}

// ==================== 修改密码 ====================
const passwordFormRef = ref<FormInstance>()
const passwordSaving = ref(false)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 确认密码自定义校验
function validateConfirm(_rule: any, value: string, callback: any) {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function changePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordSaving.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    await userStore.logout()
    router.push('/login')
  } catch (e: any) {
    ElMessage.error((e as Error).message || '修改失败')
  } finally {
    passwordSaving.value = false
  }
}

// ==================== 退出登录 ====================
async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}

// ==================== 初始化 ====================
onMounted(async () => {
  // 拉取完整资料（含 nickname/gender/createTime 等）
  await userStore.fetchProfile()
  const u = userInfo.value
  if (u) {
    profileForm.value = {
      nickname: u.nickname || '',
      email: u.email || '',
      phone: u.phone || '',
      gender: u.gender ?? 0,
    }
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.avatar-card {
  text-align: center;
  padding: 16px 8px;
}

.avatar-wrapper {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-hint {
  font-size: 12px;
  color: #909399;
}

.avatar-uploading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.avatar-letter {
  font-size: 32px;
  font-weight: bold;
  color: #fff;
}

.user-id-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
}

.user-id-row .label {
  color: #909399;
}

.user-id-row .value {
  color: #303133;
  font-family: monospace;
}

.account-status {
  text-align: center;
}

.profile-form,
.password-form {
  max-width: 480px;
  padding-top: 8px;
}

.logout-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.logout-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.logout-desc {
  font-size: 12px;
  color: #909399;
}
</style>
