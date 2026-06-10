package com.medicare.dao;

import com.medicare.model.SysUser;

import java.sql.SQLException;
import java.util.List;

/**
 * 系统用户数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SysUserDAO extends BaseDAO<SysUser> {

    private static final String SQL_SELECT_BY_USERNAME =
            "SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE username = ? AND status = 1";

    private static final String SQL_INSERT =
            "INSERT INTO sys_user (username, password, real_name, role, status) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE sys_user SET username = ?, password = ?, real_name = ?, status = ? WHERE id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE sys_user SET password = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM sys_user WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE id = ?";

    private static final String SQL_SELECT_ALL_ADMIN =
            "SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE role = 'admin' ORDER BY id";

    private static final String SQL_SELECT_ADMIN_BY_NAME =
            "SELECT id, username, password, real_name, role, status, doctor_id, create_time, update_time " +
            "FROM sys_user WHERE role = 'admin' AND username LIKE ? ORDER BY id";

    private static final String SQL_COUNT_ADMIN =
            "SELECT COUNT(*) FROM sys_user WHERE role = 'admin' AND status = 1";

    private static final String SQL_COUNT_BY_USERNAME =
            "SELECT COUNT(*) FROM sys_user WHERE username = ?";

    private static final String SQL_COUNT_BY_USERNAME_EXCLUDE_ID =
            "SELECT COUNT(*) FROM sys_user WHERE username = ? AND id != ?";

    // ============================================================
    // CRUD
    // ============================================================

    public Long insert(SysUser user) throws SQLException {
        return executeInsert(SQL_INSERT,
                user.getUsername(), user.getPassword(), user.getRealName(),
                user.getRole(), user.getStatus());
    }

    public int update(SysUser user) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                user.getUsername(), user.getPassword(), user.getRealName(),
                user.getStatus(), user.getId());
    }

    public int updatePassword(Long id, String password) throws SQLException {
        return executeUpdate(SQL_UPDATE_PASSWORD, password, id);
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public SysUser findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public SysUser findByUsername(String username) throws SQLException {
        return querySingle(SQL_SELECT_BY_USERNAME, username);
    }

    public List<SysUser> findAllAdmin() throws SQLException {
        return queryList(SQL_SELECT_ALL_ADMIN);
    }

    public List<SysUser> findAdminByName(String name) throws SQLException {
        return queryList(SQL_SELECT_ADMIN_BY_NAME, "%" + name + "%");
    }

    public Long countAdmin() throws SQLException {
        return queryScalar(SQL_COUNT_ADMIN);
    }

    public boolean existsByUsername(String username) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_USERNAME, username);
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username, Long excludeId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_USERNAME_EXCLUDE_ID, username, excludeId);
        return count != null && count > 0;
    }
}
