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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 操作用户 ID
    private Long userId;
    // 模块名称
    private String moduleName;
    // 操作动作名称
    private String actionName;
    // 操作对象类型
    private String targetType;
    // 操作对象 ID
    private Long targetId;
    // 请求 IP
    private String requestIp;
    // 客户端 User-Agent
    private String userAgent;

    /** PostgreSQL jsonb 字段，实际项目可映射为 JsonNode、Map 或 String。 */
    @TableField("detail_json")
    private String detailJson;

    // 创建时间
    private LocalDateTime createdAt;
}
