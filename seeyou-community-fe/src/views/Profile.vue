<template>
  <div class="sy-container profile">
    <div class="profile-card sy-card">
      <div class="profile-head">
        <el-avatar :size="72" :src="info.avatarUrl || avatar" />
        <div class="head-info">
          <div class="head-name">
            {{ info.nickname || '-' }}
            <el-tag v-if="info.role >= 1" size="small" type="danger">管理员</el-tag>
          </div>
          <div class="head-sub">@{{ info.username }} · 注册第 {{ days }} 天</div>
          <div class="head-bio ellipsis-1">{{ info.bio || '这个人很懒，什么都没留下' }}</div>
        </div>
        <el-button type="primary" plain @click="openEdit">编辑资料</el-button>
      </div>

      <el-descriptions :column="2" border class="profile-desc">
        <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ info.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="城市">{{ info.city || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公司/学校">{{ info.companyOrSchool || '-' }}</el-descriptions-item>
        <el-descriptions-item label="个人博客">
          <a v-if="info.blogUrl" :href="info.blogUrl" target="_blank" class="sy-link">{{ info.blogUrl }}</a>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(info.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 我的内容 -->
    <div class="my-content sy-card">
      <el-tabs v-model="tab" @tab-change="loadMine">
        <el-tab-pane label="我的帖子" name="post" />
        <el-tab-pane label="我的博客" name="blog" />
        <el-tab-pane label="我的问答" name="qa" />
      </el-tabs>
      <div v-loading="mineLoading">
        <div
          v-for="item in mineList"
          :key="item.id"
          class="mine-item"
          @click="$router.push(`/${tab}/${item.id}`)"
        >
          <span class="mine-title ellipsis-1">{{ item.title }}</span>
          <span class="mine-meta">
            <el-icon><View /></el-icon>{{ item.likeCount }} 赞 · {{ item.commentCount }} 评论 · {{ fromNow(item.createTime) }}
          </span>
        </div>
        <el-empty v-if="!mineLoading && !mineList.length" description="还没有发布内容" :image-size="80" />
        <el-pagination
          v-if="mineTotal > mineSize"
          v-model:current-page="mineCurrent"
          :total="mineTotal"
          :page-size="mineSize"
          layout="prev, pager, next"
          class="mine-page"
          @current-change="loadMine"
        />
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" maxlength="50" /></el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            accept="image/*"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="editForm.avatar_url" :src="editForm.avatar_url" class="avatar-preview" />
            <el-icon v-else class="avatar-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG/GIF/WEBP，≤ 5MB</div>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.city" /></el-form-item>
        <el-form-item label="个人简介"><el-input v-model="editForm.bio" type="textarea" :rows="2" maxlength="255" /></el-form-item>
        <el-form-item label="博客地址"><el-input v-model="editForm.blog_url" /></el-form-item>
        <el-form-item label="公司/学校"><el-input v-model="editForm.company_or_school" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getUserInfo, editUserInfo, uploadImage } from '@/api/user'
import { getContentList } from '@/api/content'
import { formatDateTime, fromNow, registerDays, defaultAvatar } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const info = ref({})
const avatar = computed(() => defaultAvatar(info.value.nickname))
const days = computed(() => registerDays(info.value.createTime))

async function loadInfo() {
  const res = await getUserInfo()
  info.value = res.data
}

// 编辑（注意后端字段为蛇形）
const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({})
function openEdit() {
  Object.assign(editForm, {
    nickname: info.value.nickname,
    avatar_url: info.value.avatarUrl,
    email: info.value.email,
    phone: info.value.phone,
    city: info.value.city,
    bio: info.value.bio,
    blog_url: info.value.blogUrl,
    company_or_school: info.value.companyOrSchool
  })
  editVisible.value = true
}
async function saveEdit() {
  saving.value = true
  try {
    // 只提交有值的字段
    const payload = {}
    for (const [k, v] of Object.entries(editForm)) {
      if (v !== undefined && v !== null) payload[k] = v
    }
    await editUserInfo(payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadInfo()
    userStore.fetchUserInfo().catch(() => {})
  } finally {
    saving.value = false
  }
}

// 头像上传：先校验再走 uploadImage，成功后回填 editForm.avatar_url
function beforeAvatarUpload(file) {
  const ok = file.type && /^image\/(jpeg|jpg|png|gif|webp)$/.test(file.type)
  if (!ok) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WEBP 格式')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}
async function handleAvatarUpload(option) {
  try {
    const res = await uploadImage(option.file)
    editForm.avatar_url = res.data
    ElMessage.success('头像已上传')
  } catch (e) {
    // 拦截器已提示
  }
}

// 我的内容
const tab = ref('post')
const mineList = ref([])
const mineLoading = ref(false)
const mineCurrent = ref(1)
const mineSize = 10
const mineTotal = ref(0)

async function loadMine() {
  if (!info.value.id) return
  mineLoading.value = true
  try {
    const res = await getContentList(tab.value, {
      current: mineCurrent.value,
      size: mineSize,
      userId: String(info.value.id)
    })
    mineList.value = res.data.records
    mineTotal.value = Number(res.data.total)
  } finally {
    mineLoading.value = false
  }
}

onMounted(async () => {
  await loadInfo()
  loadMine()
})
</script>

<style scoped>
.profile {
  padding-top: 24px;
}
.profile-card {
  padding: 28px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.head-info {
  flex: 1;
  min-width: 0;
}
.head-name {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.head-sub {
  color: var(--sy-text-secondary);
  font-size: 13px;
  margin-top: 4px;
}
.head-bio {
  color: var(--sy-text-regular);
  font-size: 14px;
  margin-top: 6px;
}
.profile-desc {
  margin-top: 24px;
}

.my-content {
  margin-top: 20px;
  padding: 8px 28px 24px;
}
.mine-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--sy-border);
  cursor: pointer;
}
.mine-item:hover .mine-title {
  color: var(--sy-primary);
}
.mine-title {
  font-size: 15px;
}
.mine-meta {
  font-size: 12px;
  color: var(--sy-text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.mine-page {
  margin-top: 16px;
  justify-content: center;
}

/* 头像上传 */
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed var(--sy-border);
  border-radius: 6px;
  cursor: pointer;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: border-color 0.2s;
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--sy-primary);
}
.avatar-preview {
  width: 80px;
  height: 80px;
  object-fit: cover;
  display: block;
}
.avatar-icon {
  font-size: 24px;
  color: var(--sy-text-secondary);
}
.upload-tip {
  font-size: 12px;
  color: var(--sy-text-secondary);
  margin-top: 6px;
}
</style>
