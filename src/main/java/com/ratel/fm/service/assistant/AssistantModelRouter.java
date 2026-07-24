package com.ratel.fm.service.assistant;

import com.ratel.fm.service.ai.AiModelUseCase;
import com.ratel.fm.service.ai.LargeModelRouter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ratel 助手模型路由器。
 *
 * <p>把操作指令、业务问答和复杂财务推理映射到不同模型使用场景，避免问答服务直接关心模型选择细节。</p>
 */
@Component
public class AssistantModelRouter {

    private final LargeModelRouter largeModelRouter;

    public AssistantModelRouter(LargeModelRouter largeModelRouter) {
        this.largeModelRouter = largeModelRouter;
    }

    public ModelRoute route(String question, String mode) {
        String text = value(question).trim();
        if (isCommandQuestion(text, mode)) {
            return modelRoute("语音/操作指令", AiModelUseCase.COMMAND);
        }
        if (isReasoningQuestion(text)) {
            return modelRoute("复杂分析", AiModelUseCase.REASONING);
        }
        return modelRoute("业务问答", AiModelUseCase.CHAT);
    }

    public boolean available(ModelRoute modelRoute) {
        return largeModelRouter.available(modelRoute.useCase());
    }

    public String displayName(ModelRoute modelRoute) {
        return largeModelRouter.displayName(modelRoute.useCase(), modelRoute.label());
    }

    public LargeModelRouter largeModelRouter() {
        return largeModelRouter;
    }

    private ModelRoute modelRoute(String label, AiModelUseCase useCase) {
        return new ModelRoute(
                label,
                useCase,
                largeModelRouter.selectedProviderCode(),
                primaryModel(largeModelRouter.primaryModel(useCase)),
                largeModelRouter.candidateModels(useCase)
        );
    }

    private boolean isCommandQuestion(String question, String mode) {
        if ("command".equals(mode)) {
            return true;
        }
        return question.length() <= 80 && containsAny(question,
                "打开", "进入", "跳转", "切换", "新增", "编辑", "查询", "搜索", "筛选",
                "填写", "填充", "选择", "点击", "关闭", "取消", "重置", "导出", "下载");
    }

    private boolean isReasoningQuestion(String question) {
        return FinancialIntentTerms.isReasoningQuestion(question) || containsAny(question,
                "分析", "原因", "为什么", "趋势", "预测", "风险", "异常", "对比",
                "同比", "环比", "占比", "报表", "利润", "现金流", "资产负债",
                "试算平衡", "逾期", "到期", "汇总", "统计", "建议", "优化");
    }

    private String primaryModel(String model) {
        return model == null || model.isBlank() ? "qwen2.5:7b" : model;
    }

    private boolean containsAny(String source, String... items) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String item : items) {
            if (source.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record ModelRoute(String label, AiModelUseCase useCase, String provider, String primaryModel, List<String> models) {
    }
}
