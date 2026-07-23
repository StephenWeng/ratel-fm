<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">日志管理</h1>
        <p class="page-subtitle">查询业务系统关键操作记录。</p>
      </div>
    </div>

    <el-form class="filter-form" :model="filters" label-width="96px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="操作时间">
            <el-date-picker
              v-model="filters.timeRange"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="full-width"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="账号">
            <el-input v-model="filters.account" clearable placeholder="模糊查询账号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="身份证">
            <el-input v-model="filters.identityNo" clearable placeholder="模糊查询身份证" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="联系方式">
            <el-input v-model="filters.contactPhone" clearable placeholder="模糊查询联系方式" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="部门">
            <el-input v-model="filters.department" clearable placeholder="模糊查询部门" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="终端类型">
            <el-select v-model="filters.terminalType" clearable placeholder="全部" class="full-width">
              <el-option label="PC" value="PC" />
              <el-option label="APP" value="APP" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label="终端标识">
            <el-input v-model="filters.terminalIdentifier" clearable placeholder="模糊查询终端标识" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-form-item label=" ">
            <div class="filter-actions">
              <el-button v-if="auth.hasMenu('BTN_OPERATION_LOG_QUERY')" :icon="Search" type="primary" :loading="loading" @click="load">
                查询
              </el-button>
              <el-button @click="reset">重置</el-button>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="operatorUsername" label="账号" min-width="120" />
      <el-table-column prop="identityNo" label="身份证" min-width="160" />
      <el-table-column prop="contactPhone" label="联系方式" min-width="130" />
      <el-table-column prop="department" label="部门" min-width="120" />
      <el-table-column label="操作时间" min-width="180">
        <template #default="{ row }">{{ displayDateTime(row.operationTime) }}</template>
      </el-table-column>
      <el-table-column prop="terminalType" label="终端类型" width="96" />
      <el-table-column prop="terminalIdentifier" label="终端标识" min-width="140" />
      <el-table-column prop="operationModule" label="操作模块" min-width="120" />
      <el-table-column prop="operationFunction" label="操作功能" min-width="140" />
      <el-table-column prop="impact" label="操作内容" min-width="280" show-overflow-tooltip />
      <el-table-column label="是否成功" width="104">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import type { OperationLogView } from '@/types/api'
import { formatLocalDate as formatDate, toLocalDateTimeBoundary as toDateTimeBoundary } from '@/utils/dateTime'

/**
 * 日志管理页面。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * @author ratel
 */
const auth = useAuthStore()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/**
 * 常量 rows：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rows = ref<OperationLogView[]>([])
/**
 * 常量 total：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const total = ref(0)
/**
 * 常量 page：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const page = ref(1)
/**
 * 常量 size：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const size = ref(20)
/**
 * 常量 filters：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const filters = reactive({
  /**
   * 字段 timeRange：表示表单、筛选条件、接口数据或组件状态中的 timeRange 值。
   */
  timeRange: defaultTimeRange(),
  /**
   * 字段 account：表示表单、筛选条件、接口数据或组件状态中的 account 值。
   */
  account: '',
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: '',
  /**
   * 字段 contactPhone：表示表单、筛选条件、接口数据或组件状态中的 contactPhone 值。
   */
  contactPhone: '',
  /**
   * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
   */
  department: '',
  /**
   * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
   */
  terminalType: '' as '' | 'PC' | 'APP',
  /**
   * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
   */
  terminalIdentifier: ''
})

/**
 * 获取日志默认查询时间范围。
 *
 * 实现步骤：结束日期取当前日期，开始日期取当前日期往前一个月；页面首次进入和重置筛选时共用。
 */
function defaultTimeRange() {
  /**
   * 常量 end：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const end = new Date()
  /**
   * 常量 start：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const start = new Date(end)
  start.setMonth(start.getMonth() - 1)
  return [formatDate(start), formatDate(end)]
}

/**
 * 按 yyyy-MM-dd 格式输出本地日期。
 */
/**
 * 把日期筛选值转换为后端 OffsetDateTime 可解析格式。
 *
 * 实现步骤：
 * 1. 开始日期拼接 00:00:00，覆盖当天最早时间；
 * 2. 结束日期拼接 23:59:59，覆盖当天最晚时间；
 * 3. 追加浏览器本地时区偏移，保证后端按本地业务日期查询。
 */
/**
 * 获取浏览器本地时区偏移，格式为 +08:00。
 */
/**
 * 将后端 OffsetDateTime 展示为年月日时分秒格式。
 *
 * 实现步骤：用浏览器 Date 解析 ISO 字符串，再按本地时区输出 yyyy-MM-dd HH:mm:ss，避免表格显示 T/Z 等技术格式。
 */
function displayDateTime(value?: string) {
  if (!value) {
    return ''
  }
  /**
   * 常量 date：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  /**
   * 常量 pad：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const pad = (item: number) => String(item).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 加载日志列表。
 *
 * 实现步骤：
 * 1. 校验当前人员是否拥有查询按钮授权；
 * 2. 组装分页和筛选条件；
 * 3. 调用后端日志查询接口并刷新表格和总数。
 */
async function load() {
  if (!auth.hasMenu('BTN_OPERATION_LOG_QUERY')) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    /**
     * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const result = await api.operationLogs({
      /**
       * 字段 startTime：表示表单、筛选条件、接口数据或组件状态中的 startTime 值。
       */
      startTime: toDateTimeBoundary(filters.timeRange?.[0]),
      /**
       * 字段 endTime：表示表单、筛选条件、接口数据或组件状态中的 endTime 值。
       */
      endTime: toDateTimeBoundary(filters.timeRange?.[1], true),
      /**
       * 字段 account：表示表单、筛选条件、接口数据或组件状态中的 account 值。
       */
      account: filters.account || undefined,
      /**
       * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
       */
      identityNo: filters.identityNo || undefined,
      /**
       * 字段 contactPhone：表示表单、筛选条件、接口数据或组件状态中的 contactPhone 值。
       */
      contactPhone: filters.contactPhone || undefined,
      /**
       * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
       */
      department: filters.department || undefined,
      /**
       * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
       */
      terminalType: filters.terminalType || undefined,
      /**
       * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
       */
      terminalIdentifier: filters.terminalIdentifier || undefined,
      /**
       * 字段 page：表示表单、筛选条件、接口数据或组件状态中的 page 值。
       */
      page: page.value - 1,
      /**
       * 字段 size：表示表单、筛选条件、接口数据或组件状态中的 size 值。
       */
      size: size.value
    })
    rows.value = result.rows
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/**
 * 重置筛选条件并回到第一页。
 */
function reset() {
  Object.assign(filters, {
    /**
     * 字段 timeRange：表示表单、筛选条件、接口数据或组件状态中的 timeRange 值。
     */
    timeRange: defaultTimeRange(),
    /**
     * 字段 account：表示表单、筛选条件、接口数据或组件状态中的 account 值。
     */
    account: '',
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: '',
    /**
     * 字段 contactPhone：表示表单、筛选条件、接口数据或组件状态中的 contactPhone 值。
     */
    contactPhone: '',
    /**
     * 字段 department：表示表单、筛选条件、接口数据或组件状态中的 department 值。
     */
    department: '',
    /**
     * 字段 terminalType：表示表单、筛选条件、接口数据或组件状态中的 terminalType 值。
     */
    terminalType: '',
    /**
     * 字段 terminalIdentifier：表示表单、筛选条件、接口数据或组件状态中的 terminalIdentifier 值。
     */
    terminalIdentifier: ''
  })
  page.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.filter-form {
  margin-bottom: 14px;
  padding: 14px 14px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.filter-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px 12px;
  white-space: nowrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

</style>
