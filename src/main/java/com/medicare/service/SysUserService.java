package com.medicare.service;

import com.medicare.dao.SysUserDAO;
import com.medicare.model.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * 系统用户服务
 * 管理员账号 CRUD + 密码修改
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SysUserService {

    private static final Logger logger = LoggerFactory.getLogger(SysUserService.class);

    private final SysUserDAO sysUserDAO = new SysUserDAO();

    // ============================================================
    // 登录
    // ============================================================

    public SysUser login(String username, String password) throws SQLException, IllegalArgumentException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        SysUser user = sysUserDAO.findByUsername(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        logger.info("用户登录成功: username={}", user.getUsername());
        return user;
    }

    // ============================================================
    // 管理员 CRUD
    // ============================================================

    public Long addAdmin(SysUser user) throws SQLException, IllegalArgumentException {
        validate(user);
        if (sysUserDAO.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        user.setRole(SysUser.ROLE_ADMIN);
        if (user.getStatus() == null) user.setStatus(1);
        Long id = sysUserDAO.insert(user);
        logger.info("新增管理员: id={}, username={}", id, user.getUsername());
        return id;
    }

    public void updateAdmin(SysUser user) throws SQLException, IllegalArgumentException {
        if (user.getId() == null) throw new IllegalArgumentException("用户 ID 不能为空");
        validate(user);
        if (sysUserDAO.existsByUsername(user.getUsername(), user.getId())) {
            throw new IllegalArgumentException("用户名已被其他账号使用: " + user.getUsername());
        }
        int rows = sysUserDAO.update(user);
        if (rows == 0) throw new IllegalArgumentException("管理员不存在");
        logger.info("更新管理员: id={}, username={}", user.getId(), user.getUsername());
    }

    public void deleteAdmin(Long id) throws SQLException, IllegalArgumentException {
        SysUser user = sysUserDAO.findById(id);
        if (user == null) throw new IllegalArgumentException("管理员不存在");

        Long count = sysUserDAO.countAdmin();
        if (count == null || count <= 1) {
            throw new IllegalArgumentException("系统至少需要一个管理员，无法删除");
        }

        sysUserDAO.delete(id);
        logger.info("删除管理员: id={}", id);
    }

    public SysUser getById(Long id) throws SQLException {
        return sysUserDAO.findById(id);
    }

    public List<SysUser> listAllAdmin() throws SQLException {
        return sysUserDAO.findAllAdmin();
    }

    public List<SysUser> searchAdmin(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) return listAllAdmin();
        return sysUserDAO.findAdminByName(keyword.trim());
    }

    // ============================================================
    // 密码修改
    // ============================================================

    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword)
            throws SQLException, IllegalArgumentException {
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new IllegalArgumentException("原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (newPassword.length() < 4) {
            throw new IllegalArgumentException("新密码长度不能少于 4 位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }

        SysUser user = sysUserDAO.findById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!oldPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        sysUserDAO.updatePassword(userId, newPassword);
        logger.info("密码修改成功: userId={}", userId);
    }

    // ============================================================
    // 私有校验
    // ============================================================

    private void validate(SysUser user) {
        if (user == null) throw new IllegalArgumentException("用户信息不能为空");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }
}
