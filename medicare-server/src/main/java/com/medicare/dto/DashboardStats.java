package com.medicare.dto;

import lombok.Data;

/**
 * 仪表盘统计
 */
@Data
public class DashboardStats {

    private long todayRegCount;
    private long waitingCount;
    private long stockAlertCount;
    private long pendingPrescriptionCount;
    private long completedRecordCount;
    private long cancelledRegCount;
}
