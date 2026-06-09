package com.example.campusrag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campusrag.domain.OperationLog;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findTop80ByOrderByCreatedAtDesc();
}
