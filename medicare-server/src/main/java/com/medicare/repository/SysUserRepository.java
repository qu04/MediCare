package com.medicare.repository;

import com.medicare.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    long countByStatus(Integer status);

    @Query("SELECT COUNT(u) FROM SysUser u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > :now")
    long countLockedUsers(@Param("now") LocalDateTime now);
}
