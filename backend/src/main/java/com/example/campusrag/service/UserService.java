package com.example.campusrag.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusrag.common.BusinessException;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.UserAccountRepository;

@Service
public class UserService {
    private final UserAccountRepository userRepository;

    public UserService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> list() {
        return userRepository.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
    }

    @Transactional
    public UserAccount create(String username, String password, String displayName, String role, String department) {
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new BusinessException("账号已存在");
        });
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(password == null || password.isBlank() ? "123456" : password);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setDepartment(department);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public UserAccount update(Long id, String displayName, String role, String department, Boolean enabled) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }
        if (role != null && !role.isBlank()) {
            user.setRole(role);
        }
        if (department != null && !department.isBlank()) {
            user.setDepartment(department);
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        return userRepository.save(user);
    }
}
