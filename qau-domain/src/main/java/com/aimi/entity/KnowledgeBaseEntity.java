package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库，对应前端 KnowledgeBase。 */
@Data
@TableName("knowledge_base")
public class KnowledgeBaseEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 知识库名称
    private String name;
    // 分类（如教务、图书、生活等）
    private String category;
    // 状态：ready 已完成 / syncing 同步中 / review 待复核 / disabled 停用
    private String status;
    // 描述
    private String description;
    // 文档数量
    private Integer documentCount;
    // 归属院系
    private String ownerDepartment;
    // 最后一次同步时间
    private LocalDateTime lastSyncedAt;
    // 创建人用户 ID
    private Long createdBy;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
