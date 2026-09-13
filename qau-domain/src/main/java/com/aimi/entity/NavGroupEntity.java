package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 侧边栏导航分组，对应前端 NavGroup。 */
@Data
@TableName("nav_group")
public class NavGroupEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupKey;
    private String title;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
