package com.example.campusrag.service;

import java.util.List;

import com.example.campusrag.service.VectorStore.SearchHit;

public interface LlmClient {
    String generate(String question, String prompt, List<SearchHit> hits);
}
