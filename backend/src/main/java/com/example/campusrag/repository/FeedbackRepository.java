package com.example.campusrag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.campusrag.domain.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findTop80ByOrderByCreatedAtDesc();

    @Query("select f.type, count(f) from Feedback f group by f.type")
    List<Object[]> countByType();
}
