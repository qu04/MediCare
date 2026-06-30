package com.medicare.config;

import com.medicare.entity.SysUser;
import com.medicare.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        ensureCommercialSchema();
        migrateLegacyPasswords();
        ensureDemoAccounts();
    }

    private void migrateLegacyPasswords() {
        List<SysUser> users = sysUserRepository.findAll();
        int migrated = 0;
        for (SysUser user : users) {
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                sysUserRepository.save(user);
                migrated++;
                log.info("Migrated password to BCrypt for user: {}", user.getUsername());
            }
        }
        if (migrated > 0) {
            log.info("Password migration completed. Total migrated users: {}", migrated);
        } else {
            log.info("All user passwords already use BCrypt.");
        }
    }

    private void ensureCommercialSchema() {
        addColumnIfMissing(
                "sys_user",
                "last_login_time",
                "ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME NULL COMMENT 'last login time'"
        );
        addColumnIfMissing(
                "sys_user",
                "last_login_ip",
                "ALTER TABLE sys_user ADD COLUMN last_login_ip VARCHAR(64) NULL COMMENT 'last login ip'"
        );
        addColumnIfMissing(
                "sys_user",
                "failed_login_attempts",
                "ALTER TABLE sys_user ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 COMMENT 'failed login attempts'"
        );
        addColumnIfMissing(
                "sys_user",
                "locked_until",
                "ALTER TABLE sys_user ADD COLUMN locked_until DATETIME NULL COMMENT 'locked until'"
        );

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS operation_log (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'log id',
                  module VARCHAR(50) NOT NULL COMMENT 'module',
                  action VARCHAR(50) NOT NULL COMMENT 'action',
                  biz_type VARCHAR(50) NULL COMMENT 'business type',
                  biz_id BIGINT UNSIGNED NULL COMMENT 'business id',
                  operator_id BIGINT UNSIGNED NULL COMMENT 'operator id',
                  operator_name VARCHAR(50) NULL COMMENT 'operator name',
                  operator_role VARCHAR(20) NULL COMMENT 'operator role',
                  request_method VARCHAR(10) NULL COMMENT 'request method',
                  request_uri VARCHAR(200) NULL COMMENT 'request uri',
                  ip_address VARCHAR(64) NULL COMMENT 'ip address',
                  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 success 0 failed',
                  message VARCHAR(500) NULL COMMENT 'message',
                  detail VARCHAR(1000) NULL COMMENT 'detail',
                  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'create time',
                  PRIMARY KEY (id),
                  KEY idx_operation_log_module_time (module, create_time),
                  KEY idx_operation_log_operator_time (operator_id, create_time),
                  KEY idx_operation_log_status_time (status, create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='operation log'
                """);
    }

    private void ensureDemoAccounts() {
        ensureUser("admin", "12345", "系统管理员", "admin", null);
        ensureUser("doctor1", "12345", "门诊医生", "doctor", 1L);
        ensureUser("pharmacist1", "12345", "药剂师", "pharmacist", null);
    }

    private void ensureUser(String username, String password, String realName, String role, Long doctorId) {
        if (sysUserRepository.existsByUsername(username)) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus(1);
        user.setDoctorId(doctorId);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        sysUserRepository.save(user);
        log.info("Created demo account: {}", username);
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("Added missing column {}.{}", tableName, columnName);
        }
    }
}
