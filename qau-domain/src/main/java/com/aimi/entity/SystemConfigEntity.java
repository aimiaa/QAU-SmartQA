package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 系统配置：模型、接口、检索参数等后台配置。 */
@Data
@TableName("system_config")
public class SystemConfigEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 配置键（全局唯一）
    private String configKey;
    // 配置值
    private String configValue;
    // 值类型：string 字符串 / number 数字 / boolean 布尔 / json 对象
    private String configType;
    // 描述
    private String description;
    // 是否启用
    private Boolean enabled;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
