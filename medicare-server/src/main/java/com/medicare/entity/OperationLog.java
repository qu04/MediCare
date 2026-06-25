package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    public static final Integer STATUS_SUCCESS = 1;
    public static final Integer STATUS_FAILED = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "biz_type", length = 50)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "operator_role", length = 20)
    private String operatorRole;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_uri", length = 200)
    private String requestUri;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private Integer status = STATUS_SUCCESS;

    @Column(length = 500)
    private String message;

    @Column(length = 1000)
    private String detail;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;
}
