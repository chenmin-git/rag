package com.example.campusrag.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.campusrag.service.VectorStore.SearchHit;

@Service
public class RerankService {
    public List<SearchHit> rerank(String question, List<SearchHit> hits) {
        List<String> queryTerms = queryTerms(question);
        return hits.stream()
                .sorted(Comparator.comparingDouble((SearchHit hit) -> hybridScore(hit, queryTerms)).reversed())
                .toList();
    }

    private double hybridScore(SearchHit hit, List<String> queryTerms) {
        String content = normalize(hit.content());
        double lexical = 0;
        for (String term : queryTerms) {
            if (content.contains(term)) {
                lexical += term.length() >= 3 ? 0.12 : 0.055;
            }
        }
        return hit.score() + Math.min(0.46, lexical);
    }

    private List<String> queryTerms(String question) {
        String normalized = normalize(question);
        Set<String> terms = new LinkedHashSet<>();
        addIfContains(normalized, terms, "校园卡", "饭卡", "一卡通");
        addIfContains(normalized, terms, "补办", "重新办理", "新卡");
        addIfContains(normalized, terms, "丢", "遗失", "挂失", "丢失");
        addIfContains(normalized, terms, "请假", "病假", "事假", "审批");
        addIfContains(normalized, terms, "奖学金", "评定", "资助");
        addIfContains(normalized, terms, "缓考", "考试", "缺考");
        addIfContains(normalized, terms, "成绩", "复核", "错登");
        addIfContains(normalized, terms, "退选", "选课", "课程");
        addIfContains(normalized, terms, "密码", "账号", "统一身份认证");
        addIfContains(normalized, terms, "报修", "维修", "后勤");
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 2) {
                terms.add(token);
            }
        }
        for (int i = 0; i < normalized.length() - 1; i++) {
            String gram = normalized.substring(i, i + 2);
            if (!gram.isBlank()) {
                terms.add(gram);
            }
        }
        return new ArrayList<>(terms);
    }

    private void addIfContains(String normalized, Set<String> terms, String... aliases) {
        for (String alias : aliases) {
            if (normalized.contains(alias)) {
                for (String item : aliases) {
                    terms.add(item);
                }
                return;
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}，。！？；：、（）【】《》“”‘’]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
