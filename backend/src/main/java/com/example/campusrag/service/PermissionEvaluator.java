package com.example.campusrag.service;

import org.springframework.stereotype.Component;

import com.example.campusrag.domain.UserAccount;

@Component
public class PermissionEvaluator {
    public boolean canAccess(String visibility, String department, UserAccount user) {
        if (user == null) {
            return "PUBLIC".equalsIgnoreCase(visibility);
        }
        if ("SYSTEM_ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if ("PUBLIC".equalsIgnoreCase(visibility)) {
            return true;
        }
        if ("DEPARTMENT_ADMIN".equalsIgnoreCase(user.getRole())
                && department != null
                && department.equalsIgnoreCase(user.getDepartment())) {
            return true;
        }
        if (visibility != null && visibility.equalsIgnoreCase(user.getRole())) {
            return true;
        }
        return visibility != null
                && visibility.startsWith("DEPARTMENT:")
                && visibility.substring("DEPARTMENT:".length()).equalsIgnoreCase(user.getDepartment());
    }
}
