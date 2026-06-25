package com.medicare.dto;

import lombok.Data;

@Data
public class SecurityOverview {
    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long recentFailedLogins;
    private long totalOperationLogs;
}
