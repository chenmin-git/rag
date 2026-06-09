package com.example.campusrag.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.campusrag.service.VectorStore.SearchHit;

@Service
public class MockSparkLlmClient {
    public String generate(String question, String prompt, List<SearchHit> hits) {
        if (hits.isEmpty()) {
            if (isCasual(question)) {
                return "你好，我是校园知识库助手。你可以继续问校园卡补办、缓考申请、宿舍报修、校园网账号找回、奖助学金申请等校园事项，我会优先依据知识库资料回答。";
            }
            return "当前知识库没有检索到足够相关的资料，无法给出可靠答案。建议联系对应部门补充或确认材料。";
        }
        SearchHit top = hits.getFirst();
        StringBuilder builder = new StringBuilder();
        builder.append("根据当前校园知识库，可以这样回答：\n\n");
        builder.append(extractRelevant(question, top.content())).append("\n\n");
        if (hits.size() > 1) {
            builder.append("同时，系统还检索到 ").append(hits.size() - 1).append(" 条相关资料，可在下方来源中继续核对。");
        } else {
            builder.append("该回答仅依据下方引用来源生成，如需办理请以原文档为准。");
        }
        return builder.toString();
    }

    private String extractRelevant(String question, String content) {
        String[] sentences = content.replace('\n', ' ').split("(?<=[。！？；])");
        StringBuilder selected = new StringBuilder();
        for (String sentence : sentences) {
            String cleaned = sentence.trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (matches(question, cleaned)) {
                selected.append(cleaned);
            }
            if (selected.length() > 260) {
                break;
            }
        }
        if (!selected.isEmpty()) {
            return selected.toString();
        }
        return compact(content);
    }

    private boolean matches(String question, String sentence) {
        if (question.contains("校园卡") && sentence.contains("校园卡")) {
            return true;
        }
        if ((question.contains("补办") || question.contains("丢")) && (sentence.contains("补办") || sentence.contains("挂失") || sentence.contains("遗失"))) {
            return true;
        }
        if (question.contains("请假") && sentence.contains("请假")) {
            return true;
        }
        if (question.contains("奖学金") && sentence.contains("奖学金")) {
            return true;
        }
        if (question.contains("缓考") && sentence.contains("缓考")) {
            return true;
        }
        if (question.contains("成绩") && sentence.contains("成绩")) {
            return true;
        }
        return false;
    }

    private String compact(String content) {
        String value = content.replaceAll("\\s+", " ").trim();
        if (value.length() <= 360) {
            return value;
        }
        return value.substring(0, 360) + "...";
    }

    private boolean isCasual(String question) {
        String value = question == null ? "" : question.trim().toLowerCase();
        if (value.length() > 20) {
            return false;
        }
        return value.matches(".*(你好|您好|hello|hi|嗨|在吗|你是谁|介绍一下|谢谢|感谢).*");
    }
}
