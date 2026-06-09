package com.example.campusrag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campusrag.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}
