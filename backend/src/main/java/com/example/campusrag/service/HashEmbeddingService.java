package com.example.campusrag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.campusrag.config.AppProperties;

@Service
public class HashEmbeddingService implements EmbeddingService {
    private final int dimension;

    public HashEmbeddingService(AppProperties properties) {
        this.dimension = properties.getRag().getEmbeddingDimension();
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        for (String term : terms(text)) {
            int hash = term.hashCode();
            int index = Math.floorMod(hash, dimension);
            float sign = (hash & 1) == 0 ? 1.0f : -1.0f;
            vector[index] += sign * weight(term);
        }
        normalize(vector);
        return vector;
    }

    private List<String> terms(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}，。！？；：、（）【】《》“”‘’]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> result = new ArrayList<>();
        if (normalized.isBlank()) {
            return result;
        }
        for (String token : normalized.split(" ")) {
            if (token.length() <= 1) {
                result.add(token);
                continue;
            }
            result.add(token);
            for (int i = 0; i < token.length() - 1; i++) {
                result.add(token.substring(i, i + 2));
            }
            for (int i = 0; i < token.length() - 2; i++) {
                result.add(token.substring(i, i + 3));
            }
        }
        return result;
    }

    private float weight(String term) {
        return term.length() > 2 ? 1.15f : 1.0f;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
