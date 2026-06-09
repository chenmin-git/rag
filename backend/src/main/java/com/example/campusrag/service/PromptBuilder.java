package com.example.campusrag.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.campusrag.config.AppProperties;
import com.example.campusrag.service.VectorStore.SearchHit;

@Component
public class PromptBuilder {
    private final AppProperties properties;

    public PromptBuilder(AppProperties properties) {
        this.properties = properties;
    }

    public String build(String question, List<SearchHit> hits) {
        if (isCasual(question)) {
            return """
                    你是高校校园知识库问答系统中的智能助手。
                    用户正在进行寒暄、打招呼或询问你的身份能力。请自然、简洁地回应，并引导用户继续咨询校园卡、考试缓考、宿舍报修、校园网账号、奖助学金等校园事项。
                    不要编造具体制度、电话、日期或办理地点。

                    用户输入：
                    %s
                    """.formatted(question);
        }
        StringBuilder context = new StringBuilder();
        int usedChars = 0;
        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            String block = "资料[" + (i + 1) + "]\n来源：" + hit.fileName()
                    + "，页码：" + hit.pageNo()
                    + "，段落：" + hit.chunkNo()
                    + "，相似度：" + String.format("%.3f", hit.score())
                    + "\n内容：" + hit.content() + "\n\n";
            if (usedChars + block.length() > properties.getRag().getMaxContextChars()) {
                break;
            }
            context.append(block);
            usedChars += block.length();
        }
        return """
                你是高校校园知识库问答助手。请严格依据参考资料回答用户问题。

                规则：
                1. 只能使用参考资料中的信息，不得编造制度、日期、金额、流程或联系方式。
                2. 如果参考资料不足以回答，请明确说明无法从当前知识库找到准确答案。
                3. 回答应简洁、准确，并提示用户以引用来源原文为准。
                4. 不要输出与问题无关的背景解释。

                用户问题：
                %s

                参考资料：
                %s
                """.formatted(question, context);
    }

    private boolean isCasual(String question) {
        String value = question == null ? "" : question.trim().toLowerCase();
        if (value.length() > 20) {
            return false;
        }
        return value.matches(".*(你好|您好|hello|hi|嗨|在吗|你是谁|介绍一下|谢谢|感谢).*");
    }
}
