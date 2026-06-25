package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.Result;
import com.medicare.dto.SecurityOverview;
import com.medicare.entity.OperationLog;
import com.medicare.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final OperationLogService operationLogService;

    @GetMapping("/security-overview")
    @RequireRole("admin")
    public Result<SecurityOverview> getSecurityOverview() {
        return Result.ok(operationLogService.getSecurityOverview());
    }

    @GetMapping("/operation-logs")
    @RequireRole("admin")
    public Result<List<OperationLog>> listOperationLogs(@RequestParam(value = "module", required = false) String module,
                                                        @RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "keyword", required = false) String keyword,
                                                        @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        return Result.ok(operationLogService.searchRecent(module, status, keyword, limit));
    }
}
