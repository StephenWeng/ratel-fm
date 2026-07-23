/**
 * 语音命令风险策略。
 *
 * 语音可以帮助用户打开页面、查询和填表，但新增、修改、删除、提交、核销、过账等写操作必须经过用户检查和明确确认。
 */
export interface VoiceRiskEvaluation {
  /**
   * 字段 explicitWriteConfirmation：表示用户是否说出了明确写操作确认口令。
   */
  explicitWriteConfirmation: boolean
  /**
   * 字段 requiresManualCheck：表示该语音包含写操作或泛确认，需要先提示用户检查。
   */
  requiresManualCheck: boolean
  /**
   * 字段 destructive: 表示该语音可能删除、作废、取消或反向业务状态。
   */
  destructive: boolean
  /**
   * 字段 message：命中风险时展示给用户的中文说明。
   */
  message: string
}

/**
 * 写操作确认口令，只接受“确认保存/确认提交/确认删除”等完整表达。
 */
const explicitConfirmationPattern = /^确认(保存|保存起|存起|提交|提交起|删除|删掉|删了|批量删除|批量删掉|批量删了|修改|改了|新增|核销|过账|作废|取消)$/
/**
 * 高风险业务动作，语音识别到后不能直接替用户最终确认。
 */
const writeIntentPattern = /(保存|保存起|存起|提交|提交起|删除|删掉|删了|删哈|删咯|批量删除|批量删掉|批量删了|删除选中|删除已选|删除选择|核销|过账|作废|反结账|关闭会计期间|取消)/
/**
 * 破坏性或反向状态动作，需要更严格的确认口令。
 */
const destructiveIntentPattern = /(删除|删掉|删了|删哈|删咯|批量删除|批量删掉|批量删了|删除选中|删除已选|删除选择|作废|反结账|取消|关闭会计期间)/
/**
 * 泛确认表达没有动作对象，不能作为写操作最终确认依据。
 */
const vagueConfirmationPattern = /^(确认|确定|同意|好的|可以|要得|要的|行|好嘛|可以嘛|阔以)$/

/**
 * 评估语音命令风险。
 */
export function evaluateVoiceCommandRisk(text: string): VoiceRiskEvaluation {
  const normalized = normalize(text)
  const explicitWriteConfirmation = explicitConfirmationPattern.test(normalized)
  const destructive = destructiveIntentPattern.test(normalized)
  const writeIntent = writeIntentPattern.test(normalized)
  const vagueConfirmation = vagueConfirmationPattern.test(normalized)
  return {
    explicitWriteConfirmation,
    requiresManualCheck: !explicitWriteConfirmation && (writeIntent || vagueConfirmation),
    destructive,
    message: riskMessage(destructive, vagueConfirmation)
  }
}

/**
 * 标准化语音文本，减少空白和标点差异对风险判断的影响。
 */
function normalize(text: string) {
  return (text || '').replace(/[，。！？,.!?；;：:\s]/g, '').trim()
}

/**
 * 生成风险提示文案。
 */
function riskMessage(destructive: boolean, vagueConfirmation: boolean) {
  if (vagueConfirmation) {
    return '请说出明确操作，例如“确认保存”或“确认删除”，单独确认不会执行写操作'
  }
  if (destructive) {
    return '删除、作废、取消等操作需要先检查对象，确认框出现后再说明确确认口令'
  }
  return '写操作需要先检查表单内容，确认无误后再说“确认保存”或“确认提交”'
}
