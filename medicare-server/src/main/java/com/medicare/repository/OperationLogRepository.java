package com.medicare.repository;

import com.medicare.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query("""
            SELECT l FROM OperationLog l
            WHERE (:module IS NULL OR l.module = :module)
              AND (:status IS NULL OR l.status = :status)
              AND (:keyword IS NULL OR :keyword = '' OR l.operatorName LIKE CONCAT('%', :keyword, '%')
                   OR l.message LIKE CONCAT('%', :keyword, '%')
                   OR l.requestUri LIKE CONCAT('%', :keyword, '%'))
            ORDER BY l.createTime DESC, l.id DESC
            """)
    List<OperationLog> searchRecent(@Param("module") String module,
                                    @Param("status") Integer status,
                                    @Param("keyword") String keyword);

    long countByStatusAndCreateTimeAfter(Integer status, LocalDateTime createTime);

    long countByStatus(Integer status);
}
