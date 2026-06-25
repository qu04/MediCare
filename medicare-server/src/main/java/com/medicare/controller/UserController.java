package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.auth.RequireRole;
import com.medicare.dto.ChangePasswordRequest;
import com.medicare.dto.Result;
import com.medicare.entity.SysUser;
import com.medicare.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @GetMapping
    @RequireRole("admin")
    public Result<List<SysUser>> list() {
        return Result.ok(sysUserService.findAll().stream().map(this::sanitize).toList());
    }

    @PostMapping
    @RequireRole("admin")
    public Result<SysUser> create(@Valid @RequestBody SysUser user) {
        return Result.ok(sanitize(sysUserService.create(user)));
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<SysUser> update(@PathVariable Long id, @Valid @RequestBody SysUser user) {
        return Result.ok(sanitize(sysUserService.update(id, user)));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable Long id,
                                       @Valid @RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest) {
        SysUser currentUser = AuthInterceptor.getCurrentUser(httpRequest);
        if (currentUser == null || !currentUser.getId().equals(id)) {
            return Result.error(403, "只能修改自己的密码");
        }
        sysUserService.changePassword(id, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
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
