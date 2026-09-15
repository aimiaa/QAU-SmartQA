package com.aimi.entity;

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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 所属导航分组 ID
    private Long groupId;
    // 菜单项唯一标识（前端引用键）
    private String itemKey;
    // 菜单名称
    private String label;
    // 描述
    private String description;
    // 前端图标名称，例如 MessageSquare、Database
    private String iconName;
    // 路由路径
    private String routePath;
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
