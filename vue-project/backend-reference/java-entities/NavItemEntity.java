package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 侧边栏导航项，对应前端 NavItem。 */
@Data
@TableName("nav_item")
public class NavItemEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long groupId;
  private String itemKey;
  private String label;
  private String description;
  private String iconName;
  private String routePath;
  private Integer sortOrder;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
