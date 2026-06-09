package com.example.campusrag.service;

import org.springframework.stereotype.Service;

import com.example.campusrag.common.BusinessException;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.UserAccountRepository;

@Service
public class AuthService {
    private final UserAccountRepository userRepository;

    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount login(String username, String password) {
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(password);
        if (normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            throw new BusinessException("账号和密码不能为空");
        }
        UserAccount user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessException("账号已停用");
        }
        if (!user.getPasswordHash().equals(normalizedPassword)) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    public UserAccount requireCurrentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("登录用户不存在"));
    }

    public UserAccount requireAnyRole(Long userId, String... roles) {
        UserAccount user = requireCurrentUser(userId);
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return user;
            }
        }
        throw new BusinessException("当前账号没有权限执行该操作");
    }

    public UserAccount currentUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElseGet(this::fallbackUser);
        }
        return fallbackUser();
    }

    private UserAccount fallbackUser() {
        return userRepository.findByUsername("admin")
                .orElseThrow(() -> new BusinessException("系统尚未初始化用户"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
