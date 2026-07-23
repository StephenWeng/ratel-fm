import { describe, expect, it } from 'vitest'
import { evaluateVoiceCommandRisk } from '@/utils/voiceRiskPolicy'

describe('voiceRiskPolicy', () => {
  it('does not treat vague Sichuan confirmations as final write confirmation', () => {
    const risk = evaluateVoiceCommandRisk('要得')

    expect(risk.explicitWriteConfirmation).toBe(false)
    expect(risk.requiresManualCheck).toBe(true)
    expect(risk.destructive).toBe(false)
  })

  it('accepts explicit Sichuan save confirmation only when action is included', () => {
    const risk = evaluateVoiceCommandRisk('确认保存起')

    expect(risk.explicitWriteConfirmation).toBe(true)
    expect(risk.requiresManualCheck).toBe(false)
  })

  it('marks colloquial delete commands as destructive and requiring manual check', () => {
    const risk = evaluateVoiceCommandRisk('删了第一条')

    expect(risk.explicitWriteConfirmation).toBe(false)
    expect(risk.requiresManualCheck).toBe(true)
    expect(risk.destructive).toBe(true)
  })
})
