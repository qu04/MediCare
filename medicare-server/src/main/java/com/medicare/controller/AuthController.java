package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.dto.LoginRequest;
import com.medicare.dto.Result;
import com.medicare.entity.OperationLog;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.service.OperationLogService;
import com.medicare.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final OperationLogService operationLogService;

    @PostMapping("/login")
    public Result<SysUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ip = operationLogService.resolveClientIp(httpRequest);
            SysUser user = sysUserService.login(request.getUsername(), request.getPassword(), ip);
            SysUser safeUser = sanitize(user);
            httpRequest.getSession(true).setAttribute(AuthInterceptor.CURRENT_USER_KEY, safeUser);
            operationLogService.record("AUTH", "LOGIN", OperationLog.STATUS_SUCCESS,
                    "用户登录成功", safeUser, httpRequest, "username=" + safeUser.getUsername());
            return Result.ok(safeUser);
        } catch (BusinessException ex) {
            operationLogService.record("AUTH", "LOGIN", OperationLog.STATUS_FAILED,
                    ex.getMessage(), null, httpRequest, "username=" + request.getUsername());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(request);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        operationLogService.record("AUTH", "LOGOUT", OperationLog.STATUS_SUCCESS,
                "用户退出登录", currentUser, request, currentUser == null ? null : "username=" + currentUser.getUsername());
        return Result.ok();
    }

    @GetMapping("/current")
    public Result<SysUser> current(HttpServletRequest request) {
        SysUser user = AuthInterceptor.getCurrentUser(request);
        return Result.ok(user == null ? null : sanitize(user));
    }

    private SysUser sanitize(SysUser user) {
        SysUser copy = new SysUser();
        copy.setId(user.getId());
        copy.setUsername(user.getUsername());
        copy.setPassword(null);
        copy.setRealName(user.getRealName());
        copy.setRole(user.getRole());
        copy.setStatus(user.getStatus());
        copy.setDoctorId(user.getDoctorId());
        copy.setLastLoginTime(user.getLastLoginTime());
        copy.setLastLoginIp(user.getLastLoginIp());
        copy.setFailedLoginAttempts(user.getFailedLoginAttempts());
        copy.setLockedUntil(user.getLockedUntil());
        copy.setCreateTime(user.getCreateTime());
        copy.setUpdateTime(user.getUpdateTime());
        return copy;
    }
}
