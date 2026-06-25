package com.medicare.service;

import com.medicare.dto.SecurityOverview;
import com.medicare.entity.OperationLog;
import com.medicare.entity.SysUser;
import com.medicare.repository.OperationLogRepository;
import com.medicare.repository.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final SysUserRepository sysUserRepository;

    public OperationLog save(OperationLog log) {
        return operationLogRepository.save(log);
    }

    public List<OperationLog> searchRecent(String module, Integer status, String keyword, Integer limit) {
        List<OperationLog> logs = operationLogRepository.searchRecent(module, status, keyword);
        int finalLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return logs.size() <= finalLimit ? logs : logs.subList(0, finalLimit);
    }

    public SecurityOverview getSecurityOverview() {
        SecurityOverview overview = new SecurityOverview();
        overview.setTotalUsers(sysUserRepository.count());
        overview.setActiveUsers(sysUserRepository.countByStatus(1));
        overview.setLockedUsers(sysUserRepository.countLockedUsers(LocalDateTime.now()));
        overview.setRecentFailedLogins(operationLogRepository.countByStatusAndCreateTimeAfter(
                OperationLog.STATUS_FAILED, LocalDateTime.now().minusDays(1)));
        overview.setTotalOperationLogs(operationLogRepository.count());
        return overview;
    }

    public void record(String module, String action, Integer status, String message,
                       SysUser operator, HttpServletRequest request, String detail) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setStatus(status);
        log.setMessage(message);
        log.setDetail(detail);
        if (operator != null) {
            log.setOperatorId(operator.getId());
            log.setOperatorName(operator.getRealName());
            log.setOperatorRole(operator.getRole());
        }
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUri(request.getRequestURI());
            log.setIpAddress(resolveClientIp(request));
        }
        operationLogRepository.save(log);
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
