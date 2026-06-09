package com.example.campusrag.service;

import org.springframework.stereotype.Service;

import com.example.campusrag.domain.OperationLog;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.OperationLogRepository;

@Service
public class AuditService {
    private final OperationLogRepository logRepository;

    public AuditService(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void log(UserAccount user, String action, String target, String detail, String ipAddress, boolean success) {
        OperationLog log = new OperationLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUserName(user.getDisplayName());
        }
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setSuccess(success);
        logRepository.save(log);
    }
}
