package com.qau.ai.domain.entity;

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
  @TableId(type = IdType.AUTO)
  private Long id;
  private String configKey;
  private String configValue;
  private String configType;
  private String description;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
