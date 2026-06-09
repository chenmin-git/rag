package com.example.campusrag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.example.campusrag.config.AppProperties;
import com.example.campusrag.service.VectorStore.SearchHit;
import com.example.campusrag.service.VectorStore.SearchRequest;
import com.example.campusrag.service.VectorStore.VectorChunk;
import com.google.gson.JsonObject;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

@Service
@ConditionalOnProperty(name = "app.rag.vector-store", havingValue = "milvus")
public class MilvusVectorStore implements VectorStore {
    private static final String VECTOR_FIELD = "embedding";
    private final AppProperties properties;
    private final MilvusClientV2 client;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public MilvusVectorStore(AppProperties properties) {
        this.properties = properties;
        this.client = new MilvusClientV2(ConnectConfig.builder()
                .uri(properties.getMilvus().getUri())
                .token(properties.getMilvus().getToken())
                .build());
    }

    @Override
    public void upsert(List<VectorChunk> chunks) {
        ensureCollection();
        if (chunks.isEmpty()) {
            return;
        }
        client.delete(DeleteReq.builder()
                .collectionName(collection())
                .ids(chunks.stream().map(VectorChunk::chunkId).map(Object.class::cast).toList())
                .build());
        List<JsonObject> rows = new ArrayList<>();
        for (VectorChunk chunk : chunks) {
            JsonObject row = new JsonObject();
            row.addProperty("id", chunk.chunkId());
            row.addProperty("document_id", chunk.documentId());
            row.addProperty("chunk_no", chunk.chunkNo());
            row.addProperty("page_no", chunk.pageNo());
            row.addProperty("content", trim(chunk.content(), 3900));
            row.addProperty("file_name", trim(chunk.fileName(), 480));
            row.addProperty("department", trim(chunk.department(), 120));
            row.addProperty("visibility", trim(chunk.visibility(), 80));
            row.add(VECTOR_FIELD, toJsonArray(chunk.embedding()));
            rows.add(row);
        }
        client.insert(InsertReq.builder()
                .collectionName(collection())
                .data(rows)
                .build());
    }

    @Override
    public List<SearchHit> search(float[] queryVector, SearchRequest request) {
        ensureCollection();
        SearchResp response = client.search(SearchReq.builder()
                .collectionName(collection())
                .data(List.of(new FloatVec(queryVector)))
                .annsField(VECTOR_FIELD)
                .filter(permissionFilter(request))
                .topK(request.topK())
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .outputFields(List.of("id", "document_id", "chunk_no", "page_no", "content", "file_name", "department", "visibility"))
                .build());
        return response.getSearchResults().stream()
                .flatMap(List::stream)
                .map(result -> {
                    Map<String, Object> entity = result.getEntity();
                    double score = result.getScore();
                    return new SearchHit(
                            String.valueOf(entity.getOrDefault("id", "")),
                            asLong(entity.get("id")),
                            asLong(entity.get("document_id")),
                            asInt(entity.get("chunk_no")),
                            asInt(entity.get("page_no")),
                            String.valueOf(entity.getOrDefault("content", "")),
                            String.valueOf(entity.getOrDefault("file_name", "")),
                            String.valueOf(entity.getOrDefault("department", "")),
                            String.valueOf(entity.getOrDefault("visibility", "")),
                            score);
                })
                .filter(hit -> hit.score() >= request.threshold())
                .toList();
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        ensureCollection();
        client.delete(DeleteReq.builder()
                .collectionName(collection())
                .filter("document_id == " + documentId)
                .build());
    }

    private void ensureCollection() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        boolean exists = client.hasCollection(HasCollectionReq.builder()
                .collectionName(collection())
                .build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = client.createSchema();
            schema.addField(AddFieldReq.builder()
                    .fieldName("id")
                    .dataType(DataType.Int64)
                    .isPrimaryKey(true)
                    .autoID(false)
                    .build());
            schema.addField(AddFieldReq.builder().fieldName("document_id").dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName("chunk_no").dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName("page_no").dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder().fieldName("content").dataType(DataType.VarChar).maxLength(4096).build());
            schema.addField(AddFieldReq.builder().fieldName("file_name").dataType(DataType.VarChar).maxLength(512).build());
            schema.addField(AddFieldReq.builder().fieldName("department").dataType(DataType.VarChar).maxLength(128).build());
            schema.addField(AddFieldReq.builder().fieldName("visibility").dataType(DataType.VarChar).maxLength(96).build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(VECTOR_FIELD)
                    .dataType(DataType.FloatVector)
                    .dimension(properties.getRag().getEmbeddingDimension())
                    .build());
            IndexParam indexParam = IndexParam.builder()
                    .fieldName(VECTOR_FIELD)
                    .metricType(IndexParam.MetricType.COSINE)
                    .build();
            client.createCollection(CreateCollectionReq.builder()
                    .collectionName(collection())
                    .collectionSchema(schema)
                    .build());
            client.createIndex(CreateIndexReq.builder()
                    .collectionName(collection())
                    .indexParams(List.of(indexParam))
                    .build());
        }
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(collection())
                .build());
    }

    private String permissionFilter(SearchRequest request) {
        if (request.user() == null) {
            return "visibility == \"PUBLIC\"";
        }
        if ("SYSTEM_ADMIN".equalsIgnoreCase(request.user().getRole())) {
            return "";
        }
        String role = escape(request.user().getRole());
        String department = escape(request.user().getDepartment());
        return "visibility == \"PUBLIC\" || visibility == \"" + role + "\" || visibility == \"DEPARTMENT:" + department + "\"";
    }

    private String collection() {
        return properties.getMilvus().getCollection();
    }

    private String trim(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private com.google.gson.JsonArray toJsonArray(float[] values) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }
}
