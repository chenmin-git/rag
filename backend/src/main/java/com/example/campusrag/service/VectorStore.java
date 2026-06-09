package com.example.campusrag.service;

import java.util.List;

import com.example.campusrag.domain.UserAccount;

public interface VectorStore {
    void upsert(List<VectorChunk> chunks);

    List<SearchHit> search(float[] queryVector, SearchRequest request);

    void deleteByDocumentId(Long documentId);

    record SearchRequest(int topK, double threshold, UserAccount user) {
    }

    record VectorChunk(
            String id,
            Long chunkId,
            Long documentId,
            int chunkNo,
            int pageNo,
            String content,
            String fileName,
            String department,
            String visibility,
            float[] embedding) {
    }

    record SearchHit(
            String id,
            Long chunkId,
            Long documentId,
            int chunkNo,
            int pageNo,
            String content,
            String fileName,
            String department,
            String visibility,
            double score) {
    }
}
