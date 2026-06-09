package com.example.campusrag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campusrag.domain.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
