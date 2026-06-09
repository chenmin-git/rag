package com.example.campusrag.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.campusrag.common.BusinessException;
import com.example.campusrag.config.AppProperties;
import com.example.campusrag.service.VectorStore.SearchHit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SparkLlmClient {
    private final AppProperties properties;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public SparkLlmClient(AppProperties properties, ConfigService configService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    public String generate(String question, String prompt, List<SearchHit> hits) {
        AppProperties.Spark spark = properties.getSpark();
        String endpoint = configService.getString("spark.endpoint", spark.getEndpoint());
        String model = configService.getString("spark.model", spark.getModel());
        String apiPassword = configService.getString("spark.apiPassword", firstNonBlank(
                spark.getApiPassword(),
                System.getenv("SPARK_API_PASSWORD"),
                System.getenv("XFYUN_SPARK_API_PASSWORD")));
        if (apiPassword.isBlank()) {
            throw new BusinessException("大模型服务 APIPassword 未配置，请在后台系统配置或环境变量中填写。");
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false,
                    "temperature", configService.getDouble("spark.temperature", spark.getTemperature()),
                    "max_tokens", configService.getInt("spark.maxTokens", spark.getMaxTokens()));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(75))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiPassword)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("大模型服务调用失败，HTTP " + response.statusCode() + "：" + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.at("/choices/0/message/content");
            if (!content.isMissingNode() && !content.asText().isBlank()) {
                return content.asText();
            }
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                throw new BusinessException(error.path("message").asText("大模型服务返回错误"));
            }
            throw new BusinessException("大模型服务返回为空");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("大模型服务调用失败：" + exception.getMessage());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
