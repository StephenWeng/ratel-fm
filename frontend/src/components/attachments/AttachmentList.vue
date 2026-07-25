<template>
  <div class="attachment-block">
    <div class="attachment-header">
      <span class="attachment-title">附件</span>
      <el-upload
        v-if="editable"
        :auto-upload="false"
        :show-file-list="false"
        multiple
        :on-change="handleFileChange"
      >
        <el-button size="small" :icon="Upload" :loading="uploading">上传附件</el-button>
      </el-upload>
    </div>

    <el-table class="attachment-table" :data="rows" size="small" border empty-text="暂无附件">
      <el-table-column prop="displayName" label="附件名称" min-width="220">
        <template #default="{ row }">
          <span>{{ row.displayName }}</span>
          <el-tag v-if="row.pending" class="pending-tag" size="small" type="warning">待上传</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="suffix" label="后缀" width="90" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="uploaderUsername" label="上传人" width="110" />
      <el-table-column label="操作" width="230">
        <template #default="{ row, $index }">
          <div class="table-actions">
            <el-button v-if="!row.pending && canPreview(row)" size="small" :icon="View" @click="preview(row)">预览</el-button>
            <el-button v-if="!row.pending" size="small" :icon="Download" @click="download(row)">下载</el-button>
            <el-button v-if="editable && !row.pending" size="small" :icon="Edit" @click="rename(row)">改名</el-button>
            <el-button v-if="editable" size="small" type="danger" :icon="Delete" @click="remove(row, $index)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="82vw" top="5vh" destroy-on-close @closed="clearPreview">
      <div v-loading="previewLoading" class="preview-body">
        <img v-if="previewMode === 'image'" class="preview-image" :src="previewUrl" alt="附件预览" />
        <iframe v-else-if="previewMode === 'pdf'" class="preview-frame" :src="previewUrl" title="PDF附件预览" />
        <div v-else-if="previewMode === 'docx'" class="preview-docx" v-html="previewHtml"></div>
        <div v-else-if="previewMode === 'xlsx'" class="preview-xlsx" v-html="previewHtml"></div>
        <el-empty v-else description="该附件暂不支持在线预览，请下载后查看" />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Delete, Download, Edit, Upload, View } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { saveBlob } from '@/api/http'
import type { AttachmentBizType, AttachmentView } from '@/types/api'

/**
 * PendingAttachmentRow 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface PendingAttachmentRow extends AttachmentView {
  /**
   * 字段 pending：表示表单、筛选条件、接口数据或组件状态中的 pending 值。
   */
  pending?: boolean
  /**
   * 字段 file：表示表单、筛选条件、接口数据或组件状态中的 file 值。
   */
  file?: File
}

/**
 * 常量 props：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const props = defineProps<{
  /**
   * 字段 businessType：表示表单、筛选条件、接口数据或组件状态中的 businessType 值。
   */
  businessType: AttachmentBizType
  /**
   * 字段 businessId：表示表单、筛选条件、接口数据或组件状态中的 businessId 值。
   */
  businessId?: number
  /**
   * 字段 editable：表示表单、筛选条件、接口数据或组件状态中的 editable 值。
   */
  editable?: boolean
}>()

/**
 * 常量 attachments：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const attachments = ref<AttachmentView[]>([])
/**
 * 常量 pendingFiles：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const pendingFiles = ref<File[]>([])
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 uploading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const uploading = ref(false)
/**
 * 常量 previewVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewVisible = ref(false)
/**
 * 常量 previewLoading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewLoading = ref(false)
/**
 * 常量 previewTitle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewTitle = ref('')
/**
 * 常量 previewMode：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewMode = ref<'image' | 'pdf' | 'docx' | 'xlsx' | 'unsupported'>('unsupported')
/**
 * 常量 previewUrl：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewUrl = ref('')
/**
 * 常量 previewHtml：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const previewHtml = ref('')
/**
 * 常量 OFFICE_PREVIEW_MAX_SIZE：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const OFFICE_PREVIEW_MAX_SIZE = 10 * 1024 * 1024

/**
 * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rows = computed<PendingAttachmentRow[]>(() => [
  ...attachments.value,
  ...pendingFiles.value.map((file, index) => ({
    /**
     * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
     */
    id: -(index + 1),
    /**
     * 字段 originalName：表示表单、筛选条件、接口数据或组件状态中的 originalName 值。
     */
    originalName: file.name,
    /**
     * 字段 displayName：表示表单、筛选条件、接口数据或组件状态中的 displayName 值。
     */
    displayName: file.name,
    /**
     * 字段 suffix：表示表单、筛选条件、接口数据或组件状态中的 suffix 值。
     */
    suffix: suffixOf(file.name),
    /**
     * 字段 fileSize：表示表单、筛选条件、接口数据或组件状态中的 fileSize 值。
     */
    fileSize: file.size,
    /**
     * 字段 contentType：表示表单、筛选条件、接口数据或组件状态中的 contentType 值。
     */
    contentType: file.type,
    /**
     * 字段 pending：表示表单、筛选条件、接口数据或组件状态中的 pending 值。
     */
    pending: true,
    file
  }))
])

watch(
  () => props.businessId,
  async (businessId) => {
    if (businessId) {
      pendingFiles.value = []
      await reload(businessId)
    } else {
      attachments.value = []
      pendingFiles.value = []
    }
  },
  { immediate: true }
)

/**
 * 加载已有业务附件。
 *
 * 实现步骤：
 * 1. 判断业务 ID 是否存在；
 * 2. 调用统一附件接口查询附件；
 * 3. 刷新附件表格，清理已上传的暂存文件。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function reload(targetBusinessId = props.businessId) {
  if (!targetBusinessId) {
    attachments.value = []
    return
  }
  loading.value = true
  try {
    attachments.value = await api.attachments(props.businessType, targetBusinessId)
  } finally {
    loading.value = false
  }
}

/**
 * 处理用户选择的附件文件。
 *
 * 实现步骤：
 * 1. 从 Element Plus 上传事件中读取原始 File；
 * 2. 新建业务记录尚无 ID 时先暂存到本地列表；
 * 3. 已有业务记录直接上传并刷新附件列表。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function handleFileChange(uploadFile: UploadFile) {
  /**
   * 常量 file：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const file = uploadFile.raw
  if (!file) {
    return
  }
  if (!props.businessId) {
    pendingFiles.value.push(file)
    return
  }
  pendingFiles.value.push(file)
  await uploadPending(props.businessId)
}

/**
 * 上传暂存附件。
 *
 * 实现步骤：
 * 1. 业务 ID 不存在或暂存列表为空时直接返回；
 * 2. 把暂存文件追加到 FormData 的 files 字段；
 * 3. 调用统一附件上传接口；
 * 4. 上传成功后清理暂存列表并重新加载附件。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function uploadPending(targetBusinessId?: number) {
  /**
   * 常量 businessId：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const businessId = targetBusinessId || props.businessId
  if (!businessId || pendingFiles.value.length === 0) {
    return
  }
  uploading.value = true
  try {
    /**
     * 常量 formData：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const formData = new FormData()
    pendingFiles.value.forEach((file) => formData.append('files', file))
    await api.uploadAttachments(props.businessType, businessId, formData)
    pendingFiles.value = []
    await reload(businessId)
    ElMessage.success('附件上传成功')
  } finally {
    uploading.value = false
  }
}

/**
 * 修改附件展示名称。
 *
 * 实现步骤：
 * 1. 弹出输入框收集新名称；
 * 2. 调用后端改名接口；
 * 3. 刷新附件列表。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function rename(row: PendingAttachmentRow) {
  if (!props.businessId) {
    return
  }
  /**
   * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const result = await ElMessageBox.prompt('附件名称', '修改附件名称', {
    /**
     * 字段 inputValue：表示表单、筛选条件、接口数据或组件状态中的 inputValue 值。
     */
    inputValue: row.displayName,
    /**
     * 字段 inputPattern：表示表单、筛选条件、接口数据或组件状态中的 inputPattern 值。
     */
    inputPattern: /\S+/,
    /**
     * 字段 inputErrorMessage：表示表单、筛选条件、接口数据或组件状态中的 inputErrorMessage 值。
     */
    inputErrorMessage: '附件名称不能为空',
    /**
     * 字段 confirmButtonText：表示表单、筛选条件、接口数据或组件状态中的 confirmButtonText 值。
     */
    confirmButtonText: '确认',
    /**
     * 字段 cancelButtonText：表示表单、筛选条件、接口数据或组件状态中的 cancelButtonText 值。
     */
    cancelButtonText: '取消'
  })
  await api.renameAttachment(props.businessType, props.businessId, row.id, result.value.trim())
  await reload()
  ElMessage.success('附件名称已修改')
}

/**
 * 删除附件。
 *
 * 实现步骤：
 * 1. 待上传附件只从前端暂存列表移除；
 * 2. 已上传附件先二次确认；
 * 3. 调用后端删除接口并刷新列表。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function remove(row: PendingAttachmentRow, index: number) {
  if (row.pending) {
    pendingFiles.value.splice(index - attachments.value.length, 1)
    return
  }
  if (!props.businessId) {
    return
  }
  await ElMessageBox.confirm(`确认删除附件“${row.displayName}”？`, '删除附件确认', {
    /**
     * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
     */
    type: 'warning',
    /**
     * 字段 confirmButtonText：表示表单、筛选条件、接口数据或组件状态中的 confirmButtonText 值。
     */
    confirmButtonText: '确认删除',
    /**
     * 字段 cancelButtonText：表示表单、筛选条件、接口数据或组件状态中的 cancelButtonText 值。
     */
    cancelButtonText: '取消'
  })
  await api.deleteAttachment(props.businessType, props.businessId, row.id)
  await reload()
  ElMessage.success('附件已删除')
}

/**
 * 判断附件是否支持在线预览。
 *
 * 实现步骤：
 * 1. 图片和 PDF 交给浏览器原生预览；
 * 2. docx/xlsx 走前端轻量解析；
 * 3. 其他格式保留下载能力，不显示预览按钮。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
function canPreview(row: PendingAttachmentRow) {
  return previewKind(row) !== 'unsupported'
}

/**
 * 在线预览附件。
 *
 * 实现步骤：
 * 1. 校验业务 ID 和文件大小限制；
 * 2. 调用后端 inline 预览接口获取文件 Blob；
 * 3. 图片/PDF 创建浏览器对象 URL；
 * 4. docx 使用 mammoth 转换为 HTML，xlsx 使用 SheetJS 转换为表格 HTML。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function preview(row: PendingAttachmentRow) {
  if (!props.businessId) {
    return
  }
  /**
   * 常量 kind：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const kind = previewKind(row)
  if (kind === 'unsupported') {
    ElMessage.warning('该附件暂不支持在线预览，请下载后查看')
    return
  }
  if ((kind === 'docx' || kind === 'xlsx') && Number(row.fileSize || 0) > OFFICE_PREVIEW_MAX_SIZE) {
    ElMessage.warning('Office 附件超过 10MB，请下载后查看')
    return
  }
  previewVisible.value = true
  previewLoading.value = true
  previewTitle.value = row.displayName
  previewMode.value = kind
  previewHtml.value = ''
  clearPreviewUrl()
  try {
    const { blob } = await api.previewAttachment(props.businessType, props.businessId, row.id)
    if (kind === 'image' || kind === 'pdf') {
      previewUrl.value = URL.createObjectURL(blob)
    } else if (kind === 'docx') {
      previewHtml.value = await renderDocx(blob)
    } else if (kind === 'xlsx') {
      previewHtml.value = await renderXlsx(blob)
    }
  } finally {
    previewLoading.value = false
  }
}

/**
 * 下载附件。
 *
 * 实现步骤：
 * 1. 调用后端下载接口获取 Blob；
 * 2. 优先使用响应头文件名，没有时使用列表展示名称；
 * 3. 触发浏览器下载。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 */
async function download(row: PendingAttachmentRow) {
  if (!props.businessId) {
    return
  }
  const { blob, filename } = await api.downloadAttachment(props.businessType, props.businessId, row.id)
  saveBlob(blob, filename || row.displayName)
}

/**
 * 使用 mammoth 把 docx 轻量转换为 HTML。
 */
async function renderDocx(blob: Blob) {
  /**
   * 常量 mammoth：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const mammoth = await import('mammoth/mammoth.browser')
  /**
   * 常量 arrayBuffer：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const arrayBuffer = await blob.arrayBuffer()
  /**
   * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const result = await mammoth.convertToHtml({ arrayBuffer })
  return result.value || '<p>文档无可预览内容</p>'
}

/**
 * 使用 SheetJS 把 xlsx 首个工作表转换为 HTML 表格。
 */
async function renderXlsx(blob: Blob) {
  /**
   * 常量 XLSX：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const XLSX = await import('xlsx')
  /**
   * 常量 arrayBuffer：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const arrayBuffer = await blob.arrayBuffer()
  /**
   * 常量 workbook：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const workbook = XLSX.read(arrayBuffer, { type: 'array' })
  /**
   * 常量 sheetName：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sheetName = workbook.SheetNames[0]
  if (!sheetName) {
    return '<p>工作簿无可预览内容</p>'
  }
  /**
   * 常量 sheet：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const sheet = workbook.Sheets[sheetName]
  return `<div class="preview-sheet-title">${escapeHtml(sheetName)}</div>${XLSX.utils.sheet_to_html(sheet)}`
}

/**
 * 根据后缀和 MIME 类型识别预览方式。
 */
function previewKind(row: PendingAttachmentRow): 'image' | 'pdf' | 'docx' | 'xlsx' | 'unsupported' {
  /**
   * 常量 suffix：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const suffix = String(row.suffix || '').toLowerCase()
  /**
   * 常量 contentType：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const contentType = String(row.contentType || '').toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(suffix) || contentType.startsWith('image/')) {
    return 'image'
  }
  if (suffix === 'pdf' || contentType.includes('pdf')) {
    return 'pdf'
  }
  if (suffix === 'docx') {
    return 'docx'
  }
  if (suffix === 'xlsx') {
    return 'xlsx'
  }
  return 'unsupported'
}

/**
 * 重置组件状态，供父页面打开新增弹窗时调用。
 */
function reset() {
  attachments.value = []
  pendingFiles.value = []
  clearPreview()
}

/**
 * 追加父组件传入的待上传附件。
 *
 * 实现步骤：
 * 1. 接收凭证导入等业务流程已经选择的原始文件；
 * 2. 过滤空文件并追加到暂存附件列表；
 * 3. 如果当前业务已经有 ID，则立即复用 uploadPending 上传；新增业务无 ID 时等待保存后上传。
 */
async function addPendingFiles(files: File[]) {
  const validFiles = files.filter((file) => file instanceof File && file.size > 0)
  if (validFiles.length === 0) {
    return
  }
  pendingFiles.value.push(...validFiles)
  if (props.businessId) {
    await uploadPending(props.businessId)
  }
}

/**
 * 执行 suffixOf 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function suffixOf(fileName: string) {
  /**
   * 常量 dotIndex：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const dotIndex = fileName.lastIndexOf('.')
  return dotIndex >= 0 && dotIndex < fileName.length - 1 ? fileName.slice(dotIndex + 1).toLowerCase() : ''
}

/**
 * 执行 formatSize 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function formatSize(size?: number) {
  /**
   * 常量 value：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const value = Number(size || 0)
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

/**
 * 执行 clearPreview 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function clearPreview() {
  clearPreviewUrl()
  previewHtml.value = ''
  previewTitle.value = ''
  previewMode.value = 'unsupported'
}

/**
 * 执行 clearPreviewUrl 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function clearPreviewUrl() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

/**
 * 执行 escapeHtml 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

defineExpose({ reload, reset, uploadPending, addPendingFiles })
</script>

<style scoped>
.attachment-block {
  margin-top: 14px;
  padding-top: 12px;
  min-width: 0;
  overflow-x: hidden;
  border-top: 1px solid var(--border-color);
}

.attachment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.attachment-title {
  font-weight: 700;
  color: var(--heading-color);
}

.attachment-table {
  width: 100%;
}

.attachment-table :deep(.el-table__inner-wrapper),
.attachment-table :deep(.el-table__body-wrapper),
.attachment-table :deep(.el-table__header-wrapper) {
  overflow-x: hidden !important;
}

.attachment-table :deep(.cell) {
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.pending-tag {
  margin-left: 8px;
}

.preview-body {
  min-height: 420px;
  max-height: 72vh;
  overflow: auto;
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: 68vh;
  margin: 0 auto;
  object-fit: contain;
}

.preview-frame {
  width: 100%;
  height: 68vh;
  border: 0;
}

.preview-docx {
  min-height: 420px;
  padding: 22px 28px;
  color: var(--text-color);
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  line-height: 1.75;
}

.preview-xlsx {
  min-height: 420px;
  padding: 12px;
  overflow: auto;
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
}

.preview-xlsx :deep(table) {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.preview-xlsx :deep(td),
.preview-xlsx :deep(th) {
  min-width: 90px;
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  white-space: nowrap;
}

.preview-xlsx :deep(th) {
  background: var(--subtle-surface-color);
  font-weight: 600;
}

.preview-sheet-title {
  margin-bottom: 10px;
  color: var(--heading-color);
  font-weight: 700;
}
</style>
