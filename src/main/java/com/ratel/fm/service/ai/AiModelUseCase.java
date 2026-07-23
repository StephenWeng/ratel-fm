package com.ratel.fm.service.ai;

/**
 * 大模型业务使用场景。
 *
 * <p>用于把业务意图和具体模型名称解耦，后续新增 DeepSeek、GLM 等提供方时只需要补充 provider 实现。</p>
 */
public enum AiModelUseCase {
    /**
     * 普通业务问答。
     */
    CHAT,
    /**
     * 语音控制、菜单跳转、填表等短指令。
     */
    COMMAND,
    /**
     * 复杂分析、原因解释、趋势判断等推理任务。
     */
    REASONING,
    /**
     * 智能检索 query 改写。
     */
    QUERY_REWRITE
}
