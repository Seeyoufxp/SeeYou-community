<template>
  <div class="sy-container publish-page">
    <div class="publish-card sy-card">
      <div class="page-title">{{ isEdit ? `编辑${typeName}` : `发布${typeName}` }}</div>

      <el-form label-position="top">
        <el-form-item :label="res === 'qa' ? '问题标题' : '标题'" required>
          <el-input
            v-model="form.title"
            maxlength="128"
            show-word-limit
            :placeholder="res === 'qa' ? '一句话说清你的问题' : '请输入标题'"
          />
        </el-form-item>
        <el-form-item :label="res === 'qa' ? '问题补充（摘要）' : '摘要'">
          <el-input
            v-model="form.summary"
            maxlength="255"
            show-word-limit
            placeholder="选填，列表页展示用"
          />
        </el-form-item>
        <el-form-item :label="res === 'qa' ? '问题详情' : '正文'" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="14"
            :placeholder="res === 'blog' ? '支持 Markdown 语法' : '请输入正文内容'"
          />
        </el-form-item>

        <div class="actions">
          <el-button @click="$router.back()">取消</el-button>
          <el-button v-if="!isEdit && res === 'blog'" :loading="savingDraft" @click="submit(0)">
            存草稿
          </el-button>
          <el-button type="primary" :loading="publishing" @click="submit(1)">
            {{ isEdit ? '保存' : '发布' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import 'element-plus/es/components/message/style/css'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publishContent, updateContent, getContentDetail } from '@/api/content'

const route = useRoute()
const router = useRouter()

const res = computed(() => route.params.res)
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)
const typeName = computed(() => ({ post: '帖子', blog: '博客', qa: '问答' }[res.value]))

const form = reactive({ title: '', summary: '', content: '' })
const publishing = ref(false)
const savingDraft = ref(false)

onMounted(async () => {
  if (isEdit.value) {
    const res2 = await getContentDetail(res.value, id.value)
    form.title = res2.data.title
    form.summary = res2.data.summary
    form.content = res2.data.content
  }
})

async function submit(status) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写正文')
    return
  }
  const loading = status === 1 ? publishing : savingDraft
  loading.value = true
  try {
    if (isEdit.value) {
      await updateContent(res.value, id.value, { ...form, status })
      ElMessage.success('保存成功')
      router.push(`/${res.value}/${id.value}`)
    } else {
      const r = await publishContent(res.value, { ...form, status })
      ElMessage.success(status === 1 ? '发布成功' : '已存草稿')
      router.push(status === 1 ? `/${res.value}/${r.data}` : `/${res.value}`)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.publish-page {
  padding-top: 20px;
}
.publish-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 28px 36px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
