package com.example.campusrag.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Cors cors = new Cors();
    private Rag rag = new Rag();
    private Milvus milvus = new Milvus();
    private Spark spark = new Spark();

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Rag getRag() {
        return rag;
    }

    public void setRag(Rag rag) {
        this.rag = rag;
    }

    public Milvus getMilvus() {
        return milvus;
    }

    public void setMilvus(Milvus milvus) {
        this.milvus = milvus;
    }

    public Spark getSpark() {
        return spark;
    }

    public void setSpark(Spark spark) {
        this.spark = spark;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Rag {
        private String vectorStore = "memory";
        private int embeddingDimension = 384;
        private int chunkSize = 900;
        private int chunkOverlap = 160;
        private int topK = 5;
        private double similarityThreshold = 0.2;
        private int maxContextChars = 5200;

        public String getVectorStore() {
            return vectorStore;
        }

        public void setVectorStore(String vectorStore) {
            this.vectorStore = vectorStore;
        }

        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getMaxContextChars() {
            return maxContextChars;
        }

        public void setMaxContextChars(int maxContextChars) {
            this.maxContextChars = maxContextChars;
        }
    }

    public static class Milvus {
        private String uri = "http://localhost:19530";
        private String token = "root:Milvus";
        private String collection = "campus_knowledge_chunks";

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }

    public static class Spark {
        private boolean enabled;
        private String provider = "Spark Pro";
        private String protocol = "http";
        private String endpoint = "https://spark-api-open.xf-yun.com/v1/chat/completions";
        private String apiPassword = "";
        private String model = "generalv3";
        private String appId = "";
        private String apiKey = "";
        private String apiSecret = "";
        private String wsUrl = "wss://spark-api.xf-yun.com/v4.0/chat";
        private String domain = "generalv3";
        private int maxTokens = 2048;
        private double temperature = 0.2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiPassword() {
            return apiPassword;
        }

        public void setApiPassword(String apiPassword) {
            this.apiPassword = apiPassword;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public String getWsUrl() {
            return wsUrl;
        }

        public void setWsUrl(String wsUrl) {
            this.wsUrl = wsUrl;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
