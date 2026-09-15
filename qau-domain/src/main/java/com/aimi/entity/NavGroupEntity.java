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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 分组唯一标识（前端引用键）
    private String groupKey;
    // 分组标题
    private String title;
    // 排序序号（升序展示）
    private Integer sortOrder;
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
