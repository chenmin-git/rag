package com.example.campusrag.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.example.campusrag.service.VectorStore.SearchHit;
import com.example.campusrag.service.VectorStore.SearchRequest;
import com.example.campusrag.service.VectorStore.VectorChunk;

@Service
@ConditionalOnProperty(name = "app.rag.vector-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryVectorStore implements VectorStore {
    private final Map<String, VectorChunk> chunks = new ConcurrentHashMap<>();
    private final PermissionEvaluator permissionEvaluator;

    public InMemoryVectorStore(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @Override
    public void upsert(List<VectorChunk> chunks) {
        for (VectorChunk chunk : chunks) {
            this.chunks.put(chunk.id(), chunk);
        }
    }

    @Override
    public List<SearchHit> search(float[] queryVector, SearchRequest request) {
        return chunks.values().stream()
                .filter(chunk -> permissionEvaluator.canAccess(chunk.visibility(), chunk.department(), request.user()))
                .map(chunk -> toHit(chunk, cosine(queryVector, chunk.embedding())))
                .filter(hit -> hit.score() >= request.threshold())
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(request.topK())
                .toList();
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        chunks.values().removeIf(chunk -> chunk.documentId().equals(documentId));
    }

    private SearchHit toHit(VectorChunk chunk, double score) {
        return new SearchHit(
                chunk.id(),
                chunk.chunkId(),
                chunk.documentId(),
                chunk.chunkNo(),
                chunk.pageNo(),
                chunk.content(),
                chunk.fileName(),
                chunk.department(),
                chunk.visibility(),
                score);
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
