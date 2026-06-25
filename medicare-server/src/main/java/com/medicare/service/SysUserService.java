package com.medicare.service;

import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final SysUserRepository sysUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<SysUser> findAll() {
        return sysUserRepository.findAll();
    }

    public SysUser findById(Long id) {
        return sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public SysUser findByUsername(String username) {
        return sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
    }

    public SysUser create(SysUser user) {
        if (sysUserRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return sysUserRepository.save(user);
    }

    public SysUser update(Long id, SysUser user) {
        SysUser existing = findById(id);
        if (sysUserRepository.existsByUsernameAndIdNot(user.getUsername(), id)) {
            throw new BusinessException("用户名已存在");
        }
        existing.setUsername(user.getUsername());
        existing.setRealName(user.getRealName());
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setDoctorId(user.getDoctorId());
        return sysUserRepository.save(existing);
    }

    public void delete(Long id) {
        SysUser user = findById(id);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除超级管理员");
        }
        sysUserRepository.deleteById(id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        SysUser user = findById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        sysUserRepository.save(user);
    }

    public SysUser login(String username, String rawPassword, String ipAddress) {
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("账号已被锁定，请在 " + user.getLockedUntil() + " 后重试");
        }

        boolean match = matchesPassword(user.getPassword(), rawPassword);
        if (!match) {
            handleFailedLogin(user);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        return sysUserRepository.save(user);
    }

    private boolean matchesPassword(String storedPassword, String rawPassword) {
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    private void handleFailedLogin(SysUser user) {
        int failed = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        failed++;
        user.setFailedLoginAttempts(failed);

        if (failed >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailedLoginAttempts(0);
            sysUserRepository.save(user);
            throw new BusinessException("连续登录失败次数过多，账号已锁定 15 分钟");
        }

        sysUserRepository.save(user);
        throw new BusinessException("用户名或密码错误，已连续失败 " + failed + " 次");
    }
}
