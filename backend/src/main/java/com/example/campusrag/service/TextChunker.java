package com.example.campusrag.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.campusrag.config.AppProperties;

@Component
public class TextChunker {
    private final AppProperties properties;

    public TextChunker(AppProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : normalized.split("\\n\\s*\\n|(?<=[。！？；])")) {
            String cleaned = paragraph.replaceAll("\\s+", " ").trim();
            if (!cleaned.isBlank()) {
                paragraphs.add(cleaned);
            }
        }
        int chunkSize = properties.getRag().getChunkSize();
        int overlap = Math.max(0, properties.getRag().getChunkOverlap());
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() + 1 > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString());
                current = new StringBuilder(tail(current.toString(), overlap));
            }
            if (paragraph.length() > chunkSize) {
                splitLongParagraph(paragraph, chunks, chunkSize, overlap);
            } else {
                if (!current.isEmpty()) {
                    current.append('\n');
                }
                current.append(paragraph);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private void splitLongParagraph(String paragraph, List<String> chunks, int chunkSize, int overlap) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(paragraph.length(), start + chunkSize);
            chunks.add(paragraph.substring(start, end));
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
    }

    private String tail(String value, int length) {
        if (length <= 0 || value.length() <= length) {
            return length <= 0 ? "" : value;
        }
        return value.substring(value.length() - length);
    }
}
