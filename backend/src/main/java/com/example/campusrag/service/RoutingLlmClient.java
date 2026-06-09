package com.example.campusrag.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.campusrag.service.VectorStore.SearchHit;

@Service
public class RoutingLlmClient implements LlmClient {
    private final ConfigService configService;
    private final MockSparkLlmClient mockClient;
    private final SparkLlmClient sparkClient;

    public RoutingLlmClient(ConfigService configService, MockSparkLlmClient mockClient, SparkLlmClient sparkClient) {
        this.configService = configService;
        this.mockClient = mockClient;
        this.sparkClient = sparkClient;
    }

    @Override
    public String generate(String question, String prompt, List<SearchHit> hits) {
        if (isSparkReady()) {
            return sparkClient.generate(question, prompt, hits);
        }
        return mockClient.generate(question, prompt, hits);
    }

    public LlmStatus status() {
        boolean enabled = configService.getBoolean("spark.enabled", false);
        boolean configured = !apiPassword().isBlank();
        String mode = enabled && configured ? "SPARK" : "MOCK";
        String message;
        if (enabled && configured) {
            message = "已接入大模型服务，回答由 RAG 检索上下文增强生成。";
        } else if (enabled) {
            message = "已开启大模型服务，但 APIPassword 未配置，当前回退到本地演示回答。";
        } else {
            message = "当前为本地演示回答，可在系统配置中开启大模型服务。";
        }
        return new LlmStatus(
                configService.getString("spark.provider", "Spark Pro"),
                configService.getString("spark.endpoint", ""),
                configService.getString("spark.model", "generalv3"),
                enabled,
                configured,
                mode,
                message);
    }

    private boolean isSparkReady() {
        return configService.getBoolean("spark.enabled", false) && !apiPassword().isBlank();
    }

    private String apiPassword() {
        return configService.getString("spark.apiPassword", firstNonBlank(
                System.getenv("SPARK_API_PASSWORD"),
                System.getenv("XFYUN_SPARK_API_PASSWORD")));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record LlmStatus(
            String provider,
            String endpoint,
            String model,
            boolean enabled,
            boolean configured,
            String mode,
            String message) {
    }
}
