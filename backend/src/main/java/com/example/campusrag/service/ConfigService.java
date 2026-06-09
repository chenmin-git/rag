package com.example.campusrag.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusrag.domain.SystemConfig;
import com.example.campusrag.repository.SystemConfigRepository;

@Service
public class ConfigService {
    private final SystemConfigRepository configRepository;

    public ConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> all() {
        return configRepository.findAll().stream()
                .sorted((left, right) -> left.getConfigKey().compareTo(right.getConfigKey()))
                .toList();
    }

    @Transactional
    public List<SystemConfig> update(Map<String, String> values) {
        values.forEach((key, value) -> {
            SystemConfig config = configRepository.findByConfigKey(key).orElseGet(() -> {
                SystemConfig created = new SystemConfig();
                created.setConfigKey(key);
                created.setDescription("运行参数");
                return created;
            });
            config.setConfigValue(value);
            config.setUpdatedAt(LocalDateTime.now());
            configRepository.save(config);
        });
        return all();
    }

    public Map<String, String> asMap() {
        return all().stream().collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
    }

    @Transactional(readOnly = true)
    public String getString(String key, String fallback) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean fallback) {
        String value = getString(key, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(getString(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
