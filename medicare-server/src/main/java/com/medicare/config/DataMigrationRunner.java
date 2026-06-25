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
    }

    private void migrateLegacyPasswords() {
        List<SysUser> users = sysUserRepository.findAll();
        int migrated = 0;
        for (SysUser user : users) {
            if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                sysUserRepository.save(user);
                migrated++;
                log.info("迁移用户密码到 BCrypt: {}", user.getUsername());
            }
        }
        if (migrated > 0) {
            log.info("密码迁移完成，共迁移 {} 个用户", migrated);
        } else {
            log.info("所有用户密码已是 BCrypt 格式，无需迁移");
        }
    }

    private void ensureCommercialSchema() {
        addColumnIfMissing("sys_user", "last_login_time",
                "ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间'");
        addColumnIfMissing("sys_user", "last_login_ip",
                "ALTER TABLE sys_user ADD COLUMN last_login_ip VARCHAR(64) NULL COMMENT '最后登录IP'");
        addColumnIfMissing("sys_user", "failed_login_attempts",
                "ALTER TABLE sys_user ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数'");
        addColumnIfMissing("sys_user", "locked_until",
                "ALTER TABLE sys_user ADD COLUMN locked_until DATETIME NULL COMMENT '锁定截止时间'");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS operation_log (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                  module VARCHAR(50) NOT NULL COMMENT '模块',
                  action VARCHAR(50) NOT NULL COMMENT '动作',
                  biz_type VARCHAR(50) NULL COMMENT '业务类型',
                  biz_id BIGINT UNSIGNED NULL COMMENT '业务ID',
                  operator_id BIGINT UNSIGNED NULL COMMENT '操作人ID',
                  operator_name VARCHAR(50) NULL COMMENT '操作人姓名',
                  operator_role VARCHAR(20) NULL COMMENT '操作人角色',
                  request_method VARCHAR(10) NULL COMMENT '请求方法',
                  request_uri VARCHAR(200) NULL COMMENT '请求路径',
                  ip_address VARCHAR(64) NULL COMMENT 'IP地址',
                  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
                  message VARCHAR(500) NULL COMMENT '摘要',
                  detail VARCHAR(1000) NULL COMMENT '详情',
                  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                  PRIMARY KEY (id),
                  KEY idx_operation_log_module_time (module, create_time),
                  KEY idx_operation_log_operator_time (operator_id, create_time),
                  KEY idx_operation_log_status_time (status, create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表'
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("已补齐字段 {}.{}", tableName, columnName);
        }
    }
}
