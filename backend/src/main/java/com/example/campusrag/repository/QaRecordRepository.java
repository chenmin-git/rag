package com.example.campusrag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.campusrag.domain.QaRecord;

public interface QaRecordRepository extends JpaRepository<QaRecord, Long> {
    List<QaRecord> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<QaRecord> findTop8ByOrderByCreatedAtDesc();

    @Query("select coalesce(avg(q.latencyMs), 0) from QaRecord q")
    double averageLatencyMs();
}
