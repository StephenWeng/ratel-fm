<template>
  <el-tooltip :disabled="!hasAmount" placement="top" :show-after="200" popper-class="amount-text-tooltip">
    <template #content>
      <div class="amount-tooltip-content">
        <div><span>数字金额：</span><strong>{{ numericText }}</strong></div>
        <div><span>中文大写：</span><strong>{{ chineseText }}</strong></div>
      </div>
    </template>
    <span class="amount-text"><slot>{{ displayText }}</slot></span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney, formatPlainMoney, toChineseCapitalAmount } from '@/utils/money'

/**
 * 金额悬浮展示组件入参。
 *
 * 实现步骤：
 * 1. value 保存实际数字金额；
 * 2. display 保存页面默认展示文本；
 * 3. currencyCode 和 currencyName 用于 tooltip 中补充币种上下文；
 * 4. fractionDigits 控制数字金额小数位。
 */
interface Props {
  /** 实际金额值，用于生成数字金额和中文大写金额。 */
  value?: number | string | null
  /** 页面上默认展示的金额文本，未传时按金额和币种自动生成。 */
  display?: string
  /** 币种编码，例如 CNY、USD。 */
  currencyCode?: string
  /** 币种名称，例如人民币、美元。 */
  currencyName?: string
  /** 金额小数位，系统默认金额精度为 8 位。 */
  fractionDigits?: number
}

/** 组件属性，默认保留 8 位金额小数。 */
const props = withDefaults(defineProps<Props>(), {
  fractionDigits: 8
})

/** 转换后的数字金额，用于 tooltip 和默认展示。 */
const numericValue = computed(() => Number(props.value ?? 0))

/** 是否具备可展示的金额值，非法金额时禁用 tooltip。 */
const hasAmount = computed(() => props.value !== undefined && props.value !== null && props.value !== '' && Number.isFinite(numericValue.value))

/** tooltip 中显示的不带千分位数字金额。 */
const numericText = computed(() => {
  /** 数字金额基础文本，固定小数位并保留原始数字形态。 */
  const amountText = formatPlainMoney(numericValue.value, props.fractionDigits)
  return props.currencyCode ? `${amountText} ${props.currencyCode}` : amountText
})

/** 中文大写金额的币种前缀，人民币按标准写法输出。 */
const currencyPrefix = computed(() => {
  if (!props.currencyCode || props.currencyCode === 'CNY' || props.currencyName === '人民币') {
    return '人民币'
  }
  return props.currencyName || props.currencyCode
})

/** tooltip 中显示的中文大写金额。 */
const chineseText = computed(() => toChineseCapitalAmount(numericValue.value, currencyPrefix.value))

/** 页面默认展示文本，优先使用调用方传入的既有格式。 */
const displayText = computed(() => {
  if (props.display) {
    return props.display
  }
  /** 默认格式化金额文本，列表中仍保持千分位和 8 位小数。 */
  const amountText = formatMoney(numericValue.value, props.fractionDigits)
  return props.currencyCode ? `${amountText} ${props.currencyCode}` : amountText
})
</script>

<style scoped>
.amount-text {
  cursor: help;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>

<style>
.amount-text-tooltip .amount-tooltip-content {
  display: grid;
  gap: 4px;
  line-height: 1.6;
}

.amount-text-tooltip strong {
  font-weight: 700;
}
</style>
