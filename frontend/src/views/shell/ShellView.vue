<template>
  <el-container class="shell">
    <el-aside
      class="aside"
      :class="{ 'is-expanded': asideExpanded }"
      :width="asideExpanded ? '232px' : '64px'"
      @mouseenter="expandAside"
      @mouseleave="collapseAside"
    >
      <div class="aside-brand">
        <div class="brand-icon">
          <SystemLogo />
        </div>
        <div class="brand-text">
          <strong>Ratel FM</strong>
          <span>财务管理 ERP</span>
        </div>
      </div>
      <el-menu router :default-active="route.path" class="nav-menu" unique-opened :collapse="!asideExpanded" :collapse-transition="false" @select="recordMenuSelect">
        <template v-for="item in visibleMenus" :key="'children' in item ? item.key : item.path">
          <el-sub-menu v-if="'children' in item" :index="item.key">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.label }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">{{ activeTitle }}</div>
        <div class="topbar-right">
          <el-tooltip placement="bottom" effect="light" popper-class="clock-tooltip">
            <template #content>
              <div class="clock-tooltip-content">
                <div>{{ formattedServerTime }}</div>
                <div>{{ formattedLunarDate }}</div>
              </div>
            </template>
            <div class="server-clock">
              <el-icon><Clock /></el-icon>
              <span>{{ formattedServerTime }}</span>
            </div>
          </el-tooltip>
          <el-popover placement="bottom-end" trigger="hover" :width="320" popper-class="weather-popper">
            <template #reference>
              <button
                type="button"
                class="weather-trigger"
                :class="{ unavailable: !weather?.available, locating: weatherLocating }"
                :title="weatherLocationActionTitle"
                @click.stop="requestWeatherLocation"
              >
                <el-icon><component :is="weatherIcon(weather?.iconType)" /></el-icon>
                <span>{{ weatherSummary }}</span>
              </button>
            </template>
            <div class="weather-panel">
              <div class="weather-panel-header">
                <div>
                  <strong>{{ weather?.locationName || '天气' }}</strong>
                  <span>{{ weatherSourceText }}</span>
                </div>
                <div class="weather-current">
                  <el-icon><component :is="weatherIcon(weather?.iconType)" /></el-icon>
                  <strong>{{ formatTemperature(weather?.temperature) }}</strong>
                </div>
              </div>
              <div v-if="weather?.available" class="weather-current-meta">
                {{ weather.weatherText }} · 紫外线 {{ formatUvIndex(weather.uvIndex) }} · 更新时间 {{ formatWeatherTime(weather.currentTime) }}
              </div>
              <div class="weather-location-actions">
                <el-button size="small" type="primary" plain :loading="weatherLocating" @click="requestWeatherLocation">使用当前位置</el-button>
                <span>{{ weatherLocationHint }}</span>
              </div>
              <div v-if="weather?.available" class="weather-list">
                <div v-for="hour in weather.futureHours" :key="hour.time" class="weather-row">
                  <span>{{ formatWeatherTime(hour.time) }}</span>
                  <span class="weather-row-condition">
                    <el-icon><component :is="weatherIcon(hour.iconType)" /></el-icon>
                    {{ hour.weatherText }}
                  </span>
                  <span class="weather-row-uv">UV {{ formatUvIndex(hour.uvIndex) }}</span>
                  <strong>{{ formatTemperature(hour.temperature) }}</strong>
                </div>
                <div v-if="weather.futureHours.length === 0" class="weather-empty">暂无未来天气数据</div>
              </div>
            </div>
          </el-popover>
          <el-dropdown trigger="click" @command="theme.setTheme">
            <button class="theme-trigger" :title="`当前主题：${theme.currentOption.label}`">
              <span class="theme-dot" :style="{ background: theme.currentOption.preview }"></span>
              <el-icon><Brush /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in themeOptions" :key="item.value" :command="item.value">
                  <span class="theme-menu-item">
                    <span class="theme-dot" :style="{ background: item.preview }"></span>
                    <span>{{ item.label }}</span>
                    <span v-if="item.value === theme.current" class="theme-current">当前</span>
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip content="操作手册" placement="bottom">
            <button class="manual-trigger" type="button" @click="manualVisible = true">
              <el-icon><QuestionFilled /></el-icon>
              <span>操作手册</span>
            </button>
          </el-tooltip>
          <el-dropdown>
            <button class="user-button">
              <el-avatar :size="24" :src="auth.user?.avatarUrl">
                {{ userInitial }}
              </el-avatar>
              <span>{{ auth.user?.realName || auth.user?.username }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ auth.user?.username }}</el-dropdown-item>
                <el-dropdown-item v-if="canEditProfile" @click="openProfile">个人信息</el-dropdown-item>
                <el-dropdown-item v-if="canChangePassword" @click="openPassword">修改密码</el-dropdown-item>
                <el-dropdown-item v-if="auth.hasMenu('BTN_LOGOUT')" divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="content">
        <div class="workspace-tabs">
          <div class="tabs-scroll">
            <el-tabs
              v-model="activeTab"
              type="card"
              class="page-tabs"
              @tab-click="switchTab"
              @tab-remove="removeTab"
            >
              <el-tab-pane
                v-for="tab in openedTabs"
                :key="tab.path"
                :label="tab.label"
                :name="tab.path"
                :closable="canCloseTab(tab.path)"
              />
            </el-tabs>
          </div>
          <div class="tabs-tools">
            <el-button :icon="Close" @click="closeAllTabs">关闭全部</el-button>
          </div>
        </div>
        <div class="workspace-body">
          <router-view v-slot="{ Component }">
            <keep-alive>
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="profileVisible" title="个人信息" width="560px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="86px">
        <el-form-item label="头像">
          <div class="profile-avatar-row">
            <el-avatar :size="64" :src="profileAvatarPreview || auth.user?.avatarUrl">{{ userInitial }}</el-avatar>
            <el-upload
              v-if="canUploadAvatar"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="selectProfileAvatar"
              accept=".jpg,.jpeg,.png,.webp"
            >
              <el-button>选择头像</el-button>
            </el-upload>
          </div>
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="profileForm.realName" :maxlength="fieldLimits.chineseName" show-word-limit placeholder="请输入中文姓名" />
      </el-form-item>
      <el-form-item label="身份证" prop="identityNo">
        <el-input v-model="profileForm.identityNo" maxlength="18" placeholder="请输入18位身份证号" />
      </el-form-item>
      <el-form-item label="联系方式" prop="phone">
        <el-input v-model="profileForm.phone" maxlength="30" placeholder="手机号或座机号" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="profileForm.email" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="profileVisible = false">取消</el-button>
      <el-button v-if="canEditProfile" type="primary" @click="saveProfile">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="passwordVisible" title="修改密码" width="460px">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="86px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button v-if="canChangePassword" type="primary" @click="savePassword">保存</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="manualVisible" title="系统操作手册" size="min(980px, 92vw)" direction="rtl" class="manual-drawer">
    <SystemManual />
  </el-drawer>

  <FloatingVoiceCommand v-if="auth.hasMenu('BTN_ASSISTANT_VOICE')" />
  <FloatingAiAssistant v-if="auth.hasMenu('BTN_ASSISTANT_ASK')" />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile, type UploadRawFile } from 'element-plus'
import {
  Box,
  Brush,
  Clock,
  Cloudy,
  ColdDrink,
  Collection,
  Close,
  Files,
  Histogram,
  House,
  Lightning,
  List,
  Monitor,
  PartlyCloudy,
  Pouring,
  QuestionFilled,
  Notebook,
  Medal,
  Search,
  Menu,
  ShoppingCart,
  Sunny,
  Tickets,
  Van,
  User
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { themeOptions, useThemeStore } from '@/stores/theme'
import SystemLogo from '@/components/brand/SystemLogo.vue'
import SystemManual from '@/components/manual/SystemManual.vue'
import { api } from '@/api/fm'
import { fieldLimits, validateAvatarImage, validateChineseName, validateContactPhone, validateOptionalIdentityNo } from '@/utils/validators'
import { locateByPublicIp } from '@/utils/ipLocation'
import { menuByPath, menuUsageUserKey, recordMenuUsage } from '@/utils/menuUsage'
import type { MenuView, WeatherView } from '@/types/api'
import FloatingAiAssistant from '@/components/assistant/FloatingAiAssistant.vue'
import FloatingVoiceCommand from '@/components/assistant/FloatingVoiceCommand.vue'

/**
 * 常量 route：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const route = useRoute()
/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 theme：保存全局主题状态，供顶栏主题切换菜单读取和切换。
 */
const theme = useThemeStore()
/**
 * 常量 asideExpanded：控制左侧菜单栏展开状态；默认收起，鼠标悬浮后展开以节约主工作区宽度。
 */
const asideExpanded = ref(false)
/**
 * 常量 manualVisible：控制右上角系统操作手册抽屉是否显示。
 */
const manualVisible = ref(false)

/**
 * LeafMenu 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface LeafMenu {
  /**
   * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
   */
  path: string
  /**
   * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
   */
  label: string
  /**
   * 字段 icon：表示表单、筛选条件、接口数据或组件状态中的 icon 值。
   */
  icon: Component
  /**
   * 字段 menuCode：表示表单、筛选条件、接口数据或组件状态中的 menuCode 值。
   */
  menuCode: string
}

/**
 * GroupMenu 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface GroupMenu {
  /**
   * 字段 key：表示表单、筛选条件、接口数据或组件状态中的 key 值。
   */
  key: string
  /**
   * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
   */
  label: string
  /**
   * 字段 icon：表示表单、筛选条件、接口数据或组件状态中的 icon 值。
   */
  icon: Component
  /**
   * 字段 children：表示表单、筛选条件、接口数据或组件状态中的 children 值。
   */
  children: LeafMenu[]
}

/**
 * ShellMenu 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
type ShellMenu = LeafMenu | GroupMenu

/**
 * OpenedTab 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface OpenedTab {
  /**
   * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
   */
  path: string
  /**
   * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
   */
  label: string
  /**
   * 字段 menuCode：表示表单、筛选条件、接口数据或组件状态中的 menuCode 值。
   */
  menuCode: string
}

/**
 * 常量 iconMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const iconMap: Record<string, Component> = {
  /**
   * 字段 MODULE_HOME：表示表单、筛选条件、接口数据或组件状态中的 MODULE_HOME 值。
   */
  MODULE_HOME: House,
  /**
   * 字段 MODULE_BASIC：表示表单、筛选条件、接口数据或组件状态中的 MODULE_BASIC 值。
   */
  MODULE_BASIC: Collection,
  /**
   * 字段 MODULE_FINANCE：表示表单、筛选条件、接口数据或组件状态中的 MODULE_FINANCE 值。
   */
  MODULE_FINANCE: Notebook,
  /**
   * 字段 MODULE_OPERATION：表示表单、筛选条件、接口数据或组件状态中的 MODULE_OPERATION 值。
   */
  MODULE_OPERATION: ShoppingCart,
  /**
   * 字段 MODULE_INVENTORY：表示表单、筛选条件、接口数据或组件状态中的 MODULE_INVENTORY 值。
   */
  MODULE_INVENTORY: Box,
  /**
   * 字段 MODULE_AR_AP：表示表单、筛选条件、接口数据或组件状态中的 MODULE_AR_AP 值。
   */
  MODULE_AR_AP: Files,
  /**
   * 字段 MODULE_WORKFLOW：表示审批中心菜单使用的图标组件。
   */
  MODULE_WORKFLOW: Tickets,
  /**
   * 字段 MODULE_REPORT：表示表单、筛选条件、接口数据或组件状态中的 MODULE_REPORT 值。
   */
  MODULE_REPORT: Histogram,
  /**
   * 字段 MODULE_ASSISTANT：表示表单、筛选条件、接口数据或组件状态中的 MODULE_ASSISTANT 值。
   */
  MODULE_ASSISTANT: SystemLogo,
  /**
   * 字段 MODULE_SEARCH：表示表单、筛选条件、接口数据或组件状态中的 MODULE_SEARCH 值。
   */
  MODULE_SEARCH: Search,
  /**
   * 字段 MODULE_AUDIT：表示表单、筛选条件、接口数据或组件状态中的 MODULE_AUDIT 值。
   */
  MODULE_AUDIT: List,
  /**
   * 字段 PAGE_DASHBOARD：表示表单、筛选条件、接口数据或组件状态中的 PAGE_DASHBOARD 值。
   */
  PAGE_DASHBOARD: House,
  /**
   * 字段 PAGE_BASIC_DICTIONARIES：表示表单、筛选条件、接口数据或组件状态中的 PAGE_BASIC_DICTIONARIES 值。
   */
  PAGE_BASIC_DICTIONARIES: Collection,
  /**
   * 字段 PAGE_USERS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_USERS 值。
   */
  PAGE_USERS: User,
  /**
   * 字段 PAGE_ROLES：表示表单、筛选条件、接口数据或组件状态中的 PAGE_ROLES 值。
   */
  PAGE_ROLES: Medal,
  /**
   * 字段 PAGE_MENUS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_MENUS 值。
   */
  PAGE_MENUS: Menu,
  /**
   * 字段 PAGE_SUBJECTS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_SUBJECTS 值。
   */
  PAGE_SUBJECTS: Notebook,
  /**
   * 字段 PAGE_VOUCHERS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_VOUCHERS 值。
   */
  PAGE_VOUCHERS: Tickets,
  /**
   * 字段 PAGE_ACCOUNTING_PLATFORM：表示会计平台菜单使用的图标组件。
   */
  PAGE_ACCOUNTING_PLATFORM: Notebook,
  /**
   * 字段 PAGE_ACCOUNTING_PERIODS：表示会计期间菜单使用的图标组件。
   */
  PAGE_ACCOUNTING_PERIODS: Clock,
  /**
   * 字段 PAGE_CASHIER：表示出纳管理菜单使用的图标组件。
   */
  PAGE_CASHIER: Tickets,
  /**
   * 字段 PAGE_PURCHASE：表示表单、筛选条件、接口数据或组件状态中的 PAGE_PURCHASE 值。
   */
  PAGE_PURCHASE: ShoppingCart,
  /**
   * 字段 PAGE_SHIPMENTS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_SHIPMENTS 值。
   */
  PAGE_SHIPMENTS: Van,
  /**
   * 字段 PAGE_INVENTORY：表示表单、筛选条件、接口数据或组件状态中的 PAGE_INVENTORY 值。
   */
  PAGE_INVENTORY: Box,
  /**
   * 字段 PAGE_AR_AP：表示表单、筛选条件、接口数据或组件状态中的 PAGE_AR_AP 值。
   */
  PAGE_AR_AP: Files,
  /**
   * 字段 PAGE_WORKFLOW_CENTER：表示审批中心页面使用的图标组件。
   */
  PAGE_WORKFLOW_CENTER: Tickets,
  /**
   * 字段 PAGE_WORKFLOW_CONFIGS：表示流程管理页面使用的图标组件。
   */
  PAGE_WORKFLOW_CONFIGS: List,
  /**
   * 字段 PAGE_WORKFLOW_DEFINITIONS：表示流程定义页面使用的图标组件。
   */
  PAGE_WORKFLOW_DEFINITIONS: Tickets,
  /**
   * 字段 PAGE_REPORTS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_REPORTS 值。
   */
  PAGE_REPORTS: Histogram,
  /**
   * 字段 PAGE_ASSISTANT：表示表单、筛选条件、接口数据或组件状态中的 PAGE_ASSISTANT 值。
   */
  PAGE_ASSISTANT: SystemLogo,
  /**
   * 字段 PAGE_AI_STATUS：表示 AI 组件状态页面使用的图标组件。
   */
  PAGE_AI_STATUS: Monitor,
  /**
   * 字段 PAGE_SEARCH：表示表单、筛选条件、接口数据或组件状态中的 PAGE_SEARCH 值。
   */
  PAGE_SEARCH: Search,
  /**
   * 字段 PAGE_OPERATION_LOGS：表示表单、筛选条件、接口数据或组件状态中的 PAGE_OPERATION_LOGS 值。
   */
  PAGE_OPERATION_LOGS: List
}

/**
 * 常量 visibleMenus：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const visibleMenus = computed(() => buildVisibleMenus(auth.menus))

/**
 * 常量 activeTitle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const activeTitle = computed(() => {
  return findVisibleLeafMenu(route.path)?.label || 'Ratel FM'
})
/**
 * 常量 openedTabs：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const openedTabs = ref<OpenedTab[]>([])
/**
 * 常量 activeTab：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const activeTab = ref('')
/**
 * 常量 serverTime：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const serverTime = ref<Date>()
/**
 * 常量 weather：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const weather = ref<WeatherView>()
/**
 * 常量 weatherPosition：保存浏览器定位到的经纬度；为空时后端使用配置中的默认地区。
 */
const weatherPosition = ref<{ latitude: number; longitude: number; accuracy?: number; locationSource?: 'BROWSER' | 'IP'; locationName?: string }>()
/**
 * 常量 weatherLocationTried：记录本轮页面生命周期是否已经尝试请求浏览器定位，避免每分钟重复弹授权。
 */
const weatherLocationTried = ref(false)
/**
 * 常量 weatherLocationStatus：记录浏览器天气定位状态，用于给用户明确反馈定位授权是否可用。
 */
const weatherLocationStatus = ref<'idle' | 'locating' | 'ready' | 'ip-locating' | 'ip-ready' | 'unsupported' | 'insecure' | 'denied' | 'timeout' | 'unavailable'>('idle')
/**
 * 变量 clockTimer：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let clockTimer: number | undefined
/**
 * 变量 statusTimer：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let statusTimer: number | undefined
/** 系统状态后台轮询连续失败次数。 */
let statusFailureCount = 0
/** 防止慢请求跨越 60 秒间隔后产生重叠请求。 */
let statusRequestRunning = false
/** 系统状态连续失败上限，达到后停止本次页面生命周期内的轮询。 */
const MAX_STATUS_FAILURES = 5
/**
 * 常量 userInitial：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const userInitial = computed(() => (auth.user?.realName || auth.user?.username || 'U').slice(0, 1))
/**
 * 常量 canEditProfile：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canEditProfile = computed(() => auth.hasMenu('BTN_PROFILE_EDIT') && !auth.user?.defaultAccount)
/**
 * 常量 canChangePassword：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canChangePassword = computed(() => auth.hasMenu('BTN_PROFILE_PASSWORD') && !auth.user?.defaultAccount)
/**
 * 常量 canUploadAvatar：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const canUploadAvatar = computed(() => auth.hasMenu('BTN_PROFILE_AVATAR') && !auth.user?.defaultAccount)
/**
 * 常量 profileVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileVisible = ref(false)
/**
 * 常量 passwordVisible：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordVisible = ref(false)
/**
 * 常量 profileFormRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileFormRef = ref<FormInstance>()
/**
 * 常量 passwordFormRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordFormRef = ref<FormInstance>()
/**
 * 常量 profileAvatarFile：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileAvatarFile = ref<UploadRawFile>()
/**
 * 常量 profileAvatarPreview：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileAvatarPreview = ref('')
/**
 * 常量 profileForm：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileForm = reactive({
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: '',
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: '',
  /**
   * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
   */
  phone: '',
  /**
   * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
   */
  email: ''
})
/**
 * 常量 passwordForm：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordForm = reactive({
  /**
   * 字段 oldPassword：表示表单、筛选条件、接口数据或组件状态中的 oldPassword 值。
   */
  oldPassword: '',
  /**
   * 字段 newPassword：表示表单、筛选条件、接口数据或组件状态中的 newPassword 值。
   */
  newPassword: ''
})

/**
 * 常量 weatherIconMap：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const weatherIconMap: Record<string, Component> = {
  /**
   * 字段 SUNNY：表示表单、筛选条件、接口数据或组件状态中的 SUNNY 值。
   */
  SUNNY: Sunny,
  /**
   * 字段 PARTLY_CLOUDY：表示表单、筛选条件、接口数据或组件状态中的 PARTLY_CLOUDY 值。
   */
  PARTLY_CLOUDY: PartlyCloudy,
  /**
   * 字段 CLOUDY：表示表单、筛选条件、接口数据或组件状态中的 CLOUDY 值。
   */
  CLOUDY: Cloudy,
  /**
   * 字段 DRIZZLE：表示表单、筛选条件、接口数据或组件状态中的 DRIZZLE 值。
   */
  DRIZZLE: Pouring,
  /**
   * 字段 RAIN：表示表单、筛选条件、接口数据或组件状态中的 RAIN 值。
   */
  RAIN: Pouring,
  /**
   * 字段 SNOW：表示表单、筛选条件、接口数据或组件状态中的 SNOW 值。
   */
  SNOW: ColdDrink,
  /**
   * 字段 THUNDER：表示表单、筛选条件、接口数据或组件状态中的 THUNDER 值。
   */
  THUNDER: Lightning
}

/**
 * 常量 formattedServerTime：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const formattedServerTime = computed(() => formatDateTime(serverTime.value))
/**
 * 常量 formattedLunarDate：根据右上角当前时间计算农历日期，供悬浮提示第二行展示。
 */
const formattedLunarDate = computed(() => formatLunarDate(serverTime.value))
/**
 * 常量 weatherSummary：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const weatherSummary = computed(() => {
  if (!weather.value?.available) {
    return '天气'
  }
  return formatTemperature(weather.value.temperature)
})
/**
 * 常量 weatherSourceText：展示天气位置来源和数据来源，区分浏览器定位与配置兜底。
 */
const weatherSourceText = computed(() => {
  if (!weather.value?.available) {
    return weather.value?.errorMessage || '天气暂不可用'
  }
  const locationText = weatherLocationSourceText(weather.value.locationSource)
  return `${locationText} · ${weather.value.source}`
})
/**
 * 常量 weatherLocating：标识天气按钮是否正在等待浏览器返回经纬度。
 */
const weatherLocating = computed(() => weatherLocationStatus.value === 'locating' || weatherLocationStatus.value === 'ip-locating')
/**
 * 常量 weatherLocationHint：展示天气定位状态；按钮已表达操作含义，只在异常或已定位时补充结果。
 */
const weatherLocationHint = computed(() => {
  if (weatherLocationStatus.value === 'ready') {
    return '已按当前位置刷新'
  }
  if (weatherLocationStatus.value === 'ip-ready') {
    return '已按公网 IP 粗定位刷新'
  }
  if (weatherLocationStatus.value === 'insecure') {
    return '当前访问地址不允许浏览器精确定位'
  }
  if (weatherLocationStatus.value === 'unsupported') {
    return '当前浏览器不支持定位'
  }
  if (weatherLocationStatus.value === 'denied') {
    return '定位授权已被拒绝'
  }
  if (weatherLocationStatus.value === 'timeout') {
    return '定位超时，已使用默认地区'
  }
  if (weatherLocationStatus.value === 'unavailable') {
    return '无法获取当前位置'
  }
  if (weather.value?.locationSource === 'IP') {
    return '当前使用公网 IP 粗定位'
  }
  return weather.value?.locationSource === 'BROWSER' ? '当前位置' : '当前使用默认地区'
})
/**
 * 常量 weatherLocationActionTitle：天气按钮悬浮提示，说明点击后会申请浏览器定位权限。
 */
const weatherLocationActionTitle = computed(() => {
  if (weatherLocating.value) {
    return '正在获取当前位置'
  }
  return '点击刷新当前位置天气'
})
/**
 * 常量 profileRules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const profileRules: FormRules = {
  /**
   * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
   */
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { validator: validateChineseName, trigger: 'blur' }
  ],
  /**
   * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
   */
  identityNo: [{ validator: validateOptionalIdentityNo, trigger: 'blur' }],
  /**
   * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
   */
  phone: [{ validator: validateContactPhone, trigger: 'blur' }],
  /**
   * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
   */
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 120, message: '邮箱长度不能超过120个字符', trigger: 'blur' }
  ]
}
/**
 * 常量 passwordRules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const passwordRules: FormRules = {
  /**
   * 字段 oldPassword：表示表单、筛选条件、接口数据或组件状态中的 oldPassword 值。
   */
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { max: 72, message: '原密码长度不能超过72个字符', trigger: 'blur' }
  ],
  /**
   * 字段 newPassword：表示表单、筛选条件、接口数据或组件状态中的 newPassword 值。
   */
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 72, message: '新密码长度必须在6到72个字符之间', trigger: 'blur' }
  ]
}

watch(
  () => route.path,
  (path) => {
    openTabForRoute(path)
  },
  { immediate: true }
)

onMounted(() => {
  loadSystemStatus()
  loadBrowserWeatherLocation()
  clockTimer = window.setInterval(tickServerTime, 1000)
  statusTimer = window.setInterval(loadSystemStatus, 60_000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
  if (statusTimer) {
    window.clearInterval(statusTimer)
  }
})

/**
 * 加载系统状态。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：从后端读取服务器时间和天气；服务器时间用于页面本地秒级递增，天气用于右上角天气入口和悬浮预报列表。
 */
async function loadSystemStatus() {
  if (statusRequestRunning) {
    return
  }
  statusRequestRunning = true
  try {
    /**
     * 常量 status：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const status = await api.systemStatus(weatherPosition.value)
    serverTime.value = new Date(status.serverTime)
    weather.value = status.weather
    statusFailureCount = 0
  } catch {
    // 系统状态只用于右上角辅助展示，请求失败不阻断主页面操作。
    statusFailureCount += 1
    if (statusFailureCount >= MAX_STATUS_FAILURES && statusTimer) {
      window.clearInterval(statusTimer)
      statusTimer = undefined
    }
  } finally {
    statusRequestRunning = false
  }
}

/**
 * 尝试读取浏览器定位并刷新当前位置天气。
 *
 * 实现步骤：
 * 1. 当前浏览器不支持定位或非安全上下文时直接保留配置地区天气；
 * 2. 浏览器返回经纬度后保存位置，并立即刷新天气；
 * 3. 用户拒绝授权、定位超时或系统无法定位时静默兜底，避免干扰业务操作。
 */
async function loadBrowserWeatherLocation() {
  if (weatherLocationTried.value) {
    return
  }
  await locateWeatherByBrowser(true)
}

/**
 * 用户主动申请浏览器定位并刷新天气。
 */
async function requestWeatherLocation() {
  await locateWeatherByBrowser(false)
}

/**
 * 统一执行浏览器天气定位。
 *
 * 实现步骤：
 * 1. 校验浏览器定位能力和安全上下文；
 * 2. 调用 Geolocation API 触发授权或读取已授权位置；
 * 3. 成功后带经纬度刷新系统状态，失败时保留配置默认地区。
 */
async function locateWeatherByBrowser(silent: boolean) {
  const unavailableMessage = weatherGeolocationUnavailableMessage()
  if (unavailableMessage) {
    await locateWeatherByPublicIp(silent, unavailableMessage)
    return
  }
  weatherLocationTried.value = true
  weatherLocationStatus.value = 'locating'
  try {
    const position = await currentBrowserPosition()
    weatherPosition.value = {
      latitude: Number(position.coords.latitude.toFixed(6)),
      longitude: Number(position.coords.longitude.toFixed(6)),
      accuracy: Number.isFinite(position.coords.accuracy) ? Math.round(position.coords.accuracy) : undefined,
      locationSource: 'BROWSER'
    }
    weatherLocationStatus.value = 'ready'
    await loadSystemStatus()
    if (!silent) {
      ElMessage.success('已按当前位置刷新天气')
    }
  } catch (error) {
    weatherLocationStatus.value = weatherGeolocationErrorStatus(error)
    if (!silent) {
      ElMessage.warning(weatherGeolocationErrorMessage(error))
    }
  }
}

/**
 * 使用公网 IP 粗定位刷新天气。
 */
async function locateWeatherByPublicIp(silent: boolean, reason: string) {
  weatherLocationTried.value = true
  weatherLocationStatus.value = 'ip-locating'
  if (!silent && reason) {
    ElMessage.info(`${reason}，改用公网 IP 粗定位`)
  }
  try {
    const location = await locateByPublicIp()
    weatherPosition.value = {
      latitude: location.latitude,
      longitude: location.longitude,
      locationSource: 'IP',
      locationName: location.locationName
    }
    weatherLocationStatus.value = 'ip-ready'
    await loadSystemStatus()
    if (!silent) {
      ElMessage.success(location.locationName ? `已按公网 IP 粗定位刷新天气：${location.locationName}` : '已按公网 IP 粗定位刷新天气')
    }
  } catch {
    weatherLocationStatus.value = window.isSecureContext === false ? 'insecure' : 'unavailable'
    if (!silent) {
      ElMessage.warning('公网 IP 粗定位失败，天气已使用配置默认地区')
    }
  }
}

/**
 * 判断当前页面是否可以调用浏览器定位。
 */
function weatherGeolocationUnavailableMessage() {
  if (!('geolocation' in navigator)) {
    return '当前浏览器不支持精确定位'
  }
  if (window.isSecureContext === false) {
    return '当前通过 HTTP IP 访问，浏览器不会弹出精确定位授权'
  }
  return ''
}

/**
 * 把浏览器定位异常转换为页面状态。
 */
function weatherGeolocationErrorStatus(error: unknown) {
  const code = typeof error === 'object' && error !== null && 'code' in error ? Number((error as GeolocationPositionError).code) : 0
  if (code === 1) {
    return 'denied'
  }
  if (code === 3) {
    return 'timeout'
  }
  return 'unavailable'
}

/**
 * 把浏览器定位异常转换为用户可读提示。
 */
function weatherGeolocationErrorMessage(error: unknown) {
  const status = weatherGeolocationErrorStatus(error)
  if (status === 'denied') {
    return '定位授权被拒绝，天气已使用配置默认地区'
  }
  if (status === 'timeout') {
    return '定位超时，天气已使用配置默认地区'
  }
  return '无法获取当前位置，天气已使用配置默认地区'
}

/**
 * 把浏览器 Geolocation 回调封装为 Promise。
 */
function currentBrowserPosition() {
  return new Promise<GeolocationPosition>((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: false,
      timeout: 5000,
      maximumAge: 600000
    })
  })
}

/**
 * 推进服务器时间显示。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：后端每分钟校准一次服务器时间，前端每秒在最近一次服务器时间基础上递增，减少接口请求。
 */
function tickServerTime() {
  if (!serverTime.value) {
    return
  }
  serverTime.value = new Date(serverTime.value.getTime() + 1000)
}

/**
 * 根据后端天气图标类型匹配 Element Plus 图标组件。
 */
function weatherIcon(iconType?: string): Component {
  return weatherIconMap[iconType || ''] || Cloudy
}

/**
 * 转换天气位置来源显示文本。
 */
function weatherLocationSourceText(source?: string) {
  if (source === 'BROWSER') {
    return '浏览器定位'
  }
  if (source === 'IP') {
    return '公网IP粗定位'
  }
  return '配置默认地区'
}

/**
 * 格式化服务器时间为年月日时分秒。
 */
function formatDateTime(value?: Date) {
  if (!value || Number.isNaN(value.getTime())) {
    return '--'
  }
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

/**
 * 格式化农历日期。
 *
 * 实现步骤：
 * 1. 先校验时间对象是否有效；
 * 2. 使用浏览器 Intl 中文农历日历得到干支年、农历月和农历日；
 * 3. 把阿拉伯数字日期规范为中文日期，最终返回“农历丙午年五月十八”这类文案。
 */
function formatLunarDate(value?: Date) {
  if (!value || Number.isNaN(value.getTime())) {
    return '农历 --'
  }
  try {
    /** 浏览器农历日历拆分出的年月日部件，用于组装中文农历提示。 */
    const lunarParts = new Intl.DateTimeFormat('zh-CN-u-ca-chinese', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }).formatToParts(value)
    return `农历${normalizeLunarParts(lunarParts, value)}`
  } catch {
    return '农历 --'
  }
}

/**
 * 规范化 Intl 返回的农历年月日部件。
 *
 * 实现步骤：
 * 1. 从 Intl 部件中读取 yearName、month 和 day；
 * 2. yearName 缺失时按公历年份计算干支年兜底；
 * 3. 将日期数字转换为初一、十五、廿九等中文农历日写法。
 */
function normalizeLunarParts(parts: Array<{ type: string; value: string }>, value: Date) {
  /** 农历干支年，Intl 缺失时按公历年份计算兜底值。 */
  const yearName = parts.find((part) => part.type === 'yearName')?.value || sexagenaryYear(value.getFullYear())
  /** 农历月份文本，直接使用浏览器返回的中文月份。 */
  const month = parts.find((part) => part.type === 'month')?.value || ''
  /** 农历日原始值，部分浏览器返回数字，需要转换为中文日名。 */
  const rawDay = parts.find((part) => part.type === 'day')?.value || ''
  /** 农历日中文表达，例如初一、十五、廿九。 */
  const day = /^\d+$/.test(rawDay) ? toChineseLunarDay(Number(rawDay)) : rawDay.replace(/日$/, '')
  return `${yearName}年${month}${day}`
}

/**
 * 按公历年份计算中国干支年。
 *
 * 实现步骤：以 1984 年甲子年为基准，分别对天干和地支取模得到干支年；用于少数浏览器不返回 Intl yearName 时兜底。
 */
function sexagenaryYear(year: number) {
  /** 十天干顺序，用于按年份偏移计算干支年。 */
  const heavenlyStems = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸']
  /** 十二地支顺序，用于按年份偏移计算干支年。 */
  const earthlyBranches = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥']
  /** 相对 1984 甲子年的年份偏移，支持正负年份差。 */
  const offset = year - 1984
  return `${heavenlyStems[positiveModulo(offset, heavenlyStems.length)]}${earthlyBranches[positiveModulo(offset, earthlyBranches.length)]}`
}

/**
 * 计算非负取模值。
 *
 * 实现步骤：先做普通取模，再加模数并再次取模，保证负年份差值也能得到合法数组下标。
 */
function positiveModulo(value: number, divisor: number) {
  return ((value % divisor) + divisor) % divisor
}

/**
 * 把农历日期数字转换为中文日期。
 *
 * 实现步骤：按农历常用日名规则处理 1-30；范围外数字原样返回，避免异常日期导致悬浮提示为空。
 */
function toChineseLunarDay(day: number) {
  /** 中文数字基础表，用于拼装初十、廿九等农历日。 */
  const chineseDigits = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  if (day < 1 || day > 30 || !Number.isInteger(day)) {
    return String(day)
  }
  if (day <= 10) {
    return `初${chineseDigits[day - 1]}`
  }
  if (day < 20) {
    return `十${chineseDigits[day - 11]}`
  }
  if (day === 20) {
    return '二十'
  }
  if (day < 30) {
    return `廿${chineseDigits[day - 21]}`
  }
  return '三十'
}

/**
 * 格式化天气小时为月日时分。
 */
function formatWeatherTime(value?: string) {
  if (!value) {
    return '--'
  }
  /**
   * 常量 normalized：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const normalized = value.replace('T', ' ')
  return normalized.length >= 16 ? normalized.slice(5, 16) : normalized
}

/**
 * 格式化温度。
 */
function formatTemperature(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }
  return `${Number(value).toFixed(1).replace(/\.0$/, '')}℃`
}

/**
 * 格式化紫外线指数。
 */
function formatUvIndex(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}

/**
 * 执行 pad 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function pad(value: number) {
  return String(value).padStart(2, '0')
}

/**
 * 查找当前登录人已授权且可显示的叶子菜单。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：先遍历分组菜单中的页面，再遍历普通页面；只返回已经通过授权过滤后的页面，避免未授权页面进入页签。
 */
function findVisibleLeafMenu(path: string): LeafMenu | undefined {
  for (const item of visibleMenus.value) {
    if ('children' in item) {
      /**
       * 常量 activeChild：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const activeChild = item.children.find((child) => child.path === path)
      if (activeChild) {
        return activeChild
      }
      continue
    }
    if (item.path === path) {
      return item
    }
  }
  return undefined
}

/**
 * 获取当前授权菜单中的第一个可访问页面。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：按左侧导航的展示顺序扫描；模块菜单取第一个页面；普通页面直接返回，用于关闭全部页签后的默认落点。
 */
function firstVisibleLeafMenu(): LeafMenu | undefined {
  for (const item of visibleMenus.value) {
    if ('children' in item) {
      /**
       * 常量 firstChild：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const firstChild = item.children[0]
      if (firstChild) {
        return firstChild
      }
      continue
    }
    return item
  }
  return undefined
}

/**
 * 按菜单管理中的模块、页面层级构建左侧导航。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：先按菜单类型拆分模块和页面；再按页面 parentId 挂到对应模块；模块内没有授权页面时不显示；如果存在无模块父级的授权页面，则作为普通一级菜单展示。
 */
function buildVisibleMenus(menus: MenuView[]): ShellMenu[] {
  /**
   * 常量 modules：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const modules = menus
    .filter((item) => item.type === 'MODULE')
    .sort(menuSort)
  /**
   * 常量 pages：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const pages = menus
    .filter((item) => item.type === 'PAGE' && Boolean(item.routePath))
    .sort(menuSort)
  /**
   * 常量 pagesByModule：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const pagesByModule = new Map<number, LeafMenu[]>()
  /**
   * 常量 topLevelPages：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const topLevelPages: LeafMenu[] = []
  for (const page of pages) {
    /**
     * 常量 leaf：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const leaf = toLeafMenu(page)
    if (page.parentId) {
      /**
       * 常量 children：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const children = pagesByModule.get(page.parentId) || []
      children.push(leaf)
      pagesByModule.set(page.parentId, children)
    } else {
      topLevelPages.push(leaf)
    }
  }
  /**
   * 常量 groupedMenus：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const groupedMenus: GroupMenu[] = []
  for (const module of modules) {
    /**
     * 常量 children：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const children = pagesByModule.get(module.id) || []
    if (children.length > 0) {
      groupedMenus.push({
        /**
         * 字段 key：表示表单、筛选条件、接口数据或组件状态中的 key 值。
         */
        key: module.code,
        /**
         * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
         */
        label: module.name,
        /**
         * 字段 icon：表示表单、筛选条件、接口数据或组件状态中的 icon 值。
         */
        icon: menuIcon(module.code),
        children
      })
    }
  }
  return [...topLevelPages, ...groupedMenus]
}

/**
 * 执行 toLeafMenu 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function toLeafMenu(menu: MenuView): LeafMenu {
  return {
    /**
     * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
     */
    path: menu.routePath || '/',
    /**
     * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
     */
    label: menu.name,
    /**
     * 字段 icon：表示表单、筛选条件、接口数据或组件状态中的 icon 值。
     */
    icon: menuIcon(menu.code),
    /**
     * 字段 menuCode：表示表单、筛选条件、接口数据或组件状态中的 menuCode 值。
     */
    menuCode: menu.code
  }
}

/**
 * 执行 menuIcon 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function menuIcon(code: string) {
  return iconMap[code] || Menu
}

/**
 * 执行 menuSort 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function menuSort(left: MenuView, right: MenuView) {
  if (left.sortOrder !== right.sortOrder) {
    return left.sortOrder - right.sortOrder
  }
  return left.id - right.id
}

/**
 * 根据路由打开或激活主工作区页签。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：根据当前路由找到授权菜单；不存在时跳过；存在时判断是否已打开，未打开则追加页签，最后同步当前激活页签。
 */
function openTabForRoute(path: string) {
  /**
   * 常量 menu：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const menu = findVisibleLeafMenu(path)
  if (!menu) {
    return
  }
  if (!openedTabs.value.some((tab) => tab.path === path)) {
    openedTabs.value.push({
      /**
       * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
       */
      path: menu.path,
      /**
       * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
       */
      label: menu.label,
      /**
       * 字段 menuCode：表示表单、筛选条件、接口数据或组件状态中的 menuCode 值。
       */
      menuCode: menu.menuCode
    })
  }
  activeTab.value = path
}

/**
 * 切换主工作区页签。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：从 Element Plus 页签对象中读取目标路由；如果目标路由不是当前页面，则通过路由跳转激活对应页面。
 */
function switchTab(tab: { props?: { name?: string | number } }) {
  /**
   * 常量 targetPath：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const targetPath = String(tab.props?.name || '')
  if (targetPath && targetPath !== route.path) {
    router.push(targetPath)
  }
}

/**
 * 判断页签是否允许关闭。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：首页作为主入口不允许关闭；最后一个页签不允许关闭，防止主工作区出现空白。
 */
function canCloseTab(path: string) {
  return path !== '/dashboard' && openedTabs.value.length > 1
}

/**
 * 关闭主工作区页签。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：先校验页签是否允许关闭；如果关闭的是当前页签，则优先切换到右侧页签，否则切换到左侧页签；最后从已打开列表移除目标页签。
 */
function removeTab(targetName: string | number) {
  /**
   * 常量 targetPath：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const targetPath = String(targetName)
  if (!canCloseTab(targetPath)) {
    return
  }
  /**
   * 常量 targetIndex：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const targetIndex = openedTabs.value.findIndex((tab) => tab.path === targetPath)
  if (targetIndex < 0) {
    return
  }
  /**
   * 常量 nextTab：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const nextTab = openedTabs.value[targetIndex + 1] || openedTabs.value[targetIndex - 1]
  openedTabs.value.splice(targetIndex, 1)
  if (targetPath === route.path && nextTab) {
    router.push(nextTab.path)
  }
}

/**
 * 关闭全部主工作区页签。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：先取得当前人员第一个有权限页面；清空旧页签后只保留该页面；如果当前路由不是该页面，则跳转过去。
 */
function closeAllTabs() {
  /**
   * 常量 firstMenu：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const firstMenu = firstVisibleLeafMenu()
  if (!firstMenu) {
    return
  }
  openedTabs.value = [{
    /**
     * 字段 path：表示表单、筛选条件、接口数据或组件状态中的 path 值。
     */
    path: firstMenu.path,
    /**
     * 字段 label：表示表单、筛选条件、接口数据或组件状态中的 label 值。
     */
    label: firstMenu.label,
    /**
     * 字段 menuCode：表示表单、筛选条件、接口数据或组件状态中的 menuCode 值。
     */
    menuCode: firstMenu.menuCode
  }]
  activeTab.value = firstMenu.path
  if (route.path !== firstMenu.path) {
    router.push(firstMenu.path)
  }
}

/**
 * 记录用户从左侧菜单进入功能的次数。
 *
 * 实现步骤：Element Plus select 只在用户点击菜单时触发，按路由反查已授权菜单并写入当前用户本地常用功能计数。
 */
function recordMenuSelect(index: string) {
  const menu = menuByPath(auth.menus, index)
  if (!menu) {
    return
  }
  recordMenuUsage(menuUsageUserKey(auth.user), menu)
}

/**
 * 展开左侧菜单栏。
 *
 * 实现步骤：鼠标进入菜单栏区域时切换为展开态，让用户可以看到完整模块和页面名称。
 */
function expandAside() {
  asideExpanded.value = true
}

/**
 * 收起左侧菜单栏。
 *
 * 实现步骤：鼠标离开菜单栏区域时切换为收起态，仅保留图标宽度，给右侧业务页面释放空间。
 */
function collapseAside() {
  asideExpanded.value = false
}

/**
 * 执行 openProfile 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openProfile() {
  profileAvatarFile.value = undefined
  profileAvatarPreview.value = ''
  Object.assign(profileForm, {
    /**
     * 字段 realName：表示表单、筛选条件、接口数据或组件状态中的 realName 值。
     */
    realName: auth.user?.realName || '',
    /**
     * 字段 identityNo：表示表单、筛选条件、接口数据或组件状态中的 identityNo 值。
     */
    identityNo: auth.user?.identityNo || '',
    /**
     * 字段 phone：表示表单、筛选条件、接口数据或组件状态中的 phone 值。
     */
    phone: auth.user?.phone || '',
    /**
     * 字段 email：表示表单、筛选条件、接口数据或组件状态中的 email 值。
     */
    email: auth.user?.email || ''
  })
  profileVisible.value = true
}

/**
 * 执行 openPassword 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function openPassword() {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '' })
  passwordVisible.value = true
}

/**
 * 执行 saveProfile 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function saveProfile() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  /**
   * 变量 user：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  let user = await api.updateProfile({ ...profileForm })
  if (profileAvatarFile.value) {
    /**
     * 常量 formData：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const formData = new FormData()
    formData.append('file', profileAvatarFile.value)
    user = await api.uploadMyAvatar(formData)
  }
  auth.updateLocalUser(user)
  await auth.loadMe()
  ElMessage.success('保存成功')
  profileVisible.value = false
}

/**
 * 执行 savePassword 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function savePassword() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  await api.changeMyPassword({ ...passwordForm })
  ElMessage.success('密码已修改')
  passwordVisible.value = false
}

/**
 * 选择个人头像。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * 实现步骤：只做本地校验和预览，不立即提交；点击个人信息弹窗“保存”后，再把头像和人员信息绑定。
 */
function selectProfileAvatar(uploadFile: UploadFile) {
  /**
   * 常量 rawFile：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }
  /**
   * 常量 message：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const message = validateAvatarImage(rawFile)
  if (message) {
    ElMessage.warning(message)
    profileAvatarFile.value = undefined
    profileAvatarPreview.value = ''
    return
  }
  profileAvatarFile.value = rawFile
  /**
   * 常量 reader：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const reader = new FileReader()
  reader.onload = () => {
    profileAvatarPreview.value = String(reader.result || '')
  }
  reader.readAsDataURL(rawFile)
}

/**
 * 执行 logout 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
async function logout() {
  await auth.logout()
  router.replace(auth.loginPath())
}
</script>

<style scoped>
.shell {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: var(--app-bg);
}

.shell > :deep(.el-container) {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.aside {
  position: relative;
  z-index: 20;
  overflow-x: hidden;
  border-right: 1px solid var(--border-color);
  background: var(--surface-color);
  transition: width 0.18s ease;
  will-change: width;
  height: 100vh;
}

.aside-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 13px;
  border-bottom: 1px solid var(--border-color);
  white-space: nowrap;
}

.brand-icon {
  flex: 0 0 38px;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  padding: 6px;
  border-radius: 6px;
  background: var(--primary-color);
  color: var(--primary-contrast);
}

.brand-text {
  overflow: hidden;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.14s ease, transform 0.14s ease;
  white-space: nowrap;
}

.aside.is-expanded .brand-text {
  opacity: 1;
  transform: translateX(0);
}

.aside-brand strong,
.aside-brand span {
  display: block;
}

.aside-brand span {
  margin-top: 2px;
  color: var(--muted-text-color);
  font-size: 12px;
}

.nav-menu {
  border-right: 0;
  padding: 8px;
  height: calc(100vh - 64px);
  overflow-y: auto;
  overflow-x: hidden;
}

.nav-menu:not(.el-menu--collapse) {
  width: 100%;
}

.nav-menu.el-menu--collapse {
  width: 63px;
}

.nav-menu.el-menu--collapse :deep(.el-sub-menu__title),
.nav-menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 18px !important;
}

.nav-menu.el-menu--collapse :deep(.el-icon) {
  margin-right: 0;
}

.topbar {
  flex: 0 0 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-color);
}

.topbar-title {
  color: var(--heading-color);
  font-size: 18px;
  font-weight: 700;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.server-clock,
.weather-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  white-space: nowrap;
  color: var(--text-color);
  font-size: 13px;
}

.server-clock {
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
}

.server-clock .el-icon {
  color: var(--primary-color);
}

:global(.clock-tooltip) {
  max-width: 220px;
}

:global(.clock-tooltip-content) {
  display: grid;
  gap: 4px;
  color: var(--text-color);
  font-size: 13px;
  line-height: 1.45;
  white-space: nowrap;
}

.weather-trigger {
  padding: 0 10px;
  border: 1px solid var(--accent-border-color);
  border-radius: 6px;
  background: var(--accent-surface-color);
  color: var(--accent-text-color);
  cursor: pointer;
}

.weather-trigger.unavailable {
  border-color: var(--border-color);
  background: var(--subtle-surface-color);
  color: var(--muted-text-color);
}

.weather-trigger.locating {
  opacity: 0.78;
}

.weather-panel {
  color: var(--heading-color);
}

.weather-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--soft-border-color);
}

.weather-panel-header strong,
.weather-panel-header span {
  display: block;
}

.weather-panel-header strong {
  font-size: 15px;
}

.weather-panel-header span {
  margin-top: 4px;
  color: var(--muted-text-color);
  font-size: 12px;
}

.weather-current {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--primary-color);
}

.weather-current strong {
  font-size: 18px;
}

.weather-current-meta {
  margin: 10px 0 8px;
  color: var(--secondary-text-color);
  font-size: 12px;
}

.weather-location-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 8px;
  color: var(--muted-text-color);
  font-size: 12px;
}

.weather-location-actions span {
  min-width: 0;
  line-height: 1.4;
}

.weather-list {
  max-height: 280px;
  overflow: auto;
}

.weather-row {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) 58px 56px;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  border-bottom: 1px solid var(--soft-border-color);
  color: var(--secondary-text-color);
  font-size: 12px;
}

.weather-row:last-child {
  border-bottom: 0;
}

.weather-row-condition {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.weather-row-condition .el-icon {
  color: var(--primary-color);
}

.weather-row strong {
  justify-self: end;
  color: var(--heading-color);
}

.weather-row-uv {
  justify-self: end;
  color: var(--muted-text-color);
  white-space: nowrap;
}

.weather-empty {
  padding: 18px 0 6px;
  color: var(--placeholder-text-color);
  text-align: center;
}

.theme-trigger,
.manual-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--subtle-surface-color);
  color: var(--text-color);
  cursor: pointer;
}

.theme-trigger {
  width: 42px;
}

.manual-trigger {
  padding: 0 10px;
  white-space: nowrap;
}

.theme-trigger:hover,
.manual-trigger:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.theme-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 999px;
  box-shadow: 0 0 0 1px var(--border-color);
}

.theme-menu-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 112px;
}

.theme-current {
  margin-left: auto;
  color: var(--primary-color);
  font-size: 12px;
}

.user-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
  color: var(--text-color);
  cursor: pointer;
}

.content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: calc(100vh - 64px);
  padding: 18px;
  overflow: hidden;
}

.workspace-tabs {
  flex: 0 0 auto;
  display: flex;
  align-items: stretch;
  gap: 10px;
  margin: -2px 0 12px;
}

.tabs-scroll {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
}

.tabs-tools {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  padding-left: 10px;
  border-left: 1px solid var(--border-color);
}

.page-tabs {
  --el-tabs-header-height: 36px;
}

.page-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.page-tabs :deep(.el-tabs__content) {
  display: none;
}

.workspace-body {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

:global(.manual-drawer .el-drawer__body) {
  padding: 18px;
  background: var(--app-bg);
}

@media (max-width: 1024px) {
  .manual-trigger span {
    display: none;
  }

  .manual-trigger {
    width: 42px;
    padding: 0;
  }
}
</style>
