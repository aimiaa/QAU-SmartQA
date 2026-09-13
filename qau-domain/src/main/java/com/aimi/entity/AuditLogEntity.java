package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 权限审计日志：记录用户关键操作。 */
@Data
@TableName("audit_log")
public class AuditLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String moduleName;
    private String actionName;
    private String targetType;
    private Long targetId;
    private String requestIp;
    private String userAgent;

    /** PostgreSQL jsonb 字段，实际项目可映射为 JsonNode、Map 或 String。 */
    @TableField("detail_json")
    private String detailJson;

    private LocalDateTime createdAt;
}
