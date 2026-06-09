package com.example.campusrag.domain;

public enum DocumentStatus {
    PENDING,
    PARSING,
    CHUNKING,
    VECTORIZING,
    INDEXED,
    FAILED
}
