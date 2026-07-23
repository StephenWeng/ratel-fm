<template>
  <main class="star-login-page">
    <div class="night-sky" :style="backgroundStyle" aria-hidden="true">
      <div class="moon"></div>
      <span
        v-for="star in stars"
        :key="star.id"
        class="star"
        :style="starStyle(star)"
      />
      <span
        v-for="meteor in meteors"
        :key="meteor.id"
        class="meteor"
        :style="meteorStyle(meteor)"
      />
    </div>

    <section class="star-card">
      <div class="brand-row">
        <div class="brand-mark">
          <SystemLogo />
        </div>
        <div>
          <h1>Ratel FM</h1>
          <p>星河登录</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="所属公司" prop="organizationCode">
          <el-select v-model="form.organizationCode" size="large" filterable class="full" placeholder="请选择所属公司" :loading="companiesLoading">
            <el-option v-for="item in companyOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" size="large" autocomplete="username" placeholder="请输入账号或身份证号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, type StyleValue } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import SystemLogo from '@/components/brand/SystemLogo.vue'
import { api } from '@/api/fm'
import { useAuthStore } from '@/stores/auth'
import { pageMenus } from '@/router/menuRoutes'
import type { BasicDictionaryView } from '@/types/api'
import meadowImage from '@/assets/star-meadow-unsplash.jpg'

/**
 * 星空登录页面。
 *
 * 开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。
 *
 * @author ratel
 */
interface Star {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
   */
  left: number
  /**
   * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
   */
  top: number
  /**
   * 字段 size：表示表单、筛选条件、接口数据或组件状态中的 size 值。
   */
  size: number
  /**
   * 字段 duration：表示表单、筛选条件、接口数据或组件状态中的 duration 值。
   */
  duration: number
  /**
   * 字段 delay：表示表单、筛选条件、接口数据或组件状态中的 delay 值。
   */
  delay: number
  /**
   * 字段 opacity：表示表单、筛选条件、接口数据或组件状态中的 opacity 值。
   */
  opacity: number
}

/**
 * Meteor 类型定义，用于约束页面状态、接口入参或接口返回数据结构。
 */
interface Meteor {
  /**
   * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
   */
  id: number
  /**
   * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
   */
  left: number
  /**
   * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
   */
  top: number
  /**
   * 字段 length：表示表单、筛选条件、接口数据或组件状态中的 length 值。
   */
  length: number
  /**
   * 字段 thickness：表示表单、筛选条件、接口数据或组件状态中的 thickness 值。
   */
  thickness: number
  /**
   * 字段 duration：表示表单、筛选条件、接口数据或组件状态中的 duration 值。
   */
  duration: number
  /**
   * 字段 delay：表示表单、筛选条件、接口数据或组件状态中的 delay 值。
   */
  delay: number
  /**
   * 字段 angle：表示表单、筛选条件、接口数据或组件状态中的 angle 值。
   */
  angle: number
  /**
   * 字段 travelX：表示表单、筛选条件、接口数据或组件状态中的 travelX 值。
   */
  travelX: number
  /**
   * 字段 travelY：表示表单、筛选条件、接口数据或组件状态中的 travelY 值。
   */
  travelY: number
  /**
   * 字段 glow：表示表单、筛选条件、接口数据或组件状态中的 glow 值。
   */
  glow: number
  /**
   * 字段 tailOpacity：表示表单、筛选条件、接口数据或组件状态中的 tailOpacity 值。
   */
  tailOpacity: number
}

/**
 * 常量 router：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const router = useRouter()
/**
 * 常量 auth：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const auth = useAuthStore()
/**
 * 常量 formRef：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const formRef = ref<FormInstance>()
/**
 * 常量 loading：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const loading = ref(false)
/** 所属公司下拉加载状态，避免公司列表未加载完成时误提交登录。 */
const companiesLoading = ref(false)
/** 登录页可选所属公司账套，来自后端 ORGANIZATION 启用字典。 */
const companyOptions = ref<BasicDictionaryView[]>([])
/**
 * 常量 meteorSerial：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const meteorSerial = ref(0)
/**
 * 常量 meteors：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const meteors = ref<Meteor[]>([])
/**
 * 变量 meteorTimer：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
let meteorTimer: number | undefined
/**
 * 常量 backgroundStyle：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const backgroundStyle = { '--meadow-image': `url(${meadowImage})` } as StyleValue

/**
 * 常量 form：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const form = reactive({
  /**
   * 字段 organizationCode：表示登录选择的所属公司账套编码。
   */
  organizationCode: '',
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: '',
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: ''
})

/**
 * 常量 rules：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const rules: FormRules = {
  /**
   * 字段 organizationCode：登录所属公司必填，后端据此按账套校验账号和身份证。
   */
  organizationCode: [
    { required: true, message: '请选择所属公司', trigger: 'change' },
    { max: 80, message: '所属公司编码长度不能超过80个字符', trigger: 'change' }
  ],
  /**
   * 字段 username：表示表单、筛选条件、接口数据或组件状态中的 username 值。
   */
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 80, message: '登录账号长度不能超过80个字符', trigger: 'blur' }
  ],
  /**
   * 字段 password：表示表单、筛选条件、接口数据或组件状态中的 password 值。
   */
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 72, message: '密码长度不能超过72个字符', trigger: 'blur' }
  ]
}

/**
 * 常量 stars：保存当前模块的页面状态、配置项、接口实例或计算结果。
 */
const stars = Array.from({ length: 130 }, (_, index) => createStar(index))

/**
 * 提交登录表单。
 *
 * 实现步骤：
 * 1. 校验账号和密码必填及长度；
 * 2. 调用统一登录接口；
 * 3. 遇到同身份证同终端重复登录时弹出二次确认；
 * 4. 登录成功后跳转到当前人员第一个有权限的页面。
 */
async function submit() {
  /**
   * 常量 valid：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    /**
     * 常量 result：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const result = await auth.login(form.organizationCode, form.username, form.password, false, '/login/star')
    if (result.repeated) {
      /**
       * 常量 confirmed：保存当前模块的页面状态、配置项、接口实例或计算结果。
       */
      const confirmed = await confirmForceLogin(result.conflictMessage || '当前人员已在同类终端登录，是否挤掉之前登录者？')
      if (!confirmed) {
        return
      }
      await auth.login(form.organizationCode, form.username, form.password, true, '/login/star')
    }
    ElMessage.success('登录成功')
    router.replace(firstAuthorizedPath())
  } catch (error) {
    ElMessage.error(loginErrorMessage(error))
  } finally {
    loading.value = false
  }
}

/**
 * 提取登录失败提示，确保账号或密码错误时登录页本身给出可见反馈。
 */
function loginErrorMessage(error: unknown) {
  const message = String((error as Error | undefined)?.message || '').trim()
  return message || '登录失败，请检查所属公司、账号和密码后重试。'
}

/**
 * 重复登录二次确认。
 */
async function confirmForceLogin(message: string): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, '重复登录提醒', {
      /**
       * 字段 confirmButtonText：表示表单、筛选条件、接口数据或组件状态中的 confirmButtonText 值。
       */
      confirmButtonText: '是，挤掉之前登录者',
      /**
       * 字段 cancelButtonText：表示表单、筛选条件、接口数据或组件状态中的 cancelButtonText 值。
       */
      cancelButtonText: '否',
      /**
       * 字段 type：表示表单、筛选条件、接口数据或组件状态中的 type 值。
       */
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

/**
 * 获取登录成功后的第一个有权限页面。
 */
function firstAuthorizedPath() {
  return pageMenus.find((item) => auth.hasMenu(item.menuCode))?.path || auth.loginPath()
}

/**
 * 加载登录所属公司选项。
 *
 * 实现步骤：
 * 1. 调用登录前专用接口读取启用所属公司；
 * 2. 默认选中第一家公司；
 * 3. 读取失败时回退系统预置公司编码，避免首次部署时无法登录初始化管理员。
 */
async function loadCompanies() {
  companiesLoading.value = true
  try {
    companyOptions.value = await api.loginCompanies()
    form.organizationCode = companyOptions.value[0]?.code || 'ORGANIZATION_RATEL'
  } catch {
    companyOptions.value = [{ id: 0, code: 'ORGANIZATION_RATEL', name: 'Ratel默认公司', sortOrder: 0, enabled: true, hasChildren: false, children: [] }]
    form.organizationCode = 'ORGANIZATION_RATEL'
  } finally {
    companiesLoading.value = false
  }
}

/**
 * 创建一颗拥有独立位置、大小和闪烁周期的星星。
 */
function createStar(index: number): Star {
  return {
    /**
     * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
     */
    id: index,
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: randomBetween(2, 98),
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: randomBetween(4, 62),
    /**
     * 字段 size：表示表单、筛选条件、接口数据或组件状态中的 size 值。
     */
    size: randomBetween(1.2, 3.6),
    /**
     * 字段 duration：表示表单、筛选条件、接口数据或组件状态中的 duration 值。
     */
    duration: randomBetween(2.4, 8.6),
    /**
     * 字段 delay：表示表单、筛选条件、接口数据或组件状态中的 delay 值。
     */
    delay: randomBetween(-8, 0),
    /**
     * 字段 opacity：表示表单、筛选条件、接口数据或组件状态中的 opacity 值。
     */
    opacity: randomBetween(0.44, 0.98)
  }
}

/**
 * 组装星星 CSS 变量。
 */
function starStyle(star: Star): StyleValue {
  return {
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: `${star.left}%`,
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: `${star.top}%`,
    /**
     * 字段 width：表示表单、筛选条件、接口数据或组件状态中的 width 值。
     */
    width: `${star.size}px`,
    /**
     * 字段 height：表示表单、筛选条件、接口数据或组件状态中的 height 值。
     */
    height: `${star.size}px`,
    /**
     * 字段 opacity：表示表单、筛选条件、接口数据或组件状态中的 opacity 值。
     */
    opacity: star.opacity,
    '--twinkle-duration': `${star.duration}s`,
    '--twinkle-delay': `${star.delay}s`
  } as StyleValue
}

/**
 * 组装流星 CSS 变量。
 */
function meteorStyle(meteor: Meteor): StyleValue {
  return {
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: `${meteor.left}%`,
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: `${meteor.top}%`,
    /**
     * 字段 width：表示表单、筛选条件、接口数据或组件状态中的 width 值。
     */
    width: `${meteor.length}px`,
    /**
     * 字段 height：表示表单、筛选条件、接口数据或组件状态中的 height 值。
     */
    height: `${meteor.thickness}px`,
    '--meteor-duration': `${meteor.duration}s`,
    '--meteor-delay': `${meteor.delay}s`,
    '--meteor-angle': `${meteor.angle}deg`,
    '--meteor-x': `${meteor.travelX}vw`,
    '--meteor-y': `${meteor.travelY}vh`,
    '--meteor-glow': meteor.glow,
    '--meteor-tail-opacity': meteor.tailOpacity
  } as StyleValue
}

/**
 * 安排下一批随机流星。
 *
 * 实现步骤：
 * 1. 使用随机延迟控制流星频率，避免出现过于密集的视觉干扰；
 * 2. 每批随机生成 1 到 8 颗流星；
 * 3. 4 颗以内按零散流星处理，4 颗及以上按集中流星雨处理；
 * 4. 流星结束后从响应式数组中移除，避免 DOM 长期累积。
 */
function scheduleMeteor() {
  /**
   * 常量 delay：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const delay = randomBetween(4600, 12800)
  meteorTimer = window.setTimeout(() => {
    spawnMeteorBurst()
    scheduleMeteor()
  }, delay)
}

/**
 * 生成一批随机数量、角度、速度和路径的流星。
 */
function spawnMeteorBurst() {
  /**
   * 常量 count：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const count = randomInt(1, 8)
  /**
   * 常量 created：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const created = count >= 4 ? createMeteorShower(count) : createScatteredMeteors(count)
  meteors.value.push(...created)
  window.setTimeout(() => {
    /**
     * 常量 removeIds：保存当前模块的页面状态、配置项、接口实例或计算结果。
     */
    const removeIds = new Set(created.map((item) => item.id))
    meteors.value = meteors.value.filter((item) => !removeIds.has(item.id))
  }, 4400)
}

/**
 * 创建 1 到 3 颗零散流星。
 *
 * 实现步骤：每颗流星使用独立的起点、角度、速度和长度，保持偶发划过天空的自然感。
 */
function createScatteredMeteors(count: number): Meteor[] {
  return Array.from({ length: count }, () => createMeteor({
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: randomBetween(-14, 72),
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: randomBetween(7, 45),
    /**
     * 字段 length：表示表单、筛选条件、接口数据或组件状态中的 length 值。
     */
    length: randomBetween(92, 178),
    /**
     * 字段 duration：表示表单、筛选条件、接口数据或组件状态中的 duration 值。
     */
    duration: randomBetween(1.05, 2.65),
    /**
     * 字段 delay：表示表单、筛选条件、接口数据或组件状态中的 delay 值。
     */
    delay: randomBetween(0, 0.42),
    /**
     * 字段 angle：表示表单、筛选条件、接口数据或组件状态中的 angle 值。
     */
    angle: randomBetween(18, 40),
    /**
     * 字段 travelX：表示表单、筛选条件、接口数据或组件状态中的 travelX 值。
     */
    travelX: randomBetween(35, 62),
    /**
     * 字段 travelY：表示表单、筛选条件、接口数据或组件状态中的 travelY 值。
     */
    travelY: randomBetween(18, 38)
  }))
}

/**
 * 创建 4 到 8 颗集中流星雨。
 *
 * 实现步骤：
 * 1. 先确定一个天空聚集区域和主方向；
 * 2. 每颗流星围绕聚集区域做小范围偏移；
 * 3. 使用短延迟错峰出现，形成同一片天空连续滑落的流星雨效果。
 */
function createMeteorShower(count: number): Meteor[] {
  /**
   * 常量 originLeft：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const originLeft = randomBetween(-18, 20)
  /**
   * 常量 originTop：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const originTop = randomBetween(4, 24)
  /**
   * 常量 baseAngle：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const baseAngle = randomBetween(24, 35)
  /**
   * 常量 baseTravelX：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const baseTravelX = randomBetween(48, 68)
  /**
   * 常量 baseTravelY：保存当前模块的页面状态、配置项、接口实例或计算结果。
   */
  const baseTravelY = randomBetween(24, 42)
  return Array.from({ length: count }, (_, index) => createMeteor({
    /**
     * 字段 left：表示表单、筛选条件、接口数据或组件状态中的 left 值。
     */
    left: originLeft + randomBetween(-5, 22),
    /**
     * 字段 top：表示表单、筛选条件、接口数据或组件状态中的 top 值。
     */
    top: originTop + randomBetween(-4, 18),
    /**
     * 字段 length：表示表单、筛选条件、接口数据或组件状态中的 length 值。
     */
    length: randomBetween(76, 158),
    /**
     * 字段 duration：表示表单、筛选条件、接口数据或组件状态中的 duration 值。
     */
    duration: randomBetween(1.18, 2.25),
    /**
     * 字段 delay：表示表单、筛选条件、接口数据或组件状态中的 delay 值。
     */
    delay: index * randomBetween(0.12, 0.26),
    /**
     * 字段 angle：表示表单、筛选条件、接口数据或组件状态中的 angle 值。
     */
    angle: baseAngle + randomBetween(-4, 4),
    /**
     * 字段 travelX：表示表单、筛选条件、接口数据或组件状态中的 travelX 值。
     */
    travelX: baseTravelX + randomBetween(-4, 8),
    /**
     * 字段 travelY：表示表单、筛选条件、接口数据或组件状态中的 travelY 值。
     */
    travelY: baseTravelY + randomBetween(-5, 7)
  }))
}

/**
 * 创建单颗流星数据。
 *
 * 实现步骤：补齐视觉参数，包括厚度、发光强度和尾迹透明度，让流星尾巴随动画自然淡出。
 */
function createMeteor(options: Omit<Meteor, 'id' | 'thickness' | 'glow' | 'tailOpacity'>): Meteor {
  return {
    /**
     * 字段 id：表示表单、筛选条件、接口数据或组件状态中的 id 值。
     */
    id: meteorSerial.value++,
    /**
     * 字段 thickness：表示表单、筛选条件、接口数据或组件状态中的 thickness 值。
     */
    thickness: randomBetween(1.3, 2.8),
    /**
     * 字段 glow：表示表单、筛选条件、接口数据或组件状态中的 glow 值。
     */
    glow: randomBetween(0.66, 1),
    /**
     * 字段 tailOpacity：表示表单、筛选条件、接口数据或组件状态中的 tailOpacity 值。
     */
    tailOpacity: randomBetween(0.54, 0.9),
    ...options
  }
}

/**
 * 执行 randomBetween 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function randomBetween(min: number, max: number) {
  return min + Math.random() * (max - min)
}

/**
 * 执行 randomInt 方法。
 * 
 * 实现步骤：
 * 1. 读取当前页面状态或调用参数；
 * 2. 完成对应的校验、接口调用或数据转换；
 * 3. 更新页面状态或返回处理结果。
 */
function randomInt(min: number, max: number) {
  return Math.floor(randomBetween(min, max + 1))
}

onMounted(() => {
  scheduleMeteor()
  void loadCompanies()
})

onBeforeUnmount(() => {
  if (meteorTimer) {
    window.clearTimeout(meteorTimer)
  }
})
</script>

<style scoped>
.star-login-page {
  --star-sky-bg:
    radial-gradient(circle at 72% 12%, rgba(31, 122, 90, 0.28), rgba(238, 247, 242, 0.1) 26%, transparent 44%),
    linear-gradient(180deg, #eef7f2 0%, #dceee7 48%, #d8e6df 100%);
  --star-photo-filter: brightness(0.72) saturate(0.88) contrast(1.02);
  --star-photo-mask: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.22) 15%, #000 34%);
  --star-overlay-bg:
    linear-gradient(180deg, rgba(238, 247, 242, 0.08) 0%, rgba(238, 247, 242, 0.28) 48%, rgba(238, 247, 242, 0.72) 100%),
    linear-gradient(90deg, rgba(10, 58, 48, 0.18) 0%, rgba(238, 247, 242, 0.02) 42%, rgba(10, 58, 48, 0.22) 100%);
  --star-card-bg: var(--surface-color);
  --star-card-border: var(--border-color);
  --star-card-shadow: 0 24px 70px var(--shadow-color);
  --star-card-heading: var(--heading-color);
  --star-card-text: var(--secondary-text-color);
  --star-card-label: var(--secondary-text-color);
  --star-mark-bg: var(--primary-light-color);
  --star-mark-border: var(--border-color);
  --star-mark-color: var(--primary-color);
  --star-input-bg: var(--surface-color);
  --star-input-border: var(--border-color);
  --star-button-bg: var(--primary-color);
  --star-button-border: var(--primary-color);
  position: relative;
  display: grid;
  min-height: 100vh;
  align-items: center;
  justify-items: end;
  overflow: hidden;
  padding: 32px clamp(28px, 5vw, 72px);
  background: var(--app-bg);
  color: var(--text-color);
}

:global(:root[data-theme='dark']) .star-login-page {
  --star-sky-bg:
    radial-gradient(circle at 72% 12%, rgba(58, 73, 108, 0.42), rgba(6, 13, 31, 0.08) 26%, transparent 44%),
    linear-gradient(180deg, #020617 0%, #071225 45%, #0a1825 62%, #07100d 100%);
  --star-photo-filter: brightness(0.36) saturate(0.8) contrast(1.12);
  --star-photo-mask: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.34) 15%, #000 34%);
  --star-overlay-bg:
    linear-gradient(180deg, rgba(2, 6, 17, 0) 0%, rgba(3, 9, 18, 0.34) 48%, rgba(3, 11, 13, 0.7) 100%),
    linear-gradient(90deg, rgba(3, 7, 16, 0.38) 0%, rgba(4, 12, 22, 0.04) 42%, rgba(3, 7, 16, 0.46) 100%);
  --star-card-bg: rgba(31, 41, 55, 0.9);
  --star-card-border: var(--border-color);
  --star-card-shadow: 0 24px 70px rgba(0, 0, 0, 0.42);
  --star-card-heading: var(--heading-color);
  --star-card-text: var(--secondary-text-color);
  --star-card-label: var(--secondary-text-color);
  --star-mark-bg: var(--primary-light-color);
  --star-mark-border: var(--border-color);
  --star-mark-color: var(--primary-color);
  --star-input-bg: var(--subtle-surface-color);
  --star-input-border: var(--border-color);
  --star-button-bg: var(--primary-color);
  --star-button-border: var(--primary-color);
}

:global(:root[data-theme='emerald']) .star-login-page {
  --star-sky-bg:
    radial-gradient(circle at 72% 12%, rgba(15, 118, 110, 0.34), rgba(225, 244, 239, 0.1) 26%, transparent 44%),
    linear-gradient(180deg, #e1f4ef 0%, #cbe9df 50%, #d7ede3 100%);
}

:global(:root[data-theme='finance-blue']) .star-login-page {
  --star-sky-bg:
    radial-gradient(circle at 72% 12%, rgba(37, 99, 235, 0.3), rgba(239, 246, 255, 0.1) 26%, transparent 44%),
    linear-gradient(180deg, #eff6ff 0%, #dbeafe 50%, #e5eefb 100%);
  --star-overlay-bg:
    linear-gradient(180deg, rgba(239, 246, 255, 0.08) 0%, rgba(239, 246, 255, 0.28) 48%, rgba(239, 246, 255, 0.72) 100%),
    linear-gradient(90deg, rgba(30, 64, 175, 0.2) 0%, rgba(239, 246, 255, 0.02) 42%, rgba(30, 64, 175, 0.24) 100%);
}

.night-sky {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    var(--star-sky-bg);
}

.night-sky::before {
  position: absolute;
  top: 30%;
  right: 0;
  bottom: 0;
  left: 0;
  background-image: var(--meadow-image);
  background-position: center bottom;
  background-size: cover;
  content: "";
  filter: var(--star-photo-filter);
  mask-image: var(--star-photo-mask);
  transform: scale(1.02);
}

.night-sky::after {
  position: absolute;
  inset: 0;
  background: var(--star-overlay-bg);
  content: "";
}

.moon {
  position: absolute;
  top: clamp(48px, 9vh, 92px);
  right: clamp(230px, 31vw, 520px);
  z-index: 1;
  width: clamp(62px, 7vw, 104px);
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    radial-gradient(circle at 35% 32%, rgba(255, 255, 255, 0.98), rgba(252, 241, 198, 0.92) 42%, rgba(232, 205, 127, 0.72) 72%, rgba(205, 174, 91, 0.42) 100%);
  box-shadow: 0 0 26px rgba(255, 226, 151, 0.58), 0 0 92px rgba(255, 215, 128, 0.28);
}

.moon::before,
.moon::after {
  position: absolute;
  border-radius: 50%;
  background: rgba(146, 119, 74, 0.18);
  content: "";
}

.moon::before {
  top: 28%;
  left: 26%;
  width: 18%;
  height: 18%;
}

.moon::after {
  right: 24%;
  bottom: 30%;
  width: 13%;
  height: 13%;
}

.star {
  position: absolute;
  z-index: 2;
  border-radius: 50%;
  background: #ffd76a;
  box-shadow: 0 0 8px rgba(255, 215, 106, 0.88), 0 0 18px rgba(255, 204, 88, 0.45);
  animation: twinkle var(--twinkle-duration) ease-in-out var(--twinkle-delay) infinite;
}

.star:nth-child(5n) {
  background: #fff2b4;
}

.meteor {
  position: absolute;
  z-index: 2;
  border-radius: 999px;
  opacity: 0;
  pointer-events: none;
  transform-origin: right center;
  transform: translate3d(0, 0, 0) rotate(var(--meteor-angle));
  animation: meteor-move var(--meteor-duration) linear var(--meteor-delay) forwards;
  will-change: transform, opacity;
}

.meteor::after {
  position: absolute;
  right: -2px;
  top: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #fff0aa;
  box-shadow:
    0 0 12px rgba(255, 232, 152, 0.9),
    0 0 24px rgba(255, 206, 91, 0.48);
  content: "";
  transform: translateY(-50%);
  animation: meteor-head var(--meteor-duration) ease-out var(--meteor-delay) forwards;
}

.meteor::before {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    radial-gradient(circle at 100% 50%, rgba(255, 246, 191, 0.96) 0 2px, transparent 4px),
    linear-gradient(
      90deg,
      rgba(255, 215, 106, 0) 0%,
      rgba(255, 209, 96, calc(var(--meteor-tail-opacity) * 0.12)) 26%,
      rgba(255, 219, 129, calc(var(--meteor-tail-opacity) * 0.58)) 68%,
      rgba(255, 242, 182, 0.98) 100%
    );
  box-shadow:
    0 0 calc(8px * var(--meteor-glow)) rgba(255, 213, 112, 0.5),
    0 0 calc(20px * var(--meteor-glow)) rgba(255, 232, 152, 0.36);
  content: "";
  filter: blur(0.12px);
  transform-origin: right center;
  animation: meteor-tail var(--meteor-duration) ease-out var(--meteor-delay) forwards;
  will-change: transform, opacity;
}

.star-card {
  position: relative;
  z-index: 4;
  width: min(420px, calc(100vw - 48px));
  padding: 34px;
  border: 1px solid var(--star-card-border);
  border-radius: 8px;
  background: var(--star-card-bg);
  box-shadow: var(--star-card-shadow);
  backdrop-filter: blur(10px);
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.brand-mark {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  padding: 8px;
  border: 1px solid var(--star-mark-border);
  border-radius: 6px;
  background: var(--star-mark-bg);
  color: var(--star-mark-color);
}

.brand-row h1 {
  margin: 0;
  color: var(--star-card-heading);
  font-size: 30px;
  line-height: 1.05;
}

.brand-row p {
  margin: 6px 0 0;
  color: var(--star-card-text);
}

.login-button {
  width: 100%;
  margin-top: 4px;
}

.full {
  width: 100%;
}

:deep(.el-form-item__label) {
  color: var(--star-card-label);
}

:deep(.el-input__wrapper) {
  border: 1px solid var(--star-input-border);
  background: var(--star-input-bg);
  box-shadow: none;
}

:deep(.el-button--primary) {
  border-color: var(--star-button-border);
  background: var(--star-button-bg);
}

@keyframes twinkle {
  0%,
  100% {
    transform: scale(0.75);
    opacity: 0.36;
  }

  48% {
    transform: scale(1.18);
    opacity: 1;
  }

  62% {
    transform: scale(0.92);
    opacity: 0.64;
  }
}

@keyframes meteor-move {
  0% {
    opacity: 0;
    transform: translate3d(0, 0, 0) rotate(var(--meteor-angle));
  }

  8% {
    opacity: 1;
  }

  72% {
    opacity: 1;
  }

  100% {
    opacity: 0;
    transform: translate3d(var(--meteor-x), var(--meteor-y), 0) rotate(var(--meteor-angle));
  }
}

@keyframes meteor-tail {
  0% {
    opacity: 0;
    transform: scaleX(0.18);
  }

  10% {
    opacity: 1;
    transform: scaleX(1);
  }

  68% {
    opacity: 0.82;
    transform: scaleX(0.78);
  }

  100% {
    opacity: 0;
    transform: scaleX(0.08);
  }
}

@keyframes meteor-head {
  0%,
  8% {
    opacity: 0;
  }

  13%,
  70% {
    opacity: 1;
  }

  100% {
    opacity: 0;
  }
}

@media (max-width: 620px) {
  .star-login-page {
    align-items: start;
    justify-items: center;
    padding: 28px 18px;
  }

  .star-card {
    margin-top: 26px;
    padding: 24px;
  }

  .brand-row h1 {
    font-size: 26px;
  }

  .moon {
    right: 34px;
  }
}
</style>
